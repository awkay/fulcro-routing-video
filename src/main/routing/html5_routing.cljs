(ns routing.html5-routing
  (:require
    [fulcro.client.routing :as r]
    [fulcro.client.mutations :as m :refer [defmutation]]
    [routing.ui.components :as comp]
    [pushy.core :as pushy]
    [bidi.verbose :refer [branch leaf param]]
    [bidi.bidi :as bidi]
    [fulcro.client.primitives :as om]
    [fulcro.client.logging :as log]
    [fulcro.client.data-fetch :as df]))

(def app-routing-tree
  (r/routing-tree
    (r/make-route :index [(r/router-instruction :top-router [:home-page :single])])
    (r/make-route :alternate [(r/router-instruction :top-router [:screen2 :single])])
    (r/make-route :report [(r/router-instruction :top-router [:reports :single])
                           (r/router-instruction :report-router [:report/by-id :param/id])])
    (r/make-route :summary [(r/router-instruction :top-router [:reports :single])
                            (r/router-instruction :report-router [:summary/by-id :param/id])])))

(def valid-handlers (-> (get app-routing-tree r/routing-tree-key) keys set))

;; To keep track of the global HTML5 pushy routing object
(def history (atom nil))

;; To indicate when we should turn on URI mapping. This is so you can use with devcards (by turning it off)
(defonce use-html5-routing (atom true))

(def url-route-mappings
  "The bidi routing map for the application. The leaf keywords are the routing tree names. Parameters
  in the route are available for use in the routing algorithm as :param/param-name."
  (branch
    "/"
    (leaf "" :index)
    (leaf "index.html" :index)
    (leaf "alternate" :alternate)
    (branch "reports/" (param :id)
      (leaf "" :report))
    (branch "summary/" (param :id)
      (leaf "" :summary))))

(comment
  url-route-mappings
  (bidi/match-route url-route-mappings "/")
  (bidi/match-route url-route-mappings "/alternate")
  (bidi/match-route url-route-mappings "/reports/20"))

(defn invalid-route?
  "Returns true if the given keyword is not a valid location in the routing tree."
  [kw]
  (or (nil? kw) (not (contains? valid-handlers kw))))

(defn redirect*
  "Use inside of mutations to generate a URI redirect to a different page than you are on. Honors setting of use-html5-history.
  Use the plain function `nav-to!` for UI-level navigation."
  [state-map {:keys [handler route-params] :as bidi-match}]
  (let [path (apply bidi/path-for url-route-mappings handler (flatten (seq route-params)))]
    (pushy/set-token! @history path)
    state-map))

(defn set-route!*
  "Implementation of choosing a particular bidi match. Used internally by the HTML5 history event implementation.
  Updates the UI only, unless the URI is invalid, in which case it redirects the UI and possibly generates new HTML5
  history events."
  [state-map {:keys [handler] :as bidi-match}]
  (if (invalid-route? handler)
    (redirect* state-map {:handler :index})
    (r/update-routing-links state-map bidi-match)))

(defn- ensure-report-loaded [{:keys [state] :as env} id]
  (when-not (get-in @state [:report/by-id id])
    (swap! state assoc-in [:report/by-id id] {:db/id id :report/name "Loading..."})
    (df/load-action env [:report/by-id id] comp/Report {:marker false})))

(defn- ensure-summary-loaded [{:keys [state] :as env} id]
  (when-not (get-in @state [:summary/by-id id])
    (swap! state assoc-in [:summary/by-id id] {:db/id id :summary/name "Loading..."})
    (df/load-action env [:summary/by-id id] comp/Summary {:marker false})))

(defn- ensure-integer
  "Helper for set-integer!, use that instead. It is recommended you use this function only on UI-related
  data (e.g. data that is used for display purposes) and write clear top-level transactions for anything else."
  [v]
  (let [rv (js/parseInt v)]
    (if (js/isNaN v) 0 rv)))

(defn- ensure-routeable
  "Make sure a placeholder object is in place, and issue a load-action if something is missing"
  [env {:keys [handler route-params] :as bidi-match}]
  (let [{:keys [id]} route-params
        id (ensure-integer id)]
    (cond
      (= :report handler) (ensure-report-loaded env id)
      (= :summary handler) (ensure-summary-loaded env id))))

(defmutation set-route!
  "Mutation:

  Set the route to the given bidi match. Checks to make sure the user is allowed to do so (are
  they logged in?). Sends them to the login screen if they are not logged in. This does NOT update the URI."
  [bidi-match]
  (action [{:keys [state] :as env}]
    (ensure-routeable env bidi-match)                       ; make sure loadable things at least look sane
    (swap! state set-route!* bidi-match))
  (remote [env] (df/remote-load env)))

(defn nav-to!
  "Run a navigation mutation from the UI, but make sure that HTML5 routing is correctly honored so the components can be
  used as an app or in devcards. Use this in nav UI links instead of href or transact. "
  ([component page] (nav-to! component page {}))
  ([component page route-params]
   (if (and @history @use-html5-routing)
     (let [path (apply bidi/path-for url-route-mappings page (flatten (seq route-params)))]
       (pushy/set-token! @history path))
     (om/transact! component `[(set-route! ~{:handler page :route-params route-params}) :pages]))))

(defn start-routing [app-root]
  (when (not @history)
    (let [set-route! (fn [match]
                       ; Delay. history events should happen after a tx is processed, but a set token could happen during.
                       ; Time doesn't matter. This thread has to let go of the CPU before timeouts can process.
                       (js/setTimeout #(om/transact! app-root `[(set-route! ~match)]) 10))]
      (reset! history (pushy/pushy set-route! (partial bidi/match-route url-route-mappings) :identity-fn (fnil identity {:handler :invalid})))
      (pushy/start! @history))))
