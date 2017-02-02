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

(ns chaintool.car.read
  (:require [flatland.protobuf.core :as fl]
            [chaintool.util :as util]
            [chaintool.car.types :refer :all]
            [chaintool.codecs :as codecs]
            [chaintool.config.parser :as config.parser]
            [chaintool.config.util :as config.util]
            [pandect.algo.sha1 :refer :all])
  (:refer-clojure :exclude [read]))

;;--------------------------------------------------------------------------------------
;; read-protobuf - reads a single protobuf message from a delimited stream
;;
;; our protobuf file consists of two primary message types, delimited by a size.
;; (fl/protobuf-seq) is designed to return an infinite lazy sequence of one type
;; so we need to feed this through (first) to extract only one.
;;--------------------------------------------------------------------------------------
(defn read-protobuf [t is] (->> is (fl/protobuf-seq t) first))

;;--------------------------------------------------------------------------------------
;; read-XX - read one instance of a specific type of protobuf message [Header|Archive]
;;--------------------------------------------------------------------------------------
(defn read-header [is] (read-protobuf Header is))
(defn read-archive [is] (read-protobuf Archive is))

;;--------------------------------------------------------------------------------------
;; make-input-stream - factory function for creating an input-stream for a specific entry
;;
;; We install the necessary decompressor such that the output of this stream represents
;; raw, uncompressed original data
;;--------------------------------------------------------------------------------------
(defn make-input-stream [type entry]
  (let [is (->> entry :data .newInput)]
    (codecs/decompressor type is)))

;;--------------------------------------------------------------------------------------
;; import-header - imports and validates a Header object from the input stream
;;
;; evaluates to the parsed header if successful, or throws an exception otherwise
;;--------------------------------------------------------------------------------------
(defn import-header [is]
  (if-let [header (read-header is)]
    (let [compat (select-keys header [:magic :version])]
      (if (= compat CompatVersion)
        (:features header)
        (util/abort -1 (str "Incompatible header detected (expected: " CompatVersion " got: " compat ")"))))
    (util/abort -1 (str "Failed to read archive header"))))

;;--------------------------------------------------------------------------------------
;; import-archive - imports an Archive object from the input stream
;;--------------------------------------------------------------------------------------
(defn import-archive [is]
  (read-archive is)) ;; FIXME - check digitial signature

;;--------------------------------------------------------------------------------------
;; import-entry - validates an entry object
;;
;; returns the proper input-stream-factory when sucessful, or throws an exception otherwise
;;--------------------------------------------------------------------------------------
(defn import-entry [compression entry]
  (let [type (:description compression)
        factory #(make-input-stream type entry)]

    ;; verify the SHA1
    (with-open [is (factory)]
      (let [sha (sha1 is)]
        (when (not= sha (:sha1 entry))
          (util/abort -1 (str (:path entry) ": hash verification failure (expected: " (:sha1 entry) ", got: " sha ")")))))

    ;; and inject our stream factory
    {:entry entry :input-stream-factory factory}))

;;--------------------------------------------------------------------------------------
;; import-payload - imports a Payload object from Archive::Payload field
;;
;; We separate Archive from Payload to delineate signature boundaries.  Everything within
;; Payload is expected to be optionally digitally signed (and thus verified upon import)
;;--------------------------------------------------------------------------------------
(defn import-payload [archive] (->> archive :payload .newInput (fl/protobuf-load-stream Payload)))

;;--------------------------------------------------------------------------------------
;; synth-index - synthesize an index of entries, keyed by :path
;;
;; Takes a payload object and constructs a map of entries, keyed by their path.  Each
;; entry is fully verified and processed (such as attaching an input-stream filter)
;;--------------------------------------------------------------------------------------
(defn synth-index [payload]
  (let [compression (:compression payload)]
    (->> (:entries payload) (map #(vector (:path %) (import-entry compression %))) (into {}))))

;;--------------------------------------------------------------------------------------
;; entry-stream - instantiate an input-stream from the factory
;;
;; This allows a caller to obtain a simple input-stream interface to our entries where
;; all the details such as compression are already factored in.  Therefore, this stream
;; is suitable to any number of tasks such as interpreting results, verifying contents,
;; or writing data to files
;;--------------------------------------------------------------------------------------
(defn entry-stream [entry]
  (let [factory (:input-stream-factory entry)]
    (factory)))

(defn read [is]
  (let [features (import-header is)
        archive (import-archive is)
        payload (import-payload archive)
        index (synth-index payload)]

    (with-open [config-stream (->> config.util/configname index entry-stream)]
      (let [config (->> config-stream slurp config.parser/from-string)]
        {:features features :payload payload :index index :config config}))))
