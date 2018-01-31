(ns routing.ui.root
  (:require
    [fulcro.client.mutations :as m]
    [fulcro.client.data-fetch :as df]
    translations.es                                         ; preload translations by requiring their namespace. See Makefile for extraction/generation
    [routing.ui.components :refer [HomePage Screen2 Report Summary]]
    [fulcro.client.dom :as dom]
    [routing.api.mutations :as api]
    [fulcro.client.primitives :as prim :refer [defsc]]
    [fulcro.client.routing :as r :refer [defrouter]]
    [fulcro.i18n :refer [tr trf]]
    [routing.html5-routing :as routing]))

(defrouter ReportRouter :report-router
  (ident [this props] [(if (contains? props :report/name) :report/by-id :summary/by-id)
                       (:db/id props)])
  :report/by-id Report
  :summary/by-id Summary)

(def ui-report-router (prim/factory ReportRouter))

(defsc ReportHome [this {:keys [report] :as props}]
  {:query         [:screen {:report (prim/get-query ReportRouter)}]
   :initial-state {:screen :reports
                   :report {}}
   :ident         (fn [] [(:screen props) :single])}
  (dom/div nil
    (dom/h2 nil "Report Main")
    (ui-report-router report)))

(def ui-report (prim/factory Report {:keyfn :db/id}))

(defrouter TopRouter :top-router
  (ident [this props] [(:screen props) :single])
  :home-page HomePage
  :screen2 Screen2
  :reports ReportHome)

(def ui-top-router (prim/factory TopRouter))

(defsc Root [this {:keys [root/router]}]
  {:query         [{:root/router (prim/get-query TopRouter)}]
   :initial-state (fn [p]
                    (merge
                      routing/app-routing-tree
                      {:root/router (prim/get-initial-state TopRouter {})}))}
  (dom/div nil
    (dom/button #js {:onClick #(routing/nav-to! this :index)} "Home")
    (dom/button #js {:onClick #(routing/nav-to! this :alternate)} "Alternate")
    (dom/button #js {:onClick #(routing/nav-to! this :report {:id 1})} "Report 1")
    (dom/button #js {:onClick #(routing/nav-to! this :summary {:id 1})} "Summary 1")
    (ui-top-router router)))

