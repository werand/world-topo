(ns world-topo.core
  (:require [cljsjs.d3]
            [cljsjs.topojson]))

(enable-console-print!)

(def world-promise (js/d3.json "world.json"))
(def places-promise (js/d3.json "places.json"))

(def height 1000)
(def width 960)

(def proj (-> js/d3
              (.geoOrthographic)
              (.translate (into-array (/ width 2) (/ height 2)))
              (.clipAngle 90)
              (.scale 420)))

(def sky (-> js/d3
             (.geoOrthographic)
             (.translate (into-array (/ width 2) (/ height 2)))))

(def path (-> js/d3
              (.geoPath)
              (.projection proj)
              (.pointRadius 2)))

#_(def swoosh (-> js/d3
                (.line)
                (.x (fn [d] (first d)))
                #_(.y (fn [d] (second d)))
                (.interpolate "cardinal")
                (.tension .0)))

(println "This text is printed from src/world-topo/core.cljs. Go ahead and edit it and see reloading in action. yeah")

;; define your app data so that it doesn't get over-written on reload

#_(defonce app-state (atom {:text "Hello world!"}))


#_(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)

(defn append-svg []
  (-> js/d3
      (.select "#graph")
      (.append "svg")
      (.attr "height" height)
      (.attr "width" width)
      #_(.on "mousedown" mousedown)))

(defn remove-svg []
  (-> js/d3
      (.selectAll "#graph svg")
      (.remove)))

(defn render [svg world places]
  #_(println (js/topojson.feature world (.. world -objects -land)))
  #_(println (.-land (.-objects world)))
  (let [ocean-fill (-> svg
                       (.append "defs")
                       (.append "radialGradient")
                       (.attr "id" "ocean_fill")
                       (.attr "cx" "75%")
                       (.attr "cy" "25%"))]
    (-> ocean-fill
        (.append "stop")
        (.attr "offset" "5%")
        (.attr "stop-color" "#fff"))
    (-> ocean-fill
        (.append "stop")
        (.attr "offset" "100%")
        (.attr "stop-color" "#ababab")))
  (let [globe-highlight (-> svg
                            (.append "defs")
                            (.append "radialGradient")
                            (.attr "id" "globe_highlight")
                            (.attr "cx" "75%")
                            (.attr "cy" "25%"))]
    (-> globe-highlight
        (.append "stop")
        (.attr "offset" "5%")
        (.attr "stop-color" "#ffd")
        (.attr "stop-opacity" "0.6"))
    (-> globe-highlight
        (.append "stop")
        (.attr "offset" "100%")
        (.attr "stop-color" "#ba9")
        (.attr "stop-opacity" "0.2")))
  (let [globe-shading (-> svg
                          (.append "defs")
                          (.append "radialGradient")
                          (.attr "id" "globe_shading")
                          (.attr "cx" "55%")
                          (.attr "cy" "45%"))]
    (-> globe-shading
        (.append "stop")
        (.attr "offset" "30%")
        (.attr "stop-color" "#fff")
        (.attr "stop-opacity" "0"))
    (-> globe-shading
        (.append "stop")
        (.attr "offset" "100%")
        (.attr "stop-color" "#505962")
        (.attr "stop-opacity" "0.3")))
  (let [drop-shadow (-> svg
                        (.append "defs")
                        (.append "radialGradient")
                        (.attr "id" "drop_shadow")
                        (.attr "cx" "50%")
                        (.attr "cy" "50%"))]
    (-> drop-shadow
        (.append "stop")
        (.attr "offset" "20%")
        (.attr "stop-color" "#000")
        (.attr "stop-opacity" "0.5"))
    (-> drop-shadow
        (.append "stop")
        (.attr "offset" "100%")
        (.attr "stop-color" "#000")
        (.attr "stop-opacity" "0")))
  (-> svg
      (.append "ellipse")
      (.attr "cx" 440)
      (.attr "cy" 450)
      (.attr "rx" (* 0.9 (.scale proj)))
      (.attr "ry" (* 0.25 (.scale proj)))
      (.attr "class" "noclicks")
      (.style "fill" "url(#drop_shadow)"))
  (-> svg
      (.append "circle")
      (.attr "cx" (/ width 2))
      (.attr "xy" (/ height 2))
      (.attr "r" (.scale proj))
      (.attr "class" "noclicks")
      (.style "fill" "url(#ocean_fill)"))
  (-> svg
      (.append "path")
      (.datum (js/topojson.feature world (.. world -objects -land)))
      (.attr "class" "land noclicks")
      (.attr "d" path))
  (-> svg
      (.append "circle")
      (.attr "cx" (/ width 2))
      (.attr "xy" (/ height 2))
      (.attr "r" (.scale proj))
      (.attr "class" "noclicks")
      (.style "fill" "url(#globe_highlight)"))
  (-> svg
      (.append "circle")
      (.attr "cx" (/ width 2))
      (.attr "xy" (/ height 2))
      (.attr "r" (.scale proj))
      (.attr "class" "noclicks")
      (.style "fill" "url(#globe_shading)"))
;; Refresh
  (-> svg
      (.selectAll ".land")
      (.attr "d" path))
  (-> svg
      (.selectAll ".point")
      (.attr "d" path))
  )

(defn ^:export main []
  (let [svg (append-svg)]
    (.then (js/Promise.all #js [world-promise places-promise])
           #(render svg (first %) (second %)))))

(defn on-js-reload []
  (println "on-js-reload")
  (remove-svg)
  (main))
