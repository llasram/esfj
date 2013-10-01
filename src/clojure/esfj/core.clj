(ns esfj.core
  (:refer-clojure :exclude [gen-class])
  (:require [shady.gen-class :refer [gen-class]])
  (:import [clojure.asm Opcodes Type ClassWriter]
           [clojure.asm.commons Method GeneratorAdapter]
           [clojure.lang DynamicClassLoader IFn RT Symbol Var]
           [javax.inject Provider]))

(defn ^:private coerce
  "Coerce `x` to be of class `c` by applying `f` to it iff `x` isn't
already an instance of `c`."
  [c f x] (if (instance? c x) x (f x)))

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

(defn ^:private make-provider
  [impl-ns impl-var cname rtype atypes]
  (let [[impl-ns impl-var cname] (map str [impl-ns impl-var cname])
        [t-obj t-ifn t-rt t-sym t-var t-ret]
        , (map ->Type [Object IFn RT Symbol Var rtype])
        t-self (self-type cname), iname (.getInternalName t-self)
        atypes (->Types atypes)
        m-base (Method/getMethod "void <init>()")
        m-init (Method. "<init>" Type/VOID_TYPE atypes)
        m-clinit (Method/getMethod "void <clinit>()")
        m-get (Method. "get" t-ret (->Types []))
        m-syn (Method. "get" t-obj (->Types []))
        cw (ClassWriter. ClassWriter/COMPUTE_FRAMES)]
    (doto cw
      (.visit (bit-or Opcodes/V1_6 1) ;; Not sure what flag 1 is :-/
              (bit-or Opcodes/ACC_PUBLIC Opcodes/ACC_SUPER)
              iname (class-signature rtype) "java/lang/Object"
              (into-array String ["javax/inject/Provider"]))
      (-> (.visitField (bit-or Opcodes/ACC_PRIVATE Opcodes/ACC_FINAL
                               Opcodes/ACC_STATIC)
                       "FACTORY" "Lclojure/lang/IFn;" nil nil)
          (.visitEnd))
      (-> (.visitField (bit-or Opcodes/ACC_PRIVATE Opcodes/ACC_FINAL)
                       "factory" "Lclojure/lang/IFn;" nil nil)
          (.visitEnd)))
    (doto (GeneratorAdapter. Opcodes/ACC_STATIC m-clinit nil nil cw)
      (.visitCode)
      (.visitLdcInsn "clojure.core")
      (.visitLdcInsn "require")
      (.invokeStatic t-rt (asm-method "var" Var [String String]))
      (.visitLdcInsn impl-ns)
      (.invokeStatic t-sym (asm-method "intern" Symbol [String]))
      (.invokeVirtual t-var (invoke-method 1))
      (.pop)
      (.visitLdcInsn impl-ns)
      (.visitLdcInsn impl-var)
      (.invokeStatic t-rt (asm-method "var" Var [String String]))
      (.putStatic t-self "FACTORY" t-ifn))
    (doto (GeneratorAdapter. Opcodes/ACC_PUBLIC m-init nil nil cw)
      (-> (.visitAnnotation "Ljavax/inject/Inject;" true) (.visitEnd))
      (-> (.visitParameterAnnotation 0 "Lesfj/Factory;" true) (.visitEnd))
      (.visitCode)
      (.loadThis)
      (.invokeConstructor t-obj m-base)
      (.loadThis)
      (.getStatic t-self "FACTORY" t-ifn)
      (.loadArgs)
      (.invokeInterface t-ifn (-> atypes count invoke-method))
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
    (with-open [outs (clojure.java.io/output-stream "Example1.class")]
      (.write outs (.toByteArray cw)))
    #_(.defineClass ^DynamicClassLoader (deref clojure.lang.Compiler/LOADER)
                    (str cname) (.toByteArray cw) nil)))

(comment
 (defn ^:private signature
   [cname]
   (let [sw (doto (SignatureWriter.) (.visitClassType cname))
         sv (.visitInterface sw)]
     sw)))
