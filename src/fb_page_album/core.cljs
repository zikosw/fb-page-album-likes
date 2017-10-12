(ns fb-page-album.core
  (:import goog.History) 
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [secretary.core :refer [defroute]])
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [re-frisk.core :refer [enable-re-frisk!]]
            [secretary.core :as secretary]
            [goog.events :as events]
            [goog.history.EventType :as EventType] 
            [cljs-http.client :as http]
            [cljs.core.async :refer [<! >! chan]]
            [clojure.pprint :refer [pprint]]
            [fb-page-album.events]
            [fb-page-album.subs]
            [fb-page-album.db]))

;; -------------------------
(defn hook-browser-navigation! []
  (doto (History.)
    (events/listen
      EventType/NAVIGATE
      (fn [event]
        (secretary/dispatch! (.-token event))))
    (.setEnabled true)))

(defn app-routes []
  (secretary/set-config! :prefix "#")
  (defroute "/" []
    (rf/dispatch [:routes/set-active-panel :home-panel]))

  (defroute page-route "/page/:token/:page-id" {token :token
                                                page-id :page-id}
    (do
      (rf/dispatch [:routes/set-active-panel :page-panel])
      (rf/dispatch [:api/set-access-token token])
      ;(prn :route-page-id page-id)))
      (rf/dispatch [:page/get-albums page-id])))

  (hook-browser-navigation!))

;; Views

(defn home-page []
  (let [albums @(rf/subscribe [:page/get-albums])]
    [:div
     [:h2 "Welcome"]
     [:h5 "/#/page/<token>/<page-id>"]
     [:a {:href "https://developers.facebook.com/tools/accesstoken/"} "Get a token here"]
     [:hr]
     [:div {:style {:display "flex"
                    :flex-wrap "wrap"}}
      (for [a albums
            :let [id (:id a)
                  name (:name a)
                  likes (:likes a)
                  cover (:cover a)]]
        ^{:key (:id a)} [:div {:style {:flex 1}}
                         [:a {:href (str "http://fb.com/" id) :target "_blank"}
                           [:img {:src cover :height 200}]]
                         [:div
                          [:p (str name " ♥ " likes)]]])]
     [:div
      [:input {:type "text" :on-change #(rf/dispatch [:api/set-access-token (-> % .-target .-value)])}]]]))

;; ---------- APP -----------
(defn ex-fetch []
  (go (let [response (<! (http/get "https://api.github.com/users"
                                   {:with-credentials? false
                                    :query-params {"since" 135}}))
            res-status (:status response)
            body (:body response)]
        (prn :status res-status)
        (prn :users (map :login body)))))

;; -------------------------
;; Initialize app

(defn mount-root []
  (r/render [home-page] (.getElementById js/document "app")))

(defn init! []
  (app-routes)
  (rf/dispatch-sync [:initialize])
  (enable-re-frisk!)
  (mount-root))
