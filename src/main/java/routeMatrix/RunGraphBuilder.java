package routeMatrix;

import org.opentripplanner.standalone.OTPMain;

public class RunGraphBuilder {

    public static void main(String[] args) {
        OTPMain.main(new String[]{"--build", Constants.BASEDIR});
    }

}
