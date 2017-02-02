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

(ns chaintool.config.util
  (:require [chaintool.util :as util]
            [clojure.java.io :as io]
            [clojure.zip :as zip]
            [chaintool.config.parser :as config.parser])
  (:refer-clojure :exclude [load find]))

(def configname "chaincode.yaml")

(defn load [path]
  (let [file (io/file path configname)]
    (cond

      (not (.isFile file))
      (util/abort -1 (str (.getCanonicalPath file) " not found"))

      :else
      (config.parser/from-file file))))

(defn load-from-options [options]
  (let [path (:path options)
        config (load path)]
    [path config]))

(defn compositename [config]
  (let [name (:Name config)
        version (:Version config)]
    (str name "-" version)))
