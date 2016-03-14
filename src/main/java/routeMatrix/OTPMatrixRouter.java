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


    public static void main(String[] args) {

        CSVReader reader = new CSVReader(INPUT_ROOT + "stops.txt", ",");

        Map<String, Coordinate> coordinates = new HashMap<>();
        reader.readLine();
        String[] line = reader.readLine();
        while (line != null) {
            if (line.length == 9) {
                coordinates.put(line[0] ,new Coordinate(Double.parseDouble(line[4]), Double.parseDouble(line[5])));
            } else if (line.length == 10) {
                coordinates.put(line[0] ,new Coordinate(Double.parseDouble(line[5]), Double.parseDouble(line[6])));
            } else {
                break;
            }
            line = reader.readLine() ;
        }
        log.info("Found " + coordinates.size() + " coordinates.");

        if (new File(OUTPUT_DIR).mkdir()) {
            log.info("Did not found outputRoot at " + OUTPUT_DIR + " Created it as a new directory.");
        }

        routeMatrix(coordinates);
    }

    public static void routeMatrix() {

        buildGraph(INPUT_ROOT);
        Graph graph = loadGraph(INPUT_ROOT);
        assert graph != null;

        Calendar calendar = prepareDefaultCalendarSettings();

        SyntheticRasterPopulation rasterPop = new SyntheticRasterPopulation();
        rasterPop.left = LEFT;
        rasterPop.right = RIGHT;
        rasterPop.top = TOP;
        rasterPop.bottom = BOTTOM;
        rasterPop.cols = RASTER_COLUMN_COUNT;
        rasterPop.rows = RASTER_ROW_COUNT;
        rasterPop.setup();

        Map<String, Vertex> vertices = indexVertices(graph, rasterPop);

        routeMatrix(graph, calendar, vertices, OUTPUT_DIR);
        log.info("Shutdown");
    }

    public static void routeMatrix(List<Coordinate> coordinates) {
        buildGraph(INPUT_ROOT);
        Graph graph = loadGraph(INPUT_ROOT);
        assert graph != null;

        Calendar calendar = prepareDefaultCalendarSettings();

        Map<String, Vertex> vertices = indexVertices(graph, coordinates);

        routeMatrix(graph, calendar, vertices, OUTPUT_DIR);
        log.info("Shutdown");
    }

    public static void routeMatrix(Map<String, Coordinate> coordinates) {
        buildGraph(INPUT_ROOT);
        Graph graph = loadGraph(INPUT_ROOT);
        assert graph != null;

        Calendar calendar = prepareDefaultCalendarSettings();

        Map<String, Vertex> vertices = indexVertices(graph, coordinates);

        routeMatrix(graph, calendar, vertices, OUTPUT_DIR);
        log.info("Shutdown");
    }

    public static boolean buildGraph(String inputRoot) {
        if (!new File(inputRoot + GRAPH_NAME).exists()) {
            log.info("No graphfile found. Building the graph from content from: " + new File(inputRoot).getAbsolutePath() + " ...");
            OTPMain.main(new String[]{"--build", inputRoot});
            log.info("Building the graph finished.");
            return true;
        } else {
            return false;
        }
    }

    public static Graph loadGraph(String inputRoot) {

        log.info("Loading the graph...");
        try {
            Graph graph = Graph.load(new File(inputRoot + GRAPH_NAME), Graph.LoadLevel.FULL);
            log.info("Loading the graph finished.");
            return graph;
        } catch (IOException | ClassNotFoundException e) {
            log.info("Error while loading the Graph.");
            e.printStackTrace();
            return null;
        }
    }

    private static Calendar prepareDefaultCalendarSettings() {
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

    public static Map<String, Vertex> indexVertices(Graph graph, List<Coordinate> coordinates) {
        log.info("Start indexing vertices and writing them out...");
        InputsCSVWriter verticesWriter = new InputsCSVWriter(OUTPUT_DIR + "ids.csv", ",");
        Map<String, Vertex> vertices = new HashMap<>();
        int idCounter = 0;
        for (Coordinate coordinate : coordinates) {
            String id = Integer.toString(idCounter);
            Vertex vertex = getNearestVertex(coordinate.x, coordinate.y, new HashSet<>(graph.getVertices()));
            vertices.put(id, vertex);
            verticesWriter.writeField(id);
            verticesWriter.writeField(vertex.getLat());
            verticesWriter.writeField(vertex.getLon());
            verticesWriter.writeNewLine();
            idCounter++;
//            if (idCounter % 1000 == 0) {
//                log.info("Current status: Vertex nr. " + (idCounter+1) + " / " + coordinates.size());
//            }
        }
        verticesWriter.close();
        log.info("Indexing vertices and writing them out: done.");
        return vertices;
    }

    public static Map<String, Vertex> indexVertices(Graph graph, Map<String, Coordinate> coordinates) {
        log.info("Start indexing vertices and writing them out...");
        InputsCSVWriter verticesWriter = new InputsCSVWriter(OUTPUT_DIR + "ids.csv", ",");
        Map<String, Vertex> vertices = new HashMap<>();
        for (String id : coordinates.keySet()) {
            Coordinate coordinate = coordinates.get(id);
            Vertex vertex = getNearestVertex(coordinate.x, coordinate.y, new HashSet<>(graph.getVertices()));
            vertices.put(id, vertex);
            verticesWriter.writeField(id);
            verticesWriter.writeField(vertex.getLat());
            verticesWriter.writeField(vertex.getLon());
            verticesWriter.writeNewLine();
        }
        verticesWriter.close();
        log.info("Indexing vertices and writing them out: done.");
        return vertices;
    }

    public static Map<String, Vertex> indexVertices(Graph graph, SyntheticRasterPopulation rasterPop) {
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
//            if (idCounter % 1000 == 0) {
//                log.info("Current status: Vertex nr. " + (idCounter+1) + " / " + rasterPop.size());
//            }
        }
        verticesWriter.close();
        log.info("Indexing vertices and writing them out: done.");
        return vertices;
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

    private static double distFrom(double lat1, double lng1, double lat2, double lng2) {
        double earthRadius = 6371000; //meters
        double dLat = Math.toRadians(lat2-lat1);
        double dLng = Math.toRadians(lng2-lng1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLng/2) * Math.sin(dLng/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));

        return (earthRadius * c);
    }

    private static RoutingRequest getRoutingRequest(Graph graph, Calendar calendar, Vertex vertex) {
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
        request.from = new GenericLocation(vertex.getLat(), vertex.getLon());
        try {
            request.setRoutingContext(graph);
        } catch (Exception e) {
            log.error(e.getMessage());
            System.out.println("Latitude  = " + vertex.getLat());
            System.out.println("Longitude = " + vertex.getLon());
            return null;
        }
        return request;
    }

    public static void routeMatrix(Graph graph, Calendar calendar, Map<String, Vertex> vertices, String outputDir) {
        log.info("Start routing...");
        InputsCSVWriter timeWriter = new InputsCSVWriter(outputDir + "tt.csv", " ");
        InputsCSVWriter distanceWriter = new InputsCSVWriter(outputDir + "td.csv", " ");

        for (String originId : vertices.keySet()) {
            long t0 = System.currentTimeMillis();

            Vertex origin = vertices.get(originId);
            RoutingRequest request = getRoutingRequest(graph, calendar, origin);
            if (request == null) continue;
            ShortestPathTree spt = (new AStar()).getShortestPathTree(request);
            if (spt != null) {
                for (String destinationId : vertices.keySet()) {
                    Vertex destination = vertices.get(destinationId);
                    if (origin.equals(destination)) continue;
                    timeWriter.writeField(originId);
                    timeWriter.writeField(destinationId);
                    distanceWriter.writeField(originId);
                    distanceWriter.writeField(destinationId);
                    route(destination, spt, timeWriter, distanceWriter);
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
    }

    public static double[][] routeMatrix(Graph graph, Calendar calendar, Map<String, Vertex> vertices) {
        log.info("Start routing...");

        double[][] result = new double[vertices.size()][vertices.size()];

        for (int i = 0; i < vertices.size(); i++) {
            long t0 = System.currentTimeMillis();

            Vertex origin = vertices.get(i);
            RoutingRequest request = getRoutingRequest(graph, calendar, origin);
            if (request == null) {
                for (int e = 0; e < result[i].length; e++) {
                    result[i][e] = -1;
                }
                continue;
            }
            System.out.println("request = " + request);
            ShortestPathTree spt = (new AStar()).getShortestPathTree(request);
            if (spt != null) {
                for (int e = 0; e < vertices.size(); e++) {
                    Vertex destination = vertices.get(e);
                    if (origin.equals(destination)) continue;
                    result[i][e] = route(destination, spt);
                }

            }
            long t1 = System.currentTimeMillis();
            System.out.printf("Time: %d\n", t1-t0);
        }
        log.info("Routing finished");
        return result;
    }

    private static double route(Vertex destination, ShortestPathTree spt) {
        GraphPath path = spt.getPath(destination, false);
        if (path == null) {
            Vertex alternativVertex = getNearestVertex(destination.getLat(), destination.getLon(), spt.getVertices());
            path = spt.getPath(alternativVertex, false);
            if (path == null) return -1;
        }
        long elapsedTime = 0;
        //boolean transited = false;

        path.dump();

        for (State state : path.states) {
            Edge backEdge = state.getBackEdge();
            if (backEdge != null && backEdge.getFromVertex() != null) {
                elapsedTime = state.getActiveTime();
                //if (state.isOnboard()) transited = true;
            }
        }
        //System.out.println("transited = " + transited);
        return elapsedTime;
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
