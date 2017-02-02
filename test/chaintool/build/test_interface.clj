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

(ns chaintool.build.test_interface
  (:require [clojure.test :refer :all]
            [chaintool.build.interface :refer :all]
            [slingshot.slingshot :as slingshot])
  (:refer-clojure :exclude [compile]))

(def example1-cci
  "
  # This is a comment
  // So is this

  message ActiveMessage {
     // mid-message comment
     string param1 = 1; # trailing comment
     int32  param2 = 2;
     int64  param3 = 3;
  }

  //message CommentedMessage {
  //   int32 param1 = 1;
  //}
  ")

(def example1-expected-result
  [[:interface [:message "ActiveMessage" [:field [:type [:scalar "string"]] [:fieldName "param1"] [:index "1"]] [:field [:type [:scalar "int32"]] [:fieldName "param2"] [:index "2"]] [:field [:type [:scalar "int64"]] [:fieldName "param3"] [:index "3"]]]] nil])

(def example2-cci
  "
  message NestedMessage {
     message Entry {
        string key = 1;
        int32  value = 2;
     }

    repeated Entry entries = 1;
  }

  ")

(def example2-expected-result
  [[:interface [:message "NestedMessage" [:message "Entry" [:field [:type [:scalar "string"]] [:fieldName "key"] [:index "1"]] [:field [:type [:scalar "int32"]] [:fieldName "value"] [:index "2"]]] [:field [:modifier "repeated"] [:type [:userType "Entry"]] [:fieldName "entries"] [:index "1"]]]] nil])

(deftest test-parser-output
  (is (= example1-expected-result (parse example1-cci)))
  (is (= example2-expected-result (parse example2-cci))))

(def example-undefined-type-cci
  "
  message BadMessage {
     message Entry {
        string key = 1;
        UnknownType value = 2;
     }

    repeated Entry entries = 1;
  }

  ")

(deftest test-parser-validation
  (let [intf (parse example-undefined-type-cci)]
    (is (some? (verify-intf intf)))))

(def example-type-resolution
  "
  message Party {
        string entity = 1;
        int32  value  = 2;
  }

  message Init {
        Party partyA = 1;
        Party partyB = 2;
  }
  ")

(deftest test-type-resolution
  (let [intf (parse example-type-resolution)]
    (is (nil? (verify-intf intf)))))

(def example-conflicting-index
  "
  message Conflict {
        string foo    = 1;
        int32  bar    = 2;
        int32  baz    = 1;
  }

  ")

(deftest test-conflict-detection
  (let [intf (parse example-conflicting-index)]
    (is (some? (verify-intf intf)))))

(def example-enum
  "
  enum MyEnum {
         ZERO = 0;
         ONE  = 1;
         TWO  = 2;
  }

  ")

(deftest test-enum
  (let [intf (parse example-enum)]
    (is (nil? (verify-intf intf)))))

(def example-conflicting-enum
  "
  enum ConflictingEnum {
         ZERO = 0;
         ONE  = 1;
         TWO  = 1;
  }

  ")

(deftest test-conflicting-enum
  (let [intf (parse example-conflicting-enum)]
    (is (some? (verify-intf intf)))))

(def example-bad-default-enum
  "
  enum BadDefaultEnum {
         ZERO = 1;
         ONE  = 2;
         TWO  = 3;
  }

  ")

(deftest test-bad-default-enum
  (let [intf (parse example-bad-default-enum)]
    (is (some? (verify-intf intf)))))

(def example-no-parameters
  "
  queries {
      string Parameterless() = 1;
  }

  ")

(deftest test-no-parameters
  (let [intf (parse example-no-parameters)]
    (is (nil? (verify-intf intf)))))
