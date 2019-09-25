package com.kaart.laneconnectivity.gui.connector;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.kaart.laneconnectivity.gui.InteractiveElement;
import com.kaart.laneconnectivity.gui.LaneGui;
import com.kaart.laneconnectivity.gui.RoadGui;
import com.kaart.laneconnectivity.gui.State;
import com.kaart.laneconnectivity.model.Lane;
import com.kaart.laneconnectivity.model.Road;

public final class IncomingConnector extends InteractiveElement {
    private final RoadGui roadGui;
    private final Road.End end;
    private final List<LaneGui> lanes;

    private final Point2D center = new Point2D.Double();
    private final Ellipse2D circle = new Ellipse2D.Double();

    public IncomingConnector(RoadGui gui, Road.End end) {
        this.end = end;
        this.roadGui = gui;

        final List<LaneGui> lanes = new ArrayList<>(end.getLanes().size());
        for (Lane l : end.getOppositeEnd().getLanes()) {
            lanes.add(new LaneGui(gui, l));
        }
        this.lanes = Collections.unmodifiableList(lanes);
    }

    @Override
    public void paintBackground(Graphics2D g2d, State state) {
        if (isActive(state)) {
            final Composite old = g2d.getComposite();
            g2d.setComposite(((AlphaComposite) old).derive(0.2f));

            g2d.setColor(new Color(255, 127, 31));

            for (LaneGui l : lanes) {
                l.fill(g2d);
            }

            g2d.setComposite(old);
        }
    }

    @Override
    public void paint(Graphics2D g2d, State state) {
        if (isVisible(state)) {
            final Composite old = g2d.getComposite();
            if (isActive(state)) {
                g2d.setComposite(((AlphaComposite) old).derive(1f));
            }

            g2d.setColor(Color.LIGHT_GRAY);
            g2d.fill(circle);

            g2d.setComposite(old);
        }
    }

    private boolean isActive(State state) {
        if (!(state instanceof State.IncomingActive)) {
            return false;
        }

        final Road.End roadEnd = ((State.IncomingActive) state).getRoadEnd();

        return roadEnd.equals(getRoadEnd());
    }

    private boolean isVisible(State state) {
        if (roadGui.getModel().isPrimary() || !getRoadEnd().getJunction().isPrimary()
                || getRoadEnd().getOppositeEnd().getLanes().isEmpty()) {
            return false;
        }

        if (state instanceof State.Connecting) {
            return ((State.Connecting) state).getJunction().equals(getRoadEnd().getJunction());
        }

        return true;
    }

    @Override
    public boolean contains(Point2D p, State state) {
        if (!isVisible(state)) {
            return false;
        } else if (circle.contains(p)) {
            return true;
        }

        for (LaneGui l : lanes) {
            if (l.contains(p)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public Type getType() {
        return Type.INCOMING_CONNECTOR;
    }

    @Override
    public State activate(State old) {
        return new State.IncomingActive(getRoadEnd());
    }

    public Point2D getCenter() {
        return (Point2D) center.clone();
    }

    public void move(double x, double y) {
        final double r = roadGui.getConnectorRadius();

        center.setLocation(x, y);
        circle.setFrame(x - r, y - r, 2 * r, 2 * r);
    }

    public Road.End getRoadEnd() {
        return end;
    }

    public List<LaneGui> getLanes() {
        return lanes;
    }

    @Override
    public int getZIndex() {
        return 1;
    }

    public void add(LaneGui lane) {
        lanes.add(lane);
    }
}