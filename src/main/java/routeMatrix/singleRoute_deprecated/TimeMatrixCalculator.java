package routeMatrix.singleRoute_deprecated;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.opentripplanner.routing.graph.Graph;
import routeMatrix.CSVReader;

public class TimeMatrixCalculator {

//	public static void main(String[] args) {
//
//		final String baseDir = args[0];
//		final String outputDir = args[1];
////		final String baseDir = "input/accessibility_berlin_2/";
////		final String outputDir = "output/accessibility_berlin/";
//
//		String fromCoordSystem = Constants.INPUT_COORDINATE_SYSTEM;
//
//		String inputFile = (baseDir + Constants.INPUT_FILE);
//
//		List<Coord> measurePoints = getFacilities(inputFile);
//
//		GraphService graphService = createGraphService(baseDir + Constants.OTP_GRAPH_FILE);
//        SPTServiceFactory sptService = new GenericAStarFactory();
//        PathService pathservice = new RetryingPathServiceImpl(graphService, sptService);
//
//        System.out.println(measurePoints.size());
//
//        ExecutorService pool = Executors.newFixedThreadPool(4);
//	    ArrayList<Future<long[]>> futures = new ArrayList<Future<long[]>>();
//	    List<long[]> output = new ArrayList<long[]>();
//
//	    CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(
//	    		fromCoordSystem, TransformationFactory.WGS84);
//
//	    for (int i = 0; i < measurePoints.size(); i++) {
//	    	Callable<long[]> callable = new OTPTimeRouterCallable(pathservice, Constants.DATE, Constants.TIME_ZONE,
//	    			Constants.MATRIX_START_TIME, measurePoints.get(i), measurePoints, ct);
//	    	Future<long[]> future = pool.submit(callable);
//	    	futures.add(future);
//	    }
//
//	    for (int i = 0; i < futures.size(); i++) {
//	    	try {
//				output.add(futures.get(i).get());
//			} catch (InterruptedException | ExecutionException e) {
//				e.printStackTrace();
//			}
//	    	InputsCSVWriter writer = new InputsCSVWriter(outputDir + "fromToAllAccessibilities_" + i + ".csv", " ");
//
//    		for (int column = 0; column < output.get(i).length; column++) {
//    				writer.writeField(i);
//    				writer.writeField(column);
//    				writer.writeField((output.get(i))[column]);
//    			writer.writeNewLine();
//    		}
//
//    		writer.close();
//	    }
//
//	    InputsCSVWriter idWriter = new InputsCSVWriter(outputDir + "ids.csv", ",");
//
//	    idWriter.writeField("id");
//	    idWriter.writeField("x");
//	    idWriter.writeField("y");
//	    idWriter.writeNewLine();
//
//	    for (int j = 0; j < output.size(); j++) {
//	    	idWriter.writeField(j);
//	    	idWriter.writeField(measurePoints.get(j).getX());
//	    	idWriter.writeField(measurePoints.get(j).getY());
//	    	idWriter.writeNewLine();
//	    }
//
//	    idWriter.close();
//
//	    InputsCSVWriter writer = new InputsCSVWriter(outputDir + "accessibility_Berlin.csv", " ");
//
//	    for (int j = 0; j < output.size(); j++) {
//	    	for (int column = 0; column < output.get(j).length; column++) {
//	    		writer.writeField(j);
//	    		writer.writeField(column);
//	    		writer.writeField((output.get(j))[column]);
////
//	    		writer.writeNewLine();
//	    	}
//	    }
//
//	    writer.close();
//
//	    pool.shutdownNow();
//	    System.out.println("Shutdown");
//
//	}
	
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
					Coord actual = new Coord(x, y);
					facilities.add(actual);
				}
			} catch (Exception e) {
				continue;
			}
		}
		return facilities;
	}
	
	private static Graph createGraphService(String graphFile) {
		try {
			return Graph.load(new File(graphFile), Graph.LoadLevel.FULL);
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}
	
}
