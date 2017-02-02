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
(ns chaintool.subcommands.package
  (:require [chaintool.config.util :as config]
            [chaintool.platforms.core :as platforms.core]
            [chaintool.platforms.api :as platforms.api]
            [clojure.java.io :as io]
            [clojure.tools.cli :refer [parse-opts]]))

(defn getoutputfile [options path config]
  (if-let [output (:output options)]
    (io/file output)
    (io/file path "build" (str (config/compositename config) ".car"))))

(defn run [options args]
  (let [[path config] (config/load-from-options options)
        compressiontype (:compress options)
        outputfile (getoutputfile options path config)
        platform (platforms.core/find config)]

    (platforms.api/package platform {:path path
                                     :config config
                                     :compressiontype compressiontype
                                     :outputfile outputfile})))
