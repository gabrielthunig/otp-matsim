package routeMatrix;

import org.opentripplanner.analyst.batch.Individual;
import org.opentripplanner.analyst.batch.SyntheticRasterPopulation;
import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.routing.algorithm.AStar;
import org.opentripplanner.routing.core.OptimizeType;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.standalone.OTPMain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Set;
import java.util.TimeZone;

/**
 * Created by gthunig
 */
public class OTPMatrixRouter {

    private static final Logger log = LoggerFactory.getLogger(OTPMatrixRouter.class);

    //editable constants
    private final static String INPUT_ROOT = "../../SVN/shared-svn/projects/accessibility_berlin/otp_2016-02-01/";
    private final static String GRAPH_NAME = "Graph.obj";
    private final static String OUTPUT_DIR = "../../SVN/shared-svn/projects/accessibility_berlin/otp_2016-02-01/output/";

    private final static String TIME_ZONE_STRING = "Europe/Berlin";
    private final static String DATE_STRING = "2016-02-01";
    private final static int DEPARTURE_TIME = 8 * 60 * 60;

    private final static double LEFT = 13.1949;
    private final static double RIGHT = 13.5657;
    private final static double BOTTOM = 52.3926; // 52.3926 13.1949
    private final static double TOP = 52.6341; // 52.6341 13.5657
    private final static int RASTER_COLUMN_COUNT = 2;
    private final static int RASTER_ROW_COUNT = 2;

    // only relevant for single-path router, not for matrix
    private final static double FROM_LAT = 52.521918;
    private final static double FROM_LON = 13.413215;
    private final static double TO_LAT = 52.538186;
    private final static double TO_LON = 13.4356;
    //editable constants end

    public static void main(String[] args) {

        routeMatrix();
//        routeSinglePath();

    }

    public static boolean buildGraph() {
        if (!new File(INPUT_ROOT + GRAPH_NAME).exists()) {
            log.info("No graphfile found. Building the graph from content from: " + new File(INPUT_ROOT).getAbsolutePath() + " ...");
            OTPMain.main(new String[]{"--build", INPUT_ROOT});
            log.info("Building the graph finished.");
            return true;
        } else {
            return false;
        }
    }

    public static Graph loadGraph() {

        log.info("Loading the graph...");
        try {
            return Graph.load(new File(INPUT_ROOT + GRAPH_NAME), Graph.LoadLevel.FULL);
        } catch (IOException | ClassNotFoundException e) {
            log.info("Error while loading the Graph.");
            e.printStackTrace();
            return null;
        }
    }

    private static Calendar prepareCalendarSettings() {
        log.info("Preparing settings for routing...");
        final Calendar calendar = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        TimeZone timeZone = TimeZone.getTimeZone(TIME_ZONE_STRING);
        df.setTimeZone(timeZone);
        calendar.setTimeZone(timeZone);
        try {
            calendar.setTime(df.parse(DATE_STRING));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        calendar.add(Calendar.SECOND, DEPARTURE_TIME);
        log.info("Preparing settings for routing finished.");
        return calendar;
    }

    private static InputsCSVWriter prepareWriter() {
        InputsCSVWriter writer = new InputsCSVWriter(OUTPUT_DIR + "accessibility_Berlin.csv", " ");
        writer.writeField("fromCoord.lat");
        writer.writeField("fromCoord.lon");
        writer.writeField("toCoord.lat");
        writer.writeField("toCoord.lon");
        writer.writeField("travelTime");
        writer.writeField("TravelDistance");
        writer.writeNewLine();
        return writer;
    }

    public static void routeMatrix() {

        buildGraph();
        Graph graph = loadGraph();
        log.info("Loading the graph finished.");
        assert graph != null;

        Calendar calendar = prepareCalendarSettings();

        SyntheticRasterPopulation rasterPop = new SyntheticRasterPopulation();
        rasterPop.left = LEFT;
        rasterPop.right = RIGHT;
        rasterPop.top = TOP;
        rasterPop.bottom = BOTTOM;
        rasterPop.cols = RASTER_COLUMN_COUNT;
        rasterPop.rows = RASTER_ROW_COUNT;
        rasterPop.setup();

        InputsCSVWriter writer = prepareWriter();

        log.info("Start routing...");
        for (Individual fromIndividual : rasterPop) {

            TraverseModeSet modeSet = new TraverseModeSet();
            modeSet.setWalk(true);
            modeSet.setTransit(true);
//		    modeSet.setBicycle(true);
            RoutingRequest request = new RoutingRequest(modeSet);
            request.setWalkBoardCost(3 * 60); // override low 2-4 minute values
            request.setBikeBoardCost(3 * 60 * 2);
            request.setOptimize(OptimizeType.QUICK);
            request.setMaxWalkDistance(Double.MAX_VALUE);
            request.batch = true;
            request.setDateTime(calendar.getTime());
            request.from = new GenericLocation(fromIndividual.lat, fromIndividual.lon);
            try {
                request.setRoutingContext(graph);
            } catch (Exception e) {
                log.info(e.getMessage());
                System.out.println("fromIndividual.lat = " + fromIndividual.lat);
                System.out.println("fromIndividual.lon = " + fromIndividual.lon);
                continue;
            }
            ShortestPathTree spt = (new AStar()).getShortestPathTree(request);
            if (spt != null) {
                for (Individual toIndividual : rasterPop) {
//                    long t0 = System.currentTimeMillis();

                    if (fromIndividual.lat == toIndividual.lat && fromIndividual.lon == toIndividual.lon) continue;
                    writer.writeField(fromIndividual.lat);
                    writer.writeField(fromIndividual.lon);
                    route(toIndividual.lat, toIndividual.lon, spt, calendar, writer);

//                    long t1 = System.currentTimeMillis();
//                    System.out.printf("Time: %d\n", t1-t0);
                }

            }
        }
        writer.close();
        log.info("Routing finished");
        log.info("Shutdown");
    }

    public static void routeSinglePath() {
        buildGraph();
        Graph graph = loadGraph();
        log.info("Loading the graph finished.");
        assert graph != null;

        Calendar calendar = prepareCalendarSettings();

        InputsCSVWriter writer = prepareWriter();

        TraverseModeSet modeSet = new TraverseModeSet();
        modeSet.setWalk(true);
        modeSet.setTransit(true);
        RoutingRequest request = new RoutingRequest(modeSet);
        request.setWalkBoardCost(3 * 60); // override low 2-4 minute values
        request.setBikeBoardCost(3 * 60 * 2);
        request.setOptimize(OptimizeType.QUICK);
        request.setMaxWalkDistance(Double.MAX_VALUE);
        request.batch = true;
        request.setDateTime(calendar.getTime());
        request.from = new GenericLocation(FROM_LAT, FROM_LON);
        request.setRoutingContext(graph);
        ShortestPathTree spt = (new AStar()).getShortestPathTree(request);
        if (spt != null) {
                long t0 = System.currentTimeMillis();

                writer.writeField(FROM_LAT);
                writer.writeField(FROM_LON);
                route(TO_LAT, TO_LON, spt, calendar, writer);

                long t1 = System.currentTimeMillis();
                System.out.printf("Time: %d\n", t1-t0);

        }
        writer.close();

    }

    private static Vertex getNearestVertex(double lat, double lon, Set<Vertex> vertices) {
        double closestDistance = Double.MAX_VALUE;
        Vertex closestVertex = null;
        for (Vertex currentVertex : vertices) {
            double currentDistance = distFrom(lat, lon, currentVertex.getLat(), currentVertex.getLon());
            if (currentDistance < closestDistance) {
                closestDistance = currentDistance;
                closestVertex = currentVertex;
            }
        }
        return closestVertex;
    }

    public static double distFrom(double lat1, double lng1, double lat2, double lng2) {
        double earthRadius = 6371000; //meters
        double dLat = Math.toRadians(lat2-lat1);
        double dLng = Math.toRadians(lng2-lng1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLng/2) * Math.sin(dLng/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));

        return (earthRadius * c);
    }

    private static void route(double destinationLat, double destinationLon, ShortestPathTree spt, Calendar calendar, InputsCSVWriter writer) {

//        Coordinate c = new Coordinate(destinationLat, destinationLon);
//        Envelope env = new Envelope(c);
//        double xscale = Math.cos(c.y * 3.141592653589793D / 180.0D);
//        double searchRadiusLat = SphericalDistanceLibrary.metersToDegrees(500.0D);
//        env.expandBy(searchRadiusLat / xscale, searchRadiusLat);
//        Collection vertices = spt.getOptions().getRoutingContext().graph.streetIndex.getVerticesForEnvelope(env);
//
//
//        Iterator iterator = vertices.iterator();
//        GraphPath path = null;
//        System.out.println(vertices.iterator().hasNext());
//        Vertex destination = (Vertex) vertices.iterator().next();
//        path = spt.getPath(destination, false);
//        //while (path == null && iterator.hasNext()) {
//
//        System.out.println("path = " + path);
//        //}

        GraphPath path = spt.getPath(getNearestVertex(destinationLat, destinationLon, spt.getVertices()), false);
        if (path == null) return;

//        long initialWaitTime;
        long elapsedTime = 0;
        double distance = 0;
//        long transitTime = 0;
//        long walkTime = 0;
//        boolean transited = false;

        path.dump();
//        if (!path.states.isEmpty()) {
//            initialWaitTime = ((path.states.getFirst().getTimeInMillis() - calendar.getTime().getTime()) / 1000);
////                        System.out.println("initial wait time: " + initialWaitTime);
//        }

        for (State state : path.states) {
//            if (state.isOnboard()) transited = true;
//                        System.out.println("");
//                        System.out.println("State infos start:");
//                        System.out.println("state elapsedTime: " + elapsedTime);
//                        System.out.println("state walkdistance: " + state.getWalkDistance());
//                        System.out.println("isOnBoard: " + state.isOnboard());

            Edge backEdge = state.getBackEdge();
            if (backEdge != null && backEdge.getFromVertex() != null) {
//                            System.out.println("backEdge = " + backEdge);
//                            System.out.println("Mode: " + backEdge.getName());
//                            System.out.println("backEdge.getFromVertex() = " + backEdge.getFromVertex());
//                            System.out.println("Label" + backEdge.getFromVertex().getLabel());
//                            System.out.println("Lat: " + backEdge.getFromVertex().getLat() + " Lon: " + backEdge.getFromVertex().getLon());
//                            System.out.println("x: " + backEdge.getFromVertex().getX() + " y: " + backEdge.getFromVertex().getY());

//                if (state.isOnboard()) transitTime += state.getActiveTime() - elapsedTime;
//                else walkTime += state.getActiveTime() - elapsedTime;

                distance += backEdge.getDistance();

                elapsedTime = state.getActiveTime();
            }
        }

        //write output
        writer.writeField(destinationLat);
        writer.writeField(destinationLon);
        writer.writeField(elapsedTime);
        writer.writeField(distance);
        writer.writeNewLine();
    }

}
