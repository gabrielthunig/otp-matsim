package routeMatrix;

import com.vividsolutions.jts.geom.Coordinate;
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
import java.util.*;

/**
 * Created by gthunig
 */
public class OTPMatrixRouter {

    private static final Logger log = LoggerFactory.getLogger(OTPMatrixRouter.class);

    //editable constants
//    private final static String INPUT_ROOT = "../../SVN/shared-svn/projects/accessibility_berlin/otp_2016-02-01/";
    private final static String INPUT_ROOT = "input/";
    private final static String GRAPH_NAME = "Graph.obj";
//    private final static String OUTPUT_DIR = "../../SVN/shared-svn/projects/accessibility_berlin/otp_2016-02-01/output/";
    private final static String OUTPUT_DIR = "output/";

    private final static String TIME_ZONE_STRING = "Europe/Berlin";
    private final static String DATE_STRING = "2016-02-01";
    private final static int DEPARTURE_TIME = 8 * 60 * 60;

    private final static double LEFT = 13.124627;
    private final static double RIGHT = 13.718464; // ca. 41000m from right to left
    private final static double BOTTOM = 52.361485;
    private final static double TOP = 52.648131; // ca. 31000m from top to bottom
    private final static int RASTER_COLUMN_COUNT = 164; // makes width of one column approx. 250m
    private final static int RASTER_ROW_COUNT = 124; // makes height of one row approx. 250m

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

        log.info("Start indexing vertices and writing them out...");
        InputsCSVWriter verticesWriter = new InputsCSVWriter(OUTPUT_DIR + "ids.csv", ",");
        Map<String, Vertex> vertices = new HashMap<>();
        int idCounter = 0;
        for (Individual individual : rasterPop) {
            String id = Integer.toString(idCounter);
            Vertex vertex = getNearestVertex(individual.lat, individual.lon, new HashSet<>(graph.getVertices()));
            vertices.put(id, vertex);
            verticesWriter.writeField(id);
            verticesWriter.writeField(vertex.getLat());
            verticesWriter.writeField(vertex.getLon());
            verticesWriter.writeNewLine();
            idCounter++;
            if (idCounter % 1000 == 0) {
                log.info("Current status: Vertex nr. " + (idCounter+1) + " / " + rasterPop.size());
            }
        }
        verticesWriter.close();
        log.info("Indexing vertices and writing them out: done.");

        log.info("Start routing...");
        InputsCSVWriter timeWriter = new InputsCSVWriter(OUTPUT_DIR + "tt.csv", " ");
        InputsCSVWriter distanceWriter = new InputsCSVWriter(OUTPUT_DIR + "td.csv", " ");

        for (int i = 0; i < RASTER_COLUMN_COUNT*RASTER_ROW_COUNT; i++) {
            long t0 = System.currentTimeMillis();

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
            request.from = new GenericLocation(vertices.get(String.valueOf(i)).getLat(), vertices.get(String.valueOf(i)).getLon());
            try {
                request.setRoutingContext(graph);
            } catch (Exception e) {
                log.info(e.getMessage());
                System.out.println("fromVertex.getLat() = " + vertices.get(String.valueOf(i)).getLat());
                System.out.println("fromVertex.getLon() = " + vertices.get(String.valueOf(i)).getLon());
                continue;
            }
            ShortestPathTree spt = (new AStar()).getShortestPathTree(request);
            if (spt != null) {
                for (int e = 0; e < RASTER_COLUMN_COUNT*RASTER_ROW_COUNT; e++) {

                    if (vertices.get(String.valueOf(i)).equals(vertices.get(String.valueOf(e)))) continue;
                    timeWriter.writeField(i);
                    timeWriter.writeField(e);
                    distanceWriter.writeField(i);
                    distanceWriter.writeField(e);
                    route(vertices.get(String.valueOf(e)), spt, timeWriter, distanceWriter);
                    timeWriter.writeNewLine();
                    distanceWriter.writeNewLine();
                }

            }
            long t1 = System.currentTimeMillis();
            System.out.printf("Time: %d\n", t1-t0);
        }
        timeWriter.close();
        distanceWriter.close();
        log.info("Routing finished");
        log.info("Shutdown");
    }

    public static void routeSinglePath() {
        buildGraph();
        Graph graph = loadGraph();
        log.info("Loading the graph finished.");
        assert graph != null;

        Calendar calendar = prepareCalendarSettings();

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

                System.out.println("TO_LAT = " + TO_LAT);
                System.out.println("TO_LON = " + TO_LON);
//                route(getNearestVertex(TO_LAT, TO_LON, spt.getVertices()), spt);

                long t1 = System.currentTimeMillis();
                System.out.printf("Time: %d\n", t1-t0);

        }

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

    private static void route(Vertex destination, ShortestPathTree spt, InputsCSVWriter timeWriter, InputsCSVWriter distanceWriter) {

        GraphPath path = spt.getPath(destination, false);
        if (path == null) {
            Vertex alternativVertex = getNearestVertex(destination.getLat(), destination.getLon(), spt.getVertices());
            path = spt.getPath(alternativVertex, false);
            if (path == null) return;
        }
        long elapsedTime = 0;
        double distance = 0;

        path.dump();

        for (State state : path.states) {
            Edge backEdge = state.getBackEdge();
            if (backEdge != null && backEdge.getFromVertex() != null) {
                distance += backEdge.getDistance();
                elapsedTime = state.getActiveTime();
            }
        }

        //write output
        timeWriter.writeField(elapsedTime);
        distanceWriter.writeField(distance);
    }

}
