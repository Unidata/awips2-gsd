package gov.noaa.gsd.viz.ensemble.display.control;

import gov.noaa.gsd.viz.ensemble.display.common.GenericResourceHolder;
import gov.noaa.gsd.viz.ensemble.display.common.NavigatorResourceList;
import gov.noaa.gsd.viz.ensemble.display.rsc.histogram.HistogramResource;
import gov.noaa.gsd.viz.ensemble.navigator.ui.layer.EnsembleTool;
import gov.noaa.gsd.viz.ensemble.navigator.ui.layer.EnsembleToolLayer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.graphics.RGB;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.viz.core.IDisplayPaneContainer;
import com.raytheon.uf.viz.core.drawables.IDescriptor;
import com.raytheon.uf.viz.core.drawables.ResourcePair;
import com.raytheon.uf.viz.core.grid.rsc.AbstractGridResource;
import com.raytheon.uf.viz.core.rsc.AbstractVizResource;
import com.raytheon.uf.viz.core.rsc.IDisposeListener;
import com.raytheon.uf.viz.core.rsc.IRefreshListener;
import com.raytheon.uf.viz.core.rsc.ResourceList;
import com.raytheon.uf.viz.core.rsc.capabilities.ColorableCapability;
import com.raytheon.uf.viz.core.rsc.capabilities.DensityCapability;
import com.raytheon.uf.viz.core.rsc.capabilities.DisplayTypeCapability;
import com.raytheon.uf.viz.xy.timeseries.rsc.TimeSeriesResource;
import com.raytheon.viz.ui.editor.AbstractEditor;

/**
 * Handle all ensemble products member resources, reference to any loaded grid
 * product may be treated as ensemble member, and ensemble calculation result
 * resources. This is the key architectural support class connecting the
 * ensemble display, GUI and D2D core. It tracks and the status of the handled
 * resources, controls them, keeps the ensemble tool works with D2D core display
 * synchronously. All registered resources are in the ensembleToolResourcesMap.
 * 
 * 
 * @author jing
 * @author polster
 * @version 1.0
 * 
 *          <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Feb, 2014      5056       jing     Initial creation
 * 
 * </pre>
 */

public class EnsembleResourceManager implements IDisposeListener {
    private final static double DEFAULT_DENSITY = 0.15;

    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(EnsembleResourceManager.class);

    /**
     * The ensemble marked resource list holds the loaded and generated
     * resources and flags, is used by ensemble display, GUI and calculation.
     */
    private ConcurrentHashMap<EnsembleToolLayer, NavigatorResourceList> ensembleToolResourcesMap;

    private ArrayBlockingQueue<AbstractVizResource<?, ?>> incomingSpooler = null;

    /*
     * The instance of the manager, a singleton.
     */
    public static EnsembleResourceManager instance = null;

    protected PollForIncomingResources registerIncomingResources = null;

    private List<VisibilityAndColorChangedListener> visibilityAndColorChangedListeners = null;

    /**
     * The listeners to any resource, GUI... change event Register to any bus or
     * event list for to catch interesting events
     */
    private EnsembleResourceManager() {
        ensembleToolResourcesMap = new ConcurrentHashMap<EnsembleToolLayer, NavigatorResourceList>();
        visibilityAndColorChangedListeners = new CopyOnWriteArrayList<>();

        incomingSpooler = new ArrayBlockingQueue<>(500);

    }

    public void startSpooler() {
        registerIncomingResources = new PollForIncomingResources(
                "Ensemble Resource Spooler");
        registerIncomingResources.setSystem(true);
        registerIncomingResources.setPriority(Job.LONG);
        registerIncomingResources.schedule();
    }

    public void stopSpooler() {
        if (registerIncomingResources != null) {
            registerIncomingResources.cancel();
        }
        registerIncomingResources = null;
    }

    /*
     * Viz resources register with the ensemble tool via this thread-safe FIFO
     * queue.
     */
    protected void addResourceForRegistration(AbstractVizResource<?, ?> rsc) {
        incomingSpooler.add(rsc);
    }

    /**
     * Get the resource manager.
     * 
     * @return
     */
    public static EnsembleResourceManager getInstance() {
        if (instance == null) {
            instance = new EnsembleResourceManager();
        }
        return instance;
    }

    /**
     * Get the resources in an editor.
     * 
     * @param editor
     *            - The product is loaded into which editor/window.
     * @return - list of resources.
     */
    public NavigatorResourceList getResourceList(EnsembleToolLayer toolLayer) {

        NavigatorResourceList list = null;

        if (toolLayer != null) {
            list = ensembleToolResourcesMap.get(toolLayer);
        }
        return list;
    }

    /**
     * Clean the resources in an editor.
     * 
     * @param editor
     *            - The product is loaded into which editor/window.
     */
    public void clearResourceList(AbstractEditor editor) {
        ensembleToolResourcesMap.get(editor).clear();
    }

    /**
     * Find a visible and color listener by resource.
     */
    protected VisibilityAndColorChangedListener findListenerByResource(
            AbstractVizResource<?, ?> rsc) {
        VisibilityAndColorChangedListener foundListener = null;
        for (VisibilityAndColorChangedListener vccl : visibilityAndColorChangedListeners) {
            if (vccl.resource == rsc) {
                foundListener = vccl;
                break;
            }
        }
        return foundListener;
    }

    /**
     * Register a loaded model product in ensemble status
     * 
     * @param rsc
     *            - the loaded product resource
     * @param guiUpdate
     *            - if need update GUI
     */
    public synchronized void registerResource(AbstractVizResource<?, ?> rsc) {

        if ((rsc == null) || (!EnsembleTool.getInstance().isToolAvailable())) {
            return;
        }

        EnsembleToolLayer toolLayer = EnsembleTool.getInstance()
                .getActiveToolLayer();

        // if this is the first resource ever registered using this editor
        // then create the resource list and map it to the editor ...
        if (ensembleToolResourcesMap.get(toolLayer) == null) {
            ensembleToolResourcesMap.put(toolLayer, new NavigatorResourceList(
                    toolLayer));
        }

        // no duplicates
        if (rsc == null
                || ensembleToolResourcesMap.get(toolLayer)
                        .containsResource(rsc)) {
            return;
        }

        // TODO: Must refactor so we don't use setSystemResource(boolean)
        // method.
        rsc.getProperties().setSystemResource(true);

        rsc.registerListener((IDisposeListener) this);

        if (rsc.hasCapability(DisplayTypeCapability.class)) {
            rsc.getCapability(DisplayTypeCapability.class)
                    .setSuppressingMenuItems(true);
        }

        // Set to default density for all loaded resources if can
        // But it may be unmatched with current display. Fix it later
        if (rsc.getCapability(DensityCapability.class) != null) {
            DensityCapability densityCapability = (DensityCapability) rsc
                    .getCapability(DensityCapability.class);
            densityCapability.setDensity(DEFAULT_DENSITY);
        }

        GenericResourceHolder ensToolResource = GenericResourceHolder
                .createResourceHolder(rsc, true);
        ensToolResource.setGenerated(false);
        ensembleToolResourcesMap.get(toolLayer).add(ensToolResource);

        checkExistingRegisteredResources(toolLayer);

        notifyClientListChanged();

        visibilityAndColorChangedListeners
                .add(new VisibilityAndColorChangedListener(rsc));

        // now update the calculation ensemble resource, if not already
        // set ... there can only be one, and it will be the first
        // ensemble resource loaded into this editor ...
        ensembleToolResourcesMap.get(toolLayer)
                .updateEnsembleCalculationResource();
    }

    /**
     * Remove a resource from the tracking list.
     * 
     * @param gr
     *            - the tracked resource.
     * @param editor
     *            - loaded editor.
     * @param notifyGUI
     *            - if update the GUI.
     */
    public synchronized void unregisterResource(GenericResourceHolder gr,
            EnsembleToolLayer toolLayer, boolean notifyGUI) {
        if (toolLayer == null
                || gr == null
                || ensembleToolResourcesMap.get(toolLayer).getResourceHolders()
                        .isEmpty())
            return;

        VisibilityAndColorChangedListener vccl = findListenerByResource(gr
                .getRsc());
        if (vccl != null) {
            visibilityAndColorChangedListeners.remove(vccl);
        }

        ensembleToolResourcesMap.get(toolLayer).remove(gr);
        gr.getRsc().unregisterListener((IDisposeListener) this);

        checkExistingRegisteredResources(toolLayer);

        // if requested, notify client the known loaded resources have changed
        if (notifyGUI) {
            notifyClientListChanged();
        }

    }

    /**
     * Register Ensemble generated resource. after creating or deleting a
     * ensemble generated resource.
     * 
     * @param rsc
     *            - the generated resource by calculation.
     */
    public synchronized void registerGenerated(AbstractVizResource<?, ?> rsc) {

        if ((rsc == null) || (!EnsembleTool.getInstance().isToolAvailable())) {
            return;
        }

        EnsembleToolLayer toolLayer = EnsembleTool.getInstance()
                .getActiveToolLayer();

        // This may be the first resource ever registered using this editor. If
        // so, then create the resource list and associate it with the editor
        // using the map ...

        if (ensembleToolResourcesMap.get(toolLayer) == null) {
            ensembleToolResourcesMap.put(toolLayer, new NavigatorResourceList(
                    toolLayer));
        }

        // Only one instance of a generated resource is saved. If a duplicate is
        // created then have it overwrite the existing one.
        for (GenericResourceHolder gr : ensembleToolResourcesMap.get(toolLayer)
                .getUserGeneratedRscs()) {

            // Remove any resource in this list, since it was unloaded/ isn't
            // existing.
            if (!gr.getRsc().getDescriptor().getResourceList()
                    .containsRsc(gr.getRsc())) {
                ensembleToolResourcesMap.get(toolLayer).remove(gr);

            } else if (rsc.getClass().cast(rsc).getName()
                    .equals(gr.getRsc().getClass().cast(gr.getRsc()).getName())) {
                // Same generated resource name, unload old one
                ensembleToolResourcesMap.get(toolLayer).remove(gr);
                gr.getRsc().getDescriptor().getResourceList()
                        .removeRsc(gr.getRsc());
            }
        }

        // Set to default density for all loaded resources if can
        // But it may be unmatched with current display. Fix it later

        // Set resource as a system resource so we don't show the legend
        // in the main map, because we will display it in the ensemble
        // navigator view.

        // TODO: Must refactor so we don't use setSystemResource(boolean)
        // method.
        rsc.getProperties().setSystemResource(true);
        rsc.registerListener((IDisposeListener) this);

        if (rsc.hasCapability(DisplayTypeCapability.class)) {
            rsc.getCapability(DisplayTypeCapability.class)
                    .setSuppressingMenuItems(true);
        }

        GenericResourceHolder ensToolResource = GenericResourceHolder
                .createResourceHolder(rsc, true);
        ensToolResource.setGenerated(true);
        ensembleToolResourcesMap.get(toolLayer).add(ensToolResource);

        checkExistingRegisteredResources(toolLayer);

        visibilityAndColorChangedListeners
                .add(new VisibilityAndColorChangedListener(rsc));

        notifyClientListChanged();
    }

    /**
     * Remove a generated resource from the tracking list.
     * 
     * @param gr
     *            - the tracked resource.
     * @param editor
     *            - loaded editor.
     * @param notifyGUI
     *            - if update the GUI.
     */
    public synchronized void unregisterGenerated(GenericResourceHolder gr,
            EnsembleToolLayer toolLayer, boolean notifyGUI) {
        if (gr == null
                || ensembleToolResourcesMap.get(toolLayer).getResourceHolders()
                        .isEmpty()) {
            return;
        }

        VisibilityAndColorChangedListener vccl = findListenerByResource(gr
                .getRsc());
        if (vccl != null) {
            visibilityAndColorChangedListeners.remove(vccl);
        }

        ensembleToolResourcesMap.get(toolLayer).remove(gr);
        gr.getRsc().unregisterListener((IDisposeListener) this);

        // notify client the generated resource change?

        checkExistingRegisteredResources(toolLayer);

        // if requested, notify client the known loaded resources have changed
        if (notifyGUI) {
            notifyClientListChanged();
        }
    }

    /**
     * Get the time series point location
     * 
     * @param activeToolLayer
     *            - the time series tool layer
     * 
     * @return - point location as a String
     */
    public String getTimeSeriesPoint(EnsembleToolLayer toolLayer) {
        String s = "";
        if (toolLayer != null && getResourceList(toolLayer) != null) {
            s = getResourceList(toolLayer).getTimeSeriesPoint();
        }
        return s;
    }

    /**
     * Get the resource name string
     * 
     * @param activeToolLayer
     *            - the time series tool layer
     * 
     * @return- resource name
     */
    public String getTimeBasisResourceName(EnsembleToolLayer toolLayer) {

        String s = "";
        if (toolLayer != null && getResourceList(toolLayer) != null) {
            s = getResourceList(toolLayer).getTimeBasisResourceName();
        }
        return s;

    }

    /**
     * Get the resource legend time
     * 
     * @param activeToolLayer
     *            - the time series tool layer
     * 
     * @return- legend time
     */
    public String getTimeBasisLegendTime(EnsembleToolLayer toolLayer) {

        String s = "";
        if (toolLayer != null && getResourceList(toolLayer) != null) {
            s = getResourceList(toolLayer).getTimeBasisLegendTime();
        }
        return s;

    }

    /**
     * Handle ENS GUI change
     * 
     * @param editor
     */
    public void updateFrameChanges(EnsembleToolLayer toolLayer) {
        checkExistingRegisteredResources(toolLayer);
        // update generated ensemble Resource if need
        updateGenerated(toolLayer);
    }

    /**
     * Match up the registered resource list within editor(s) and GUI. Verify if
     * the resources in the list exist. Remove any resource in this list, if it
     * was unloaded. Notify GUI if there is any change.
     */
    public void checkExistingRegisteredResources(EnsembleToolLayer toolLayer) {

        if (!EnsembleTool.getInstance().isToolAvailable()) {
            return;
        }
        if (ensembleToolResourcesMap.get(toolLayer) == null) {
            return;
        }

        if (ensembleToolResourcesMap.get(toolLayer).getAllRscsAsList() == null
                || ensembleToolResourcesMap.get(toolLayer).getAllRscsAsList()
                        .isEmpty())
            return;

        for (GenericResourceHolder gr : ensembleToolResourcesMap.get(toolLayer)
                .getAllRscsAsList()) {
            // verify if the resources in the list are existing.
            if (!gr.getRsc().getDescriptor().getResourceList()
                    .containsRsc(gr.getRsc())) {

                // Remove any resource in this list, since it was unloaded/
                // isn't existing.
                ensembleToolResourcesMap.get(toolLayer).remove(gr);

                // TODO: notify GUI if there is any change.
                // notifyClientListChanged();
            } else {
                /*
                 * don't show legend when the ensemble tool is controlling the
                 * resource
                 */
                gr.getRsc().getProperties().setSystemResource(true);

                // Marks to unselected if the resource is not visible
                // set resource selection to true.
                if (gr.getRsc().getProperties().isVisible()) {
                    gr.setSelected(true);
                } else {
                    gr.setSelected(false);
                }
            }
        }
    }

    /**
     * Return all ensemble-related resources not currently registered with this
     * resource manager.
     * 
     * This method will allow the Ensemble Tool to read any given editor and
     * find the resources not yet registered, so, for example, we can then
     * register those resources.
     * 
     * @param editor
     * @return
     */
    public ResourceList getUnregisteredResources(IDisplayPaneContainer editor) {

        ResourceList unRegisteredResources = new ResourceList();

        // The registered resources for this editor in the manager
        List<GenericResourceHolder> resourceList = ensembleToolResourcesMap
                .get(editor).getAllRscsAsList();

        IDescriptor desc = (editor.getActiveDisplayPane().getDescriptor());
        ResourceList rscList = desc.getResourceList();
        for (ResourcePair rp : rscList) {

            AbstractVizResource<?, ?> rsc = rp.getResource();
            if ((rsc instanceof AbstractGridResource)
                    || (rsc instanceof TimeSeriesResource)
                    || (rsc instanceof HistogramResource)) {

                boolean isRegistered = false;
                for (GenericResourceHolder rcsHolder : resourceList) {
                    if (rcsHolder.getRsc() == rsc) {
                        isRegistered = true;
                    }
                }
                if (!isRegistered) {
                    unRegisteredResources.add(rp);
                }
            }
        }
        return unRegisteredResources;
    }

    /**
     * Check generated resources update data and display if need
     * 
     * @param editor
     */
    public void updateGenerated(EnsembleToolLayer toolLayer) {
        if (toolLayer == null
                || ensembleToolResourcesMap.get(toolLayer) == null) {
            return;
        }
        if (ensembleToolResourcesMap.get(toolLayer).getUserGeneratedRscs() == null
                || ensembleToolResourcesMap.get(toolLayer)
                        .getUserGeneratedRscs().isEmpty())
            return;
        // Check each resource if need update
        for (GenericResourceHolder rsc : ensembleToolResourcesMap
                .get(toolLayer).getUserGeneratedRscs()) {
            /**
             * TODO : Generated resources will not get auto-updated by the
             * server, so leave this for future use.
             */
        }
    }

    /**
     * post process in this level
     * 
     * @param editor
     */
    public void disposal(AbstractEditor editor) {
        clearResourceList(editor);
        saveResourceList(editor);

        // notify client
        notifyClientListChanged();
    }

    /**
     * Marked ResourceList change event should be sent to client by the event
     * bus. The client can be GUI, calculator, etc, ...
     */
    protected void notifyClientListChanged() {

        if (EnsembleTool.getInstance().isToolAvailable()) {
            EnsembleTool.getInstance().refreshView();
        }
    }

    /**
     * Save the registered resource list into a file
     * 
     * @param editor
     */
    private void saveResourceList(AbstractEditor editor) {
        clearResourceList(editor);

        // notify client
        notifyClientListChanged();
    }

    /**
     * post process for other interfaces
     * 
     */
    @Override
    public void disposed(AbstractVizResource<?, ?> rsc) {

        // TODO Find editor for resource ...

        // TODO ResourceManager.getInstance().unregisterResource(rsc);
        notifyClientListChanged();
    }

    /**
     * Set ensemble calculation resource name.
     * 
     * @param editor
     * @param rscGroupName
     */
    public void setEnsembleCalculationResourceName(AbstractEditor editor,
            String rscGroupName) {

        if (ensembleToolResourcesMap.get(editor) != null) {
            ensembleToolResourcesMap.get(editor)
                    .setEnsembleCalculationResource(rscGroupName);
        }
    }

    /**
     * Get ensemble calculation resource name.
     * 
     * @param editor
     * @return
     */
    public String getEnsembleCalculationResourceName(AbstractEditor editor) {

        String name = "";
        if (ensembleToolResourcesMap.get(editor) != null) {
            name = ensembleToolResourcesMap.get(editor)
                    .getEnsembleCalculationResource();
        }
        return name;
    }

    /**
     * This job sleeps for a poll-period and is only active when there are
     * resources being registered.
     * 
     * It runs for as long as the Ensemble Tool is open (i.e. not disposed). It
     * wakes when new resources need to be registered.
     * 
     */
    private class PollForIncomingResources extends Job {

        private IStatus status = Status.OK_STATUS;

        private AbstractVizResource<?, ?> nextRsc = null;

        public PollForIncomingResources(String name) {
            super(name);
            status = Status.OK_STATUS;
        }

        @Override
        protected IStatus run(IProgressMonitor monitor) {

            final List<AbstractVizResource<?, ?>> items = new ArrayList<>();
            try {
                items.clear();
                nextRsc = incomingSpooler.poll(60, TimeUnit.SECONDS);
                if (!monitor.isCanceled()) {
                    if (nextRsc != null) {
                        EnsembleResourceManager.getInstance().registerResource(
                                nextRsc);
                    }
                }
                if (!monitor.isCanceled() && (!incomingSpooler.isEmpty())) {
                    incomingSpooler.drainTo(items);
                    if (!monitor.isCanceled()) {
                        for (AbstractVizResource<?, ?> r : items) {
                            if (r != null) {
                                EnsembleResourceManager.getInstance()
                                        .registerResource(r);
                            }
                        }
                    }
                }
            } catch (InterruptedException e) {
                /* ignore */
            }

            if (!monitor.isCanceled()) {
                setPriority(Job.LONG);
                schedule();
            } else {
                status = Status.CANCEL_STATUS;
            }

            return status;

        }
    }

    protected class VisibilityAndColorChangedListener implements
            IRefreshListener, IDisposeListener {

        public final AbstractVizResource<?, ?> resource;

        private boolean visible;

        private RGB color;

        protected VisibilityAndColorChangedListener(
                AbstractVizResource<?, ?> resource) {
            this.resource = resource;
            this.visible = resource.getProperties().isVisible();
            this.color = resource.getCapability(ColorableCapability.class)
                    .getColor();
            resource.registerListener((IRefreshListener) this);
            resource.registerListener((IDisposeListener) this);
        }

        @Override
        public void disposed(AbstractVizResource<?, ?> rsc) {
            if (rsc == resource && resource != null) {
                resource.unregisterListener((IRefreshListener) this);
                resource.unregisterListener((IDisposeListener) this);
            }
        }

        @Override
        public void refresh() {
            boolean visible = resource.getProperties().isVisible();
            if (this.visible != visible) {
                notifyClientListChanged();
                this.visible = visible;
            }
            RGB color = resource.getCapability(ColorableCapability.class)
                    .getColor();
            if (!this.color.equals(color)) {
                notifyClientListChanged();
                this.color = color;
            }
        }

    }

    public void removeMapping(EnsembleToolLayer toolLayer) {
        ensembleToolResourcesMap.remove(toolLayer);
    }

}
