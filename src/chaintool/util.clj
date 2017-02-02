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

(ns chaintool.util
  (:require [clojure.java.io :as io]
            [slingshot.slingshot :as slingshot]))

(def app-version (System/getProperty "chaintool.version"))

(defn truncate-file [filename content]

  ;; ensure the path exists
  (io/make-parents filename)

  ;; and blast it out to the filesystem
  (spit filename content :truncate true))

;; throws an exception that should unwind us all the way to the core/main
;; function and exit cleanly with an error message rather than a stacktrace, etc
(defn abort [retval msg]
  (slingshot/throw+ {:type :chaintoolabort :retval retval :msg msg}))

(defn safe-slurp [file]
  (if (.exists file)
    (slurp file)
    (abort -1 (str (.getCanonicalPath file) " not found"))))
