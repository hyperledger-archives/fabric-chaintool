;;-----------------------------------------------------------------------------
;; Copyright 2017 Greg Haskins
;;
;; SPDX-License-Identifier: Apache-2.0
;;-----------------------------------------------------------------------------
(ns example02.connection
  (:require [fabric-sdk.core :as fabric]
            [fabric-sdk.chain :as fabric.chain]
            [fabric-sdk.eventhub :as fabric.eventhub]
            [fabric-sdk.user :as fabric.user]
            [promesa.core :as p :include-macros true]))

(defn- set-state-store [client path]
  (-> (fabric/new-default-kv-store path)
      (p/then #(fabric/set-state-store client %))))

(defn- create-user [client identity]
  (let [config #js {:username (:principal identity)
                    :mspid (:mspid identity)
                    :cryptoContent #js {:privateKeyPEM (:privatekey identity)
                                        :signedCertPEM (:certificate identity)}}]

    (fabric/create-user client config)))

(defn- connect-orderer [client chain config]
  (let [{:keys [ca hostname url]} (:orderer config)
        orderer (fabric/new-orderer client
                                    url
                                    #js {:pem ca
                                         :ssl-target-name-override hostname})]

    (fabric.chain/add-orderer chain orderer)

    orderer))

(defn- connect-peer [client chain config peercfg]
  (let [ca (-> config :ca :certificate)
        {:keys [api hostname]} peercfg
        peer (fabric/new-peer client
                              api
                              #js {:pem ca
                                   :ssl-target-name-override hostname
                                   :request-timeout 120000})]

    (fabric.chain/add-peer chain peer)

    peer))

(defn- connect-eventhub [client chain config]
  (let [ca (-> config :ca :certificate)
        {:keys [events hostname]} (-> config :peers first)
        eventhub (fabric.eventhub/new client)]

    (fabric.eventhub/set-peer-addr eventhub
                                   events
                                   #js {:pem ca
                                        :ssl-target-name-override hostname})
    (fabric.eventhub/connect! eventhub)

    eventhub))

(defn connect! [{:keys [config id channel] :as options}]

  (let [client (fabric/new-client)
        identity (:identity config)]

    (-> (set-state-store client ".hfc-kvstore")
        (p/then #(create-user client identity))
        (p/then (fn [user]

                  (let [chain (fabric.chain/new client channel)
                        orderer (connect-orderer client chain config)
                        peers (->> config
                                   :peers
                                   (map #(connect-peer client chain config %)))
                        eventhub (connect-eventhub client chain config)]

                    (-> (fabric.chain/initialize chain)
                        (p/then (fn []
                                  {:client client
                                   :chain chain
                                   :orderer orderer
                                   :peers peers
                                   :eventhub eventhub
                                   :user user})))))))))

(defn disconnect! [{:keys [eventhub]}]
  (fabric.eventhub/disconnect! eventhub))

