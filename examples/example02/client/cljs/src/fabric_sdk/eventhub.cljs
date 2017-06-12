;;-----------------------------------------------------------------------------
;; Copyright 2017 Greg Haskins
;;
;; SPDX-License-Identifier: Apache-2.0
;;-----------------------------------------------------------------------------
(ns fabric-sdk.eventhub
  (:require-macros [fabric-sdk.macros :as m])
  (:require [cljs.nodejs :as nodejs]
            [promesa.core :as p :include-macros true]))

(defn set-peer-addr [instance addr opts]
  (.setPeerAddr instance addr opts))

(defn connect! [instance]
  (.connect instance))

(defn disconnect! [instance]
  (.disconnect instance))

(defn register-tx-event [instance txid cb]
  (.registerTxEvent instance (.getTransactionID txid) cb))

(defn unregister-tx-event [instance txid]
  (.unregisterTxEvent instance txid))
