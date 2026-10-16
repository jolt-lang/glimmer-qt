(ns glimmer.placement-test
  "A widget a slot gains after its siblings have theirs is placed where the slot
  sits among them, not at the end of the container."
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [glimmer.backend :as b]
            [glimmer.core :as ui]
            [glimmer.ratom :as r]
            [glimmer.mock-backend :as mock]))

(use-fixtures :each
  (fn [test]
    (mock/install!)
    (reset! b/loop-running? false)
    (try (test) (finally (reset! b/loop-running? false)))))

;; A component that re-renders on its own and contributes a widget where it had
;; none appends to the parent container, so without positioning it would land
;; after the siblings that should follow it.
(defn- labels-of [w] (mapv #(get-in @% [:props :label]) (mock/children w)))

(deftest a-component-toggled-off-and-on-keeps-its-place-before-its-sibling
  (let [show (r/atom true) win (mock/widget :window {})
        first-comp (fn [] (when @show [:label {:label "first"}]))]
    (ui/mount win :window [:vbox [first-comp] [:label {:label "tail"}]])
    (let [vbox (first (mock/children win))
          tail-id (second (mock/ids vbox))]
      (is (= ["first" "tail"] (labels-of vbox)))
      (reset! show false)
      (is (= ["tail"] (labels-of vbox)))
      (reset! show true)
      (is (= ["first" "tail"] (labels-of vbox)))
      (is (= tail-id (second (mock/ids vbox))) "the sibling is moved around, not recreated"))))

(deftest a-component-that-starts-hidden-appears-before-its-sibling
  (let [show (r/atom false) win (mock/widget :window {})
        first-comp (fn [] (when @show [:label {:label "first"}]))]
    (ui/mount win :window [:vbox [first-comp] [:label {:label "tail"}]])
    (let [vbox (first (mock/children win))]
      (is (= ["tail"] (labels-of vbox)))
      (reset! show true)
      (is (= ["first" "tail"] (labels-of vbox))))))

(deftest a-reappearing-component-goes-after-its-nearest-present-sibling
  (let [show (r/atom false) win (mock/widget :window {})
        never (fn [] (when false [:label {:label "never"}]))
        middle (fn [] (when @show [:label {:label "middle"}]))]
    (ui/mount win :window [:vbox [:label {:label "head"}] [never] [middle] [:label {:label "tail"}]])
    (let [vbox (first (mock/children win))]
      (is (= ["head" "tail"] (labels-of vbox)))
      (reset! show true)
      (is (= ["head" "middle" "tail"] (labels-of vbox)) "the hidden slot before it is skipped")
      (reset! show false)
      (reset! show true)
      (is (= ["head" "middle" "tail"] (labels-of vbox))))))

(deftest a-keyed-component-that-appears-is-placed-before-its-keyed-sibling
  (let [show (r/atom false) win (mock/widget :window {})
        first-comp (fn [] (when @show [:label {:label "first"}]))]
    (ui/mount win :window [:vbox [first-comp {:key :first}] [:label {:key :tail :label "tail"}]])
    (let [vbox (first (mock/children win))]
      (is (= ["tail"] (labels-of vbox)))
      (reset! show true)
      (is (= ["first" "tail"] (labels-of vbox)))
      (reset! show false)
      (is (= ["tail"] (labels-of vbox)))
      (reset! show true)
      (is (= ["first" "tail"] (labels-of vbox))))))

(deftest a-slot-turning-from-a-component-into-an-element-keeps-its-place
  (let [native? (r/atom false) win (mock/widget :window {})
        comp (fn [] [:label {:label "comp"}])
        app (fn [] [:vbox (if @native? [:button {:label "button"}] [comp]) [:label {:label "tail"}]])]
    (ui/mount win :window [app])
    (let [vbox (first (mock/children win))]
      (is (= ["comp" "tail"] (labels-of vbox)))
      (reset! native? true)
      (is (= ["button" "tail"] (labels-of vbox))))))

(deftest a-component-nested-in-a-wrapper-is-placed-in-the-wrapper-slot
  (let [show (r/atom false) win (mock/widget :window {})
        conditional (fn [] (when @show [:label {:label "first"}]))
        wrapper (fn [] [conditional])]
    (ui/mount win :window [:vbox [wrapper] [:label {:label "tail"}]])
    (let [vbox (first (mock/children win))
          tail-id (first (mock/ids vbox))]
      (is (= ["tail"] (labels-of vbox)))
      (reset! show true)
      (is (= ["first" "tail"] (labels-of vbox)))
      (reset! show false)
      (reset! show true)
      (is (= ["first" "tail"] (labels-of vbox)))
      (is (= tail-id (second (mock/ids vbox))) "the sibling is moved around, not recreated"))))

;; Placement is only for a slot that gains a widget after its siblings have
;; theirs. A mount appends in order, so it must neither move a widget nor scan
;; the slots after each child — that scan made a mount quadratic in the number
;; of children.
(deftest mounting-children-neither-moves-widgets-nor-scans-siblings
  (let [moves (atom 0) scans (atom 0) show (r/atom false) win (mock/widget :window {})
        row (fn [i] [:label {:label (str i)}])
        late (fn [] (when @show [:label {:label "late"}]))
        inst-widget @#'ui/inst-widget]
    (b/register! (assoc mock/backend :reorder-child!
                        (fn [& args] (swap! moves inc) (apply (:reorder-child! mock/backend) args))))
    (with-redefs [ui/inst-widget (fn [a] (swap! scans inc) (inst-widget a))]
      (ui/mount win :window [:vbox
                             (into [:hbox] (for [i (range 5)] [row i]))
                             (into [:hbox] (for [i (range 5)] [row {:key i} i]))
                             [late] [:label {:label "tail"}]])
      (is (zero? @moves))
      (is (zero? @scans))
      (reset! show true)
      (is (= 1 @moves) "only a widget appearing ahead of its siblings is moved")
      (is (= ["late" "tail"] (subvec (labels-of (first (mock/children win))) 2))
          "and it lands before the sibling that follows it")
      (is (pos? @scans) "which is when the siblings are looked at"))))

(deftest a-backend-without-reorder-keeps-appending-new-widgets
  (let [show (r/atom false) win (mock/widget :window {})
        first-comp (fn [] (when @show [:label {:label "first"}]))]
    (b/register! (dissoc mock/backend :reorder-child!))
    (ui/mount win :window [:vbox [first-comp] [:label {:label "tail"}]])
    (reset! show true)
    (is (= ["tail" "first"] (labels-of (first (mock/children win)))))))
