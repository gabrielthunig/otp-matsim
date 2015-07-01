package routeMatrix;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.vehicles.VehicleWriterV1;

import otp.OTPTripRouterFactory;
import otp.ReadGraph;

public class ExtractNetwork {

    public static void main(String[] args) {
        ReadGraph readGraph = new ReadGraph(OTPTripRouterFactory.createGraphService(Constants.BASEDIR + Constants.OTP_GRAPH_FILE),
                TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, 
                		Constants.TARGET_SCENARIO_COORDINATE_SYSTEM),
                		Constants.DATE,
                		Constants.TIME_ZONE,
                		Constants.SCHEDULE_END_TIME_ON_FOLLOWING_DATE);
        readGraph.run();
        
        Network network = readGraph.getScenario().getNetwork();
        new NetworkWriter(network).write(Constants.NETWORK_FILE);

        new TransitScheduleWriter(readGraph.getScenario().getTransitSchedule()).writeFile(Constants.TRANSIT_SCHEDULE_FILE);
        new VehicleWriterV1(readGraph.getScenario().getTransitVehicles()).writeFile(Constants.TRANSIT_VEHICLE_FILE);
    }

}
