(ns example02.hlc.user
  (:require [cljs.nodejs :as nodejs]
            [promesa.core :as p :include-macros true]))

(def hlc (nodejs/require "hlc"))

(defn enroll [member password]
  (p/promise
   (fn [resolve reject]
     (.enroll member password
              (fn [err user]
                (if err
                  (reject err)
                  (resolve :success)))))))

(defn enrolled? [member]
  (p/do* (.isEnrolled member)))

(defn- post [func]
  (p/promise
   (fn [resolve reject]
     (let [tx (func)]
       (.on tx "complete" resolve)
       (.on tx "error" reject)))))

(defn invoke [user request]
  (post #(.invoke user request)))

(defn query [user request]
  (post #(.query user request)))
