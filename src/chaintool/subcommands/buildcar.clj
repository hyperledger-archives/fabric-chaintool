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
(ns chaintool.subcommands.buildcar
  (:require [clojure.java.io :as io]
            [me.raynes.fs :as fs]
            [clojure.tools.file-utils :as fileutils]
            [chaintool.util :as util]
            [chaintool.config.util :as config.util]
            [chaintool.car.read :as car.read]
            [chaintool.car.unpack :as car.unpack]
            [chaintool.build.core :as build.core]))

(defn getoutput [options]
  (if-let [output (:output options)]
    (io/file output)
    (util/abort -1 "Missing -o output (see -h for details)")))

(defn run [options args]
  (let [output (getoutput options)
        file (io/file (first args))
        {:keys [index config]} (with-open [is (io/input-stream file)] (car.read/read is))
        workingdir (fs/temp-dir "buildcar-")]

    (car.unpack/unpack index workingdir :false)
    (let [config (config.util/load workingdir)]
      (println "Building CAR" (.getCanonicalPath file))
      (build.core/compile {:path workingdir :config config :output output})
      (fileutils/recursive-delete (io/file workingdir)))))
