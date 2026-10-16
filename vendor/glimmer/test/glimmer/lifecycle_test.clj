(ns glimmer.lifecycle-test
  "The explicit root lifecycle seam between glimmer.core/run and a backend."
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

(deftest explicit-lifecycle-follows-reload-without-a-mount-return-value
  (let [cell (r/atom 0) win (mock/widget :window {})]
    (b/register!
     (assoc mock/backend :run-lifecycle
            (fn [_ {:keys [mount! unmount!]}]
              (is (nil? (mount! win :window)))
              (dotimes [_ 8]
                (ui/reload!)
                (is (= 1 (count @(:watches cell))))
                (is (= 1 (count (mock/children win)))))
              (unmount!)
              (unmount!)
              (is (empty? @(:watches cell)))
              (is (empty? (mock/children win))))))
    (ui/run (fn [] [:label {:label @cell}]))
    (reset! cell 1)
    (ui/reload!)
    (is (empty? (mock/children win)))))

(deftest legacy-run-backends-detach-subscriptions-when-the-loop-exits
  (let [cell (r/atom 0) win (mock/widget :window {})]
    (b/register! (assoc mock/backend :run (fn [_ mount-root!] (mount-root! win :window) nil)))
    (ui/run (fn [] [:label {:label @cell}]))
    ;; A legacy backend owns its container, so the widget stays; the tree does not.
    (is (= 1 (count (mock/children win))))
    (is (empty? @(:watches cell)))
    (reset! cell 1)
    (is (= 0 (label-of win)))))

(deftest queued-reloads-belong-to-their-run
  (let [queue (atom []) win (mock/widget :window {}) phase (atom 0)]
    (b/register!
     (assoc mock/backend :schedule #(swap! queue conj %)
            :run-lifecycle
            (fn [_ {:keys [mount! unmount!]}]
              (reset! b/loop-running? true)
              (mount! win :window)
              (if (zero? @phase)
                (do (ui/reload! (fn [] [:label {:label "stale"}])) (swap! phase inc))
                (do ((first @queue))
                    (is (= "current" (label-of win)))))
              (unmount!))))
    (ui/run (fn [] [:label {:label "first"}]))
    (ui/run (fn [] [:label {:label "current"}]))))

(deftest a-later-run-takes-over-the-live-root
  (let [outer (mock/widget :window {}) inner (mock/widget :window {}) phase (atom 0)]
    (b/register!
     (assoc mock/backend :run-lifecycle
            (fn [_ {:keys [mount! unmount!]}]
              (if (zero? @phase)
                (do (swap! phase inc)
                    (mount! outer :window)
                    ;; a second run while this one is live replaces it quietly
                    (ui/run (fn [] [:label {:label "inner"}]))
                    (is (= "outer" (label-of outer)))
                    (unmount!))
                (do (mount! inner :window)
                    (ui/reload! (fn [] [:label {:label "inner reloaded"}]))
                    (is (= "inner reloaded" (label-of inner)))
                    (unmount!))))))
    (ui/run (fn [] [:label {:label "outer"}]))
    (is (empty? (mock/children outer)))
    (is (empty? (mock/children inner)))))

(deftest abandon-detaches-only-the-selected-independent-root
  (let [cell (r/atom 0) a (mock/widget :window {}) b (mock/widget :window {})
        root (fn [] [:label {:label @cell}])
        root-a (ui/mount a :window [root]) root-b (ui/mount b :window [root])]
    (is (= 2 (count @(:watches cell))))
    (ui/abandon! root-a)
    (ui/abandon! root-a)
    (is (= 1 (count @(:watches cell))))
    (reset! cell 1)
    (is (= 0 (label-of a)))
    (is (= 1 (label-of b)))
    (ui/unmount! root-b b :window)
    (is (empty? @(:watches cell)))))

(deftest failed-attachment-disposes-the-new-widget
  (let [win (mock/widget :window {}) disposed (atom [])]
    (b/register! (assoc mock/backend
                        :append-child! (fn [& _] (throw (ex-info "attach failed" {})))
                        :dispose! (fn [w] (swap! disposed conj (:tag @w)))))
    (is (thrown? Exception (ui/mount win :window [:label {:label "x"}])))
    (is (= [:label] @disposed))
    (is (empty? (mock/children win)))))
