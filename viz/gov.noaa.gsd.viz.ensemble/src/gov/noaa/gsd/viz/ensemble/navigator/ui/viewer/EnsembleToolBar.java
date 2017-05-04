package gov.noaa.gsd.viz.ensemble.navigator.ui.viewer;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.NotEnabledException;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.services.IServiceLocator;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.viz.core.IDisplayPaneContainer;
import com.raytheon.uf.viz.core.VizApp;
import com.raytheon.viz.ui.EditorUtil;

import gov.noaa.gsd.viz.ensemble.control.EnsembleTool;
import gov.noaa.gsd.viz.ensemble.control.EnsembleTool.EnsembleToolMode;
import gov.noaa.gsd.viz.ensemble.display.calculate.Calculation;
import gov.noaa.gsd.viz.ensemble.navigator.ui.layer.EnsembleToolLayer;
import gov.noaa.gsd.viz.ensemble.navigator.ui.viewer.common.GlobalPreferencesComposite;
import gov.noaa.gsd.viz.ensemble.navigator.ui.viewer.common.PreferencesDialog;
import gov.noaa.gsd.viz.ensemble.util.EnsembleToolImageStore;

/***
 * 
 * This class is a Composite which contains only a ToolBar. It is tightly
 * coupled with the EnsembleToolViewer (ETV) and is intended to be stored only
 * inside the ETV's top-level CTabFolder.
 * 
 * It contains tool bar items ("buttons") which change behavior depending upon
 * which state the Ensemble Tool is in: Legend browser or Matrix navigator mode.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#    Engineer      Description
 * ------------ ---------- ----------- --------------------------
 * Oct 15, 2015   12565      polster     Initial creation
 * Jan 15, 2016   12301      jing        Added distribution feature
 * Oct 12, 2016   19443      polster     Moved model family dialog access
 * Dec 29, 2016   19325      jing        Added image items in the calculation menu
 * Mar 01, 2017   19443      polster     Fixed toggle editability problem
 * 
 * </pre>
 * 
 * @author polster
 * @author jing
 * @version 1.0
 */

public class EnsembleToolBar extends Composite {

    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(EnsembleToolBar.class);

    private EnsembleToolViewer ensembleToolViewer = null;

    private CTabFolder rootTabFolder = null;

    private ToolBar toolBar = null;

    private ToolItem browserToolItem = null;

    private ToolItem powerToggleToolItem = null;

    private ToolItem clearAllEntriesToolItem = null;

    private Menu dropdownMenu = null;

    private ToolItem actionsDropdownToolItem = null;

    private EnsembleToolItemActionDropdown toolRelevantActions = null;

    private PreferencesDialog prefsDialog = null;

    private final LegendsBrowserCalculationSelectionAdapter legendsCalculationListener = new LegendsBrowserCalculationSelectionAdapter();

    public EnsembleToolBar(Composite parent, int style,
            EnsembleToolViewer etv) {
        super(parent, style);
        rootTabFolder = (CTabFolder) parent;
        ensembleToolViewer = etv;
        createToolBar();
    }

    private void createToolBar() {
        Composite toolbarComposite = new Composite(rootTabFolder, SWT.BORDER);

        FillLayout toolbarContainer_fl = new FillLayout(SWT.HORIZONTAL);
        toolbarContainer_fl.marginWidth = 1;
        toolbarContainer_fl.marginHeight = 1;
        toolbarComposite.setLayout(toolbarContainer_fl);

        /* Fill the tool bar and add it to the main tab folder */
        toolBar = makeToolBar(toolbarComposite);
        rootTabFolder.setTopRight(toolbarComposite);

    }

    public void disableTool() {
        powerToggleToolItem.setEnabled(false);
    }

    synchronized public void setEditable(final boolean enabled) {

        VizApp.runSync(new Runnable() {

            @Override
            public void run() {

                if (isWidgetReady()) {
                    if (enabled) {
                        powerToggleToolItem
                                .setImage(EnsembleToolImageStore.POWER_ON_IMG);
                        powerToggleToolItem.setToolTipText("Tool Off");
                    } else {
                        powerToggleToolItem
                                .setImage(EnsembleToolImageStore.POWER_OFF_IMG);
                        powerToggleToolItem.setToolTipText("Tool On");
                    }

                    browserToolItem.setEnabled(enabled);
                    toolRelevantActions.setEnabled(enabled);
                    clearAllEntriesToolItem.setEnabled(enabled);

                    /* Always on items */
                    powerToggleToolItem.setEnabled(true);
                    toolBar.setEnabled(true);
                }
            }
        });

    }

    public void setToolbarMode(EnsembleToolMode mode) {

        toolRelevantActions.setToolMode(mode);
        if (mode == EnsembleToolMode.LEGENDS_PLAN_VIEW
                || mode == EnsembleToolMode.LEGENDS_TIME_SERIES) {
            browserToolItem.setToolTipText("Open Volume Browser");
            browserToolItem.setImage(EnsembleToolImageStore.VOLUME_BROWSER_IMG);
        } else if (mode == EnsembleToolMode.MATRIX) {
            browserToolItem.setToolTipText("Open a Model Family");
            browserToolItem.setImage(EnsembleToolImageStore.MATRIX_BROWSER_IMG);
        }

    }

    /*
     * Create the ViewPart's main tool bar.
     */
    private ToolBar makeToolBar(Composite parent) {

        toolBar = new ToolBar(parent, SWT.NONE);

        browserToolItem = new ToolItem(toolBar, SWT.PUSH);
        browserToolItem.setImage(EnsembleToolImageStore.VOLUME_BROWSER_IMG);
        browserToolItem.setToolTipText("Volume Browser");

        browserToolItem.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {

                EnsembleToolMode mode = EnsembleTool.getInstance()
                        .getToolMode();
                if (mode == EnsembleToolMode.LEGENDS_PLAN_VIEW
                        || mode == EnsembleToolMode.LEGENDS_TIME_SERIES) {

                    IServiceLocator serviceLocator = PlatformUI.getWorkbench();
                    ICommandService commandService = (ICommandService) serviceLocator
                            .getService(ICommandService.class);

                    Command command = commandService.getCommand(
                            "com.raytheon.viz.volumebrowser.volumeBrowserRef");

                    /**
                     * Optionally pass a ExecutionEvent instance, default
                     * (empty) signature creates blank event
                     */
                    try {
                        command.executeWithChecks(new ExecutionEvent());
                    } catch (ExecutionException | NotDefinedException
                            | NotEnabledException | NotHandledException e1) {
                        statusHandler.warn(e1.getLocalizedMessage()
                                + "; Unable to open Volume Browser");
                    }

                } else if (mode == EnsembleTool.EnsembleToolMode.MATRIX) {

                    ensembleToolViewer.getMatrixNavigator()
                            .openFamilyLoaderDialog();

                }
            }
        });

        clearAllEntriesToolItem = new ToolItem(toolBar, SWT.PUSH);
        clearAllEntriesToolItem.setImage(EnsembleToolImageStore.CLEAR_ALL_IMG);
        clearAllEntriesToolItem.setToolTipText("Clear All Entries");
        clearAllEntriesToolItem.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {

                if (EnsembleTool.getInstance() == null) {
                    return;
                }
                boolean isFull = (EnsembleTool.getInstance()
                        .getToolLayer() == null
                        || EnsembleTool.getInstance().getToolLayer().isEmpty())
                                ? false : true;

                IDisplayPaneContainer editor = null;
                String clearResourcesPrompt = "Are you sure you want to clear all Ensemble Tool resources in the active editor?";
                if (EnsembleTool.getInstance().getActiveEditor() != null) {
                    editor = EnsembleTool.getInstance().getActiveEditor();
                    if (EnsembleTool.isMatrixEditor(editor)) {
                        clearResourcesPrompt = "Are you sure you want to clear all Matrix resources in the active editor?";
                    }
                }
                if (isFull) {
                    boolean isOkay = MessageDialog.open(MessageDialog.QUESTION,
                            getShell(), "Confirm Clear All Entries",
                            clearResourcesPrompt, SWT.NONE);
                    if (isOkay) {
                        EnsembleTool.getInstance().clearToolLayer();
                    }
                }
            }

        });

        toolRelevantActions = new EnsembleToolItemActionDropdown(toolBar);
        toolRelevantActions.setToolMode(
                EnsembleTool.getToolMode(EditorUtil.getActiveVizContainer()));

        ToolItem separator_3 = new ToolItem(toolBar, SWT.SEPARATOR);
        separator_3.setWidth(0);

        powerToggleToolItem = new ToolItem(toolBar, SWT.PUSH);
        powerToggleToolItem.setImage(EnsembleToolImageStore.POWER_ON_IMG);
        powerToggleToolItem.setToolTipText("Tool Off");
        powerToggleToolItem.setSelection(EnsembleToolViewer.isEditable());
        powerToggleToolItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {

                /* In association with VLab AWIPS2_GSD Issue #29762 */
                EnsembleTool.getInstance().setEditable(
                        !EnsembleTool.getInstance().isToolEditable());
            }
        });

        toolBar.redraw();

        return toolBar;
    }

    private boolean isToolEnabled() {
        boolean isToolEnabled = false;
        EnsembleTool et = EnsembleTool.getInstance();
        if (et != null) {
            IDisplayPaneContainer editor = et.getActiveEditor();
            if (editor != null) {
                EnsembleToolLayer toolLayer = EnsembleTool.getToolLayer(editor);
                if (toolLayer != null) {
                    if (toolLayer.isEmpty()) {
                        isToolEnabled = false;
                    } else {
                        isToolEnabled = true;
                    }
                }
            }
        }
        return isToolEnabled;
    }

    protected void addLegendsPlanViewItems() {

        if (EnsembleTool.getInstance() == null) {
            return;
        }
        removeAllMenuItems();

        new MenuItem(dropdownMenu, SWT.SEPARATOR);

        boolean isEnabled = isToolEnabled();

        add(Calculation.MEAN.getTitle(), "Calculate mean of visible resources",
                isEnabled, legendsCalculationListener);
        add(Calculation.MEAN_IMAGE.getTitle(),
                "Calculate mean image of visible resources", isEnabled,
                legendsCalculationListener);
        add(Calculation.MIN.getTitle(),
                "Calculate minimum on visible resources", isEnabled,
                legendsCalculationListener);
        add(Calculation.MIN_IMAGE.getTitle(),
                "Calculate minimum image on visible resources", isEnabled,
                legendsCalculationListener);
        add(Calculation.MAX.getTitle(),
                "Calculate maxixum on visible resources", isEnabled,
                legendsCalculationListener);
        add(Calculation.MAX_IMAGE.getTitle(),
                "Calculate maxixum image on visible resources", isEnabled,
                legendsCalculationListener);
        add(Calculation.MEDIAN.getTitle(),
                "Calculate median on visible resources", isEnabled,
                legendsCalculationListener);
        add(Calculation.MEDIAN_IMAGE.getTitle(),
                "Calculate median image on visible resources", isEnabled,
                legendsCalculationListener);
        add(Calculation.RANGE.getTitle(),
                "Calculate range on visible resources", isEnabled,
                legendsCalculationListener);
        add(Calculation.RANGE_IMAGE.getTitle(),
                "Calculate range image on visible resources", isEnabled,
                legendsCalculationListener);
        add(Calculation.SUMMATION.getTitle(),
                "Calculate summation on visible resources", isEnabled,
                legendsCalculationListener);
        add(Calculation.SUMMATION_IMAGE.getTitle(),
                "Calculate summation image on visible resources", isEnabled,
                legendsCalculationListener);
        add(Calculation.STANDARD_DEVIATION.getTitle(),
                "Calculate standard deviation on visible resources", isEnabled,
                legendsCalculationListener);
        add(Calculation.STANDARD_DEVIATION_IMAGE.getTitle(),
                "Calculate standard deviation image on visible resources",
                isEnabled, legendsCalculationListener);
        add(Calculation.VALUE_SAMPLING.getTitle(), "Turn on value sampling",
                isEnabled, legendsCalculationListener);
        add(Calculation.HISTOGRAM_SAMPLING.getTitle(),
                "Turn on text histogram sampling", isEnabled,
                legendsCalculationListener);
        add(Calculation.HISTOGRAM_GRAPHICS.getTitle(),
                "Turn on distribution viewer sampling", isEnabled,
                legendsCalculationListener);

        new MenuItem(dropdownMenu, SWT.SEPARATOR);

        MenuItem mi = new MenuItem(dropdownMenu, SWT.NONE);
        mi.setText(GlobalPreferencesComposite.PREFERENCES_NAME);
        mi.setEnabled(true);
        mi.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                prefsDialog = new PreferencesDialog(getParent().getShell());
                if (prefsDialog.open() == Window.OK) {
                    prefsDialog.close();
                    prefsDialog = null;
                }

            }
        });
    }

    private void addLegendsTimeSeriesItems() {
        removeAllMenuItems();

        new MenuItem(dropdownMenu, SWT.SEPARATOR);

        boolean isEnabled = isToolEnabled();

        add(Calculation.MEAN.getTitle(), "Calculate mean of visible resources",
                true, legendsCalculationListener);
        add(Calculation.MIN.getTitle(),
                "Calculate minimum on visible resources", isEnabled,
                legendsCalculationListener);
        add(Calculation.MAX.getTitle(),
                "Calculate maxixum on visible resources", isEnabled,
                legendsCalculationListener);
        add(Calculation.MEDIAN.getTitle(),
                "Calculate median on visible resources", isEnabled,
                legendsCalculationListener);
        add(Calculation.RANGE.getTitle(),
                "Calculate range on visible resources", isEnabled,
                legendsCalculationListener);

        new MenuItem(dropdownMenu, SWT.SEPARATOR);

        MenuItem mi = new MenuItem(dropdownMenu, SWT.NONE);
        mi.setText(GlobalPreferencesComposite.PREFERENCES_NAME);
        mi.setEnabled(true);
        mi.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                prefsDialog = new PreferencesDialog(getParent().getShell());
                if (prefsDialog.open() == Window.OK) {
                    prefsDialog.close();
                    prefsDialog = null;
                }

            }
        });
    }

    /**
     * TODO: No tool bar drop down menu items for the Matrix tool. Save for
     * future use.
     */
    protected void addMatrixItems() {
        removeAllMenuItems();
        new MenuItem(dropdownMenu, SWT.SEPARATOR);
    }

    protected void removeAllMenuItems() {
        for (MenuItem mi : dropdownMenu.getItems()) {
            mi.dispose();
        }
    }

    protected void add(String item, String tooltip, boolean isEnabled,
            SelectionAdapter selectionListener) {
        MenuItem mi = new MenuItem(dropdownMenu, SWT.NONE);
        mi.setText(item);
        mi.setEnabled(isEnabled);
        mi.addSelectionListener(selectionListener);
    }

    private class EnsembleToolItemActionDropdown extends SelectionAdapter {

        public EnsembleToolItemActionDropdown(ToolBar parentToolBar) {
            actionsDropdownToolItem = new ToolItem(parentToolBar,
                    SWT.DROP_DOWN);
            actionsDropdownToolItem.setImage(EnsembleToolImageStore.GEAR_IMG);
            actionsDropdownToolItem.addSelectionListener(this);
            actionsDropdownToolItem.setToolTipText("Actions");
            dropdownMenu = new Menu(
                    actionsDropdownToolItem.getParent().getShell());
        }

        public void setToolMode(EnsembleToolMode mode) {
            if (mode == EnsembleToolMode.LEGENDS_PLAN_VIEW) {
                addLegendsPlanViewItems();
            } else if (mode == EnsembleToolMode.LEGENDS_TIME_SERIES) {
                addLegendsTimeSeriesItems();
            } else if (mode == EnsembleToolMode.MATRIX) {
                addMatrixItems();
            }
        }

        public void setEnabled(boolean isEnabled) {
            actionsDropdownToolItem.setEnabled(isEnabled);
        }

        /*
         * This is the selection listener that acts when the tool bar "actions"
         * drop down is selected.
         */
        public void widgetSelected(SelectionEvent event) {

            setToolMode(EnsembleTool.getInstance().getToolMode());

            if ((event.detail == SWT.ARROW)
                    || (event.detail == SWT.MENU_MOUSE)) {
                ToolItem item = (ToolItem) event.widget;
                Rectangle rect = item.getBounds();
                Point pt = item.getParent()
                        .toDisplay(new Point(rect.x, rect.y));
                dropdownMenu.setLocation(pt.x, pt.y + rect.height);
                dropdownMenu.setVisible(true);
            }
        }

    }

    class LegendsBrowserCalculationSelectionAdapter extends SelectionAdapter {

        public void widgetSelected(SelectionEvent event) {
            MenuItem selected = (MenuItem) event.widget;

            for (Calculation c : Calculation.values()) {
                if (c.getTitle().equals(selected.getText())) {
                    EnsembleTool.getInstance().calculate(c);
                }
            }
        }
    }

    /**
     * Convenience method to make certain all tree components are not null and
     * not disposed.
     * 
     * @return
     */
    public boolean isWidgetReady() {
        boolean isReady = false;

        if (toolBar != null && !toolBar.isDisposed() && browserToolItem != null
                && !browserToolItem.isDisposed() && powerToggleToolItem != null
                && !powerToggleToolItem.isDisposed()
                && clearAllEntriesToolItem != null
                && !clearAllEntriesToolItem.isDisposed() && dropdownMenu != null
                && !dropdownMenu.isDisposed() && actionsDropdownToolItem != null
                && !actionsDropdownToolItem.isDisposed()) {
            isReady = true;
        }

        return isReady;
    }

}
