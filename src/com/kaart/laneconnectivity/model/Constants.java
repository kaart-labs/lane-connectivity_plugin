// License: GPL. For details, see LICENSE file.
package com.kaart.laneconnectivity.model;

import java.util.regex.Pattern;

public final class Constants {
	private Constants() {
		// Do nothing
	}
    static final String SEPARATOR = ";";
    static final String SPLIT_REGEX = "\\p{Zs}*[,:;]\\p{Zs}*";
    static final Pattern SPLIT_PATTERN = Pattern.compile(SPLIT_REGEX);

    static final String TYPE_LENGTHS = "turnlanes:lengths";

    static final String LENGTHS_KEY_LENGTHS_LEFT = "lengths:left";
    static final String LENGTHS_KEY_LENGTHS_RIGHT = "lengths:right";

    static final String TYPE_TURNS = "connectivity";
    static final String TYPE_CONNECTION = "connectivity";
    static final String TYPE_CONNECTIVITY = "1:1|2:2|3:3|4:4";

    static final String TURN_ROLE_VIA = "via";
    static final String TURN_ROLE_FROM = "from";
    static final String TURN_ROLE_TO = "to";

    static final String TURN_KEY_LANES = "lanes";
    static final String TURN_KEY_EXTRA_LANES = "lanes:extra";
    static final String LENGTHS_ROLE_END = "end";
    static final String LENGTHS_ROLE_WAYS = "ways";

}
