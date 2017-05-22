;;-----------------------------------------------------------------------------
;; Copyright 2017 Greg Haskins
;;
;; SPDX-License-Identifier: Apache-2.0
;;-----------------------------------------------------------------------------
(ns fabric-sdk.chain
  (:require-macros [fabric-sdk.macros :as m])
  (:require [promesa.core :as p :include-macros true]))

(defn new [client name]
  (.newChain client name))

(defn initialize [chain]
  (m/pwrap (.initialize chain)))

(defn add-peer [chain instance]
  (.addPeer chain instance))

(defn add-orderer [chain instance]
  (.addOrderer chain instance))

(defn set-dev-mode [chain enabled]
  (.setDevMode chain enabled))

(defn send-instantiate-proposal [chain request]
  (m/pwrap (.sendInstantiateProposal chain request)))

(defn send-transaction-proposal [chain request]
  (m/pwrap (.sendTransactionProposal chain request)))

(defn send-transaction [chain request]
  (m/pwrap (.sendTransaction chain request)))

(defn query-by-chaincode [chain request]
  (m/pwrap (.queryByChaincode chain request)))
