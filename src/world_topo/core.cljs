(ns world-topo.core
  (:require [cljsjs.d3]
            [cljsjs.topojson]))

(enable-console-print!)

(defonce world-promise (js/d3.json "world.json"))
(defonce places-promise (js/d3.json "places.json"))

(defonce height 1000)
(defonce width 960)

(defonce initialized (atom nil))

(defonce proj (atom nil))

#_(atom (-> js/d3
            (.geoOrthographic)
            (.translate #js [(/ width 2) (/ height 2)])
            (.clipAngle 90)
            (.scale 420)))

#_(defonce sky (-> js/d3
             (.geoOrthographic)
             (.translate #js [(/ width 2) (/ height 2)])))

(def path (atom nil))
#_(atom (-> js/d3
          (.geoPath)
          (.projection @proj)
          (.pointRadius 2)))
(def svg (atom nil))

#_(def swoosh (-> js/d3
                (.line)
                (.x (fn [d] (first d)))
                #_(.y (fn [d] (second d)))
                (.interpolate "cardinal")
                (.tension .0)))

(defonce m0 (atom nil))
(defonce o0 (atom [0 0 0]))

(defn mousedown [proj]
  (reset! m0 [js/d3.event.pageX js/d3.event.pageY])
  (reset! o0 (.rotate proj))
  (println (str "mousedown" @o0))
  (js/d3.event.preventDefault))

(defn mouseup []
  (reset! m0 nil))

(declare on-js-reload)

(defn mousemove [proj path svg]
  (when-not (nil? @m0)
    (println "mousemove "(str o0 m0))
    (let [m1-x js/d3.event.pageX
          m1-y js/d3.event.pageY
          m0-x (first @m0)
          m0-y (second @m0)
          o0-x (first @o0)
          o0-y (second @o0)
          o1-x (+ o0-x
                  (/ (- m1-x m0-x) 6))
          o1-y (+ o0-y
                  (/ (- m0-y m1-y) 6))
          o1 [o1-x (if (> o1-y 30)
                     30
                     (if (< o1-y -30)
                       -30
                       o1-y))]]
      #_(println m1)
      (println o1)
      (.rotate proj (clj->js o1))
      #_(.rotate sky o1)
      #_(reset! o0 o1)
      #_(on-js-reload)))
  (-> svg
      (.selectAll ".land")
      (.attr "d" path))
  (-> svg
      (.selectAll ".point")
      (.attr "d" path)))

(println "This text is printed from src/world-topo/core.cljs. Go ahead and edit it and see reloading in action.")

;; define your app data so that it doesn't get over-written on reload

#_(defonce app-state (atom {:text "Hello world!"}))


#_(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)

(defn append-svg [proj]
  (if (nil? @svg)
    (do
      (println "append-svg")
      (-> js/d3
         (.select "#graph")
         (.append "svg")
         (.attr "height" height)
         (.attr "width" width)
         (.on "mousedown" (partial mousedown proj))))
    @svg))

(defn remove-svg []
  #_(-> js/d3
      (.selectAll "#graph svg")
      (.remove)))

(defn render [world places]
  #_(println (js/topojson.feature world (.. world -objects -land)))
  #_(println (.-land (.-objects world)))
  (let [ocean-fill (-> @svg
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
  (let [globe-highlight (-> @svg
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
  (let [globe-shading (-> @svg
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
  (let [drop-shadow (-> @svg
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
  (-> @svg
      (.append "ellipse")
      (.attr "cx" 440)
      (.attr "cy" 450)
      (.attr "rx" (* .9 (.scale @proj)))
      (.attr "ry" (* .25 (.scale @proj)))
      (.attr "class" "noclicks")
      (.style "fill" "url(#drop_shadow)"))
  (-> @svg
      (.append "circle")
      (.attr "cx" (/ width 2))
      (.attr "cy" (/ height 2))
      (.attr "r" (.scale @proj))
      (.attr "class" "noclicks")
      (.style "fill" "url(#ocean_fill)"))
  (-> @svg
      (.append "path")
      (.datum (js/topojson.feature world (.. world -objects -land)))
      (.attr "class" "land noclicks")
      (.attr "d" @path))
  (-> @svg
      (.append "circle")
      (.attr "cx" (/ width 2))
      (.attr "cy" (/ height 2))
      (.attr "r" (.scale @proj))
      (.attr "class" "noclicks")
      (.style "fill" "url(#globe_highlight)"))
  (-> @svg
      (.append "circle")
      (.attr "cx" (/ width 2))
      (.attr "cy" (/ height 2))
      (.attr "r" (.scale @proj))
      (.attr "class" "noclicks")
      (.style "fill" "url(#globe_shading)"))
;; Refresh
  (-> @svg
      (.selectAll ".land")
      (.attr "d" @path))
  (-> @svg
      (.selectAll ".point")
      (.attr "d" @path))
  )

(defn inner-main [world places]
  (let [p (-> js/d3
              (.geoOrthographic)
              (.translate #js [(/ width 2) (/ height 2)])
              (.clipAngle 90)
              (.scale 420))
        lsvg (append-svg p)]
    (reset! svg lsvg)
    (reset! proj p)
    (println (str "ROTATE ON PROJ " (.rotate p)))
    (reset! path (-> js/d3
                     (.geoPath)
                     (.projection @proj)
                     (.pointRadius 2)))
    (println (str "ROTATE ON PROJ2 " (.rotate @proj)))
    (render world places)
    (-> js/d3
        (.select js/window)
        (.on "mouseup" mouseup)
        (.on "mousemove" (partial mousemove p @path @svg)))))

(defn ^:export main []
  (when (nil? @initialized)
    (.then (js/Promise.all #js [world-promise places-promise])
           (fn [[world places]] (inner-main world places)))
    (reset! initialized true)))

(defn on-js-reload []
  (println "on-js-reload called")
  (remove-svg)
  (main))
