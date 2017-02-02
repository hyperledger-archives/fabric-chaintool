(ns example02.util
  (:require [cljs.nodejs :as nodejs]))

(def _mkdirp (nodejs/require "mkdirp"))

(defn mkdirp [path]
  (_mkdirp.sync path))
