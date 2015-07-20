package routeMatrix;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;

import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.FacilitiesUtils;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.GenericAStarFactory;
import org.opentripplanner.routing.impl.GraphServiceImpl;
import org.opentripplanner.routing.impl.RetryingPathServiceImpl;
import org.opentripplanner.routing.impl.SPTServiceFactory;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.services.PathService;

public class OTPTimeRouterTest {
	
//	@Test
//	public void testRouterInstantiation() {
//		assertNotNull(router);
//		System.out.println("Router instantiated");
//	}
	
	@Test
	public void testRouteLeg() {
	
	String fromCoordSystem;
	double minX, minY, maxX, maxY;
	switch (0) {
    	case 1:
    		fromCoordSystem = Constants.TARGET_SCENARIO_COORDINATE_SYSTEM;
    		minX = 1486664.452457776;
    		minY = 6904535.315134093;
    		maxX = 1612601.082948133;
    		maxY = 6864957.156776331;		
            break;
        
    	default:
    		fromCoordSystem = TransformationFactory.WGS84_UTM33N;
    		minX = 387233;
    		minY = 5821893;
    		maxX = 392803;
    		maxY = 5820931;	
            break;    
    }
		
		Coord fromCoord = new CoordImpl(minX, minY);
		Coord toCoord = new CoordImpl(maxX, maxY);
		
		OTPTimeRouter router = instantiateRouter(TransformationFactory.getCoordinateTransformation( 
        		fromCoordSystem, TransformationFactory.WGS84));
		double time = router.routeLegTime(fromCoord, toCoord, Constants.MATRIX_START_TIME);
		System.out.println(time);
	}
	
	private OTPTimeRouter instantiateRouter(CoordinateTransformation coordinateTransformation) {
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
