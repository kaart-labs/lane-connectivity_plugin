// License: GPL. For details, see LICENSE file.
package com.kaart.laneconnectivity.model;

import java.util.regex.Pattern;

public final class Constants {
    private Constants() {}

    /** The standard OSM tag separator */
    static final String SEPARATOR = ";";
    /** An escaped | (used for lane separation) for use in regex split statements */
    static final String LANE_SEPARATOR = "\\|";
    /** The connectivity separator for from lane to to lane(s) */
    static final String CONNECTIVITY_TO_FROM_SEPARATOR = ":";
    /** The connectivity "optional" values */
    static final String CONNECTIVITY_OPTIONAL_LANES_REGEX = "\\([0-9]+\\)";
    /** A compiled pattern for optional connectivities */
    static final Pattern CONNECTIVITY_OPTIONAL_LANES_PATTERN = Pattern.compile(CONNECTIVITY_OPTIONAL_LANES_REGEX);

    static final String SPLIT_REGEX = "\\p{Zs}*[,:;]\\p{Zs}*";
    static final Pattern SPLIT_PATTERN = Pattern.compile(SPLIT_REGEX);

    static final String TYPE_LENGTHS = "turnlanes:lengths";

    static final String LENGTHS_KEY_LENGTHS_LEFT = "lengths:left";
    static final String LENGTHS_KEY_LENGTHS_RIGHT = "lengths:right";

    /** The relation type for turnlanes relations */
    static final String TYPE_TURNS = "turnlanes:turns";
    /** The relation type for connectivity relations */
    static final String TYPE_CONNECTIVITY = "connectivity";

    /** The standard "via" role */
    static final String ROLE_VIA = "via";
    /** The standard "from" role */
    static final String ROLE_FROM = "from";
    /** The standard "to" role */
    static final String ROLE_TO = "to";

    static final String TURN_KEY_LANES = "lanes";
    static final String TURN_KEY_EXTRA_LANES = "lanes:extra";
    static final String LENGTHS_ROLE_END = "end";
    static final String LENGTHS_ROLE_WAYS = "ways";

    /** The maximum selection of objects that the UI will look at */
    public static final int MAX_SELECTION = 100;
    public static final String COMMA_SEPARATOR = ",";

}
