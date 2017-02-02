(ns example02.rpc
  (:require [cljs.nodejs :as nodejs]))

(def http (nodejs/require "http"))

(defn- stringify [json]
  (.stringify js/JSON json))

(defn- response-handler [cb resp]
  (.setEncoding resp "utf8")
  (.on resp "data" (fn [data]
                     (let [resp (js->clj (.parse js/JSON data) :keywordize-keys true)]
                       (cb (select-keys resp [:error :result]))))))

(defn- post [{:keys [host port path method id func args cb]}]
  (let [meta #js {:host host
                  :port port
                  :path path
                  :method "POST"
                  :headers #js {:Content-Type "application/json"}}
        data (stringify
              #js {:jsonrpc "2.0"
                   :method method
                   :params #js {:type 3
                                :chaincodeID id
                                :ctorMsg #js {:function func
                                              :args #js [(.toBase64 args)]}}
                   :id "1"})
        req (.request http meta (partial response-handler cb))]

    (println "HTTP POST:" (str "http://" host ":" port) "-" data)
    (.write req data)
    (.end req)))

(defn- chaincode-post [args]
  (post (assoc args :path "/chaincode")))

(defn deploy [args]
  (chaincode-post (assoc args :method "deploy")))

(defn invoke [args]
  (chaincode-post (assoc args :method "invoke")))

(defn query [args]
  (chaincode-post (assoc args :method "query")))
