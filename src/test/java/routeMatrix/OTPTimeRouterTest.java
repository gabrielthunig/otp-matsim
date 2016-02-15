package routeMatrix;

import junit.framework.TestCase;
import net.opengis.ows11.validation.OnlineResourceTypeValidator;
import org.junit.Assert;
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
    public static final double EPSILON = 1e-10;

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
        rasterPop.top = 33.9538;
        rasterPop.left = -117.4495;
        rasterPop.bottom = 33.9244;
        rasterPop.right = -117.3978;
        rasterPop.cols = 2;
        rasterPop.rows = 2;
        rasterPop.setup();

        Map<String, Vertex> vertices = OTPMatrixRouter.indexVertices(graph, rasterPop);

        double[][] timeMatrix = OTPMatrixRouter.routeMatrix(graph, calendar, vertices);
        double[] actuals = new double[timeMatrix.length * timeMatrix[0].length];
        int counter = 0;
        for (int i = 0; i < timeMatrix.length; i++) {
            for (int e = 0; e < timeMatrix[i].length; e++) {
                actuals[counter++] = timeMatrix[i][e];
            }
        }

        double[] expecteds = new double[16];
        expecteds[0] = 0.0;
        expecteds[1] = 366.0;
        expecteds[2] = 1659.0;
        expecteds[3] = 1577.0;
        expecteds[4] = 1780.0;
        expecteds[5] = 0.0;
        expecteds[6] = 1493.0;
        expecteds[7] = 1411.0;
        expecteds[8] = 1677.0;
        expecteds[9] = 2569.0;
        expecteds[10] = 0.0;
        expecteds[11] = 2447.0;
        expecteds[12] = 2290.0;
        expecteds[13] = 1924.0;
        expecteds[14] = 1499.0;
        expecteds[15] = 0.0;
        Assert.assertArrayEquals(expecteds, actuals, EPSILON);

        log.info("Shutdown");
	}

}
