package routeMatrix;

import com.vividsolutions.jts.geom.Coordinate;
import junit.framework.TestCase;
import net.opengis.ows11.validation.OnlineResourceTypeValidator;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.opentripplanner.analyst.batch.Individual;
import org.opentripplanner.analyst.batch.SyntheticRasterPopulation;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.util.TestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class OTPTimeRouterTest {

    private static final Logger log = LoggerFactory.getLogger(OTPTimeRouterTest.class);
    private static final double EPSILON = 1e-10;

    @Ignore
    @Test
    public void testMatrixRouting() throws Exception {

        SyntheticRasterPopulation rasterPop = new SyntheticRasterPopulation();
        rasterPop.top = 33.9538;
        rasterPop.left = -117.4495;
        rasterPop.bottom = 33.9244;
        rasterPop.right = -117.3978;
        rasterPop.cols = 2;
        rasterPop.rows = 2;
        rasterPop.setup();

        String input_dir = "input/testMatrixRouting/";

        Iterator<Individual> iterator = rasterPop.iterator();
        List<Individual> individuals = new ArrayList<>();
        while (iterator.hasNext()) {
            individuals.add(iterator.next());
        }
        double[] actuals = new double[individuals.size()*individuals.size()];

        Graph graph = OTPMatrixRouter.loadGraph(input_dir);

        for (int i = 0; i < individuals.size(); i++) {
            for (int e = 0; e < individuals.size(); e++) {
                Coordinate origin = new Coordinate(individuals.get(i).lat, individuals.get(i).lon);
                Coordinate destination = new Coordinate(individuals.get(e).lat, individuals.get(e).lon);
                actuals[i*individuals.size()+e] = OTPMatrixRouter.getSingleRouteTime(graph, origin, destination);
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
