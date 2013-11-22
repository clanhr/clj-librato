(ns clj-librato.metrics-test
  (:use clojure.test
        clj-librato.metrics))

(def user   (System/getenv "LIBRATO_METRICS_USER"))
(def apikey (System/getenv "LIBRATO_METRICS_API_KEY"))

(when-not user
  (println "export LIBRATO_METRICS_USER=\"...\" to run these tests."))
(when-not apikey
  (println "export LIBRATO_METRICS_API_KEY=\"...\" to run these tests."))

(defn now []
  (long (/ (System/currentTimeMillis) 1000)))

(deftest parse-kw-test
         (is (= (parse-kw {"hello_there" {"yarr" 2}})
                {:hello-there {"yarr" 2}})))

(deftest unparse-kw-test
         (is (= (unparse-kw {:hello-there [{:tiny-kitten 3}]})
                {"hello_there" [{"tiny_kitten" 3}]})))

(defn test-value
  []
  {:name "test.gauge"
   :source "clj-librato"
   :value (/ (rand-int 1000) (rand-int 1000))
   :measure-time (now)})

(defn test-collate-and-guage
  [options]
  (testing "gauge"
    (let [gauge (test-value)]
      ;; Submit gauge
      (is (collate user apikey [gauge] [] options) "is created")

      (testing "can be queried"
        (let [metric (metric user apikey (:name gauge)
                             {:end-time (:measure-time gauge)
                              :count 1
                              :resolution 1}
                             options)]
          (is (= (:type metric) "gauge"))
          (is (= (:name metric) (:name gauge)))
          (testing "has a last metric value"
            (let [m (-> metric
                        :measurements
                        (get (:source gauge))
                        (first))]
              (is m)
              (is (= (:measure-time m) (:measure-time gauge)))
              (is (= (:value m) (double (:value gauge))))
              (is (= (:count m) 1)))))))))

(deftest collate-test
  (testing "reject nil names"
    (is (thrown? java.lang.AssertionError
                 (collate user apikey [{:name nil}] [] {})))
    (is (thrown? java.lang.AssertionError
                 (collate user apikey [] [{:name nil}] {}))))

  (testing "reject nil values"
    (is (thrown? java.lang.AssertionError
                 (collate user apikey [{:name "foo"
                                        :value nil}] [] {})))
    (is (thrown? java.lang.AssertionError
                 (collate user apikey [] [{:name "foo"
                                           :value nil}] {}))))

  (testing "with no http options"
    (test-collate-and-guage nil))
  (testing "with persistent http options"
    (let [cm (connection-manager {})]
      (is cm "connection manager created")
      (test-collate-and-guage {:connection-manager cm}))))

(defn test-annotation
  []
  {:title (str "A test event: " (rand 10000000))
   :source "clj-librato"
   :description "Testing clj-librato annotations"
   :start-time (now)
   :end-time (+ 10 (now))})

(defn annotation=
  [e1 e2]
  (let [ks [:title :description :source :start-time :end-time]]
    (= (select-keys e1 ks) (select-keys e2 ks))))

(defn test-annotate
  [options]
  (testing "a test annotation"
    (let [name "test.annotations"
          annot-map (test-annotation)]

      (testing "can be created"
        (let [res (annotate user apikey name annot-map options)]
          (is res "is created without error")

          (testing "and is queryable"
            (let [a (annotation user apikey name (:id res) options)]
              (is a "can be queried by returned id")
              (is (= res a) "is as returned by annotate")
              (is (annotation= a annot-map) "has matching attributes")))

          (testing "and can be updated"
            (annotate user apikey name (:id res)
                      {:end-time (inc (:end-time annot-map))}
                      options)
            (is (= (inc (:end-time annot-map))
                   (:end-time (annotation user apikey name (:id res) options)))
                "updated attribute matches")))))))

(deftest annotate-test
  (testing "with no http options"
    (test-annotate nil))
  (testing "with persistent http options"
    (let [cm (connection-manager {})]
      (is cm "connection manager created")
      (test-annotate {:connection-manager cm}))))

(deftest annotation-test
         ; 404s return nil
         (is (nil? (annotation user apikey "asdilhugflsdbfg" 1234 {})))
         (is (nil? (annotation user apikey name 2345235624534 {}))))
