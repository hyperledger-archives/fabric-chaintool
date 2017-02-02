(ns example02.hlc.core
  (:require [cljs.nodejs :as nodejs]
            [promesa.core :as p :include-macros true]))

(def hlc (nodejs/require "hlc"))

(defn new-chain [name]
  (p/do* (.newChain hlc name)))

(defn new-file-kv-store [path]
  (p/do* (.newFileKeyValStore hlc path)))

(defn set-kv-store [chain store]
  (p/do* (.setKeyValStore chain store)))

(defn set-membersrvc-url [chain url]
  (p/do* (.setMemberServicesUrl chain url)))

(defn add-peer [chain url]
  (p/do* (.addPeer chain url)))

(defn get-member [chain username]
  (p/promise
   (fn [resolve reject]
     (.getMember chain username
                 (fn [err member]
                   (if err
                     (reject err)
                     (resolve member)))))))

(defn register-and-enroll [chain request]
  (p/promise
   (fn [resolve reject]
     (.registerAndEnroll chain request
                         (fn [err user]
                           (if err
                             (reject err)
                             (resolve user)))))))
