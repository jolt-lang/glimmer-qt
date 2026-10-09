(ns glimmer-qt.counter
  "A reactive counter with component-local state. Run with `jolt counter`."
  (:require [glimmer.ratom :as r]
            [glimmer.core :as ui]
            [glimmer-qt.core]))

;; Adapted from glimmer-gtk. Copyright (c) 2026 Dmitri Sotnikov.
;; MIT licensed; see ../../LICENSE.
(defn counter
  "Form-2: the outer function creates state once; the inner function renders."
  []
  (let [count (r/atom 0)]
    (fn []
      [:vbox {:spacing 12 :padding 24 :hexpand true :vexpand true}
       [:label {:id "count" :label (str "Count: " @count) :font-size 24}]
       [:hbox {:spacing 8}
        [:button {:id "decrement" :label "− 1" :on-click #(swap! count dec)}]
        [:button {:id "increment" :label "+ 1" :on-click #(swap! count inc)}]
        [:button {:id "reset" :label "Reset" :on-click #(reset! count 0)}]]])))

(defn -main [& args]
  (when (seq args)
    (throw (ex-info "Usage: jolt counter" {:args args})))
  (ui/run counter :title "Glimmer Qt · Counter" :width 360 :height 180
          :app-id "glimmer.qt.counter"))
