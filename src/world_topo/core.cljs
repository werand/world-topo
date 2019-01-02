(ns world-topo.core
  (:require [cljsjs.d3]
            [cljsjs.topojson]))

(enable-console-print!)

(defonce world-promise (js/d3.json "world.json"))
(defonce places-promise (js/d3.json "places.json"))

(defonce height 1000)
(defonce width 960)

(defonce m0 (atom nil))
(defonce o0 (atom [0 0 0]))

(defn create-arcs [places]
  (clj->js
   (for [a places.features b places.features :when (not= a b)]
     {:type "Feature"
      :geometry {:type "LineString"
                 :coordinates [(.. a -geometry -coordinates)
                               (.. b -geometry -coordinates)]}})))

(defn mousedown [proj]
  (reset! m0 [js/d3.event.pageX js/d3.event.pageY])
  (reset! o0 (.rotate proj))
  #_(println (str "mousedown" @o0))
  (js/d3.event.preventDefault))

(defn mouseup []
  (reset! m0 nil))

(defn refresh [svg path]
  (-> svg
      (.selectAll ".land")
      (.attr "d" path))
  (-> svg
      (.selectAll ".point")
      (.attr "d" path))
  (-> svg
      (.selectAll ".arc")
      (.attr "d" path)))

(defn mousemove [proj path svg]
  (when-not (nil? @m0)
    #_(println "mousemove "(str o0 m0))
    (let [m1-x js/d3.event.pageX
          m1-y js/d3.event.pageY
          m0-x (first @m0)
          m0-y (second @m0)
          o0-x (first @o0)
          o0-y (second @o0)
          o1-x (+ o0-x (/ (- m1-x m0-x) 6))
          o1-y (+ o0-y (/ (- m0-y m1-y) 6))
          o1 [o1-x
              (cond
                (> o1-y 30)   30
                (< o1-y -30) -30
                :else o1-y)]]
      (.rotate proj (clj->js o1))
      #_(.rotate sky o1)))
  (refresh svg path))

(defn append-svg [proj]
  (println "append-svg")
  (-> js/d3
      (.select "#graph")
      (.append "svg")
      (.attr "height" height)
      (.attr "width" width)
      (.on "mousedown" (partial mousedown proj))))

(defn remove-svg []
   (-> js/d3
       (.selectAll "#graph svg")
       (.remove)))

(defn render [proj path svg world places]
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
  #_(let [globe-highlight (-> svg
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
      (.attr "rx" (* .9 (.scale proj)))
      (.attr "ry" (* .25 (.scale proj)))
      (.attr "class" "noclicks")
      (.style "fill" "url(#drop_shadow)"))
  (-> svg
      (.append "circle")
      (.attr "cx" (/ width 2))
      (.attr "cy" (/ height 2))
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
      (.attr "cy" (/ height 2))
      (.attr "r" (.scale proj))
      (.attr "class" "noclicks")
      (.style "fill" "url(#globe_highlight)"))
  (-> svg
      (.append "circle")
      (.attr "cx" (/ width 2))
      (.attr "cy" (/ height 2))
      (.attr "r" (.scale proj))
      (.attr "class" "noclicks")
      (.style "fill" "url(#globe_shading)"))
  (-> svg
      (.append "g")
      (.attr "class" "points")
      (.selectAll "text")
      (.data places.features)
      (.enter)
      (.append "path")
      (.attr "class" "point")
      (.attr "d" path))
  (-> svg
      (.append "g")
      (.attr "class" "arcs")
      (.selectAll "path")
      (.data (create-arcs places))
      (.enter)
      (.append "path")
      (.attr "class" "arc")
      (.attr "d" path))
  (refresh svg path))



;; only works when initialization ist done within the async block
;; i currently don't know why. It wont't work when proj path and svg
;; are stored in atoms as state ...
(defn initialize [world places]
  (println (create-arcs places))
  (let [_ (remove-svg)
        proj (-> js/d3
                 (.geoOrthographic)
                 (.translate #js [(/ width 2) (/ height 2)])
                 (.clipAngle 90)
                 (.scale 420))
        sky (-> js/d3
                (.geoOrthographic)
                (.translate #js [(/ width 2) (/ height 2)])
                (.clipAngle 90)
                (.scale 500))
        path (-> js/d3
                 (.geoPath)
                 (.projection proj)
                 (.pointRadius 2))
        svg (append-svg proj)]
    (render proj path svg world places)
    (-> js/d3
        (.select js/window)
        (.on "mouseup" mouseup)
        (.on "mousemove" (partial mousemove proj path svg)))))

(defn ^:export main []
  (.then (js/Promise.all #js [world-promise places-promise])
         (fn [[world places]] (initialize world places))))

(defn on-js-reload []
  (println "on-js-reload called")
  (main))
