(ns glimmer.subscription-test
  "Component subscriptions are exactly the cells the latest render read, and they
  are detached the moment a subtree is unmounted, replaced or fails to mount."
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

(defn- label-of [win] (get-in @(first (mock/children win)) [:props :label]))

(deftest partial-mount-detaches-new-child-subscriptions
  (let [cell (r/atom 0) win (mock/widget :window {})
        bad (fn [] @cell (throw (ex-info "render failed" {})))]
    (is (thrown? Exception (ui/mount win :window [:vbox [bad]])))
    (is (empty? @(:watches cell)))
    (is (empty? (mock/children win)))))

(deftest partial-keyed-mount-detaches-new-child-subscriptions
  (let [cell (r/atom 0) win (mock/widget :window {})
        bad (fn [] @cell (throw (ex-info "render failed" {})))]
    (is (thrown? Exception (ui/mount win :window [:vbox [:label {:key "ok" :label "x"}] [bad {:key "bad"}]])))
    (is (empty? @(:watches cell)))
    (is (empty? (mock/children win)))))

(deftest conditional-subscriptions-and-nil-components-dispose-immediately
  (let [show (r/atom true) cell (r/atom "value") win (mock/widget :window {})
        root (ui/mount win :window [(fn [] (when @show [:label {:label @cell}]))])]
    (is (= 1 (count @(:watches cell))))
    (reset! show false)
    (is (empty? @(:watches cell)))
    (is (empty? (mock/children win)))
    (ui/unmount! root win :window)
    (is (empty? @(:watches show)))))

(deftest replacing-form-two-components-discards-state-and-subscriptions
  (let [cell (r/atom 0) which (r/atom true) mounts (atom [])
        a (fn [] (swap! mounts conj :a) (fn [] [:label {:label @cell}]))
        z (fn [] (swap! mounts conj :z) (fn [] [:label {:label "z"}]))
        win (mock/widget :window {})
        root (ui/mount win :window [(fn [] [(if @which a z)])])]
    (is (= [:a] @mounts))
    (reset! which false)
    (is (= [:a :z] @mounts))
    (is (empty? @(:watches cell)))
    (is (= "z" (label-of win)))
    (ui/unmount! root win :window)
    (is (empty? @(:watches which)))))

(deftest replacing-a-form-two-component-with-a-form-one-drops-its-render-fn
  (let [which (r/atom true) tick (r/atom 0) win (mock/widget :window {})
        two (fn [] (fn [] [:label {:label "two"}]))
        one (fn [] [:label {:label (str "one " @tick)}])
        root (ui/mount win :window [(fn [] [(if @which two one)])])]
    (is (= "two" (label-of win)))
    (reset! which false)
    (is (= "one 0" (label-of win)))
    (reset! tick 1)
    (is (= "one 1" (label-of win)))
    (ui/unmount! root win :window)
    (is (empty? @(:watches tick)))))

(deftest queued-renders-are-fenced-by-disposal
  (let [queue (atom []) renders (atom 0) disposed (atom false)
        watcher (ui/make-rerender-watcher #(swap! renders inc) (atom true)
                                          #(swap! queue conj %) disposed)]
    (watcher nil)
    (reset! disposed true)
    ((first @queue))
    (is (zero? @renders))))

(deftest unmount-clears-the-root-when-native-removal-fails
  (let [cell (r/atom 0) win (mock/widget :window {})
        root (ui/mount win :window [(fn [] [:label {:label @cell}])])]
    (is (= 1 (count @(:watches cell))))
    (b/register! (assoc mock/backend :remove-child! (fn [& _] (throw (ex-info "native removal failed" {})))))
    (is (thrown? Exception (ui/unmount! root win :window)))
    (is (nil? @root))
    (is (empty? @(:watches cell)))))

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
