package gov.noaa.gsd.viz.ensemble.display.control.contour;

import com.raytheon.uf.viz.core.rsc.capabilities.AbstractCapability;
/**
 * 
 * The capability to pop up the "Contour Control" dialog on the legend by right clicking menu of 
 * a grid product or a ensemble product.
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Mar 14, 2017  DCS19598   polster     Initial creation
 * Mar 14, 2017  DCS19598   jing        removed stuff no longer to be used
 *
 * </pre>
 *
 * @author poster
 */

public class ContourControlCapability extends AbstractCapability {

  
    public ContourControlCapability() {
        
    }

    @Override
    public AbstractCapability clone() {
        return new ContourControlCapability();
    }
}
