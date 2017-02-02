;; Copyright London Stock Exchange Group 2016 All Rights Reserved.
;;
;; Licensed under the Apache License, Version 2.0 (the "License");
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at
;;
;;                  http://www.apache.org/licenses/LICENSE-2.0
;;
;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and
;; limitations under the License.

(ns chaintool.protobuf.generate
  (:require [chaintool.build.interface :as intf]
            [chaintool.util :as util]
            [clojure.zip :as zip])
  (:import (java.util ArrayList)
           (org.stringtemplate.v4 STGroupFile))
  (:refer-clojure :exclude [compile]))

;; types to map to java objects that string template expects.
;;

(deftype Field       [^String modifier ^String type ^String name ^String index])
(deftype Definition  [^String type ^String name ^ArrayList entries])
(deftype Entry       [^Definition message ^Definition enum ^Field field])
(deftype Function    [^String key ^String rettype ^String name ^String param])

(defn- typeconvert [[_ name]] name)

(defn- find-toplevel-definitions [ast]
  (loop [loc (->> ast zip/down zip/right) defs []]
    (cond

      (nil? loc)
      defs

      :else
      (let [type (->> loc zip/down zip/node)]
        (recur (zip/right loc)
               (if (or (= type :message) (= type :enum))
                 (conj defs loc)
                 defs))))))

(def function-class
  {:transactions "txn"
   :queries "query"})

(defn- getallfunctions [ast]
  (flatten (for [[type functions] (intf/getallfunctions ast)]
             (for [[_ func] functions]
               (assoc func :type (function-class type))))))

;;-----------------------------------------------------------------
;; buildX - build our ST friendly objects from the AST
;;-----------------------------------------------------------------
(defn- build-msgfield [ast]
  (let [field (zip/down ast)
        {:keys [modifier type fieldName index]} (intf/getattrs field)]
    (->Entry nil nil (->Field modifier (typeconvert type) fieldName index))))

(defn- build-enumfield [ast]
  (let [field (zip/down ast)
        name (->> field zip/right zip/node)
        index (->> field zip/right zip/right zip/node)]
    (->Field nil nil name index)))

(declare build-message)
(declare build-enum)

(defn- build-subentry [ast]
  (let [type (->> ast zip/down zip/node)]
    (case type
      :message (build-message ast)
      :enum (build-enum ast)
      :field (build-msgfield ast))))

(defn- build-message [ast]
  (let [elem (zip/down ast)
        name (->> elem zip/right zip/node)
        first (->> elem zip/right zip/right)
        entries (loop [loc first entries {} index 0]
                  (cond

                    (nil? loc)
                    entries

                    :else
                    (recur (zip/right loc) (assoc entries index (build-subentry loc)) (inc index))))]
    (->Entry (->Definition "message" name entries) nil nil)))

(defn- build-enum [ast]
  (let [elem (zip/down ast)
        name (->> elem zip/right zip/node)
        first (->> elem zip/right zip/right)
        entries (loop [loc first entries {} index 0]
                  (cond

                    (nil? loc)
                    entries

                    :else
                    (recur (zip/right loc) (assoc entries index (build-enumfield loc)) (inc index))))]
    (->Entry nil (->Definition "enum" name entries) nil)))

(defn- build-toplevel-entry [ast]
  (let [type (->> ast zip/down zip/node)]
    (case type
      :message (build-message ast)
      :enum (build-enum ast))))

(defn- build-toplevel-entries [ast]
  (->> ast
       find-toplevel-definitions
       (mapv build-toplevel-entry)
       (interleave (range))
       (partition 2)
       (mapv vec)
       (into {})))

(defn- buildfunction [name {:keys [rettype functionName param index type] :as ast}]
  (let [key (str name "/" type "/" index)]
    (->Function key rettype functionName param)))

(defn- buildfunctions [name ast]
  (let [funcs (map #(buildfunction name %) (getallfunctions ast))]
    (into {} (map #(vector (.key %) %) funcs))))

;;-----------------------------------------------------------------
;; to-string - compiles the interface into a protobuf
;; specification in a string, suitable for writing to a file or
;; passing to protoc
;;-----------------------------------------------------------------
(defn to-string [package [name ast]]
  (let [definitions (build-toplevel-entries ast)
        functions (buildfunctions name ast)
        stg  (STGroupFile. "generators/proto.stg")
        template (.getInstanceOf stg "protobuf")]

    (.add template "package" (if (nil? package) name package))
    (.add template "definitions" definitions)
    (.add template "functions" functions)
    (.render template)))

;;-----------------------------------------------------------------
;; to-file - generates a protobuf specification and writes
;; it to a file
;;-----------------------------------------------------------------
(defn to-file [filename package interface]
  (util/truncate-file filename (to-string package interface)))
