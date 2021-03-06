(defproject thai2english "1.0.5"
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.170"]
                 [org.clojure/core.async "0.2.374"]
                 [binaryage/chromex "0.2.0"]
                 [binaryage/devtools "0.5.2"]
                 [figwheel "0.5.0-6"]
                 [environ "1.0.2"]
                 [hiccups "0.3.0"]
                 [domina "1.0.3"]
                 [cljs-ajax "0.5.3"]
                 [com.cognitect/transit-cljs "0.8.237"]]

  :plugins [[lein-cljsbuild "1.1.1"]
            [lein-figwheel "0.5.0-6"]
            [lein-shell "0.4.2"]
            [lein-environ "1.0.2"]
            [lein-cooper "1.1.2"]]

  :figwheel
  {:server-port    7000
   :server-logfile ".figwheel_server.log"}

  :source-paths ["src"]

  :clean-targets ^{:protect false} ["target"
                                    "resources/unpacked/compiled"
                                    "resources/release/compiled"]

  :cljsbuild {:builds {}}                                                                                                     ; prevent https://github.com/emezeske/lein-cljsbuild/issues/413

  :profiles {:unpacked
             {:cljsbuild {:builds
                          {:background
                           {:source-paths ["src/dev"
                                           "src/figwheel"
                                           "src/background"]
                            :compiler     {:output-to             "resources/unpacked/compiled/background/thai2english.js"
                                           :output-dir            "resources/unpacked/compiled/background"
                                           :asset-path            "compiled/background"
                                           :optimizations         :none
                                           :anon-fn-naming-policy :unmapped
                                           :compiler-stats        true
                                           :cache-analysis        true
                                           :source-map            true
                                           :source-map-timestamp  true}}
                           :content-script
                           {:source-paths ["src/dev"
                                           "src/content_script"]
                            :compiler     {:output-to             "resources/unpacked/compiled/content_script/thai2english.js"
                                           :output-dir            "resources/unpacked/compiled/content_script"
                                           :asset-path            "compiled/content_script"
                                           :optimizations         :whitespace                                                 ; content scripts cannot do eval / load script dynamically
                                           :anon-fn-naming-policy :unmapped
                                           :pretty-print          true
                                           :compiler-stats        true
                                           :cache-analysis        true
                                           :source-map            "resources/unpacked/compiled/content_script/thai2english.js.map"
                                           :source-map-timestamp  true}}}}}
             :checkouts
             {:cljsbuild {:builds
                          {:background     {:source-paths ["checkouts/chromex/src/lib"
                                                           "checkouts/chromex/src/exts"]}
                           :content-script {:source-paths ["checkouts/chromex/src/lib"
                                                           "checkouts/chromex/src/exts"]}}}}
             :release
             {:env       {:chromex-elide-verbose-logging "true"}
              :cljsbuild {:builds
                          {:background
                           {:source-paths ["src/background"]
                            :compiler     {:output-to      "resources/release/compiled/background.js"
                                           :output-dir     "resources/release/compiled/background"
                                           :asset-path     "compiled/background"
                                           :optimizations  :advanced
                                           :elide-asserts  true
                                           :compiler-stats true}}
                           :content-script
                           {:source-paths ["src/content_script"]
                            :compiler     {:output-to      "resources/release/compiled/content_script.js"
                                           :output-dir     "resources/release/compiled/content_script"
                                           :asset-path     "compiled/content_script"
                                           :optimizations  :advanced
                                           :elide-asserts  true
                                           :compiler-stats true}}}}}}

  :aliases {"dev-build" ["with-profile" "+unpacked" "cljsbuild" "once" "background" "content-script"]
            "fig"       ["with-profile" "+unpacked" "figwheel" "background"]
            "content"   ["with-profile" "+unpacked" "cljsbuild" "auto" "content-script"]
            "devel"     ["do" "clean," "cooper"]
            "release"   ["with-profile" "+release" "do" "clean," "cljsbuild" "once" "background" "content-script"]
            "package"   ["shell" "scripts/package.sh"]})
