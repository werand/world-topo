(ns world-topo.core
  (:require [cljsjs.d3]
            [cljsjs.topojson]))

(enable-console-print!)

;; Inspired from
;; Usage of d3 with clojurescript
;; https://lambdaisland.com/blog/26-04-2018-d3-clojurescript
;; d3 - Example - this is the original code i transfered to clojurescript
;; http://bl.ocks.org/dwtkns/4973620

(defonce world-promise (js/d3.json "world.json"))
(defonce places-promise (js/d3.json "places.json"))

(defonce height 1000)
(defonce width 960)

(defonce m0 (atom nil))
(defonce o0 (atom #js [0 0 0]))

(defn link-places [places]
  (let [p (js->clj places.features)]
    (loop [acc [] a p a1 (first a) o (rest p)]
      (if (seq a)
        (recur
         (conj acc
               (for [b o]
                 [(get (get a1 "geometry") "coordinates")
                  (get (get b "geometry") "coordinates")]))
         (rest a)
         (first (rest a))
         (rest o))
        (apply concat acc)))))

(defn create-arcs [places]
  (clj->js
   (for [coords (link-places places)]
     {:type "Feature" :geometry {:type "LineString" :coordinates coords}})))

(defn create-links [places]
  (clj->js
   (for [[a b] (link-places places)]
     {:source a :target b})))

(defn location-along-arc [start end loc]
  ((js/d3.geoInterpolate start end) loc))

(defn flying-arc [proj sky pts]
  (let [source pts.source
        target pts.target
        mid (location-along-arc source target .5)]
    #js [(proj source)
         (sky mid)
         (proj target)]))

;; see https://github.com/d3/d3-shape#curveCardinal_tension
(defn interpolate []
  (-> js/d3
      (.line)
      (.x #(first %))
      (.y #(second %))
      (.curve (js/d3.curveBundle.beta 0.5))))

;; https://github.com/d3/d3-geo/blob/master/README.md#geoDistance
;; https://stackoverflow.com/questions/35953892/d3-scale-linear-vs-d3-scalelinear
(defn fade-at-edge [proj d]
  (let [center-pos (.invert proj #js [(/ width 2) (/ height 2)])
        d (js->clj d)
        start (clj->js
               (if (nil? (get d "source"))
                 (first (get (get d "geometry") "coordinates"))
                 (get d "source")))
        end (clj->js
             (if (nil? (get d "source"))
               (second (get (get d "geometry") "coordinates"))
               (get d "target")))
        start-dist (- 1.57 (js/d3.geoDistance start center-pos))
        end-dist (- 1.57 (js/d3.geoDistance end center-pos))
        dist (if (< start-dist end-dist) start-dist end-dist)
        fade (-> (js/d3.scaleLinear)
                 (.domain #js [-0.1 0])
                 (.range #js [-1 0.1]))]
    (fade dist)))

(defn mousedown [proj]
  (reset! m0 [js/d3.event.pageX js/d3.event.pageY])
  (reset! o0 (.rotate proj))
  (js/d3.event.preventDefault))

(defn mouseup [proj]
  (reset! o0 (.rotate proj))
  (reset! m0 nil))

(defn refresh [proj sky path svg]
  (-> svg
      (.selectAll ".land")
      (.attr "d" path))
  (-> svg
      (.selectAll ".point")
      (.attr "d" path))
  (-> svg
      (.selectAll ".arc")
      (.attr "d" path)
      (.attr "opacity" (partial fade-at-edge proj)))
  (-> svg
      (.selectAll ".flyer")
      (.attr "d" #((interpolate) (flying-arc proj sky %)))
      (.attr "opacity" (partial fade-at-edge proj))))

(defn mousemove [proj sky path svg]
  (when-not (nil? @m0)
    (let [[m1-x m1-y] [js/d3.event.pageX js/d3.event.pageY]
          [m0-x m0-y] @m0
          [o0-x o0-y] @o0
          o1-x (+ o0-x (/ (- m1-x m0-x) 6))
          o1-y (+ o0-y (/ (- m0-y m1-y) 6))
          o1 #js [o1-x (cond
                         (> o1-y 30)   30
                         (< o1-y -30) -30
                         :else o1-y)]]
      (.rotate proj o1)
      (.rotate sky o1)))
  (refresh proj sky path svg))

(defn append-svg [proj]
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

(defn render [proj sky path svg world places]
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
  (-> svg
      (.append "g")
      (.attr "class" "flyers")
      (.selectAll "path")
      (.data (create-links places))
      (.enter)
      (.append "path")
      (.attr "class" "flyer")
      (.attr "d" #((interpolate) (flying-arc proj sky %))))
  (refresh proj sky path svg))

;; only works when initialization is done within the async block
;; i currently don't know why. It wont't work when proj path and svg
;; are stored in atoms as state ...
(defn initialize [world places]
  (let [_ (remove-svg)
        proj (-> js/d3
                 (.geoOrthographic)
                 (.translate #js [(/ width 2) (/ height 2)])
                 (.clipAngle 90)
                 (.scale 420)
                 (.rotate @o0))
        sky (-> js/d3
                (.geoOrthographic)
                (.translate #js [(/ width 2) (/ height 2)])
                (.clipAngle 90)
                (.scale 700)
                (.rotate @o0))
        path (-> js/d3
                 (.geoPath)
                 (.projection proj)
                 (.pointRadius 2))
        svg (append-svg proj)]
    (render proj sky path svg world places)
    (-> js/d3
        (.select js/window)
        (.on "mouseup" (partial mouseup proj))
        (.on "mousemove" (partial mousemove proj sky path svg)))))

(defn ^:export main []
  (.then (js/Promise.all #js [world-promise places-promise])
         (fn [[world places]] (initialize world places))))

(defn on-js-reload []
  (main))
