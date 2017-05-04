package gov.noaa.gsd.viz.ensemble.navigator.ui.layer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.viz.core.IExtent;
import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.drawables.AbstractDescriptor;
import com.raytheon.uf.viz.core.drawables.IDescriptor;
import com.raytheon.uf.viz.core.drawables.IDescriptor.IFrameChangedListener;
import com.raytheon.uf.viz.core.drawables.PaintProperties;
import com.raytheon.uf.viz.core.drawables.ResourcePair;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.rsc.AbstractNameGenerator;
import com.raytheon.uf.viz.core.rsc.AbstractRequestableResourceData;
import com.raytheon.uf.viz.core.rsc.AbstractResourceData;
import com.raytheon.uf.viz.core.rsc.AbstractVizResource;
import com.raytheon.uf.viz.core.rsc.DisplayType;
import com.raytheon.uf.viz.core.rsc.IInitListener;
import com.raytheon.uf.viz.core.rsc.IRefreshListener;
import com.raytheon.uf.viz.core.rsc.IResourceDataChanged;
import com.raytheon.uf.viz.core.rsc.LoadProperties;
import com.raytheon.uf.viz.core.rsc.RenderingOrderFactory.ResourceOrder;
import com.raytheon.uf.viz.core.rsc.ResourceList;
import com.raytheon.uf.viz.core.rsc.ResourceProperties;
import com.raytheon.uf.viz.core.rsc.capabilities.EditableCapability;
import com.raytheon.uf.viz.core.rsc.tools.GenericToolsResourceData;
import com.raytheon.uf.viz.xy.timeseries.rsc.TimeSeriesResource;
import com.raytheon.viz.core.rsc.BestResResource;
import com.raytheon.viz.grid.rsc.general.D2DGridResource;
import com.raytheon.viz.grid.rsc.general.GridResource;
import com.raytheon.viz.ui.input.EditableManager;
import com.vividsolutions.jts.geom.Coordinate;

import gov.noaa.gsd.viz.ensemble.control.EnsembleResourceManager;
import gov.noaa.gsd.viz.ensemble.control.EnsembleTool;
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
import gov.noaa.gsd.viz.ensemble.display.common.AbstractResourceHolder;
import gov.noaa.gsd.viz.ensemble.display.control.load.GeneratedDataLoader;
import gov.noaa.gsd.viz.ensemble.display.rsc.GeneratedEnsembleGridResource;
import gov.noaa.gsd.viz.ensemble.display.rsc.histogram.HistogramResource;
import gov.noaa.gsd.viz.ensemble.display.rsc.timeseries.GeneratedTimeSeriesResource;
import gov.noaa.gsd.viz.ensemble.navigator.ui.viewer.matrix.FieldPlanePair;
import gov.noaa.gsd.viz.ensemble.navigator.ui.viewer.matrix.ModelSources;
import gov.noaa.gsd.viz.ensemble.util.RequestableResourceMetadata;

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
 * This class is constructed to be associated with either the Matrix or Legends
 * mode of the Ensemble Tool. (TODO: The better way of doing this would be to
 * make this EnsembleToolLayer class, instead, abstract, and derive both a
 * LegendsBrowserToolLayer and MatrixNavigatorToolLayer from this class. We
 * chose against this direction, for now, because creating different flavors of
 * base class from the AbstractResourceData's construct method seemed not easily
 * do-able.)
 * 
 * This class is either a legend-browser mode tool layer, or a matrix-navigator
 * mode tool layer. The method <code>isResourceCompatible</code> defines what
 * types of resources the tool mode accepts.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#      Engineer   Description
 * ------------ ---------- ----------- --------------------------
 * Nov 16, 2014    5056      polster    Initial creation
 * Nov 13, 2015   13211      polster    Changes in support of Matrix vs Legend
 * Jan 15, 2016   12301      jing       Added distribution feature
 * Nov 12, 2016   19443      polster    Clean up dispose process
 * Dec 29, 2016   19325      jing       Process image items in calculation menu
 * Feb 17, 2017   19325      jing       Added ERF image capability
 * Mar 01, 2017   19443    polster      Fix clear and close behavior
 * 
 * </pre>
 * 
 * @author polster
 * @version 1.0
 */
public class EnsembleToolLayer
        extends AbstractVizResource<AbstractResourceData, AbstractDescriptor>
        implements IRefreshListener, IResourceDataChanged,
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

    private Map<String, List<AbstractResourceHolder>> ensemblesTree = null;

    private List<IToolLayerChanged> changeListeners = null;

    private EnsembleTool.EnsembleToolMode toolMode = EnsembleTool.EnsembleToolMode.LEGENDS_PLAN_VIEW;

    // private IDisplayPaneContainer thisToolLayersContainer = null;

    public EnsembleToolLayer(
            GenericToolsResourceData<EnsembleToolLayer> resourceData,
            LoadProperties loadProperties) {
        super(resourceData, loadProperties);
        ensemblesTree = new ConcurrentHashMap<>();
        registerListener((IInitListener) this);

        changeListeners = new ArrayList<>();

    }

    public EnsembleTool.EnsembleToolMode getToolMode() {
        return toolMode;
    }

    public void setToolMode(EnsembleTool.EnsembleToolMode toolMode) {
        this.toolMode = toolMode;
    }

    public boolean isEmpty() {
        boolean isEmpty = false;
        if (EnsembleResourceManager.getInstance().isEmpty(this)) {
            isEmpty = true;
        }
        return isEmpty;
    }

    synchronized public Map<String, List<AbstractResourceHolder>> getEnsembleResources() {

        if (EnsembleResourceManager.getInstance()
                .getResourceList(this) != null) {
            ensemblesTree = EnsembleResourceManager.getInstance()
                    .getResourceList(this).getAllRscsAsMap(ensemblesTree);
        } else {
            if (ensemblesTree == null) {
                ensemblesTree = new ConcurrentHashMap<>();
            }
        }

        return ensemblesTree;
    }

    /**
     * Is the tool layer is supposed to keep track of the resource inside this
     * resource pair?
     * 
     * TODO: This method contains selection based on the state of the tool layer
     * mode (Legend vs Matrix). The better way of doing this would be to make
     * the EnsembleToolLayer class abstract, and derive both a
     * LegendsBrowserToolLayer and MatrixNavigatorToolLayer from this class. We
     * chose against this direction (for now) because creating different flavors
     * of base class from the AbstractResourceData's construct method seemed not
     * easily do-able.
     */
    public boolean isResourceCompatible(ResourcePair rp) {
        boolean isCompatible = false;

        /* ignore null resource pair and the tool layer itself */
        if (rp == null || rp.getResource() instanceof EnsembleToolLayer) {
            isCompatible = false;
        } else {
            /* ignore a null resource */
            AbstractVizResource<?, ?> resource = rp.getResource();
            if (resource == null) {
                isCompatible = false;
            } else {
                if (toolMode == EnsembleTool.EnsembleToolMode.LEGENDS_PLAN_VIEW
                        || toolMode == EnsembleTool.EnsembleToolMode.LEGENDS_TIME_SERIES) {

                    /* ignore the image resources in legend mode */
                    if (resource instanceof D2DGridResource) {
                        D2DGridResource gr = (D2DGridResource) resource;
                        if (gr.getDisplayType() == DisplayType.IMAGE) {
                            isCompatible = false;
                        }
                    }

                    /*
                     * Ignore an ensemble-tool generated resource.
                     * 
                     * Otherwise, if resource is gridded or time series data
                     * then the resource is compatible.
                     */
                    if (!isGeneratedResource(rp)
                            && ((resource instanceof GridResource)
                                    || (resource instanceof TimeSeriesResource))) {
                        isCompatible = true;
                    }

                } else if (toolMode == EnsembleTool.EnsembleToolMode.MATRIX) {
                    // if (resource instanceof GridResource
                    // || resource instanceof BestResResource) {
                    isCompatible = true;
                    // }
                }
            }
        }

        return isCompatible;

    }

    /**
     * Is this an ensemble tool generated resource?
     */
    private boolean isGeneratedResource(ResourcePair rp) {
        if (rp.getResource() instanceof HistogramResource
                || rp.getResource() instanceof GeneratedEnsembleGridResource
                || rp.getResource() instanceof GeneratedTimeSeriesResource) {
            return true;
        }

        return false;
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

        if (EnsembleResourceManager.getInstance()
                .getResourceList(this) == null) {
            return;
        }
        Map<String, List<AbstractResourceHolder>> allRscs = getEnsembleResources();

        List<AbstractResourceHolder> currEnsembleMembers = null;
        Set<String> keySet = allRscs.keySet();
        Iterator<String> iter = keySet.iterator();
        while (iter.hasNext()) {
            String currRscListName = iter.next();
            currEnsembleMembers = allRscs.get(currRscListName);
            for (AbstractResourceHolder gr : currEnsembleMembers) {
                AbstractVizResource<?, ?> rsc = gr.getRsc();
                if (rsc instanceof TimeSeriesResource) {
                    rsc.unload(getDescriptor().getResourceList());
                    rsc.dispose();
                } else {
                    rsc.unload();
                    rsc.dispose();
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

    public ResourcePair getResourcePair(FieldPlanePair e,
            ModelSources modelSrc) {

        ResourcePair foundRp = null;
        ResourceList rscList = getResourceContainer().getActiveDisplayPane()
                .getDescriptor().getResourceList();
        String fieldAbbrev = null;
        String plane = null;
        for (ResourcePair rp : rscList) {
            if (rp != null && rp.getResourceData() != null
                    && rp.getResource() != null) {
                /**
                 * TODO: Get the gest resolution resource and return it with the
                 * resource pair.
                 */
                // if (rp.getResource() instanceof BestResResource) {
                // BestResResource brr = (BestResResource) rp.getResource();
                // brr.getBestResResource(time);
                // } else
                if (rp.getResourceData() instanceof AbstractRequestableResourceData) {
                    AbstractRequestableResourceData ard = (AbstractRequestableResourceData) rp
                            .getResourceData();
                    RequestableResourceMetadata rrmd = new RequestableResourceMetadata(
                            ard);
                    fieldAbbrev = rrmd.getFieldAbbrev();
                    plane = rrmd.getPlane();
                    String rscName = rp.getResource().getName();
                    String modelName = modelSrc.getModelName();
                    if (rscName.startsWith(modelName)) {
                        if (e.getFieldAbbrev().equals(fieldAbbrev)
                                && e.getPlane().equals(plane)) {
                            foundRp = rp;
                            break;
                        }
                    }
                }
            }
        }

        return foundRp;

    }

    public ResourcePair getResourcePair(AbstractVizResource<?, ?> rsc) {

        ResourcePair foundRp = null;
        ResourceList rscList = null;

        /**
         * TODO: Need to make the context menu work for BestResResource.
         */
        if (rsc instanceof BestResResource) {
            BestResResource brr = (BestResResource) rsc;
            rscList = brr.getResourceList();
            for (ResourcePair rp : rscList) {
                if (rsc.equals(rp.getResource())) {
                    foundRp = rp;
                    break;
                }
            }
        } else {
            if (TimeSeriesResource.class.isAssignableFrom(rsc.getClass())) {
                rscList = getResourceContainer().getActiveDisplayPane()
                        .getDescriptor().getResourceList();
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
        }
        return foundRp;

    }

    public void unloadAllResourcesByName(String ensembleName) {

        List<AbstractResourceHolder> ensembleMembers = EnsembleResourceManager
                .getInstance().getResourceList(this)
                .getUserLoadedRscs(ensembleName);
        int total = ensembleMembers.size();
        int count = 0;
        for (AbstractResourceHolder gr : ensembleMembers) {
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
    }

    public boolean isDisposed() {
        return isDisposed;
    }

    /**
     * When the tool layer is disposed, clean up any listeners, unload all
     * resources associated with the layer, and call the tool manager to let it
     * handle its necessary cleanup.
     */
    @Override
    protected void disposeInternal() {

        unregisterListener((IToolLayerChanged) EnsembleTool.getInstance());

        descriptor.removeFrameChangedListener(this);

        resourceData.removeChangeListener((IResourceDataChanged) this);

        unloadAllResources();

        /* AWIPS2_GSD VLab Issue #29204 */
        EnsembleResourceManager.getInstance().remove(this);

        EnsembleTool.getInstance().checkForLastToolLayerClosed();

        isDisposed = true;

    }

    @Override
    protected void initInternal(IGraphicsTarget target) throws VizException {
        isDisposed = false;
        descriptor.getResourceList().getProperties(this)
                .setRenderingOrderId("HIGHEST");

    }

    @Override
    public void inited(AbstractVizResource<?, ?> initedRsc) {

        descriptor.addFrameChangedListener(this);
        registerListener((IRefreshListener) this);
        resourceData.addChangeListener((IResourceDataChanged) this);
        resourceData.setNameGenerator(
                new EnsembleToolLayerNameGeneratorWithTimeStampBasis(this));
        descriptor.getResourceList()
                .addPostRemoveListener(EnsembleTool.getInstance());

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

    public void calculate(Calculation algorithm, final Range range) {

        final EnsembleToolLayer thisToolLayer = this;

        if (algorithm == Calculation.ENSEMBLE_RELATIVE_FREQUENCY
                || algorithm == Calculation.ENSEMBLE_RELATIVE_FREQUENCY_IMAGE) {

            Thread t = null;
            t = new Thread() {
                public void run() {

                    ERFCalculator erfCalculation = new ERFCalculator(range);
                    if (algorithm == Calculation.ENSEMBLE_RELATIVE_FREQUENCY_IMAGE) {
                        erfCalculation.setImage(true);
                    } else {
                        erfCalculation.setImage(false);
                    }

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
        Job calculateJob = null;

        GeneratedDataLoader loader = new GeneratedDataLoader(thisToolLayer,
                GeneratedDataLoader.GeneratedloadMode.SAME_UNIT_AND_LEVEL);

        switch (algorithm) {

        case MEAN:

            calculateJob = new Job("Calculate Mean") {
                @Override
                protected IStatus run(IProgressMonitor monitor) {
                    // Load the mean overlay
                    MeanCalculator meanRsc = new MeanCalculator();
                    loader.load(meanRsc);

                    return Status.OK_STATUS;
                }
            };

            break;
        case MEAN_IMAGE:

            calculateJob = new Job("Calculate Mean Image") {
                @Override
                protected IStatus run(IProgressMonitor monitor) {
                    // Load the mean image overlay
                    MeanCalculator meanRsc = new MeanCalculator();
                    meanRsc.setImage(true);
                    loader.load(meanRsc);

                    return Status.OK_STATUS;
                }
            };

            break;
        case MIN:

            calculateJob = new Job("Calculate Minimum") {

                @Override
                protected IStatus run(IProgressMonitor monitor) {
                    // Load the min overlay
                    MinCalculator minRsc = new MinCalculator();
                    loader.load(minRsc);
                    return Status.OK_STATUS;

                }
            };
            break;
        case MIN_IMAGE:

            calculateJob = new Job("Calculate Minimum Image") {

                @Override
                protected IStatus run(IProgressMonitor monitor) {
                    // Load the min image overlay
                    MinCalculator minRsc = new MinCalculator();
                    minRsc.setImage(true);
                    loader.load(minRsc);
                    return Status.OK_STATUS;

                }
            };
            break;
        case MAX:

            calculateJob = new Job("Calculate Maximum") {

                @Override
                protected IStatus run(IProgressMonitor monitor) {
                    // Load the max overlay
                    MaxCalculator maxRsc = new MaxCalculator();
                    loader.load(maxRsc);
                    return Status.OK_STATUS;
                }
            };
            break;
        case MAX_IMAGE:

            calculateJob = new Job("Calculate Maximum Image") {

                @Override
                protected IStatus run(IProgressMonitor monitor) {
                    // Load the max image overlay
                    MaxCalculator maxRsc = new MaxCalculator();
                    maxRsc.setImage(true);
                    loader.load(maxRsc);
                    return Status.OK_STATUS;
                }
            };
            break;
        case MEDIAN:

            calculateJob = new Job("Calculate Median") {

                @Override
                protected IStatus run(IProgressMonitor monitor) {

                    // Load the median overlay
                    MedianCalculator medianRsc = new MedianCalculator();
                    loader.load(medianRsc);
                    return Status.OK_STATUS;
                }
            };
            break;
        case MEDIAN_IMAGE:

            calculateJob = new Job("Calculate Median Image") {

                @Override
                protected IStatus run(IProgressMonitor monitor) {

                    // Load the median image overlay
                    MedianCalculator medianRsc = new MedianCalculator();
                    medianRsc.setImage(true);
                    loader.load(medianRsc);
                    return Status.OK_STATUS;
                }
            };
            break;
        case MODE:

            calculateJob = new Job("Calculate Mode") {
                @Override
                protected IStatus run(IProgressMonitor monitor) {

                    // Load the median overlay
                    ModeCalculator modeRsc = new ModeCalculator();
                    loader.load(modeRsc);
                    return Status.OK_STATUS;
                }
            };
            break;
        case MODE_IMAGE:

            calculateJob = new Job("Calculate Mode Image") {
                @Override
                protected IStatus run(IProgressMonitor monitor) {

                    // Load the median image overlay
                    ModeCalculator modeRsc = new ModeCalculator();
                    modeRsc.setImage(true);
                    loader.load(modeRsc);
                    return Status.OK_STATUS;
                }
            };
            break;
        case RANGE:

            calculateJob = new Job("Calculate Range") {
                @Override
                protected IStatus run(IProgressMonitor monitor) {

                    // Load the range overlay
                    RangeCalculator rangeRsc = new RangeCalculator();
                    loader.load(rangeRsc);
                    return Status.OK_STATUS;
                }
            };
            break;
        case RANGE_IMAGE:

            calculateJob = new Job("Calculate Range Image") {
                @Override
                protected IStatus run(IProgressMonitor monitor) {

                    // Load the range image overlay
                    RangeCalculator rangeRsc = new RangeCalculator();
                    rangeRsc.setImage(true);
                    loader.load(rangeRsc);
                    return Status.OK_STATUS;
                }
            };
            break;
        case SUMMATION:

            calculateJob = new Job("Calculate Summation") {
                @Override
                protected IStatus run(IProgressMonitor monitor) {

                    // Load the sum overlay
                    SumCalculator sumRsc = new SumCalculator();
                    loader.load(sumRsc);
                    return Status.OK_STATUS;
                }
            };
            break;
        case SUMMATION_IMAGE:

            calculateJob = new Job("Calculate Summation Image") {
                @Override
                protected IStatus run(IProgressMonitor monitor) {

                    // Load the sum image overlay
                    SumCalculator sumRsc = new SumCalculator();
                    sumRsc.setImage(true);
                    loader.load(sumRsc);
                    return Status.OK_STATUS;
                }
            };
            break;
        case STANDARD_DEVIATION:

            calculateJob = new Job("Calculate Std Deviation") {
                @Override
                protected IStatus run(IProgressMonitor monitor) {

                    // Load the sum overlay
                    StddevCalculator stddevRsc = new StddevCalculator();
                    loader.load(stddevRsc);
                    return Status.OK_STATUS;
                }
            };
            break;
        case STANDARD_DEVIATION_IMAGE:

            calculateJob = new Job("Calculate Std Deviation Image") {
                @Override
                protected IStatus run(IProgressMonitor monitor) {

                    // Load the sum image overlay
                    StddevCalculator stddevRsc = new StddevCalculator();
                    stddevRsc.setImage(true);
                    loader.load(stddevRsc);
                    return Status.OK_STATUS;
                }
            };
            break;
        case AVG_MINUS_STD_DEV:

            calculateJob = new Job("Calculate Avg Minus Std Deviation") {
                @Override
                protected IStatus run(IProgressMonitor monitor) {

                    // Load the sum overlay
                    AvgM1StddevCalculator m1StddevRsc = new AvgM1StddevCalculator();
                    loader.load(m1StddevRsc);
                    return Status.OK_STATUS;
                }
            };
            break;
        case AVG_MINUS_STD_DEV_IMAGE:

            calculateJob = new Job("Calculate Avg Minus Std Deviation Image") {
                @Override
                protected IStatus run(IProgressMonitor monitor) {

                    // Load the avg minus std deviation image overlay
                    AvgM1StddevCalculator m1StddevRsc = new AvgM1StddevCalculator();
                    m1StddevRsc.setImage(true);
                    loader.load(m1StddevRsc);
                    return Status.OK_STATUS;
                }
            };
            break;
        case AVG_PLUS_STD_DEV:

            calculateJob = new Job("Calculate Avg Plus Std Deviation") {
                @Override
                protected IStatus run(IProgressMonitor monitor) {

                    // Load the sum overlay
                    AvgP1StddevCalculator p1StddevRsc = new AvgP1StddevCalculator();
                    loader.load(p1StddevRsc);
                    return Status.OK_STATUS;
                }
            };
            break;
        case AVG_PLUS_STD_DEV_IMAGE:

            calculateJob = new Job("Calculate Avg Plus Std Deviation Image") {
                @Override
                protected IStatus run(IProgressMonitor monitor) {

                    // Load the avg plus std deviation image overlay
                    AvgP1StddevCalculator p1StddevRsc = new AvgP1StddevCalculator();
                    p1StddevRsc.setImage(true);
                    loader.load(p1StddevRsc);
                    return Status.OK_STATUS;
                }
            };
            break;
        case VALUE_SAMPLING:

            calculateJob = new Job("Do Value Sampling") {
                @Override
                protected IStatus run(IProgressMonitor monitor) {

                    loader.loadOverlay(Calculation.VALUE_SAMPLING);
                    return Status.OK_STATUS;
                }
            };
            break;

        case HISTOGRAM_SAMPLING:

            calculateJob = new Job("Do Histogram Sampling") {
                @Override
                protected IStatus run(IProgressMonitor monitor) {

                    loader.loadOverlay(Calculation.HISTOGRAM_SAMPLING);
                    return Status.OK_STATUS;
                }
            };
            break;

        case HISTOGRAM_GRAPHICS:

            calculateJob = new Job("Do Histogram Graphics") {
                @Override
                protected IStatus run(IProgressMonitor monitor) {

                    loader.loadOverlay(Calculation.HISTOGRAM_GRAPHICS);
                    return Status.OK_STATUS;
                }
            };
            break;

        default:
            break;
        }

        if (calculateJob != null) {
            calculateJob.setSystem(false);
            calculateJob.setPriority(Job.INTERACTIVE);
            calculateJob.schedule();
        }
    }

    public void setEditable(boolean makeEditable) {
        isEditable = makeEditable;
        EditableManager.makeEditable(this, makeEditable);
    }

    @Override
    public void frameChanged(IDescriptor descriptor, DataTime oldTime,
            DataTime newTime) {

        EnsembleTool.getInstance().frameChanged(descriptor.getFramesInfo());

    }

    class EnsembleToolLayerNameGeneratorWithTimeStampBasis
            extends AbstractNameGenerator {

        EnsembleToolLayer toolLayer = null;

        EnsembleToolLayerNameGeneratorWithTimeStampBasis(EnsembleToolLayer tl) {
            toolLayer = tl;
        }

        @Override
        public String getName(AbstractVizResource<?, ?> resource) {
            String timeBasis = "";
            if (!isEmpty()) {
                timeBasis = EnsembleTool.getInstance().getActiveResourceTime();
                if (EnsembleResourceManager.isTimeEmpty(timeBasis)) {
                    timeBasis = "";
                } else {
                    timeBasis = " (" + timeBasis + ") ";
                }
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
     * All ensemble tool classes should make certain they check to see whether
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

    /* In association with VLab AWIPS2_GSD Issue #29762 */
    @Override
    public void resourceChanged(ChangeType type, Object object) {

        if (type == ChangeType.CAPABILITY
                && object instanceof EditableCapability) {
            EditableCapability editability = (EditableCapability) object;
            isEditable = editability.isEditable();
            for (IToolLayerChanged etl : changeListeners) {
                etl.resourceChanged(this, type, object);
            }
        }
    }

}
