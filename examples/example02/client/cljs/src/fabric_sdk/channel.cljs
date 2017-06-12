;;-----------------------------------------------------------------------------
;; Copyright 2017 Greg Haskins
;;
;; SPDX-License-Identifier: Apache-2.0
;;-----------------------------------------------------------------------------
(ns fabric-sdk.channel
  (:require-macros [fabric-sdk.macros :as m])
  (:require [promesa.core :as p :include-macros true]))

(defn new [client name]
  (.newChannel client name))

(defn initialize [channel]
  (m/pwrap (.initialize channel)))

(defn add-peer [channel instance]
  (.addPeer channel instance))

(defn add-orderer [channel instance]
  (.addOrderer channel instance))

(defn set-dev-mode [channel enabled]
  (.setDevMode channel enabled))

(defn send-instantiate-proposal [channel request]
  (m/pwrap (.sendInstantiateProposal channel request)))

(defn send-transaction-proposal [channel request]
  (m/pwrap (.sendTransactionProposal channel request)))

(defn send-transaction [channel request]
  (m/pwrap (.sendTransaction channel request)))

(defn query-by-chaincode [channel request]
  (m/pwrap (.queryByChaincode channel request)))
