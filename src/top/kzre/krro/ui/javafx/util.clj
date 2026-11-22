(ns top.kzre.krro.ui.javafx.util
  (:require
   [clojure.string :as str])
  (:import
   [javafx.animation PauseTransition]
   (javafx.event EventHandler)
   (javafx.geometry Orientation Pos)
   [javafx.scene.input KeyCode KeyEvent]
   [javafx.scene.layout HBox Priority VBox]
   [javafx.util Duration]))


(defn kw->orientation [kw]
  (case kw
    :horizontal Orientation/HORIZONTAL
    :vertical Orientation/VERTICAL))

(defn set-grow [box grow]
  (if grow
    (do (HBox/setHgrow box Priority/ALWAYS)
        (VBox/setVgrow box Priority/ALWAYS))
    (do (HBox/setHgrow box Priority/NEVER)
        (VBox/setVgrow box Priority/NEVER))))

(defn kw->pos [kw]
  (case kw
    :top-left Pos/TOP_LEFT
    :top-center Pos/TOP_CENTER
    :top-right Pos/TOP_RIGHT
    :center-left Pos/CENTER_LEFT
    :center Pos/CENTER
    :center-right Pos/CENTER_RIGHT
    :bottom-left Pos/BOTTOM_LEFT
    :bottom-center Pos/BOTTOM_CENTER
    :bottom-right Pos/BOTTOM_RIGHT
    :baseline-left Pos/BASELINE_LEFT
    :baseline-center Pos/BASELINE_CENTER
    :baseline-right Pos/BASELINE_RIGHT
    Pos/CENTER)) ; 默认居中


(defn key-event->kw
  "将 JavaFX KeyEvent 转换为 Emacs 风格的键描述字符串，例如 'C-x', 'C-S-d', 'RET'。"
  [^KeyEvent e]
  (let [code (.getCode e)
        modifiers (set
                    (cond-> []
                            (.isControlDown e) (conj "C")
                            (.isShiftDown e)   (conj "S")
                            (.isAltDown e)     (conj "M")
                            (.isMetaDown e)    (conj "s"))) ; Windows Key / Command
        modifier-str (if (seq modifiers)
                       (str (str/join "-" (sort modifiers)) "-")
                       "")
        key-name (case code
                   KeyCode/ENTER "RET"
                   KeyCode/SPACE "SPC"
                   KeyCode/TAB "TAB"
                   KeyCode/ESCAPE "ESC"
                   KeyCode/BACK_SPACE "DEL"
                   KeyCode/DELETE "DEL"
                   KeyCode/UP "UP"
                   KeyCode/DOWN "DOWN"
                   KeyCode/LEFT "LEFT"
                   KeyCode/RIGHT "RIGHT"
                   KeyCode/F1 "f1"
                   KeyCode/F2 "f2"
                   KeyCode/F3 "f3"
                   KeyCode/F4 "f4"
                   KeyCode/F5 "f5"
                   KeyCode/F6 "f6"
                   KeyCode/F7 "f7"
                   KeyCode/F8 "f8"
                   KeyCode/F9 "f9"
                   KeyCode/F10 "f10"
                   KeyCode/F11 "f11"
                   KeyCode/F12 "f12"
                   (let [ch (str/lower-case (.getText e))]
                     (if (and ch (not (str/blank? ch)))
                       ch
                       (.getName code))))]
    (keyword (str modifier-str key-name))))


(defn debounced
  "返回防抖回调：delay-ms 内的多次调用只执行最后一次。
   必须在 JavaFX 应用线程创建与调用。

   参数：
     - callback: (fn [value] ...) 实际执行的函数
     - delay-ms: 延迟毫秒数
   返回：
     - (fn [value] ...) 防抖包装后的回调"
  [callback delay-ms]
  (let [pending    (atom nil)
        transition (PauseTransition. (Duration/millis delay-ms))]
    (.setOnFinished transition
                    (reify EventHandler
                      (handle [_ _]
                        (when-let [v @pending]
                          (reset! pending nil)
                          (callback v)))))
    (fn [value]
      (reset! pending value)
      (.playFromStart transition))))

;; ── 节流 ─────────────────────────────────────────
(defn throttled
  "返回节流回调：每 interval-ms 至多执行一次，保证首帧立即执行，
   并在 interval 窗口结束后触发最后一次（尾部保证）。

   参数：
     - callback:    (fn [value] ...) 实际执行的函数
     - interval-ms: 节流间隔（毫秒）
   返回：
     - (fn [value] ...) 节流包装后的回调"
  [callback interval-ms]
  (let [last-fire  (atom 0)
        pending    (atom nil)
        transition (PauseTransition. (Duration/millis interval-ms))]
    (.setOnFinished transition
                    (reify EventHandler
                      (handle [_ _]
                        (when-let [v @pending]
                          (reset! pending nil)
                          (reset! last-fire (System/currentTimeMillis))
                          (callback v)))))
    (fn [value]
      (let [now (System/currentTimeMillis)]
        (if (>= (- now @last-fire) interval-ms)
          ;; 窗口已过，立即执行
          (do
            (reset! last-fire now)
            (callback value))
          ;; 窗口内，记录最后一次，等待尾部触发
          (do
            (reset! pending value)
            (.playFromStart transition)))))))