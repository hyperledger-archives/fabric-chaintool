(ns example02.rpc
  (:require [cljs.nodejs :as nodejs]
            [example02.hlc.user :as hlc.user]
            [promesa.core :as p :include-macros true]))

(defn- post [method {:keys [user id func args] :as options}]
  (let [request #js {:chaincodeID id
                     :fcn func
                     :args #js [(.toBase64 args)]}]

    (p/chain (method user request) str)))

(defn deploy [args]
  (post hlc.user/deploy args))

(defn invoke [args]
  (post hlc.user/invoke args))

(defn query [args]
  (post hlc.user/query args))
