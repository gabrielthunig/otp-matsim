package routeMatrix;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opentripplanner.analyst.batch.Individual;
import org.opentripplanner.analyst.batch.SyntheticRasterPopulation;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.routing.algorithm.AStar;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.standalone.OTPMain;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.TimeZone;

public class OTPTimeRouterTest {

    private static Graph graph;

    @BeforeClass
	public static void initialize() {

	}

    @Test
    public void testRouting() throws Exception {

        String inputRoot = "input/";
        String graphName = "Graph.obj";
        if (!new File(inputRoot + graphName).exists()) {
            OTPMain.main(new String[]{"--build", inputRoot});
        }
        graph = Graph.load(new File(inputRoot + graphName), Graph.LoadLevel.FULL);

//        GraphService graphService = new GraphService();
//        graphService.registerGraph("", InputStreamGraphSource.newFileGraphSource("", new File(inputRoot), Graph.LoadLevel.FULL));
//        OTPServer otpServer = new OTPServer(new CommandLineParameters(), graphService);
////        Graph graph = graphService.getRouter().graph;
//        graph = otpServer.getRouter((String)null).graph;

        double left = 13.1949;
        double right = 13.5657;
        double bottom = 52.3926;
        double top = 52.6341;

        final Calendar calendar = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        TimeZone timeZone = TimeZone.getTimeZone("Europe/Berlin");
        df.setTimeZone(timeZone);
        calendar.setTimeZone(timeZone);
        calendar.setTime(df.parse("2016-08-01"));
        int departureTime = 8 * 60 * 60;
        calendar.add(Calendar.SECOND, departureTime);

        SyntheticRasterPopulation rasterPop = new SyntheticRasterPopulation();
        rasterPop.left = left;
        rasterPop.right = right;
        rasterPop.top = top;
        rasterPop.bottom = bottom;
        rasterPop.cols = 2;
        rasterPop.rows = 2;
        rasterPop.setup();

        for (Individual individual : rasterPop) {
            RoutingRequest request = new RoutingRequest();
            request.batch = true;
            request.setDateTime(calendar.getTime());
            request.from = new GenericLocation(individual.lat, individual.lon);
            request.setRoutingContext(graph);
            ShortestPathTree spt = (new AStar()).getShortestPathTree(request);
            if (spt != null) {
                for (Individual individual2 : rasterPop) {

                    double searchRadiusLat = SphericalDistanceLibrary.metersToDegrees(500.0D);

                    Coordinate c = new Coordinate(individual2.lon, individual2.lat);
                    Envelope env = new Envelope(c);
                    double xscale = Math.cos(c.y * 3.141592653589793D / 180.0D);
                    env.expandBy(searchRadiusLat / xscale, searchRadiusLat);
                    Collection vertices = graph.streetIndex.getVerticesForEnvelope(env);
                    Vertex destination = (Vertex) vertices.iterator().next();

                    GraphPath path = spt.getPath(destination, false);
                    // look in path


                    long initialWaitTime = Long.MIN_VALUE;
                    long elapsedTime = 0;
                    long transitTime = 0;
                    long walkTime = 0;

                    path.dump();
                    if (!path.states.isEmpty()) {
                        initialWaitTime = ((path.states.getFirst().getTimeInMillis() - calendar.getTime().getTime()) / 1000);
                        System.out.println("initial wait time: " + initialWaitTime);
                    }

                    for (State state : path.states) {
                        System.out.println("");
                        System.out.println("State infos start:");
                        System.out.println("state elapsedTime: " + elapsedTime);
                        System.out.println("state walkdistance: " + state.getWalkDistance());
                        System.out.println("isOnBoard: " + state.isOnboard());

                        Edge backEdge = state.getBackEdge();
                        if (backEdge != null && backEdge.getFromVertex() != null) {
                            System.out.println("backEdge = " + backEdge);
                            System.out.println("Mode: " + backEdge.getName());
                            System.out.println("backEdge.getFromVertex() = " + backEdge.getFromVertex());
//                            System.out.println("Name: " + backEdge.getFromVertex().getName());
                            System.out.println("Label" + backEdge.getFromVertex().getLabel());
                            System.out.println("Lat: " + backEdge.getFromVertex().getLat() + " Lon: " + backEdge.getFromVertex().getLon());
                            System.out.println("x: " + backEdge.getFromVertex().getX() + " y: " + backEdge.getFromVertex().getY());

                            if (state.isOnboard()) transitTime += state.getActiveTime() - elapsedTime;
                            else walkTime += state.getActiveTime() - elapsedTime;

                            elapsedTime = state.getActiveTime();


                            Assert.assertNotNull(path);
                            // TODO: write output
                        } else {
                            System.out.println("null");
                        }
                    }
                }

            }
        }

//        GraphPath path = null;

//        path = spt.getPath(stop_b, false);
//        Assert.assertNull(path);
//        Assert.assertEquals(6, path.states.size());

//        // A to C
//        request.setRoutingContext(graph, stop_a, stop_c);
//        spt = aStar.getShortestPathTree(request);
//
//        path = spt.getPath(stop_c, false);
//        Assert.assertNotNull(path);
//        Assert.assertEquals(8, path.states.size());
//
//        // A to D (change at C)
//        request.setRoutingContext(graph, stop_a, stop_d);
//        spt = aStar.getShortestPathTree(request);
//
//        path = spt.getPath(stop_d, false);
//        Assert.assertNotNull(path);
//        // there are two paths of different lengths
//        // both arrive at 40 minutes after midnight
//        //assertTrue(path.states.size() == 13);
//        long endTime = startTime + 40 * 60;
//        Assert.assertEquals(endTime, path.getEndTime());
//
//        //A to E (change at C)
//        request.setRoutingContext(graph, stop_a, stop_e);
//        spt = aStar.getShortestPathTree(request);
//        path = spt.getPath(stop_e, false);
//        Assert.assertNotNull(path);
//        Assert.assertTrue(path.states.size() == 14);
//        endTime = startTime + 70 * 60;
//        Assert.assertEquals(endTime, path.getEndTime());
    }

//    @Test
//	public void testRouteWithoutInitialWaitTime() {
//
//		String fromCoordSystem;
//		double fromX, fromY, toX, toY;
//
////		Alexanderplatz -> S Greifswalder
//		fromCoordSystem = "EPSG:31468";
//		fromX = 4596113.022;
//		fromY = 5821967.146;
//		toX = 4597730.486;
//		toY = 5823991.927;
//
//		Coord fromCoord = CoordUtils.createCoord(fromX, fromY);
//		Coord toCoord = CoordUtils.createCoord(toX, toY);
//
//		final ExecutorService service;
//	    final Future<long[]>  task;
//
//	    service = Executors.newFixedThreadPool(1);
//	    task    = service.submit(instantiateRouter(TransformationFactory.getCoordinateTransformation(
//	    		fromCoordSystem, TransformationFactory.WGS84), Constants.MATRIX_START_TIME, fromCoord, new ArrayList<>(Collections.singletonList(toCoord))));
//
//	    try {
//	        final long[] accessibilities;
//
//	        accessibilities = task.get();
////	        Assert.assertTrue(accessibilities[1] == 699);
//	        System.out.println(Arrays.toString(accessibilities));
//	    } catch(final InterruptedException | ExecutionException ex) {
//	    	ex.printStackTrace();
//	    }
//
//        service.shutdownNow();
//	}
//
//
//	public void testRouteWithInitialWaitTime() {
//
//		String fromCoordSystem;
//		double fromX, fromY, toX, toY;
//
////		Alexanderplatz -> S Greifswalder
//		fromCoordSystem = "EPSG:31468";
//		fromX = 4596113.022;
//		fromY = 5821967.146;
//		toX = 4597730.486;
//		toY = 5823991.927;
//
//		Coord fromCoord = CoordUtils.createCoord(fromX, fromY);
//		Coord toCoord = CoordUtils.createCoord(toX, toY);
//
//		final ExecutorService service;
//	    final Future<long[]>  task;
//
//	    service = Executors.newFixedThreadPool(1);
//	    task    = service.submit(instantiateRouter(TransformationFactory.getCoordinateTransformation(
//	    		fromCoordSystem, TransformationFactory.WGS84), Constants.MATRIX_START_TIME, fromCoord, new ArrayList<>(Collections.singletonList(toCoord))));
//
//	    try {
//	        final long[] accessibilities;
//
//	        accessibilities = task.get();
////	        Assert.assertTrue(accessibilities[1] == 699);
//	        System.out.println(Arrays.toString(accessibilities));
//	    } catch(final InterruptedException | ExecutionException ex) {
//	    	ex.printStackTrace();
//	    }
//
//        service.shutdownNow();
//	}
//
//	private OTPTimeRouterCallable instantiateRouter(CoordinateTransformation coordinateTransformation, double departureTime, Coord departure, List<Coord> measurePoints) {
//		return new OTPTimeRouterCallable(pathservice, Constants.DATE, Constants.TIME_ZONE, departureTime,
//																	departure, measurePoints, coordinateTransformation);
//	}
	

}
