(ns thai2english.content-script.core
  (:require-macros [cljs.core.async.macros :refer [go-loop]]
                   [hiccups.core :refer [html]])
  (:require [cljs.core.async :refer [<!]]
            [chromex.logging :refer-macros [log info warn error group group-end]]
            [chromex.protocols :refer [post-message! get-name]]
            [chromex.ext.runtime :as runtime :refer-macros [connect]]
            [hiccups.runtime :as hiccupsrt]
            [domina.xpath :refer [xpath]]
            [domina.events :as de]
            [domina :as d]
            [clojure.string :as str]
            [cognitect.transit :as t]))

(enable-console-print!)

(def reader (t/reader :json))
(def writer (t/writer :json))

(defn append-to-body! [el]
  (d/append! (xpath "//body") el))

(defn get-window-offset [pixels]
  (str (+ pixels 5) "px"))

(defn style [& info]
  (apply str (map #(let [[kwd val] %]
                     (str (name kwd) ":" val "; "))
                  (apply hash-map info))))

(defn target-ancestor? [ancestor e]
  (d/ancestor? ancestor (.-target (.-evt e))))

(defn create-tran-result-el [sentences [page-x page-y]]
  (html
    [:div#tran-result {:style (style :left (get-window-offset page-x)
                                     :top (get-window-offset page-y))}
     [:table
      (for [sentence sentences]
        (for [word (:WordObjects sentence)]
          [:tr
           [:td (:Word word)]
           [:td (:Transliteration word)]
           [:td
            (for [meaning (:Meanings word)]
              [:div (:Meaning meaning)])]]))]]))

(defn show-th->en-translation [message]
  (let [el (-> (create-tran-result-el (:Sentences message)
                                      (-> message :orig-message :coords)))]
    (append-to-body! el)))

(defn process-message! [message]
  (let [msg (t/read reader message)]
    (condp = (:type msg)
      :th->en (show-th->en-translation msg))))

(defn run-message-loop! [message-channel]
  (go-loop []
           (when-let [message (<! message-channel)]
             (process-message! message)
             (recur))))

(defn contains-thai? [text]
  (re-find #"[\u0e00-\u0e7e]" text))

(defn append-prompt-el! []
  (let [el (html [:div#tran-prompt {:class "hidden"} ""])]
    (append-to-body! el)))

(defn request-translation [text background-port coords]
  (post-message! background-port (t/write writer {:type :th->en
                                                  :text text
                                                  :coords coords})))

(defn position-el! [el page-x page-y]
  (d/set-styles! el {:left (get-window-offset page-x) :top (get-window-offset page-y)}))

(defn show-prompt-el! [el page-x page-y selection background-port]
  (position-el! el page-x page-y)
  (d/remove-class! el "hidden")
  (de/listen-once! el :click (fn [e]
                               (.stopPropagation (.-evt e))
                               (request-translation selection background-port [page-x page-y]))))

(defn hide-prompt-el! [el]
  (d/add-class! el "hidden"))

(defn on-mouse-up [background-port event]
  (let [selection (str/trim (str (.getSelection js/window)))
        prompt-el (d/by-id "tran-prompt")]
    (if (and (not (str/blank? selection)) (contains-thai? selection))
      (show-prompt-el! prompt-el (.-pageX event) (.-pageY event) selection background-port)
      (hide-prompt-el! prompt-el))))

(defn listen-text-selection! [backgroud-port]
  (.addEventListener js/document "mouseup" (partial on-mouse-up backgroud-port))
  (de/listen! js/document :mousedown (fn [e]
                                       (let [result-el (d/by-id "tran-result")
                                             prompt-el (d/by-id "tran-prompt")]
                                         (when-not (target-ancestor? prompt-el e)
                                           (de/unlisten! prompt-el))
                                         (when-not (target-ancestor? result-el e)
                                           (d/destroy! result-el))))))

(defn connect-to-background-page! []
  (let [background-port (runtime/connect)]
    (run-message-loop! background-port)
    (append-prompt-el!)
    (listen-text-selection! background-port)))

; -- main entry point -------------------------------------------------------------------------------------------------------

(defn init! []
  (connect-to-background-page!))