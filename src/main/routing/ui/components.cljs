(ns routing.ui.components
  (:require
    [fulcro.server :as server :refer [defquery-root defquery-entity]]
    [fulcro.client.dom :as dom]
    [fulcro.client.mutations :as m :refer [defmutation]]
    [fulcro.client.primitives :as prim :refer [defsc]]
    [fulcro.client.routing :as r :refer [defrouter]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Server queries
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def report-db {1 {:db/id 1 :report/name "Statistics"}
                2 {:db/id 2 :report/name "Raw Sales"}})

(def summary-db {1 {:db/id 1 :summary/name "Year to Date Revenue"}})

(defquery-entity :report/by-id
  (value [env id params]
    (get report-db id)))

(defquery-entity :summary/by-id
  (value [env id params]
    (get summary-db id)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Common screen definitions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defsc HomePage [this {:keys [screen] :as props}]
  {:query         [:screen]
   :initial-state {:screen :home-page}
   :ident         (fn [] [(:screen props) :single])}
  (dom/div nil "INDEX PAGE"))

(def ui-home-page (prim/factory HomePage))

(defsc Screen2 [this {:keys [screen] :as props}]
  {:query         [:screen]
   :initial-state {:screen :screen2}
   :ident         (fn [] [(:screen props) :single])}
  (dom/div nil "Alternate screen"))

(def ui-screen-2 (prim/factory Screen2))

(defsc Report [this {:keys [db/id report/name]}]
  {:query [:db/id :kind :report/by-id :report/name]
   :ident [:report/by-id :db/id]}
  (dom/div nil
    (dom/h4 nil name)
    (dom/div nil "Various tables and charts...")))

(defsc Summary [this {:keys [db/id summary/name]}]
  {:query [:db/id :kind :summary/by-id :summary/name]
   :ident [:summary/by-id :db/id]}
  (dom/div nil
    (dom/h4 nil name)
    (dom/div nil "Summary of various things...")))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Using DOM switching
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmutation dom-goto [{:keys [page]}]
  (action [{:keys [state]}]
    (swap! state assoc :root/current-page page)))

(defsc RootDOMRouting [this {:keys [root/current-page root/index root/screen2]}]
  {:query         [:root/current-page
                   {:root/index (prim/get-query HomePage)}
                   {:root/screen2 (prim/get-query Screen2)}]
   :initial-state {:root/current-page :index
                   :root/index        {}
                   :root/screen2      {}}}
  (dom/div nil
    (dom/button #js {:onClick #(prim/transact! this `[(dom-goto {:page :index})])} "Home")
    (dom/button #js {:onClick #(prim/transact! this `[(dom-goto {:page :screen2})])} "Alternate")
    (case current-page
      :index (ui-home-page index)
      :screen2 (ui-screen-2 screen2))))






;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; A manual union component for routing
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defsc RouteUnion [this {:keys [screen] :as props}]
  {:ident         (fn [] [(:screen props) (or (:id props) :single)])
   :initial-state (fn [p] (prim/get-initial-state HomePage {}))
   :query         (fn [] {:home-page (prim/get-query HomePage)
                          :screen2   (prim/get-query Screen2)})}
  (case screen
    :home-page (ui-home-page props)
    :screen2 (ui-screen-2 props)
    (dom/div nil "INVALID ROUTE!")))

(def ui-route-union (prim/factory RouteUnion))

(defmutation goto [{:keys [page]}]
  (action [{:keys [state]}]
    (swap! state assoc :root/router [page :single])))

(defsc ManualUnionRoot [this {:keys [root/router]}]
  {:query         [{:root/router (prim/get-query RouteUnion)}]
   :initial-state (fn [p]
                    {:root/router (prim/get-initial-state RouteUnion {})})}
  (dom/div nil
    (dom/button #js {:onClick #(prim/transact! this `[(goto {:page :home-page})])} "Home")
    (dom/button #js {:onClick #(prim/transact! this `[(goto {:page :screen2})])} "Alternate")
    (ui-route-union router)))






;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Wrapping the Union in a Control Component
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defsc Router [this {:keys [current-route]}]
  {:query         [:id {:current-route (prim/get-query RouteUnion)}]
   :initial-state (fn [{:keys [id]}] {:id id :current-route (prim/get-initial-state RouteUnion {})})
   :ident         [:router/by-id :id]}
  (ui-route-union current-route))

(def ui-router (prim/factory Router {:keyfn :id}))

(defmutation set-route [{:keys [id screen]}]
  (action [{:keys [state]}]
    (swap! state assoc-in [:router/by-id id :current-route] [screen :single])))

(defsc ComponentRoot [this {:keys [root/router]}]
  {:query         [{:root/router (prim/get-query Router)}]
   :initial-state (fn [p] {:root/router (prim/get-initial-state Router {:id :my-router})})}
  (dom/div nil
    (dom/button #js {:onClick #(prim/transact! this `[(set-route {:id :my-router :screen :home-page})])} "Home")
    (dom/button #js {:onClick #(prim/transact! this `[(set-route {:id :my-router :screen :screen2})])} "Alternate")
    (ui-router router)))






;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Using defrouter
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defrouter TopRouter :top-router
  (ident [this props] [(:screen props) :single])
  :home-page HomePage
  :screen2 Screen2)

(def ui-top-router (prim/factory TopRouter))

(def routing-tree (r/routing-tree
                    (r/make-route :index [(r/router-instruction :top-router [:home-page :single])])
                    (r/make-route :alternate [(r/router-instruction :top-router [:screen2 :single])])))

(defsc Root [this {:keys [root/router]}]
  {:query         [{:root/router (prim/get-query TopRouter)}]
   :initial-state (fn [p]
                    (merge
                      routing-tree
                      {:root/router (prim/get-initial-state TopRouter {})}))}
  (dom/div nil
    (dom/button #js {:onClick #(prim/transact! this `[(r/route-to {:handler :index})])} "Home")
    (dom/button #js {:onClick #(prim/transact! this `[(r/route-to {:handler :alternate})])} "Alternate")
    (ui-top-router router)))

