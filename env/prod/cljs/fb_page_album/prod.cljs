(ns fb-page-album.prod
  (:require
    [fb-page-album.core :as core]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))

(core/init!)
