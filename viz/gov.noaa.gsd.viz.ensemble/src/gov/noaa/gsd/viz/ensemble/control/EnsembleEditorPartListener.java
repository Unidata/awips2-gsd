package gov.noaa.gsd.viz.ensemble.control;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPartReference;

import com.raytheon.uf.viz.core.IDisplayPaneContainer;
import com.raytheon.uf.viz.xy.timeseries.TimeSeriesEditor;

import gov.noaa.gsd.viz.ensemble.control.EnsembleTool.EnsembleToolCompatibility;
import gov.noaa.gsd.viz.ensemble.control.EnsembleTool.EnsembleToolMode;

/**
 * This is the part listener for the editors associated with Ensemble Tool.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Nov 17, 2014    5056      polster     Initial creation
 * Mar 01, 2017    19443      polster     handle part opened for time series
 * 
 * </pre>
 * 
 * @author polster
 * @version 1.0
 */
public class EnsembleEditorPartListener implements IPartListener2 {

    private boolean ignorePartActivatedEvent = false;

    public EnsembleEditorPartListener() {
    }

    @Override
    public void partActivated(IWorkbenchPartReference partRef) {

        /*
         * Ignore the part activated event when ignore flag is set
         */
        if (!ignorePartActivatedEvent) {
            EnsembleToolCompatibility compatibility = EnsembleTool
                    .getEditorCompatibility(partRef);
            if (compatibility == EnsembleToolCompatibility.COMPATIBLE_CONTAINS_ENSEMBLE_TOOL_LAYER
                    || compatibility == EnsembleToolCompatibility.COMPATIBLE_NO_ENSEMBLE_TOOL_LAYER) {
                EnsembleTool.getInstance().refreshToolByActiveEditor();
            }
        }
    }

    @Override
    public void partClosed(final IWorkbenchPartReference partRef) {
        // Needed only because the interface requires it
    }

    @Override
    public void partDeactivated(IWorkbenchPartReference partRef) {
        // Needed only because the interface requires it
    }

    @Override
    public void partBroughtToTop(IWorkbenchPartReference partRef) {
        // Needed only because the interface requires it
    }

    @Override
    public void partOpened(IWorkbenchPartReference partRef) {
        /*
         * Did another editor open? If so, if there is not already a tool layer
         * associated with it the tell the manager about the new editor.
         */
        if (partRef instanceof IEditorReference) {
            IEditorPart openedEditor = ((IEditorReference) partRef)
                    .getEditor(false);
            if (openedEditor instanceof TimeSeriesEditor
                    && EnsembleTool.isExtant()) {
                IDisplayPaneContainer editor = (IDisplayPaneContainer) openedEditor;
                /*
                 * The time series editor opens automatically when a time series
                 * product is requested from the Volume Browser. If the ensemble
                 * tool is editable when the time series editor is opened then
                 * read the products into the ensemble tool.
                 */
                if (EnsembleTool.getInstance().isToolEditable()) {
                    EnsembleTool.getInstance().setEditor(editor);
                    /*
                     * Ensemble Tool only allows one time series editor at one
                     * time to be associated.
                     */
                    if (!EnsembleTool.hasToolLayer(editor)) {
                        EnsembleTool.getInstance().createToolLayer(editor,
                                EnsembleToolMode.LEGENDS_TIME_SERIES);
                    }
                    EnsembleTool.getInstance().refreshTool(true);
                }
                /*
                 * If the ensemble tool was not editable then make sure it gets
                 * or stays minimized.
                 */
                else {
                    EnsembleTool.getInstance().setEditor(null);
                    EnsembleTool.getInstance().refreshToolByActiveEditor();
                }
            }
        }
    }

    @Override
    public void partHidden(IWorkbenchPartReference partRef) {
        // Needed only because the interface requires it
    }

    @Override
    public void partVisible(IWorkbenchPartReference partRef) {
        // Needed only because the interface requires it
    }

    @Override
    public void partInputChanged(IWorkbenchPartReference partRef) {
        // Needed only because the interface requires it
    }

    /**
     * Allow this listener to ignore incoming part activated events.
     * 
     * @param ignore
     *            boolean to turn on/off the "ignore" state
     */
    public void ignorePartActivatedEvent(boolean ignore) {
        ignorePartActivatedEvent = ignore;
    }

}
