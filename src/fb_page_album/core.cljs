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
      (rf/dispatch [:page/get-albums page-id])))

  (defroute photo-route "/photos/:token/:page-id" {token :token
                                                   page-id :page-id}
            (do
              (rf/dispatch [:routes/set-active-panel :page-panel])
              (rf/dispatch [:api/set-access-token token])
              (rf/dispatch [:page/get-photos page-id])))

  (hook-browser-navigation!))

;; Views

(defn home-page []
  (let [albums @(rf/subscribe [:page/get-albums])]
    [:div
     [:h2 "Welcome"]

     [:p "for page albums go to "
      [:a {:href "/#/page/<token>/<page-id>"} "/#/page/<token>/<page-id>"]
      " - eg. "
      [:a {:href "/#/page/USER-TOKEN/IRoamAlone"} "/#/page/EXAMPLE-USER-TOKEN/IRoamAlone"]]

     [:p "for page photos go to "
       [:a {:href "/#/photos/<token>/<page-id>"} "/#/photos/<token>/<page-id>"]]
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
                          [:p (str name " ♥ " likes)]]])]]
   #_[:div
      [:input {:type "text" :on-change #(rf/dispatch [:api/set-access-token (-> % .-target .-value)])}]]))

;; -------------------------
;; Initialize app

(defn mount-root []
  (r/render [home-page] (.getElementById js/document "app")))

(defn init! []
  (app-routes)
  (rf/dispatch-sync [:initialize])
  (enable-re-frisk!)
  (mount-root))
