(ns fb-page-album.subs
  (:require [re-frame.core :as rf]))

(comment
  (->> (get-in @re-frame.db/app-db [:albums])
       (sort-by :likes))
  @(rf/subscribe [:page/get-albums "cupemag"]))



(rf/reg-sub
  :page/get-albums
  (fn [db _]
    (->> (get-in db [:albums])
         ;(map second)
         (sort-by :likes)
         reverse)))

(rf/reg-sub
  :api/get-access-token
  (fn [db _]
    (get db :api/access-token)))
