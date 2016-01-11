package routeMatrix;

import org.junit.BeforeClass;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.GenericAStarFactory;
import org.opentripplanner.routing.impl.GraphServiceImpl;
import org.opentripplanner.routing.impl.RetryingPathServiceImpl;
import org.opentripplanner.routing.impl.SPTServiceFactory;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.services.PathService;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class OTPTimeRouterTest {
	
	private static PathService pathservice;

    @BeforeClass
	public static void initialize() {
		GraphService graphService = createGraphService(Constants.BASEDIR + Constants.OTP_GRAPH_FILE);
        SPTServiceFactory sptService = new GenericAStarFactory();
        pathservice = new RetryingPathServiceImpl(graphService, sptService);
	}

    @Test
	public void testRouteWithoutInitialWaitTime() {
			
		String fromCoordSystem;
		double fromX, fromY, toX, toY;
		
//		Alexanderplatz -> S Greifswalder
		fromCoordSystem = "EPSG:31468";
		fromX = 4596113.022;
		fromY = 5821967.146;
		toX = 4597730.486;
		toY = 5823991.927;			
				
		Coord fromCoord = CoordUtils.createCoord(fromX, fromY);
		Coord toCoord = CoordUtils.createCoord(toX, toY);
			
		final ExecutorService service;
	    final Future<long[]>  task;
	    
	    service = Executors.newFixedThreadPool(1);        
	    task    = service.submit(instantiateRouter(TransformationFactory.getCoordinateTransformation( 
	    		fromCoordSystem, TransformationFactory.WGS84), Constants.MATRIX_START_TIME, fromCoord, new ArrayList<Coord>(Arrays.asList(toCoord))));
	
	    try {
	        final long[] accessibilities;
	        
	        accessibilities = task.get();
//	        Assert.assertTrue(accessibilities[1] == 699);
	        System.out.println(Arrays.toString(accessibilities));
	    } catch(final InterruptedException ex) {
	    	ex.printStackTrace();
	    } catch(final ExecutionException ex) {
	    	ex.printStackTrace();
	    }
	
	    service.shutdownNow();	
	}
	
	
	public void testRouteWithInitialWaitTime() {
		
		String fromCoordSystem;
		double fromX, fromY, toX, toY;
		
//		Alexanderplatz -> S Greifswalder
		fromCoordSystem = "EPSG:31468";
		fromX = 4596113.022;
		fromY = 5821967.146;
		toX = 4597730.486;
		toY = 5823991.927;			
				
		Coord fromCoord = CoordUtils.createCoord(fromX, fromY);
		Coord toCoord = CoordUtils.createCoord(toX, toY);
			
		final ExecutorService service;
	    final Future<long[]>  task;
	    
	    service = Executors.newFixedThreadPool(1);        
	    task    = service.submit(instantiateRouter(TransformationFactory.getCoordinateTransformation( 
	    		fromCoordSystem, TransformationFactory.WGS84), Constants.MATRIX_START_TIME, fromCoord, new ArrayList<Coord>(Arrays.asList(toCoord))));
	
	    try {
	        final long[] accessibilities;
	        
	        accessibilities = task.get();
//	        Assert.assertTrue(accessibilities[1] == 699);
	        System.out.println(Arrays.toString(accessibilities));
	    } catch(final InterruptedException ex) {
	    	ex.printStackTrace();
	    } catch(final ExecutionException ex) {
	    	ex.printStackTrace();
	    }
	
	    service.shutdownNow();	
	}
	
	private OTPTimeRouterCallable instantiateRouter(CoordinateTransformation coordinateTransformation, double departureTime, Coord departure, List<Coord> measurePoints) {
		return new OTPTimeRouterCallable(pathservice, Constants.DATE, Constants.TIME_ZONE, departureTime,
																	departure, measurePoints, coordinateTransformation);
	}
	
	private static GraphService createGraphService(String graphFile) {
        try {
            final Graph graph = Graph.load(new File(graphFile), Graph.LoadLevel.FULL);
            return new GraphServiceImpl() {
                public Graph getGraph(String routerId) {
                    return graph;
                }
            };
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException();
        }
    }
}
