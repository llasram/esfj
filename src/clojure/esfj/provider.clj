(ns esfj.provider
  (:import [clojure.asm Opcodes Type ClassWriter]
           [clojure.asm.commons Method GeneratorAdapter]
           [clojure.lang DynamicClassLoader IFn IDeref RT Symbol Var]
           [javax.inject Provider]))

(defn ^:private coerce
  "Coerce `x` to be of class `c` by applying `f` to it iff `x` isn't
already an instance of `c`."
  [c f x] (if (instance? c x) x (f x)))

(defn ^:private ->Class*
  [c] (or (ns-resolve *ns* (symbol c))
          (RT/classForName (str c))))

(defn ^:private ->Class
  [c] (coerce Class ->Class* c))

(defn ^:private self-type
  {:tag `Type}
  [sym]
  (let [iname (-> sym str (.replace \. \/))]
    (Type/getType (str "L" iname ";"))))

(defn ^:private class-signature
  [rtype]
  (str "Ljava/lang/Object;Ljavax/inject/Provider<"
       (Type/getDescriptor rtype)
       ">;"))

(defn ^:private ->Type
  {:tag `Type}
  [x] (coerce Type #(Type/getType ^Class %) x))

(defn ^:private ->Types
  {:tag "[Lclojure.asm.Type;"}
  [xs] (into-array Type (map ->Type xs)))

(defn ^:private asm-method
  [name rtype atypes]
  (Method. ^String name ^Type (->Type rtype) (->Types atypes)))

(defn ^:private invoke-method
  [n] (asm-method "invoke" Object (repeat n Object)))

(defn ^:private generate-provider
  [cname rtype atypes]
  (let [rtype (->Class rtype), atypes (map ->Class atypes)
        [t-obj t-ifn t-ideref t-ret] (map ->Type [Object IFn IDeref rtype])
        t-self (self-type cname), iname (.getInternalName t-self)
        atypes (->Types atypes)
        m-clinit (Method/getMethod "void <clinit>()")
        m-base (Method/getMethod "void <init>()")
        m-init (Method. "<init>" Type/VOID_TYPE atypes)
        m-deref (Method/getMethod "Object deref()")
        m-get (Method. "get" t-ret (->Types []))
        m-syn (Method. "get" t-obj (->Types []))
        f-construct "__construct", f-get "__get"
        cw (ClassWriter. ClassWriter/COMPUTE_FRAMES)]
    (doto cw
      (.visit Opcodes/V1_6 (bit-or Opcodes/ACC_PUBLIC Opcodes/ACC_SUPER)
              iname (class-signature rtype) "java/lang/Object"
              (into-array String ["javax/inject/Provider"]))
      (-> (.visitField (bit-or Opcodes/ACC_PUBLIC Opcodes/ACC_STATIC
                               Opcodes/ACC_FINAL)
                       f-construct "Lclojure/lang/IDeref;" nil nil)
          (.visitEnd))
      (-> (.visitField (bit-or Opcodes/ACC_PRIVATE Opcodes/ACC_FINAL)
                       f-get "Lclojure/lang/IFn;" nil nil)
          (.visitEnd)))
    (doto (GeneratorAdapter. Opcodes/ACC_PUBLIC m-init nil nil cw)
      (-> (.visitAnnotation "Ljavax/inject/Inject;" true) (.visitEnd))
      (.visitCode)
      (.loadThis)
      (.invokeConstructor t-obj m-base)
      (.loadThis)
      (.getStatic t-self f-construct t-ideref)
      (.invokeInterface t-ideref m-deref)
      (.checkCast t-ifn)
      (.loadArgs)
      (.invokeInterface t-ifn (-> atypes count invoke-method))
      (.checkCast t-ifn)
      (.putField t-self f-get t-ifn)
      (.returnValue)
      (.endMethod))
    (doto (GeneratorAdapter. Opcodes/ACC_PUBLIC m-get nil nil cw)
      (.visitCode)
      (.loadThis)
      (.getField t-self f-get t-ifn)
      (.invokeInterface t-ifn (invoke-method 0))
      (.checkCast t-ret)
      (.returnValue)
      (.endMethod))
    (doto (GeneratorAdapter. (bit-or Opcodes/ACC_PUBLIC Opcodes/ACC_BRIDGE
                                     Opcodes/ACC_SYNTHETIC)
                             m-syn nil nil cw)
      (.visitCode)
      (.loadThis)
      (.invokeVirtual t-self m-get)
      (.returnValue)
      (.endMethod))
    (doto (GeneratorAdapter. Opcodes/ACC_STATIC m-clinit nil nil cw)
      (.visitCode)
      (.push "clojure.core")
      (.push "promise")
      (.invokeStatic (->Type RT) (asm-method "var" Var [String String]))
      (.invokeVirtual (->Type Var) (invoke-method 0))
      (.checkCast t-ideref)
      (.putStatic t-self f-construct t-ideref)
      (.returnValue)
      (.endMethod))
    (.visitEnd cw)
    (.toByteArray cw)))

(defn ^:internal gen-provider
  [cname rtype atypes]
  (let [bytecode (generate-provider cname rtype atypes)
        dcl ^DynamicClassLoader (RT/makeClassLoader)]
    (.defineClass dcl (str cname) bytecode nil)))

(defn ^:private class-name
  [cname]
  (-> (namespace-munge *ns*)
      (str "$" (gensym "provider") "$" (munge cname))
      symbol))

(defn provider
  "Define and return an anonymous Provider class which provides an instances of
`rtype` (a class), is constructed with instances of `atypes` (a vector of
classes), and is implemented by the function `f`.  The function `f` will be
called with the `atypes` instances during construction and should return a
zero-argument function which will be called on each `get`."
  {:tag `Class}
  ([rtype atypes f] (provider (gensym "provider__") rtype atypes f))
  ([cname rtype atypes f]
     (let [cname (class-name cname)]
       (let [^Class klass (gen-provider cname rtype atypes)
             construct (-> klass (.getField "__construct") (.get nil))]
         (deliver construct f)
         klass))))

(defmacro fn-provider
  "Define and return anonymous Provider class using `fn`-like syntax, where
`params` is a vector of constructor parameters and `body` defines the provider
`get` implementation.  The `:tag` metadata on the `params` vector and on each
`params` member symbol defines the Provider-produced type and constructor
argument types respectively."
  [params & body]
  (let [rtype (-> params meta :tag)
        atypes (mapv (comp :tag meta) params)
        params (vary-meta params dissoc :tag)
        fdecl (if (-> params meta :hof)
                `(fn ~params ~@body)
                `(fn ~params (fn [] ~@body)))]
    `(provider ~rtype ~atypes ~fdecl)))

(defmacro defprovider
  "Like `defn`, but for providers as per `fn-provider`.  The resulting var will
hold a reference to the defined Provider class."
  {:arglists '([name doc-string? attr-map? [params*] & body])}
  [name & forms]
  (let [[[doc attrs] [params & body]] (split-with (complement vector?) forms)
        attrs (-> (meta name) (merge attrs) (cond-> doc (assoc :doc doc)))
        rtype (or (-> attrs :tag) (-> params meta :tag) (-> name meta :tag))
        atypes (mapv (comp :tag meta) params)
        params (vary-meta params dissoc :tag)
        name (vary-meta name merge (assoc attrs :tag `Class))
        fdecl (if (:hof attrs)
                `(fn ~params ~@body)
                `(fn ~params (fn [] ~@body)))]
    `(def ~name
       (provider '~name ~rtype ~atypes ~fdecl))))
