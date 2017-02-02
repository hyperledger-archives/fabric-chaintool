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

(ns chaintool.protobuf.test_generate
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [clojure.zip :as zip]
            [instaparse.core :as insta]
            [chaintool.ast :as ast]
            [chaintool.protobuf.generate :as pb]
            [slingshot.slingshot :as slingshot])
  (:refer-clojure :exclude [compile]))

(def skipper (insta/parser (io/resource "parsers/proto/skip.bnf")))
(def grammar (insta/parser (io/resource "parsers/proto/grammar.bnf") :auto-whitespace skipper))

(defn- parse [intf]
  (let [result (insta/add-line-and-column-info-to-metadata intf (grammar intf))]
    (if (insta/failure? result)
      (let [{:keys [line column text]} result]
        (str "could not parse \"" text "\": line=" line " column=" column))
      (zip/vector-zip result))))

(defn- find
  "Finds an arbitrary item in the tree based on (pred)"
  [ast pred]
  (loop [loc ast]
    (cond

      (or (nil? loc) (zip/end? loc))
      nil

      (pred loc)
      loc

      :else
      (recur (zip/next loc)))))

(defn- tree-depth
  "Counts the zipper depth of the node represented by ast"
  [ast]
  (loop [loc ast depth 0]
    (cond

      (or (nil? loc) (zip/end? loc))
      depth

      :else
      (recur (zip/up loc) (inc depth)))))

(defn- round-about
  "takes an AST, exports it to protobuf, and then reparses the protobuf back to a new AST"
  [ast]
  (let [pb (->> ["fictional.interface" ast] (pb/to-string "fictional.package"))]
    (parse pb)))

(def nested-input
  (zip/vector-zip
   [:interface
    [:message "NestedMessage"
     [:message "Entry"
      [:field [:type [:scalar "string"]] [:fieldName "key"] [:index "1"]]
      [:field [:type [:scalar "int32"]] [:fieldName "value"] [:index "2"]]]
     [:field [:modifier "repeated"] [:type [:userType "Entry"]] [:fieldName "entries"] [:index "1"]]
     [:message "Level1"
      [:message "Level2"
       [:message "Level3"
        [:message "Level4"]]]]]]))

(deftest nested-messages
  (let [result (round-about nested-input)
        level4 (find result (fn [loc] (= (zip/node loc) "Level4")))]
    (is level4)
    (is (= (tree-depth level4) 7))))

(def enum-input
  (zip/vector-zip
   [:interface [:enum "MyEnum" [:enumField "ZERO" 0] [:enumField "ONE" 1] [:enumField "TWO" 2]]]))

(deftest enum-test
  (let [result (round-about enum-input)]
    (is (= (->> result zip/down zip/node) :proto))
    (is (= (->> result (ast/find :syntax) zip/down zip/right zip/node) "proto3"))
    (is (= (->> result (ast/find :enum) zip/down zip/right zip/right zip/node) [:enumField "ZERO" "0"]))))

(def parameterless-function
  (zip/vector-zip
   [:interface [:functions [:function [:rettype "string"] [:functionName "Parameterless"] [:index "1"]]]]))

(deftest parameterless-test
  (let [result (round-about parameterless-function)]
    (some? result)))
