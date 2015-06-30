package gov.noaa.gsd.viz.ensemble.navigator.ui.layer;

import gov.noaa.gsd.viz.ensemble.display.calculate.Calculation;
import gov.noaa.gsd.viz.ensemble.display.calculate.Range;
import gov.noaa.gsd.viz.ensemble.display.common.GenericResourceHolder;
import gov.noaa.gsd.viz.ensemble.display.control.EnsembleResourceManager;
import gov.noaa.gsd.viz.ensemble.navigator.ui.viewer.EnsembleToolViewer;
import gov.noaa.gsd.viz.ensemble.navigator.ui.viewer.ViewerWindowState;
import gov.noaa.gsd.viz.ensemble.util.Utilities;
import gov.noaa.gsd.viz.ensemble.util.diagnostic.EnsembleToolDiagnosticStateDialog;
import gov.noaa.gsd.viz.ensemble.util.diagnostic.EnsembleToolDiagnosticTraceMessage;
import gov.noaa.gsd.viz.ensemble.util.diagnostic.EnsembleToolLayerDataMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.ISaveablePart2;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.viz.core.IDisplayPane;
import com.raytheon.uf.viz.core.IDisplayPaneContainer;
import com.raytheon.uf.viz.core.IRenderableDisplayChangedListener;
import com.raytheon.uf.viz.core.drawables.IDescriptor;
import com.raytheon.uf.viz.core.drawables.IDescriptor.FramesInfo;
import com.raytheon.uf.viz.core.drawables.IRenderableDisplay;
import com.raytheon.uf.viz.core.drawables.ResourcePair;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.rsc.AbstractVizResource;
import com.raytheon.uf.viz.core.rsc.IDisposeListener;
import com.raytheon.uf.viz.core.rsc.IResourceDataChanged.ChangeType;
import com.raytheon.uf.viz.core.rsc.LoadProperties;
import com.raytheon.uf.viz.core.rsc.ResourceType;
import com.raytheon.uf.viz.core.rsc.capabilities.EditableCapability;
import com.raytheon.uf.viz.core.rsc.tools.GenericToolsResourceData;
import com.raytheon.uf.viz.d2d.ui.map.SideView;
import com.raytheon.uf.viz.xy.timeseries.TimeSeriesEditor;
import com.raytheon.uf.viz.xy.timeseries.display.TimeSeriesDescriptor;
import com.raytheon.viz.ui.EditorUtil;
import com.raytheon.viz.ui.UiUtil;
import com.raytheon.viz.ui.editor.AbstractEditor;
import com.raytheon.viz.ui.editor.IMultiPaneEditor;
import com.raytheon.viz.ui.editor.VizMultiPaneEditor;
import com.raytheon.viz.ui.tools.AbstractTool;

/**
 * The Ensemble Tool is a CAVE feature/tool that currently runs in the D/2D
 * perspective by either clicking an optional button on the CAVE perspective
 * menu, or choosing a menu item of the same name in the Tools menu. Depending
 * upon how this tool is initially configured in CAVE there may be only one of
 * those starting buttons available.
 * 
 * This class is the controlling manager class for the D/2D Ensemble Tool. When
 * the user opens the Ensemble Tool, a navigator view is opened and an
 * EnsembleToolLayer instance, given the name "Ensemble Tool", gets associated
 * with the currently active AbstractEditor, and also gets set to "editable". It
 * is this active and editable instance of tool layer which allows the user to
 * activate or deactivate the Ensemble Tool for the associated active editor.
 * 
 * When the Ensemble Tool is opened and editable ("powered on"), any resources
 * subsequently loaded will be virtually associated with the currently active
 * editor and tool layer pair. In addition, these resources will be stored in
 * the ResourceManager class (see gov.noaa.gsd.viz.ensemble.display.control
 * package). Yet instead of showing these resource's legends in the default
 * manner of displaying the legend in the active editor, these resources are
 * displayed in the Ensemble Tool's "navigator view", which is a PartView called
 * the EnsembleToolViewer.
 * 
 * This manager class is therefore responsible for associating and maintaining
 * all instances of EnsembleToolLayers and their associated AbstractEditor
 * instances, and adding an additional layer of control for displaying those
 * resources, and representing those resources in the EnsembleToolViewer.
 * 
 * @author polster
 * @author jing
 * 
 *         <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Nov 16, 2014    5056    polster     Initial creation
 * 
 * </pre>
 * 
 * @version 1.0
 */

public class EnsembleTool extends AbstractTool implements
        IRenderableDisplayChangedListener, IToolLayerChanged, IDisposeListener {

    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(EnsembleTool.class);

    private static EnsembleTool SINGLETON = null;

    protected static final int MAX_TOOL_LAYER_COUNT = 5;

    protected EnsembleToolViewer ensembleToolViewer = null;

    private IWorkbenchPartReference viewPartRef = null;

    private boolean foreignEditableToolLoading = false;

    /*
     * Diagnostics display and content
     */
    private EnsembleToolDiagnosticStateDialog diagnosticsDialog = null;

    private List<EnsembleToolDiagnosticTraceMessage> traceMessages = null;

    private List<EnsembleToolLayerDataMessage> toolLayerDataMessages = null;

    private static int DiagnosticCount = 0;

    private boolean showDiagnostic = false;

    private Point lastKnownShellLocation = null;

    private Point lastKnownShellSize = null;

    private EnsembleEditorPartListener theEditorsListener = null;

    /*
     * The EnsembleTool adheres to a singleton pattern. This method is
     * the accessor to the singleton instance reference.
     */
    public static EnsembleTool getInstance() {
        if (SINGLETON == null) {
            SINGLETON = new EnsembleTool();
        }
        return SINGLETON;
    }

    /*
     * The EnsembleTool adheres to a singleton pattern. This constructor
     * is therefore private.
     * 
     * This ctor creates the ETLMResourceDataManager singleton and also adds a
     * shutdown hook which guarantees that this class is disposed of properly.
     */
    private EnsembleTool() {

        ETLMResourceDataManager.getInstance();

        traceMessages = new ArrayList<>();

        lastKnownShellLocation = new Point(25, 25);
        lastKnownShellSize = new Point(
                EnsembleToolDiagnosticStateDialog.DIALOG_WIDTH,
                EnsembleToolDiagnosticStateDialog.DIALOG_HEIGHT);

        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                dispose();
            }

        });

    }

    public void dispose() {
        EnsembleResourceManager.getInstance().stopSpooler();
    }

    /*
     * This method is associated with the AbstractTool parent class. It would
     * normally be associated with an action such as a button click, but the RCP
     * requires those classes to have a default constructor, and this class is a
     * singleton. So the EnsembleToolLayerManagerAction class is the pass-thru
     * AbstractTool whose execute method calls this execute method.
     */
    @Override
    public Object execute(ExecutionEvent arg0) throws ExecutionException {

        if (editor == null) {
            super.setEditor(EditorUtil.getActiveVizContainer());
            if (editor == null) {
                /*
                 * no editor availabe (e.g. all editors closed) so do nothing.
                 */
                return null;
            }
        }

        if (theEditorsListener == null) {
            theEditorsListener = new EnsembleEditorPartListener();
            PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                    .getActivePage().addPartListener(theEditorsListener);
        }

        if (ensembleToolViewer == null) {
            try {
                ensembleToolViewer = initView();
            } catch (VizException e) {
                statusHandler.handle(Priority.PROBLEM, e.getLocalizedMessage(),
                        e);
            }
        }

        /*
         * either find an existing ensemble tool layer in the active editor ...
         * or create a new one. Either way this method will make the new or
         * existing tool editable.
         */
        EnsembleToolLayer etl = getActiveToolLayer();
        if (etl == null) {
            createToolLayer(editor);
            EnsembleResourceManager.getInstance().startSpooler();
        }
        refreshTool(false);

        return null;

    }

    public int verifyCloseActiveToolLayer() {
        int userResponse = 0;

        String tln = "";
        boolean lastRemainingResource = false;

        /* No need to prompt when there are no tool layers. Just close the view. */
        if (ETLMResourceDataManager.getInstance().isEmpty()) {
            userResponse = ISaveablePart2.NO;
            ensembleToolViewer = null;
        }
        /* There is still at least one tool layer existing */
        else {
            EnsembleToolLayer etl = getActiveToolLayer();
            if (etl != null) {
                tln = etl.getName();

                if ((ETLMResourceDataManager.getInstance().getToolLayerCount() == 1)
                        && (isActiveToolLayerEmpty())) {
                    userResponse = ISaveablePart2.NO;
                    etl.unload();
                } else {
                    boolean userRequestedClose = MessageDialog.openConfirm(
                            Display.getCurrent().getActiveShell(),
                            "Confirm Ensemble Tool layer close",
                            "Close the current tool layer: " + tln + "?");

                    if (userRequestedClose) {
                        if (ETLMResourceDataManager.getInstance()
                                .getToolLayerCount() == 1) {
                            lastRemainingResource = true;
                        }
                        etl.unload();
                        if (lastRemainingResource) {
                            userResponse = ISaveablePart2.NO;
                            ensembleToolViewer = null;
                        } else {
                            userResponse = ISaveablePart2.CANCEL;
                        }
                    } else {
                        userResponse = ISaveablePart2.CANCEL;
                    }
                }
            }
        }
        return userResponse;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.viz.core.rsc.IDisposeListener#disposed(com.raytheon.uf
     * .viz.core.rsc.AbstractVizResource)
     * 
     * Did the ensemble tool layer get disposed?
     */
    @Override
    public void disposed(AbstractVizResource<?, ?> rsc) {

        if (rsc instanceof EnsembleToolLayer) {
            EnsembleToolLayer toolLayer = (EnsembleToolLayer) rsc;
            EnsembleResourceManager.getInstance().removeMapping(toolLayer);
            removeToolLayer(toolLayer);
        }

    }

    private void removeToolLayer(EnsembleToolLayer toolLayer) {

        if (toolLayer != null) {

            toolLayer.unregisterListener((IToolLayerChanged) this);
            toolLayer.unregisterListener((IDisposeListener) this);

            IDisplayPaneContainer editorPane = ETLMResourceDataManager
                    .getInstance().findEditor(toolLayer);

            if (editorPane != null) {

                editorPane.removeRenderableDisplayChangedListener(this);

                theEditorsListener.removeEditor(editorPane);

                ETLMResourceDataManager.getInstance().unload(toolLayer);

                if (ensembleToolViewer != null) {
                    if (ETLMResourceDataManager.getInstance().isEmpty()) {
                        setViewerWindowState(ViewerWindowState.CLOSE);
                        EnsembleResourceManager.getInstance().stopSpooler();
                        ensembleToolViewer = null;
                    }
                }
            }
        }
    }

    /*
     * Add a new ensemble tool layer to the given editor. Keep track of the
     * association in the ETLMResourceDataManager. This method requires that the
     * super.editor data member is already set.
     */
    public void createToolLayer(IDisplayPaneContainer editorPane) {

        if (editorPane != null) {
            editor = editorPane;
            IDescriptor descr = editor.getActiveDisplayPane().getDescriptor();
            EnsembleToolLayer etl = null;
            try {
                etl = ETLMResourceDataManager.getInstance().constructToolLayer(
                        editor, descr);
            } catch (VizException e) {
                statusHandler.handle(Priority.PROBLEM, e.getLocalizedMessage(),
                        e);
                statusHandler.error("Unable to create a tool layer in editor: "
                        + ((AbstractEditor) editor).getTitle());
                return;
            }
            if (etl != null) {
                descr.getResourceList().add(etl);
                etl.setEditable(true);
                etl.registerListener((IToolLayerChanged) this);
                editor.addRenderableDisplayChangedListener(this);
                theEditorsListener.addEditor(editor);
                etl.registerListener((IDisposeListener) EnsembleTool
                        .getInstance());

                etl.issueRefresh();
                editor.refresh();
            }
        }
    }

    /*
     * Given a viz resource, return the associated resource pair.
     */
    public ResourcePair getResourcePair(AbstractVizResource<?, ?> rsc) {

        ResourcePair rp = null;
        EnsembleToolLayer etl = getActiveToolLayer();
        if (etl != null) {
            rp = etl.getResourcePair(rsc);
        }
        return rp;
    }

    /*
     * Given an ensemble tool layer return its associated editor.
     */
    public IDisplayPaneContainer findEditor(EnsembleToolLayer tool) {
        IDisplayPaneContainer e = ETLMResourceDataManager.getInstance()
                .findEditor(tool);
        return e;
    }

    /*
     * Does the given editor have an assoicated ensemble tool layer?
     */
    public boolean hasToolLayer(IDisplayPaneContainer editor) {

        EnsembleToolLayer etl = getActiveToolLayer();
        if (etl != null) {
            return true;
        }
        return false;
    }

    public boolean hasEditor(IDisplayPaneContainer editorPane) {
        return ETLMResourceDataManager.getInstance().hasEditor(editorPane);
    }

    /*
     * Given the group name (aka the top-level title of an ensemble product) set
     * it to be the default resource for subsequent calculations on the
     * ResourceManager.
     */
    public void setEnsembleCalculationResource(String rscGroupName) {
        if (editor != null) {
            EnsembleResourceManager.getInstance()
                    .setEnsembleCalculationResourceName(
                            (AbstractEditor) editor, rscGroupName);
        }
    }

    /*
     * Get the group name (aka the top-level title of an ensemble product) for
     * the default resource for subsequent calculations on the ResourceManager.
     */
    public String getEnsembleCalculationResourceName() {

        String s = "";
        if (editor != null) {
            s = EnsembleResourceManager
                    .getInstance()
                    .getEnsembleCalculationResourceName((AbstractEditor) editor);
        }
        return s;
    }

    /*
     * Update the legend time information in the navigator view.
     */
    public void frameChanged(FramesInfo framesInfo) {
        if (ensembleToolViewer != null) {
            ensembleToolViewer.frameChanged(framesInfo);
        }
    }

    /*
     * Return the time basis resource name by getting the active editor and
     * requesting the name from the ResourceManager.
     */
    public String getTimeBasisResourceName() {

        String s = "";
        if (getActiveToolLayer() != null) {
            s = EnsembleResourceManager.getInstance().getTimeBasisResourceName(
                    getActiveToolLayer());
        }
        return s;
    }

    public String getTimeSeriesPoint() {
        String s = "";
        if (getActiveToolLayer() != null) {
            s = EnsembleResourceManager.getInstance().getTimeSeriesPoint(
                    getActiveToolLayer());
        }
        return s;
    }

    /*
     * Return the time basis legend time by getting the active editor and
     * requesting the time from the ResourceManager.
     */
    public String getTimeBasisLegendTime() {

        String s = "";
        if (getActiveToolLayer() != null) {
            s = EnsembleResourceManager.getInstance().getTimeBasisLegendTime(
                    getActiveToolLayer());
        }
        return s;

    }

    public IDisplayPaneContainer getActiveEditor() {
        return editor;
    }

    /*
     * This class is an IRenderableDisplayChangedListener so we must override
     * this method. This happens during a swap.
     */
    @Override
    public void renderableDisplayChanged(IDisplayPane pane,
            IRenderableDisplay newRenderableDisplay, DisplayChangeType type) {

        /*
         * New editor becoming coming active
         */
        if (type == DisplayChangeType.ADD
                && newRenderableDisplay.getContainer() instanceof IDisplayPaneContainer) {

            IDisplayPaneContainer editorChanged = (IDisplayPaneContainer) newRenderableDisplay
                    .getContainer();
            super.setEditor(editorChanged);

            EnsembleToolLayer etl = getActiveToolLayer();
            if (etl != null) {
                if (pane.getRenderableDisplay().getContainer() == editor) {
                    ETLMResourceDataManager.getInstance()
                            .updateToolLayerEditor(
                                    etl,
                                    (IWorkbenchPart) newRenderableDisplay
                                            .getContainer());
                }
            }
            refreshTool(false);
        }

        /*
         * Remove to side view
         */
        if ((type == DisplayChangeType.REMOVE)
                && (newRenderableDisplay.getContainer() instanceof SideView)) {
            EnsembleToolLayer etl = getActiveToolLayer();
            if (etl != null) {
                if (pane.getRenderableDisplay().getContainer() == editor) {
                    ETLMResourceDataManager.getInstance()
                            .updateToolLayerEditor(
                                    etl,
                                    (IWorkbenchPart) newRenderableDisplay
                                            .getContainer());
                }
            }
        }
    }

    /*
     * The currently active editor (super.editor) must be set before calling
     * this method.
     */
    protected IDisplayPane[] getSelectedPanes() {

        IDisplayPane[] displayPanes = editor.getDisplayPanes();

        if (editor instanceof IMultiPaneEditor) {
            IDisplayPane selected = ((IMultiPaneEditor) editor)
                    .getSelectedPane(IMultiPaneEditor.LOAD_ACTION);
            if (selected != null) {
                displayPanes = new IDisplayPane[] { selected };
            }
        }
        return displayPanes;
    }

    /*
     * Is there an ensemble tool layer in the active editor?
     */
    public EnsembleToolLayer getActiveToolLayer() {

        EnsembleToolLayer foundToolLayer = null;
        if (editor != null) {
            for (IDisplayPane pane : getSelectedPanes()) {
                IDescriptor desc = pane.getDescriptor();
                Iterator<ResourcePair> iter = desc.getResourceList().iterator();

                while (iter.hasNext()) {
                    ResourcePair pair = iter.next();
                    AbstractVizResource<?, ?> rsc = pair.getResource();

                    if (rsc instanceof EnsembleToolLayer) {
                        foundToolLayer = (EnsembleToolLayer) rsc;
                        break;
                    }
                }
            }
        }
        return foundToolLayer;
    }

    /*
     * This method will set the visibility state of the EnsembleToolViewer to
     * "show with focus", "show without focus", "minimized", or "closed".
     */
    synchronized public void setViewerWindowState(
            final ViewerWindowState windowState) {

        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {

                IWorkbenchWindow window = null;
                IWorkbenchPage page = null;

                // Get the active window
                window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
                if (window == null) {
                    return;
                }

                // Get the active page
                page = window.getActivePage();
                if (page == null) {
                    return;
                }

                if (windowState == ViewerWindowState.CLOSE) {
                    IViewPart viewPart = page.findView(EnsembleToolViewer.ID);
                    page.hideView(viewPart);
                } else if (windowState == ViewerWindowState.SHOW_WITH_FOCUS) {
                    try {
                        if (viewPartRef != null) {
                            // IWorkbenchPartReference viewPartRef = page
                            // .findViewReference(EnsembleToolViewer.ID);
                            page.setPartState(viewPartRef,
                                    IWorkbenchPage.STATE_RESTORED);
                            page.showView(EnsembleToolViewer.ID, null,
                                    IWorkbenchPage.VIEW_ACTIVATE);
                        }
                    } catch (PartInitException e) {
                        statusHandler.handle(Priority.PROBLEM,
                                "Unable to show Ensemble viewer.");
                    }
                } else if (windowState == ViewerWindowState.SHOW_WITHOUT_FOCUS) {
                    try {
                        if (viewPartRef != null) {
                            // IWorkbenchPartReference viewPartRef = page
                            // .findViewReference(EnsembleToolViewer.ID);
                            page.setPartState(viewPartRef,
                                    IWorkbenchPage.STATE_RESTORED);
                            page.showView(EnsembleToolViewer.ID, null,
                                    IWorkbenchPage.VIEW_VISIBLE);
                        }
                    } catch (PartInitException e) {
                        statusHandler.handle(Priority.PROBLEM,
                                "Unable to show Ensemble viewer.");
                    }
                } else if (windowState == ViewerWindowState.MINIMIZED) {
                    if (viewPartRef != null) {
                        page.setPartState(viewPartRef,
                                IWorkbenchPage.STATE_MINIMIZED);
                    }
                }
            }
        });
    }

    /*
     * This method should be called when the Ensemble Tool is initially opened
     * or re-opened. It returns the newly created EnsmebleToolViewer, which is,
     * in essence, a singleton.
     */
    private EnsembleToolViewer initView() throws VizException {

        IViewPart viewPart = null;

        EnsembleToolViewer viewer = null;
        // Get the active window
        IWorkbenchWindow window = PlatformUI.getWorkbench()
                .getActiveWorkbenchWindow();
        if (window == null) {
            throw new VizException("Can't get Active Workbench Window!");
        }

        // Get the active page
        IWorkbenchPage page = window.getActivePage();
        if (page == null) {
            throw new VizException("Can't get Active Workbench Page!");
        }

        viewPart = page.findView(EnsembleToolViewer.ID);
        if (viewPart == null) {
            // EnsembleToolViewer was not already created ...

            try {
                page.showView(EnsembleToolViewer.ID, null,
                        IWorkbenchPage.VIEW_CREATE);
            } catch (PartInitException e) {
                statusHandler.handle(Priority.PROBLEM,
                        "Unable to show EnsembleToolViewer.");
            }
            viewPart = page.findView(EnsembleToolViewer.ID);

            viewPartRef = page.findViewReference(EnsembleToolViewer.ID);
            page.setPartState(viewPartRef, IWorkbenchPage.STATE_RESTORED);
            if (viewPart == null) {
                statusHandler.handle(Priority.PROBLEM,
                        "Unable to create EnsembleToolViewer on second pass.");
                throw new VizException(
                        "Unable to create EnsembleToolViewer on second pass!");
            } else {
                if (viewPart instanceof EnsembleToolViewer) {
                    viewer = (EnsembleToolViewer) viewPart;
                }
            }

            // the EnsembleToolViewer is already created ....
        } else {
            IWorkbenchPartReference viewPartRef = page
                    .findViewReference(EnsembleToolViewer.ID);
            page.setPartState(viewPartRef, IWorkbenchPage.STATE_RESTORED);
            if (viewPart instanceof EnsembleToolViewer) {
                viewer = (EnsembleToolViewer) viewPart;
            } else {
                viewer = null;
                statusHandler.handle(Priority.PROBLEM,
                        "EnsembleToolViewer gone MIA");
                throw new VizException("EnsembleToolViewer gone MIA!");
            }
        }
        return viewer;
    }

    /*
     * Given a top level ensemble resource name, unload its members.
     */
    public void unloadResourcesByName(String topLevelEnsembleName) {

        EnsembleToolLayer etl = getActiveToolLayer();
        if (etl != null) {
            etl.unloadAllResourcesByName(topLevelEnsembleName);
        }
    }

    /*
     * Get the map of resources associated with the current active tool layer.
     */
    public Map<String, List<GenericResourceHolder>> getEnsembleResources() {

        Map<String, List<GenericResourceHolder>> ensembleResources = null;
        EnsembleToolLayer etl = getActiveToolLayer();
        if (etl != null) {
            ensembleResources = etl.getEnsembleResources();
        } else {
            ensembleResources = getEmptyResourceList();
        }
        return ensembleResources;
    }

    public Map<String, List<GenericResourceHolder>> getEmptyResourceList() {
        return new HashMap<String, List<GenericResourceHolder>>();
    }

    /*
     * Exercise a given calculation on the visible resources of a given tool
     * layer.
     */
    public void calculate(Calculation algorithm) {
        EnsembleToolLayer etl = getActiveToolLayer();
        if (etl != null) {
            etl.calculate(algorithm);
        }
    }

    /*
     * Exercise a given calculation against a given range on the visible
     * resources of a given tool layer.
     */
    public void calculate(Calculation algorithm, Range range) {
        EnsembleToolLayer etl = getActiveToolLayer();
        if (etl != null) {
            etl.calculate(algorithm, range);
        }
    }

    /*
     * This tool manager is responsible for updating the state of the navigator
     * widget, EnsembleToolViewer. Call this method to refresh the view to
     * reflect a change in state of the active editor.
     */
    public void refreshView() {

        if (isToolAvailable()) {
            ensembleToolViewer.updateInfoTab();
            ensembleToolViewer.refreshInput();
        }
    }

    synchronized private void refreshToolForeignLoad() {
        if (ensembleToolViewer != null) {
            ensembleToolViewer.setViewEditable(false);
            if (isMinimizeOnForeignToolLoadPreference()) {
                setViewerWindowState(ViewerWindowState.MINIMIZED);
            }
            foreignEditableToolLoading = false;
        }
    }

    /*
     * Make sure you have called the super.setEditor method before calling this
     * method. Return true if there is an ensemble tool layer associated with
     * the given editor argument.
     */
    synchronized public boolean refreshTool(boolean isAlreadyActivated) {

        boolean toolLayerFound = false;

        EnsembleToolLayer etl = getActiveToolLayer();

        ResourceType rt = ResourceType.PLAN_VIEW;
        if (ensembleToolViewer != null) {
            if (TimeSeriesEditor.class.isAssignableFrom(editor.getClass())) {
                rt = ResourceType.TIME_SERIES;
            } else if (VizMultiPaneEditor.class.isAssignableFrom(editor
                    .getClass())) {
                rt = ResourceType.PLAN_VIEW;
            }
            ensembleToolViewer.setEditorType(rt);

            if (etl == null) {
                ensembleToolViewer.disableTool();
                if (!isAlreadyActivated) {
                    setViewerWindowState(ViewerWindowState.MINIMIZED);
                }
            } else {
                toolLayerFound = true;
                ensembleToolViewer.setViewEditable(etl.isEditable());
                if (!isAlreadyActivated) {
                    if (etl.isEditable()) {
                        setViewerWindowState(ViewerWindowState.SHOW_WITHOUT_FOCUS);
                    } else {
                        if (ensembleToolViewer.isMinimizeOnToggleUneditable()) {
                            setViewerWindowState(ViewerWindowState.MINIMIZED);
                        }
                    }
                }
            }
            refreshView();
        }

        return toolLayerFound;
    }

    /*
     * (non-Javadoc)
     * 
     * The sole purpose of this method is to minimize or restore the ensemble
     * tool viewer (ViewPart) when its editability is turned off or on,
     * respectively (if it is not already done).
     */
    @Override
    public void resourceChanged(EnsembleToolLayer etl, ChangeType type,
            Object object) {

        if (!isToolAvailable()) {
            return;
        }

        if (type == ChangeType.CAPABILITY
                && object instanceof EditableCapability) {

            if (isForeignEditableToolLoading()) {
                refreshToolForeignLoad();
            } else {
                refreshTool(false);
            }
        }
    }

    /*
     * Turn the tool layer for the ensemble tool on or off.
     */
    public void setEditable(boolean makeEditable) {

        EnsembleToolLayer etl = getActiveToolLayer();
        if (etl != null) {
            etl.setEditable(makeEditable);
        }
    }

    /*
     * Is the Ensemble Tool "on"? Is it filtering and trapping incoming
     * resources? Only if the activeToolLayer is editable.
     */
    public boolean isToolEditable() {

        boolean isReady = false;
        EnsembleToolLayer etl = getActiveToolLayer();
        if ((etl != null) && (ensembleToolViewer != null)) {
            if (etl.isEditable()) {
                isReady = true;
            }
        }
        return isReady;
    }

    public boolean isDirty() {
        return !isActiveToolLayerEmpty();
    }

    /*
     * Are there resources in the given tool layer
     */
    private boolean isActiveToolLayerEmpty() {
        boolean isEmpty = true;
        EnsembleToolLayer toolLayer = getActiveToolLayer();
        if (toolLayer != null) {
            Map<String, List<GenericResourceHolder>> ensembleResources = toolLayer
                    .getEnsembleResources();
            if (ensembleResources != null) {
                isEmpty = ensembleResources.isEmpty();
            }
        }
        return isEmpty;
    }

    /*
     * Is the Ensemble Tool "on"? Is it filtering and trapping incoming
     * resources? Only if the activeToolLayer is editable.
     */
    public boolean isToolAvailable() {

        boolean isAvailable = false;
        EnsembleToolLayer etl = getActiveToolLayer();
        if ((etl != null) && (ensembleToolViewer != null)) {
            isAvailable = true;
        }
        return isAvailable;
    }

    /*
     * Is the user preference set to minimize the viewer when another tool type
     * is being loaded into the active display?
     */
    public boolean isMinimizeOnForeignToolLoadPreference() {
        boolean i = false;
        if (isToolAvailable()) {
            i = ensembleToolViewer.isMinimizeOnForeignToolLoadPreference();
        }
        return i;
    }

    /*
     * Is the user preference set to set the active ensemble tool layer to
     * editable when
     */
    public boolean isMakeEditableOnRestorePreference() {
        boolean i = false;
        if (isToolAvailable()) {
            i = ensembleToolViewer.isMakeEditableOnRestorePreference();
        }
        return i;
    }

    /*
     * Is the user preference set to set the active ensemble tool layer to
     * editable when it is being swapped-in?
     */
    public boolean isMakeEditableOnSwapInPreference() {
        boolean i = false;
        if (isToolAvailable()) {
            i = ensembleToolViewer.isMakeEditableOnSwapInPreference();
        }
        return i;

    }

    /*
     * Is the user preference set to set the active ensemble tool layer to
     * editable when it is being swapped-in?
     */
    public boolean isCreateNewToolLayerOnNewEditor() {
        boolean i = false;
        if (ensembleToolViewer != null) {
            i = ensembleToolViewer.isCreateNewToolLayerOnNewEditor();
        }
        return i;
    }

    /*
     * Did the ensemble tool renderable display customizer recognize a new
     * editor being created?
     */
    public void prepareForNewEditor() {
        ensembleToolViewer.prepareForNewToolInput();
    }

    /*
     * Is another editable tool type (i.e. not an ensemble tool) in the process
     * of loading?
     */
    public boolean isForeignEditableToolLoading() {
        return foreignEditableToolLoading;
    }

    /*
     * Did the ensemble tool renderable display customizer recognize another
     * tool type (i.e. not an ensemble tool) loading?
     */
    public void setForeignEditableToolLoading() {
        foreignEditableToolLoading = true;
    }

    /*
     * Clear any flags associated with the tool layer and view editable states.
     */
    public void clearEditableToggleFlags() {
        foreignEditableToolLoading = false;
    }

    public void setExpandedElements(List<String> expandedElems) {
        EnsembleToolLayer etl = getActiveToolLayer();
        if (etl != null) {
            etl.setExpandedElements(expandedElems);
        }
    }

    public List<String> getExpandedElements() {
        List<String> expandedElems = null;
        EnsembleToolLayer etl = getActiveToolLayer();
        if (etl != null) {
            expandedElems = etl.getExpandedElements();
        } else {
            expandedElems = new ArrayList<String>();

        }
        return expandedElems;
    }

    /*
     * Diagnostic tool/behavior - Start
     */

    public void toggleDiagnosticDialog() {

        showDiagnostic = !showDiagnostic;

        if (showDiagnostic == true) {
            diagnosticsDialog = new EnsembleToolDiagnosticStateDialog(UiUtil
                    .getCurrentWindow().getShell());
            diagnosticsDialog.open();
        } else {
            if (diagnosticsDialog != null) {
                diagnosticsDialog.close();
                diagnosticsDialog = null;
            }
        }
    }

    public void diagnosticTrace(String action) {
        EnsembleToolDiagnosticTraceMessage msg = null;
        msg = new EnsembleToolDiagnosticTraceMessage(
                Integer.toString(DiagnosticCount++), action);
        traceMessages.add(msg);
        if (diagnosticsDialog != null) {
            diagnosticsDialog.setTraceMessages(traceMessages);
        }
    }

    public void diagnosticTrace(String action, String toolLayer,
            boolean isEditable, String editor, String activeEditor,
            String method) {
        EnsembleToolDiagnosticTraceMessage msg = null;
        msg = new EnsembleToolDiagnosticTraceMessage(
                Integer.toString(DiagnosticCount++), action, toolLayer,
                isEditable, editor, activeEditor, method);
        traceMessages.add(msg);
        if (diagnosticsDialog != null) {
            diagnosticsDialog.setTraceMessages(traceMessages);
        }
    }

    protected void diagnosticToolLayerDataRefresh() {
        if (diagnosticsDialog != null) {
            diagnosticsDialog
                    .setToolLayerDataMessages(getDiagnosticToolLayerDataMessages());
        }

    }

    public void resetDiagnosticToggle() {
        showDiagnostic = false;
    }

    public List<EnsembleToolDiagnosticTraceMessage> getDiagnosticTraceMessages() {
        return traceMessages;
    }

    public List<EnsembleToolLayerDataMessage> getDiagnosticToolLayerDataMessages() {
        toolLayerDataMessages = null;
        toolLayerDataMessages = ETLMResourceDataManager.getInstance()
                .getDiagnosticToolLayerDataMessages();
        return toolLayerDataMessages;
    }

    public Point getLastKnownShellLocation() {
        return lastKnownShellLocation;
    }

    public void setLastKnownShellLocation(Point previousShellLocation) {
        lastKnownShellLocation = previousShellLocation;
    }

    public Point getLastKnownShellSize() {
        return lastKnownShellSize;
    }

    public void setLastKnownShellSize(Point size) {
        lastKnownShellSize = size;
    }

    /*
     * Diagnostic tool/behavior - End
     */

    /*
     * The resource data manager keeps track of the associated components of a
     * given tool layer including whether the tool layer is in use, the owning
     * editor, and the editor part listener. These components are stored in
     * instances of the inner class ETLMResourceDataUseState.
     */
    protected static class ETLMResourceDataManager {

        private static int TOOL_LAYER_COUNT = 0;

        public static ETLMResourceDataManager SINGLETON = null;

        public static ETLMResourceDataManager getInstance() {
            if (SINGLETON == null) {
                SINGLETON = new ETLMResourceDataManager();
            }
            return SINGLETON;
        }

        public boolean isEmpty() {
            return toolLayerMetaData.isEmpty();
        }

        public int getToolLayerCount() {
            return toolLayerMetaData.size();
        }

        protected List<ETLMResourceData> toolLayerMetaData = new ArrayList<ETLMResourceData>();

        protected ETLMResourceDataManager() {
        }

        protected void unload(EnsembleToolLayer etl) {
            ETLMResourceData foundData = null;
            for (ETLMResourceData d : toolLayerMetaData) {
                if (d.getToolLayer() == etl) {

                    foundData = d;
                    d.clear();
                    break;
                }
            }
            etl.unregisterListener((IToolLayerChanged) EnsembleTool
                    .getInstance());
            toolLayerMetaData.remove(foundData);
        }

        protected void updateToolLayerEditor(EnsembleToolLayer etl,
                IWorkbenchPart part) {

            for (ETLMResourceData d : toolLayerMetaData) {
                if (d.getToolLayer() == etl) {
                    d.workbenchPart = part;
                    break;
                }
            }
        }

        protected List<EnsembleToolLayer> getAllToolLayers() {

            List<EnsembleToolLayer> toolLayers = new ArrayList<EnsembleToolLayer>();
            for (ETLMResourceData d : toolLayerMetaData) {
                toolLayers.add(d.getToolLayer());
            }
            return toolLayers;
        }

        protected boolean hasEditor(IDisplayPaneContainer editor) {
            boolean hasEditor = false;
            for (ETLMResourceData d : toolLayerMetaData) {
                if (d.getEditor().equals(editor)) {
                    hasEditor = true;
                    break;
                }
            }
            return hasEditor;
        }

        protected boolean hasLayer(EnsembleToolLayer toolLayer) {
            boolean hasLayer = false;
            for (ETLMResourceData d : toolLayerMetaData) {
                if (toolLayer.equals(d.getToolLayer())) {
                    hasLayer = true;
                    break;
                }
            }
            return hasLayer;

        }

        protected IDisplayPaneContainer findEditor(EnsembleToolLayer tool) {

            IDisplayPaneContainer editor = null;
            for (ETLMResourceData d : toolLayerMetaData) {
                if (d.getToolLayer() == tool) {
                    editor = d.getEditor();
                    break;
                }
            }
            return editor;
        }

        protected EnsembleToolLayer findToolLayer(IDisplayPaneContainer editor) {

            EnsembleToolLayer etl = null;
            for (ETLMResourceData d : toolLayerMetaData) {
                if (d.getEditor() == editor) {
                    etl = d.getToolLayer();
                    break;
                }
            }
            return etl;
        }

        protected EnsembleToolLayer constructToolLayer(
                IDisplayPaneContainer editor, IDescriptor desc)
                throws VizException {

            GenericToolsResourceData<EnsembleToolLayer> ard = null;

            if (isEmpty()) {
                TOOL_LAYER_COUNT = 0;
            }
            String fullName = EnsembleToolLayer.DEFAULT_NAME + " "
                    + ((AbstractEditor) editor).getTitle().trim() + "-"
                    + TOOL_LAYER_COUNT++;
            ard = new GenericToolsResourceData<EnsembleToolLayer>(fullName,
                    EnsembleToolLayer.class);
            LoadProperties props = null;
            if (desc instanceof TimeSeriesDescriptor) {
                props = new LoadProperties();
                props.setResourceType(ResourceType.TIME_SERIES);
                props.setLoadWithoutData(true);
            } else {
                props = new LoadProperties();
            }
            EnsembleToolLayer tool = ard.construct(props, desc);

            tool.setInnerName(fullName);

            ETLMResourceData rdState = new ETLMResourceData(tool,
                    (IWorkbenchPart) editor);
            toolLayerMetaData.add(rdState);

            return tool;

        }

        public ArrayList<EnsembleToolLayerDataMessage> getDiagnosticToolLayerDataMessages() {
            EnsembleToolLayerDataMessage msg = null;
            ArrayList<EnsembleToolLayerDataMessage> msgs = new ArrayList<>();
            for (ETLMResourceData rd : toolLayerMetaData) {
                msg = new EnsembleToolLayerDataMessage(
                        Utilities.getReference(rd.workbenchPart),
                        rd.toolLayer.getInnerName(),
                        Utilities.getReference(null));
                msgs.add(msg);
            }
            return msgs;
        }

        private class ETLMResourceData {

            private IWorkbenchPart workbenchPart = null;

            private EnsembleToolLayer toolLayer = null;

            public IDisplayPaneContainer getEditor() {
                IDisplayPaneContainer editorPane = null;
                if (workbenchPart instanceof SideView) {
                    SideView view = (SideView) workbenchPart;
                    editorPane = view.getActiveDisplayPane()
                            .getRenderableDisplay().getContainer();
                } else if (workbenchPart instanceof AbstractEditor) {
                    AbstractEditor editor = (AbstractEditor) workbenchPart;
                    editorPane = editor.getActiveDisplayPane()
                            .getRenderableDisplay().getContainer();
                }
                return editorPane;
            }

            public ETLMResourceData(EnsembleToolLayer t, IWorkbenchPart e) {
                workbenchPart = e;
                toolLayer = t;
            }

            public EnsembleToolLayer getToolLayer() {
                return toolLayer;
            }

            public void clear() {
                workbenchPart = null;
                toolLayer = null;
            }
        }
    }

}
