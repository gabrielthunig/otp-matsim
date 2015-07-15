package routeMatrix;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.routing.core.OptimizeType;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.services.PathService;
import org.opentripplanner.routing.spt.GraphPath;

public class OTPTimeRouter {

	private PathService pathservice;
	
	private Date day;
	
	private final TimeZone timeZone;

	private CoordinateTransformation transitScheduleToPathServiceCt;
	
	public OTPTimeRouter(PathService pathservice, /*TransitSchedule transitSchedule, 
			Network matsimNetwork,*/ String dateString, String timeZoneString, CoordinateTransformation ct) {
		this.pathservice = pathservice;
//		this.transitSchedule = transitSchedule;
//		this.matsimNetwork = matsimNetwork;
		this.transitScheduleToPathServiceCt = ct;
		this.timeZone = TimeZone.getTimeZone(timeZoneString);
		try {
			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
			df.setTimeZone(timeZone);
			this.day = df.parse(dateString);
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}
	
	public long routeLegTime(Coord fromCoord, Coord toCoord, double departureTime) {
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

		Coord transformedFromCoord = transitScheduleToPathServiceCt.transform(fromCoord);
		Coord transformedToCoord = transitScheduleToPathServiceCt.transform(toCoord);
		
		options.from =  new GenericLocation(transformedFromCoord.getY(), transformedFromCoord.getX());
		options.to   =  new GenericLocation(transformedToCoord.getY(), transformedToCoord.getX());
		options.numItineraries = 1;
//		System.out.println("--------");
//		System.out.println("Path from " + options.from + " to " + options.to + " at " + when);
//		System.out.println("\tModes: " + modeSet);
//		System.out.println("\tOptions: " + options);

		List<GraphPath> paths = pathservice.getPaths(options);
		
		return paths.get(0).getDuration();
		
	}	
	
}
