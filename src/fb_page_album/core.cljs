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
            [fb-page-album.config :as config]
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

(defn fb-login-btn [attr child]
  [:div.panel.panel-default
   [:div.panel-body
     [:h5 "login to continue"]
     [:img
       {:src "/img/fblogin.png"
        :height 30
        :style {:cursor "pointer"}
        :on-click (fn []
                    (.login js/FB #(rf/dispatch [:fb/login %])))}]]])
(defn fb-logout-btn [username]
  [:div.panel.panel-default
   [:div.panel-body
     (str "Logged in as " username " ")
     [:button.btn.btn-danger {:on-click #(rf/dispatch [:fb/logout])} "Logout"]]])

(defn url-guide []
  [:p "for page albums go to "
   [:a {:href "/#/page/<token>/<page-id>"} "/#/page/<token>/<page-id>"]
   " - eg. "
   [:a {:href "/#/page/USER-TOKEN/IRoamAlone"} "/#/page/EXAMPLE-USER-TOKEN/IRoamAlone"]]
  [:p "for page photos go to "
   [:a {:href "/#/photos/<token>/<page-id>"} "/#/photos/<token>/<page-id>"]])

;; Views
(defn home-page []
  (let [albums @(rf/subscribe [:page/get-albums])
        logged-in? @(rf/subscribe [:api/get-access-token])
        username @(rf/subscribe [:fb/get-username])]
    [:div.container
      [:h2 "Welcome"]
      (if (not logged-in?)
        [:div
          [fb-login-btn]
          [:div
           [:h3 "Screenshot Example"]
           [:img {:src "/img/screenshot.jpg"}]]]
        [:div
          [fb-logout-btn username]
          [:div.panel.panel-default
            [:div.panel-body
              [:div {:style {:margin-bottom 10}}
               [:img {:src "/img/page-id.jpg"}]]
              [:div.form-inline
                [:input.form-control
                 {:type "text"
                  :style {:width 250}
                  :placeholder "Page ID, eg. bnk48official.cherprang"
                  :on-change #(rf/dispatch [:api/set-page (-> % .-target .-value)])}]
                [:button.btn.btn-default
                 {:type "button"
                  :on-click #(rf/dispatch [:page/get-albums @(rf/subscribe [:api/get-page])])}
                 "Get Albums!"]
                [:button.btn.btn-default
                 {:type "button"
                  :on-click #(rf/dispatch [:page/get-photos @(rf/subscribe [:api/get-page])])}
                 "Get Photos!"]]]]

          [:div {:style {:padding-top 10
                         :display "flex"
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
                                [:p (str name " ♥ " likes)]]])]])]))

;; -------------------------
;; Initialize app
(defn main-panel []
  (r/create-class
    {:component-did-mount #(rf/dispatch [:fb/init-user])
     :reagent-render home-page}))

(defn dev-setup []
  (when config/debug?
    (enable-re-frisk!)
    (println "dev mode")))

(defn mount-root []
  (r/render [main-panel] (.getElementById js/document "app")))

(defn init! []
  (app-routes)
  (rf/dispatch-sync [:initialize])
  (dev-setup)
  (mount-root))
