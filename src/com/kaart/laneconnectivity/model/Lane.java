// License: GPL. For details, see LICENSE file.
package com.kaart.laneconnectivity.model;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;

public class Lane {
    public enum Kind {
        EXTRA_LEFT,
        EXTRA_RIGHT,
        REGULAR;

        public boolean isExtra() {
            return this == EXTRA_LEFT || this == EXTRA_RIGHT;
        }
    }

    static List<Lane> load(Road.End roadEnd) {
        final List<Lane> result = new ArrayList<>();
        int i;

        i = 0;
        for (double l : roadEnd.getLengths(Kind.EXTRA_LEFT)) {
            result.add(new Lane(roadEnd, --i, Kind.EXTRA_LEFT, l));
        }
        Collections.reverse(result);

        final int regulars = getRegularCount(roadEnd.getWay(), roadEnd.getJunction().getNode());
        for (i = 1; i <= regulars; ++i) {
            result.add(new Lane(roadEnd, i));
        }

        i = 0;
        for (double l : roadEnd.getLengths(Kind.EXTRA_RIGHT)) {
            result.add(new Lane(roadEnd, ++i, Kind.EXTRA_RIGHT, l));
        }

        return result;
    }

    static List<Double> loadLengths(Relation r, String key, double lengthBound) {
        final List<Double> result = new ArrayList<>();

        if (r != null && r.get(key) != null) {
            for (String s : Constants.SPLIT_PATTERN.split(r.get(key))) {
                // TODO what should the exact input be (there should probably be
                // a unit (m))
                final Double length = Double.parseDouble(s.trim());

                if (length >= lengthBound) {
                    result.add(length);
                }
            }
        }

        return result;
    }

    static int getRegularCount(Way w, Node end) {
        final int count = TurnlanesUtils.parseIntTag(w, "lanes");
        final boolean forward = w.lastNode().equals(end);

        if (w.hasDirectionKeys()) {
            return getRegularCountOneWay(w, forward, count);
        } else {
            return getRegularCountTwoWay(w, forward, count);
        }
    }

    private static int getRegularCountOneWay(Way w, boolean forward, final int count) {
        if (forward ^ "-1".equals(w.get("oneway"))) {
            return count;
        } else {
            return 0;
        }
    }

    private static int getRegularCountTwoWay(Way w, boolean forward, final int count) {
        if (w.get("lanes:backward") != null) {
            final int backwardCount = TurnlanesUtils.parseIntTag(w, "lanes:backward");
            return forward ? count - backwardCount : backwardCount;
        }

        if (w.get("lanes:forward") != null) {
            final int forwardCount = TurnlanesUtils.parseIntTag(w, "lanes:forward");
            return forward ? forwardCount : count - forwardCount;
        }

        // default: round up in forward direction...
        return forward ? (count + 1) / 2 : count / 2;
    }

    private final Road.End roadEnd;
    private final int index;
    private final Kind kind;

    private Set<Turn> turns;
    private double length = -1;

    public Lane(Road.End roadEnd, int index) {
        this.roadEnd = roadEnd;
        this.index = index;
        this.kind = Kind.REGULAR;
    }

    public Lane(Road.End roadEnd, int index, Kind kind, double length) {
        if (kind == Kind.EXTRA_LEFT || kind == Kind.EXTRA_RIGHT) {
            this.roadEnd = roadEnd;
            this.index = index;
            this.kind = kind;
            this.length = length;

            if (length <= 0) {
                throw new IllegalArgumentException("Length must be positive");
            }
        } else {
            throw new IllegalArgumentException("kind must be Kind.EXTRA_LEFT or Kind.EXTRA_RIGHT");
        }
    }

    public Road getRoad() {
        return roadEnd.getRoad();
    }

    public Kind getKind() {
        return kind;
    }

    public double getLength() {
        return isExtra() ? length : getRoad().getLength();
    }

    public void setLength(double length) {
        if (!isExtra()) {
            throw new UnsupportedOperationException("Length can only be set for extra lanes.");
        } else if (length <= 0) {
            throw new IllegalArgumentException("Length must positive.");
        }

        this.length = length;

        // TODO if needed, increase length of other lanes
        getOutgoingRoadEnd().updateLengths();
    }

    public boolean isExtra() {
        return getKind() != Kind.REGULAR;
    }

    public int getIndex() {
        return index;
    }

    public String genericConnectivity(int laneCount1,int laneCount2){
      String connectivity =null;
      String connectivity2;
      if (laneCount1==laneCount2) {
        for(int i=0;i <laneCount1;i++){
          if(i==0)connectivity = "1:1";
          else{
            connectivity2 = "|" + (i + 1) + ":" + (i + 1);
            connectivity += connectivity2;
          }
        }
      }
      else if(laneCount1>laneCount2){
        for(int i=0;i<laneCount2;i++){
          if(i==0)connectivity = "1:1";
          else{
            connectivity2 = "|" + (i + 1) + ":" + (i + 1);
            connectivity = connectivity + connectivity2;
          }
        }
      }
      else if(laneCount1<laneCount2){
        for(int i=0;i<laneCount1;i++){
          if(i==0)connectivity = "1:1";
          else{
            connectivity2 = "|" + (i + 1) + ":" + (i + 1);
            connectivity = connectivity + connectivity2;
          }
        }
      }
      return connectivity;
    }

    public Junction getOutgoingJunction() {
        return getOutgoingRoadEnd().getJunction();
    }

    public Junction getIncomingJunction() {
        return getIncomingRoadEnd().getJunction();
    }

    public Road.End getOutgoingRoadEnd() {
        return roadEnd;
    }

    public Road.End getIncomingRoadEnd() {
        return roadEnd.getOppositeEnd();
    }

    public ModelContainer getContainer() {
        return getRoad().getContainer();
    }

    /**
     * Add a turn relation
     * @param via The list of via Roads
     * @param to The Road that is getting the "to" role
     */
    public void addTurn(List<Road> via, Road.End to) {
        final GenericCommand cmd = new GenericCommand(getOutgoingJunction().getNode().getDataSet(), tr("Add turn"));

        Relation existing = null;
        for (Turn t : to.getTurns()) {
            if (t.getFrom().getOutgoingRoadEnd().equals(getOutgoingRoadEnd()) && t.getVia().equals(via)) {
                if (t.getFrom().equals(this)) {
                    // was already added
                    return;
                }

                existing = t.getRelation();
            }
        }

        final Relation r;
        if (existing == null) {
            r = new Relation();
            r.put("type", Constants.TYPE_TURNS);

            r.addMember(new RelationMember(Constants.ROLE_FROM, getOutgoingRoadEnd().getWay()));
            if (via.isEmpty()) {
                r.addMember(new RelationMember(Constants.ROLE_VIA, getOutgoingJunction().getNode()));
            } else {
                for (Way w : TurnlanesUtils.flattenVia(getOutgoingJunction().getNode(), via, to.getJunction().getNode())) {
                    r.addMember(new RelationMember(Constants.ROLE_VIA, w));
                }
            }
            r.addMember(new RelationMember(Constants.ROLE_TO, to.getWay()));

            cmd.add(r);
        } else {
            r = existing;
        }

        final String key = isExtra() ? Constants.TURN_KEY_EXTRA_LANES : Constants.TURN_KEY_LANES;
        // TODO don't use keySet, use all of the information.
        final List<Integer> lanes = new ArrayList<>(Turn.indices(r, key).keySet());
        lanes.add(getIndex());
        cmd.backup(r).put(key, Turn.join(lanes));

        UndoRedoHandler.getInstance().add(cmd);
    }
//here is my stuff, currently does not work for everything i need. has bug where keeps added relations even after it exists,
    public void addConnection(List<Road> via, Road.End to) {
        final GenericCommand cmd = new GenericCommand(getOutgoingJunction().getNode().getDataSet(), tr("Add connectivity"));

        int laneCount1;
        int laneCount2;

        Relation existing = null;
        //this for loop doesn't actually do anything.
        //to clarify this loop is what checks if the Relation already exists, however this version only works for turnlane:turns with a lane tag on the relation
        for (Turn t : to.getTurns()) {
            if (t.getFrom().getOutgoingRoadEnd().equals(getOutgoingRoadEnd()) && t.getVia().equals(via)) {
                if (t.getFrom().equals(this)) {
                    // was already added
                    return;
                }

                existing = t.getRelation();
            }
        }

        //generic connectivity
        laneCount1 = Integer.parseInt(getOutgoingRoadEnd().getWay().get("lanes"));
        laneCount2 = Integer.parseInt(to.getWay().get("lanes"));
        String connectivity = genericConnectivity(laneCount1, laneCount2);

        final Relation r;
        if (existing == null) {
            r = new Relation();
            r.put("type", Constants.TYPE_CONNECTIVITY);
            r.put(Constants.TYPE_CONNECTIVITY, connectivity);

            r.addMember(new RelationMember(Constants.ROLE_FROM, getOutgoingRoadEnd().getWay()));
            if (via.isEmpty()) {
                r.addMember(new RelationMember(Constants.ROLE_VIA, getOutgoingJunction().getNode()));
            } else {
                for (Way w : TurnlanesUtils.flattenVia(getOutgoingJunction().getNode(), via, to.getJunction().getNode())) {
                    r.addMember(new RelationMember(Constants.ROLE_VIA, w));
                }
            }
            r.addMember(new RelationMember(Constants.ROLE_TO, to.getWay()));

            cmd.add(r);
        } else {
            r = existing;
        }

        //existing = r;//feeble attempt//have tried some things with this but agian they dont work
        //adds lane and its index to Relation worthless for our plugin, that is unless ian would like to keep this functinality
        //final String key = isExtra() ? Constants.TURN_KEY_EXTRA_LANES : Constants.TURN_KEY_LANES;//will not build without
        //final List<Integer> lanes = Turn.indices(r, key);//Will not work without this line // sets lane tag
        //lanes.add(getIndex()); //1st test no longer added lane tag
        //cmd.backup(r).put(key, Turn.join(lanes)); //2nd test no longer adds lane tag

        UndoRedoHandler.getInstance().add(cmd);
    }

    public Set<Turn> getTurns() {
        return turns;
    }

    public void remove() {
        if (!isExtra()) {
            throw new UnsupportedOperationException();
        }

        final GenericCommand cmd = new GenericCommand(getOutgoingJunction().getNode().getDataSet(), tr("Delete lane."));

        for (Turn t : getTurns()) {
            t.remove(cmd);
        }

        getOutgoingRoadEnd().removeLane(cmd, this);

        UndoRedoHandler.getInstance().add(cmd);
    }

    void initialize() {
        final Set<Turn> turns = Turn.load(getContainer(), Constants.ROLE_FROM, getOutgoingRoadEnd().getWay());

        final Iterator<Turn> it = turns.iterator();
        while (it.hasNext()) {
            final Turn t = it.next();

            if (!t.getFrom().equals(this)) {
                it.remove();
            }
        }

        this.turns = Collections.unmodifiableSet(turns);
    }
}
