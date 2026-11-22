(ns top.kzre.krro.ui.javafx.core
  "Krrō JavaFX 入口，管理窗口、交互器、状态栏、渲染器和 Frame。"
  (:require
    [top.kzre.krro.core.core :as krro]
   [top.kzre.krro.core.frame :as frame]
   [top.kzre.krro.core.interactive :as i]
    [top.kzre.krro.core.project :as proj]
   [top.kzre.krro.core.ui.protocol :as ui]
   [top.kzre.krro.core.util.re-export :refer [re-export]]
   [top.kzre.krro.core.variable :as variable]
   [top.kzre.krro.core.window :as win]
   [top.kzre.krro.ui.core.protocol :as proto]
   [top.kzre.krro.ui.javafx.factory :as factory]
   [top.kzre.krro.ui.javafx.interactor :as interactor]
   [top.kzre.krro.ui.javafx.patcher :as patcher]
   [top.kzre.krro.ui.javafx.plugin]
   [top.kzre.krro.ui.javafx.renderer :as renderer]
   [top.kzre.krro.ui.javafx.tags :as tags]
   [top.kzre.krro.ui.javafx.util]
   [top.kzre.krro.ui.javafx.window :as javafx.window])
  (:import
    (javafx.application Platform)))

(when variable/*debug*
  (System/setProperty "prism.verbose" "true"))

(re-export
  [top.kzre.krro.ui.javafx.util
   :refer [throttled debounced]])


(defn make-component
  "创建一个组件工厂函数。
   watched-props - 需要监听的 props 关键字向量，变化时触发 update
   create-fn     - 无参函数，返回组件的根节点（仅首次调用）
   init-fn       - (fn [node props frame] ...) 返回清理函数，每次 props 变化时调用。
                  首次挂载时也会调用，用于初始化组件内部运行时。
   返回一个组件函数，可注册为虚拟 DOM 标签。"
  [watched-props create-fn init-fn
   & {:keys [bind dispose-fn]}]
  (fn [props frame]
    (let [node (create-fn)                       ;; 创建根节点（仅一次）
          cleanup-atom (atom (fn []))
          init (fn [old-p p f]
                 (let [cleanup (init-fn node old-p p f)]
                   (reset! cleanup-atom cleanup)))
          update (fn [old-p p f] (init-fn node old-p p f))]
      (init nil props frame)                         ;; 首次初始化
      (merge
        {:node node
         :on-update (fn [_element old-vnode new-vnode]
                      (let [old-props (proto/node-props old-vnode)
                            new-props (proto/node-props new-vnode)]
                        (when (not= (select-keys old-props watched-props)
                                    (select-keys new-props watched-props))
                          (@cleanup-atom)           ;; 清理旧运行时
                          (update old-props new-props frame)))) ;; 重新初始化
         :on-unmount (fn [_ _]
                       (@cleanup-atom)
                       (when dispose-fn
                         (dispose-fn node)))}
        (tags/make-binding props bind)))))





;; ── 启动辅助 ────────────────────────────────────────────
(defn launch-app
  "启动 JavaFX 并执行初始化回调。
   回调签名为 (fn [window])，window 是 IWindow 实例。
   内部已创建渲染器、设置交互器、初始化项目。"
  [init-fn]
  (Platform/startup
    (fn []
      (proj/init-project!)
      (i/set-interactor! (interactor/->JavaFxInteractor))
      (let [{:keys [stage]} (javafx.window/create-stage)
            krro-renderer (renderer/make-renderer (factory/->JavaFxElementFactory)
                                                  (patcher/->JavaFxNodePatcher))]
        ;; 先设置渲染器，再创建窗口（窗口创建时会自动激活 fundamental 模式，此时渲染器已就绪）
        (ui/set-renderer! krro-renderer)
        (let [window (krro/create-window! stage)
              initial-frame (win/current-frame window)]
          ;; 保留兼容 TODO 移除
          (alter-var-root #'frame/*current-frame* (constantly initial-frame))
          ;; 调用外部回调，只传递 window
          (init-fn window))))))