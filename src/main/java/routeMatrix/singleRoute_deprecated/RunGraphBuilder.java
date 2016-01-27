package routeMatrix.singleRoute_deprecated;

import org.opentripplanner.standalone.OTPMain;

public class RunGraphBuilder {

    public static void main(String[] args) {
        OTPMain.main(new String[]{"--build", args[0]});
    }

}
