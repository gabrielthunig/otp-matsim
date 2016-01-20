package routeMatrix;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.routing.algorithm.AStar;
import org.opentripplanner.routing.core.OptimizeType;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;



/**
 * Created by gthunig on 13.01.16.
 */
public class OTPTimeRouter {

    private Graph graph;

    private Date day;

    private final TimeZone timeZone;

    private CoordinateTransformation transitScheduleToPathServiceCt;

    private double departureTime;

    public OTPTimeRouter(Graph graph, String dateString, String timeZoneString, double departureTime, CoordinateTransformation ct) {
        this.graph = graph;
        this.transitScheduleToPathServiceCt = ct;
        this.timeZone = TimeZone.getTimeZone(timeZoneString);
        try {
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            df.setTimeZone(timeZone);
            this.day = df.parse(dateString);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        this.departureTime = departureTime;
    }

    public long[] calculateFromToAll(Coord departure, List<Coord> destinations) throws Exception {
        long[] accessibilities = new long[destinations.size()];

        RoutingRequest options = new RoutingRequest();
        options.setWalkBoardCost(3 * 60); // override low 2-4 minute values
        options.setBikeBoardCost(3 * 60 * 2);
        options.setOptimize(OptimizeType.QUICK);
        options.setMaxWalkDistance(Double.MAX_VALUE);

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(timeZone);
        calendar.setTime(day);
        calendar.add(Calendar.SECOND, (int) departureTime);
        options.setDateTime(calendar.getTime());

//        options.setRoutingContext(graph, stop_a, stop_b);
//        options.set


        AStar aStar = new AStar();
        ShortestPathTree spt = aStar.getShortestPathTree(options);
//        spt.
        for (int actual = 0; actual < accessibilities.length; actual++) {
            try {
                accessibilities[actual] = routeLegTime(destinations.get(actual));
            } catch (Exception e) {
                accessibilities[actual] = -1;
            }
        }
        return accessibilities;
    }

    private long routeLegTime(Coord toCoord) {
        LinkedList<Leg> legs = new LinkedList<Leg>();
        TraverseModeSet modeSet = new TraverseModeSet();
        modeSet.setWalk(true);
        modeSet.setTransit(true);
//		modeSet.setBicycle(true);
        RoutingRequest options = new RoutingRequest(modeSet);
        options.setWalkBoardCost(3 * 60); // override low 2-4 minute values
        options.setBikeBoardCost(3 * 60 * 2);
        options.setOptimize(OptimizeType.QUICK);
        options.setMaxWalkDistance(Double.MAX_VALUE);

        Calendar when = Calendar.getInstance();
        when.setTimeZone(timeZone);
        when.setTime(day);
        when.add(Calendar.SECOND, (int) departureTime);
        options.setDateTime(when.getTime());

        Coord transformedFromCoord = null;//transitScheduleToPathServiceCt.transform(fromCoord);
        Coord transformedToCoord = transitScheduleToPathServiceCt.transform(toCoord);

        options.from =  new GenericLocation(transformedFromCoord.getY(), transformedFromCoord.getX());
        options.to   =  new GenericLocation(transformedToCoord.getY(), transformedToCoord.getX());
        options.numItineraries = 1;
//		System.out.println("--------");
//		System.out.println("Path from " + options.from + " to " + options.to + " at " + when);
//		System.out.println("\tModes: " + modeSet);
//		System.out.println("\tOptions: " + options);

        List<GraphPath> paths = null;//pathservice.getPaths(options);

        long initialWaitTime = Long.MIN_VALUE;
        long elapsedTime = 0;
        long transitTime = 0;
        long walkTime = 0;
        if (paths != null) {
            GraphPath path = paths.get(0);
            path.dump();
            if(!path.states.isEmpty()){
                initialWaitTime = ((path.states.getFirst().getTimeInMillis() - day.getTime())/1000 - Math.round(departureTime));
//				System.out.println("initial wait time: " + initialWaitTime);
            }

            for (State state : path.states) {
//				System.out.println("");
//				System.out.println("State infos start:");
//				System.out.println("state elapsedTime: " + ((state.getTimeInMillis() - day.getTime())/1000 - Math.round(departureTime)));
//				System.out.println("state walkdistance: " + state.getWalkDistance());
//				System.out.println("isOnBoard: " + state.isOnboard());

                Edge backEdge = state.getBackEdge();
                if (backEdge == null) continue;
//				System.out.println("Mode: " + backEdge.getName());
//				System.out.println("Name: " + backEdge.getFromVertex().getName());
//				System.out.println("Label" + backEdge.getFromVertex().getLabel());
//				System.out.println("Lat: " + backEdge.getFromVertex().getLat() + " Lon: " + backEdge.getFromVertex().getLon());
//				System.out.println("x: " + backEdge.getFromVertex().getX() + " y: " + backEdge.getFromVertex().getY());

                if (state.isOnboard()) transitTime += state.getActiveTime() - elapsedTime;
                else walkTime += state.getActiveTime() - elapsedTime;

                elapsedTime = state.getActiveTime();

            }
        }
//		System.out.println("states elapsed times: " + elapsedTime);
//		System.out.println("thereof walked: " + walkTime);
//		System.out.println("thereof transited: " + transitTime);
//		System.out.println("initial wait time: "  + initialWaitTime);

        return (paths.get(0).getDuration() + initialWaitTime);
    }

}
