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

import com.vividsolutions.jts.geom.Coordinate;

public class TimeMatrixCalculator {

	public static void main(String[] args) {
		
		String fromCoordSystem = Constants.INPUT_COORDINATE_SYSTEM;
		
		String inputFile = (Constants.BASEDIR + Constants.INPUT_FILE);
		
		List<Coord> measurePoints = getFacilities(inputFile);
		
		GraphService graphService = createGraphService(Constants.BASEDIR + Constants.OTP_GRAPH_FILE);
        SPTServiceFactory sptService = new GenericAStarFactory();
        PathService pathservice = new RetryingPathServiceImpl(graphService, sptService);
        
        System.out.println(measurePoints.size());
        
        ExecutorService pool = Executors.newFixedThreadPool(16);
	    ArrayList<Future<long[]>> futures = new ArrayList<Future<long[]>>();
	    List<long[]> output = new ArrayList<long[]>();
	    
	    CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation( 
	    		fromCoordSystem, TransformationFactory.WGS84);
	    
	    for (int i = 0; i < measurePoints.size(); i++) {
	    	Callable<long[]> callable = new OTPTimeRouterCallable(pathservice, Constants.DATE, Constants.TIME_ZONE, 
	    			Constants.MATRIX_START_TIME, measurePoints.get(i), measurePoints, ct);
	    	Future<long[]> future = pool.submit(callable);
	    	futures.add(future);
	    }
	    
	    for (int i = 0; i < futures.size(); i++) {
	    	try {
				output.add(futures.get(i).get());
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
	    	InputsCSVWriter writer = new InputsCSVWriter("output/accessibility_Berlin/singleFromToAllFiles/fromToAllAccessibilities_" + i + ".csv", " ");
    		
    		for (int column = 0; column < output.get(i).length; column++) {
    				writer.writeField(i);
    				writer.writeField(column);
    				writer.writeField((output.get(i))[column]);
    			writer.writeNewLine();
    		}
    		
    		writer.close();
	    }
		
	    InputsCSVWriter idWriter = new InputsCSVWriter("output/accessibility_Berlin/ids.csv", ",");
		
	    idWriter.writeField("id");
	    idWriter.writeField("x");
	    idWriter.writeField("y");
	    idWriter.writeNewLine();
	    
	    for (int j = 0; j < output.size(); j++) {
	    	idWriter.writeField(j);
	    	idWriter.writeField(measurePoints.get(j).getX());	
	    	idWriter.writeField(measurePoints.get(j).getY());
	    	idWriter.writeNewLine();
	    }
		
	    idWriter.close();
		
	    InputsCSVWriter writer = new InputsCSVWriter("output/accessibility_Berlin/allAccessibilities/accessibility_Berlin.csv", " ");
		
	    for (int j = 0; j < output.size(); j++) {
	    	for (int column = 0; column < output.get(j).length; column++) {
	    		writer.writeField(j);
	    		writer.writeField(column);	
	    		writer.writeField((output.get(j))[column]);
//				
	    		writer.writeNewLine();
	    	}
	    }
		
	    writer.close();
	
	    pool.shutdownNow();
	    System.out.println("Shutdown");
	    
	}
	
	private static List<Coord> sampleMeasurePoints(List<Coord> measurePoints) {
		List<Coord> sampleMeasurePoints = new ArrayList<Coord>();
		int percentage = 3;
		for (Coord measurePoint : measurePoints) {
			int random = (int)((Math.random() * 100) + 1);
			if (random < percentage) {
				sampleMeasurePoints.add(measurePoint);
			}
		}
		return sampleMeasurePoints;
	}

	private static List<Coord> getFacilities(String inputFile) {
		CSVReader reader = new CSVReader(inputFile, ",");
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
