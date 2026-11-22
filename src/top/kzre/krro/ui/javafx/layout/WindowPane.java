package top.kzre.krro.ui.javafx.layout;

import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.Objects;

/**
 * 纯功能性窗口容器，放在 UNDECORATED Stage 里。
 * 完全透明，不绘制任何内容，只提供边缘 resize 能力。
 * <p>
 * 事件处理采用捕获阶段的 EventFilter，因此即使内容子节点 consume 了鼠标事件，
 * 边缘 resize 仍然有效；非边缘区域的 pressed/dragged 不会 consume，正常传给子节点。
 * <p>
 * 注意：JavaFX 的光标由鼠标下方最深节点决定。若子节点自身设置了非 null cursor
 * （例如 TextArea 的 TEXT），边缘处可能看不到 resize 光标，但几何判断仍会触发 resize。
 */
public final class WindowPane extends Region {
    public static final int WINDOW_MARGIN = 8;
    private static final double ABSOLUTE_MIN_SIZE = 1;

    private final Stage stage;
    private final Node content;

    /** 当前 resize 方向；NONE 表示未在 resize。 */
    private ResizeEdge activeEdge = ResizeEdge.NONE;

    private double startStageX, startStageY;
    private double startStageW, startStageH;
    private double startScreenX, startScreenY;

    public WindowPane(Stage stage, Node content) {
        this.stage = Objects.requireNonNull(stage, "stage");
        this.content = Objects.requireNonNull(content, "content");

        getChildren().add(content);

        // 透明但存在的背景，确保 WindowPane 自身可被 pick（虽然 filter 不依赖 pick）
        setBackground(new Background(new BackgroundFill(Color.TRANSPARENT, null, null)));
        setPadding(Insets.EMPTY);

        setupResize();
    }

    @Override
    protected void layoutChildren() {
        // 内容填满整个区域，不留 margin
        layoutInArea(content, 0, 0, getWidth(), getHeight(), 0, HPos.CENTER, VPos.CENTER);
    }

    private void setupResize() {
        // ---------- 捕获阶段处理，避免子节点 consume 导致 resize 失效 ----------

        addEventFilter(MouseEvent.MOUSE_MOVED, e -> {
            if (!canResize()) {
                setCursor(Cursor.DEFAULT);
                return;
            }
            if (activeEdge != ResizeEdge.NONE) {
                return; // 拖拽中不刷新光标
            }
            setCursor(cursorFor(edgeFor(e.getX(), e.getY())));
            // 不 consume，允许子节点正常处理 hover
        });

        addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if (!canResize() || e.getButton() != MouseButton.PRIMARY) {
                return;
            }
            ResizeEdge edge = edgeFor(e.getX(), e.getY());
            if (edge == ResizeEdge.NONE) {
                return; // 非边缘，不拦截，交给子节点
            }

            activeEdge = edge;
            startStageX = stage.getX();
            startStageY = stage.getY();
            startStageW = stage.getWidth();
            startStageH = stage.getHeight();
            startScreenX = e.getScreenX();
            startScreenY = e.getScreenY();

            setCursor(cursorFor(edge));

            // 边缘区域：消费 pressed，阻止子节点收到不完整点击
            e.consume();
        });

        addEventFilter(MouseEvent.MOUSE_DRAGGED, e -> {
            if (activeEdge == ResizeEdge.NONE) {
                return;
            }
            // 中途窗口状态变化（最大化/全屏/不可 resize），终止
            if (!canResize()) {
                activeEdge = ResizeEdge.NONE;
                setCursor(Cursor.DEFAULT);
                e.consume();
                return;
            }

            e.consume();
            doResize(e.getScreenX(), e.getScreenY());
        });

        addEventFilter(MouseEvent.MOUSE_RELEASED, e -> {
            if (activeEdge == ResizeEdge.NONE) {
                return;
            }
            if (e.getButton() == MouseButton.PRIMARY) {
                activeEdge = ResizeEdge.NONE;
            }
            e.consume();

            if (activeEdge == ResizeEdge.NONE) {
                if (canResize() && isInside(e.getX(), e.getY())) {
                    setCursor(cursorFor(edgeFor(e.getX(), e.getY())));
                } else {
                    setCursor(Cursor.DEFAULT);
                }
            }
        });

        // 鼠标真正离开窗口时重置光标（进入子节点不算离开 WindowPane）
        addEventFilter(MouseEvent.MOUSE_EXITED, e -> {
            if (activeEdge == ResizeEdge.NONE) {
                setCursor(Cursor.DEFAULT);
            }
        });

        // 状态变化时立即终止 resize
        stage.maximizedProperty().addListener((o, ov, nv) -> abortResize());
        stage.fullScreenProperty().addListener((o, ov, nv) -> abortResize());
        stage.resizableProperty().addListener((o, ov, nv) -> {
            if (!nv) {
                abortResize();
            }
        });
    }

    private void abortResize() {
        activeEdge = ResizeEdge.NONE;
        setCursor(Cursor.DEFAULT);
    }

    private boolean canResize() {
        return stage.isShowing()
                && stage.isResizable()
                && !stage.isMaximized()
                && !stage.isFullScreen();
    }

    private boolean isInside(double x, double y) {
        return x >= 0 && y >= 0 && x <= getWidth() && y <= getHeight();
    }

    private void doResize(double screenX, double screenY) {
        double dx = screenX - startScreenX;
        double dy = screenY - startScreenY;

        double minW = Math.max(ABSOLUTE_MIN_SIZE, stage.getMinWidth());
        double minH = Math.max(ABSOLUTE_MIN_SIZE, stage.getMinHeight());

        double newX = startStageX;
        double newY = startStageY;
        double newW = startStageW;
        double newH = startStageH;

        if (activeEdge.west()) {
            newX = startStageX + dx;
            newW = startStageW - dx;
        } else if (activeEdge.east()) {
            newW = startStageW + dx;
        }

        if (activeEdge.north()) {
            newY = startStageY + dy;
            newH = startStageH - dy;
        } else if (activeEdge.south()) {
            newH = startStageH + dy;
        }

        boolean clampedW = false;
        boolean clampedH = false;

        // clamp 宽度，并同步修正 X，保证对侧边缘不动
        if (activeEdge.west()) {
            if (newW < minW) {
                newW = minW;
                newX = startStageX + startStageW - minW;
                clampedW = true;
            }
        } else if (activeEdge.east()) {
            if (newW < minW) {
                newW = minW;
                clampedW = true;
            }
        }

        if (activeEdge.north()) {
            if (newH < minH) {
                newH = minH;
                newY = startStageY + startStageH - minH;
                clampedH = true;
            }
        } else if (activeEdge.south()) {
            if (newH < minH) {
                newH = minH;
                clampedH = true;
            }
        }

        // 先位置后尺寸（west/north），尽量让对侧边缘稳定
        if (activeEdge.west()) {
            if (newX != stage.getX()) {
                stage.setX(newX);
            }
        }
        if (activeEdge.north()) {
            if (newY != stage.getY()) {
                stage.setY(newY);
            }
        }

        if (newW != stage.getWidth()) {
            stage.setWidth(newW);
        }
        if (newH != stage.getHeight()) {
            stage.setHeight(newH);
        }

        // 触底后重置基准，消除“反向拖动死区”
        if (clampedW) {
            startStageX = stage.getX();
            startStageW = stage.getWidth();
            startScreenX = screenX;
        }
        if (clampedH) {
            startStageY = stage.getY();
            startStageH = stage.getHeight();
            startScreenY = screenY;
        }
    }

    /**
     * 纯几何判断：坐标落在哪条/哪几条边上。
     */
    private ResizeEdge edgeFor(double x, double y) {
        double w = getWidth();
        double h = getHeight();
        double m = WINDOW_MARGIN;

        boolean left = x < m;
        boolean right = x > w - m;
        boolean top = y < m;
        boolean bottom = y > h - m;

        // 窗口过窄/过矮时，避免 left&&right / top&&bottom 同时成立
        if (left && right) {
            left = x < w / 2;
            right = !left;
        }
        if (top && bottom) {
            top = y < h / 2;
            bottom = !top;
        }

        if (left && top) return ResizeEdge.NW;
        if (right && top) return ResizeEdge.NE;
        if (left && bottom) return ResizeEdge.SW;
        if (right && bottom) return ResizeEdge.SE;
        if (left) return ResizeEdge.W;
        if (right) return ResizeEdge.E;
        if (top) return ResizeEdge.N;
        if (bottom) return ResizeEdge.S;
        return ResizeEdge.NONE;
    }

    private static Cursor cursorFor(ResizeEdge edge) {
        if (Objects.requireNonNull(edge) == ResizeEdge.N) {
            return Cursor.N_RESIZE;
        } else if (edge == ResizeEdge.S) {
            return Cursor.S_RESIZE;
        } else if (edge == ResizeEdge.E) {
            return Cursor.E_RESIZE;
        } else if (edge == ResizeEdge.W) {
            return Cursor.W_RESIZE;
        } else if (edge == ResizeEdge.NE) {
            return Cursor.NE_RESIZE;
        } else if (edge == ResizeEdge.NW) {
            return Cursor.NW_RESIZE;
        } else if (edge == ResizeEdge.SE) {
            return Cursor.SE_RESIZE;
        } else if (edge == ResizeEdge.SW) {
            return Cursor.SW_RESIZE;
        } else if (edge == ResizeEdge.NONE) {
            return Cursor.DEFAULT;
        }
        ;
         return Cursor.DEFAULT;
    }

    private enum ResizeEdge {
        NONE,
        N, S, E, W,
        NE, NW, SE, SW;

        boolean north() {
            return this == N || this == NE || this == NW;
        }

        boolean south() {
            return this == S || this == SE || this == SW;
        }

        boolean west() {
            return this == W || this == NW || this == SW;
        }

        boolean east() {
            return this == E || this == NE || this == SE;
        }
    }
}