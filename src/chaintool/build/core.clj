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
(ns chaintool.build.core
  (:require [chaintool.platforms.core :as platforms.core]
            [chaintool.platforms.api :as platforms.api])
  (:refer-clojure :exclude [compile]))


(defn- run [fcn {:keys [config] :as params}]
  (when-let [platform (platforms.core/find config)]
    (fcn platform params)))

;; generate platform output (shim, protobufs, etc)
(defn compile [params]
  (run platforms.api/build params))

;; display environment variables used for build
(defn env [params]
  (run platforms.api/env params))
