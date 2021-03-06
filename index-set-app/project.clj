(defproject nasa-cmr/cmr-index-set-app "0.1.0-SNAPSHOT"
  :description "index-set app is a microservice enabling CMR system create/maintain a logical set of indexes in Elasticsearch
               for indexing and searching for concepts."
  :url "https://github.com/nasa/Common-Metadata-Repository/tree/master/index-set-app"
  :exclusions [
    [cheshire]
    [clj-time]
    [instaparse]
    [org.apache.httpcomponents/httpclient]
    [org.apache.httpcomponents/httpcore]
    [org.clojure/core.async]
    [org.clojure/tools.reader]
    [potemkin]]
  :dependencies [
    [cheshire "5.8.1"]
    [clj-time "0.15.1"]
    [compojure "1.6.1"]
    [instaparse "1.4.10"]
    [nasa-cmr/cmr-acl-lib "0.1.0-SNAPSHOT"]
    [nasa-cmr/cmr-common-app-lib "0.1.0-SNAPSHOT"]
    [nasa-cmr/cmr-elastic-utils-lib "0.1.0-SNAPSHOT"]
    [org.apache.httpcomponents/httpclient "4.5.6"]
    [org.apache.httpcomponents/httpcore "4.4.10"]
    [org.clojure/clojure "1.10.0"]
    [org.clojure/core.async "0.4.490"]
    [org.clojure/tools.nrepl "0.2.13"]
    [org.clojure/tools.reader "1.3.2"]
    [potemkin "0.4.5"]
    [ring/ring-core "1.7.1"]
    [ring/ring-json "0.4.0"]]
  :plugins [
    [lein-shell "0.5.0"]
    [test2junit "1.3.3"]]
  :repl-options {:init-ns user}
  :jvm-opts ^:replace ["-server"
                       "-Dclojure.compiler.direct-linking=true"]
  :test-paths ["test" "int-test"]
  :profiles {
    :dev {
      :dependencies [
        [clj-http "2.3.0"]
        [nasa-cmr/cmr-mock-echo-app "0.1.0-SNAPSHOT"]
        [org.clojars.gjahad/debug-repl "0.3.3"]
        [org.clojure/tools.namespace "0.2.11"]
        [pjstadig/humane-test-output "0.9.0"]
        [proto-repl "0.3.1"]]
      :jvm-opts ^:replace ["-server"]
      :source-paths ["src" "dev" "test" "int-test"]
      :injections [(require 'pjstadig.humane-test-output)
                   (pjstadig.humane-test-output/activate!)]}
    :integration-test {:test-paths ["int-test"]
                       :dependencies [[clj-http "2.3.0"]]}
    :uberjar {
      :main cmr.index-set.runner
      :aot :all}
    :static {}
    ;; This profile is used for linting and static analysis. To run for this
    ;; project, use `lein lint` from inside the project directory. To run for
    ;; all projects at the same time, use the same command but from the top-
    ;; level directory.
    :lint {
      :source-paths ^:replace ["src"]
      :test-paths ^:replace []
      :plugins [
        [jonase/eastwood "0.2.5"]
        [lein-ancient "0.6.15"]
        [lein-bikeshed "0.5.0"]
        [lein-kibit "0.1.6"]
        [venantius/yagni "0.1.4"]]}
    ;; The following profile is overriden on the build server or in the user's
    ;; ~/.lein/profiles.clj file.
    :internal-repos {}}
  :aliases {;; Prints out documentation on configuration environment variables.
            "env-config-docs" ["exec" "-ep" "(do (use 'cmr.common.config) (print-all-configs-docs) (shutdown-agents))"]
            ;; Alias to test2junit for consistency with lein-test-out
            "test-out" ["test2junit"]
            ;; Linting aliases
            "kibit" ["do" ["with-profile" "lint" "shell" "echo" "== Kibit =="]
                          ["with-profile" "lint" "kibit"]]
            "eastwood" ["with-profile" "lint" "eastwood" "{:namespaces [:source-paths]}"]
            "bikeshed" ["with-profile" "lint" "bikeshed" "--max-line-length=100"]
            "yagni" ["with-profile" "lint" "yagni"]
            "check-deps" ["with-profile" "lint" "ancient" ":all"]
            "lint" ["do" ["check"] ["kibit"] ["eastwood"]]
            ;; Placeholder for future docs and enabler of top-level alias
            "generate-static" ["with-profile" "static" "shell" "echo"]})
