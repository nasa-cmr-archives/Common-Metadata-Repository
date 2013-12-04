(ns cmr.es-spatial-plugin.StringMatchScript
  (:import org.elasticsearch.common.logging.ESLogger
           org.elasticsearch.index.fielddata.ScriptDocValues$Strings)
  (:gen-class :extends org.elasticsearch.script.AbstractSearchScript
              :constructors {[String String org.elasticsearch.common.logging.ESLogger] []}
              :init init
              :exposes-methods {doc getDoc}
              :state data))


(defn- -init [field-name value logger]
  [[] {:field-name field-name
       :search-value value
       :logger logger}])

(defn- field-name [this]
  (:field-name (.data this)))

(defn- search-value [this]
  (:search-value (.data this)))

(defn- info [this ^String msg]
  (let [logger (:logger (.data this))]
    (.info logger msg nil)))

(defn- doc-values
  "Gets the values out of the doc for the given field-name"
  [doc field-name]
  (let [^ScriptDocValues$Strings doc-value (get doc field-name)]
    (when (and (not (nil? doc-value))
             (not (.isEmpty doc-value)))
      (.getValues doc-value))))

(defn- doc-matches?
  "Returns true if the doc contains a value in the field name that matches the search-value"
  [doc field-name search-value]
  ; Must explicitly return true or false or elastic search will complain
  (if (some #{search-value} (doc-values doc field-name))
    true
    false))

(defn -run [this]
  (doc-matches? (.getDoc this) (field-name this) (search-value this)))
