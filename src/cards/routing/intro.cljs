(ns routing.intro
  (:require [devcards.core :as rc]
            [routing.ui.components :as comp]
            [fulcro.server :as server :refer [defquery-root defquery-entity]]
            [fulcro.client.dom :as dom]
            [fulcro.client.cards :refer [defcard-fulcro]]
            [fulcro.client.mutations :as m :refer [defmutation]]
            [fulcro.client.primitives :as prim :refer [defsc]]
            [fulcro.client.routing :as r :refer [defrouter]]))

(defcard-fulcro dom-routing
  comp/RootDOMRouting
  {}
  {:inspect-data true})

(defcard-fulcro manual-union-routing
  comp/ManualUnionRoot
  {}
  {:inspect-data true})

(defcard-fulcro manually-built-router-component
  comp/ComponentRoot
  {}
  {:inspect-data true})

(defcard-fulcro prebuilt-router-card
  comp/Root
  {}
  {:inspect-data true
   :fulcro       {:started-callback (fn [app]
                                      )}})
