package com.kaart.laneconnectivity.gui.connector;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;

import com.kaart.laneconnectivity.gui.InteractiveElement;
import com.kaart.laneconnectivity.gui.LaneGui;
import com.kaart.laneconnectivity.gui.State;
import com.kaart.laneconnectivity.model.Junction;
import com.kaart.laneconnectivity.model.Road;

public final class IncomingLaneConnector extends InteractiveElement {
    private final Point2D center = new Point2D.Double();
    private final Ellipse2D circle = new Ellipse2D.Double();

    private final LaneGui laneGui;

    public IncomingLaneConnector(LaneGui laneGui) {
        this.laneGui = laneGui;
    }

    @Override
    public void paintBackground(Graphics2D g2d, State state) {
        if (isActive(state)) {
            final Composite old = g2d.getComposite();
            g2d.setComposite(((AlphaComposite) old).derive(0.2f));

            g2d.setColor(new Color(255, 127, 31));
            laneGui.fill(g2d);

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

            g2d.setColor(Color.YELLOW);
            g2d.fill(circle);
            g2d.setComposite(old);
        }
    }

    private boolean isActive(State state) {
        return state instanceof State.OutgoingActive && laneGui.equals(((State.OutgoingActive) state).getLane());
    }

    private boolean isVisible(State state) {
    	if (laneGui.getRoad().getModel().isPrimary()) {
            return false;
        }
    	//Only make connector visible if possible relations would make sense
    	if (state instanceof State.Connecting) {
	    	final State.Connecting s = (State.Connecting) state;
	    	final Junction stateJunc =  s.getJunction();
	    	final Junction myJunc = laneGui.getModel().getRoad().getToEnd().getJunction();
	    	final Road stateRoad = s.getLane().getRoad();
	    	final Road myRoad = laneGui.getModel().getRoad();
	    	if (stateJunc.equals(myJunc) && !stateRoad.equals(myRoad)) {
	    		return true;
	    	}
	    	return false;
    	}
    	return false;
    }

    @Override
    public boolean contains(Point2D p, State state) {
        return isVisible(state) && (circle.contains(p) || laneGui.contains(p));
    }

    @Override
    public Type getType() {
        return Type.INCOMING_LANE_CONNECTOR;
    }

    @Override
    public State activate(State old) {
        return new State.OutgoingActive(laneGui);
    }

    public Point2D getCenter() {
        return (Point2D) center.clone();
    }

    public void move(double x, double y) {
        final double r = laneGui.getRoad().getConnectorRadius();

        center.setLocation(x, y);
        circle.setFrame(x - r, y - r, 2 * r, 2 * r);
    }

    @Override
    public int getZIndex() {
        return 1;
    }

    public LaneGui getLaneGui() {
	return laneGui;
    }
}