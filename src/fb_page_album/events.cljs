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

(comment
  (reset! re-frame.db/app-db {})
  (rf/subscribe [:api/get-page-id]))

(rf/reg-event-db
  :api/set-access-token
  (fn [db [_ token]]
    (assoc db :api/access-token token)))

(rf/reg-event-db
  :api/set-page
  (fn [db [_ page-id]]
    (assoc db :api/page-id page-id)))

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



(defn get-photos [images]
  (get-in images [:images 0 :source]))

(def t
  {:likes {:data [], :summary {:total_count 43, :can_like false, :has_liked false}}, :images [{:height 639, :source "https://scontent.xx.fbcdn.net/v/t1.0-9/21231906_1449885198432870_4276172318398922158_n.jpg?oh=b9bcd8b6993111b77c1a46ba1521959a&oe=5A3B42B5", :width 960} {:height 600, :source "https://scontent.xx.fbcdn.net/v/t1.0-0/p600x600/21231906_1449885198432870_4276172318398922158_n.jpg?oh=e06bbe0a12914a6716f0d988335a5b7a&oe=5A79FC87", :width 901} {:height 480, :source "https://scontent.xx.fbcdn.net/v/t1.0-0/p480x480/21231906_1449885198432870_4276172318398922158_n.jpg?oh=4163bc99a4915248632b4bd5b0153488&oe=5A79E8AE", :width 721} {:height 320, :source "https://scontent.xx.fbcdn.net/v/t1.0-0/p320x320/21231906_1449885198432870_4276172318398922158_n.jpg?oh=ae6f0e293371886ba55f499670311d0a&oe=5A7C04E9", :width 480} {:height 540, :source "https://scontent.xx.fbcdn.net/v/t1.0-0/p180x540/21231906_1449885198432870_4276172318398922158_n.jpg?oh=e5dde8eb45317527e10087d2834d5cb1&oe=5A6D21A2", :width 811} {:height 130, :source "https://scontent.xx.fbcdn.net/v/t1.0-0/p130x130/21231906_1449885198432870_4276172318398922158_n.jpg?oh=348d21adff28d2b9b4d05d1e76bedc20&oe=5A6EF50E", :width 195} {:height 225, :source "https://scontent.xx.fbcdn.net/v/t1.0-0/p75x225/21231906_1449885198432870_4276172318398922158_n.jpg?oh=d4f492cb7092fc8f9a7eec1a4d55c175&oe=5A6F45B9", :width 338}], :id "1449885198432870"})


(defn get-photos-in-album [album]
  (let [images (get-in album [:photos :data])]
    (map #(get-photos %) images)))

(defn get-likes [album]
  (get-in album [:likes :summary :total_count]))

(comment
  (let [d (get-in data [:data 1])]
    [(get-photos-in-album d) (get-likes d)]))

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
                            :cover (first (get-photos-in-album a))})
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

(defn fetch-page-photos [page-id after]
  (let [photos (chan)
        access-token (rf/subscribe [:api/get-access-token])]
    (go (let [response (<! (http/get (photo-url page-id)
                                     {:with-credentials? false
                                      :query-params {"access_token" @access-token
                                                     "limit" 50
                                                     "fields" "likes.summary(true).limit(0),images,name"
                                                     "type" "uploaded"
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
                            :likes (get-likes a)
                            :cover (get-photos a)})
                         data)]
          (prn :status res-status)
          (prn :p paging)
          (prn :d (count data))
          ;(prn :data mapped-a)
          (doseq [p mapped-a]
            (prn :ppp p))
          (>! photos mapped-a)))
    photos))

(comment
  (fetch-page-photos "bnk48official.cherprang" nil))

(rf/reg-fx
  :page/fetch-photos
  (fn [{:keys [page-id on-success on-failed]}]
    ;; TODO: Implement paging loop
    (go
      (on-success (<! (fetch-page-photos page-id nil))))))

(comment
  (rf/dispatch [:page/get-photos "IRoamAlone"])
  (rf/dispatch [:page/get-photos "bnk48official.cherprang"])
  (rf/dispatch [:page/get-photos "bnk48unclefan"]))

(rf/reg-event-fx
  :page/get-photos
  (fn [db [_ page-id]]
    (prn :get-photos page-id)
    {:page/fetch-photos {:page-id page-id
                         :on-success #(rf/dispatch [:page/set-photos page-id %])}}))

(rf/reg-event-fx
  :page/set-photos
  (fn [{:keys [db]} [_ page-id albums]]
    {:db (assoc db :albums albums)}))

;; ROUTES

(rf/reg-event-db
 :routes/set-active-panel
 (fn [db [_ panel]]
   (assoc db :active-panel panel)))
