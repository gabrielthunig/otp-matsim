package otp;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.router.TripRouter;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.GenericAStarFactory;
import org.opentripplanner.routing.impl.GraphServiceImpl;
import org.opentripplanner.routing.impl.RetryingPathServiceImpl;
import org.opentripplanner.routing.impl.SPTServiceFactory;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.services.PathService;

import javax.inject.Provider;
import java.io.File;
import java.io.IOException;

public final class OTPTripRouterFactory implements
		Provider<TripRouter> {
	// TripRouterFactory: Matsim interface for routers
	
	private CoordinateTransformation ct;
	private String day;
	private String timeZone;
	private PathService pathservice;
    private TransitSchedule transitSchedule;
	private Network matsimNetwork;

	public OTPTripRouterFactory(TransitSchedule transitSchedule, Network matsimNetwork, 
			CoordinateTransformation ct, String day, String timeZone, String graphFile) {
        GraphService graphservice = createGraphService(graphFile);
        SPTServiceFactory sptService = new GenericAStarFactory();
        pathservice = new RetryingPathServiceImpl(graphservice, sptService);
		this.transitSchedule = transitSchedule;
		this.matsimNetwork = matsimNetwork;
		this.ct = ct;
		this.day = day;
		this.timeZone = timeZone;
	}

	public OTPTripRouterFactory(TransitSchedule transitSchedule, Network matsimNetwork, 
			CoordinateTransformation ct, String day, String timeZone, GraphService graphService) {
        SPTServiceFactory sptService = new GenericAStarFactory();
        pathservice = new RetryingPathServiceImpl(graphService, sptService);
		this.transitSchedule = transitSchedule;
		this.matsimNetwork = matsimNetwork;
		this.ct = ct;
		this.day = day;
		this.timeZone = timeZone;
	}
	
    public static GraphService createGraphService(String graphFile) {
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


    @Override
	public TripRouter get() {
		TripRouter tripRouter = new TripRouter();
		tripRouter.setRoutingModule("pt", new OTPRoutingModule(pathservice, transitSchedule, 
				matsimNetwork, day, timeZone, ct));
		return tripRouter;
	}
	
}