;;-----------------------------------------------------------------------------
;; Copyright 2017 Greg Haskins
;;
;; SPDX-License-Identifier: Apache-2.0
;;-----------------------------------------------------------------------------

(defproject example02 "0.1.0-SNAPSHOT"
  :description "FIXME: write this!"
  :url "http://example.com/FIXME"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.542"]
                 [org.clojure/tools.cli "0.3.5"]
                 [funcool/promesa "1.8.1"]]
  :plugins [[lein-nodecljs "0.7.0"]]
  :npm {:dependencies [[source-map-support "0.4.15"]
                       [protobufjs "5.0.3"]
                       [read-yaml "1.1.0"]
                       [fabric-client "1.0.0-beta"]]}
  :source-paths ["src" "target/classes"]
  :clean-targets ["out" "release"]
  :target-path "target"
  :nodecljs {:main example02.main
             :files ["protos"]})
