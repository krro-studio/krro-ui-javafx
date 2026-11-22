(ns top.kzre.krro.ui.javafx.interactor
  "交互器"
  (:require
    [top.kzre.krro.core.interactive :as i]
    [top.kzre.krro.core.message :as msg]
    [top.kzre.krro.ui.javafx.plugin]
    [top.kzre.krro.ui.javafx.util]
    [top.kzre.krro.ui.javafx.window])
  (:import
    (java.util Collection)
    (javafx.scene.control ChoiceDialog TextInputDialog)))




;; ── 交互器实现 ───────────────────────────────────────────
(defrecord JavaFxInteractor []
  i/IInteractor
  (read-args [_this spec]
    (mapv (fn [item]
            (let [[type prompt & opts] (if (keyword? item)
                                         [item (str "Enter " (name item) ": ")]
                                         item)
                  prompt (or prompt "Enter value: ")]
              (case type
                :string
                (let [dialog (TextInputDialog. "")]
                  (.setTitle dialog "Input")
                  (.setHeaderText dialog prompt)
                  (let [result (.showAndWait dialog)]
                    (if (.isPresent result)
                      (.get result)
                      "")))

                :number
                (let [dialog (TextInputDialog. "")]
                  (.setTitle dialog "Number Input")
                  (.setHeaderText dialog prompt)
                  (let [result (.showAndWait dialog)]
                    (if (.isPresent result)
                      (let [text (.get result)]
                        (try
                          (Long/parseLong text)
                          (catch NumberFormatException _
                            (try
                              (Double/parseDouble text)
                              (catch NumberFormatException _
                                (msg/warn "Invalid number format, returning nil")
                                nil)))))
                      nil)))

                :keyword
                (let [dialog (TextInputDialog. "")]
                  (.setTitle dialog "Keyword Input")
                  (.setHeaderText dialog prompt)
                  (let [result (.showAndWait dialog)]
                    (if (.isPresent result)
                      (keyword (.get result))
                      nil)))

                :choice
                (let [options-fn (first opts)
                      options (if (fn? options-fn) (options-fn) options-fn)
                      choice-prompt (or (second opts) "Choose: ")
                      ^ChoiceDialog dialog
                      (ChoiceDialog. (first options) ^Collection options)]
                  (.setTitle dialog "Choice")
                  (.setHeaderText dialog choice-prompt)
                  (let [result (.showAndWait dialog)]
                    (if (.isPresent result)
                      (.get result)
                      nil)))

                ;; 未知类型
                (do
                  (msg/error (str "Unsupported interactive spec: " type))
                  nil))))
          spec)))