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

(ns chaintool.subcommands.unpack
  (:require [chaintool.config.util :as config]
            [chaintool.util :as util]
            [chaintool.car.read :as car.read]
            [chaintool.car.unpack :as car.unpack]
            [clojure.java.io :as io]))

(defn getoutputdir [options config]
  (if-let [dir (:directory options)]
    (io/file dir)
    (io/file "./" (str (config/compositename config)))))

(defn run [options args]
  (let [file (io/file (first args))
        {:keys [index config]} (with-open [is (io/input-stream file)] (car.read/read is))
        outputdir (getoutputdir options config)]

    (when (.exists outputdir)
      (util/abort -1 (str "output directory " (.getCanonicalPath outputdir) " exists")))

    (println "Unpacking CAR to:" (.getCanonicalPath outputdir))
    (println)
    (car.unpack/unpack index outputdir :true)))
