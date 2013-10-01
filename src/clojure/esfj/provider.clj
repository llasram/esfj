(ns esfj.provider
  (:import [clojure.asm Opcodes Type ClassWriter]
           [clojure.asm.commons Method GeneratorAdapter]
           [clojure.lang DynamicClassLoader IFn RT Symbol Var]
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
  [x] (coerce Type #(Type/getType %) x))

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
        [t-obj t-ifn t-ret] (map ->Type [Object IFn rtype])
        t-self (self-type cname), iname (.getInternalName t-self)
        atypes (->Types (cons IFn atypes))
        m-base (Method/getMethod "void <init>()")
        m-init (Method. "<init>" Type/VOID_TYPE atypes)
        m-get (Method. "get" t-ret (->Types []))
        m-syn (Method. "get" t-obj (->Types []))
        cw (ClassWriter. ClassWriter/COMPUTE_FRAMES)]
    (doto cw
      (.visit (bit-or Opcodes/V1_6 1) ;; Not sure what flag 1 is :-/
              (bit-or Opcodes/ACC_PUBLIC Opcodes/ACC_SUPER)
              iname (class-signature rtype) "java/lang/Object"
              (into-array String ["javax/inject/Provider"]))
      (-> (.visitField (bit-or Opcodes/ACC_PRIVATE Opcodes/ACC_FINAL)
                       "factory" "Lclojure/lang/IFn;" nil nil)
          (.visitEnd)))
    (doto (GeneratorAdapter. Opcodes/ACC_PUBLIC m-init nil nil cw)
      (-> (.visitAnnotation "Ljavax/inject/Inject;" true) (.visitEnd))
      (-> (.visitParameterAnnotation 0 "Lesfj/provider/Factory;" true)
          (.visitEnd))
      (.visitCode)
      (.loadThis)
      (.invokeConstructor t-obj m-base)
      (.loadThis)
      (.loadArgs)
      (.invokeInterface t-ifn (-> atypes count dec invoke-method))
      (.checkCast t-ifn)
      (.putField t-self "factory" t-ifn)
      (.returnValue)
      (.endMethod))
    (doto (GeneratorAdapter. Opcodes/ACC_PUBLIC m-get nil nil cw)
      (.visitCode)
      (.loadThis)
      (.getField t-self "factory" t-ifn)
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
    (.visitEnd cw)
    (.toByteArray cw)))

(defmacro gen-provider
  [cname rtype atypes]
  (let [bytecode (generate-provider cname rtype atypes)]
    (if *compile-files*
      (clojure.lang.Compiler/writeClassFile cname bytecode)
      (.defineClass ^DynamicClassLoader (deref clojure.lang.Compiler/LOADER)
                    (str cname) bytecode nil))))

(defmacro defprovider
  "Creates a new Java class implementing the JSR-330 `Provider`
interface for the class `rtype`.  The created class has a single
constructor which accepts an `IFn` factory-factory parameter followed
by parameter of the types provided in `atypes`.  The constructor is
annotated with the `Inject` annotation and the `IFn` parameter is
annotated with the `esfj.provider.Factory` `Qualifier` annotation."
  [name rtype atypes]
  (let [cname (-> (namespace-munge *ns*) (str "." name) symbol
                  (with-meta (meta name)))]
    `(do
       (gen-provider ~cname ~rtype ~atypes)
       (import ~cname))))
