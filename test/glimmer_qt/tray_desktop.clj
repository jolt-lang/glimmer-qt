(ns glimmer-qt.tray-desktop
  "Short-lived macOS menu-bar check. Run with QT_QPA_PLATFORM=cocoa
  jolt -A:tray-test -m glimmer-qt.tray-desktop and open the Glimmer Qt icon."
  (:require [glimmer-qt.core :as qt]
            [glimmer-qt.tray :as tray]))

(defn -main [& _]
  (try
    (qt/run-application!
     (fn []
       (let [icon (str (System/getProperty "user.dir") "/test/icons/scissors.svg")
             tray-id (tray/create! icon :tooltip "Glimmer Qt tray desktop check")
             editor (atom nil)]
         (tray/add-action! tray-id :label "Capture region" :shortcut-hint "⌘⇧5"
                           :on-trigger #(println "Capture requested"))
         (tray/add-action! tray-id :label "Open editor"
                           :on-trigger #(if @editor
                                          (qt/activate! @editor)
                                          (reset! editor (qt/open-window! (fn [] [:label {:label "Editor shown from tray"}])
                                                                         :title "Glimmer Qt tray check"))))
         (tray/add-separator! tray-id)
         (tray/add-action! tray-id :label "Quit Glimmer Qt" :on-trigger qt/quit!)
         (tray/show! tray-id)
         (println "TRAY READY: open the Glimmer Qt menu-bar icon; demo exits after 30 seconds.")))
     :auto-quit-ms 30000)
    (finally (shutdown-agents))))
