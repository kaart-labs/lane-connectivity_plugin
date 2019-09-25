// License: GPL. For details, see LICENSE file.
package com.kaart.laneconnectivity.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.kaart.laneconnectivity.gui.connector.ViaConnector;
import com.kaart.laneconnectivity.model.Junction;
import com.kaart.laneconnectivity.model.Lane;
import com.kaart.laneconnectivity.model.Road;

public abstract class State {
    static class AllTurns extends State {
        private final State wrapped;

        AllTurns(State wrapped) {
            this.wrapped = wrapped;
        }

        public State unwrap() {
            return wrapped;
        }

        @Override
        public State carryOver(GuiContainer newContainer) {
            return new AllTurns(wrapped.carryOver(newContainer));
        }
    }

    public static class Connecting extends State {
        private final Lane lane;
        private final List<ViaConnector> vias;

        public Connecting(Lane lane) {
            this(lane, Collections.<ViaConnector>emptyList());
        }

        public Connecting(Lane lane, List<ViaConnector> vias) {
            this.lane = lane;
            this.vias = vias;
        }

        public Connecting next(ViaConnector via) {
            if (vias.isEmpty()) {
                return new Connecting(lane, Collections.unmodifiableList(Arrays.asList(via)));
            }

            final List<ViaConnector> tmp = new ArrayList<>(vias.size() + 1);
            final boolean even = (vias.size() & 1) == 0;
            final ViaConnector last = vias.get(vias.size() - 1);

            if (last.equals(via) || !even && last.getRoadEnd().getJunction().equals(via.getRoadEnd().getJunction())) {
                return pop().next(via);
            }

            if (vias.size() >= 2) {
                if (lane.getOutgoingJunction().equals(via.getRoadEnd().getJunction())) {
                    return new Connecting(lane);
                } else if (via.equals(getBacktrackViaConnector())) {
                    return new Connecting(lane, vias.subList(0, vias.size() - 1));
                }
            }

            for (ViaConnector v : vias) {
                tmp.add(v);

                if (!(even && v.equals(last)) && v.getRoadEnd().getJunction().equals(via.getRoadEnd().getJunction())) {
                    return new Connecting(lane, Collections.unmodifiableList(tmp));
                }
            }

            tmp.add(via);
            return new Connecting(lane, Collections.unmodifiableList(tmp));
        }

        public Junction getJunction() {
            return vias.isEmpty() ? lane.getOutgoingJunction() : vias.get(vias.size() - 1).getRoadEnd().getJunction();
        }

        public ViaConnector getBacktrackViaConnector() {
            return vias.size() < 2 ? null : vias.get(vias.size() - 2);
        }

        public List<ViaConnector> getViaConnectors() {
            return vias;
        }

        public Lane getLane() {
            return lane;
        }

        public Connecting pop() {
            return new Connecting(lane, vias.subList(0, vias.size() - 1));
        }
    }

    static class Invalid extends State {
        private final State wrapped;

        Invalid(State wrapped) {
            this.wrapped = wrapped;
        }

        public State unwrap() {
            return wrapped;
        }
    }

    public static class Dirty extends State {
        private final State wrapped;

        public Dirty(State wrapped) {
            this.wrapped = wrapped;
        }

        public State unwrap() {
            return wrapped;
        }

        @Override
        public State carryOver(GuiContainer newContainer) {
            return new Dirty(wrapped.carryOver(newContainer));
        }
    }

    static class Default extends State {
        Default() {}
    }

    public static class IncomingActive extends State {
        private final Road.End roadEnd;

        public IncomingActive(Road.End roadEnd) {
            this.roadEnd = roadEnd;
        }

        public Road.End getRoadEnd() {
            return roadEnd;
        }

        @Override
        public State carryOver(GuiContainer newContainer) {
            if (newContainer.getModel().equals(roadEnd.getRoad().getContainer())) {
                return this;
            }

            final Junction newJunction = newContainer.getModel().getJunction(roadEnd.getJunction().getNode());

            for (Road.End e : newJunction.getRoadEnds()) {
                if (e.isToEnd() && e.getWay().equals(roadEnd.getWay())) {
                    return new IncomingActive(e);
                }
            }

            return new Default();
        }
    }

    public static class OutgoingActive extends State {
        private final LaneGui lane;

        public OutgoingActive(LaneGui lane) {
            this.lane = lane;
        }

        public LaneGui getLane() {
            return lane;
        }

        @Override
        public State delete() {
            if (!lane.getModel().isExtra()) {
                return this;
            }

            lane.getModel().remove();

            return new Invalid(this);
        }

        @Override
        public State carryOver(GuiContainer newContainer) {
            if (newContainer.equals(lane.getContainer())) {
                return this;
            }

            final Lane model = lane.getModel();
            final Junction newJunction = newContainer.getModel().getJunction(model.getOutgoingJunction().getNode());

            for (Road.End e : newJunction.getRoadEnds()) {
                if (e.isToEnd() && e.getWay().equals(model.getOutgoingRoadEnd().getWay())) {
                    for (Lane l : e.getLanes()) { // e.getLane(...) can fail on lane removal
                        if (l.getKind() == model.getKind() && l.getIndex() == model.getIndex()) {
                            return new OutgoingActive(newContainer.getGui(l));
                        }
                    }

                    break;
                }
            }

            return new Default();
        }
    }

    public State delete() {
        return this;
    }

    public State carryOver(GuiContainer newContainer) {
        return this;
    }
}
