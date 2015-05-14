(ns cmr.metadata-db.services.concept-constraints
  "Functions for enforcing constraint checks just after a concept has been saved."
  (:require [cmr.common.services.errors :as errors]
            [cmr.metadata-db.data.concepts :as c]
            [cmr.common.config :refer [defconfig]]
            [cmr.metadata-db.services.messages :as msg]
            [cmr.common.util :as util]
            [clojure.set :as set]))

(defconfig enforce-granule-ur-constraint
  "Configuration to allow enabling and disabling of the granule UR uniqueness constraint"
  {:default false
   :type Boolean})

(defn- find-latest-matching-concepts
  "Returns any concepts which match the concept-type, provider-id, and value for the given field
  that matches the passed in concept"
  [db concept field field-value]
  (->> (c/find-latest-concepts db {:concept-type (:concept-type concept)
                                   :provider-id (:provider-id concept)
                                   field field-value})
       ;; Remove tombstones from the list of concepts
       (remove :deleted)))

(defn- validate-num-concepts-found
  "Validates that exactly one matching concept is found. Otherwise throw an error with an
  appropriate error message."
  [concepts concept field field-value]
  (let [num-concepts (count concepts)]
    (cond
      (zero? num-concepts)
      (errors/internal-error!
        (format "Unable to find saved concept for provider [%s] and %s [%s]"
                (:provider-id concept)
                (name field)
                field-value))
      (> num-concepts 1)
      [(msg/duplicate-field-msg
         field
         (remove #(= (:concept-id concept) (get % :concept-id)) concepts))])))

(defn unique-field-constraint
  "Returns a function which verifies that there is only one non-deleted concept for a provider
  with the value for the given field."
  [field]
  (fn [db concept]
    (let [field-value (get-in concept [:extra-fields field])
          concepts (find-latest-matching-concepts db concept field field-value)]
      (validate-num-concepts-found concepts concept field field-value))))

(defn granule-ur-unique-constraint
  "Verifies that there is only one non-deleted concept for a provider
  with the same value in the granule-ur field.

  The reason this cannot be done using the unique-field-constraint function is that granule-ur
  can be null. When the granule-ur is null that means the native-id is the same as the granule-ur.
  As a result we need to look for granules with granule-ur or native-id that match the granule-ur
  of the newly ingested concept. We then take the union of those results and check if more than
  one concept is found."
  [db concept]
  (let [granule-ur (get-in concept [:extra-fields :granule-ur])
        granule-ur-concept-matches (find-latest-matching-concepts db concept :granule-ur granule-ur)
        native-id-concept-matches (find-latest-matching-concepts db concept :native-id granule-ur)
        combined-matches (->> (set/union (set granule-ur-concept-matches)
                                         (set native-id-concept-matches))
                              (filter #(= granule-ur (get-in % [:extra-fields :granule-ur]))))]
    (validate-num-concepts-found combined-matches concept :granule-ur granule-ur)))

;; Note - change back to a var once the enforce-granule-ur-constraint configuration is no longer
;; needed. Using a function for now so that configuration can be changed in tests.
(defn- constraints-by-concept-type
  []
  "Maps concept type to a list of constraint functions to run."

  {:collection [(unique-field-constraint :entry-title)
                (unique-field-constraint :entry-id)]
   :granule (when (enforce-granule-ur-constraint) [granule-ur-unique-constraint])})

(defn perform-post-commit-constraint-checks
  "Perform the post commit constraint checks aggregating any constraint violations. Returns nil if
  there are no constraint violations. Otherwise it performs any necessary database cleanup using
  the provided rollback-function and throws a :conflict error."
  [db concept rollback-function]
  (let [constraints ((constraints-by-concept-type) (:concept-type concept))]
    (when-let [errors (seq (util/apply-validations constraints db concept))]
      (rollback-function)
      (errors/throw-service-errors :conflict errors))))
