(defproject nasa-cmr/cmr-search-relevancy-test "0.1.0-SNAPSHOT"
  :description "Tests for measuring CMR search relevancy"
  :url "https://github.com/nasa/Common-Metadata-Repository/tree/master/search-relevancy-test"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :exclusions [
    [cheshire]
    [clj-time]
    [com.fasterxml.jackson.core/jackson-core]
    [commons-codec/commons-codec]
    [commons-fileupload]
    [commons-io]
    [org.apache.httpcomponents/httpclient]
    [org.apache.httpcomponents/httpcore]
    [org.clojure/tools.reader]
    [potemkin]]
  :dependencies [
    [camel-snake-kebab "0.4.0"]
    [cheshire "5.8.1"]
    [clj-http "2.3.0"]
    [clj-time "0.15.1"]
    [com.fasterxml.jackson.core/jackson-core "2.9.8"]
    [commons-codec/commons-codec "1.11"]
    [commons-fileupload "1.3.3"]
    [commons-io "2.6"]
    [nasa-cmr/cmr-system-int-test "0.1.0-SNAPSHOT"]
    [nasa-cmr/cmr-transmit-lib "0.1.0-SNAPSHOT"]
    [org.apache.httpcomponents/httpclient "4.5.6"]
    [org.apache.httpcomponents/httpcore "4.4.10"]
    [org.clojure/clojure "1.10.0"]
    [org.clojure/tools.reader "1.3.2"]
    [potemkin "0.4.5"]]
  :plugins [
    [lein-shell "0.5.0"]
    [test2junit "1.3.3"]]
  :main ^:skip-aot search-relevancy-test.runner
  :jvm-opts ^:replace ["-server"
                       "-XX:-OmitStackTraceInFastThrow"
                       "-Dclojure.compiler.direct-linking=true"]
  :profiles {
    :dev {
    :dependencies [
      [org.clojars.gjahad/debug-repl "0.3.3"]
      [org.clojure/tools.namespace "0.2.11"]
      [pjstadig/humane-test-output "0.9.0"]]
    :injections [(require 'pjstadig.humane-test-output)
                (pjstadig.humane-test-output/activate!)]
    :jvm-opts ^:replace ["-server"
                        "-XX:-OmitStackTraceInFastThrow"]
    :source-paths ["src" "dev"]}
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
  :aliases {;; Alias to test2junit for consistency with lein-test-out
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
