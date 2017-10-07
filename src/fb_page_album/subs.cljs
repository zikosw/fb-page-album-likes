(ns fb-page-album.subs
  (:require [re-frame.core :as rf]))

(comment
  @(rf/subscribe [:page/get-albums "cupemag"]))

(rf/reg-sub
  :page/get-albums
  (fn [db [_ page-id]]
    (get-in db [page-id :albums :data])))
