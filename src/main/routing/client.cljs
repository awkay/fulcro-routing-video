(ns routing.client
  (:require [fulcro.client :as fc]
            [fulcro.server :as server]
            [routing.html5-routing :as r]
            [routing.ui.components :as comp]
            [routing.ui.root :as root]
            [fulcro.client.data-fetch :as df]
            [fulcro.client.primitives :as prim]))

(defonce app (atom nil))

(defn mount []
  (reset! app (fc/mount @app root/Root "app")))

(defn start []
  (mount))

(defn ^:export init []
  (reset! app (fc/new-fulcro-client
                :networking (server/new-server-emulator (server/fulcro-parser) 1000)
                :started-callback
                (fn [{:keys [reconciler] :as app}]
                  (r/start-routing (prim/app-root reconciler)))))
  (start))
