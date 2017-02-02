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

(ns chaintool.car.ls
  (:require [chaintool.car.read :as car]
            [clojure.java.io :as io]
            [doric.core :as doric]
            [pandect.algo.sha3-512 :refer :all]))

(defn platform-version [config]
  (str (->> config :Platform :Name) " version " (->> config :Platform :Version)))

(defn ls [file]
  (let [{:keys [payload config]} (with-open [is (io/input-stream file)] (car/read is))
        entries (:entries payload)]

    (println (doric/table [{:name :size} {:name :sha1 :title "SHA1"} {:name :path}] entries))
    (println "Platform:           " (platform-version config))
    (println "Digital Signature:   none")
    (println "Raw Data Size:      " (->> entries (map :size) (reduce +)) "bytes")
    (println "Archive Size:       " (.length file) "bytes")
    (println "Compression Alg:    " (get-in payload [:compression :description]))
    (println "Chaincode SHA3:     " (sha3-512 file))))
