(ns user
  (:require [clojure.tools.namespace.repl :refer [refresh]]))

;; user is a namespace that the Clojure runtime looks for and loads if
;; its available

;; You can place helper functions in here. This is great for starting
;; and stopping your webserver and other development services

;; The definitions in here will be available if you run "lein repl" or launch a
;; Clojure repl some other way

;; You have to ensure that the libraries you :require are listed in the :dependencies
;; in the project.clj

;; Once you start down this path
;; you will probably want to look at
;; tools.namespace https://github.com/clojure/tools.namespace
;; and Component https://github.com/stuartsierra/component

;; or the exciting newcomer https://github.com/weavejester/integrant

;; DEVELOPMENT SERVER HELPERS: starting and stopping a development server in the REPL
