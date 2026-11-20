(ns top.kzre.krro.ui.javafx.tool.compile-stylesheet
  "命令行工具：将 EDN 样式表编译为 JavaFX CSS 文件。"
  (:refer-clojure :exclude [compile])
  (:require
    [clojure.string :as str]
    [clojure.tools.cli :refer [parse-opts]]
    [top.kzre.krro.ui.core.spec.stylesheet :as stylesheet]
    [top.kzre.krro.ui.javafx.stylesheet :as javafx.stylesheet]))

;; ---------------------------------------------------------------------------
;; 纯函数层：tools.build / 编程调用入口
;; ---------------------------------------------------------------------------

(defn- load-and-merge
  "加载所有 src-files，并以各自文件为 base 展开其 import，然后合并为一个向量。

   跨文件共享 loaded 集合，保证同一被引文件（无论通过相对路径还是绝对路径）
   在整批源文件中只展开一次；in-progress 用顶层文件自身的 URI 初始化，
   以便检测文件内部直接/间接 import 自身。"
  [src-files]
  (first
    (reduce
      (fn [[acc loaded] path]
        (let [base-uri (stylesheet/->uri path)]
          (if (contains? loaded base-uri)
            ;; 已被其它文件 import 过，直接跳过
            [acc loaded]
            (let [edn (stylesheet/load-stylesheet-edn base-uri)]
              (when-not (vector? edn)
                (throw (ex-info "Invalid stylesheet: expected vector"
                                {:path path})))
              (let [[expanded loaded']
                    (stylesheet/expand-imports
                      edn {:base-uri    base-uri
                           :loaded      loaded
                           :in-progress #{base-uri}})]
                [(into acc expanded) (conj loaded' base-uri)])))))
      [[] #{}]
      src-files)))

(defn compile-stylesheet
  "tools.build 风格的纯 edn 输入，方便 deps.edn 编译集成。

   params 形如：
     {:src-files   [\"a.edn\" \"b.edn\"]   ; 必需，非空
      :output-path \"out.css\"}            ; 可选，nil 表示不写文件

   行为：
     - 加载并合并所有 src-files 中的 EDN 样式表（每个文件必须是向量）；
     - 调用 javafx.stylesheet/compile-stylesheet 编译；
     - 若提供 :output-path，则将结果写入该文件；
     - 始终返回编译后的 CSS 字符串。

   该函数不读命令行、不打印、不调用 System/exit，
   失败时以 ex-info 抛出异常，由调用方（CLI 或 tools.build）处理。"
  [{:keys [src-files output-path]}]
  (when (empty? src-files)
    (throw (ex-info "compile-stylesheet: :src-files must be non-empty"
                    {:params {:src-files src-files
                              :output-path output-path}})))
  (let [all-edn  (load-and-merge src-files)
        compiled (javafx.stylesheet/compile-stylesheet all-edn)]
    (when output-path
      (spit output-path compiled))
    compiled))

;; ---------------------------------------------------------------------------
;; CLI 层
;; ---------------------------------------------------------------------------

(def cli-options
  [["-o" "--output PATH"
    "输出 CSS 文件路径；省略则打印到标准输出"]
   ["-h" "--help"
    "显示帮助信息"]])

(defn- resolve-arguments
  "从位置参数中分离源文件与输出文件，得到纯 edn 的 params。

   兼容原行为：若最后一个位置参数以 .css 结尾，且未通过 -o/--output
   指定输出文件，则将其视为输出文件。"
  [arguments output-opt]
  (let [last-arg       (last arguments)
        positional-out (when (and (string? last-arg)
                                  (str/ends-with? last-arg ".css"))
                         last-arg)
        output-path    (or output-opt positional-out)
        src-files      (if (and positional-out (not output-opt))
                         (butlast arguments)
                         arguments)]
    {:src-files   src-files
     :output-path output-path}))

(defn compile
  "命令行输入，解析为 params 然后转发给 compile-stylesheet。

   用法：
     compile-stylesheet [options] input.edn ...
     compile-stylesheet [options] input1.edn input2.edn ... output.css

   选项：
     -o, --output PATH   输出 CSS 文件路径；省略则打印到标准输出
     -h, --help          显示帮助信息"
  [args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    (cond
      ;; 1. 帮助优先
      (:help options)
      (do (println "Usage: compile-stylesheet [options] input.edn ...")
          (println "       compile-stylesheet [options] input1.edn input2.edn ... output.css")
          (println)
          (println summary)
          (System/exit 0))

      ;; 2. 参数解析错误
      (seq errors)
      (do (binding [*out* *err*]
            (doseq [err errors]
              (println "Error:" err)))
          (System/exit 1))

      ;; 3. 没有输入文件
      (empty? arguments)
      (do (binding [*out* *err*]
            (println "Error: no input files specified.")
            (println "Usage: compile-stylesheet [options] input.edn ..."))
          (System/exit 1))

      ;; 4. 正常编译：解析为 params，转发给纯函数层
      :else
      (let [params (resolve-arguments arguments (:output options))]
        (try
          (let [compiled (compile-stylesheet params)]
            (if (:output-path params)
              (println "Compiled stylesheet written to" (:output-path params))
              (println compiled))
            (System/exit 0))
          (catch Exception e
            (binding [*out* *err*]
              (println "Error:" (.getMessage e)))
            (System/exit 1)))))))

;; ---------------------------------------------------------------------------
;; 入口
;; ---------------------------------------------------------------------------

(defn -main
  "编译样式表命令行入口。"
  [& args]
  (compile args))