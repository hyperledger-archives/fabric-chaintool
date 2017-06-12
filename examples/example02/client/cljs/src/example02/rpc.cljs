;;-----------------------------------------------------------------------------
;; Copyright 2017 Greg Haskins
;;
;; SPDX-License-Identifier: Apache-2.0
;;-----------------------------------------------------------------------------
(ns example02.rpc
  (:require [cljs.nodejs :as nodejs]
            [fabric-sdk.core :as fabric]
            [fabric-sdk.channel :as fabric.channel]
            [fabric-sdk.eventhub :as fabric.eventhub]
            [promesa.core :as p :include-macros true]))

(defn- create-base-request [{:keys [client peers channelId chaincodeId]}]
  (let [txid (fabric/new-txnid client)]

    {:chaincodeType "car"
     :targets peers
     :chainId channelId
     :chaincodeId chaincodeId
     :txId txid}))

(defn- create-request [{:keys [func args] :as options}]
  (-> (create-base-request options)
      (assoc :fcn func :args #js [(.toBuffer args)])))

(defn- decodejs [js]
  (js->clj js :keywordize-keys true))

(defn- verify-results [[results proposal header :as response]]
  (doseq [result results]
    (let [retval (-> result decodejs :response :status)]
      (when-not (= retval 200)
        (throw result))))

  response)

(defn- register-tx-event [eventhub txid]
  (p/promise
   (fn [resolve reject]
     (fabric.eventhub/register-tx-event eventhub txid resolve))))

(defn- send-transaction [{:keys [channel response]}]
  (let [[results proposal header] response]
    (fabric.channel/send-transaction channel
                                   #js {:proposalResponses results
                                        :proposal proposal
                                        :header header})))

(defn- forward-endorsements [{:keys [eventhub request tmo] :as options}]
  (let [txid (-> request decodejs :txId)]
    (-> (p/all [(register-tx-event eventhub txid)
                (send-transaction options)])
        (p/timeout tmo))))

(defn install [{:keys [client channel path version] :as options}]
  (let [request (-> (create-base-request options)
                    (assoc :chaincodeVersion version
                           :chaincodePath path)
                    clj->js)]

    (when (not path)
      (fabric.channel/set-dev-mode channel true))

    (-> (fabric/install-chaincode client request)
        (p/then verify-results))))

(defn instantiate [{:keys [client channel version] :as options}]
  (let [request (-> (create-request options)
                    (assoc :chaincodeVersion version)
                    clj->js)]

    (-> (fabric.channel/send-instantiate-proposal channel request)
        (p/then verify-results)
        (p/then #(forward-endorsements (assoc options
                                              :request request
                                              :response %
                                              :tmo 120000))))))

(defn transaction [{:keys [client channel] :as options}]
  (let [request (-> options create-request clj->js)]

    (-> (fabric.channel/send-transaction-proposal channel request)
        (p/then verify-results)
        (p/then #(forward-endorsements (assoc options
                                              :request request
                                              :response %
                                              :tmo 30000))))))

(defn query [{:keys [client channel] :as options}]
  (let [request (-> options create-request clj->js)]
    (fabric.channel/query-by-chaincode channel request)))
