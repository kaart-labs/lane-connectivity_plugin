package com.kaart.laneconnectivity.gui.connector;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import org.openstreetmap.josm.tools.Logging;

import com.kaart.laneconnectivity.gui.GuiContainer;
import com.kaart.laneconnectivity.gui.InteractiveElement;
import com.kaart.laneconnectivity.gui.LaneGui;
import com.kaart.laneconnectivity.gui.State;
import com.kaart.laneconnectivity.model.Road;

public final class OutgoingConnector extends InteractiveElement {
    private final Point2D center = new Point2D.Double();
    private final Ellipse2D circle = new Ellipse2D.Double();

    private Point2D dragLocation;
    private IncomingLaneConnector dropTarget;

    private final LaneGui laneGui;

    public OutgoingConnector(LaneGui laneGui) {
	//Remove me
	//Logging.info("Outgoing made");
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

        if (dragLocation != null) {
            final State.Connecting s = (State.Connecting) state;
            final Path2D path = new Path2D.Double();
            path.moveTo(center.getX(), center.getY());

            final List<ViaConnector> vias = s.getViaConnectors();
            for (int i = 0; i < vias.size() - 1; i += 2) {
                final ViaConnector v = vias.get(i);
                final PathIterator it = v.getRoad().getLaneMiddle(v.getRoadEnd().isFromEnd()).getIterator();
                path.append(it, true);
            }
            if ((vias.size() & 1) != 0) {
                final ViaConnector last = vias.get(vias.size() - 1);
                path.lineTo(last.getCenter().getX(), last.getCenter().getY());
            }

            if (dropTarget == null) {
                g2d.setColor(GuiContainer.RED);
                path.lineTo(dragLocation.getX(), dragLocation.getY());
            } else {
                g2d.setColor(GuiContainer.GREEN);
                path.lineTo(dropTarget.getCenter().getX(), dropTarget.getCenter().getY());
            }

            g2d.setStroke(laneGui.getContainer().getConnectionStroke());
            g2d.draw(path);
        }
    }

    @Override
    public void paint(Graphics2D g2d, State state) {
        if (isVisible(state)) {
            final Composite old = g2d.getComposite();
            if (isActive(state)) {
                g2d.setComposite(((AlphaComposite) old).derive(1f));
            }

            g2d.setColor(Color.WHITE);
            g2d.fill(circle);
            g2d.setComposite(old);
        }
    }

    private boolean isActive(State state) {
        return state instanceof State.OutgoingActive && laneGui.equals(((State.OutgoingActive) state).getLane());
    }

    private boolean isVisible(State state) {
	return true;
	/*
        if (state instanceof State.Connecting) {
            return ((State.Connecting) state).getLane().equals(laneGui.getModel());
        }
        return !laneGui.getRoad().getModel().isPrimary() && laneGui.getModel().getOutgoingJunction().isPrimary();
        */
    }

    @Override
    public boolean contains(Point2D p, State state) {
        return isVisible(state) && (circle.contains(p) || laneGui.contains(p));
    }

    @Override
    public Type getType() {
        return Type.OUTGOING_CONNECTOR;
    }

    @Override
    public State activate(State old) {
        return new State.OutgoingActive(laneGui);
    }

    @Override
    public boolean beginDrag(double x, double y) {
        return circle.contains(x, y);
    }

    @Override
    public State.Connecting drag(double x, double y, InteractiveElement target, State old) {
        dragLocation = new Point2D.Double(x, y);
        dropTarget = null;

        if (!(old instanceof State.Connecting)) {
            return new State.Connecting(laneGui.getModel());
        }

        final State.Connecting s = (State.Connecting) old;
        if (target != null && target.getType() == Type.INCOMING_LANE_CONNECTOR) {
            dropTarget = (IncomingLaneConnector) target;
            String outString = Integer.toString(dropTarget.getLaneGui().getModel().getIndex());
            Logging.info(outString);

            return (s.getViaConnectors().size() & 1) == 0 ? s : s.pop();
        } else if (target != null && target.getType() == Type.VIA_CONNECTOR) {
            return s.next((ViaConnector) target);
        }

        return s;
    }

    @Override
    public State drop(double x, double y, InteractiveElement target, State old) {
        final State.Connecting s = drag(x, y, target, old);
        dragLocation = null;
        if (dropTarget == null) {
            return activate(old);
        }

        final List<Road> via = new ArrayList<>();
        assert (s.getViaConnectors().size() & 1) == 0;
        for (int i = 0; i < s.getViaConnectors().size(); i += 2) {
            final ViaConnector a = s.getViaConnectors().get(i);
            final ViaConnector b = s.getViaConnectors().get(i + 1);
            assert a.getRoadEnd().getOppositeEnd().equals(b.getRoadEnd());
            via.add(a.getRoadEnd().getRoad());
        }

        // getModel().addTurn(via, dropTarget.getRoadEnd());
        // currently running my add rather then the lane add
        laneGui.getModel().addConnection(via, dropTarget.getLaneGui().getModel());
        dropTarget = null;
        return new State.Dirty(activate(old));
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
}