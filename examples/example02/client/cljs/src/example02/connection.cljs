;;-----------------------------------------------------------------------------
;; Copyright 2017 Greg Haskins
;;
;; SPDX-License-Identifier: Apache-2.0
;;-----------------------------------------------------------------------------
(ns example02.connection
  (:require [fabric-sdk.core :as fabric]
            [fabric-sdk.channel :as fabric.channel]
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

(defn- connect-orderer [client channel config]
  (let [{:keys [ca hostname url]} (:orderer config)
        orderer (fabric/new-orderer client
                                    url
                                    #js {:pem ca
                                         :ssl-target-name-override hostname})]

    (fabric.channel/add-orderer channel orderer)

    orderer))

(defn- connect-peer [client channel config peercfg]
  (let [ca (-> config :ca :certificate)
        {:keys [api hostname]} peercfg
        peer (fabric/new-peer client
                              api
                              #js {:pem ca
                                   :ssl-target-name-override hostname
                                   :request-timeout 120000})]

    (fabric.channel/add-peer channel peer)

    peer))

(defn- connect-eventhub [client channel config]
  (let [ca (-> config :ca :certificate)
        {:keys [events hostname]} (-> config :peers first)
        eventhub (fabric/new-eventhub client)]

    (fabric.eventhub/set-peer-addr eventhub
                                   events
                                   #js {:pem ca
                                        :ssl-target-name-override hostname})
    (fabric.eventhub/connect! eventhub)

    eventhub))

(defn connect! [{:keys [config id channelId] :as options}]

  (let [client (fabric/new-client)
        identity (:identity config)]

    (-> (set-state-store client ".hfc-kvstore")
        (p/then #(create-user client identity))
        (p/then (fn [user]

                  (let [channel (fabric.channel/new client channelId)
                        orderer (connect-orderer client channel config)
                        peers (->> config
                                   :peers
                                   (map #(connect-peer client channel config %)))
                        eventhub (connect-eventhub client channel config)]

                    (-> (fabric.channel/initialize channel)
                        (p/then (fn []
                                  {:client client
                                   :channel channel
                                   :orderer orderer
                                   :peers peers
                                   :eventhub eventhub
                                   :user user})))))))))

(defn disconnect! [{:keys [eventhub]}]
  (fabric.eventhub/disconnect! eventhub))

