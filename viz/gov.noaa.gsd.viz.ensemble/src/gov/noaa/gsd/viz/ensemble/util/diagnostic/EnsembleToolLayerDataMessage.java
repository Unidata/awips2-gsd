package gov.noaa.gsd.viz.ensemble.util.diagnostic;

/**
 * Used as the message payload for the Ensemble Tool diagnostic internal state
 * dialog.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * June 20, 2015            polster     Initial creation
 * 
 * </pre>
 * 
 * @author polster
 * @version 1.0
 */

public class EnsembleToolLayerDataMessage {

    private String displayContainerRef = null;

    private String toolLayerName = null;

    public EnsembleToolLayerDataMessage(String displayContainerRef,
            String toolLayerName, String resourceDataRef) {
        super();
        this.displayContainerRef = displayContainerRef;
        this.toolLayerName = toolLayerName;
    }

    public String getDisplayContainerRef() {
        return displayContainerRef;
    }

    public String getToolLayerName() {
        return toolLayerName;
    }

}
