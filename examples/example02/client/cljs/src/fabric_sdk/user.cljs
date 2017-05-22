;;-----------------------------------------------------------------------------
;; Copyright 2017 Greg Haskins
;;
;; SPDX-License-Identifier: Apache-2.0
;;-----------------------------------------------------------------------------
(ns fabric-sdk.user
  (:require-macros [fabric-sdk.macros :as m])
  (:require [cljs.nodejs :as nodejs]
            [promesa.core :as p :include-macros true]))

(def user (nodejs/require "fabric-client/lib/User.js"))

(defn new [username client]
  (new user username client))

(defn enrolled? [user]
  (and user (.isEnrolled user)))

(defn set-enrollment [user enrollment mspid]
  (m/pwrap (.setEnrollment user enrollment.key enrollment.certificate mspid)))
