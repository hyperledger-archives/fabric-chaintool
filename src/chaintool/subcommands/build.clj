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
(ns chaintool.subcommands.build
  (:require [clojure.java.io :as io]
            [chaintool.config.util :as config.util]
            [chaintool.build.core :as build.core]))

(defn getoutput [options path config]
  (if-let [output (:output options)]
    (io/file output)
    (io/file path "build/bin" (config.util/compositename config))))

(defn run [options args]
  (let [[path config] (config.util/load-from-options options)
        output (getoutput options path config)]
    (println "Build using configuration for " path)
    (build.core/compile {:path path :config config :output output})))
