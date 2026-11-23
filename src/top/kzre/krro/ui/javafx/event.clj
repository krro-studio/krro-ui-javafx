(ns top.kzre.krro.ui.javafx.event
  "JavaFX 事件适配层：将原生事件转换为标准化事件 map，并提供绑定函数。
   不支持已创建组件的事件动态更新。
   注意：不再使用 ObservableValue 监听器来触发变更事件，
   而是依赖 ActionEvent（用户交互触发）来保持单向数据流。"
  (:import
    (java.util.function BiConsumer)
    (javafx.beans.value ChangeListener)
    (javafx.event Event EventHandler)
    (javafx.scene Node)
    (javafx.scene.control
      CheckBox
      ComboBox
      RadioButton
      Slider
      TextInputControl
      ToggleButton)
    (javafx.scene.input KeyEvent MouseButton MouseEvent))
  (:require
    [top.kzre.krro.ui.javafx.drag :as drag]))

(defn ->event-map
  "将 JavaFX 事件转换为符合 spec.event 的标准化 map。"
  [^Event fx-event event-type target-key]
  (merge
    {:type      event-type
     :target    target-key
     :timestamp (System/currentTimeMillis)}
    ;; 鼠标事件
    (when (instance? MouseEvent fx-event)
      (let [me ^MouseEvent fx-event]
        {:x (.getX me)
         :y (.getY me)
         :click-count (.getClickCount me)
         :screen-x (.getScreenX me)
         :screen-y (.getScreenY me)
         :button   (case (.getButton me)
                     MouseButton/PRIMARY :left
                     MouseButton/SECONDARY :right
                     MouseButton/MIDDLE :middle
                     nil)
         :ctrl? (.isControlDown me)
         :shift? (.isShiftDown me)
         :alt? (.isAltDown me)
         :meta? (.isMetaDown me)}))
    ;; 键盘事件
    (when (instance? KeyEvent fx-event)
      (let [ke ^KeyEvent fx-event]
        {:key      (.getText ke)
         :key-code (.getCode ke)
         :repeat?  (.isRepeat ke)
         :ctrl?    (.isControlDown ke)
         :shift?   (.isShiftDown ke)
         :alt?   (.isAltDown ke)
         :meta?  (.isMetaDown ke)}))))

;; ── 鼠标事件 ─────────────────────────────────

(defn bind-click!
  [^Node node props target-key]
  (when-let [f (:on-click props)]
    (.setOnMouseClicked node
                        (reify EventHandler
                          (handle [_ e]
                            (f (->event-map e :click target-key)))))))

(defn bind-mouse-press!
  [^Node node props target-key]
  (when-let [f (:on-press props)]
    (.setOnMousePressed node
                        (reify EventHandler
                          (handle [_ e]
                            (f (->event-map e :press target-key)))))))

(defn bind-mouse-release!
  [^Node node props target-key]
  (when-let [f (:on-release props)]
    (.setOnMouseReleased node
                         (reify EventHandler
                           (handle [_ e]
                             (f (->event-map e :release target-key)))))))

(defn bind-mouse-enter!
  [^Node node props target-key]
  (when-let [f (:on-mouse-enter props)]
    (.setOnMouseEntered node
                        (reify EventHandler
                          (handle [_ e]
                            (f (->event-map e :mouse-enter target-key)))))))

(defn bind-mouse-leave!
  [^Node node props target-key]
  (when-let [f (:on-mouse-leave props)]
    (.setOnMouseExited node
                       (reify EventHandler
                         (handle [_ e]
                           (f (->event-map e :mouse-leave target-key)))))))

;; ── 键盘事件 ─────────────────────────────────

(defn bind-key-down!
  [^Node node props target-key]
  (when-let [f (:on-key-down props)]
    (.setOnKeyPressed node
                      (reify EventHandler
                        (handle [_ e]
                          (f (->event-map e :key-down target-key)))))))

(defn bind-key-up!
  [^Node node props target-key]
  (when-let [f (:on-key-up props)]
    (.setOnKeyReleased node
                       (reify EventHandler
                         (handle [_ e]
                           (f (->event-map e :key-up target-key)))))))

;; ── 焦点事件 ─────────────────────────────────

(defn bind-focus!
  [^Node node props target-key]
  (when-let [f (:on-focus props)]
    (.addListener (.focusedProperty node)
                  (proxy [ChangeListener] []
                    (changed [_ _ focused?]
                      (when focused?
                        (f
                          {:type :focus
                           :target target-key
                           :timestamp (System/currentTimeMillis)})))))))

(defn bind-blur!
  [^Node node props target-key]
  (when-let [f (:on-blur props)]
    (.addListener (.focusedProperty node)
                  (proxy [ChangeListener] []
                    (changed [_ _ focused?]
                      (when-not focused?
                        (f {:type :blur :target target-key :timestamp (System/currentTimeMillis)})))))))

;; ── Action / Change ─────────────────────────

(defn bind-action!
  [node props target-key]
  (when-let [f (:on-action props)]
    (.setOnAction node
                  (reify EventHandler
                    (handle [_ e]
                      (f (->event-map e :action target-key)))))))

(defn make-change-event [key old new]
  {:type      :change
   :target    key
   :timestamp (System/currentTimeMillis)
   :old-value old
   :new-value new})

(defn bind-change-action!
  "绑定 ActionEvent 作为变更事件。仅当用户交互触发。
   程序修改控件值不会触发此事件，从而保持单向数据流。"
  [node props target-key]
  (when-let [f (:on-change props)]
    (cond
      (or (instance? CheckBox node)
          (instance? RadioButton node))
      (.setOnAction node
                    (reify EventHandler
                      (handle [_ _e]
                        (let [new-val (cond
                                        (instance? CheckBox node) (.isSelected ^CheckBox node)
                                        (instance? RadioButton node) (.isSelected ^RadioButton node))]
                          (f (make-change-event target-key nil new-val))))))
      :else
      (let [observable
            (cond
              (instance? TextInputControl node) (.textProperty ^TextInputControl node)
              (instance? Slider node) (.valueProperty ^Slider node)
              (instance? CheckBox node) (.selectedProperty ^CheckBox node)
              (instance? ComboBox node) (.valueProperty ^ComboBox node)
              (instance? ToggleButton node) (.selectedProperty ^ToggleButton node))]

        (.subscribe observable
                    (reify BiConsumer
                      (accept [_ old-value new-value]
                        (f (make-change-event target-key old-value new-value)))))))))

;; ── 统一分发 ─────────────────────────────────

(def ^:private event-binding-fns
  "事件属性名 → 绑定函数映射。每个绑定函数签名：[node props target-key]"
  {:on-click           bind-click!
   :on-press           bind-mouse-press!
   :on-release         bind-mouse-release!
   :on-mouse-enter     bind-mouse-enter!
   :on-mouse-leave     bind-mouse-leave!
   :on-key-down        bind-key-down!
   :on-key-up          bind-key-up!
   :on-focus           bind-focus!
   :on-blur            bind-blur!
   :on-action          bind-action!
   :on-change          bind-change-action!
   :on-drag            drag/setup-drag-source!
   :on-drop            drag/setup-drag-target!})

(def ^:private events* (keys event-binding-fns))

(defn events [] events*)

(defn bind-events!
  "统一绑定所有在 props 中声明的事件。
   调用方只需一行：(event/bind-events! node props target-key)"
  [^Node node props target-key]
  (doseq [[k bind-fn] event-binding-fns]
    (when (contains? props k)
      (bind-fn node props target-key)))
  node)