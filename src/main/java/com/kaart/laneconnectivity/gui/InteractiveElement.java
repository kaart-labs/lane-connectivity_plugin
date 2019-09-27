// License: GPL. For details, see LICENSE file.
package com.kaart.laneconnectivity.gui;

import java.awt.Graphics2D;
import java.awt.geom.Point2D;

public abstract class InteractiveElement {
    public interface Type {
        Type INCOMING_CONNECTOR = new Type() {};
        Type OUTGOING_CONNECTOR = new Type() {};
        Type INCOMING_LANE_CONNECTOR = new Type() {};
        Type TURN_CONNECTION = new Type() {};
        Type LANE_ADDER = new Type() {};
        Type EXTENDER = new Type() {};
        Type VIA_CONNECTOR = new Type() {};
    }

    public void paintBackground(Graphics2D g2d, State state) {}

    public abstract void paint(Graphics2D g2d, State state);

    public abstract boolean contains(Point2D p, State state);

    public abstract Type getType();

    public State activate(State old) {
        return old;
    }

    public boolean beginDrag(double x, double y) {
        return false;
    }

    public State drag(double x, double y, InteractiveElement target, State old) {
        return old;
    }

    public State drop(double x, double y, InteractiveElement target, State old) {
        return old;
    }

    public abstract int getZIndex();

    public State click(State old) {
        return old;
    }
}
