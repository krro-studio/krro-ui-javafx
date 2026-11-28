(ns build
      (:require
        [clojure.tools.build.api :as b]
        [top.kzre.clj-build-template.simple :as tpl]))

(def params {:template (tpl/create-template)
             :artifact "krro-ui-javafx"
             :compile-java? true
             :java-version "17"
             :version "0.1.0"})

(def kfxc-params
  (assoc params
         :artifact "kfxc"
         :main 'top.kzre.krro.ui.javafx.tool.compile-stylesheet))

(defn clean [_]
      (tpl/clean))

(defn jar [_]
      (tpl/jar* params))

(defn install [_]
      (tpl/install params))

(defn kfxc [_]
      ;; 样式编译器命令行
      (b/delete {:path "target/classes/META-INF"})
      (tpl/compile-clj params)
      (tpl/uber kfxc-params))