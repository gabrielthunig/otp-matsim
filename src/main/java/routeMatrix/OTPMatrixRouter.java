package routeMatrix;

import com.vividsolutions.jts.geom.Coordinate;
import org.opentripplanner.analyst.batch.Individual;
import org.opentripplanner.analyst.core.Sample;
import org.opentripplanner.analyst.request.SampleFactory;
import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.routing.algorithm.AStar;
import org.opentripplanner.routing.core.OptimizeType;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.standalone.OTPMain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

/**
 * Created by gthunig
 */
public class OTPMatrixRouter {

    private static final Logger log = LoggerFactory.getLogger(OTPMatrixRouter.class);

    //editable constants
//    private final static String INPUT_ROOT = "../../Workspace/shared-svn/projects/accessibility_berlin/otp/2016-03-14/";
    private final static String INPUT_ROOT = "input/";
    private final static String GRAPH_NAME = "Graph.obj";
//    private final static String OUTPUT_DIR = "../../Workspace/shared-svn/projects/accessibility_berlin/otp/2016-03-14/output/";
    private final static String OUTPUT_DIR = "output/";

    private final static String TIME_ZONE_STRING = "Europe/Berlin";
    private final static String DATE_STRING = "2016-04-01";
    private final static int DEPARTURE_TIME = 8 * 60 * 60;

    public static void main(String[] args) {

        CSVReader reader = new CSVReader(INPUT_ROOT + "stops.txt", ",");

        List<Individual> individuals = new ArrayList<>();
        reader.readLine();
        String[] line = reader.readLine();
        while (line != null) {
            if (line.length == 9) {
                individuals.add(new Individual("", Double.parseDouble(line[5]), Double.parseDouble(line[4]), 0));
            } else if (line.length == 10) {
                individuals.add(new Individual("", Double.parseDouble(line[6]), Double.parseDouble(line[5]), 0));
            } else {
                break;
            }
            line = reader.readLine() ;
        }
        log.info("Found " + individuals.size() + " coordinates.");

        if (new File(OUTPUT_DIR).mkdir()) {
            log.info("Did not found outputRoot at " + OUTPUT_DIR + " Created it as a new directory.");
        }

        routeMatrix(individuals);
    }

    private static void routeMatrix(List<Individual> individuals) {
        buildGraph(INPUT_ROOT);
        Graph graph = loadGraph(INPUT_ROOT);
        assert graph != null;

        Calendar calendar = prepareDefaultCalendarSettings();

        routeMatrix(graph, calendar, indexIndividuals(graph, individuals), OUTPUT_DIR);
        log.info("Shutdown");
    }

    private static boolean buildGraph(String inputRoot) {
        if (!new File(inputRoot + GRAPH_NAME).exists()) {
            log.info("No graphfile found. Building the graph from content from: " + new File(inputRoot).getAbsolutePath() + " ...");
            OTPMain.main(new String[]{"--build", inputRoot});
            log.info("Building the graph finished.");
            return true;
        } else {
            return false;
        }
    }

    static Graph loadGraph(String inputRoot) {

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

    private static List<Individual> indexIndividuals(Graph graph, List<Individual> individuals) {
        log.info("Start indexing vertices and writing them out...");
        InputsCSVWriter individualsWriter = new InputsCSVWriter(OUTPUT_DIR + "ids.csv", ",");
        SampleFactory sampleFactory = graph.getSampleFactory();
        int idCounter = 0;
        for (Individual individual : individuals) {
            individual.label = Integer.toString(idCounter++);
            individual.sample = sampleFactory.getSample(individual.lon, individual.lat);
            individualsWriter.writeField(individual.label);
            individualsWriter.writeField(individual.lat);
            individualsWriter.writeField(individual.lon);
            individualsWriter.writeNewLine();
        }
        individualsWriter.close();
        log.info("Indexing vertices and writing them out: done.");
        return individuals;
    }

    private static RoutingRequest getRoutingRequest(Graph graph, Calendar calendar, Individual individual) {
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
        request.from = new GenericLocation(individual.lat, individual.lon);
        try {
            request.setRoutingContext(graph);
        } catch (Exception e) {
            log.error(e.getMessage());
            System.out.println("Latitude  = " + individual.lat);
            System.out.println("Longitude = " + individual.lon);
            return null;
        }
        return request;
    }

    private static void routeMatrix(Graph graph, Calendar calendar, List<Individual> individuals, String outputDir) {
        log.info("Start routing...");
        InputsCSVWriter timeWriter = new InputsCSVWriter(outputDir + "tt.csv", " ");
//        InputsCSVWriter distanceWriter = new InputsCSVWriter(outputDir + "td.csv", " ");

        for (Individual fromIndividual : individuals) {
            long t0 = System.currentTimeMillis();

            RoutingRequest request = getRoutingRequest(graph, calendar, fromIndividual);
            if (request == null) continue;
            ShortestPathTree spt = (new AStar()).getShortestPathTree(request);
            if (spt != null) {
                for (Individual toIndividual1 : individuals) {
                    if (fromIndividual.equals(toIndividual1)) continue;
                    timeWriter.writeField(fromIndividual.label);
                    timeWriter.writeField(toIndividual1.label);
                    route(toIndividual1, spt, timeWriter);
                    timeWriter.writeNewLine();
                }

            }
            long t1 = System.currentTimeMillis();
            System.out.printf("Time: %d\n", t1-t0);
        }
        timeWriter.close();
        log.info("Routing finished");
    }

    private static void route(Individual toIndividual, ShortestPathTree spt, InputsCSVWriter timeWriter) {

//        GraphPath path = spt.getPath(toIndividual.sample.v0, false);
//        long elapsedTime = 0;
//
////        path.dump();
//
//        for (State state : path.states) {
//            Edge backEdge = state.getBackEdge();
//            if (backEdge != null && backEdge.getFromVertex() != null) {
//                elapsedTime = state.getActiveTime();
//            }
//        }

        long elapsedTime = toIndividual.sample.eval(spt);

        //write output
        timeWriter.writeField(elapsedTime);
    }

    static long getSingleRouteTime(Graph graph, Calendar calendar, Coordinate origin, Coordinate destination) {
        if (origin.equals(destination)) return 0;
        assert graph != null;

        Individual startIndividual = new Individual("origin", origin.y, origin.x, 0);
        RoutingRequest routingRequest = getRoutingRequest(graph, calendar, startIndividual);

        ShortestPathTree spt = (new AStar()).getShortestPathTree(routingRequest);
        if (spt != null) {
            SampleFactory sampleFactory = graph.getSampleFactory();
            //return sampleFactory.getSample(destination.y, destination.x).eval(spt);
            Sample sample = sampleFactory.getSample(destination.y, destination.x);
            GraphPath path = eval(spt, sample);
            long elapsedTime = 0;

            for (State state : path.states) {
                Edge backEdge = state.getBackEdge();
                if (backEdge != null && backEdge.getFromVertex() != null) {
                    elapsedTime = state.getActiveTime();
                }
            }
            return elapsedTime;
        }
        return -1;
    }

    private static GraphPath eval(ShortestPathTree spt, Sample sample) {
        State s0 = spt.getState(sample.v0);
        State s1 = spt.getState(sample.v1);
        long m0 = Long.MAX_VALUE;
        long m1 = Long.MAX_VALUE;
        double walkSpeed = spt.getOptions().walkSpeed;
        if (s0 != null)
            m0 = (int)(s0.getActiveTime() + sample.d0 / walkSpeed);
        if (s1 != null)
            m1 = (int)(s1.getActiveTime() + sample.d1 / walkSpeed);
        if (m0 < m1) {
            return new GraphPath(s0, false);
        } else {
            return new GraphPath(s1, false);
        }
    }
}
