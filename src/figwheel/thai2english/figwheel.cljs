(ns thai2english.figwheel
  (:require [thai2english.dev.repl :refer [repl-plugin echoing-eval]]
            [figwheel.client :as figwheel]))

; -------------------------------------------------------------------------------------------------------------------
; has to be included before boot

(figwheel/start
  {:websocket-url "ws://localhost:7000/figwheel-ws"
   :eval-fn       (partial echoing-eval {})
   :merge-plugins {:repl-plugin repl-plugin}})
