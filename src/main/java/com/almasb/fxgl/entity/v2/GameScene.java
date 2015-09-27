/*
 * The MIT License (MIT)
 *
 * FXGL - JavaFX Game Library
 *
 * Copyright (c) 2015 AlmasB (almaslvl@gmail.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.almasb.fxgl.entity.v2;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.almasb.fxgl.asset.AssetManager;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.util.FXGLLogger;

import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.ImageCursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.transform.Scale;

public final class GameScene {

    private static final Logger log = FXGLLogger.getLogger("FXGL.GameScene");

    /**
     * Root for entities, it is affected by viewport movement.
     */
    private Group gameRoot = new Group();

    /**
     * Canvas for particles to accelerate drawing.
     */
    private Canvas particlesCanvas = new Canvas();

    /**
     * Graphics context for drawing particles.
     */
    private GraphicsContext particlesGC = particlesCanvas.getGraphicsContext2D();

    /**
     * The overlay root above {@link #gameRoot}. Contains UI elements, native JavaFX nodes.
     * May also contain entities as Entity is a subclass of Parent.
     * uiRoot isn't affected by viewport movement.
     */
    private Group uiRoot = new Group();

    /**
     * THE root of the {@link #gameScene}. Contains {@link #gameRoot}, {@link #particlesCanvas}
     * and {@link #uiRoot} in this order.
     */
    private Pane root = new Pane(gameRoot, particlesCanvas, uiRoot);

    private Scene scene = new Scene(root);

    private double sizeRatio;

    public GameScene(double width, double height, double sizeRatio) {
        this.sizeRatio = sizeRatio;

        root.setPrefSize(width, height);
        root.getTransforms().add(new Scale(sizeRatio, sizeRatio));

        // TODO: check scaling
        initParticlesCanvas(width, height);
    }

    private void initParticlesCanvas(double width, double height) {
        particlesCanvas.setWidth(width);
        particlesCanvas.setHeight(height);
        particlesCanvas.setMouseTransparent(true);
    }

    public void addGameNode() {

    }

    public void addUINode(Node node) {
        uiRoot.getChildren().add(node);
    }

    /**
     * Add a node to the UI overlay.
     *
     * @param n
     * @param nodes
     */
    public void addUINodes(Node... nodes) {
        uiRoot.getChildren().addAll(nodes);
    }

    /**
     * Remove given node from the UI overlay.
     *
     * @param n
     */
    public void removeUINode(Node n) {
        uiRoot.getChildren().remove(n);
    }

    public void removeUINodes(Node... nodes) {
        uiRoot.getChildren().removeAll(nodes);
    }

    public <T extends Event> void addEventHandler(EventType<T> eventType,
            EventHandler<? super T> eventHandler) {
        scene.addEventHandler(eventType, eventHandler);
    }

    /**
     * Returns render group for entity based on entity's
     * render layer. If no such group exists, a new group
     * will be created for that layer and placed
     * in the scene graph according to its layer index.
     *
     * @param e
     * @return
     */
    //private Group getRenderLayer(RenderLayer layer)
    private Group getRenderLayerFor(Entity e) {
        Integer renderLayer = e.getRenderLayer().index();
        Group group = gameRoot.getChildren()
                .stream()
                .filter(n -> (int)n.getUserData() == renderLayer)
                .findAny()
                .map(n -> (Group)n)
                .orElse(new Group());


        if (group.getUserData() == null) {
            log.finer("Creating render group for layer: " + e.getRenderLayer().asString());
            group.setUserData(renderLayer);
            gameRoot.getChildren().add(group);
        }

        List<Node> tmpGroups = new ArrayList<>(gameRoot.getChildren());
        tmpGroups.sort((g1, g2) -> (int)g1.getUserData() - (int)g2.getUserData());

        gameRoot.getChildren().setAll(tmpGroups);

        return group;
    }

    /**
     * Converts a point on screen to a point within game scene.
     *
     * @param screenPoint
     * @return
     */
    public Point2D screenToGame(Point2D screenPoint) {
        return screenPoint.multiply(1.0 / sizeRatio).add(getViewportOrigin());
    }

    /**
     * Sets viewport origin. Use it for camera movement.
     *
     * Do NOT use if the viewport was bound.
     *
     * @param x
     * @param y
     */
    public void setViewportOrigin(int x, int y) {
        gameRoot.setLayoutX(-x);
        gameRoot.setLayoutY(-y);
    }

    /**
     * Note: viewport origin, like anything in a scene, has top-left origin point.
     *
     * @return viewport origin
     */
    public Point2D getViewportOrigin() {
        return new Point2D(-gameRoot.getLayoutX(), -gameRoot.getLayoutY());
    }

    /**
     * Binds the viewport origin so that it follows the given entity
     * distX and distY represent bound distance between entity and viewport origin
     *
     * <pre>
     * Example:
     *
     * bindViewportOrigin(player, (int) (getWidth() / 2), (int) (getHeight() / 2));
     *
     * the code above centers the camera on player
     * For most platformers / side scrollers use:
     *
     * bindViewportOriginX(player, (int) (getWidth() / 2));
     *
     * </pre>
     *
     * @param entity
     * @param distX
     * @param distY
     */
    public void bindViewportOrigin(Entity entity, int distX, int distY) {
        gameRoot.layoutXProperty().bind(entity.xProperty().negate().add(distX));
        gameRoot.layoutYProperty().bind(entity.yProperty().negate().add(distY));
    }

    /**
     * Binds the viewport origin so that it follows the given entity
     * distX represent bound distance in X axis between entity and viewport origin.
     *
     * @param entity
     * @param distX
     */
    public void bindViewportOriginX(Entity entity, int distX) {
        gameRoot.layoutXProperty().bind(entity.xProperty().negate().add(distX));
    }

    /**
     * Binds the viewport origin so that it follows the given entity
     * distY represent bound distance in Y axis between entity and viewport origin.
     *
     * @param entity
     * @param distY
     */
    public void bindViewportOriginY(Entity entity, int distY) {
        gameRoot.layoutYProperty().bind(entity.yProperty().negate().add(distY));
    }

    /**
     * Set true if UI elements should forward mouse events
     * to the game layer
     *
     * @param b
     * @defaultValue false
     */
    public void setUIMouseTransparent(boolean b) {
        uiRoot.setMouseTransparent(b);
    }

    /**
     * Sets global game cursor using given name to find
     * the image cursor within assets/ui/cursors/.
     * Hotspot is location of the pointer end on the image.
     *
     * @param imageName
     * @param hotspot
     */
    public void setGameCursor(String imageName, Point2D hotspot) {
        scene.setCursor(new ImageCursor(AssetManager.INSTANCE.loadCursorImage(imageName),
                hotspot.getX(), hotspot.getY()));
    }
}
