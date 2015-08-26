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
import com.raytheon.viz.ui.input.EditableManager;
import com.vividsolutions.jts.geom.Coordinate;

/**
 * The EnsembleToolLayer is an AbstractVizResource that the user can turn on or
 * off (i.e. make editable or not). Typically there is one instance of this
 * class per AbstractEditor window. This class is meant to be controlled by the
 * EnsembleTool. Though this class is decoupled from the other classes in this
 * plug-in, the idea is that the EnsembleToolLayer is the class which knows how
 * to have access to its virtually contained resources and how to act upon them.
 * In the greater picture, the tool layer controls the entire state of the
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
        IInputHandler, IRefreshListener, IResourceDataChanged,
        IFrameChangedListener, IInitListener {

    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(EnsembleToolLayer.class);

    public static final String DEFAULT_NAME = "Ensembles";

    private String innerName = null;

    protected Coordinate initialDragPos = null;

    protected Coordinate continualDragPos = null;

    protected IExtent activeBoundingArea = null;

    protected boolean isClosingToolLayer = false;

    private boolean isDisposed = true;

    private boolean isEditable = true;

    private List<String> expandedElements = new ArrayList<String>();

    private TreePath[] expandedTreePaths = new TreePath[0];

    private Map<String, List<GenericResourceHolder>> ensemblesTree = null;

    private List<IToolLayerChanged> changeListeners = null;

    public EnsembleToolLayer(
            GenericToolsResourceData<EnsembleToolLayer> resourceData,
            LoadProperties loadProperties) {
        super(resourceData, loadProperties);
        registerListener((IInitListener) this);

        changeListeners = new ArrayList<IToolLayerChanged>();

    }

    synchronized public Map<String, List<GenericResourceHolder>> getEnsembleResources() {

        if (EnsembleResourceManager.getInstance().getResourceList(this) != null) {
            ensemblesTree = EnsembleResourceManager.getInstance()
                    .getResourceList(this).getAllRscsAsMap(ensemblesTree);
        } else {
            if (ensemblesTree == null) {
                ensemblesTree = new HashMap<String, List<GenericResourceHolder>>();
            }
        }

        return ensemblesTree;
    }

    public List<String> getExpandedElements() {
        return expandedElements;
    }

    public void setExpandedElements(List<String> ee) {
        expandedElements = ee;
    }

    public TreePath[] getExpandedTreePaths() {
        return expandedTreePaths;
    }

    public void setExpandedTreePaths(TreePath[] expandedTreePaths) {
        this.expandedTreePaths = expandedTreePaths;
    }

    public void unloadAllResources() {

        if (EnsembleResourceManager.getInstance().getResourceList(this) == null) {
            return;
        }
        Map<String, List<GenericResourceHolder>> allRscs = EnsembleResourceManager
                .getInstance().getResourceList(this).getAllRscsAsMap();

        List<GenericResourceHolder> currEnsembleMembers = null;
        Set<String> keySet = allRscs.keySet();
        Iterator<String> iter = keySet.iterator();
        while (iter.hasNext()) {
            String currRscListName = iter.next();
            currEnsembleMembers = allRscs.get(currRscListName);
            for (GenericResourceHolder gr : currEnsembleMembers) {
                if (gr.getRsc() instanceof TimeSeriesResource) {
                    gr.getRsc().unload(
                            getEditor().getActiveDisplayPane().getDescriptor()
                                    .getResourceList());
                    gr.getRsc().dispose();
                } else {
                    gr.getRsc().unload();
                    gr.getRsc().dispose();
                }
                EnsembleResourceManager.getInstance().unregisterResource(gr,
                        this, false);
            }
        }

        EnsembleResourceManager.getInstance().updateFrameChanges(this);
        if ((getResourceContainer() != null)
                && (getResourceContainer().getActiveDisplayPane() != null)) {
            getResourceContainer().getActiveDisplayPane().refresh();
        }

    }

    public ResourcePair getResourcePair(AbstractVizResource<?, ?> rsc) {

        ResourcePair foundRp = null;
        ResourceList rscList = null;

        if (TimeSeriesResource.class.isAssignableFrom(rsc.getClass())) {
            IDisplayPaneContainer editor = EnsembleTool.getInstance()
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

        List<GenericResourceHolder> ensembleMembers = EnsembleResourceManager
                .getInstance().getResourceList(this)
                .getUserLoadedRscs(ensembleName);
        int total = ensembleMembers.size();
        int count = 0;
        for (GenericResourceHolder gr : ensembleMembers) {
            count++;
            if (count < total) {
                EnsembleResourceManager.getInstance().unregisterResource(gr,
                        this, false);
            } else {
                EnsembleResourceManager.getInstance().unregisterResource(gr,
                        this, true);
            }
            gr.getRsc().unload();
        }

        EnsembleResourceManager.getInstance().updateFrameChanges(this);

        getResourceContainer().getActiveDisplayPane().refresh();

    }

    public void propertiesChanged(ResourceProperties updatedProps) {
        // TODO
    }

    public boolean isDisposed() {
        return isDisposed;
    }

    public IDisplayPaneContainer getEditor() {
        IDisplayPaneContainer editor = EnsembleTool.getInstance().findEditor(
                this);
        return editor;
    }

    @Override
    protected void disposeInternal() {

        descriptor.removeFrameChangedListener(this);
        resourceData.removeChangeListener((IResourceDataChanged) this);

        unloadAllResources();

        IDisplayPaneContainer container = getResourceContainer();
        if (container != null) {
            container.unregisterMouseHandler(this);
        }
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
        registerListener((IRefreshListener) this);
        resourceData.addChangeListener((IResourceDataChanged) this);
        resourceData
                .setNameGenerator(new EnsembleToolLayerNameGeneratorWithTimeStampBasis(
                        this));

    }

    @Override
    protected void paintInternal(IGraphicsTarget target,
            PaintProperties paintProps) throws VizException {
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
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean handleMouseDownMove(int x, int y, int mouseButton) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean handleMouseUp(int x, int y, int mouseButton) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean handleKeyUp(int keyCode) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean handleMouseDown(int x, int y, int mouseButton) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean handleMouseHover(int x, int y) {
        // TODO Auto-generated method stub
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

        final EnsembleToolLayer thisToolLayer = this;

        if (algorithm == Calculation.ENSEMBLE_RELATIVE_FREQUENCY) {
            Thread t = null;
            t = new Thread() {
                public void run() {

                    ERFCalculator erfCalculation = new ERFCalculator(range);
                    GeneratedDataLoader loader = new GeneratedDataLoader(
                            thisToolLayer,
                            GeneratedDataLoader.GeneratedloadMode.SAME_UNIT_AND_LEVEL);
                    loader.load(erfCalculation);

                }
            };
            t.start();
        }

    }

    public void calculate(Calculation algorithm) {

        final EnsembleToolLayer thisToolLayer = this;
        if (algorithm == Calculation.MEAN) {

            Thread t = null;

            t = new Thread() {
                public void run() {

                    // Load the mean overlay
                    MeanCalculator meanRsc = new MeanCalculator();
                    GeneratedDataLoader loader = new GeneratedDataLoader(
                            thisToolLayer,
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
                            thisToolLayer,
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
                            thisToolLayer,
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
                            thisToolLayer,
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
                            thisToolLayer,
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
                            thisToolLayer,
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
                            thisToolLayer,
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
                            thisToolLayer,
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
                            thisToolLayer,
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
                            thisToolLayer,
                            GeneratedDataLoader.GeneratedloadMode.SAME_UNIT_AND_LEVEL);
                    loader.load(p1StddevRsc);

                }
            };
            t.start();
        } else if (algorithm == Calculation.VALUE_SAMPLING) {
            Thread t = null;
            t = new Thread() {
                public void run() {

                    GeneratedDataLoader loader = new GeneratedDataLoader(
                            thisToolLayer,
                            GeneratedDataLoader.GeneratedloadMode.SAME_UNIT_AND_LEVEL);
                    loader.loadOverlay(Calculation.VALUE_SAMPLING);

                }
            };
            t.start();
        } else if (algorithm == Calculation.HISTOGRAM_SAMPLING) {

            Thread t = null;
            t = new Thread() {
                public void run() {

                    GeneratedDataLoader loader = new GeneratedDataLoader(
                            thisToolLayer,
                            GeneratedDataLoader.GeneratedloadMode.SAME_UNIT_AND_LEVEL);
                    loader.loadOverlay(Calculation.HISTOGRAM_SAMPLING);
                }
            };
            t.start();
        }
    }

    protected void setEditable(boolean makeEditable) {
        isEditable = makeEditable;
        EditableManager.makeEditable(this, makeEditable);
    }

    @Override
    public void frameChanged(IDescriptor descriptor, DataTime oldTime,
            DataTime newTime) {

        EnsembleTool.getInstance().frameChanged(descriptor.getFramesInfo());

    }

    class EnsembleToolLayerNameGeneratorWithTimeStampBasis extends
            AbstractNameGenerator {

        EnsembleToolLayer toolLayer = null;

        EnsembleToolLayerNameGeneratorWithTimeStampBasis(EnsembleToolLayer tl) {
            toolLayer = tl;
        }

        @Override
        public String getName(AbstractVizResource<?, ?> resource) {
            String timeBasis = EnsembleTool.getInstance()
                    .getTimeBasisLegendTime();
            if (NavigatorResourceList.isTimeEmpty(timeBasis)) {
                timeBasis = "";
            } else {
                timeBasis = " (" + timeBasis + ") ";
            }

            String fullName = innerName + " " + timeBasis;
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
                        EnsembleTool.getInstance().setViewerWindowState(state);
                        EnsembleTool.getInstance().clearEditableToggleFlags();
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
        return isEditable;
    }

    public String getInnerName() {
        return innerName;
    }

    public void setInnerName(String innerName) {
        this.innerName = innerName;
    }

    /**
     * Allow the caller to register for tool editability state change events.
     * 
     * @param listener
     *            the dispose listener
     */
    public final void registerListener(IToolLayerChanged listener) {
        changeListeners.add(listener);
    }

    public final void unregisterListener(IToolLayerChanged listener) {
        changeListeners.remove(listener);
    }

    public boolean hasListener(IToolLayerChanged listener) {
        return changeListeners.contains(listener);
    }

    @Override
    public void resourceChanged(ChangeType type, Object object) {

        if (type == ChangeType.CAPABILITY
                && object instanceof EditableCapability) {
            EditableCapability editability = (EditableCapability) object;
            isEditable = editability.isEditable();
            // issueRefresh();
        }
        for (IToolLayerChanged etl : changeListeners) {
            etl.resourceChanged(this, type, object);
        }
    }

}
