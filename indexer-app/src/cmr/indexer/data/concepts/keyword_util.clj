(ns cmr.indexer.data.concepts.keyword-util
  "Contains utility functions for working with keywords when adding data
  to elasticsearch for indexing."
  (:require
   [clojure.string :as string]
   [cmr.common.util :as util]
   [cmr.indexer.data.concepts.attribute :as attrib]
   [cmr.umm-spec.location-keywords :as lk]
   [cmr.umm-spec.util :as su]))

;; Aliases for NEAR_REAL_TIME
(def nrt-aliases
  ["near_real_time","nrt","near real time","near-real time","near-real-time","near real-time"])

(def ^:private keywords-separator-regex
  "Defines Regex to split strings with special characters into multiple words for keyword searches."
  #"[!@#$%^&()\-=_+{}\[\]|;'.,\\\"/:<>?`~* ]")

(defn- prepare-keyword-field
  [field-value]
  "Convert a string to lowercase then separate it into keywords"
  (when field-value
    (let [field-value (string/lower-case field-value)]
      (into [field-value] (string/split field-value keywords-separator-regex)))))

(defn field-values->keyword-text
  "Returns the keyword text for the given list of field values."
  [field-values]
  (->> field-values
       (mapcat prepare-keyword-field)
       (keep not-empty)
       (apply sorted-set)
       (string/join \space)))

(defn- contact-group->keywords
  "Converts a contact group into a vector of terms for keyword searches."
  [contact-group]
  (let [{group-name :GroupName
         roles :Roles} contact-group]
    (concat [group-name]
            roles)))

(defn- contact-person->keywords
  "Converts a compound field into a vector of terms for keyword searches."
  [contact-person]
  (let [{first-name :FirstName
         last-name :LastName
         roles :Roles} contact-person]
    (concat [first-name last-name]
            roles)))

(defn- get-contact-persons
  "Retrieve a list of contact persons from the given collection."
  [collection]
  (let [{:keys [ContactPersons ContactGroups DataCenters]} collection]
    (concat ContactPersons
            ContactGroups
            (mapcat :ContactGroups DataCenters)
            (mapcat :ContactPersons DataCenters))))

(defn- get-contact-mechanisms->keywords
  "Retrieve contact mechanisms from the given collection
  and convert into a list of terms for keyword searches."
  [collection]
  (map #(:Value (first %))
       (map #(get-in % [:ContactInformation :ContactMechanisms])
            (get-contact-persons collection))))

(defn- data-center->keywords
  "Convert a compound field into a list of terms for keyword searches."
  [data-center]
  (let [{contact-persons :ContactPersons
         contact-groups :ContactGroups} data-center]
    (concat (mapcat contact-person->keywords contact-persons)
            (mapcat contact-group->keywords contact-groups)
            [(:ShortName data-center)])))

(defn- collection-citation->keywords
  "Convert a compound field into a vector of terms for keyword searches."
  [collection-citation]
  [(:Creator collection-citation)
   (:OtherCitationDetails collection-citation)])

(defn- characteristic->keywords
  "Convert a compound field into a vector of terms for keyword searches."
  [characteristic]
  [(:name characteristic)
   (:description characteristic)
   (:value characteristic)])

(defn- collection-platforms->keywords
  "Convert the given platforms to a list of terms for keyword searches."
  [platforms]
  (let [platforms (map util/map-keys->kebab-case
                       (when-not (= su/not-provided-platforms platforms) platforms))
        platform-short-names (map :short-name platforms)
        platform-instruments (mapcat :instruments platforms)
        instruments (concat platform-instruments (mapcat :composed-of platform-instruments))
        instrument-short-names (distinct (keep :short-name instruments))
        instrument-techniques (keep :technique instruments)
        instrument-characteristics (mapcat characteristic->keywords
                                           (mapcat :characteristics instruments))
        platform-characteristics (mapcat characteristic->keywords
                                         (mapcat :characteristics platforms))]
    (concat platform-characteristics
            instrument-characteristics
            instrument-short-names
            instrument-techniques
            platform-short-names)))

(defn- names->keywords
  "Converts a compound field into a vector of terms for keyword searches."
  [data]
  (let [{long-name :LongName
         short-name :ShortName} data]
    [long-name
     short-name]))

(defn- additional-attribute->keywords
  "Convert a compound field into a vector of terms for keyword searches."
  [attribute]
  (attrib/aa->keywords (util/map-keys->kebab-case attribute)))

(defn- collection-data-type->keywords
  "Return collection data type keywords."
  [data-type]
  (if (= "NEAR_REAL_TIME" data-type)
    nrt-aliases
    data-type))

(defn- platform->keywords
  "Converts a compound field into a vector of terms for keyword searches."
  [platform]
  (let [{instruments :Instruments} platform]
    (concat (names->keywords platform)
            (mapcat names->keywords instruments))))

(defn- related-url->keywords
  "Converts a compound field into a vector of terms for keyword searches."
  [data]
  (let [{description :Description
         subtype :Subtype
         type :Type
         url :URL
         url-content-type :URLContentType} data]
    [description
     subtype
     type
     url
     url-content-type]))

(defn science-keyword->keywords
  "Converts a science keyword into a vector of terms for keyword searches."
  [science-keyword]
  (let [{category :Category
         detailed-variable :DetailedVariable
         term :Term
         topic :Topic
         variable-level-1 :VariableLevel1
         variable-level-2 :VariableLevel2
         variable-level-3 :VariableLevel3} science-keyword]
    [category
     detailed-variable
     term
     topic
     variable-level-1
     variable-level-2
     variable-level-3]))

(defn- service-keyword->keywords
  "Converts a service keyword into a vector of terms for keyword searches."
  [service-keyword]
  (let [{service-category :ServiceCategory
         service-specific-term :ServiceSpecificTerm
         service-term :ServiceTerm
         service-topic :ServiceTopic} service-keyword]
    [service-category
     service-specific-term
     service-term
     service-topic]))

(defn- service-organization->keywords
  "Converts a service keyword into a vector of terms for keyword searches."
  [service-organization]
  (let [{roles :Roles
         service-contact-persons :ContactPersons} service-organization]
    (concat (names->keywords service-organization)
            (mapcat contact-person->keywords service-contact-persons)
            roles)))

(def ^:private variable-fields->fn-mapper
  "A data structure that maps UMM variable field names to functions that
  extract keyword data for those fields. Intended only to be used as part
  of a larger map for multiple field types.

  See `fields->fn-mapper`, below."
  {;; Simple single-valued data
   :variable-name :variable-name
   :measurement :measurement})

(def ^:private service-fields->fn-mapper
  "A data structure that maps UMM service field names to functions that
  extract keyword data for those fields. Intended only to be used as part
  of a larger map for multiple field types.

  See `fields->fn-mapper`, below."
  {;; Simple single-valued data
   :LongName :LongName
   :Name :Name
   :Version :Version
   ;; Simple multi-valued data
   :AncillaryKeywords :AncillaryKeywords
   :ContactGroups #(mapcat contact-group->keywords (:ContactGroups %))
   :ContactPersons #(mapcat contact-person->keywords (:ContactPersons %))
   :Platforms #(mapcat platform->keywords (:Platforms %))
   :RelatedURLs #(mapcat related-url->keywords (:RelatedURLs %))
   :ServiceKeywords #(mapcat service-keyword->keywords (:ServiceKeywords %))
   :ServiceOrganizations #(mapcat service-organization->keywords (:ServiceOrganizations %))})

(def ^:private collection-fields->fn-mapper
  "A data structure that maps UMM collection field names to functions that
  extract keyword data for those fields. Intended only to be used as part
  of a larger map for multiple field types.

  See `fields->fn-mapper`, below."
  {;; Simple single-values data
   :Abstract :Abstract
   :DOI #(get-in % [:DOI :DOI])
   :EntryTitle :EntryTitle
   :ProcessingLevel #(get-in % [:ProcessingLevel :Id])
   :ShortName :ShortName
   :VersionDescription :VersionDescription
   ;; Simple multi-values data
   :AdditionalAttributes #(mapcat additional-attribute->keywords (:AdditionalAttributes %))
   :CollectionCitations #(mapcat collection-citation->keywords (:CollectionCitations %))
   :CollectionDataType #(collection-data-type->keywords (:CollectionDataType %))
   :CollectionPlatforms #(collection-platforms->keywords (:Platforms %))
   :ContactMechanisms get-contact-mechanisms->keywords
   :DataCenters #(mapcat data-center->keywords (:DataCenters %))
   :DirectoryNames #(mapcat names->keywords (:DirectoryNames %))
   :ISOTopicCategories :ISOTopicCategories
   :LocationKeywords #(lk/location-keywords->spatial-keywords-for-indexing (:LocationKeywords %))
   :Projects #(mapcat names->keywords (:Projects %))
   :RelatedUrls #(mapcat related-url->keywords (:RelatedUrls %))
   :TemporalKeywords :TemporalKeywords
   :TilingIdentificationSystems #(map :TilingIdentificationSystemName (:TilingIdentificationSystems %))})

(def ^:private shared-fields->fn-mapper
  "A data structure that maps UMM field names used by multiple types to
  functions that extract keyword data for those fields. Intended only to be
  used as part of a larger map for multiple field types.

  See `fields->fn-mapper`, below."
  {;; Simple multi-valued data
   :ScienceKeywords #(mapcat science-keyword->keywords (:ScienceKeywords %))})

(def fields->fn-mapper
  "A data structure that maps UMM field names to functions that extract keyword
  data for those fields. Intended to be used instead of `case` statements for
  dispatching based upon field name.

  For example, to iterate over all the science keywords in a concept and return
  textual data that will be indexd (i.e., from sub-fields):

    (map (:ScienceKeywords fields->fn-mapper) parsed-concept))"
  (merge variable-fields->fn-mapper
         service-fields->fn-mapper
         collection-fields->fn-mapper
         shared-fields->fn-mapper))

(defn- flatten-collections
  "This function is used to conditionally prepare schema texutal field data,
  in the form of collections of strings or strings, to be used by higher
  order functions that operate on flat collections of textual field data. As
  such, textual data that is not in a collection needs to be converted to
  one. Additionally, any nested collections need to be flattened. Everything
  returned from this function should be a collection of one or more strings."
  [data]
  (if (coll? data)
    (flatten data)
    (vector data)))

(defn concept-key->keywords
  "Given a parsed concept and a single schema key, build a list of keywords
  for that key."
  [parsed-concept schema-key]
  (let [extractor (schema-key fields->fn-mapper)]
    (->> parsed-concept
         extractor
         flatten-collections
         (remove nil?)
         (remove #(su/default-value? %)))))

(defn concept-keys->keywords
  "Given a parsed concept and a sequence of schema keys, get the keywords
  using all the schema keys."
  [parsed-concept schema-keys]
  (->> schema-keys
       (map (partial concept-key->keywords parsed-concept))
       flatten
       (remove empty?)))

(defn concept-key->keyword-text
  "Given a parsed concept and a single schema key, build a concatenated string
  of keywords for that key."
  [parsed-concept schema-key]
  (field-values->keyword-text
   (concept-key->keywords parsed-concept schema-key)))

(defn concept-keys->keyword-text
  "Given a parsed concept and a sequence of schema keys, build a concatenated
  string of keywords using all the schema keys."
  [parsed-concept schema-keys]
  (field-values->keyword-text
   (concept-keys->keywords parsed-concept schema-keys)))
