;;-----------------------------------------------------------------------------
;; Copyright 2017 Greg Haskins
;;
;; SPDX-License-Identifier: Apache-2.0
;;-----------------------------------------------------------------------------
(ns example02.rpc
  (:require [cljs.nodejs :as nodejs]
            [fabric-sdk.core :as fabric]
            [fabric-sdk.chain :as fabric.chain]
            [fabric-sdk.eventhub :as fabric.eventhub]
            [promesa.core :as p :include-macros true]))

(defn- create-base-request [{:keys [chain peers channel id user]}]
  (let [nonce (fabric/get-nonce)
        txid (fabric/build-txnid nonce user)]

    {:chaincodeType "car"
     :targets peers
     :chainId channel
     :chaincodeId id
     :txId txid
     :nonce nonce}))

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

(defn- send-transaction [{:keys [chain response]}]
  (let [[results proposal header] response]
    (fabric.chain/send-transaction chain
                                   #js {:proposalResponses results
                                        :proposal proposal
                                        :header header})))

(defn- forward-endorsements [{:keys [eventhub request tmo] :as options}]
  (let [txid (-> request decodejs :txId)]
    (-> (p/all [(register-tx-event eventhub txid)
                (send-transaction options)])
        (p/timeout tmo))))

(defn install [{:keys [client chain path version] :as options}]
  (let [request (-> (create-base-request options)
                    (assoc :chaincodeVersion version
                           :chaincodePath path)
                    clj->js)]

    (when (not path)
      (fabric.chain/set-dev-mode chain true))

    (-> (fabric/install-chaincode client request)
        (p/then verify-results))))

(defn instantiate [{:keys [client chain version] :as options}]
  (let [request (-> (create-request options)
                    (assoc :chaincodeVersion version)
                    clj->js)]

    (-> (fabric.chain/send-instantiate-proposal chain request)
        (p/then verify-results)
        (p/then #(forward-endorsements (assoc options
                                              :request request
                                              :response %
                                              :tmo 120000))))))

(defn transaction [{:keys [client chain] :as options}]
  (let [request (-> options create-request clj->js)]

    (-> (fabric.chain/send-transaction-proposal chain request)
        (p/then verify-results)
        (p/then #(forward-endorsements (assoc options
                                              :request request
                                              :response %
                                              :tmo 30000))))))

(defn query [{:keys [client chain] :as options}]
  (let [request (-> options create-request clj->js)]
    (fabric.chain/query-by-chaincode chain request)))
