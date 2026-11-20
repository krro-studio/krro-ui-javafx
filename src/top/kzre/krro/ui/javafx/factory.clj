(ns top.kzre.krro.ui.javafx.factory
  "JavaFX 平台实现：元素工厂与节点渲染器。适配层，调用 tags 创建节点，处理通用属性。
   多方法根据容器类型的全限定名字符串进行分派，避免编译期加载 JavaFX 类。"
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [taoensso.timbre :as log]
   [top.kzre.krro.ui.core.bind :as bind]
   [top.kzre.krro.ui.core.protocol :as proto]
   [top.kzre.krro.ui.core.spec.stylesheet :as stylesheet]
   [top.kzre.krro.ui.javafx.drag :as drag]
   [top.kzre.krro.ui.javafx.renderer :as renderer]
   [top.kzre.krro.ui.javafx.stylesheet :as javafx.stylesheet]
   [top.kzre.krro.ui.javafx.tags :as tags]
   [top.kzre.krro.ui.javafx.util :as javafx.util])
  (:import
   (java.io File)
   (java.net URI)
   (java.util Collection)
   (javafx.scene Node Parent)
   (javafx.scene.control
    Button
    CheckBox
    ComboBox
    Hyperlink
    Label
    MenuItem
    ProgressBar
    RadioButton
    Slider
    TextArea
    TextField)
   [javafx.scene.image ImageView]
   [javafx.scene.layout HBox VBox]))

(defonce ^:private stylesheet-cache
         ;; canonical-uri -> {:mtime long-or-nil, :url string}
         (atom {}))

(defn clear-stylesheet-cache!
  "清空编译缓存。开发期改了 EDN 后调用。"
  []
  (reset! stylesheet-cache {}))

(defn- uri-mtime
  "file: URI 返回 lastModified；classpath 等返回 nil（永不失效）。"
  [^URI uri]
  (when (and uri (= "file" (.getScheme uri)))
    (let [f (io/file uri)]
      (when (.exists f) (.lastModified f)))))


(defn- write-temp-css
  "把 CSS 写入临时文件，返回可给 JavaFX 用的 URL 字符串。"
  ^String [^String css]
  (let [f (doto (File/createTempFile "krro-style-" ".css")
            (.deleteOnExit)
            (spit css))]
    (str (.toURI f))))

(defn- cached-stylesheet-url
  "返回该 EDN 编译后的 CSS 临时文件 URL（按 canonical URI 缓存）。"
  ^String [^URI base-uri]
  (let [mtime (uri-mtime base-uri)
        hit   (get @stylesheet-cache base-uri)]
    (if (and hit (or (nil? mtime) (= mtime (:mtime hit))))
      (:url hit)
      (let [edn          (stylesheet/load-stylesheet-edn base-uri)
            [expanded _] (stylesheet/expand-imports
                           edn {:base-uri    base-uri
                                :in-progress #{base-uri}})
            css          (javafx.stylesheet/compile-stylesheet expanded)
            url          (write-temp-css css)]
        (swap! stylesheet-cache assoc base-uri {:mtime mtime :url url})
        url))))


;; ── 样式表 ──────────────────────────────────────────

(defn- edn-stylesheet?
  "判断 location 是否是待编译的 EDN 样式表。
   支持字符串与 java.net.URI；只看路径部分后缀，忽略 scheme。"
  [location]
  (let [path (cond
               (instance? URI location) (.getPath ^URI location)
               (string? location)       location
               :else                    nil)]
    (and (string? path) (str/ends-with? path ".edn"))))
;
;(defn- compile-edn-stylesheet
;  "把 EDN 样式表 location 编译为 JavaFX CSS 字符串。
;
;   - 以 location 归一化后的 URI 作为 base-uri，支持相对 import；
;   - 把自身加入 in-progress，检测自引用 / 循环 import；
;   - 兄弟 import 由 loaded 去重（这里只传一次，单文件内部生效）。"
;  [location]
;  (let [base-uri (stylesheet/->uri (str location))
;        edn      (stylesheet/load-stylesheet-edn base-uri)
;        [expanded _] (stylesheet/expand-imports
;                       edn {:base-uri    base-uri
;                            :in-progress #{base-uri}})]
;    ;; 已展开数据，compile-stylesheet 内部的单 arity expand-imports 是 no-op
;    (javafx.stylesheet/compile-stylesheet expanded)))


(defn- apply-stylesheets! [^Parent element new-styles]
  (let [styles (.getStylesheets element)]
    (.clear styles)
    (when new-styles
      (doseq [s (if (sequential? new-styles) new-styles [new-styles])]
        (try
          (if (edn-stylesheet? s)
            (.add styles (cached-stylesheet-url (stylesheet/->uri (str s))))
            (.add styles (str s)))
          (catch Exception e
            (log/error e "Failed to load stylesheet" s)))))))

;; ── 通用属性处理 ──────────────────────────────────────
(defn- set-style [^Node node style-map]
  (when style-map
    (let [css-str (->> (for [[k v] style-map]
                         (str "-fx-" (name k) ": " v ";"))
                       (str/join " "))]
      (.setStyle node css-str))))

(defn- set-class [^Node node class-val]
  (when class-val
    (let [classes (if (vector? class-val) class-val [class-val])]
      (.addAll ^Collection (.getStyleClass node) (map name classes)))))


;; ── 属性 diff ──────────────────────────────────────
(defn- attrs-diff! [element old-props new-props]
  ;; 样式
  (when (not= (:style old-props) (:style new-props))
    (if-let [style (:style new-props)]
      (set-style element style)
      (.setStyle element "")))
  ;; 类
  (when (not= (:class old-props) (:class new-props))
    (.clear (.getStyleClass element))
    (set-class element (:class new-props)))

  (let [grow (:grow new-props)]
    (when (not= grow (:grow old-props))
      (javafx.util/set-grow element grow)))

  ;; 样式表：仅对 Parent 类型节点生效
  (when (not= (:stylesheet old-props) (:stylesheet new-props))
    (if (instance? Parent element)
      (apply-stylesheets! element (:stylesheet new-props))
      (log/warn
        "should not mark stylesheet on a node not Parent."
        {:element   element
         :old-props old-props
         :new-props new-props})))

  ;; 可见性和禁用
  (when (not= (:visible old-props) (:visible new-props))
    (.setVisible element (boolean (:visible new-props))))
  (when (not= (:disabled old-props) (:disabled new-props))
    (.setDisable element (boolean (:disabled new-props))))

  ;; 拖拽：先清理旧的，再设置新的
  (when (not= (:drag-source old-props) (:drag-source new-props))
    (drag/clear-drag-source! element))
  (when (not= (:drag-target old-props) (:drag-target new-props))
    (drag/clear-drag-target! element))
  (when-let [ds (:drag-source new-props)]
    (drag/setup-drag-source! element ds))
  (when-let [dt (:drag-target new-props)]
    (drag/setup-drag-target! element dt))

  ;; 组件特定
  (cond
    (or (instance? VBox element) (instance? HBox element))
    (let [new-alignment (:alignment new-props :top-left)]
      (when (not= (:alignment old-props) new-alignment)
        (.setAlignment element (javafx.util/kw->pos new-alignment))))

    (instance? Label element)
    (when (not= (:content old-props) (:content new-props))
      (.setText ^Label element (or (:content new-props) "")))

    (instance? Button element)
    (when (not= (:content old-props) (:content new-props))
      (.setText ^Button element (or (:content new-props) "")))

    (instance? CheckBox element)
    (do
      (when (not= (:content old-props) (:content new-props))
        (.setText ^CheckBox element (or (:content new-props) "")))
      (when (not= (:checked old-props) (:checked new-props))
        (.setSelected ^CheckBox element (boolean (:checked new-props)))))

    (instance? RadioButton element)
    (do
      (when (not= (:content old-props) (:content new-props))
        (.setText ^RadioButton element (or (:content new-props) "")))
      (when (not= (:checked old-props) (:checked new-props))
        (.setSelected ^RadioButton element (boolean (:checked new-props)))))

    (instance? TextField element)
    (when (not= (:content old-props) (:content new-props))
      (.setText ^TextField element (or (:content new-props) "")))

    (instance? TextArea element)
    (when (not= (:content old-props) (:content new-props))
      (.setText ^TextArea element (or (:content new-props) "")))

    (instance? ComboBox element)
    (do
      (when (not= (:items old-props) (:items new-props))
        (let [^ComboBox cb element
              ^Collection cur-items (.getItems cb)]
          (.clear cur-items)
          (when-let [items (:items new-props)]
            (.addAll cur-items items))))
      (when (not= (:value old-props) (:value new-props))
        (.setValue ^ComboBox element (:value new-props))))

    (instance? Slider element)
    (when (not= (:value old-props) (:value new-props))
      (.setValue ^Slider element (double (:value new-props))))

    (instance? ProgressBar element)
    (when (not= (:value old-props) (:value new-props))
      (.setProgress ^ProgressBar element (double (:value new-props))))

    (instance? ImageView element)
    (when (not= (:src old-props) (:src new-props))
      (.setImage ^ImageView element (javafx.scene.image.Image. ^String (:src new-props))))

    (instance? Hyperlink element)
    (when (not= (:content old-props) (:content new-props))
      (.setText ^Hyperlink element (or (:content new-props) "")))

    (instance? MenuItem element)
    (when (not= (:content old-props) (:content new-props))
      (.setText ^MenuItem element (or (:content new-props) "")))))

(defn- register-bindings!
  "注册项目绑定与 Frame 绑定。"
  [^Node node bind bindf frame]
  (when bind
    (let [{:keys [path apply-fn getter bind-ctx]} bind
          ctx (or bind-ctx bind/*default-bind-manager*)]
      (bind/register! ctx node apply-fn :path path :getter getter)))
  (when bindf
    (let [{:keys [path apply-fn getter]} bindf
          ctx (renderer/ensure-frame-bind-ctx frame)]
      (bind/register! ctx node apply-fn :path path :getter getter))))

(defn- setup-hooks!
  "安装 vnode 生命週期钩子。"
  [vnode hooks]
  (when hooks
    (doseq [[k f] hooks]
      (proto/add-hook! vnode k f))))

;; ── 元素工厂 ────────────────────────────────────────
(defrecord JavaFxElementFactory []
  proto/IElementFactory
  (create-element [_ vnode frame]
    (let [tag    (proto/node-type vnode)
          props  (proto/node-props vnode)
          result (tags/create-element tag props frame)]
      (if (map? result)
        (let [{:keys [node hooks binding frame-binding]} result]
          ;; 1. 应用静态属性（样式、拖拽、样式表等）
          (attrs-diff! node {} props)
          ;; 2. 注册钩子
          (setup-hooks! vnode hooks)
          ;; 3. 注册绑定
          (register-bindings! node binding frame-binding frame)
          node)
        ;; 传统返回值（仅节点）
        (let [^Node node result]
          (attrs-diff! node {} props)
          node))))
  (update-properties [_ element old-vnode new-vnode]
    (if (-> new-vnode proto/node-hooks :on-update)
      element
      (let [old-props (proto/node-props old-vnode)
            new-props (proto/node-props new-vnode)]
        (attrs-diff! element old-props new-props))))
  (destroy-element [_ vnode f]
    (when-let [element (proto/node-element vnode)]
      (bind/unregister! element)
      (when-let [ctx (renderer/get-frame-bind-ctx f)]
        (bind/unregister! ctx element))
      (when-let [ctx (:bind-ctx (proto/node-props vnode))]
        (bind/unregister! ctx element)))))