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

(ns chaintool.subcommands.proto
  (:require [chaintool.build.interface :as intf]
            [chaintool.protobuf.generate :as pb]
            [chaintool.util :as util]
            [clojure.java.io :as io]
            [me.raynes.fs :as fs]))

(defn getoutputfile [options input name]
  (if-let [output (:output options)]
    (io/file output)
    (io/file (str name ".proto"))))

(defn run [options args]
  (let [input (io/file (first args))
        name (fs/base-name input true)
        output (getoutputfile options input name)
        intf (intf/compileintf {:path (.getCanonicalPath input) :data (util/safe-slurp input)})]
    (pb/to-file output name [name intf])))
