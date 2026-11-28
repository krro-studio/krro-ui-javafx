package top.kzre.krro.ui.javafx;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.ToolBar;
import top.kzre.jdom.fx.FxDOM;
import top.kzre.jdom.fx.FxElement;

import java.util.List;

public final class JavaFxNodePatcherHelper {
    private JavaFxNodePatcherHelper() {}

    // ========== append ==========

    public static void appendChild(Object parent, Object child) {
        if (parent instanceof MenuBar mb && child instanceof Menu m) {
            mb.getMenus().add(m);
        } else if (parent instanceof Menu m && child instanceof MenuItem mi) {
            m.getItems().add(mi);
        } else if (parent instanceof MenuItem) {
            throw new UnsupportedOperationException("MenuItem cannot have children");
        } else if (parent instanceof ToolBar tb && child instanceof Node n) {
            tb.getItems().add(n);
        } else if (parent instanceof TabPane tp && child instanceof Tab t) {
            tp.getTabs().add(t);
        } else if (parent instanceof SplitPane sp && child instanceof Node n) {
            sp.getItems().add(n);
        } else if (parent instanceof ScrollPane sp && child instanceof Node n) {
            sp.setContent(n);
        } else if (parent instanceof Pane pane && child instanceof Node n) {
            pane.getChildren().add(n);
        } else if (parent instanceof Group group && child instanceof Node n) {
            group.getChildren().add(n);
        } else if (parent instanceof FxDOM dom && child instanceof Node n) {
            dom.getRootElement().addChild(new FxElement(n));
        } else if (parent instanceof FxDOM dom && child instanceof FxElement el) {
            dom.getRootElement().addChild(el);
        } else if (parent instanceof FxElement el && child instanceof FxElement childFx) {
            el.addChild(childFx);
        } else if (parent instanceof FxElement el && child instanceof Node node) {
            el.addChild(new FxElement(node));
        } else {
            throw new UnsupportedOperationException(
                    "Cannot append " + (child != null ? child.getClass().getName() : "null")
                            + " to " + (parent != null ? parent.getClass().getName() : "null"));
        }
    }

    // ========== insert ==========

    public static void insertChild(Object parent, Object child, int index) {
        if (parent instanceof MenuBar mb && child instanceof Menu m) {
            mb.getMenus().add(index, m);
        } else if (parent instanceof Menu m && child instanceof MenuItem mi) {
            m.getItems().add(index, mi);
        } else if (parent instanceof ToolBar tb && child instanceof Node n) {
            tb.getItems().add(index, n);
        } else if (parent instanceof TabPane tp && child instanceof Tab t) {
            tp.getTabs().add(index, t);
        } else if (parent instanceof SplitPane sp && child instanceof Node n) {
            sp.getItems().add(index, n);
        } else if (parent instanceof ScrollPane sp && child instanceof Node n) {
            sp.setContent(n);
        } else if (parent instanceof Pane pane && child instanceof Node n) {
            pane.getChildren().add(index, n);
        } else if (parent instanceof Group group && child instanceof Node n) {
            group.getChildren().add(index, n);
        } else if (parent instanceof FxDOM dom && child instanceof Node n) {
            dom.getRootElement().addChild(index, new FxElement(n));
        } else if (parent instanceof FxDOM dom && child instanceof FxElement el) {
            dom.getRootElement().addChild(index, el);
        } else if (parent instanceof FxElement el && child instanceof FxElement childFx) {
            el.addChild(index, childFx);
        } else if (parent instanceof FxElement el && child instanceof Node node) {
            el.addChild(index, new FxElement(node));
        } else {
            throw new UnsupportedOperationException(
                    "Cannot insert " + (child != null ? child.getClass().getName() : "null")
                            + " into " + (parent != null ? parent.getClass().getName() : "null"));
        }
    }

    // ========== remove ==========

    public static void removeChild(Object parent, Object child) {
        if (parent instanceof MenuBar mb && child instanceof Menu m) {
            mb.getMenus().remove(m);
        } else if (parent instanceof Menu m && child instanceof MenuItem mi) {
            m.getItems().remove(mi);
        } else if (parent instanceof ToolBar tb && child instanceof Node n) {
            tb.getItems().remove(n);
        } else if (parent instanceof TabPane tp && child instanceof Tab t) {
            tp.getTabs().remove(t);
        } else if (parent instanceof SplitPane sp && child instanceof Node n) {
            sp.getItems().remove(n);
        } else if (parent instanceof ScrollPane sp) {
            sp.setContent(null);
        } else if (parent instanceof Pane pane && child instanceof Node n) {
            pane.getChildren().remove(n);
        } else if (parent instanceof Group group && child instanceof Node n) {
            group.getChildren().remove(n);
        } else if (parent instanceof FxDOM dom && child instanceof Node n) {
            dom.getRootElement().removeNode(n);
        } else if (parent instanceof FxDOM dom && child instanceof FxElement el) {
            dom.getRootElement().removeChild(el);
        } else if (parent instanceof FxElement el && child instanceof FxElement childFx) {
            el.removeChild(childFx);
        } else if (parent instanceof FxElement el && child instanceof Node node) {
            el.removeNode(node);
        } else {
            throw new UnsupportedOperationException(
                    "Cannot remove " + (child != null ? child.getClass().getName() : "null")
                            + " from " + (parent != null ? parent.getClass().getName() : "null"));
        }
    }

    // ========== replace ==========

    public static void replaceChild(Object parent, Object oldChild, Object newChild) {
        if (parent instanceof MenuBar mb && oldChild instanceof Menu oldM && newChild instanceof Menu newM) {
            int idx = mb.getMenus().indexOf(oldM);
            if (idx >= 0) mb.getMenus().set(idx, newM);
        } else if (parent instanceof Menu m && oldChild instanceof MenuItem oldMi && newChild instanceof MenuItem newMi) {
            int idx = m.getItems().indexOf(oldMi);
            if (idx >= 0) m.getItems().set(idx, newMi);
        } else if (parent instanceof ToolBar tb && oldChild instanceof Node oldN && newChild instanceof Node newN) {
            int idx = tb.getItems().indexOf(oldN);
            if (idx >= 0) tb.getItems().set(idx, newN);
        } else if (parent instanceof TabPane tp && oldChild instanceof Tab oldT && newChild instanceof Tab newT) {
            int idx = tp.getTabs().indexOf(oldT);
            if (idx >= 0) tp.getTabs().set(idx, newT);
        } else if (parent instanceof SplitPane sp && oldChild instanceof Node oldN && newChild instanceof Node newN) {
            int idx = sp.getItems().indexOf(oldN);
            if (idx >= 0) sp.getItems().set(idx, newN);
        } else if (parent instanceof ScrollPane sp && newChild instanceof Node newN) {
            sp.setContent(newN);
        } else if (parent instanceof Pane pane && oldChild instanceof Node oldN && newChild instanceof Node newN) {
            int idx = pane.getChildren().indexOf(oldN);
            if (idx >= 0) pane.getChildren().set(idx, newN);
        } else if (parent instanceof Group group && oldChild instanceof Node oldN && newChild instanceof Node newN) {
            int idx = group.getChildren().indexOf(oldN);
            if (idx >= 0) group.getChildren().set(idx, newN);
        }
        // ── FxDOM / FxElement 分支（硬编码，不依赖额外 API）──
        else if (parent instanceof FxDOM dom && oldChild instanceof Node oldN && newChild instanceof Node newN) {
            FxElement root = dom.getRootElement();
            int idx = indexOfNode(root, oldN);
            if (idx >= 0) {
                root.removeNode(oldN);
                root.addChild(idx, new FxElement(newN));
            }
        } else if (parent instanceof FxDOM dom && oldChild instanceof FxElement oldEl && newChild instanceof FxElement newEl) {
            FxElement root = dom.getRootElement();
            int idx = indexOfElement(root, oldEl);
            if (idx >= 0) {
                root.removeChild(oldEl);
                root.addChild(idx, newEl);
            }
        } else if (parent instanceof FxElement el && oldChild instanceof FxElement oldFx && newChild instanceof FxElement newFx) {
            int idx = indexOfElement(el, oldFx);
            if (idx >= 0) {
                el.removeChild(oldFx);
                el.addChild(idx, newFx);
            }
        } else if (parent instanceof FxElement el && oldChild instanceof Node oldN && newChild instanceof Node newN) {
            int idx = indexOfNode(el, oldN);
            if (idx >= 0) {
                el.removeNode(oldN);
                el.addChild(idx, new FxElement(newN));
            }
        } else if (parent instanceof FxElement el && oldChild instanceof FxElement oldFx && newChild instanceof Node newN) {
            int idx = indexOfElement(el, oldFx);
            if (idx >= 0) {
                el.removeChild(oldFx);
                el.addChild(idx, new FxElement(newN));
            }
        } else if (parent instanceof FxElement el && oldChild instanceof Node oldN && newChild instanceof FxElement newFx) {
            int idx = indexOfNode(el, oldN);
            if (idx >= 0) {
                el.removeNode(oldN);
                el.addChild(idx, newFx);
            }
        }// ── FxDOM old=FxElement, new=Node ──
        else if (parent instanceof FxDOM dom && oldChild instanceof FxElement oldEl && newChild instanceof Node newN) {
            FxElement root = dom.getRootElement();
            int idx = indexOfElement(root, oldEl);
            if (idx >= 0) {
                root.removeChild(oldEl);
                root.addChild(idx, new FxElement(newN));
            }
        }
        // ── FxDOM old=Node, new=FxElement ──
        else if (parent instanceof FxDOM dom && oldChild instanceof Node oldN && newChild instanceof FxElement newFx) {
            FxElement root = dom.getRootElement();
            int idx = indexOfNode(root, oldN);
            if (idx >= 0) {
                root.removeNode(oldN);
                root.addChild(idx, newFx);
            }
        }
        // ── MenuItem 不可替换子节点 ──
        else if (parent instanceof MenuItem) {
            throw new UnsupportedOperationException("MenuItem cannot have children");
        } else {
            throw new UnsupportedOperationException(
                    "Cannot replace in " + (parent != null ? parent.getClass().getName() : "null"));
        }
    }

    // ========== move ==========

    public static void moveChild(Object parent, Object child, int targetIndex) {
        if (parent instanceof MenuBar mb && child instanceof Menu m) {
            moveInList(mb.getMenus(), m, targetIndex);
        } else if (parent instanceof Menu m && child instanceof MenuItem mi) {
            moveInList(m.getItems(), mi, targetIndex);
        } else if (parent instanceof ToolBar tb && child instanceof Node n) {
            moveInList(tb.getItems(), n, targetIndex);
        } else if (parent instanceof TabPane tp && child instanceof Tab t) {
            moveInList(tp.getTabs(), t, targetIndex);
        } else if (parent instanceof SplitPane sp && child instanceof Node n) {
            moveInList(sp.getItems(), n, targetIndex);
        } else if (parent instanceof ScrollPane) {
            // 只有一个内容节点，移动无意义
        } else if (parent instanceof Pane pane && child instanceof Node n) {
            moveInList(pane.getChildren(), n, targetIndex);
        } else if (parent instanceof Group group && child instanceof Node n) {
            moveInList(group.getChildren(), n, targetIndex);
        }
        // ── FxDOM / FxElement 分支（硬编码，走 addChild/removeChild，不碰 unmodifiable list）──
        else if (parent instanceof FxDOM dom && child instanceof FxElement el) {
            FxElement root = dom.getRootElement();
            int idx = indexOfElement(root, el);
            if (idx >= 0 && idx != targetIndex) {
                root.removeChild(el);
                root.addChild(Math.min(targetIndex, root.getChildren().size() + 1), el);
            }
        } else if (parent instanceof FxDOM dom && child instanceof Node node) {
            FxElement root = dom.getRootElement();
            int idx = indexOfNode(root, node);
            if (idx >= 0 && idx != targetIndex) {
                FxElement found = (FxElement) root.getChildren().get(idx);
                root.removeNode(node);
                root.addChild(Math.min(targetIndex, root.getChildren().size() + 1), found);
            }
        } else if (parent instanceof FxElement el && child instanceof FxElement childFx) {
            int idx = indexOfElement(el, childFx);
            if (idx >= 0 && idx != targetIndex) {
                el.removeChild(childFx);
                el.addChild(Math.min(targetIndex, el.getChildren().size() + 1), childFx);
            }
        } else if (parent instanceof FxElement el && child instanceof Node node) {
            int idx = indexOfNode(el, node);
            if (idx >= 0 && idx != targetIndex) {
                FxElement found = (FxElement) el.getChildren().get(idx);
                el.removeChild(found);
                el.addChild(Math.min(targetIndex, el.getChildren().size() + 1), found);
            }
        } else {
            throw new UnsupportedOperationException(
                    "Cannot move " + (child != null ? child.getClass().getName() : "null")
                            + " in " + (parent != null ? parent.getClass().getName() : "null"));
        }
    }

    // ========== 通用 list move（仅用于可修改的 ObservableList） ==========

    private static <T> void moveInList(List<T> list, T element, int targetIndex) {
        int idx = list.indexOf(element);
        if (idx < 0 || idx == targetIndex) return;
        list.remove(idx);
        list.add(Math.min(targetIndex, list.size()), element);
    }

    // ========== 硬编码查找（不依赖 FxElement 暴露额外 API） ==========

    private static int indexOfElement(FxElement parent, FxElement target) {
        List<top.kzre.jdom.Element> children = parent.getChildren();
        for (int i = 0; i < children.size(); i++) {
            if (children.get(i) == target) return i;
        }
        return -1;
    }

    private static int indexOfNode(FxElement parent, Node target) {
        List<top.kzre.jdom.Element> children = parent.getChildren();
        for (int i = 0; i < children.size(); i++) {
            if (((FxElement) children.get(i)).getNode() == target) return i;
        }
        return -1;
    }
}