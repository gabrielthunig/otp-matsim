package routeMatrix;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.GenericAStarFactory;
import org.opentripplanner.routing.impl.GraphServiceImpl;
import org.opentripplanner.routing.impl.RetryingPathServiceImpl;
import org.opentripplanner.routing.impl.SPTServiceFactory;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.services.PathService;

import playground.gthunig.utils.CSVReader;
import playground.gthunig.utils.TimeWatch;

public class TimeMatrixCalculator {

	public static void main(String[] args) {
		
TimeWatch watch = TimeWatch.start();
		
		String fromCoordSystem = TransformationFactory.DHDN_GK4;
		
		
		String inputFile = (Constants.BASEDIR + Constants.INPUT_FILE);
		
		List<Coord> measurePoints = getFacilities(inputFile);
		
		GraphService graphService = createGraphService(Constants.BASEDIR + Constants.OTP_GRAPH_FILE);
        SPTServiceFactory sptService = new GenericAStarFactory();
        PathService pathservice = new RetryingPathServiceImpl(graphService, sptService);
        
	    ExecutorService pool = Executors.newFixedThreadPool(measurePoints.size());
	    ArrayList<Future<long[]>> futures = new ArrayList<Future<long[]>>();
	    for (int i = 0; i < measurePoints.size(); i++) {
	    	Callable<long[]> callable = new OTPTimeRouterCallable(pathservice, Constants.DATE, Constants.TIME_ZONE, 
	    			Constants.MATRIX_START_TIME, TransformationFactory.getCoordinateTransformation( 
	        		fromCoordSystem, TransformationFactory.WGS84), measurePoints, i);
	    	Future<long[]> future = pool.submit(callable);
	    	futures.add(future);
	    }
	    List<long[]> output = new ArrayList<long[]>();
	    for (Future<long[]> future : futures) {
	    	try {
				output.add(future.get());
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
	    }
		
//		CSVWriter writer = new CSVWriter("output/accessibilities_output.csv");
//		
//		for (int column = 0; column < matrix.length; column++) {
//			for (int row = 0; row < matrix[column].length; row++) {
//				writer.writeField(matrix[column][row]);
//			}
//			writer.writeNewLine();
//		}
//		
//		writer.close();
		
		System.out.println("elapsed Time  in Minutes: " + watch.timeInMin());
	}
	
	private static List<Coord> getFacilities(String inputFile) {
		CSVReader reader = new CSVReader(inputFile);
		List<Coord> facilities = new ArrayList<Coord>();
		String[] line = null;
		while ((line = reader.readLine()) != null) {
			try {
				double x,y;
				x = Double.parseDouble(line[0]);
				y = Double.parseDouble(line[1]);
				if (x > 0 && y > 0) {
					Coord actual = new CoordImpl(x, y);
					facilities.add(actual);
				}
			} catch (Exception e) {
				continue;
			}
		}
		return facilities;
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
