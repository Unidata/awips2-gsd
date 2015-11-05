package gov.noaa.gsd.viz.ensemble.action;

import gov.noaa.gsd.viz.ensemble.navigator.ui.layer.EnsembleTool;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import com.raytheon.viz.ui.tools.AbstractTool;

/**
 * This is how you start the Ensemble Tool.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Dec 7, 2014    5056      polster     Initial creation
 * 
 * </pre>
 * 
 * @author polster
 * @version 1.0
 */
public class EnsembleToolAction extends AbstractTool {

    public EnsembleToolAction() {

    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {

        if (EnsembleTool.isToolNotLoaded()) {
            EnsembleTool.getInstance().execute(event);
        }

        return null;

    }

}
