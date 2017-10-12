(ns fb-page-album.events
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [re-frame.core :as rf]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<! >! chan]]
            [clojure.pprint :refer [pprint]]))

(rf/reg-event-db              ;; sets up initial application state
  :initialize                 ;; usage:  (dispatch [:initialize])
  (fn [_ _]
    {:active-panel :home}))

(comment
  (reset! re-frame.db/app-db {})
  (rf/subscribe [:api/get-access-token]))

(rf/reg-event-db
  :api/set-access-token
  (fn [db [_ token]]
    (assoc db :api/access-token token)))

(def fb-graph-url "https://graph.facebook.com/")

(defn gen-url [id]
  (str fb-graph-url id))

(defn page-url [id]
  (gen-url id))

(defn page-album-url [id]
  (str (page-url id) "/albums"))

(defn album-url [id]
  (gen-url id))

(defn likes [resource-url]
  (str resource-url "/likes?summary=true"))

(def album-req
  "likes.summary(true).limit(0),photos.limit(1){images},name,description")

(defn photo-url [album-id]
  (str fb-graph-url album-id "/photos"))

(def photo-req
  {:fields "event,link,picture,place,target,updated_time"})

(defn get-photos [album]
  (let [img (get-in album [:photos :data])]
    (map #(get-in % [:images 0 :source]) img)))

(defn get-likes [album]
  (get-in album [:likes :summary :total_count]))

(comment
  (let [d (get-in data [:data 1])]
    [(get-photos d) (get-likes d)]))

(defn fetch-page-album [page-id after]
  (let [albums (chan)
        access-token (rf/subscribe [:api/get-access-token])]
    (go (let [response (<! (http/get (page-album-url page-id)
                                     {:with-credentials? false
                                      :query-params {"access_token" @access-token
                                                     "limit" 50
                                                     "fields" album-req
                                                     "after" after}}))  ;; MAX is 50
                                                    ;; TODO: Implement paging
                                                    ;;"after" "MTM5NjY4NjU4NzEwMzQzNQZDZD"}}))
              res-status (:status response)
              body (:body response)
              paging (:paging body)
              data (:data body)
              mapped-a (map
                         (fn [a]
                           {:id (:id a)
                            :name (:name a)
                            :desc (:description a)
                            :likes (get-likes a)
                            :cover (first (get-photos a))})
                         data)]
          (prn :status res-status)
          (prn :p paging)
          (prn :d (count data))
          (prn :data mapped-a)
          (>! albums mapped-a)))
    albums))

(rf/reg-fx
  :page/fetch-albums
  (fn [{:keys [page-id on-success on-failed]}]
    ;; TODO: Implement paging loop
    (go
      (on-success (<! (fetch-page-album page-id nil))))))

(comment
  (rf/dispatch [:page/get-albums "IRoamAlone"])
  (rf/dispatch [:page/get-albums "bnk48unclefan"]))

(rf/reg-event-fx
  :page/get-albums
  (fn [db [_ page-id]]
    (prn :get-albums page-id)
    {:page/fetch-albums {:page-id page-id
                         :on-success #(rf/dispatch [:page/set-albums page-id %])}}))

(rf/reg-event-fx
  :page/set-albums
  (fn [{:keys [db]} [_ page-id albums]]
    {:db (assoc db :albums albums)}))

;; ROUTES

(rf/reg-event-db
 :routes/set-active-panel
 (fn [db [_ panel]]
   (assoc db :active-panel panel)))
