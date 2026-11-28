(ns top.kzre.krro.ui.javafx.patcher
  (:require [top.kzre.krro.ui.core.protocol :as proto])
  (:import (top.kzre.krro.ui.javafx JavaFxNodePatcherHelper)))

(defrecord JavaFxNodePatcher []
  proto/INodePatcher
  (append-child [_ parent child]       (JavaFxNodePatcherHelper/appendChild parent child))
  (insert-child [_ parent child index] (JavaFxNodePatcherHelper/insertChild parent child (int index)))
  (remove-child [_ parent child]       (JavaFxNodePatcherHelper/removeChild parent child))
  (replace-child [_ parent old new]    (JavaFxNodePatcherHelper/replaceChild parent old new))
  (move-child [_ parent child idx]     (JavaFxNodePatcherHelper/moveChild parent child (int idx))))