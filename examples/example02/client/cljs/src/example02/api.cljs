;;-----------------------------------------------------------------------------
;; Copyright 2017 Greg Haskins
;;
;; SPDX-License-Identifier: Apache-2.0
;;-----------------------------------------------------------------------------
(ns example02.api
  (:require [example02.protobuf :as pb]
            [example02.rpc :as rpc]
            [promesa.core :as p :include-macros true]))

(def intf-name "org.hyperledger.chaincode.example02")

(def all-interfaces ["appinit" intf-name])

(defn init [dir]
  (pb/init dir all-interfaces))

(defn install [context]
  (-> (rpc/install context)
      (p/then #(println "Success!"))))

(defn instantiate [{:keys [args] :as context}]
  (let [proto (pb/get "appinit")]
    (-> context
        (assoc :func "init"
               :args (proto.Init. args))
        rpc/instantiate
        (p/then #(println "Success!")))))

(defn make-payment [{:keys [args] :as context}]
  (let [proto (pb/get intf-name)]
    (-> context
        (assoc :func "org.hyperledger.chaincode.example02/fcn/1"
               :args (proto.PaymentParams. args))
        rpc/transaction
        (p/then #(println "Success!")))))

(defn delete-account [{:keys [args] :as context}]
  (let [proto (pb/get intf-name)]
    (-> context
        (assoc :func "org.hyperledger.chaincode.example02/fcn/2"
               :args (proto.Entity. args))
        rpc/transaction
        (p/then #(println "Success!")))))

(defn check-balance [{:keys [args] :as context}]
  (let [proto (pb/get intf-name)]
    (-> context
        (assoc :func "org.hyperledger.chaincode.example02/fcn/3"
               :args (proto.Entity. args))
        rpc/query
        (p/then #(println "Success: Balance ="
                          (->> % first proto.BalanceResult.decode64 .-balance))))))
