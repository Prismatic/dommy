(ns dommy.macros)

(defmacro deftempl [name args node-form]
  `(defn ~name ~args
    (dommy.template/node ~node-form)))

(defmacro defctempl [name args node-form]
  `(defn ~name ~args
    (dommy.template-compile/node ~node-form)))

(defmacro defnode [name node-form]
  `(def ~name
     (dommy.template/node ~node-form)))

(defmacro defcnode [name node-form]
  `(def ~name
     (dommy.template-compile/node ~node-form)))