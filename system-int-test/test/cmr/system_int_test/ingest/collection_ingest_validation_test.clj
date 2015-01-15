(ns cmr.system-int-test.ingest.collection-ingest-validation-test
  "Tests ingest validation of collections"
  (:require [clojure.test :refer :all]
            [cmr.system-int-test.utils.ingest-util :as ingest]
            [cmr.system-int-test.data2.collection :as dc]
            [cmr.system-int-test.data2.core :as d]))

(use-fixtures :each (ingest/reset-fixture {"provguid1" "PROV1"}))

(comment

  (do
    (ingest/reset)
    (ingest/create-provider "provguid1" "PROV1")
    (d/ingest "PROV1" (dc/collection {:product-specific-attributes
                                          [(dc/psa "bool" :boolean true)
                                           (dc/psa "bool" :boolean true)]})))

  )

(deftest collection-product-specific-attribute-validation
  (testing "additional attribute names must be unique"
    (let [response (d/ingest "PROV1"
                             (dc/collection {:product-specific-attributes
                                             [(dc/psa "bool" :boolean true)
                                              (dc/psa "bool" :boolean true)]}))]
      (is (= {:status 400
              :errors ["AdditionalAttributes must be unique. It contained duplicate names [bool]."]}
             (select-keys response [:status :errors]))))))