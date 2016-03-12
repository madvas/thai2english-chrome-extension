(ns thai2english.background.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :refer [<! chan]]
            [chromex.logging :refer-macros [log info warn error group group-end]]
            [chromex.chrome-event-channel :refer [make-chrome-event-channel]]
            [chromex.protocols :refer [post-message! get-sender get-name]]
            [chromex.ext.runtime :as runtime]
            [ajax.core :refer [POST]]
            [cognitect.transit :as t]))

(def clients (atom []))
(def url-thai->en "http://www.thai2english.com/ajax/AddNewQueryDoSpacing.aspx")
(def reader (t/reader :json))
(def writer (t/writer :json))

; -- clients manipulation ---------------------------------------------------------------------------------------------------

(defn add-client! [client]
  (swap! clients conj client))

(defn remove-client! [client]
  (let [remove-item (fn [coll item] (remove #(identical? item %) coll))]
    (swap! clients remove-item client)))

; -- client event loop ------------------------------------------------------------------------------------------------------

(defn resp-handler [client orig-message response]
  (post-message! client (t/write writer (assoc response :type :th->en
                                                        :orig-message orig-message))))

(defn err-handler [client error]
  (post-message! client error))

(defn request-th->en [message client]
  (POST url-thai->en {:params          {:unspacedText (:text message)
                                        :queryDivId   "queryText"}
                      :format          :url
                      :response-format :json
                      :keywords?       true
                      :handler         (partial resp-handler client message)
                      :error-handler   (partial err-handler client)}))

(defn run-client-message-loop! [client]
  (go-loop []
           (when-let [message (t/read reader (<! client))]
             (condp = (:type message)
               :th->en (request-th->en message client)
               nil)
             (recur))
           (remove-client! client)))

; -- event handlers ---------------------------------------------------------------------------------------------------------

(defn handle-client-connection! [client]
  (add-client! client)
  (run-client-message-loop! client))


; -- main event loop --------------------------------------------------------------------------------------------------------

(defn process-chrome-event [event-num event]
  (let [[event-id event-args] event]
    (case event-id
      ::runtime/on-connect (apply handle-client-connection! event-args)
      nil)))

(defn run-chrome-event-loop! [chrome-event-channel]
  (go-loop [event-num 1]
           (when-let [event (<! chrome-event-channel)]
             (process-chrome-event event-num event)
             (recur (inc event-num)))))

(defn boot-chrome-event-loop! []
  (let [chrome-event-channel (make-chrome-event-channel (chan))]
    (runtime/tap-all-events chrome-event-channel)
    (run-chrome-event-loop! chrome-event-channel)))

; -- main entry point -------------------------------------------------------------------------------------------------------

(defn init! []
  (boot-chrome-event-loop!))