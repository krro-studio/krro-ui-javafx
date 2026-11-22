(ns top.kzre.krro.ui.javafx.window
  "将 INativeWindow 协议实现到 javafx.stage.Stage 上。"
  (:require
   [top.kzre.krro.core.keymap :as km]
   [top.kzre.krro.core.message :as msg]
   [top.kzre.krro.core.mode :as mode]
   [top.kzre.krro.core.window :as win :refer [INativeWindow]]
   [top.kzre.krro.ui.javafx.plugin]
   [top.kzre.krro.ui.javafx.util :as javafx.util])
  (:import
    (javafx.application Platform)
    (javafx.event EventHandler)
    (javafx.scene Scene)
    [javafx.scene.control Label]
    (javafx.scene.input KeyEvent)
    (javafx.scene.layout Background BackgroundFill BorderPane CornerRadii)
    (javafx.scene.paint Color)
    (javafx.stage Stage StageStyle)
    (top.kzre.krro.ui.javafx JavaFXUtils)
    (top.kzre.krro.ui.javafx.layout WindowPane)))

(extend-protocol INativeWindow
  Stage
  (native-show! [s] (.show s))
  (native-hide! [s] (.hide s))
  (native-close! [s] (.close s))
  (native-title [s] (.getTitle s))
  (native-set-title! [s title] (.setTitle s title))
  (native-set-bounds! [s {:keys [x y width height]}]
    (when x (.setX s (double x)))
    (when y (.setY s (double y)))
    (when width (.setWidth s (double width)))
    (when height (.setHeight s (double height))))
  (native-bounds [s]
    {:x (.getX s) :y (.getY s) :width (.getWidth s) :height (.getHeight s)})
  (native-visible? [s] (.isShowing s))
  (native-focused? [s] (.isFocused s))
  (native-object [s] s))

;; ── minibuffer ──────────────────────────────────────────────
(defn- minibuffer []
  (let [status-label (Label. "Ready")]
    (add-watch msg/messages :status-bar
               (fn [_ _ _ new-msgs]
                 (when-let [m (last new-msgs)]
                   (Platform/runLater
                     (fn []
                       (.setText status-label (str "[" (name (:type m)) "] " (:content m))))))))
    status-label))

(defn stage-root [^Stage stage]
  (-> stage
      (.getScene)
      ^WindowPane (.getRoot)
      (.getChildrenUnmodifiable)
      (.get 0)))

;; ── 主舞台创建 ──────────────────────────────────────────
(defn create-stage
  "创建并显示 Krrō 主舞台。返回 {:keys [stage]}。
   Scene 的根节点是一个空的 BorderPane，其底部固定为状态栏，
   中心区域由渲染器在同步布局描述时动态填充。"
  [& {:keys [width height title]
      :or {width 1024 height 768 title "Krro Studio"}}]
  (let [border (BorderPane.)
        stage (Stage.)
        scene (Scene. (WindowPane. stage border)
                      (double width) (double height))
        ]
    ;; 标题栏请自己装饰
    (.initStyle stage StageStyle/TRANSPARENT)
    (.setFill scene Color/TRANSPARENT)
    ;; 纯透明，容器内部的底色铺满
    (.setBackground border
                    (JavaFXUtils/fillBackground
                      (BackgroundFill. Color/TRANSPARENT CornerRadii/EMPTY nil)))
    (.setStyle border
              (str
                "-fx-background-color: #1e1e1e;
               -fx-background-radius: 8;
               -fx-background-insets: 0;"
                "-fx-padding: " WindowPane/WINDOW_MARGIN ";"))

    (.setOnKeyPressed scene
                      (reify EventHandler
                        (handle [_ e]
                          (let [^KeyEvent ke e]
                            (when-not (.isConsumed ke)
                              (let [key-desc (javafx.util/key-event->kw ke)
                                    keymaps (mode/keymaps (win/active-frame))]
                                (try
                                  (km/handle-key! key-desc keymaps)
                                  (catch Exception ex
                                    (.printStackTrace ex)))))))))
    ;; 将状态栏固定在底部
    (.setBottom border (minibuffer))
    ;; 中心区域不预先创建，由渲染器接管
    (.setScene stage scene)
    (.setTitle stage title)
    (.show stage)
    {:stage stage}))