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

(ns chaintool.subcommands.inspect
  (:require [chaintool.inspect.core :as inspect]
            [clojure.java.io :as io]))

(defn getoutputdir [options]
  (if-let [path (:interfaces options)]
    (io/file path)
    (io/file ".")))

(defn run [options args]
  (let [output (getoutputdir options)]
    (inspect/run (assoc options :output output))))
