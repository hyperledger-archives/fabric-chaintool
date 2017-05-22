;;-----------------------------------------------------------------------------
;; Copyright 2017 Greg Haskins
;;
;; SPDX-License-Identifier: Apache-2.0
;;-----------------------------------------------------------------------------
(ns fabric-sdk.macros
    (:require [promesa.core :as promesa]))

(defmacro wrapped-resolve [expr exec result]
  `(do
     (comment (println "expr:" ~expr "resolved with" ~result))
     (~exec ~result)))

(defmacro wrapped-reject [expr exec result]
  `(do
     (comment (println "expr:" ~expr "rejected with" ~result))
     (~exec ~result)))

(defmacro pwrap
  ;; Implements an interop wrapper between JS promise and promesa
  [expr]
  `(promesa/promise
    (fn [resolve# reject#]
      (-> ~expr
          (.then #(wrapped-resolve '~expr resolve# %)
                 #(wrapped-reject '~expr reject# %))))))
