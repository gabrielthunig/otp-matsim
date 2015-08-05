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
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.GenericAStarFactory;
import org.opentripplanner.routing.impl.GraphServiceImpl;
import org.opentripplanner.routing.impl.RetryingPathServiceImpl;
import org.opentripplanner.routing.impl.SPTServiceFactory;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.services.PathService;

public class TimeMatrixCalculator {

	public static void main(String[] args) {
		
		String fromCoordSystem = TransformationFactory.DHDN_GK4;
		
		
		String inputFile = (Constants.BASEDIR + Constants.INPUT_FILE);
		
		List<Coord> measurePoints = getFacilities(inputFile);
		
		GraphService graphService = createGraphService(Constants.BASEDIR + Constants.OTP_GRAPH_FILE);
        SPTServiceFactory sptService = new GenericAStarFactory();
        PathService pathservice = new RetryingPathServiceImpl(graphService, sptService);
        
//      TODO: evtl erst mit späteren berechnungen anfangen wenn erste ergebnisse da sind, alpha
        
	    ExecutorService pool = Executors.newFixedThreadPool(measurePoints.size());
	    ArrayList<Future<long[]>> futures = new ArrayList<Future<long[]>>();
	    List<long[]> output = new ArrayList<long[]>();
	    int exectutedCallablesAtTime = 0;
	    int callablesExecuted = 0;
	    
	    for (int i = 0; i < measurePoints.size(); i++) {
	    	while (exectutedCallablesAtTime >= 8) {
	    		try {
					output.add(futures.get(callablesExecuted).get());
				} catch (InterruptedException | ExecutionException e) {
					e.printStackTrace();
				}
	    		
//	    	    TODO: output schreiben in einzelne dateinen, alpha
	    		
	    		InputsCSVWriter writer = new InputsCSVWriter("output/accessibility_Berlin/singleFromToAllFiles/fromToAllAccessibilities_" + callablesExecuted + ".csv", " ");
	    		
	    		for (int column = 0; column < output.get(callablesExecuted).length; column++) {
//	    			for (int row = 0; row < matrix[column].length; row++) {
	    				writer.writeField(callablesExecuted);
	    				writer.writeField(column);
	    				writer.writeField((output.get(callablesExecuted))[column]);
//	    			}
	    			writer.writeNewLine();
	    		}
	    		
	    		writer.close();
	    		callablesExecuted++;
	    		exectutedCallablesAtTime--;
	    	}
	    	Callable<long[]> callable = new OTPTimeRouterCallable(new Config(), pathservice, measurePoints.get(i), measurePoints, 
	    			TransformationFactory.getCoordinateTransformation( 
	        		fromCoordSystem, TransformationFactory.WGS84));
	    	Future<long[]> future = pool.submit(callable);
	    	futures.add(future);
	    	exectutedCallablesAtTime++;
	    }
	    for (int i = callablesExecuted; i < measurePoints.size(); i++) {
	    	try {
				output.add(futures.get(callablesExecuted).get());
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
	    }
		
//	    TODO: output gesammt in eine datei schreiben
	    
	    InputsCSVWriter idWriter = new InputsCSVWriter("output/accessibility_Berlin/ids.csv", ",");
		
	    idWriter.writeField("id");
	    idWriter.writeField("x");
	    idWriter.writeField("y");
	    
	    for (int j = 0; j < output.size(); j++) {
	    	idWriter.writeField(j);
	    	idWriter.writeField(measurePoints.get(j).getX());	
	    	idWriter.writeField(measurePoints.get(j).getY());
	    	idWriter.writeNewLine();
	    }
		
	    idWriter.close();
		
	    InputsCSVWriter writer = new InputsCSVWriter("output/accessibility_Berlin/allAccessibilities/accessibilities_berlin.csv", " ");
		
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
