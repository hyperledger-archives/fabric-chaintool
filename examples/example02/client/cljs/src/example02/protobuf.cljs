;;-----------------------------------------------------------------------------
;; Copyright 2017 Greg Haskins
;;
;; SPDX-License-Identifier: Apache-2.0
;;-----------------------------------------------------------------------------
(ns example02.protobuf
  (:require [cljs.nodejs :as nodejs])
  (:refer-clojure :exclude [get]))

(def path (nodejs/require "path"))
(def pb (nodejs/require "protobufjs"))

(def builder (.newBuilder pb))

(defn- load [dir name]
  (let [path (.resolve path dir (str name ".proto"))]
    (.loadProtoFile pb path builder)
    (.build builder name)))

(def protos (atom {}))

(defn get [name]
  (@protos name))

(defn init [dir interfaces]
  (let [entries (->> interfaces
                     (map #(vector % (load dir %)))
                     (into {}))]
    (swap! protos merge entries)))


