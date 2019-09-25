// License: GPL. For details, see LICENSE file.
package com.kaart.laneconnectivity.gui;

import static com.kaart.laneconnectivity.gui.GuiUtil.area;
import static java.lang.Math.max;
import static java.lang.Math.min;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;

import com.kaart.laneconnectivity.gui.connector.OutgoingConnector;
import com.kaart.laneconnectivity.model.Lane;

public final class LaneGui {
    final class LengthSlider extends InteractiveElement {
        private final Point2D center = new Point2D.Double();
        private final Ellipse2D circle = new Ellipse2D.Double();

        private Point2D dragDelta;

        private LengthSlider() {}

        @Override
        public void paint(Graphics2D g2d, State state) {
            if (isVisible(state)) {
                g2d.setColor(Color.BLUE);
                g2d.fill(circle);

                final String len = METER_FORMAT.format(getLength() * getRoad().getContainer().getMpp());
                final Rectangle2D bounds = circle.getBounds2D();
                g2d.setFont(g2d.getFont().deriveFont(Font.BOLD, (float) bounds.getHeight()));
                g2d.drawString(len, (float) bounds.getMaxX(), (float) bounds.getMaxY());
            }
        }

        private boolean isVisible(State state) {
            if (state instanceof State.OutgoingActive) {
                return LaneGui.this.equals(((State.OutgoingActive) state).getLane());
            }

            return false;
        }

        @Override
        public boolean contains(Point2D p, State state) {
            return isVisible(state) && circle.contains(p);
        }

        @Override
        public Type getType() {
            return Type.INCOMING_CONNECTOR;
        }

        @Override
        public
        boolean beginDrag(double x, double y) {
            dragDelta = new Point2D.Double(center.getX() - x, center.getY() - y);
            return true;
        }

        @Override
        public
        State drag(double x, double y, InteractiveElement target, State old) {
            move(x + dragDelta.getX(), y + dragDelta.getY(), false);
            return new State.Dirty(old);
        }

        @Override
        public
        State drop(double x, double y, InteractiveElement target, State old) {
            move(x + dragDelta.getX(), y + dragDelta.getY(), true);
            return old;
        }

        void move(double x, double y, boolean updateModel) {
            final double r = getRoad().getConnectorRadius();

            final double offset = getRoad().getOffset(x, y);
            final double newLength = getModel().getOutgoingRoadEnd().isFromEnd() ? offset : getRoad().getLength()
                    - offset;
            final double adjustedLength = min(max(newLength, 0.1), getRoad().getLength());

            length = adjustedLength;
            if (updateModel) {
                getModel().setLength(adjustedLength * getRoad().getContainer().getMpp());
            }

            center.setLocation(x, y);
            circle.setFrame(x - r, y - r, 2 * r, 2 * r);
        }

        public void move(Point2D loc) {
            final double x = loc.getX();
            final double y = loc.getY();
            final double r = getRoad().getConnectorRadius();

            center.setLocation(x, y);
            circle.setFrame(x - r, y - r, 2 * r, 2 * r);
        }

        @Override
        public
        int getZIndex() {
            return 2;
        }
    }

    static final NumberFormat METER_FORMAT = new DecimalFormat("0.0m");

    private final RoadGui road;
    private final Lane lane;

    final Path2D area = new Path2D.Double();

    final OutgoingConnector outgoing = new OutgoingConnector(this);
    final LengthSlider lengthSlider;

    private Shape clip;
    private double length;

    public LaneGui(RoadGui road, Lane lane) {
        this.road = road;
        this.lane = lane;
        this.lengthSlider = lane.isExtra() ? new LengthSlider() : null;
        this.length = lane.isExtra() ? lane.getLength() / road.getContainer().getMpp() : Double.NaN;
    }

    public double getLength() {
        return lane.isExtra() ? length : road.getLength();
    }

    public Lane getModel() {
        return lane;
    }

    public RoadGui getRoad() {
        return road;
    }

    public GuiContainer getContainer() {
        return getRoad().getContainer();
    }

    public Path recalculate(Path inner, Path2D innerLine) {
        area.reset();

        double W = road.getContainer().getModel().isLeftDirection() ? -getContainer().getLaneWidth() : getContainer().getLaneWidth();

        final double L = getLength();

        final double WW = 3 / getContainer().getMpp();

        final LaneGui left = left();
        final Lane leftModel = left == null ? null : left.getModel();
        final double leftLength = leftModel == null
                || !leftModel.getOutgoingRoadEnd().equals(getModel().getOutgoingRoadEnd()) ? Double.NEGATIVE_INFINITY
                : leftModel.getKind() == Lane.Kind.EXTRA_LEFT ? left.getLength() : L;

        final Path outer;
        if (getModel().getKind() == Lane.Kind.EXTRA_LEFT) {
            final double AL = 30 / getContainer().getMpp();
            final double SL = max(L, leftLength + AL);

            outer = inner.offset(W, SL, SL + AL, 0);
            area(area, inner.subpath(0, L, true), outer.subpath(0, L + WW, true));

            lengthSlider.move(inner.getPoint(L, true));

            if (L > leftLength) {
                innerLine.append(inner.subpath(leftLength + WW, L, true).getIterator(), leftLength >= 0
                        || getModel().getOutgoingRoadEnd().isFromEnd());
                final Point2D op = outer.getPoint(L + WW, true);
                innerLine.lineTo(op.getX(), op.getY());
            }
        } else if (getModel().getKind() == Lane.Kind.EXTRA_RIGHT) {
            outer = inner.offset(W, L, L + WW, 0);
            area(area, inner.subpath(0, L + WW, true), outer.subpath(0, L, true));

            lengthSlider.move(outer.getPoint(L, true));
        } else {
            outer = inner.offset(W, -1, -1, W);
            area(area, inner, outer);

            if (leftLength < L) {
                innerLine.append(inner.subpath(leftLength + WW, L, true).getIterator(), leftLength >= 0
                        || getModel().getOutgoingRoadEnd().isFromEnd());
            }
        }

        return outer;
    }

    private LaneGui left() {
        final List<LaneGui> lanes = getRoad().getLanes(getModel().getOutgoingRoadEnd());
        final int i = lanes.indexOf(this);
        return i > 0 ? lanes.get(i - 1) : null;
    }

    public void fill(Graphics2D g2d) {
        final Shape old = g2d.getClip();
        g2d.clip(clip);
        g2d.fill(area);
        g2d.setClip(old);
    }

    public void setClip(Shape clip) {
        this.clip = clip;
    }

    public boolean contains(Point2D p) {
        return area.contains(p) && clip.contains(p);
    }
}
