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
(ns chaintool.car.types
  (:import [chaintool.car.abi
            Car$CompatibilityHeader
            Car$Archive
            Car$Archive$Signature
            Car$Archive$Payload
            Car$Archive$Payload$Compression
            Car$Archive$Payload$Entries])
  (:require [flatland.protobuf.core :as fl]))

(def Header      (fl/protodef Car$CompatibilityHeader))
(def Archive     (fl/protodef Car$Archive))
(def Signature   (fl/protodef Car$Archive$Signature))
(def Payload     (fl/protodef Car$Archive$Payload))
(def Compression (fl/protodef Car$Archive$Payload$Compression))
(def Entries     (fl/protodef Car$Archive$Payload$Entries))

(def CompatVersion {:magic "org.hyperledger.chaincode-archive" :version 1})
