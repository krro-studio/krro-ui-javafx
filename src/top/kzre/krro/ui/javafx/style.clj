(ns top.kzre.krro.ui.javafx.style
  (:require [clojure.string :as str]
            [taoensso.timbre :as log])
  (:import (top.kzre.jdom.fx ElementBox)
           (top.kzre.krro.ui.javafx JavaFXUtils)))

(defn- format-style-value [key-str value]
  (let [s (str value)]
    (if (ElementBox/isStringStyle key-str)
      (JavaFXUtils/ensureStringQuoted s)
      s)))


;; ---------- CSS 工具 ----------

(defn- css-entry
  [css key-str value]
  (let [entry (str key-str ": " (format-style-value key-str value) ";")]
    (if (str/blank? css)
      entry
      (str css " " entry))))

;; ---------- key 解析 ----------

(defn- resolve-unqualified-key
  "根据上下文解析未限定样式 key"
  [dom-node? k-str]
  (if dom-node?
    ;; dom 节点优先 -dom-
    (let [k-dom (str "-dom-" k-str)]
      (if (ElementBox/isStyle k-dom)
        {:type :css :key k-dom}
        (let [k-fx (str "-fx-" k-str)]
          (if (JavaFXUtils/isFxStyle k-fx)
            {:type :css :key k-fx}
            {:type :unknown :key k-str}))))
    ;; 非 dom 节点优先 -fx-
    (let [k-fx (str "-fx-" k-str)]
      (if (JavaFXUtils/isFxStyle k-fx)
        {:type :css :key k-fx}
        (let [k-dom (str "-dom-" k-str)]
          (if (ElementBox/isStyle k-dom)
            {:type :prop :key k-dom}
            {:type :unknown :key k-str}))))))

(defn- classify-style-key
  "返回 {:type :css/:prop/:unknown :key ...}"
  [k-str dom-node?]
  (cond
    ;; 已限定 fx 样式
    (JavaFXUtils/isFxStyle k-str)
    {:type :css :key k-str}

    ;; 已限定 dom 样式
    (ElementBox/isStyle k-str)
    (if dom-node?
      {:type :css :key k-str}
      {:type :prop :key k-str})

    ;; 其他已限定样式（如 -xxx-）视为 css
    (str/starts-with? k-str "-")
    {:type :css :key k-str}

    ;; 未限定样式
    :else
    (resolve-unqualified-key dom-node? k-str)))

;; ---------- 主 API ----------

(defn process-styles
  "对样式 map 进行预处理

   返回：
   {:css  \"...\"
    :props {...}
    :unknown [...]}

   opts:
   - dom-node?  当前上下文是否为 dom 节点
   - force-css? 强制将 prop 也输出到 css 字符串
   - on-unknown :warn / :drop / :pass"
  [style-map
   {:keys [dom-node? force-css? on-unknown]
    :or   {dom-node?  false
           force-css? false
           on-unknown :warn}
    :as   _opts}]
  (reduce
    (fn [acc [k v]]
      (let [k-str (str/trim (name k))
            {:keys [type key]}
            (classify-style-key k-str dom-node?)]
        (case type
          :css
          (update acc :css css-entry key v)

          :prop
          (if force-css?
            (update acc :css css-entry key v)
            (assoc-in acc [:props key] v))

          :unknown
          (case on-unknown
            :warn
            (do
              (log/warn "unknown unqualified style key=" k-str)
              (update acc :unknown conj {:key k :value v}))

            :pass
            (update acc :css css-entry k-str v)

            :drop
            acc))))
    {:css ""
     :props {}
     :unknown []}
    style-map))