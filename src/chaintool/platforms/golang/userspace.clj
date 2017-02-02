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

(ns chaintool.platforms.golang.userspace
  (:require [chaintool.car.ls :refer :all]
            [chaintool.car.write :as car]
            [chaintool.config.util :as config]
            [chaintool.platforms.api :as platforms.api]
            [chaintool.platforms.golang.core :refer :all]
            [chaintool.util :as util]
            [clojure.java.io :as io]
            [clojure.tools.file-utils :as fileutils])
  (:refer-clojure :exclude [compile]))

;;-----------------------------------------------------------------
;; Supports "org.hyperledger.chaincode.golang" platform, a golang
;; based environment for standard chaincode applications.
;;-----------------------------------------------------------------
(deftype GolangUserspacePlatform []
  platforms.api/Platform

  ;;-----------------------------------------------------------------
  ;; env - Emits the GOPATH used for building golang chaincode
  ;;-----------------------------------------------------------------
  (env [_ {:keys [path]}]
    (println (str "GOPATH=" (buildgopath path))))

  ;;-----------------------------------------------------------------
  ;; build - generates all golang platform artifacts within the
  ;; default location in the build area
  ;;-----------------------------------------------------------------
  (build [_ {:keys [path config output]}]
    (let [builddir (io/file path "build")]

      ;; run our code generator
      (generate {:base "hyperledger"
                 :ipath (io/file path "src/interfaces")
                 :opath (io/file builddir "src")
                 :config config})

      ;; install go dependencies
      (go-cmd path {} "get" "-d" "-v" "chaincode")

      ;; build the actual code
      (let [gobin (io/file builddir "bin")]
        (io/make-parents (io/file gobin ".dummy"))
        (io/make-parents output)
        (go-cmd path {"GOBIN" (.getCanonicalPath gobin)} "build" "-o" (.getCanonicalPath output) "chaincode"))

      (println "Compilation complete")))

  ;;-----------------------------------------------------------------
  ;; clean - cleans up any artifacts from a previous build, if any
  ;;-----------------------------------------------------------------
  (clean [_ {:keys [path]}]
    (fileutils/recursive-delete (io/file path "build")))

  ;;-----------------------------------------------------------------
  ;; package - writes the chaincode package to the filesystem
  ;;-----------------------------------------------------------------
  (package [_ {:keys [path config outputfile compressiontype]}]
    (let [filespec ["src" config/configname]]

      ;; emit header information after we know the file write was successful
      (println "Writing CAR to:" (.getCanonicalPath outputfile))
      (println "Using path" path (str filespec))

      ;; generate the actual file
      (car/write path filespec compressiontype outputfile)

      ;; re-use the ls function to display the contents
      (ls outputfile))))

(defn factory [version]
  (if (= version 1)
    (GolangUserspacePlatform.)
    (util/abort -1 (str "Version " version " not supported"))))
