package routeMatrix;

import net.opengis.ows11.validation.OnlineResourceTypeValidator;
import org.junit.Test;
import org.opentripplanner.analyst.batch.SyntheticRasterPopulation;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.util.TestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;
import java.util.TimeZone;

public class OTPTimeRouterTest {

    private static final Logger log = LoggerFactory.getLogger(OTPTimeRouterTest.class);

    @Test
    public void testMatrixRouting() throws Exception {

        String input_dir = "input/testMatrixRouting/";

        OTPMatrixRouter.buildGraph(input_dir);
        Graph graph = OTPMatrixRouter.loadGraph(input_dir);
        assert graph != null;

        log.info("Preparing settings for routing...");
        final Calendar calendar = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        TimeZone timeZone = TimeZone.getTimeZone("America/San_Francisco");
        df.setTimeZone(timeZone);
        calendar.setTimeZone(timeZone);
        try {
            calendar.setTime(df.parse("2016-02-02"));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        calendar.add(Calendar.SECOND, (7*60*57));
        log.info("Preparing settings for routing finished.");


        SyntheticRasterPopulation rasterPop = new SyntheticRasterPopulation();
        /*mission street
        rasterPop.left = -122.4231;
        rasterPop.right = -122.4174;
        rasterPop.top = 37.7528;
        rasterPop.bottom = 37.7401;*/
        rasterPop.top = 33.9538;
        rasterPop.left = -117.4495;
        rasterPop.bottom = 33.9244;
        rasterPop.right = -117.3978;
        rasterPop.cols = 3;
        rasterPop.rows = 3;
        rasterPop.setup();

        Map<String, Vertex> vertices = OTPMatrixRouter.indexVertices(graph, rasterPop);

        double[][] timeMatrix = OTPMatrixRouter.routeMatrix(graph, calendar, vertices);
        for (double[] column : timeMatrix) {
            for (double time : column) {
                System.out.println("time = " + time);
            }
        }




        log.info("Shutdown");

	}

}
