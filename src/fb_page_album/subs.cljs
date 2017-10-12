(ns fb-page-album.subs
  (:require [re-frame.core :as rf]))

(comment
  @(rf/subscribe [:page/get-albums]))

(rf/reg-sub
  :page/get-albums
  (fn [db _]
    (->> (get db :albums)
         (map second)
         (sort-by :likes)
         reverse)))

(rf/reg-sub
  :api/get-access-token
  (fn [db _]
    (get db :api/access-token)))
