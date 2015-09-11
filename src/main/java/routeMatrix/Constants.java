package routeMatrix;

import java.util.Calendar;
import java.util.TimeZone;

public class Constants {
	/**
	 * BASEDIR should include Openstreetmap data in a file named *.osm or *.pbf
	 * and a zip file with GTFS data
	 */
//    public static final String BASEDIR = "input/accessibility_berlin/";
//	public static final String BASEDIR = "input/accessibility_berlin_2/";
    /** Created by ExtractNetwork*/
    public static final String TRANSIT_SCHEDULE_FILE = "extracted-transitschedule.xml";
    /** Created by ExtractNetwork*/
    public static final String TRANSIT_VEHICLE_FILE = "extracted-transitvehicles.xml";
    /** Created by ExtractNetwork*/
    public static final String NETWORK_FILE = "extracted-network.xml";
    
    public static final String OTP_GRAPH_FILE = "Graph.obj";

    public static final String INPUT_COORDINATE_SYSTEM = "EPSG:31468";
    
    public static final String TIME_ZONE = "Europe/Berlin";
//    public static final String DATE = "2015-05-26";
    public static final String DATE = "2015-07-01";
    public static final double SCHEDULE_END_TIME_ON_FOLLOWING_DATE = 24*60*60;
    public static final double MATRIX_START_TIME = 8*60*60;
    public static final String INPUT_FILE = "accessibilities.csv";
}
