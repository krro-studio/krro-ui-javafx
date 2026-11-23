(ns top.kzre.krro.ui.javafx.tags
  "JavaFX 标签多方法。每个标签只创建对应的平台节点，返回节点和可选的绑定描述。
   数据绑定通过 :bindings 和 :frame-bindings 声明，由工厂统一注册。
   事件处理委托给 top.kzre.krro.ui.javafx.event，遵循平台无关的事件规范。
   保留对 Krrō 命令系统的支持：若事件属性值为关键字，则自动作为命令 ID 执行。
   双向绑定通过 getter（单向同步）和 setter/on-change（用户操作写回）实现，
   不再使用 ObservableValue 自动监听，确保单向数据流。"
  (:require
   [top.kzre.krro.core.core :as krro]
   [top.kzre.krro.ui.javafx.event :as event]
   [top.kzre.krro.ui.javafx.util :as javafx.util]
   [taoensso.timbre :as log]
   [top.kzre.krro.core.window :as win])
  (:import
    (java.util Collection)
    (javafx.scene Node)
    (javafx.scene.control
      Button
      CheckBox
      ComboBox
      Hyperlink
      Label
      Menu
      MenuBar
      MenuItem
      ProgressBar
      RadioButton
      ScrollPane
      Separator
      SeparatorMenuItem
      Slider
      SplitPane
      TextArea
      TextField
      ToolBar)
    (javafx.scene.image ImageView)
    (javafx.scene.layout HBox VBox)
    (top.kzre.krro.ui.javafx JavaFXUtils)))


(defmulti create-element (fn [tag _props _frame] tag))

(defn target-key [props]
  (or (:key props) (:id props) "unknown"))

(defn- resolve-action
  "将事件属性值转换为函数。若已是函数则直接返回；若为关键字则视为命令 ID 并返回执行该命令的函数；
   若为 [command-id & args] 向量，则返回带参数执行该命令的函数；否则返回 nil。"
  [on-spec]
  (cond
    (fn? on-spec)
    on-spec

    (keyword? on-spec)
    (fn [_] (krro/exe-command! on-spec))

    (and (vector? on-spec) (keyword? (first on-spec)))
    (let [cmd-id (first on-spec)
          args   (rest on-spec)]
      (fn [_] (apply krro/exe-command! cmd-id args)))

    :else nil))

(defn- resolve-events [props]
  (reduce
    (fn [acc k]
      (update acc k resolve-action))
    props
    (event/events)))

(defn- build-events! [^Node node props]
  (let [tk (target-key props)]
    (event/bind-events!
      node
      (resolve-events props) tk)))


(defn make-binding
  "生成一个绑定描述 map。若 props 包含 :getter，则使用 getter；否则使用 project-binding / frame-param-binding 路径。"
  [{:keys [getter bind bindf bind-ctx]} apply-fn]
  (if getter
    (let [b {:bind-ctx bind-ctx
             :getter getter
             :apply-fn apply-fn}]
      {:binding b})
    (if bind
      {:binding {:bind-ctx bind-ctx
                 :path bind
                 :apply-fn apply-fn}}
      (if bindf
        {:frame-binding {:path bindf :apply-fn apply-fn}}
        {}))))


(defn- polyfill-action
  "如果 props 有 :setter 但没有 :on-change，则自动添加一个调用 setter 的 :on-change 回调。"
  [props]
  (if (and (:setter props) (not (:on-change props)))
    (assoc props :on-change (fn [e] ((:setter props) (:new-value e))))
    props))

;; ── 布局容器 ──────────────────────────────────────────
(defmethod create-element :block [_ {:keys [direction
                                            grow]
                                     :or {direction :horizontal
                                          grow false}
                                     :as props} _]
  (let [box (if (= direction :vertical) (VBox.) (HBox.))]
    (build-events! box props)
    (javafx.util/set-grow box grow)
    {:node box}))

(defmethod create-element :split [_ props _]
  {:node (doto (SplitPane.)
           (.setOrientation
             (javafx.util/kw->orientation (:direction props :horizontal))))})


(defmethod create-element :scroll [_ _ _]
  (let [pane (ScrollPane.)]
    ;; 和内容适应
    (.setFitToWidth pane true)
    (.setFitToHeight pane true)
    {:node pane}))

(defmethod create-element :tool-bar [_ _ _] {:node (ToolBar.)})
(defmethod create-element :menu-bar [_ _ _] {:node (MenuBar.)})
(defmethod create-element :menu [_ props _] {:node (Menu. (or (:content props) ""))})
(defmethod create-element :menu-item [_ props _]
  (let [item (MenuItem. (or (:content props) ""))
        tk (target-key props)]
    (event/bind-action! item (update props :on-action resolve-action) tk)
    {:node item}))
(defmethod create-element :menu-separator [_ _ _] {:node (SeparatorMenuItem.)})

;; ── 基础控件 ──────────────────────────────────────────
(defmethod create-element :text [_ props _]
  (let [lbl (Label. (or (:content props) ""))]
    (build-events! lbl props)
    (merge
      {:node lbl}
      (make-binding props (fn [n v] (.setText ^Label n (str v)))))))

(defmethod create-element :button [_ props _]
  (let [btn (Button. (or (:content props) ""))]
    (build-events! btn props)
    {:node btn}))

(defmethod create-element :input [_ props f]
  (let [props' (polyfill-action props)
        tf (TextField. (or (:content props') ""))]
    (build-events! tf props')
    (merge {:node tf}
           (make-binding props' (fn [n v] (.setText ^TextField n (str v)))))))

(defmethod create-element :text-area [_ props f]
  (let [props' (polyfill-action props)
        ta (TextArea. (or (:content props') ""))]
    (build-events! ta props')
    (merge {:node ta}
           (make-binding props' (fn [n v] (.setText ^TextArea n (str v))))
      )))

(defmethod create-element :check-box [_ props f]
  (let [props' (polyfill-action props)
        cb (CheckBox. (or (:content props') ""))]
    ;; 当不存在绑定时候，使用checked?属性.
    (when-not (and (:getter props')
                   (:bind props')
                   (:bindf props'))
      (when-let [v (:checked props')] (.setSelected cb (boolean v))))
    (build-events! cb props')
    (merge {:node cb}
      (make-binding props'
                         (fn [c v]
                           (.setSelected ^CheckBox c (boolean v)))))))

(defmethod create-element :combo-box [_ props f]
  (let [props' (polyfill-action props)
        cb (ComboBox.)
        tk (target-key props')]
    (when-let [items (:items props')] (.addAll ^Collection (.getItems cb) items))
    (when-let [val (:value props')] (.setValue cb val))
    (build-events! tk props')
    (merge {:node cb}
           (make-binding props' (fn [c v]
                                  (.setValue ^ComboBox c v))))))

(defmethod create-element :slider [_ props f]
  (let [props' (polyfill-action props)
        sl (Slider. (double (or (:min props') 0)) (double (or (:max props') 100)) (double (or (:value props') 50)))
        ]
    (build-events! sl props')
    (merge {:node sl}
      (make-binding props' (fn [s v] (.setValue ^Slider s (double v)))))))

(defmethod create-element :progress [_ props _]
  {:node (ProgressBar. (double (or (:value props) 0)))})

(defmethod create-element :separator [_ _ _] {:node (Separator.)})


(defmethod create-element :image
  [_
   {:keys [src width height]
    :as props} _]
  (let [view (ImageView.)]
    (when-not src
      (log/warn "image src is nil"))
    (JavaFXUtils/imageViewSetSrc view src)
    (when width
      (.setFitWidth view (double width)))
    (when height
      (.setFitWidth view (double height)))
    (if (and width height)
      (.setPreserveRatio view false)
      (.setPreserveRatio view true))

    (build-events! view props)
    {:node view}))


(defmethod create-element :radio-button [_ props f]
  (let [props' (polyfill-action props)
        rb (RadioButton. (or (:content props') ""))
        tk (target-key props')]
    (when-let [v (:checked props')] (.setSelected rb (boolean v)))
    (build-events! rb props)
    (merge {:node rb}
           (make-binding props' (fn [r v] (.setSelected ^RadioButton r (boolean v))))
           )))

(defmethod create-element :link [_ props _]
  (let [hl (Hyperlink. (or (:content props) ""))]
    (build-events! hl props)
    {:node hl}))

