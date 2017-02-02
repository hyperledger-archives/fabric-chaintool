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

(ns chaintool.subcommands.env
  (:require [chaintool.config.util :as config.util]
            [chaintool.build.core :as build.core]))

(defn run [options args]
  (let [[path config] (config.util/load-from-options options)]
    (build.core/env {:path path :config config})))
