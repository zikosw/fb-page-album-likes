(ns ^:figwheel-no-load fb-page-album.dev
  (:require
    [fb-page-album.core :as core]
    [devtools.core :as devtools]))


(enable-console-print!)

(devtools/install!)

(core/init!)
