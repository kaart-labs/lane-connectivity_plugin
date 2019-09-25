package com.kaart.laneconnectivity.gui.connector;

import static com.kaart.laneconnectivity.gui.GuiUtil.angle;
import static com.kaart.laneconnectivity.gui.GuiUtil.closest;
import static com.kaart.laneconnectivity.gui.GuiUtil.relativePoint;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

import com.kaart.laneconnectivity.gui.InteractiveElement;
import com.kaart.laneconnectivity.gui.RoadGui;
import com.kaart.laneconnectivity.gui.State;
import com.kaart.laneconnectivity.model.Road;

public final class ViaConnector extends InteractiveElement {
    private final RoadGui roadGui;
    private final Road.End end;

    private final Line2D line;
    private final float strokeWidth;

    public ViaConnector(RoadGui roadGui, Road.End end) {
        this.end = end;
        this.roadGui = roadGui;
        this.line = new Line2D.Double(roadGui.getLeftCorner(end), roadGui.getRightCorner(end));
        this.strokeWidth = (float) (3 * roadGui.getContainer().getLaneWidth() / 4);
    }

    @Override
    public void paint(Graphics2D g2d, State state) {
        if (isVisible(state)) {
            g2d.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER));
            g2d.setColor(Color.ORANGE);
            g2d.draw(line);
        }
    }

    @Override
    public boolean contains(Point2D p, State state) {
        if (!isVisible(state)) {
            return false;
        }

        final Point2D closest = closest(line, p);
        return p.distance(closest) <= strokeWidth / 2;
    }

    private boolean isVisible(State state) {
        if (!(state instanceof State.Connecting)) {
            return false;
        }

        final State.Connecting s = (State.Connecting) state;

        if (s.getJunction().equals(end.getJunction()) || equals(s.getBacktrackViaConnector())) {
            return true;
        } else if (!s.getViaConnectors().isEmpty()
                && s.getViaConnectors().get(s.getViaConnectors().size() - 1).getRoadModel().equals(getRoadModel())) {
            return true;
        }

        return false;
    }

    private Road getRoadModel() {
        return roadGui.getModel();
    }

    public RoadGui getRoad() {
        return roadGui;
    }

    @Override
    public Type getType() {
        return Type.VIA_CONNECTOR;
    }

    @Override
    public int getZIndex() {
        return 1;
    }

    public Road.End getRoadEnd() {
        return end;
    }

    public Point2D getCenter() {
        return relativePoint(line.getP1(), line.getP1().distance(line.getP2()) / 2, angle(line.getP1(), line.getP2()));
    }
}