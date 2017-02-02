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

(ns chaintool.platforms.golang.core
  (:require [chaintool.build.interface :as intf]
            [chaintool.codecs :as codecs]
            [chaintool.protobuf.generate :as pb]
            [chaintool.util :as util]
            [clojure.algo.generic.functor :as algo]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [me.raynes.conch :as conch]
            [me.raynes.conch.low-level :as sh])
  (:import (java.util ArrayList)
           (org.apache.commons.io.output ByteArrayOutputStream)
           (org.stringtemplate.v4 STGroupFile)))

(conch/programs protoc)
(conch/programs gofmt)
(conch/programs go)

;; types to map to java objects that string template expects.
;;

(deftype Function            [^String rettype ^String name ^String param ^Integer index])
(deftype Interface           [^String name ^String package ^String packageCamel ^String packagepath ^ArrayList functions])
(deftype InterfaceDefinition [^String name ^String bytes])
(deftype Fact                [^String name ^String value])

;;------------------------------------------------------------------
;; helper functions
;;------------------------------------------------------------------
(defn pkg-to-relpath [path]
  (string/replace path #"^_/" ""))

(defn- package-name [name] (-> name (string/split #"\.") last))
(defn- package-camel [name] (-> name package-name string/capitalize))
(defn- package-path [base name] (str (pkg-to-relpath base) "/cci/" (string/replace name "." "/")))

;;------------------------------------------------------------------
;; return a string with composite GOPATH elements, separated by ":"
;;
;; (note that the first entry is where the system will write dependencies
;; retrived by "go get")
;;------------------------------------------------------------------
(defn buildgopath [path]
  (->> [[path "build/deps"] [path "build"] [path] [(System/getenv "GOPATH")]]
       (filter #(not= % [nil]))
       (map #(.getCanonicalPath (apply io/file %)))
       (clojure.string/join ":")))

;;------------------------------------------------------------------
;; X-cmd interfaces: Invoke external commands
;;------------------------------------------------------------------
(defn- protoc-cmd [_path _proto]
  (let [path (->> _path io/file .getCanonicalPath)
        go_out (str "--go_out=" path)
        proto_path (str "--proto_path=" path)
        proto (.getCanonicalPath _proto)]
    (println "[PB] protoc" go_out proto_path proto)
    (try
      (let [result (protoc go_out proto_path proto {:verbose true})]
        (println (:stderr result)))
      (catch clojure.lang.ExceptionInfo e
        (util/abort -1 (-> e ex-data :stderr))))))

(defn go-cmd [path env & args]
  (println "[GO] go" (apply print-str args))
  (let [gopath (buildgopath path)
        _args (vec (concat ["go"] args [:env (merge {"GOPATH" gopath} env)]))]

    (println "\tUsing GOPATH" gopath)
    (let [result (apply sh/proc _args)
          _ (sh/done result)
          stderr (sh/stream-to-string result :err)]

      (if (zero? (sh/exit-code result))
        (println stderr)
        (util/abort -1 stderr)))))

;;-----------------------------------------------------------------
;; buildX - build our ST friendly objects
;;-----------------------------------------------------------------

(defn- build-function [{:keys [rettype functionName param index]}]
  (vector functionName (->Function (when (not= rettype "void") rettype) functionName param index)))

(defn- build-functions [functions]
  (into {} (for [[k v] functions]
             (build-function v))))

(defn- build-interface [base name interface]
  (let [functions (build-functions (:functions interface))]
    (vector name (->Interface name (package-name name) (package-camel name) (package-path base name) functions))))

(defn- build-interfaces [base interfaces]
  (into {} (map (fn [[name interface]] (build-interface base name interface)) interfaces)))

(defn- build-interface-definition [ipath name]
  (let [path (io/file ipath (str name ".cci"))
        os (ByteArrayOutputStream.)]

    ;; first compress the file into memory
    (with-open [is (io/input-stream path)
                compressor (codecs/compressor "gzip" os)]
      (io/copy is compressor))

    ;; compute our new string value for []byte
    (let [data (string/join (for [i (seq (.toByteArray os))] (format "\\x%02x" i)))]
      ;; finally, construct a new definition object
      (vector name (->InterfaceDefinition name data)))))

(defn- build-interface-definitions [ipath interfaces]
  (into {} (map (fn [interface] (build-interface-definition ipath interface)) interfaces)))

(defn- build-facts [config]
  (let [facts [["Application Name" (:Name config)]
               ["Application Version" (:Version config)]
               ["Platform" (str (-> config :Platform :Name) " version " (-> config :Platform :Version))]
               ["Go Version" (-> (go "version") string/trim-newline (string/replace "go version " ""))]
               ["Chaintool Version" util/app-version]]]
    (into {} (map (fn [[name value]] (vector name (->Fact name value))) facts))))

;;-----------------------------------------------------------------
;; generic template rendering
;;-----------------------------------------------------------------
(defn- render-golang [templatename params]
  (let [stg  (STGroupFile. "generators/golang.stg")
        template (.getInstanceOf stg templatename)]

    (dorun (for [[param value] params] (.add template param value)))
    (.render template)))

;;-----------------------------------------------------------------
;; render stub output - compiles the interfaces into the primary
;; golang stub, suitable for writing to a file
;;-----------------------------------------------------------------
(defn- render-primary-stub [base package config interfaces]
  (let [functions (algo/fmap intf/getallfunctions interfaces)
        provides (build-interfaces base (select-keys functions (intf/getprovides config)))]

    (render-golang "primary" [["base" base]
                              ["system" package]
                              ["provides" provides]])))

;;-----------------------------------------------------------------
;; render metadata - compiles the interfaces into metadata
;; structures suitable for surfacing via the
;; org.hyperledger.chaintool.meta interface
;;-----------------------------------------------------------------
(defn- render-metadata [config ipath]
  (let [facts (build-facts config)
        provides (->> config intf/getprovides (build-interface-definitions ipath))]
    (render-golang "metadata" [["facts" facts]
                               ["provides" provides]])))

;;-----------------------------------------------------------------
;; write golang source to the filesystem, using gofmt to clean
;; up the generated code
;;-----------------------------------------------------------------
(defn- emit-golang [outputfile content]
  (util/truncate-file outputfile content)
  (gofmt "-w" (.getCanonicalPath outputfile)))

;;-----------------------------------------------------------------
;; emit-stub
;;-----------------------------------------------------------------
(defn- emit-stub [base name functions template srcdir filename]
  (let [[_ interface] (build-interface base name functions)
        content (render-golang template [["base" base] ["intf" interface]])
        output (io/file srcdir (package-path base name) filename)]

    (emit-golang output content)))

;;-----------------------------------------------------------------
;; emit-server-stub
;;-----------------------------------------------------------------
(defn- emit-server-stub [base name functions srcdir]
  (emit-stub base name functions "server" srcdir "server-stub.go"))

;;-----------------------------------------------------------------
;; emit-proto
;;-----------------------------------------------------------------
(defn- emit-proto [base srcdir [name ast :as interface]]
  (let [outputdir (io/file srcdir (package-path base name))
        output (io/file outputdir "interface.proto")]

    ;; emit the .proto file
    (pb/to-file output (package-name name) interface)

    ;; execute the protoc compiler to generate golang
    (protoc-cmd outputdir output)))

;;-----------------------------------------------------------------
;; compile-metadata
;;-----------------------------------------------------------------
(def metadata-name "org.hyperledger.chaintool.meta")
(defn- compile-metadata []
  (let [data (->> (str "metadata/" metadata-name ".cci") io/resource slurp)]
    (intf/compileintf {:path metadata-name :data data})))

;;-----------------------------------------------------------------
;; compile interfaces
;;-----------------------------------------------------------------
(defn- compile-interfaces [ipath config]
  (let [interfaces (intf/compile ipath config)
        metadata (compile-metadata)]
    (assoc interfaces metadata-name metadata)))

;;-----------------------------------------------------------------
;; generate - generates all of our protobuf/go code based on the
;; config
;;-----------------------------------------------------------------
(defn generate [{:keys [ipath opath config base package] :as params}]
  (let [interfaces (compile-interfaces ipath config)]

     ;; generate protobuf output
    (dorun (for [interface interfaces]
             (emit-proto base opath interface)))

     ;; generate our primary stub
    (let [path (io/file opath (pkg-to-relpath base) "ccs")]
      (let [content (render-primary-stub base package config interfaces)
            filename (io/file path "entrypoint.go")]
        (emit-golang filename content))
      (let [content (render-metadata config ipath)
            filename (io/file path "metadata.go")]
        (emit-golang filename content))
      (let [content (render-golang "api" [])
            filename (io/file path "api" "api.go")]
        (emit-golang filename content)))

     ;; generate our server stubs
    (let [provides (->> config intf/getprovides (filter #(not= % "appinit")) (cons metadata-name))]

       ;; first process all _except_ the appinit interface
      (dorun (for [name provides]
               (let [functions (intf/getallfunctions (interfaces name))]
                 (emit-server-stub base name functions opath))))

       ;; and now special case the appinit  interface
      (emit-server-stub base "appinit" {:functions {1 {:rettype "void", :functionName "Init", :param "Init", :index 1, :subType nil, :typeName nil}}} opath))

     ;; generate our client stubs
    (dorun (for [name (intf/getconsumes config)]
             (let [functions (intf/getallfunctions (interfaces name))]
               (emit-stub base name functions "client" opath "client-stub.go"))))))
