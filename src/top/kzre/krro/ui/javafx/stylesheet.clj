(ns top.kzre.krro.ui.javafx.stylesheet
  "JavaFX 样式表编译器。将 EDN 样式描述编译为 CSS 字符串。"
  (:require
    [top.kzre.krro.ui.core.spec.stylesheet :as stylesheet]
    [clojure.string :as str]
    [top.kzre.krro.ui.javafx.style :as style]))

;; ── 内部辅助函数 ──────────────────────────────────

(defn- compile-rule
  "编译单条规则及其子规则，返回 CSS 字符串。"
  [rule parent-selector bindings]
  (let [{:keys [selector styles children]} rule

        ;; 选择器解析
        full-selector
        (if (and parent-selector (str/starts-with? selector "&"))
          (str parent-selector (subs selector 1))
          (if parent-selector
            (str parent-selector " " selector)
            selector))

        ;; 复用 process-styles
        {:keys [css]}
        (style/process-styles
          (stylesheet/resolve-values bindings styles)
          {:dom-node?  false
           :force-css? true      ;; 样式表永远输出 CSS
           :on-unknown :warn})

        ;; 子规则
        children-css
        (->> children
             (map #(compile-rule % full-selector bindings))
             (str/join ""))]

    (str full-selector "{" css "}" children-css)))

;; ── 公共 API ──────────────────────────────────

(defn compile-stylesheet
  "将样式表 EDN 编译为 JavaFX CSS 字符串。
   参数：
     - stylesheet-edn: 样式表 EDN 数据（向量）
   返回 CSS 字符串。"
  [stylesheet-edn]
  (let [expanded (stylesheet/expand-imports stylesheet-edn)
        {:keys [bindings rules]} (stylesheet/parse-stylesheet expanded)
        compiled-rules (map #(compile-rule % nil bindings ) rules)]
    (str/join "\n" compiled-rules)))

