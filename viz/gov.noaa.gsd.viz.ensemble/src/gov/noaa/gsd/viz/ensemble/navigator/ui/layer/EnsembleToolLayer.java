package gov.noaa.gsd.viz.ensemble.navigator.ui.layer;

import gov.noaa.gsd.viz.ensemble.display.calculate.AvgM1StddevCalculator;
import gov.noaa.gsd.viz.ensemble.display.calculate.AvgP1StddevCalculator;
import gov.noaa.gsd.viz.ensemble.display.calculate.Calculation;
import gov.noaa.gsd.viz.ensemble.display.calculate.ERFCalculator;
import gov.noaa.gsd.viz.ensemble.display.calculate.MaxCalculator;
import gov.noaa.gsd.viz.ensemble.display.calculate.MeanCalculator;
import gov.noaa.gsd.viz.ensemble.display.calculate.MedianCalculator;
import gov.noaa.gsd.viz.ensemble.display.calculate.MinCalculator;
import gov.noaa.gsd.viz.ensemble.display.calculate.ModeCalculator;
import gov.noaa.gsd.viz.ensemble.display.calculate.Range;
import gov.noaa.gsd.viz.ensemble.display.calculate.RangeCalculator;
import gov.noaa.gsd.viz.ensemble.display.calculate.StddevCalculator;
import gov.noaa.gsd.viz.ensemble.display.calculate.SumCalculator;
import gov.noaa.gsd.viz.ensemble.display.common.GenericResourceHolder;
import gov.noaa.gsd.viz.ensemble.display.common.NavigatorResourceList;
import gov.noaa.gsd.viz.ensemble.display.control.EnsembleResourceManager;
import gov.noaa.gsd.viz.ensemble.display.control.load.GeneratedDataLoader;
import gov.noaa.gsd.viz.ensemble.navigator.ui.viewer.ViewerWindowState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.viz.core.IDisplayPaneContainer;
import com.raytheon.uf.viz.core.IExtent;
import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.VizApp;
import com.raytheon.uf.viz.core.drawables.AbstractDescriptor;
import com.raytheon.uf.viz.core.drawables.IDescriptor;
import com.raytheon.uf.viz.core.drawables.IDescriptor.IFrameChangedListener;
import com.raytheon.uf.viz.core.drawables.PaintProperties;
import com.raytheon.uf.viz.core.drawables.ResourcePair;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.rsc.AbstractNameGenerator;
import com.raytheon.uf.viz.core.rsc.AbstractResourceData;
import com.raytheon.uf.viz.core.rsc.AbstractVizResource;
import com.raytheon.uf.viz.core.rsc.IInitListener;
import com.raytheon.uf.viz.core.rsc.IInputHandler;
import com.raytheon.uf.viz.core.rsc.IRefreshListener;
import com.raytheon.uf.viz.core.rsc.IResourceDataChanged;
import com.raytheon.uf.viz.core.rsc.LoadProperties;
import com.raytheon.uf.viz.core.rsc.RenderingOrderFactory.ResourceOrder;
import com.raytheon.uf.viz.core.rsc.ResourceList;
import com.raytheon.uf.viz.core.rsc.ResourceProperties;
import com.raytheon.uf.viz.core.rsc.capabilities.EditableCapability;
import com.raytheon.uf.viz.core.rsc.tools.GenericToolsResourceData;
import com.raytheon.uf.viz.xy.timeseries.rsc.TimeSeriesResource;
import com.raytheon.viz.ui.EditorUtil;
import com.raytheon.viz.ui.editor.AbstractEditor;
import com.raytheon.viz.ui.input.EditableManager;
import com.vividsolutions.jts.geom.Coordinate;

/**
 * The EnsembleToolLayer is an AbstractVizResource that the user can turn on or
 * off (i.e. make editable or not). Typically there is one instance of this
 * class per AbstractEditor window. This class is meant to be controlled by the
 * EnsembleToolManager. Though this class is decoupled from the other classes in
 * this plug-in, the idea is that the EnsembleToolLayer is the class which knows
 * how to have access to its virtually contained resources and how to act upon
 * them. In the greater picture, the tool layer controls the entire state of the
 * Ensemble Tool; if the activel tool layer is turned off (made "not editable")
 * then the Ensemble Tool is also disabled, but if the user switches to another
 * editor that has an EnsembleToolLayer that is "editable", the manager will
 * become active again.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Nov 16, 2014    5056    polster     Initial creation
 * 
 * </pre>
 * 
 * @author polster
 * @version 1.0
 */
public class EnsembleToolLayer extends
        AbstractVizResource<AbstractResourceData, AbstractDescriptor> implements
        IInputHandler, IResourceDataChanged, IRefreshListener,
        IFrameChangedListener, IInitListener {

    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(EnsembleToolLayer.class);

    public static final String DEFAULT_NAME = "Ensembles";

    protected Coordinate initialDragPos = null;

    protected Coordinate continualDragPos = null;

    protected IExtent activeBoundingArea = null;

    protected boolean isClosingToolLayer = false;

    private boolean isDisposed = true;

    private boolean userRequestedEditableToggleViaLegend = false;

    private boolean userRequestedNewEditableTool = false;

    private boolean viewStateAlreadyModified = false;

    private List<String> expandedElements = new ArrayList<String>();

    private TreePath[] expandedTreePaths = new TreePath[0];

    private Map<String, List<GenericResourceHolder>> ensemblesTree = new HashMap<String, List<GenericResourceHolder>>();

    public EnsembleToolLayer(
            GenericToolsResourceData<EnsembleToolLayer> resourceData,
            LoadProperties loadProperties) {
        super(resourceData, loadProperties);
        registerListener((IInitListener) this);

        resourceData
                .setNameGenerator(new EnsembleToolLayerNameGeneratorWithTimeStampBasis());

    }

    synchronized public Map<String, List<GenericResourceHolder>> getEnsembleResources() {

        AbstractEditor editor = EnsembleToolManager.getInstance().findEditor(
                this);

        if (EnsembleResourceManager.getInstance().getResourceList(editor) != null) {
            ensemblesTree = EnsembleResourceManager.getInstance()
                    .getResourceList(editor).getAllRscsAsMap(ensemblesTree);
        } else {
            ensemblesTree = null;
        }

        return ensemblesTree;
    }

    public synchronized List<String> getExpandedElements() {
        return expandedElements;
    }

    public synchronized void setExpandedElements(List<String> ee) {
        expandedElements = ee;
    }

    public TreePath[] getExpandedTreePaths() {
        return expandedTreePaths;
    }

    public void setExpandedTreePaths(TreePath[] expandedTreePaths) {
        this.expandedTreePaths = expandedTreePaths;
    }

    public void unloadAllResources(AbstractEditor editor) {

        if (EnsembleResourceManager.getInstance().getResourceList(editor) == null)
            return;
        Map<String, List<GenericResourceHolder>> allRscs = EnsembleResourceManager
                .getInstance().getResourceList(editor).getAllRscsAsMap();

        List<GenericResourceHolder> currEnsembleMembers = null;
        Set<String> keySet = allRscs.keySet();
        Iterator<String> iter = keySet.iterator();
        while (iter.hasNext()) {
            String currRscListName = iter.next();
            currEnsembleMembers = allRscs.get(currRscListName);
            for (GenericResourceHolder gr : currEnsembleMembers) {
                if (gr.getRsc() instanceof TimeSeriesResource) {
                    gr.getRsc().unload(
                            editor.getActiveDisplayPane().getDescriptor()
                                    .getResourceList());
                    gr.getRsc().dispose();
                } else {
                    gr.getRsc().unload();
                    gr.getRsc().dispose();
                }
                EnsembleResourceManager.getInstance().unregisterResource(gr,
                        editor, false);
            }
        }

        EnsembleResourceManager.getInstance().updateFrameChanges(editor);
        if ((getResourceContainer() != null)
                && (getResourceContainer().getActiveDisplayPane() != null)) {
            getResourceContainer().getActiveDisplayPane().refresh();
        }

    }

    public ResourcePair getResourcePair(AbstractVizResource<?, ?> rsc) {

        ResourcePair foundRp = null;
        ResourceList rscList = null;

        if (TimeSeriesResource.class.isAssignableFrom(rsc.getClass())) {
            AbstractEditor editor = EnsembleToolManager.getInstance()
                    .findEditor(this);
            rscList = editor.getActiveDisplayPane().getDescriptor()
                    .getResourceList();
        } else {
            rscList = getResourceContainer().getActiveDisplayPane()
                    .getDescriptor().getResourceList();
        }
        for (ResourcePair rp : rscList) {
            if (rsc.equals(rp.getResource())) {
                foundRp = rp;
                break;
            }
        }

        return foundRp;

    }

    public void unloadAllResourcesByName(String ensembleName) {

        AbstractEditor editor = EnsembleToolManager.getInstance().findEditor(
                this);
        List<GenericResourceHolder> ensembleMembers = EnsembleResourceManager
                .getInstance().getResourceList(editor)
                .getUserLoadedRscs(ensembleName);
        int total = ensembleMembers.size();
        int count = 0;
        for (GenericResourceHolder gr : ensembleMembers) {
            count++;
            if (count < total) {
                EnsembleResourceManager.getInstance().unregisterResource(gr,
                        editor, false);
            } else {
                EnsembleResourceManager.getInstance().unregisterResource(gr,
                        editor, true);
            }
            gr.getRsc().unload();
        }

        EnsembleResourceManager.getInstance().updateFrameChanges(editor);

        getResourceContainer().getActiveDisplayPane().refresh();

    }

    public void propertiesChanged(ResourceProperties updatedProps) {
        // TODO
    }

    public boolean isDisposed() {
        return isDisposed;
    }

    @Override
    protected void disposeInternal() {

        descriptor.removeFrameChangedListener(this);

        AbstractEditor editor = EnsembleToolManager.getInstance().findEditor(
                this);
        unloadAllResources(editor);
        EnsembleToolManager.getInstance().removeToolLayer(this);

        IDisplayPaneContainer container = getResourceContainer();
        if (container != null) {
            container.unregisterMouseHandler(this);
        }

        // TODO: Investigate if this updateFrameChanges call is necessary.
        // EnsembleResourceManager.getInstance().updateFrameChanges(editor);
        isDisposed = true;

    }

    @Override
    protected void initInternal(IGraphicsTarget target) throws VizException {
        isDisposed = false;
        descriptor.getResourceList().getProperties(this)
                .setRenderingOrderId("HIGHEST");

        IDisplayPaneContainer container = getResourceContainer();
        if (container != null) {
            container.registerMouseHandler(this);
        }

    }

    @Override
    public void inited(AbstractVizResource<?, ?> initedRsc) {

        descriptor.addFrameChangedListener(this);
        resourceData.addChangeListener(this);
        registerListener((IRefreshListener) this);

    }

    @Override
    protected void paintInternal(IGraphicsTarget target,
            PaintProperties paintProps) throws VizException {

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.viz.core.rsc.IResourceDataChanged#resourceChanged(com
     * .raytheon.uf.viz.core.rsc.IResourceDataChanged.ChangeType,
     * java.lang.Object)
     * 
     * The sole purpose of this method is to minimize or restore the ensemble
     * tool viewer (ViewPart) when its editability is turned off or on,
     * respectively (if it is not already done).
     */
    @Override
    public void resourceChanged(ChangeType type, Object object) {
        if (type == ChangeType.CAPABILITY
                && object instanceof EditableCapability) {

            EditableCapability editable = (EditableCapability) object;

            /* *
             * The editable state of an instance of the active ensemble tool
             * layer (i.e. this class) can be changed in one of four ways: the
             * first is by minimizing or restoring the ensemble tool view, the
             * second by swapping a display (that contains this instance) into
             * or out of the active editor, the third by right-clicking on this
             * (viz resource's) legend and toggling editability from there, or
             * the fourth by opening another tool which causes the
             * EditableManager to turn off any other tool layer that is
             * editable.
             * 
             * In the first case, when the ensemble viewer (part listener)
             * listens for a 'minimize' or 'restore' of the ensemble tool view
             * the view then gets automatically minimized or restored and the
             * active tool layer is set to 'not editable' or to 'editable',
             * respectively; this method intentionally ignores this first case
             * as the view is already automatically minimized or restored.
             * 
             * In the second case, the EnsembleToolManager listens for the
             * IRenderableDisplayChangedListener.renderableDisplayChanged event
             * which occurs when a swap happens. When a display with an ensemble
             * tool layer is swapped in or out, the EnsembleToolManager restores
             * or minimizes the viewer and, in turn, the viewer's part listener
             * sets this tool layer to be 'editable' or 'not editable',
             * respectively. This method intentionally ignores this second case
             * as the view is already automatically minimized or restored.
             * 
             * In the third case, the user changes the editable state via the
             * legend pop-up menu (right-clicking on a legend). This class
             * currently listens for a right-click mouse event and sets a flag.
             * If the flag is true then the ensemble tool view is restored. If
             * the flag is false then the view is minimized.
             * 
             * In the fourth case, the user has opened another editable tool
             * layer which will make this layer 'not editable'. This method will
             * then minimize the view.
             * 
             * The "viewer state change" is run in an asynchronous Job due to a
             * bug in Eclipse. If this is not done this way a user's request
             * (e.g. to minimize the view) can cause a race condition between
             * manually and programmatically setting the state. The problem is
             * when the part listener is told of the view becoming hidden before
             * the ViewPart view state is actually set to minimized.
             */
            if ((userRequestedEditableToggleViaLegend == true)
                    || (userRequestedNewEditableTool == true)) {
                if (editable.isEditable() == false) {
                    SetViewerStateJob ccj = new SetViewerStateJob(
                            "Minimize Tool View");
                    ccj.setViewerWindowState(ViewerWindowState.MINIMIZED);
                    ccj.setPriority(Job.SHORT);
                    ccj.schedule();
                } else {
                    SetViewerStateJob ccj = new SetViewerStateJob(
                            "Restore Tool View");
                    ccj.setViewerWindowState(ViewerWindowState.SHOW_WITHOUT_FOCUS);
                    ccj.setPriority(Job.SHORT);
                    ccj.schedule();
                }
            }
            /*
             * This condition handles when the ensemble tool layer is not
             * editable and therefore would not be able to listen for the
             * right-click mouse event so would not be able to set the
             * respective flag.
             * 
             * Don't change the view state if it has already been done.
             */
            else if (viewStateAlreadyModified == false) {
                if (editable.isEditable() == true) {
                    SetViewerStateJob ccj = new SetViewerStateJob(
                            "Restore Tool View");
                    ccj.setViewerWindowState(ViewerWindowState.SHOW_WITHOUT_FOCUS);
                    ccj.setPriority(Job.SHORT);
                    ccj.schedule();
                } else {
                    SetViewerStateJob ccj = new SetViewerStateJob(
                            "Minimize Tool View");
                    ccj.setViewerWindowState(ViewerWindowState.MINIMIZED);
                    ccj.setPriority(Job.SHORT);
                    ccj.schedule();
                }
            }
        }
        issueRefresh();
    }

    public void transferFocusToEditor() {

        // Get the active window
        IWorkbenchWindow window = PlatformUI.getWorkbench()
                .getActiveWorkbenchWindow();
        if (window == null) {
            return;
        }

        IEditorPart editor = window.getActivePage().getActiveEditor();
        if (editor != null) {
            editor.setFocus();
        }
    }

    @Override
    public void refresh() {
        // TODO
    }

    @Override
    public boolean handleMouseMove(int x, int y) {
        boolean handledMouseAction = false;
        // TODO
        return handledMouseAction;
    }

    @Override
    public boolean handleMouseDownMove(int x, int y, int mouseButton) {
        boolean handledMouseAction = false;
        // TODO
        return handledMouseAction;
    }

    @Override
    public boolean handleMouseUp(int x, int y, int mouseButton) {
        boolean alreadyHandled = false;
        // TODO
        return alreadyHandled;
    }

    @Override
    public boolean handleKeyUp(int keyCode) {

        boolean handledKeyAction = false;
        // TODO
        return handledKeyAction;
    }

    @Override
    public boolean handleMouseDown(int x, int y, int mouseButton) {
        if (mouseButton == 3) {
            /*
             * TODO: need a better solution to know of the user has requested an
             * action from right-clicking on the the legend.
             */
            userRequestedEditableToggleViaLegend = true;
        }

        return false;
    }

    @Override
    public boolean handleMouseHover(int x, int y) {
        // TODO
        return false;
    }

    @Override
    public boolean handleDoubleClick(int x, int y, int button) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean handleMouseWheel(Event event, int x, int y) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean handleMouseExit(Event event) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean handleMouseEnter(Event event) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean handleKeyDown(int keyCode) {
        // TODO Auto-generated method stub
        return false;
    }

    public void calculate(Calculation algorithm, final Range range) {

        final AbstractEditor editor = EnsembleToolManager.getInstance()
                .findEditor(this);
        if (algorithm == Calculation.ENSEMBLE_RELATIVE_FREQUENCY) {
            Thread t = null;
            t = new Thread() {
                public void run() {

                    ERFCalculator erfRsc = new ERFCalculator(range);
                    GeneratedDataLoader loader = new GeneratedDataLoader(
                            editor,
                            GeneratedDataLoader.GeneratedloadMode.SAME_UNIT_AND_LEVEL);
                    loader.load(erfRsc);

                }
            };
            t.start();
        }

    }

    public void calculate(Calculation algorithm) {

        final AbstractEditor editor = EnsembleToolManager.getInstance()
                .findEditor(this);
        if (algorithm == Calculation.MEAN) {

            Thread t = null;

            t = new Thread() {
                public void run() {

                    // Load the mean overlay
                    MeanCalculator meanRsc = new MeanCalculator();
                    GeneratedDataLoader loader = new GeneratedDataLoader(
                            editor,
                            GeneratedDataLoader.GeneratedloadMode.SAME_UNIT_AND_LEVEL);
                    loader.load(meanRsc);

                }
            };
            t.start();
        } else if (algorithm == Calculation.MIN) {

            Thread t = null;

            t = new Thread() {
                public void run() {

                    // Load the min overlay
                    MinCalculator minRsc = new MinCalculator();
                    GeneratedDataLoader loader = new GeneratedDataLoader(
                            editor,
                            GeneratedDataLoader.GeneratedloadMode.SAME_UNIT_AND_LEVEL);
                    loader.load(minRsc);

                }
            };
            t.start();
        } else if (algorithm == Calculation.MAX) {

            Thread t = null;

            t = new Thread() {
                public void run() {

                    // Load the max overlay
                    MaxCalculator maxRsc = new MaxCalculator();
                    GeneratedDataLoader loader = new GeneratedDataLoader(
                            editor,
                            GeneratedDataLoader.GeneratedloadMode.SAME_UNIT_AND_LEVEL);
                    loader.load(maxRsc);

                }
            };
            t.start();
        } else if (algorithm == Calculation.MEDIAN) {

            Thread t = null;
            t = new Thread() {
                public void run() {

                    // Load the median overlay
                    MedianCalculator medianRsc = new MedianCalculator();
                    GeneratedDataLoader loader = new GeneratedDataLoader(
                            editor,
                            GeneratedDataLoader.GeneratedloadMode.SAME_UNIT_AND_LEVEL);
                    loader.load(medianRsc);

                }
            };
            t.start();
        } else if (algorithm == Calculation.MODE) {

            Thread t = null;
            t = new Thread() {
                public void run() {

                    // Load the median overlay
                    ModeCalculator modeRsc = new ModeCalculator();
                    GeneratedDataLoader loader = new GeneratedDataLoader(
                            editor,
                            GeneratedDataLoader.GeneratedloadMode.SAME_UNIT_AND_LEVEL);
                    loader.load(modeRsc);

                }
            };
            t.start();
        } else if (algorithm == Calculation.RANGE) {

            Thread t = null;
            t = new Thread() {
                public void run() {

                    // Load the range overlay
                    RangeCalculator rangeRsc = new RangeCalculator();
                    GeneratedDataLoader loader = new GeneratedDataLoader(
                            editor,
                            GeneratedDataLoader.GeneratedloadMode.SAME_UNIT_AND_LEVEL);
                    loader.load(rangeRsc);

                }
            };
            t.start();
        } else if (algorithm == Calculation.SUMMATION) {

            Thread t = null;
            t = new Thread() {
                public void run() {

                    // Load the sum overlay
                    SumCalculator sumRsc = new SumCalculator();
                    GeneratedDataLoader loader = new GeneratedDataLoader(
                            editor,
                            GeneratedDataLoader.GeneratedloadMode.SAME_UNIT_AND_LEVEL);
                    loader.load(sumRsc);

                }
            };
            t.start();
        } else if (algorithm == Calculation.STANDARD_DEVIATION) {

            Thread t = null;
            t = new Thread() {
                public void run() {

                    // Load the sum overlay
                    StddevCalculator stddevRsc = new StddevCalculator();
                    GeneratedDataLoader loader = new GeneratedDataLoader(
                            editor,
                            GeneratedDataLoader.GeneratedloadMode.SAME_UNIT_AND_LEVEL);
                    loader.load(stddevRsc);

                }
            };
            t.start();
        } else if (algorithm == Calculation.AVG_MINUS_STD_DEV) {

            Thread t = null;
            t = new Thread() {
                public void run() {

                    // Load the sum overlay
                    AvgM1StddevCalculator m1StddevRsc = new AvgM1StddevCalculator();
                    GeneratedDataLoader loader = new GeneratedDataLoader(
                            editor,
                            GeneratedDataLoader.GeneratedloadMode.SAME_UNIT_AND_LEVEL);
                    loader.load(m1StddevRsc);

                }
            };
            t.start();
        } else if (algorithm == Calculation.AVG_PLUS_STD_DEV) {

            Thread t = null;
            t = new Thread() {
                public void run() {

                    // Load the sum overlay
                    AvgP1StddevCalculator p1StddevRsc = new AvgP1StddevCalculator();
                    GeneratedDataLoader loader = new GeneratedDataLoader(
                            editor,
                            GeneratedDataLoader.GeneratedloadMode.SAME_UNIT_AND_LEVEL);
                    loader.load(p1StddevRsc);

                }
            };
            t.start();
        } else if (algorithm == Calculation.HISTOGRAM_SAMPLING) {
            Thread t = null;
            t = new Thread() {
                public void run() {

                    GeneratedDataLoader loader = new GeneratedDataLoader(
                            editor,
                            GeneratedDataLoader.GeneratedloadMode.SAME_UNIT_AND_LEVEL);
                    loader.loadOverlay(Calculation.HISTOGRAM_SAMPLING);

                }
            };
            t.start();
        } else if (algorithm == Calculation.HISTOGRAM_TEXT) {

            Thread t = null;
            t = new Thread() {
                public void run() {

                    GeneratedDataLoader loader = new GeneratedDataLoader(
                            editor,
                            GeneratedDataLoader.GeneratedloadMode.SAME_UNIT_AND_LEVEL);
                    loader.loadOverlay(Calculation.HISTOGRAM_TEXT);
                }
            };
            t.start();
        }
    }

    protected void setEditable(boolean makeEditable) {
        EditableManager.makeEditable(this, makeEditable);
    }

    @Override
    public void frameChanged(IDescriptor descriptor, DataTime oldTime,
            DataTime newTime) {

        EnsembleToolManager.getInstance().updateLegendTimeInfo();

    }

    class EnsembleToolLayerNameGeneratorWithTimeStampBasis extends
            AbstractNameGenerator {

        @Override
        public String getName(AbstractVizResource<?, ?> resource) {
            String timeBasis = EnsembleToolManager.getInstance()
                    .getTimeBasisLegendTime();
            if (NavigatorResourceList.isTimeEmpty(timeBasis)) {
                timeBasis = "";
            } else {
                timeBasis = " (" + timeBasis + ") ";
            }
            AbstractEditor editor = (AbstractEditor) EditorUtil
                    .getActiveEditor();
            String fullName = EnsembleToolLayer.DEFAULT_NAME + " "
                    + editor.getTitle().trim() + timeBasis;
            return fullName;
        }

    }

    @Override
    public ResourceOrder getResourceOrder() {
        return ResourceOrder.HIGHEST;
    }

    /*
     * This Job allows the caller to set the viewer state (minimized, restored,
     * etc) asynchronously.
     */
    private class SetViewerStateJob extends Job {

        public SetViewerStateJob(String name) {
            super(name);
        }

        private ViewerWindowState state = ViewerWindowState.UNDEFINED;

        private void setViewerWindowState(ViewerWindowState s) {
            state = s;
        }

        @Override
        protected IStatus run(IProgressMonitor monitor) {
            IStatus status = null;

            if (state == ViewerWindowState.UNDEFINED) {
                status = Status.CANCEL_STATUS;
            } else {
                status = Status.OK_STATUS;
                VizApp.runAsync(new Runnable() {

                    @Override
                    public void run() {
                        EnsembleToolManager.getInstance().setViewerWindowState(
                                state);
                        clearEditableToggleFlags();
                    }
                });
            }

            return status;
        }
    }

    /*
     * All ensemble tool classes shold make certain they check to see whether
     * the tool layer is editable before interacting with the user.
     */
    public boolean isEditable() {

        boolean isEditable = false;
        if (!isDisposed) {
            isEditable = getCapability(EditableCapability.class).isEditable();
        }
        return isEditable;
    }

    public void setEditabilityChangeInProcess() {
        userRequestedNewEditableTool = true;
    }

    public void clearEditableToggleFlags() {
        userRequestedEditableToggleViaLegend = false;
        userRequestedNewEditableTool = false;
        viewStateAlreadyModified = false;
    }

    public void setViewStateAlreadyModified() {
        viewStateAlreadyModified = true;
    }

}
