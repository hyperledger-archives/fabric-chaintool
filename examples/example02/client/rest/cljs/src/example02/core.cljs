(ns example02.core
  (:require [cljs.nodejs :as nodejs]
            [example02.rpc :as rpc]))

(def pb (nodejs/require "protobufjs"))
(def builder (.newBuilder pb))

(defn- loadproto [name]
  (do
    (.loadProtoFile pb (str "./" name ".proto") builder)
    (.build builder name)))

(def init (loadproto "appinit"))
(def app (loadproto "org.hyperledger.chaincode.example02"))

(defn deploy [{:keys [args] :as options}]
  (rpc/deploy (assoc options
                     :func "init"
                     :args (init.Init. args)
                     :cb (fn [resp] (println "Response:" resp)))))

(defn make-payment [{:keys [args] :as options}]
  (rpc/invoke (assoc options
                     :func "org.hyperledger.chaincode.example02/txn/1"
                     :args (app.PaymentParams. args)
                     :cb (fn [resp] (println "Response:" resp)))))

(defn delete-account [{:keys [args] :as options}]
  (rpc/invoke (assoc options
                     :func "org.hyperledger.chaincode.example02/txn/2"
                     :args (app.Entity. args)
                     :cb (fn [resp] (println "Response:" resp)))))

(defn check-balance [{:keys [args] :as options}]
  (rpc/query (assoc options
                    :func "org.hyperledger.chaincode.example02/query/1"
                    :args (app.Entity. args)
                    :cb (fn [resp]
                          (if (= (->> resp :result :status) "OK")
                            (let [result (->> resp :result :message app.BalanceResult.decode64)]
                              (println "Success: Balance =" (.-balance result)))
                            ;; else
                            (println "Failure:" resp))))))
