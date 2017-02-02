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

(ns chaintool.platforms.golang.system
  (:require [chaintool.platforms.api :as platforms.api]
            [chaintool.platforms.golang.core :refer :all]
            [chaintool.util :as util]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.tools.file-utils :as fileutils])
  (:refer-clojure :exclude [compile]))

(defn get-package-name [path]
  ;; FIXME: This will only work when chaintool is in CWD, need to "cd $path" first
  (string/trim-newline (go "list")))

(defn subtract-paths [fqpath relpath]
  (->> (string/replace (str fqpath) relpath "") io/file .getCanonicalPath str))

(defn get-fqp [path]
  (->> path io/file .getCanonicalPath))

(defn compute-gopath [path pkgname]
  (->> pkgname pkg-to-relpath (subtract-paths (get-fqp path))))

;;-----------------------------------------------------------------
;; Supports "org.hyperledger.chaincode.system" platform, a golang
;; based environment for system chaincode applications.
;;-----------------------------------------------------------------
(deftype GolangSystemPlatform []
  platforms.api/Platform

  ;;-----------------------------------------------------------------
  ;; env - Emits the GOPATH used for building system chaincode
  ;;-----------------------------------------------------------------
  (env [_ _])

  ;;-----------------------------------------------------------------
  ;; build - generates all golang platform artifacts within the
  ;; default location in the build area
  ;;-----------------------------------------------------------------
  (build [_ {:keys [path config output]}]
    (let [builddir "build"
          opath (io/file path builddir)
          pkgname (get-package-name path)
          gopath (compute-gopath path pkgname)]

      ;; ensure we clean up any previous runs
      (fileutils/recursive-delete opath)

      ;; run our code generator
      (generate {:base (str pkgname "/" builddir)
                 :package pkgname
                 :ipath (io/file path "interfaces")
                 :opath (io/file gopath)
                 :config config})

      (println "Compilation complete")))

  ;;-----------------------------------------------------------------
  ;; clean - cleans up any artifacts from a previous build, if any
  ;;-----------------------------------------------------------------
  (clean [_ {:keys [path]}]
    (fileutils/recursive-delete (io/file path "build")))

  ;;-----------------------------------------------------------------
  ;; package - not supported for system chaincode
  ;;-----------------------------------------------------------------
  (package [_ _]
    (util/abort -1 "unsupported platform operation: package")))

(defn factory [version]
  (if (= version 1)
    (GolangSystemPlatform.)
    (util/abort -1 (str "Version " version " not supported"))))
