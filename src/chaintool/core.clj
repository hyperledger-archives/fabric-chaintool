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

(ns chaintool.core
  (:require [chaintool.subcommands.build :as buildcmd]
            [chaintool.subcommands.buildcar :as buildcarcmd]
            [chaintool.subcommands.clean :as cleancmd]
            [chaintool.subcommands.inspect :as inspectcmd]
            [chaintool.subcommands.ls :as lscmd]
            [chaintool.subcommands.package :as packagecmd]
            [chaintool.subcommands.proto :as protocmd]
            [chaintool.subcommands.unpack :as unpackcmd]
            [chaintool.subcommands.env :as envcmd]
            [chaintool.util :as util]
            [clojure.string :as string]
            [clojure.tools.cli :refer [parse-opts]]
            [slingshot.slingshot :as slingshot])
  (:gen-class))

(defn option-merge [& args] (vec (apply concat args)))

;; options common to all modes, top-level as well as subcommands
(def common-options
  [["-h" "--help"]])

(def toplevel-options
  (option-merge [["-v" "--version" "Print the version and exit"]]
                common-options))

;; these options are common to subcommands that are expected to operate on a chaincode tree
(def common-path-options
  (option-merge [["-p" "--path PATH" "path to chaincode project" :default "./"]]
                common-options))

(def subcommand-descriptors
  [{:name "build" :desc "Build the chaincode project"
    :handler  buildcmd/run
    :options (option-merge [["-o" "--output NAME" "path to the output destination"]]
                           common-path-options)}

   {:name "buildcar" :desc "Build the chaincode project from a CAR file"
    :handler  buildcarcmd/run
    :arguments "path/to/file.car"
    :validate (fn [options arguments] (= (count arguments) 1))
    :options (option-merge [["-o" "--output NAME" "path to the output destination"]]
                           common-options)}

   {:name "clean" :desc "Clean the chaincode project"
    :handler cleancmd/run
    :options common-path-options}

   {:name "package" :desc "Package the chaincode into a CAR file for deployment"
    :handler packagecmd/run
    :options (option-merge [["-o" "--output NAME" "path to the output destination"]
                            ["-c" "--compress NAME" "compression algorithm to use" :default "gzip"]]
                           common-path-options)}

   {:name "unpack" :desc "Unpackage a CAR file"
    :handler unpackcmd/run
    :arguments "path/to/file.car"
    :validate (fn [options arguments] (= (count arguments) 1))
    :options (option-merge [["-d" "--directory NAME" "path to the output destination"]]
                           common-options)}

   {:name "ls" :desc "List the contents of a CAR file"
    :handler lscmd/run
    :arguments "path/to/file.car"
    :validate (fn [options arguments] (= (count arguments) 1))
    :options common-options}

   {:name "env" :desc "Display variables used in the build environment"
    :handler envcmd/run
    :options common-path-options}

   {:name "proto" :desc "Compiles a CCI file to a .proto"
    :handler protocmd/run
    :arguments "path/to/file.cci"
    :validate (fn [options arguments] (= (count arguments) 1))
    :options (option-merge [["-o" "--output NAME" "path to the output destination"]]
                           common-options)}

   {:name "inspect" :desc "Retrieves metadata from a running instance"
    :handler inspectcmd/run
    :validate (fn [options arguments] (:name options))
    :options (option-merge [[nil "--host HOST" "The API hostname of the running fabric"
                             :default "localhost"]
                            [nil "--port PORT" "The API port of the running fabric"
                             :default 5000
                             :parse-fn #(Integer/parseInt %)
                             :validate [#(< 0 % 65536) "Must be a number between 0 and 65536"]]
                            ["-n" "--name NAME" "The name of the chaincode instance"]
                            ["-i" "--interfaces PATH" "retrieve interfaces from endpoint and saves them to PATH"]]
                           common-options)}])

;; N.B. the resulting map values are vectors each with a single map as an element
;;
(def subcommands (group-by :name subcommand-descriptors))

(defn exit [status msg & rest]
  (do
    (apply println msg rest)
    status))

(defn version [] (str "chaintool version: v" util/app-version))

(defn prep-usage [msg] (->> msg flatten (string/join \newline)))

(defn usage [options-summary]
  (prep-usage [(version)
               ""
               "Usage: chaintool [general-options] action [action-options]"
               ""
               "General Options:"
               options-summary
               ""
               "Actions:"
               (map (fn [[_ [{:keys [name desc]}]]] (str "  " name " -> " desc)) subcommands)
               ""
               "(run \"chaintool <action> -h\" for action specific help)"]))

(defn subcommand-usage [subcommand options-summary]
  (prep-usage [(version)
               ""
               (str "Description: chaintool " (:name subcommand) " - " (:desc subcommand))
               ""
               (str "Usage: chaintool " (:name subcommand) " [options] " (when-let [arguments (:arguments subcommand)] arguments))
               ""
               "Command Options:"
               options-summary
               ""]))

(defn -app [& args]
  (let [{:keys [options arguments errors summary]} (parse-opts args toplevel-options :in-order true)]
    (cond

      (:help options)
      (exit 0 (usage summary))

      (not= errors nil)
      (exit -1 "Error: " (string/join errors))

      (:version options)
      (exit 0 (version))

      (zero? (count arguments))
      (exit -1 (usage summary))

      :else
      (if-let [[subcommand] (subcommands (first arguments))]
        (let [{:keys [options arguments errors summary]} (parse-opts (rest arguments) (:options subcommand))]
          (cond

            (:help options)
            (exit 0 (subcommand-usage subcommand summary))

            (not= errors nil)
            (exit -1 "Error: " (string/join errors))

            (and (:validate subcommand) (not ((:validate subcommand) options arguments)))
            (exit -1 (subcommand-usage subcommand summary))

            :else
            (slingshot/try+
             ((:handler subcommand) options arguments)
             (exit 0 "")
             (catch [:type :chaintoolabort] {:keys [msg retval]}
               (exit retval (str "Error: " msg))))))

        ;; unrecognized subcommand
        (exit 1 (usage summary))))))

(defn -main [& args]
  (System/exit (apply -app args)))
