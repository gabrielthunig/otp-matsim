package routeMatrix;

import java.io.File;
import java.io.IOException;
import java.util.List;

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

public class TimeMatrixCalculator {

	public static void main(String[] args) {
		
		String fromCoordSystem = TransformationFactory.WGS84_UTM33N;
		double minX = 4574000;
		double minY = 5802000;
		double maxX = 4620000;
		double maxY = 5839000;
		
		Coord fromCoord = new CoordImpl(minX, minY);
		Coord toCoord = new CoordImpl(maxX, maxY);
		
		OTPTimeRouter router = instantiateRouter(TransformationFactory.getCoordinateTransformation( 
        		fromCoordSystem, TransformationFactory.WGS84));
		System.out.println(router.routeLegTime(fromCoord, toCoord, Constants.MATRIX_START_TIME));
//		long[][] matrix = calcMatrix(null, Constants.MATRIX_START_TIME, router);
	}
	
	public static long[][] calcMatrix(List<Coord> facilities, double departureTime, OTPTimeRouter router) {
		long[][] result = new long[facilities.size()][facilities.size()];
		for (int i = 0; i < facilities.size(); i++) {
			for (int j = 0; j < facilities.size(); j++) {
				result[i][j] = router.routeLegTime(facilities.get(i), facilities.get(j), departureTime);
			}
		}
		return result;
	}
	
	private static OTPTimeRouter instantiateRouter(CoordinateTransformation coordinateTransformation) {
		GraphService graphService = createGraphService(Constants.BASEDIR + Constants.OTP_GRAPH_FILE);
        SPTServiceFactory sptService = new GenericAStarFactory();
        PathService pathservice = new RetryingPathServiceImpl(graphService, sptService);
        
		OTPTimeRouter router = new OTPTimeRouter(pathservice, Constants.DATE, Constants.TIME_ZONE, coordinateTransformation);
		
		return router;
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
