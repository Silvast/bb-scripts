#!/usr/bin/env bb

(ns eduskunta_queries
  (:require
   [babashka.deps :as deps]
   [clojure.java.io :as io]
   [clojure.java.shell :as sh]
   [babashka.http-client :as http]))

(def db "/tmp/eduskunta.db")

(defn query [& args]
  (apply sh/sh "sqlite3" "-quote" "-header" db args))

(defn create-db! []
  (when (not (.exists (io/file db)))
    (query "create table seatings (
             hetekaId text,
             seatNumber text,
             lastname text,
             firstname text,
             party text,
             minister text
           )")))

(deps/add-deps '{:deps {com.github.seancorfield/honeysql {:mvn/version "2.7.1350"}}})
(require '[honey.sql :as hsql])
(def url "https://avoindata.eduskunta.fi/api/v1/tables/SeatingOfParliament/batch?pkName=seatNumber&pkStartValue=101&perPage=100")

(println
 (hsql/format {:select [:a :b :c] :from [:foo] :where [:= :a 1]}))

;; (create-db!)
;;
(deps/add-deps '{:deps {org.clojure/data.json {:mvn/version "2.4.0"}}})
(require '[clojure.data.json :as json])

(defn fetch-rowdata []
  (let [resp (http/get url)
        body (:body resp)
        parsed (json/read-str body :key-fn keyword)]
    (:rowData parsed)))

(defn sql-quote [s]
  (str "'" (clojure.string/replace (str s) "'" "''") "'"))

(defn insert-rowdata []
  (doseq [[hetekaId seatNumber lastname firstname party minister] (fetch-rowdata)]
    (let [sql (format "insert into seatings (hetekaId, seatNumber, lastname, firstname, party, minister) values (%s, %s, %s, %s, %s, %s)"
                      (sql-quote hetekaId)
                      (sql-quote seatNumber)
                      (sql-quote lastname)
                      (sql-quote firstname)
                      (sql-quote party)
                      (sql-quote minister))]
      (println hetekaId)
      (query sql))))

;; (.delete (io/file db))
;; (create-db!)
(insert-rowdata)
