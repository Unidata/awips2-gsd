package gov.noaa.gsd.viz.ensemble.navigator.ui.viewer;

import gov.noaa.gsd.viz.ensemble.display.calculate.Calculation;
import gov.noaa.gsd.viz.ensemble.display.common.GeneratedGridResourceHolder;
import gov.noaa.gsd.viz.ensemble.display.common.GeneratedTimeSeriesResourceHolder;
import gov.noaa.gsd.viz.ensemble.display.common.GenericResourceHolder;
import gov.noaa.gsd.viz.ensemble.display.common.GridResourceHolder;
import gov.noaa.gsd.viz.ensemble.display.common.HistogramGridResourceHolder;
import gov.noaa.gsd.viz.ensemble.display.common.TimeSeriesResourceHolder;
import gov.noaa.gsd.viz.ensemble.navigator.ui.layer.EnsembleTool;
import gov.noaa.gsd.viz.ensemble.util.ChosenGEFSColors;
import gov.noaa.gsd.viz.ensemble.util.ChosenSREFColors;
import gov.noaa.gsd.viz.ensemble.util.EnsembleGEFSColorChooser;
import gov.noaa.gsd.viz.ensemble.util.EnsembleSREFColorChooser;
import gov.noaa.gsd.viz.ensemble.util.SWTResourceManager;
import gov.noaa.gsd.viz.ensemble.util.Utilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.NotEnabledException;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.TreeExpansionEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IPartService;
import org.eclipse.ui.ISaveablePart2;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.services.IServiceLocator;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.viz.core.VizApp;
import com.raytheon.uf.viz.core.drawables.IDescriptor.FramesInfo;
import com.raytheon.uf.viz.core.drawables.ResourcePair;
import com.raytheon.uf.viz.core.rsc.AbstractVizResource;
import com.raytheon.uf.viz.core.rsc.ResourceType;
import com.raytheon.uf.viz.core.rsc.capabilities.ColorableCapability;
import com.raytheon.uf.viz.core.rsc.capabilities.OutlineCapability;
import com.raytheon.uf.viz.xy.timeseries.TimeSeriesEditor;
import com.raytheon.viz.ui.EditorUtil;
import com.raytheon.viz.ui.editor.AbstractEditor;

/**
 * This class represents the Ensemble Tool navigator widget, which is an RCP
 * View (ViewPart). It contains all user interface control logic for those
 * features which are otherwise accomplished by the user through the main CAVE
 * (primary) editor pane (e.g. Map editor, TimeSeries editor, etc.).
 * 
 * Features in this class include the ability to:
 * 
 * 1) Behave as proxy for the current active CAVE "editable" ensemble tool; this
 * class is an RCP CAVE view that is coupled to the EnsembleTool which
 * associates an active RCP CAVE editor ("Map", "TimeSeries", etc.) to an
 * EnsembleToolLayer. This view will display the resources for the active
 * EnsembleToolLayer.
 * 
 * 2) Display resources for the active EnsembleToolLayer.
 * 
 * 3) Allow D/2D-style resource interactivity over a given currently displayed
 * resource. This would include all the things you can do to a viz resource via
 * the context-sensitive popup menu via the D2D legend (i.e. right-click on the
 * legend).
 * 
 * 4) Allow new derived resources to be created via well-known calculation
 * methods (mean, median, sum, probability, etc.), by using only non-hidden
 * resources.
 * 
 * 5) Allow users to change interactivity behavior via a Preferences control.
 * 
 * 6) Allow users to display meta-information for a selected resource via an
 * Information pane.
 * 
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Oct 08, 2014    5056      polster     Initial creation
 * Apr 21, 2015    7681      polster     Ignore toggle group visibility for 14.4.1
 * Apr 21, 2015    7684      polster     Fixes job which constantly checks for refresh
 * Apr 21, 2015    7682      polster     ERF 'below threshold' field entry fixed
 * Apr 21, 2015    7653      polster     Ctrl-MB1 selects viz resource again
 * 
 * </pre>
 * 
 * @author polster
 * @version 1.0
 */

public class EnsembleToolViewer extends ViewPart implements ISaveablePart2 {

    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(EnsembleToolViewer.class);

    public static final String ID = "gov.noaa.gsd.viz.ensemble.tool.viewer";

    public static final String NAME = "Ensembles";

    protected static boolean WAS_MAP_CHECKBOX_SELECTION = false;;

    private static int CLIENT_BODY_AREA_HEIGHT = 739;

    private boolean isDisabled = false;

    private TreeViewer ensembleTreeViewer = null;

    private Tree ensembleTree = null;

    private ArrayList<ColumnLabelProvider> columnLabelProviders = new ArrayList<ColumnLabelProvider>();

    private ScrolledComposite rootTabComposite = null;

    private CTabFolder tabEnsemblesMainTabFolder = null;

    private Composite lowerSashComposite = null;

    private ToolBar toolBar = null;

    private Font viewFont = null;

    private Font smallViewFont = null;

    private Composite planViewInfoTabComposite = null;

    private Composite timeSeriesInfoTabComposite = null;

    private Label timeSeriesPointLabelLbl = null;

    private Label timeSeriesPointValueLbl = null;

    private Label primaryRscTimeLbl = null;

    private Label frameTimeUsingBasisLbl = null;

    private Label timeMatchResourceLbl = null;

    private Label primaryRscLbl = null;

    private Composite thickenOnSelectionComposite = null;

    private Composite smallFlagsComposite = null;

    private Button thickenOnSelectionBtn = null;

    private Button useResourceColorRdo = null;

    private Button chooseColorRdo = null;

    private Label colorChooserLbl = null;

    private Label thicknessChooserLbl = null;

    private Spinner thicknessChooserSpinner = null;

    private Button editableOnRestoreBtn = null;

    private Button editableOnSwapInBtn = null;

    private Button uneditableOnMinimizeBtn = null;

    private Button minimizeOnForeignToolBtn = null;

    private Button minimizeOnToggleUneditableBtn = null;

    private Button createToolLayerOnNewEditorBtn = null;

    private Button autoHidePreferencesBtn = null;

    private boolean editableOnRestore = false;

    private boolean editableOnSwapIn = false;

    private boolean uneditableOnMinimize = false;

    private boolean minimizeOnForeignToolLoad = false;

    protected boolean minimizeOnToggleUneditable = false;

    protected boolean createToolLayerOnNewEditor = false;

    protected boolean autoHidePreferences = false;

    private CTabItem itemLegendsTabItem = null;

    private CTabItem itemMatrixTabItem = null;

    private TabFolder lowerSashTabFolder = null;

    private TabItem preferencesTabItem = null;

    private TabItem resourceInfoTabItem = null;

    private MenuItem addERFLayerMenuItem = null;

    private EnsembleViewPartListener viewPartListener = null;

    private ToolItem browserToolItem = null;

    private ToolItem powerToggleToolItem = null;

    /*
     * the current frame index based on the frameChanged event.
     */
    private FramesInfo currentFramesInfo;

    private ToolItem runToolItem = null;

    private boolean viewEditable = true;

    private ResourceType editorResourceType = ResourceType.PLAN_VIEW;

    private SashForm sashForm = null;

    public static int FEATURE_SET_COLUMN_INDEX = 1;

    public static int VISIBLE_COLUMN_INDEX = 2;

    public static int SAMPLING_COLUMN_INDEX = 3;

    private AbstractVizResource<?, ?> currentEnsembleRsc = null;

    private static AbstractVizResource<?, ?> LAST_HIGHLIGHTED_RESOURCE = null;

    private static RGB LAST_HIGHLIGHTED_RESOURCE_RGB = null;

    private static int LAST_HIGHLIGHTED_RESOURCE_WIDTH = 1;

    private static boolean LAST_HIGHLIGHTED_RESOURCE_OUTLINE_ASSERTED = false;

    private Color thickenOnSelectionColor = SWTResourceManager.PASTEL_LIGHT_BLUE;

    private boolean thickenOnSelection = true;

    private boolean useResourceColorOnThicken = true;

    private int thickenWidth = 4;

    protected Composite ownerComposite = null;

    final private ITreeViewerListener expandCollapseListener = new EnsembleTreeExpandCollapseListener();

    private ERFProductDialog_Modal erfDialog = null;

    private static final Color ENABLED_FOREGROUND_COLOR = SWTResourceManager.BLACK;

    private static final Color DISABLED_FOREGROUND_COLOR = SWTResourceManager.MEDIUM_GRAY;

    protected TreeItem calculationSelectedTreeItem = null;

    private Menu calculationMenu = null;

    private Cursor selectionModeCursor = null;

    private Cursor normalCursor = null;

    private Cursor waitCursor = null;

    private List<Image> imageCache = null;

    private final long elegantWaitPeriod = 100;

    /* class scope for accessibility from inner anonymous class */
    protected TreeItem foundTreeItem = null;

    /* class scope for accessibility from inner anonymous class */
    protected TreeItem[] directDescendants = null;

    private static boolean isDisposing = false;

    public static boolean isDisposing() {
        return isDisposing;
    }

    public static void setDisposing(boolean id) {
        isDisposing = id;
    }

    public EnsembleToolViewer() {

        minimizeOnForeignToolLoad = true;
        editableOnRestore = true;
        autoHidePreferences = true;

        imageCache = new ArrayList<>();

    }

    /**
     * This is a call-back that will allow us to create the viewer and
     * initialize it.
     * 
     * @param parent
     */
    public void createPartControl(Composite parent) {

        ownerComposite = parent;

        /*
         * TODO: This colorized backdrop should happen not by being hard-coded
         * but via state change (ie. by calling setEnabled).
         */
        ownerComposite.setBackground(SWTResourceManager.PALE_DULL_AZURE);

        /* create the defaut icons when the view is initially opened */
        EnsembleToolViewerImageStore.constructImages();

        viewFont = SWTResourceManager.getFont("Dialog", 9, SWT.NONE);
        smallViewFont = SWTResourceManager.getFont("Dialog", 8, SWT.NONE);

        GridLayout parent_gl = new GridLayout(1, false);
        parent_gl.marginHeight = 2;
        parent_gl.marginWidth = 2;
        parent.setLayout(parent_gl);

        GridData parent_gd = new GridData(SWT.FILL, SWT.CENTER, true, true, 2,
                3);
        parent.setLayoutData(parent_gd);

        /* the main container holds the resource "legends" and is scrollable */
        rootTabComposite = new ScrolledComposite(parent, SWT.BORDER
                | SWT.H_SCROLL | SWT.V_SCROLL);

        /* this is the root grid layout of all grid layouts */
        GridLayout tabContainer_gl = new GridLayout();
        tabContainer_gl.marginWidth = 2;
        tabContainer_gl.marginHeight = 2;
        GridData tabContainer_gd = new GridData(SWT.FILL, SWT.FILL, true, true,
                1, 1);
        tabContainer_gd.heightHint = CLIENT_BODY_AREA_HEIGHT;
        rootTabComposite.setLayout(tabContainer_gl);
        rootTabComposite.setLayoutData(tabContainer_gd);

        /*
         * main tab container to allow user to switch between legends, matrix,
         * and future ideas ...
         */
        tabEnsemblesMainTabFolder = new CTabFolder(rootTabComposite, SWT.TOP
                | SWT.BORDER);
        tabEnsemblesMainTabFolder.setFont(viewFont);
        tabEnsemblesMainTabFolder
                .setSelectionBackground(SWTResourceManager.PALE_LIGHT_AZURE);

        /* here's the toolbar */
        Composite toolbarComposite = new Composite(tabEnsemblesMainTabFolder,
                SWT.NONE);
        toolbarComposite.setBackground(SWTResourceManager.WHITE);

        FillLayout toolbarContainer_fl = new FillLayout(SWT.HORIZONTAL);
        toolbarContainer_fl.marginWidth = 1;
        toolbarContainer_fl.marginHeight = 1;
        toolbarComposite.setLayout(toolbarContainer_fl);

        /* fill the tool bar */
        toolBar = makeToolBar(toolbarComposite);
        Rectangle r = toolbarComposite.getBounds();
        r.height = r.height + 32;
        toolbarComposite.setBounds(r);

        tabEnsemblesMainTabFolder.setTabHeight(42);
        tabEnsemblesMainTabFolder.setTopRight(toolbarComposite);
        tabEnsemblesMainTabFolder.setFont(SWTResourceManager.getFont(
                "SansSerif", 10, SWT.NONE));

        tabEnsemblesMainTabFolder
                .addSelectionListener(new UpperTabSelectionListener());

        /* tab entry for Legends */
        itemLegendsTabItem = new CTabItem(tabEnsemblesMainTabFolder, SWT.NONE);
        itemLegendsTabItem
                .setImage(EnsembleToolViewerImageStore.TAB_LEGENDS_ENABLED_UNSELECTED_IMG);
        // tabItemLegends.setText("  Legends  ");

        /* tab entry for Matrix */
        itemMatrixTabItem = new CTabItem(tabEnsemblesMainTabFolder, SWT.NONE);
        itemMatrixTabItem
                .setImage(EnsembleToolViewerImageStore.TAB_MATRIX_ENABLED_UNSELECTED_IMG);
        // tabItemMatrix.setText("  Matrix  ");
        /*
         * let's have an upper sash and lower sash that the user can resize
         * vertically (see SWT concept of sash)
         */

        sashForm = new SashForm(tabEnsemblesMainTabFolder, SWT.BORDER);
        sashForm.setOrientation(SWT.VERTICAL);

        rootTabComposite.setContent(tabEnsemblesMainTabFolder);
        rootTabComposite.setMinSize(tabEnsemblesMainTabFolder.computeSize(
                SWT.DEFAULT, SWT.DEFAULT));

        /* upper sash contains the resource "legend" tree. */
        ensembleTree = new Tree(sashForm, SWT.BORDER | SWT.H_SCROLL
                | SWT.V_SCROLL | SWT.SINGLE | SWT.FULL_SELECTION);
        ensembleTree.setLinesVisible(true);
        ensembleTree.setHeaderVisible(false);
        ensembleTreeViewer = new TreeViewer(ensembleTree);
        createColumns(ensembleTreeViewer);

        /* keep track of the collapse/expand state */
        ensembleTreeViewer.addTreeListener(expandCollapseListener);

        /* recognize when the user clicks on something in the tree */
        ensembleTree.addMouseListener(new EnsembleTreeMouseListener());

        /*
         * the lower sash contains a composite which itself contains a tab
         * folder (i.e. set of tabs in one container) ...
         */
        lowerSashComposite = new Composite(sashForm, SWT.BORDER_SOLID);
        lowerSashComposite.setBackground(SWTResourceManager.LIGHT_GRAY);
        GridLayout lowerSash_gl = new GridLayout();
        lowerSashComposite.setLayout(lowerSash_gl);
        GridData lowerSash_gd = new GridData(SWT.FILL, SWT.FILL, true, true, 1,
                1);
        lowerSash_gd.horizontalIndent = 1;
        lowerSash_gd.verticalIndent = 1;
        lowerSashComposite.setLayoutData(lowerSash_gd);

        /*
         * put the Info, ERF, and Preferences tabs in the lower sash container
         */
        fillLowerSash(lowerSashComposite);

        /* what is the best ratio of visibility of upper vs. lower sash */
        sashForm.setWeights(new int[] { 66, 33 });

        itemLegendsTabItem.setControl(sashForm);

        /* fill the contents of the tree with */
        ensembleTreeViewer
                .setContentProvider(new EnsembleTreeContentProvider());
        ensembleTreeViewer.setSorter(new EnsembleTreeSorter());

        ensembleTree.addMouseTrackListener(new MouseTrackAdapter() {

            @Override
            public void mouseEnter(MouseEvent e) {
                switchToInfoTabIfAutoHide();
            }

        });

        rootTabComposite.setExpandHorizontal(true);
        rootTabComposite.setExpandVertical(true);

        tabEnsemblesMainTabFolder.setSelection(itemLegendsTabItem);

        parent.pack();

        if (viewPartListener == null) {
            viewPartListener = new EnsembleViewPartListener(this);
            IWorkbenchWindow window = PlatformUI.getWorkbench()
                    .getActiveWorkbenchWindow();
            IPartService service = window.getPartService();
            service.addPartListener(viewPartListener);
        }

        selectionModeCursor = ownerComposite.getDisplay().getSystemCursor(
                SWT.CURSOR_HAND);

        normalCursor = ownerComposite.getDisplay().getSystemCursor(
                SWT.CURSOR_ARROW);

        waitCursor = ownerComposite.getDisplay().getSystemCursor(
                SWT.CURSOR_WAIT);

        ensembleTree.addKeyListener(new EnsembleTreeKeyListener());
        updateCursor(normalCursor);

    }

    /*
     * Keep track of the resource type of the current editor
     */
    public void setEditorType(ResourceType rt) {
        editorResourceType = rt;
    }

    /*
     * Grab all root items and all descendants from the tree and return true if
     * any entry is "toggled on" meaning that the rsc.isVisible() == true
     */
    protected boolean anyChildrenToggleOn(String productName) {
        boolean anyChildrenToggledOn = false;

        TreeItem parentItem = findTreeItemByLabelName(productName);

        List<TreeItem> descendants = new ArrayList<TreeItem>();
        getAllDescendants(parentItem, descendants);

        /*
         * TODO: this test is due to the fact that for some reason the ensemble
         * children aren't being initially recognized as actual members of the
         * ensemble root TreeItem even though they are really there.
         */
        if (descendants.size() == 0) {
            return true;
        }

        for (TreeItem ti : descendants) {
            Object data = ti.getData();
            if (data == null) {
                anyChildrenToggledOn = true;
                break;
            }
            if (data instanceof GenericResourceHolder) {
                GenericResourceHolder gr = (GenericResourceHolder) data;
                AbstractVizResource<?, ?> rsc = gr.getRsc();
                if (rsc.getProperties().isVisible()) {
                    anyChildrenToggledOn = true;
                    break;
                }
            }

        }
        return anyChildrenToggledOn;
    }

    /*
     * The tree items in the tree are grouped by a top level ensemble name whose
     * children are all ensemble members (viz-resources). Visibility defines
     * whether the resource is visible on the main CAVE map, or not.
     * 
     * If any child tree item (viz resource) is visible then the parent tree
     * item (ensemble name) must also be NOT-grayed-out. Likewise, if all
     * children resources of a given parent are invisible then the parent tree
     * item should be grayed out.
     */
    protected void matchParentToChildrenVisibility(TreeItem ci) {

        final TreeItem childItem = ci;
        VizApp.runSync(new Runnable() {

            @Override
            public void run() {

                if (!isViewerTreeReady()) {
                    return;
                }
                if (childItem == null) {
                    return;
                }
                if (childItem.isDisposed()) {
                    return;
                }
                if (childItem.getParentItem() == null) {
                    return;
                }
                if (childItem.getParentItem().isDisposed()) {
                    return;
                }
                final TreeItem parentItem = childItem.getParentItem();

                final Object d = parentItem.getData();
                if (d instanceof String) {

                    List<TreeItem> descendants = new ArrayList<TreeItem>();
                    getAllDescendants(parentItem, descendants);

                    boolean ai = true;

                    for (TreeItem ti : descendants) {
                        Object data = ti.getData();
                        if (data instanceof GenericResourceHolder) {
                            GenericResourceHolder gr = (GenericResourceHolder) data;
                            AbstractVizResource<?, ?> rsc = gr.getRsc();
                            if (rsc.getProperties().isVisible()) {
                                ai = false;
                                break;
                            }
                        }
                    }

                    final boolean allInvisible = ai;

                    /* if all invisible then make sure the parent is grayed-out. */
                    if (allInvisible) {
                        parentItem
                                .setForeground(EnsembleToolViewer.DISABLED_FOREGROUND_COLOR);
                        if (isViewerTreeReady()) {
                            ensembleTreeViewer.getTree().deselectAll();
                        }
                    }
                    /*
                     * otherwise, if any one item is visible then make sure the
                     * parent is normalized.
                     */

                    else {
                        parentItem
                                .setForeground(EnsembleToolViewer.ENABLED_FOREGROUND_COLOR);
                        if (isViewerTreeReady()) {
                            ensembleTreeViewer.getTree().deselectAll();
                        }
                    }
                }
            }
        });
    }

    /*
     * This searches only root level items to see if a root item of a given name
     * has any children (ensemble members) and, if so, returns those children.
     */
    private TreeItem findTreeItemByLabelName(final String name) {

        VizApp.runSync(new Runnable() {

            @Override
            public void run() {
                TreeItem[] allRoots = ensembleTree.getItems();

                for (TreeItem ti : allRoots) {
                    if (ti.getData() instanceof String) {
                        String treeItemLabelName = (String) ti.getData();
                        if (treeItemLabelName.equals(name)) {
                            foundTreeItem = ti;
                            break;
                        }
                    }
                }
            }
        });
        return foundTreeItem;

    }

    /*
     * Return the first tree item that equals the passed in
     * GenericResourceHolder.
     */
    private TreeItem findTreeItemByResource(GenericResourceHolder rsc) {

        TreeItem foundItem = null;
        TreeItem[] allRoots = ensembleTree.getItems();

        for (TreeItem ti : allRoots) {
            if (foundItem != null)
                break;
            Object tio = ti.getData();
            if ((tio != null) && (tio instanceof GenericResourceHolder)) {
                GenericResourceHolder gr = (GenericResourceHolder) tio;
                if (gr == rsc) {
                    foundItem = ti;
                    break;
                }
            } else if ((tio != null) && (tio instanceof String)) {
                TreeItem[] children = ti.getItems();
                for (TreeItem treeItem : children) {
                    Object o = treeItem.getData();
                    if ((o != null) && (o instanceof GenericResourceHolder)) {
                        GenericResourceHolder gr = (GenericResourceHolder) o;
                        if (gr == rsc) {
                            foundItem = treeItem;
                            break;
                        }
                    }
                }
            }
        }
        return foundItem;
    }

    private void createColumns(TreeViewer ensembleTableViewer) {

        TreeViewerColumn column = new TreeViewerColumn(ensembleTableViewer,
                SWT.LEFT);
        column.getColumn().setWidth(263);
        column.getColumn().setMoveable(false);
        column.getColumn().setText("   Legends");
        column.getColumn().setAlignment(SWT.LEFT);

        EnsembleTreeColumnLabelProvider clp = new EnsembleTreeColumnLabelProvider();
        columnLabelProviders.add(clp);
        column.setLabelProvider(clp);

    }

    /*
     * Clean up all resources, fonts, providers, etc.
     * 
     * @see org.eclipse.ui.part.WorkbenchPart#dispose()
     */
    @Override
    public void dispose() {

        super.dispose();

        EnsembleToolViewer.setDisposing(true);

        for (Image img : imageCache) {
            img.dispose();
        }

        if (columnLabelProviders != null) {
            for (ColumnLabelProvider clp : columnLabelProviders) {
                clp.dispose();
            }
        }
        if (erfDialog != null) {
            erfDialog.close();
        }

        if (isViewerTreeReady()) {
            ensembleTree.removeAll();
            ensembleTree.dispose();
            ensembleTree = null;
        }
        if (ensembleTreeViewer != null) {
            ensembleTreeViewer = null;
        }

        SWTResourceManager.dispose();

        EnsembleToolViewer.setDisposing(false);

    }

    public void disableTool() {
        setViewEditable(false);
        powerToggleToolItem.setEnabled(false);
        isDisabled = true;
    }

    public boolean isDisabled() {
        return isDisabled;
    }

    synchronized public boolean isEnabled() {
        return viewEditable;
    }

    /*
     * The ViewPart should be enabled when an ensemble tool layer is editable in
     * the currently active editor, and disabled when the tool layer is set to
     * 'not editable'.
     */
    synchronized public void setViewEditable(boolean enabled) {

        isDisabled = false;
        viewEditable = enabled;

        VizApp.runSync(new Runnable() {

            @Override
            public void run() {
                if (viewEditable) {
                    powerToggleToolItem
                            .setImage(EnsembleToolViewerImageStore.POWER_ON_IMG);
                    powerToggleToolItem.setToolTipText("Tool Off");
                    ownerComposite
                            .setBackground(SWTResourceManager.PALE_DULL_AZURE);
                    tabEnsemblesMainTabFolder.setSelection(itemLegendsTabItem);
                    resourceInfoTabItem
                            .setImage(EnsembleToolViewerImageStore.TAB_INFO_ENABLED_IMG);
                    preferencesTabItem
                            .setImage(EnsembleToolViewerImageStore.TAB_OPTIONS_ENABLED_IMG);
                    itemLegendsTabItem
                            .setImage(EnsembleToolViewerImageStore.TAB_LEGENDS_ENABLED_SELECTED_IMG);
                    itemMatrixTabItem
                            .setImage(EnsembleToolViewerImageStore.TAB_MATRIX_ENABLED_UNSELECTED_IMG);
                    lowerSashComposite
                            .setBackground(SWTResourceManager.PALE_DULL_AZURE);

                } else {
                    powerToggleToolItem
                            .setImage(EnsembleToolViewerImageStore.POWER_OFF_IMG);
                    powerToggleToolItem.setToolTipText("Tool On");
                    ownerComposite.setBackground(SWTResourceManager.LIGHT_GRAY);
                    resourceInfoTabItem
                            .setImage(EnsembleToolViewerImageStore.TAB_INFO_DISABLED_IMG);
                    preferencesTabItem
                            .setImage(EnsembleToolViewerImageStore.TAB_OPTIONS_DISABLED_IMG);
                    itemLegendsTabItem
                            .setImage(EnsembleToolViewerImageStore.TAB_LEGENDS_DISABLED_IMG);
                    itemMatrixTabItem
                            .setImage(EnsembleToolViewerImageStore.TAB_MATRIX_DISABLED_IMG);
                    ensembleTreeViewer.getTree().deselectAll();
                    lowerSashComposite
                            .setBackground(SWTResourceManager.LIGHT_GRAY);
                }

                thickenOnSelectionComposite.setEnabled(viewEditable);
                smallFlagsComposite.setEnabled(viewEditable);
                thickenOnSelectionBtn.setEnabled(viewEditable);
                useResourceColorRdo.setEnabled(viewEditable);
                chooseColorRdo.setEnabled(viewEditable);
                colorChooserLbl.setEnabled(viewEditable);
                thicknessChooserLbl.setEnabled(viewEditable);
                thicknessChooserSpinner.setEnabled(viewEditable);

                editableOnRestoreBtn.setEnabled(viewEditable);
                // btnEditableOnSwapIn.setEnabled(viewEditable);
                // btnUneditableOnMinimize.setEnabled(viewEditable);
                // btnCreateToolLayerOnNewEditor.setEnabled(viewEditable);
                minimizeOnForeignToolBtn.setEnabled(viewEditable);
                minimizeOnToggleUneditableBtn.setEnabled(viewEditable);
                autoHidePreferencesBtn.setEnabled(viewEditable);

                createToolLayerOnNewEditorBtn.setEnabled(false);
                editableOnSwapInBtn.setEnabled(false);
                uneditableOnMinimizeBtn.setEnabled(false);

                primaryRscLbl.setEnabled(viewEditable);
                primaryRscTimeLbl.setEnabled(viewEditable);
                timeMatchResourceLbl.setEnabled(viewEditable);
                frameTimeUsingBasisLbl.setEnabled(viewEditable);
                lowerSashComposite.setEnabled(viewEditable);
                ensembleTreeViewer.getTree().setEnabled(viewEditable);
                browserToolItem.setEnabled(viewEditable);
                runToolItem.setEnabled(viewEditable);

                /* Always on items */
                toolBar.setEnabled(true);
                powerToggleToolItem.setEnabled(true);

                if (isViewerTreeReady()) {
                    // ensemblesTreeViewer.refresh(true);
                    tabEnsemblesMainTabFolder.getSelection().reskin(SWT.ALL);
                    tabEnsemblesMainTabFolder.redraw();
                }
            }
        });
    }

    /*
     * Given a tree item, find the item in the tree and toggle it's visibility
     * state.
     */
    private void toggleItemVisible(TreeItem item) {

        final TreeItem finalItem = item;

        VizApp.runSync(new Runnable() {

            @Override
            public void run() {

                if (finalItem.isDisposed()) {
                    return;
                }
                final Object mousedItem = finalItem.getData();
                if (mousedItem instanceof String) {

                    Color fg = finalItem.getForeground();
                    boolean isVisible = false;

                    /*
                     * Awkward way of seeing if the item has been already
                     * grayed-out. This needs to be further evaluated for a
                     * better solution.
                     */
                    if (fg.getRGB().equals(DISABLED_FOREGROUND_COLOR.getRGB())) {
                        isVisible = false;
                    } else {
                        isVisible = true;
                    }

                    /* if it was on turn it off */
                    if (isVisible) {
                        finalItem
                                .setForeground(EnsembleToolViewer.DISABLED_FOREGROUND_COLOR);
                        if (isViewerTreeReady()) {
                            ensembleTreeViewer.getTree().deselectAll();
                        }
                    }
                    /* if it was off turn it on */
                    else {
                        finalItem
                                .setForeground(EnsembleToolViewer.ENABLED_FOREGROUND_COLOR);
                        if (isViewerTreeReady()) {
                            ensembleTreeViewer.getTree().deselectAll();
                        }
                    }

                } else if (mousedItem instanceof GenericResourceHolder) {

                    GenericResourceHolder gr = (GenericResourceHolder) mousedItem;
                    boolean isVisible = gr.getRsc().getProperties().isVisible();
                    /* toggle visibility */
                    isVisible = !isVisible;
                    gr.getRsc().getProperties().setVisible(isVisible);
                    gr.getRsc().issueRefresh();
                    /* update tree item to reflect new state */
                    if (isVisible) {
                        finalItem
                                .setForeground(EnsembleToolViewer.ENABLED_FOREGROUND_COLOR);
                    } else {
                        finalItem
                                .setForeground(EnsembleToolViewer.DISABLED_FOREGROUND_COLOR);
                    }
                    if (isViewerTreeReady()) {
                        ensembleTreeViewer.getTree().deselectAll();
                    }
                }
            }
        });
    }

    protected TreeItem[] getDirectDescendants(TreeItem ri) {

        final TreeItem rootItem = ri;
        VizApp.runSync(new Runnable() {

            @Override
            public void run() {
                directDescendants = rootItem.getItems();
            }

        });

        return directDescendants;
    }

    /*
     * Return all descendants of a given root tree item.
     */
    private List<TreeItem> getAllDescendants(TreeItem rootItem,
            List<TreeItem> descendants) {

        TreeItem[] children = rootItem.getItems();
        List<TreeItem> immediateChildren = Arrays.asList(children);
        for (TreeItem child : immediateChildren) {
            descendants = getAllDescendants(child, descendants);
            descendants.add(child);
        }
        return descendants;
    }

    /*
     * This is the content provider for the tree.
     */
    private class EnsembleTreeContentProvider implements ITreeContentProvider {

        @Override
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            // do nothing
        }

        @Override
        @SuppressWarnings("unchecked")
        public Object[] getElements(Object inputElement) {

            // The root elements of the tree are all strings representing
            // the grid product name ... we get the map from the Ensemble-
            // ToolManager. Create a primitive array of Objects having
            // those names.

            Map<String, List<GenericResourceHolder>> allLoadedProducts = (Map<String, List<GenericResourceHolder>>) inputElement;
            if ((allLoadedProducts == null) || (allLoadedProducts.size() == 0)) {
                return new Object[0];
            }

            Object[] members = new Object[allLoadedProducts.size()];

            List<GenericResourceHolder> currResources = null;
            Set<String> loadedProductNames = allLoadedProducts.keySet();
            Iterator<String> iterator = loadedProductNames.iterator();
            String currName = null;
            for (int i = 0; iterator.hasNext(); i++) {
                currName = iterator.next();
                if (currName == null || currName.equals("")) {
                    continue;
                }
                currResources = allLoadedProducts.get(currName);
                // is this resource empty?
                if (currResources.size() == 0) {
                    continue;
                }
                // is this an individual resource?
                else if (currResources.size() == 1) {
                    members[i] = currResources.get(0);
                }
                // otherwise it is an ensemble resource (1-to-many ensemble
                // members)
                else {
                    members[i] = currName;
                }
            }

            return members;
        }

        @Override
        public Object[] getChildren(Object parentElement) {

            // The children of a top-level product are currently only
            // perturbation members of an ensemble product. Find all
            // children, which are guaranteed to be of Class type
            // GenericResourceHolder, and return them in a primitive
            // array of Objects.
            Map<String, List<GenericResourceHolder>> ensembles = EnsembleTool
                    .getInstance().getEnsembleResources();

            if ((ensembles == null) || (ensembles.size() == 0)
                    || parentElement == null) {
                return new Object[0];
            }

            Object[] members = null;
            if (String.class.isAssignableFrom(parentElement.getClass())) {

                String ensembleName = (String) parentElement;
                if (ensembleName.length() == 0) {
                    return new Object[0];
                }
                List<GenericResourceHolder> resources = ensembles
                        .get(ensembleName);
                members = new Object[resources.size()];
                int i = 0;
                for (GenericResourceHolder rsc : resources) {
                    members[i++] = rsc;
                }
            } else if (GenericResourceHolder.class
                    .isAssignableFrom(parentElement.getClass())) {
                members = new Object[0];
            }
            return members;
        }

        @Override
        public Object getParent(Object element) {

            // Given an item from the tree, return its parent
            // in the tree. Currently, only perturbation members
            // can have parents, so the Object that gets returned
            // is guaranteed to be an ensemble product. This is
            // not critical to the logic that follows but just
            // an FYI.
            String parentEnsembleName = null;
            boolean parentFound = false;
            if (GenericResourceHolder.class
                    .isAssignableFrom(element.getClass())) {
                GenericResourceHolder targetRsc = (GenericResourceHolder) element;
                Map<String, List<GenericResourceHolder>> ensembles = EnsembleTool
                        .getInstance().getEnsembleResources();
                Set<Entry<String, List<GenericResourceHolder>>> entries = ensembles
                        .entrySet();
                Iterator<Entry<String, List<GenericResourceHolder>>> iterator = entries
                        .iterator();
                Entry<String, List<GenericResourceHolder>> currChild = null;
                while (iterator.hasNext() && !parentFound) {
                    currChild = iterator.next();
                    List<GenericResourceHolder> resources = currChild
                            .getValue();
                    for (GenericResourceHolder gr : resources) {
                        if (gr == targetRsc) {
                            parentFound = true;
                            parentEnsembleName = currChild.getKey();
                        }
                    }
                }
            }
            return parentEnsembleName;
        }

        @Override
        public boolean hasChildren(Object element) {

            // Currently, if the given element has children than it is
            // an ensemble product. Get the map of resources from the
            // EnsembleTool and see whether the element
            // given is an ensemble product (by having an associated
            // "not empty" list ...
            boolean hasChildren = false;
            Map<String, List<GenericResourceHolder>> ensembles = EnsembleTool
                    .getInstance().getEnsembleResources();
            if (String.class.isAssignableFrom(element.getClass())) {
                String ensembleName = (String) element;
                List<GenericResourceHolder> resources = ensembles
                        .get(ensembleName);
                if ((resources != null) && (resources.size() > 0)) {
                    hasChildren = true;
                }
            } else {
                // currently no other class types have children
            }
            return hasChildren;
        }

        @Override
        public void dispose() {
        }
    }

    private void switchToInfoTabIfAutoHide() {
        if (lowerSashTabFolder != null && !lowerSashTabFolder.isDisposed()) {
            if (autoHidePreferences) {
                lowerSashTabFolder.setSelection(resourceInfoTabItem);
            }
        }
    }

    /*
     * Create the ViewPart's main tool bar.
     */
    private ToolBar makeToolBar(Composite parent) {

        toolBar = new ToolBar(parent, SWT.BORDER_SOLID);

        browserToolItem = new ToolItem(toolBar, SWT.PUSH);
        browserToolItem
                .setImage(EnsembleToolViewerImageStore.VOLUME_BROWSER_IMG);
        browserToolItem.setToolTipText("Volume Browser");
        browserToolItem.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {

                IServiceLocator serviceLocator = PlatformUI.getWorkbench();
                ICommandService commandService = (ICommandService) serviceLocator
                        .getService(ICommandService.class);

                Command command = commandService
                        .getCommand("com.raytheon.viz.volumebrowser.volumeBrowserRef");

                /**
                 * Optionally pass a ExecutionEvent instance, default (empty)
                 * signature creates blank event
                 */
                try {
                    command.executeWithChecks(new ExecutionEvent());
                } catch (ExecutionException e1) {
                    statusHandler.warn(e1.getLocalizedMessage()
                            + "; Unable to open Volume Browser");
                } catch (NotDefinedException e1) {
                    statusHandler.warn(e1.getLocalizedMessage()
                            + "; Unable to open Volume Browser");
                } catch (NotEnabledException e1) {
                    statusHandler.warn(e1.getLocalizedMessage()
                            + "; Unable to open Volume Browser");
                } catch (NotHandledException e1) {
                    statusHandler.warn(e1.getLocalizedMessage()
                            + "; Unable to open Volume Browser");
                }

            }

        });

        ToolItem separator_00 = new ToolItem(toolBar, SWT.SEPARATOR);
        separator_00.setWidth(2);

        runToolItem = new ToolItem(toolBar, SWT.DROP_DOWN);
        runToolItem.setImage(EnsembleToolViewerImageStore.GEAR_IMG);
        runToolItem.setToolTipText("Run Calculation");
        runToolItem.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {

                AbstractEditor editor = EditorUtil
                        .getActiveEditorAs(AbstractEditor.class);

                boolean isTimeSeries = (editor instanceof TimeSeriesEditor);

                if (calculationMenu != null) {
                    MenuItem[] items = calculationMenu.getItems();
                    for (MenuItem item : items) {
                        if (item.getText().equals(
                                Calculation.STANDARD_DEVIATION.getTitle())
                                || item.getText().equals(
                                        Calculation.HISTOGRAM_SAMPLING
                                                .getTitle())
                                || item.getText().equals(
                                        Calculation.SUMMATION.getTitle())
                                || item.getText().equals(
                                        Calculation.VALUE_SAMPLING.getTitle())) {
                            if (isTimeSeries) {
                                item.setEnabled(false);
                            } else {
                                item.setEnabled(true);
                            }
                        }
                    }
                }
            }

        });

        ToolItem separator_3 = new ToolItem(toolBar, SWT.SEPARATOR);
        separator_3.setWidth(0);

        EnsembleToolItemActionListener ecl = new EnsembleToolItemActionListener(
                runToolItem);

        ecl.addSeparator();
        ecl.add(Calculation.MEAN.getTitle(), true);
        ecl.add(Calculation.MIN.getTitle(), true);
        ecl.add(Calculation.MAX.getTitle(), true);
        ecl.add(Calculation.MEDIAN.getTitle(), true);
        ecl.add(Calculation.RANGE.getTitle(), true);
        ecl.add(Calculation.SUMMATION.getTitle(), true);
        ecl.add(Calculation.STANDARD_DEVIATION.getTitle(), true);
        ecl.add(Calculation.VALUE_SAMPLING.getTitle(), true);
        ecl.add(Calculation.HISTOGRAM_SAMPLING.getTitle(), true);

        runToolItem.addSelectionListener(ecl);

        powerToggleToolItem = new ToolItem(toolBar, SWT.PUSH);
        powerToggleToolItem.setImage(EnsembleToolViewerImageStore.POWER_ON_IMG);
        powerToggleToolItem.setToolTipText("Tool Off");
        powerToggleToolItem.setSelection(viewEditable);
        powerToggleToolItem.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                viewEditable = !viewEditable;
                EnsembleTool.getInstance().setEditable(viewEditable);
            }

        });

        parent.pack();

        return toolBar;
    }

    private class EnsembleToolItemActionListener extends SelectionAdapter {
        private ToolItem dropdown;

        public EnsembleToolItemActionListener(ToolItem dd) {
            dropdown = dd;
            calculationMenu = new Menu(dropdown.getParent().getShell());
        }

        public void addSeparator() {
            new MenuItem(calculationMenu, SWT.SEPARATOR);
        }

        public void add(String item, boolean isEnabled) {
            MenuItem mi = new MenuItem(calculationMenu, SWT.NONE);
            mi.setText(item);
            mi.setEnabled(isEnabled);
            mi.addSelectionListener(new SelectionAdapter() {

                public void widgetSelected(SelectionEvent event) {
                    MenuItem selected = (MenuItem) event.widget;

                    for (Calculation c : Calculation.values()) {
                        if (c.getTitle().equals(selected.getText())) {
                            EnsembleTool.getInstance().calculate(c);
                        }
                    }
                }
            });
        }

        public void widgetSelected(SelectionEvent event) {
            // display the menu
            if ((event.detail == SWT.ARROW) || (event.detail == SWT.MENU_MOUSE)) {
                ToolItem item = (ToolItem) event.widget;
                Rectangle rect = item.getBounds();
                Point pt = item.getParent()
                        .toDisplay(new Point(rect.x, rect.y));
                calculationMenu.setLocation(pt.x, pt.y + rect.height);
                calculationMenu.setVisible(true);
            }
        }

        public void widgetDefaultSelected(SelectionEvent e) {
            widgetSelected(e);
        }
    }

    /*
     * Here's how we control the items displayed in the tree.
     */
    private class EnsembleTreeColumnLabelProvider extends ColumnLabelProvider {

        public Font getFont(Object element) {

            Font f = null;
            if (viewEditable) {
                if (element instanceof String) {
                    f = SWTResourceManager.getFont("courier new", 10, SWT.BOLD);
                }
                if (element instanceof GenericResourceHolder) {
                    GenericResourceHolder gr = (GenericResourceHolder) element;
                    if (!gr.requiresLoadCheck()
                            || (gr.isLoadedAtFrame(currentFramesInfo))) {
                        f = SWTResourceManager.getFont("courier new", 10,
                                SWT.BOLD);
                    } else {
                        f = SWTResourceManager.getFont("courier new", 9,
                                SWT.BOLD);
                    }
                }

            } else {
                f = SWTResourceManager.getFont("courier new", 10, SWT.NONE);
            }
            return f;
        }

        public Image getImage(Object element) {
            Image image = null;
            if (element instanceof String) {
                String productName = (String) element;
                int imageWidth = 46;
                int imageHeight = 18;

                ImageData imageData = new ImageData(imageWidth, imageHeight,
                        24, new PaletteData(255, 255, 255));
                imageData.transparentPixel = imageData.palette
                        .getPixel(new RGB(255, 255, 255));
                image = new Image(ownerComposite.getDisplay(), imageData);
                GC gc = new GC(image);
                gc.setBackground(SWTResourceManager.WHITE);
                gc.fillRectangle(0, 0, imageWidth, imageHeight);

                // if any ensemble members are visible then the root tree item
                // should be "toggled on" ...
                if (anyChildrenToggleOn(productName)) {
                    gc.setBackground(SWTResourceManager.BLACK);
                }
                // otherwise, the root tree item should appear "toggled off" ...
                else {
                    gc.setBackground(SWTResourceManager.DARKER_GRAY);
                }

                int listEntryLineUpperLeft_x = 4;
                int listEntryTopLineUpperLeft_y = 4;
                int listEntryMiddleLineUpperLeft_y = 8;
                int listEntryBottomLineUpperLeft_y = 12;
                int listEntryWidth = 23;
                int listEntryHeight = 2;

                // the icon for an ensemble product is three black
                // horizontal bars which is an attempt to represent
                // a list of the ensemble's pertubation members.
                gc.fillRectangle(listEntryLineUpperLeft_x,
                        listEntryTopLineUpperLeft_y, listEntryWidth,
                        listEntryHeight);
                gc.fillRectangle(listEntryLineUpperLeft_x,
                        listEntryMiddleLineUpperLeft_y, listEntryWidth,
                        listEntryHeight);
                gc.fillRectangle(listEntryLineUpperLeft_x,
                        listEntryBottomLineUpperLeft_y, listEntryWidth,
                        listEntryHeight);

                int bulletSize = 3;
                int bulletUpperLeftMargin_x = 15;
                int bulletUpperLeft_y = 9;

                // then put a nice hyphen
                gc.fillRectangle(listEntryWidth + bulletUpperLeftMargin_x,
                        bulletUpperLeft_y, bulletSize + 2, bulletSize - 1);
                gc.dispose();

            } else if (element instanceof GenericResourceHolder) {

                GenericResourceHolder gr = (GenericResourceHolder) element;
                RGB color = gr.getRsc()
                        .getCapability(ColorableCapability.class).getColor();

                int imageWidth = 46;
                int imageHeight = 18;
                int colorWidth = 24;
                int colorHeight = 14;
                int innerColorWidth = 20;
                int innerColorHeight = 10;
                int bulletSize = 3;
                int bulletUpperLeftMargin_x = 13;
                int bulletUpperLeft_y = 9;

                ImageData imageData = new ImageData(imageWidth, imageHeight,
                        24, new PaletteData(255, 255, 255));
                imageData.transparentPixel = imageData.palette
                        .getPixel(new RGB(255, 255, 255));
                image = new Image(ownerComposite.getDisplay(), imageData);
                GC gc = new GC(image);
                gc.setBackground(SWTResourceManager.WHITE);
                gc.fillRectangle(0, 0, imageWidth, imageHeight);
                if (gr.getRsc().getProperties().isVisible()) {

                    // need the following tweaking integers which cosmetic
                    // center things nicely
                    gc.setBackground(SWTResourceManager.BLACK);

                    // the icon for a visible individual grid resources put the
                    // color of the resource inside a black bordered rectangle.
                    gc.fillRectangle(4, imageHeight - colorHeight - 2,
                            colorWidth, colorHeight);

                    if (element instanceof GeneratedGridResourceHolder) {
                        color = Utilities.brighten(color);
                    }
                    gc.setBackground(SWTResourceManager.getColor(color));
                    gc.fillRectangle(4 + ((colorWidth - innerColorWidth) / 2),
                            (imageHeight - colorHeight)
                                    + ((colorHeight - innerColorHeight) / 2)
                                    - 2, innerColorWidth, innerColorHeight);

                    // then put a nice hyphen
                    gc.setBackground(SWTResourceManager.BLACK);
                    gc.fillRectangle(colorWidth + bulletUpperLeftMargin_x,
                            bulletUpperLeft_y, bulletSize + 2, bulletSize - 1);
                } else {
                    // need the following tweaking integers which cosmetic
                    // center things nicely
                    gc.setBackground(SWTResourceManager.BLACK);

                    // the icon for a hidden individual grid resources put the
                    // color of the resource inside a greyed bordered rectangle.
                    gc.fillRectangle(4, imageHeight - colorHeight - 2,
                            colorWidth, colorHeight);
                    gc.setBackground(SWTResourceManager.getColor(Utilities
                            .desaturate(color)));
                    gc.fillRectangle(4 + ((colorWidth - innerColorWidth) / 2),
                            (imageHeight - colorHeight)
                                    + ((colorHeight - innerColorHeight) / 2)
                                    - 2, innerColorWidth, innerColorHeight);

                    gc.setBackground(SWTResourceManager.LIGHT_GRAY);
                    gc.fillRectangle(colorWidth + bulletUpperLeftMargin_x,
                            bulletUpperLeft_y, bulletSize + 2, bulletSize - 1);
                }
                if (gr.requiresLoadCheck()
                        && (!gr.isLoadedAtFrame(currentFramesInfo))) {
                    gc.setBackground(SWTResourceManager.DARKER_GRAY);

                    // the icon for a hidden individual grid resources put the
                    // color of the resource inside a greyed bordered rectangle.
                    gc.fillRectangle(4, imageHeight - colorHeight - 2,
                            colorWidth, colorHeight);
                    gc.setBackground(SWTResourceManager.getColor(Utilities
                            .desaturate(color)));
                    gc.fillRectangle(4 + ((colorWidth - innerColorWidth) / 2),
                            (imageHeight - colorHeight)
                                    + ((colorHeight - innerColorHeight) / 2)
                                    - 2, innerColorWidth, innerColorHeight);
                }
                gc.dispose();

            }
            if (image != null) {
                imageCache.add(image);
            }
            return image;
        }

        public String getText(Object element) {

            String nodeLabel = null;
            if (element instanceof String) {
                nodeLabel = (String) element;
            } else if (GeneratedGridResourceHolder.class
                    .isAssignableFrom(element.getClass())
                    || (GeneratedTimeSeriesResourceHolder.class
                            .isAssignableFrom(element.getClass()))) {
                GenericResourceHolder gr = (GenericResourceHolder) element;
                nodeLabel = gr.getUniqueName();
            } else if (element instanceof GridResourceHolder) {

                GridResourceHolder gr = (GridResourceHolder) element;
                if ((gr.getEnsembleId() != null)
                        && (gr.getEnsembleId().length() > 0)) {
                    nodeLabel = gr.getEnsembleId();
                } else {
                    nodeLabel = gr.getUniqueName();
                }
            } else if (TimeSeriesResourceHolder.class.isAssignableFrom(element
                    .getClass())) {

                TimeSeriesResourceHolder tsr = (TimeSeriesResourceHolder) element;
                if ((tsr.getEnsembleId() != null)
                        && (tsr.getEnsembleId().length() > 0)) {
                    nodeLabel = tsr.getEnsembleId();
                } else {
                    nodeLabel = tsr.getUniqueName();
                }
            } else if (element instanceof HistogramGridResourceHolder) {

                HistogramGridResourceHolder gr = (HistogramGridResourceHolder) element;
                if ((gr.getEnsembleId() != null)
                        && (gr.getEnsembleId().length() > 0)) {
                    nodeLabel = gr.getEnsembleId();
                } else {
                    nodeLabel = gr.getUniqueName();
                }
            }
            // update the visibility status cosmetically (i.e. normal text
            // versus graying-out)
            if (element instanceof GenericResourceHolder) {
                GenericResourceHolder gr = (GenericResourceHolder) element;

                TreeItem treeItem = findTreeItemByResource(gr);
                if (treeItem != null) {
                    if (gr.getRsc().getProperties().isVisible()) {
                        treeItem.setForeground(EnsembleToolViewer.ENABLED_FOREGROUND_COLOR);
                    } else {
                        treeItem.setForeground(EnsembleToolViewer.DISABLED_FOREGROUND_COLOR);
                    }
                    // matchParentToChildrenVisibility(treeItem);
                }
                if (nodeLabel != null) {
                    nodeLabel = nodeLabel.trim();
                    if (gr != null && currentFramesInfo != null
                            && gr.requiresLoadCheck()
                            && !gr.isLoadedAtFrame(currentFramesInfo)) {
                        nodeLabel = nodeLabel.concat(" (Not Loaded)");
                    }
                }
            }
            return nodeLabel;
        }
    }

    // TODO we need come up with a more intuitive solution
    // to sorting than what is provided here.
    private class EnsembleTreeSorter extends ViewerSorter {

        public int compare(Viewer v, Object av1, Object av2) {

            boolean compareResultFound = false;

            int compareResult = 0;

            if ((av1 instanceof String) && (av2 instanceof String)) {
                String n1 = (String) av1;
                String n2 = (String) av2;
                compareResult = n1.compareTo(n2);
            } else if ((av1 instanceof GenericResourceHolder)
                    && (av2 instanceof GenericResourceHolder)) {

                GenericResourceHolder gr1 = (GenericResourceHolder) av1;
                GenericResourceHolder gr2 = (GenericResourceHolder) av2;

                AbstractVizResource<?, ?> vr1 = gr1.getRsc();
                AbstractVizResource<?, ?> vr2 = gr2.getRsc();

                if ((av1 instanceof GridResourceHolder)
                        && (av2 instanceof GridResourceHolder)) {

                    GridResourceHolder grh1 = (GridResourceHolder) av1;
                    GridResourceHolder grh2 = (GridResourceHolder) av2;

                    // If there are perturbation names then let's compare those
                    // ...
                    String av1_pert = grh1.getEnsembleId();
                    String av2_pert = grh2.getEnsembleId();

                    if ((av1_pert != null) && (av1_pert.length() > 0)
                            && (av2_pert != null) && (av2_pert.length() > 0)) {

                        compareResult = av1_pert.compareTo(av2_pert);
                        compareResultFound = true;
                    } else {
                        String ts_fullName_1 = vr1.getName();
                        String ts_fullName_2 = vr2.getName();

                        if ((ts_fullName_1 != null) && (ts_fullName_2 != null)) {

                            compareResult = ts_fullName_1
                                    .compareTo(ts_fullName_2);
                            compareResultFound = true;
                        }
                    }
                }
                if (!compareResultFound) {

                    if (TimeSeriesResourceHolder.class.isAssignableFrom(av1
                            .getClass())
                            && (TimeSeriesResourceHolder.class
                                    .isAssignableFrom(av2.getClass()))) {

                        TimeSeriesResourceHolder tsr1 = (TimeSeriesResourceHolder) av1;
                        TimeSeriesResourceHolder tsr2 = (TimeSeriesResourceHolder) av2;

                        // If there are perturbation names then let's compare
                        // those ...
                        String pert_1 = tsr1.getEnsembleId();
                        String pert_2 = tsr2.getEnsembleId();

                        if ((pert_1 != null) && (pert_1.length() > 0)
                                && (pert_2 != null) && (pert_2.length() > 0)) {

                            compareResult = pert_1.compareTo(pert_2);
                            compareResultFound = true;
                        } else {

                            String ts_fullName_1 = vr1.getName();
                            String ts_fullName_2 = vr2.getName();

                            if ((ts_fullName_1 != null)
                                    && (ts_fullName_2 != null)) {

                                compareResult = ts_fullName_1
                                        .compareTo(ts_fullName_2);
                                compareResultFound = true;
                            }
                        }
                    }
                    if (!compareResultFound) {

                        if ((vr1 != null) && (vr1.getName() != null)
                                && (vr2 != null) && (vr2.getName() != null)) {

                            compareResult = vr1.getName().compareTo(
                                    vr2.getName());
                        }
                    }
                }
            }
            /*
             * Finally, check for apples to oranges comparisons.
             */
            else if ((av1 instanceof String)
                    && (av2 instanceof GenericResourceHolder)) {
                compareResult = 1;
            } else if ((av1 instanceof GenericResourceHolder)
                    && (av2 instanceof String)) {
                compareResult = -1;
            }

            return compareResult;
        }
    }

    static boolean CTRL_KEY_DEPRESSED = false;

    /*
     * This key listener class is used to set the mouse pointer to either the
     * HAND cursor, when the Ctrl key is depressed, or to the default ARROW
     * cursor, when the Ctrl key is released.
     * 
     * When the user presses CTRL-MB1 (e.g. Ctrl LEFT-CLICK) over a resource in
     * the navigator, the cursor will first change to the HAND cursor, and the
     * item will be selected (e.g. the contour highlighted).
     * 
     * When the user does not have the Ctrl key depressed, the mouse pointer
     * remains an ARROW cursor. When the user presses MB1 over (e.g. LEFT-CLICK)
     * a resource in the navigator that resource will have its visibility
     * toggled.
     */

    private class EnsembleTreeKeyListener implements KeyListener {

        @Override
        public void keyPressed(KeyEvent e) {

            if ((e.keyCode & SWT.CTRL) == SWT.CTRL) {
                CTRL_KEY_DEPRESSED = true;
                updateCursor(selectionModeCursor);
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {

            if ((e.keyCode & SWT.CTRL) == SWT.CTRL) {
                CTRL_KEY_DEPRESSED = false;
                updateCursor(normalCursor);
            }
        }

    }

    private class EnsembleTreeMouseListener implements MouseListener {

        @Override
        public void mouseDoubleClick(MouseEvent event) {
            // TODO No need for a double click event just yet.
        }

        @Override
        public void mouseDown(MouseEvent event) {

            /**
             * The mouseDown event is what initiates the displaying of a
             * context-sensitive popup menu for either ensemble products or
             * individual grid products ...
             */

            /*
             * if the ensemble tool isn't open then ignore user mouse clicks ...
             */
            if (!EnsembleTool.getInstance().isToolEditable()
                    || !isViewerTreeReady()) {
                return;
            }

            // get the tree item that was clicked on ...
            Point point = new Point(event.x, event.y);

            final TreeItem userClickedTreeItem = ensembleTree.getItem(point);
            if (userClickedTreeItem == null || userClickedTreeItem.isDisposed()) {
                return;
            }

            /*
             * is this a mouse-button-3 (e.g. typical RIGHT-CLICK) over a tree
             * item?
             */
            if ((userClickedTreeItem != null) && (event.button == 3)) {
                /*
                 * let's put up a context-sensitive menu similar to cave legend
                 * pop-up menu ...
                 */
                final Object mousedItem = userClickedTreeItem.getData();

                /*
                 * Currently, top level tree items (which also contain child
                 * tree items) are always Ensemble names (unique strings). So if
                 * the user clicks on this item then display the popup menu for
                 * the Ensemble product.
                 */
                if (mousedItem instanceof String) {
                    final Menu legendMenu = new Menu(ownerComposite.getShell(),
                            SWT.POP_UP);
                    final String mousedEnsembleName = (String) mousedItem;

                    /*
                     * Relative frequency menu item allows the user to generate
                     * a probability display demonstrating the chance a value
                     * p(x) lies within a range, outside a range, above a
                     * threshold, or below a threshold.
                     */
                    addERFLayerMenuItem = new MenuItem(legendMenu, SWT.PUSH);
                    addERFLayerMenuItem.setText("Relative Frequency");

                    /* only enable the RF menu item if we are in plan view */
                    if (editorResourceType == ResourceType.TIME_SERIES) {
                        addERFLayerMenuItem.setEnabled(false);
                    }
                    if (editorResourceType == ResourceType.PLAN_VIEW) {
                        addERFLayerMenuItem.setEnabled(true);
                    }

                    addERFLayerMenuItem.addListener(SWT.Selection,
                            new Listener() {

                                public void handleEvent(Event event) {

                                    updateCursor(waitCursor);
                                    startAddERFLayer(mousedEnsembleName);
                                    updateCursor(normalCursor);

                                }
                            });

                    /*
                     * This menu item allows the user to choose a color gradient
                     * for either the SREF or GEFS ensemble products.
                     */
                    MenuItem ensembleColorizeMenuItem = new MenuItem(
                            legendMenu, SWT.PUSH);
                    ensembleColorizeMenuItem.setText("Color Gradient");

                    ensembleColorizeMenuItem.addListener(SWT.Selection,
                            new Listener() {
                                public void handleEvent(Event event) {

                                    /*
                                     * TODO: SWT bug prevents getting children
                                     * on a collapsed tree item. Must expand
                                     * first for color gradient change method to
                                     * "see" children.
                                     */
                                    boolean isExpanded = userClickedTreeItem
                                            .getExpanded();
                                    if (!isExpanded) {
                                        userClickedTreeItem.setExpanded(true);
                                    }

                                    updateColorsOnEnsembleResource(mousedEnsembleName);

                                }
                            });

                    /*
                     * this menu item allows the user to remove the ensemble and
                     * all of its members.
                     */
                    MenuItem unloadRscMenuItem = new MenuItem(legendMenu,
                            SWT.PUSH);
                    unloadRscMenuItem.setText("Unload Members");
                    unloadRscMenuItem.addListener(SWT.Selection,
                            new Listener() {
                                public void handleEvent(Event event) {

                                    boolean proceed = MessageDialog.openConfirm(
                                            ownerComposite.getShell(),
                                            "Unload Ensemble Members",
                                            "Are you sure you want to unload all members of "
                                                    + mousedEnsembleName + "?");

                                    if (proceed) {

                                        EnsembleTool.getInstance()
                                                .unloadResourcesByName(
                                                        userClickedTreeItem
                                                                .getText());
                                    }

                                }
                            });

                    /*
                     * this menu item allows the user to hide/show an ensemble
                     * product and all of its members.
                     */
                    final MenuItem toggleVisibilityMenuItem = new MenuItem(
                            legendMenu, SWT.CHECK);
                    toggleVisibilityMenuItem.setText("Display Product");
                    toggleVisibilityMenuItem.setEnabled(false);
                    toggleVisibilityMenuItem.addListener(SWT.Selection,
                            new Listener() {

                                public void handleEvent(Event event) {

                                    ToggleProductVisiblityJob ccj = new ToggleProductVisiblityJob(
                                            "Toggle Product Visibility");
                                    ccj.setPriority(Job.INTERACTIVE);
                                    ccj.setTargetTreeItem(userClickedTreeItem);
                                    ccj.schedule(elegantWaitPeriod);

                                }
                            });

                    legendMenu.setVisible(true);

                } else if (mousedItem instanceof GenericResourceHolder) {

                    /*
                     * If the tree item the user clicked on was an individual
                     * grid product then show the context-sensitive popup menu
                     * for it ...
                     */
                    final GenericResourceHolder gr = (GenericResourceHolder) mousedItem;

                    MenuManager menuMgr = new MenuManager("#PopupMenu");
                    menuMgr.setRemoveAllWhenShown(true);

                    /* The popup menu is generated by the ContextMenuManager */
                    menuMgr.addMenuListener(new IMenuListener() {
                        public void menuAboutToShow(IMenuManager manager) {
                            ResourcePair rp = EnsembleTool.getInstance()
                                    .getResourcePair(gr.getRsc());
                            if (rp != null) {
                                com.raytheon.viz.ui.cmenu.ContextMenuManager
                                        .fillContextMenu(manager, rp, gr
                                                .getRsc()
                                                .getResourceContainer());
                            }
                        }
                    });

                    final Menu legendMenu = menuMgr
                            .createContextMenu(ownerComposite);
                    legendMenu.setVisible(true);

                }
                ensembleTree.deselect(userClickedTreeItem);
            }
            /*
             * Is this a simple left-click (MB1) over a tree item? Then
             * show/hide. Also, make certain this isn't a Ctrl-MB1 (as that key
             * sequence is the selection feature and is handled in the mouseUp
             * event.
             */
            else if ((userClickedTreeItem != null) && (event.button == 1)
                    && ((event.stateMask & SWT.CTRL) == 0)) {
                /*
                 * By default, left-clicking on a tree item in the tree will
                 * toggle that product's visibility.
                 */

                final Object mousedItem = userClickedTreeItem.getData();

                /* A string means it is an ensemble product name */
                if (mousedItem instanceof String) {

                    String ensembleName = (String) mousedItem;

                    /*
                     * The way that the TreeItem appears to work is that it
                     * returns the correct number of child items when the
                     * TreeItem is expanded and always the wrong number of child
                     * items (usually 1) when the TreeItem is collapsed.
                     * 
                     * Therefore, always expand a collpased tree item before
                     * asking for starting a job that will attempt to get its
                     * child items.
                     * 
                     * TODO: Fix this in the future so we don't force the root
                     * item to be expanded just to toggle the child items
                     * visibility. This place to make this change will not be
                     * here, but instead in the getDirectDescendants method.
                     */

                    boolean isExpanded = userClickedTreeItem.getExpanded();
                    if (!isExpanded) {
                        userClickedTreeItem.setExpanded(true);
                    }
                    ToggleEnsembleVisiblityJob ccj = new ToggleEnsembleVisiblityJob(
                            "Toggle Ensemble Members Visibility");
                    ccj.setPriority(Job.INTERACTIVE);
                    ccj.setTargetEnsembleProduct(ensembleName);
                    ccj.schedule(elegantWaitPeriod);

                } else if (mousedItem instanceof GenericResourceHolder) {

                    ToggleProductVisiblityJob ccj = new ToggleProductVisiblityJob(
                            "Toggle Product Visibility");
                    ccj.setPriority(Job.INTERACTIVE);
                    ccj.setTargetTreeItem(userClickedTreeItem);
                    ccj.schedule(elegantWaitPeriod);
                }
            }
        }

        @Override
        public void mouseUp(MouseEvent event) {

            Point point = new Point(event.x, event.y);

            if (!EnsembleTool.getInstance().isToolEditable()
                    || !isViewerTreeReady()) {
                return;
            }

            final TreeItem userClickedTreeItem = ensembleTree.getItem(point);

            /*
             * The mouse up event currently only acts on items in the tree so if
             * the tree item is null then just return ...
             */
            if (userClickedTreeItem == null || userClickedTreeItem.isDisposed()) {
                return;
            }

            /*
             * keep track of the last highlighted resource and make sure it is
             * still displayed properly ...
             */
            if (EnsembleToolViewer.LAST_HIGHLIGHTED_RESOURCE != null) {
                EnsembleToolViewer.LAST_HIGHLIGHTED_RESOURCE.getCapability(
                        OutlineCapability.class).setOutlineWidth(
                        LAST_HIGHLIGHTED_RESOURCE_WIDTH);
                EnsembleToolViewer.LAST_HIGHLIGHTED_RESOURCE.getCapability(
                        ColorableCapability.class).setColor(
                        LAST_HIGHLIGHTED_RESOURCE_RGB);
                EnsembleToolViewer.LAST_HIGHLIGHTED_RESOURCE
                        .getCapability(OutlineCapability.class)
                        .setOutlineOn(
                                EnsembleToolViewer.LAST_HIGHLIGHTED_RESOURCE_OUTLINE_ASSERTED);
            }

            /*
             * Is this a CTRL-MB1 (e.g. CONTROL LEFT-CLICK) over a tree item?
             * ... then this is a UI SWT item selection
             */
            if ((event.button == 1) && ((event.stateMask & SWT.CTRL) != 0)) {

                final Object mousedObject = userClickedTreeItem.getData();

                /* Ctrl-click on a item that is already selected deselects it. */
                if (EnsembleToolViewer.LAST_HIGHLIGHTED_RESOURCE != null) {

                    if (mousedObject instanceof GenericResourceHolder) {
                        GenericResourceHolder grh = (GenericResourceHolder) mousedObject;
                        if (grh.getRsc() == EnsembleToolViewer.LAST_HIGHLIGHTED_RESOURCE) {
                            ensembleTree.deselect(userClickedTreeItem);
                            EnsembleToolViewer.LAST_HIGHLIGHTED_RESOURCE = null;
                            return;
                        }
                    }
                } else if (isViewerTreeReady()) {

                    /* Ctrl-click on a item that is not selected selects it. */
                    if (mousedObject instanceof GenericResourceHolder) {
                        GenericResourceHolder grh = (GenericResourceHolder) mousedObject;
                        EnsembleToolViewer.LAST_HIGHLIGHTED_RESOURCE = grh
                                .getRsc();
                        ensembleTreeViewer.getTree().deselectAll();
                        ensembleTreeViewer.getTree()
                                .select(userClickedTreeItem);
                    }
                    ensembleTreeViewer.getTree().update();
                }

                /*
                 * if the user selects an generated ERF product then update the
                 * ERF tabs ...
                 */
                if (mousedObject instanceof GeneratedGridResourceHolder) {
                    GeneratedGridResourceHolder grh = (GeneratedGridResourceHolder) mousedObject;
                    if (grh.getCalculation() == Calculation.ENSEMBLE_RELATIVE_FREQUENCY) {
                        /**
                         * TODO: Do we want to bring up the ERF dialog with the
                         * existing values to populate?
                         */
                    }
                }

                /* is this an individual grid product? */
                if (mousedObject instanceof GridResourceHolder) {
                    GridResourceHolder grh = (GridResourceHolder) mousedObject;

                    /* only highlight a visible resource */
                    if ((grh.getRsc() != null)
                            && (grh.getRsc().getProperties().isVisible())) {

                        currentEnsembleRsc = grh.getRsc();

                        /*
                         * Then highlight the resource in the primary pane and
                         * keep track of the resource as the most recently
                         * highlighted resource.
                         */
                        if ((currentEnsembleRsc != null)
                                && (thickenOnSelection)) {
                            EnsembleToolViewer.LAST_HIGHLIGHTED_RESOURCE = currentEnsembleRsc;
                            EnsembleToolViewer.LAST_HIGHLIGHTED_RESOURCE_WIDTH = currentEnsembleRsc
                                    .getCapability(OutlineCapability.class)
                                    .getOutlineWidth();
                            EnsembleToolViewer.LAST_HIGHLIGHTED_RESOURCE_RGB = currentEnsembleRsc
                                    .getCapability(ColorableCapability.class)
                                    .getColor();
                            EnsembleToolViewer.LAST_HIGHLIGHTED_RESOURCE_OUTLINE_ASSERTED = currentEnsembleRsc
                                    .getCapability(OutlineCapability.class)
                                    .isOutlineOn();
                            currentEnsembleRsc.getCapability(
                                    OutlineCapability.class).setOutlineOn(true);
                            currentEnsembleRsc.getCapability(
                                    OutlineCapability.class).setOutlineWidth(
                                    thickenWidth);
                            if (!useResourceColorOnThicken) {
                                currentEnsembleRsc.getCapability(
                                        ColorableCapability.class).setColor(
                                        thickenOnSelectionColor.getRGB());
                            }
                        }
                    }
                }
                /*
                 * Is this a generated product (e.g. a user-requested
                 * calculation)?
                 */
                if (mousedObject instanceof GeneratedGridResourceHolder) {
                    GeneratedGridResourceHolder grh = (GeneratedGridResourceHolder) mousedObject;

                    /* only highlight a visible resource */
                    if ((grh.getRsc() != null)
                            && (grh.getRsc().getProperties().isVisible())) {

                        currentEnsembleRsc = grh.getRsc();

                        /*
                         * Then highlight the resource in the primary pane and
                         * keep track of the resource as the most recently
                         * highlighted resource.
                         */
                        if ((currentEnsembleRsc != null)
                                && (thickenOnSelection)) {
                            EnsembleToolViewer.LAST_HIGHLIGHTED_RESOURCE = currentEnsembleRsc;
                            EnsembleToolViewer.LAST_HIGHLIGHTED_RESOURCE_WIDTH = currentEnsembleRsc
                                    .getCapability(OutlineCapability.class)
                                    .getOutlineWidth();
                            EnsembleToolViewer.LAST_HIGHLIGHTED_RESOURCE_RGB = currentEnsembleRsc
                                    .getCapability(ColorableCapability.class)
                                    .getColor();
                            EnsembleToolViewer.LAST_HIGHLIGHTED_RESOURCE_OUTLINE_ASSERTED = currentEnsembleRsc
                                    .getCapability(OutlineCapability.class)
                                    .isOutlineOn();
                            currentEnsembleRsc.getCapability(
                                    OutlineCapability.class).setOutlineOn(true);
                            currentEnsembleRsc.getCapability(
                                    OutlineCapability.class).setOutlineWidth(
                                    thickenWidth);
                            if (!useResourceColorOnThicken) {
                                currentEnsembleRsc.getCapability(
                                        ColorableCapability.class).setColor(
                                        thickenOnSelectionColor.getRGB());
                            }
                        }
                    }
                }
            }
        }
    }

    private List<GenericResourceHolder> perturbationMembers = null;

    public List<GenericResourceHolder> getPerturbationMembers() {
        return perturbationMembers;
    }

    public void setPerturbationMembers(
            List<GenericResourceHolder> perturbationMembers) {
        this.perturbationMembers = perturbationMembers;
    }

    /*
     * This method will update color on a given ensemble resource. Underlying
     * methods know how to colorize the contained perturbation members (eg. for
     * SREF, breaks colors into three categories for NMM, NMB and EM).
     * 
     * We need a better way of determining what type of resource we have.
     */
    protected void updateColorsOnEnsembleResource(final String ensembleName) {

        VizApp.runSync(new Runnable() {
            @Override
            public void run() {
                setPerturbationMembers(getEnsembleMemberGenericResources(ensembleName));
            }
        });

        // TODO: poor-man's way of knowing what type of flavor this ensemble is
        if ((ensembleName.indexOf("GEFS") >= 0)
                || (ensembleName.indexOf("GFS Ensemble") >= 0)) {
            updateGEFSEnsembleColors(ensembleName, getPerturbationMembers());
        } else if (ensembleName.indexOf("SREF") >= 0) {
            updateSREFColors(ensembleName, getPerturbationMembers());
        }
    }

    private void updateSREFColors(String ensembleName,
            final List<GenericResourceHolder> children) {

        EnsembleSREFColorChooser cd = new EnsembleSREFColorChooser(
                ownerComposite.getShell());
        cd.setBlockOnOpen(true);
        if (cd.open() == Window.OK) {

            cd.close();

            SREFMembersColorChangeJob ccj = new SREFMembersColorChangeJob(
                    "Changing SREF Ensemble Members Colors");
            ccj.setPriority(Job.INTERACTIVE);
            ccj.schedule();

        }

    }

    private void updateGEFSEnsembleColors(String ensembleName,
            final List<GenericResourceHolder> children) {

        EnsembleGEFSColorChooser cd = new EnsembleGEFSColorChooser(
                ownerComposite.getShell());
        cd.setBlockOnOpen(true);
        if (cd.open() == Window.OK) {
            cd.close();

            GEFSMembersColorChangeJob ccj = new GEFSMembersColorChangeJob(
                    "Changing GEFS Ensemble Members Colors");
            ccj.setPriority(Job.INTERACTIVE);
            ccj.schedule();
        }

    }

    public boolean isMakeEditableOnRestorePreference() {
        return editableOnRestore;
    }

    public boolean isMakeEditableOnSwapInPreference() {
        return editableOnSwapIn;
    }

    public boolean isMinimizeOnForeignToolLoadPreference() {
        return minimizeOnForeignToolLoad;
    }

    public boolean isMinimizeOnToggleUneditable() {
        return minimizeOnToggleUneditable;
    }

    public boolean isCreateNewToolLayerOnNewEditor() {
        return createToolLayerOnNewEditor;
    }

    public RGB getStartColor(RGB inColor) {

        RGB maxHue = null;
        float[] hsb = inColor.getHSB();
        maxHue = new RGB(hsb[0], 1.0f, 0.35f);

        return maxHue;
    }

    // wouldn't it be nice if when we attempt to automatically
    // assign a color to a resource it would be somewhat dis-
    // tinct from any previously assigned color?
    public RGB getNextShadeColor(RGB inColor) {

        RGB shadeDown = null;

        float[] hsb = inColor.getHSB();

        float brightness = hsb[2];
        float saturation = hsb[1];

        brightness = (float) Math.min(1.0f, (brightness + 0.05f));
        saturation = (float) Math.max(0.15f, (saturation - 0.05f));

        shadeDown = new RGB(hsb[0], saturation, brightness);

        return shadeDown;
    }

    /*
     * Returns all resources (perturbation members) associated with an ensemble
     * resource name.
     */
    protected List<GenericResourceHolder> getEnsembleMemberGenericResources(
            String ensembleName) {

        TreeItem parentItem = findTreeItemByLabelName(ensembleName);

        List<TreeItem> descendants = new ArrayList<TreeItem>();
        getAllDescendants(parentItem, descendants);

        List<GenericResourceHolder> childResources = new ArrayList<GenericResourceHolder>();

        if (descendants.size() > 0) {
            for (TreeItem ti : descendants) {
                Object data = ti.getData();
                if (data == null) {
                    continue;
                }
                if (data instanceof GenericResourceHolder) {
                    GenericResourceHolder gr = (GenericResourceHolder) data;
                    childResources.add(gr);
                }
            }
        }
        return childResources;
    }

    public void prepareForNewToolInput() {
        VizApp.runAsync(new Runnable() {
            public void run() {
                if (isViewerTreeReady()) {
                    ensembleTreeViewer.setInput(EnsembleTool.getInstance()
                            .getEmptyResourceList());
                }
            }
        });
    }

    synchronized public void refreshInput() {

        if (!isViewerTreeReady()) {
            return;
        }

        final Map<String, List<GenericResourceHolder>> ensembleResourcesMap = EnsembleTool
                .getInstance().getEnsembleResources();

        if (ensembleResourcesMap == null) {
            return;
        }

        VizApp.runAsync(new Runnable() {
            public void run() {

                if (!isViewerTreeReady()) {
                    return;
                }

                // this method only acts on an instance of a Map ...
                if (ensembleResourcesMap != null) {

                    ensembleTree.clearAll(true);

                    // only change the tree's input reference (via setInput)
                    // when the ensembleResourcesMap reference is different.

                    if ((ensembleTreeViewer.getInput() == null)
                            || (ensembleResourcesMap != ensembleTreeViewer
                                    .getInput())) {
                        ensembleTreeViewer.setInput(ensembleResourcesMap);
                    }
                    ensembleTreeViewer.refresh(true);

                    GenericResourceHolder grh = null;
                    String ensembleRscName = null;
                    if (isViewerTreeReady()) {
                        TreeItem[] selectedItems = ensembleTree.getSelection();
                        if ((selectedItems != null)
                                && (selectedItems.length > 0)
                                && (selectedItems[0] != null)) {
                            Object o = selectedItems[0].getData();
                            if (o instanceof GenericResourceHolder) {
                                grh = (GenericResourceHolder) o;
                            } else if (o instanceof String) {
                                ensembleRscName = (String) o;
                            }
                        }

                        updateInfoTab();

                        setTreeExpansion(EnsembleTool.getInstance()
                                .getExpandedElements());

                        if ((selectedItems != null)
                                && (selectedItems.length > 0)
                                && (selectedItems[0] != null)) {
                            if (grh != null) {
                                TreeItem ti = findTreeItemByResource(grh);
                                if (ti != null)
                                    ensembleTree.select(ti);
                            } else if (ensembleRscName != null) {
                                TreeItem ti = findTreeItemByLabelName(ensembleRscName);
                                if (ti != null)
                                    ensembleTree.select(ti);
                            }
                        }
                    }
                    ensembleTreeViewer.refresh(false);
                }
            }
        });
    }

    @Override
    public void doSave(IProgressMonitor monitor) {
        /**
         * TODO Currently we don't have any way to Save the loaded products as a
         * bundle, that is in a way that would allow the user to reopen the
         * products back into the Ensemble Tool.
         */
    }

    @Override
    public void doSaveAs() {
        /**
         * TODO Currently we don't have any way to Save As the loaded products
         * as a bundle, that is in a way that would allow the user to reopen the
         * products back into the Ensemble Tool.
         */
    }

    @Override
    public boolean isDirty() {
        if (PlatformUI.getWorkbench().isClosing()) {
            return false;
        }
        return true;
    }

    @Override
    public boolean isSaveAsAllowed() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isSaveOnCloseNeeded() {
        boolean saveOnClose = true;
        if (PlatformUI.getWorkbench().isClosing()) {
            saveOnClose = false;
        }
        return saveOnClose;
    }

    @Override
    public int promptToSaveOnClose() {

        if (PlatformUI.getWorkbench().isClosing()) {
            return ISaveablePart2.NO;
        } else if (isDisabled) {
            return ISaveablePart2.CANCEL;
        }

        int userResponseToClose = EnsembleTool.getInstance()
                .verifyCloseActiveToolLayer();
        return userResponseToClose;

    }

    private void setTreeExpansion(List<String> expandedElements) {
        TreeItem ti = null;
        for (String s : expandedElements) {
            ti = this.findTreeItemByLabelName(s);
            if (ti != null && !ti.isDisposed()) {
                ti.setExpanded(true);
            }
        }
    }

    private List<String> getTreeExpansion() {

        List<String> expandedItems = new ArrayList<String>();

        if (isViewerTreeReady()) {
            TreeItem[] children = ensembleTreeViewer.getTree().getItems();
            List<TreeItem> immediateChildren = Arrays.asList(children);
            for (TreeItem ti : immediateChildren) {
                if (ti != null && !ti.isDisposed()) {
                    if (ti.getData() instanceof String) {
                        String s = (String) ti.getData();
                        if (ti.getExpanded()) {
                            expandedItems.add(s);
                        }
                    }
                }
            }
        }
        return expandedItems;
    }

    /*
     * This method is so we can keep track of the expansion state of the tree.
     */
    public void updateExpansionState() {
        EnsembleTool.getInstance().setExpandedElements(getTreeExpansion());
    }

    private void startAddERFLayer(String mousedEnsembleName) {

        erfDialog = new ERFProductDialog_Modal(ownerComposite.getShell(),
                mousedEnsembleName);
        if (erfDialog.open() == Window.OK) {
            erfDialog.close();
            erfDialog = null;
        }
    }

    /*
     * The lower sash currently contains three tabs: a Info tab, a
     * RelativeFrequency tab, and a Preferences tab.
     */
    private void fillLowerSash(Composite lowerSash) {

        lowerSashTabFolder = new TabFolder(lowerSash, SWT.NONE);
        GridLayout lowerSashTabFolder_gl = new GridLayout(1, true);
        lowerSashTabFolder_gl.marginHeight = 1;
        lowerSashTabFolder_gl.marginWidth = 1;
        lowerSashTabFolder.setLayout(lowerSashTabFolder_gl);
        fillInfoTab(lowerSashTabFolder);
        fillPreferencesTab(lowerSashTabFolder);
        lowerSashTabFolder.setSelection(resourceInfoTabItem);
    }

    // The preference tab contains the preference widgets including
    // "thicken on selection", how thick a selection should be, and whether to
    // use the resource color or user-defined color for selection.
    private void fillPreferencesTab(TabFolder tabFolder_lowerSash) {

        Composite prefsRootComposite = new Composite(tabFolder_lowerSash,
                SWT.BORDER);
        prefsRootComposite.setLayout(new GridLayout(10, true));
        GridData prefsComposite_gd = new GridData(SWT.LEFT, SWT.CENTER, false,
                false, 1, 1);
        prefsComposite_gd.heightHint = 205;
        prefsComposite_gd.widthHint = 351;
        prefsRootComposite.setLayoutData(prefsComposite_gd);
        prefsRootComposite.setBackground(SWTResourceManager.MEDIUM_GRAY);
        preferencesTabItem = new TabItem(tabFolder_lowerSash, SWT.NONE);
        preferencesTabItem
                .setImage(EnsembleToolViewerImageStore.TAB_OPTIONS_ENABLED_IMG);
        preferencesTabItem.setControl(prefsRootComposite);

        final int numCols = 5;
        thickenOnSelectionComposite = new Composite(prefsRootComposite,
                SWT.SHADOW_ETCHED_IN);
        thickenOnSelectionComposite.setLayout(new GridLayout(numCols, false));

        GridData thickenOnSelectionComposite_gd = new GridData(SWT.LEFT,
                SWT.TOP, false, false, 1, 10);
        thickenOnSelectionComposite_gd.widthHint = 152;
        thickenOnSelectionComposite
                .setLayoutData(thickenOnSelectionComposite_gd);

        GridData separatorLbl_gd = new GridData(SWT.LEFT, SWT.CENTER, false,
                false, numCols, 1);
        separatorLbl_gd.widthHint = 218;

        Label separatorLbl_0 = new Label(thickenOnSelectionComposite,
                SWT.SEPARATOR | SWT.HORIZONTAL);
        separatorLbl_0.setLayoutData(separatorLbl_gd);
        separatorLbl_0.setVisible(false);

        thickenOnSelectionBtn = new Button(thickenOnSelectionComposite,
                SWT.CHECK);
        thickenOnSelectionBtn.setSelection(true);
        thickenOnSelection = true;
        thickenOnSelectionBtn.setText("Thicken On Selection");
        thickenOnSelectionBtn.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER,
                false, false, 4, 1));
        thickenOnSelectionBtn.setFont(viewFont);

        Label label_separator_A = new Label(thickenOnSelectionComposite,
                SWT.SEPARATOR | SWT.HORIZONTAL);
        label_separator_A.setLayoutData(separatorLbl_gd);
        label_separator_A.setVisible(false);

        Label separatorLbl_1 = new Label(thickenOnSelectionComposite,
                SWT.SEPARATOR | SWT.HORIZONTAL);
        separatorLbl_1.setLayoutData(separatorLbl_gd);

        Label separatorLbl_B = new Label(thickenOnSelectionComposite,
                SWT.SEPARATOR | SWT.HORIZONTAL);
        separatorLbl_B.setLayoutData(separatorLbl_gd);
        separatorLbl_B.setVisible(false);

        useResourceColorRdo = new Button(thickenOnSelectionComposite, SWT.RADIO);
        useResourceColorRdo.setSelection(true);
        useResourceColorRdo.setText("Use Product Color");
        useResourceColorRdo.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM,
                false, false, numCols, 2));
        useResourceColorRdo.setFont(smallViewFont);

        chooseColorRdo = new Button(thickenOnSelectionComposite, SWT.RADIO);
        GridData chooseColorBtn_gd = new GridData(SWT.LEFT, SWT.CENTER, false,
                false, 3, 1);
        chooseColorBtn_gd.widthHint = 93;
        chooseColorRdo.setLayoutData(chooseColorBtn_gd);
        chooseColorRdo.setFont(smallViewFont);
        chooseColorRdo.setText("Choose Color ");
        chooseColorRdo.setSelection(false);

        colorChooserLbl = new Label(thickenOnSelectionComposite, SWT.BORDER);
        colorChooserLbl.setBackground(thickenOnSelectionColor);
        colorChooserLbl.setFont(SWTResourceManager.getFont("Dialog", 14,
                SWT.NONE));
        colorChooserLbl.setAlignment(SWT.CENTER);
        GridData colorChooserLbl_gd = new GridData(SWT.LEFT, SWT.CENTER, false,
                false, 2, 1);
        colorChooserLbl_gd.widthHint = 40;
        colorChooserLbl.setLayoutData(colorChooserLbl_gd);
        colorChooserLbl.setEnabled(false);
        colorChooserLbl.setBackground(SWTResourceManager.LIGHT_GRAY);
        colorChooserLbl.setText("X");

        Label separatorLbl_3 = new Label(thickenOnSelectionComposite,
                SWT.SEPARATOR | SWT.HORIZONTAL);
        separatorLbl_3.setLayoutData(separatorLbl_gd);
        separatorLbl_3.setVisible(false);

        thicknessChooserLbl = new Label(thickenOnSelectionComposite, SWT.BORDER
                | SWT.CENTER);
        GridData thicknessChooserLbl_gd = new GridData(SWT.LEFT, SWT.CENTER,
                false, false, 2, 1);
        thicknessChooserLbl_gd.widthHint = 65;
        thicknessChooserLbl_gd.heightHint = 23;
        thicknessChooserLbl.setLayoutData(thicknessChooserLbl_gd);
        thicknessChooserLbl.setText("Thickness: ");
        thicknessChooserLbl.setFont(smallViewFont);
        thicknessChooserLbl.setAlignment(SWT.CENTER);

        thicknessChooserSpinner = new Spinner(thickenOnSelectionComposite,
                SWT.BORDER);
        thicknessChooserSpinner.setValues(thickenWidth, 2, 7, 0, 1, 1);
        GridData thicknessChooser_gd = new GridData(SWT.LEFT, SWT.CENTER,
                false, false, 2, 1);
        thicknessChooser_gd.widthHint = 45;
        thicknessChooserLbl_gd.heightHint = 27;
        thicknessChooserSpinner.setLayoutData(thicknessChooser_gd);
        thicknessChooserSpinner.setFont(viewFont);
        thicknessChooserSpinner.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                thickenWidth = ((Spinner) e.getSource()).getSelection();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                thickenWidth = ((Spinner) e.getSource()).getSelection();
            }

        });

        colorChooserLbl.addMouseListener(new MouseAdapter() {

            public void mouseUp(MouseEvent e) {
                if (!useResourceColorOnThicken) {
                    ColorDialog cd = new ColorDialog(ownerComposite.getShell());
                    cd.setRGB(thickenOnSelectionColor.getRGB());
                    cd.setText("Choose Selection Color");
                    RGB result = cd.open();
                    if (result != null) {
                        Color c = SWTResourceManager.getColor(result);
                        thickenOnSelectionColor = c;
                        colorChooserLbl.setBackground(c);
                    }
                }
            }
        });

        thickenOnSelectionBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                boolean isResourceColorBeingUsed = useResourceColorRdo
                        .getSelection();
                boolean isChecked = thickenOnSelectionBtn.getSelection();
                if (isChecked) {
                    useResourceColorRdo.setEnabled(true);
                    chooseColorRdo.setEnabled(true);
                    colorChooserLbl.setEnabled(true);
                    if (isResourceColorBeingUsed) {
                        colorChooserLbl
                                .setBackground(SWTResourceManager.LIGHT_GRAY);
                        colorChooserLbl.setText("X");
                    } else {
                        colorChooserLbl.setBackground(thickenOnSelectionColor);
                        colorChooserLbl.setText("");
                    }
                    thicknessChooserLbl.setEnabled(true);
                    thicknessChooserSpinner.setEnabled(true);
                    thickenOnSelection = true;
                } else {
                    useResourceColorRdo.setEnabled(false);
                    chooseColorRdo.setEnabled(false);
                    colorChooserLbl.setEnabled(false);
                    colorChooserLbl
                            .setBackground(SWTResourceManager.LIGHT_GRAY);
                    colorChooserLbl.setText("X");
                    thicknessChooserLbl.setEnabled(false);
                    thicknessChooserSpinner.setEnabled(false);
                    thickenOnSelection = false;
                }
            }
        });

        useResourceColorRdo.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                boolean isSelected = ((Button) e.getSource()).getSelection();
                if (isSelected) {
                    colorChooserLbl
                            .setBackground(SWTResourceManager.LIGHT_GRAY);
                    colorChooserLbl.setText("X");
                    colorChooserLbl.setEnabled(false);
                    useResourceColorOnThicken = true;
                }
            }

        });

        chooseColorRdo.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                boolean isSelected = ((Button) e.getSource()).getSelection();
                if (isSelected) {
                    colorChooserLbl.setBackground(thickenOnSelectionColor);
                    colorChooserLbl.setText("");
                    colorChooserLbl.setEnabled(true);
                    useResourceColorOnThicken = false;
                }
            }

        });

        /* Additional preferences */

        smallFlagsComposite = new Composite(prefsRootComposite,
                SWT.SHADOW_ETCHED_IN);
        smallFlagsComposite.setLayout(new GridLayout(5, false));
        GridData smallFlagsComposite_gd = new GridData(SWT.LEFT, SWT.CENTER,
                false, false, 4, 6);
        smallFlagsComposite_gd.widthHint = 175;
        smallFlagsComposite.setLayoutData(smallFlagsComposite_gd);

        /*
         * Allow the user to control editability of the ensemble tool layer when
         * this view (ViewPart) is restored.
         */
        editableOnRestoreBtn = new Button(smallFlagsComposite, SWT.CHECK);
        editableOnRestoreBtn.setSelection(editableOnRestore);
        editableOnRestoreBtn.setText("Make editable on restore");
        editableOnRestoreBtn.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER,
                true, false, 4, 1));
        editableOnRestoreBtn.setFont(smallViewFont);
        editableOnRestoreBtn.setEnabled(true);
        editableOnRestoreBtn.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                editableOnRestore = ((Button) e.getSource()).getSelection();
            }

        });

        /*
         * Allow the user to control whether this view (ViewPart) is minimized
         * when the user loads another tool (e.g. Points, Baselines, etc).
         */
        minimizeOnForeignToolBtn = new Button(smallFlagsComposite, SWT.CHECK);
        minimizeOnForeignToolBtn.setSelection(minimizeOnForeignToolLoad);
        minimizeOnForeignToolBtn.setText("Minimize on foreign tool");
        minimizeOnForeignToolBtn.setLayoutData(new GridData(SWT.LEFT,
                SWT.CENTER, true, false, 4, 1));
        minimizeOnForeignToolBtn.setFont(smallViewFont);
        minimizeOnForeignToolBtn.setEnabled(true);
        minimizeOnForeignToolBtn.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                minimizeOnForeignToolLoad = ((Button) e.getSource())
                        .getSelection();
            }

        });

        /*
         * Allow the user to control whether the ensemble tool view is minimized
         * when the active tool layer is toggled to uneditable.
         */
        minimizeOnToggleUneditableBtn = new Button(smallFlagsComposite,
                SWT.CHECK);
        minimizeOnToggleUneditableBtn.setSelection(minimizeOnToggleUneditable);
        minimizeOnToggleUneditableBtn.setText("Minimize on toggle uneditable");
        minimizeOnToggleUneditableBtn.setLayoutData(new GridData(SWT.LEFT,
                SWT.CENTER, true, false, 5, 1));
        minimizeOnToggleUneditableBtn.setFont(smallViewFont);
        minimizeOnToggleUneditableBtn.setEnabled(true);
        minimizeOnToggleUneditableBtn
                .addSelectionListener(new SelectionAdapter() {

                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        minimizeOnToggleUneditable = ((Button) e.getSource())
                                .getSelection();
                    }

                });

        /*
         * Allow the user to control whether the Prefs tab automatically swtches
         * back to the Info tab after use.
         */
        autoHidePreferencesBtn = new Button(smallFlagsComposite, SWT.CHECK);
        autoHidePreferencesBtn.setSelection(autoHidePreferences);
        autoHidePreferencesBtn.setText("Auto-Hide Preferences");
        autoHidePreferencesBtn.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER,
                true, false, 5, 1));
        autoHidePreferencesBtn.setFont(smallViewFont);
        autoHidePreferencesBtn.setEnabled(true);
        autoHidePreferencesBtn.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                autoHidePreferences = ((Button) e.getSource()).getSelection();
            }

        });

        /*
         * Allow the user to control the way swapping-in a pane containing an
         * ensemble tool layer effects the editability of the tool layer.
         */
        editableOnSwapInBtn = new Button(smallFlagsComposite, SWT.CHECK);
        editableOnSwapIn = false;
        editableOnSwapInBtn.setSelection(editableOnSwapIn);
        editableOnSwapInBtn.setText("Make editable on swap-in");
        editableOnSwapInBtn.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER,
                true, false, 4, 1));
        editableOnSwapInBtn.setFont(smallViewFont);
        // btnEditableOnSwapIn.setEnabled(true);
        editableOnSwapInBtn.setEnabled(false);
        editableOnSwapInBtn.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                editableOnSwapIn = ((Button) e.getSource()).getSelection();
            }

        });

        /*
         * Allow the user to control whether the active ensemble tool layer
         * should be made uneditable when this view (ViewPart) is minimized.
         */
        uneditableOnMinimizeBtn = new Button(smallFlagsComposite, SWT.CHECK);
        editableOnSwapIn = false;
        uneditableOnMinimizeBtn.setSelection(uneditableOnMinimize);
        uneditableOnMinimizeBtn.setText("Make uneditable on minimize");
        uneditableOnMinimizeBtn.setLayoutData(new GridData(SWT.LEFT,
                SWT.CENTER, true, false, 4, 1));
        uneditableOnMinimizeBtn.setFont(smallViewFont);
        // btnUneditableOnMinimize.setEnabled(true);
        uneditableOnMinimizeBtn.setEnabled(false);
        uneditableOnMinimizeBtn.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                uneditableOnMinimize = ((Button) e.getSource()).getSelection();
            }

        });

        /*
         * Allow the user to control, when a new editor is opened, whether a new
         * ensemble tool layer is created and made editable.
         */

        createToolLayerOnNewEditorBtn = new Button(smallFlagsComposite,
                SWT.CHECK);
        createToolLayerOnNewEditorBtn.setSelection(createToolLayerOnNewEditor);
        createToolLayerOnNewEditorBtn.setText("New tool layer on new editor");
        createToolLayerOnNewEditorBtn.setLayoutData(new GridData(SWT.LEFT,
                SWT.CENTER, true, false, 5, 1));
        createToolLayerOnNewEditorBtn.setFont(smallViewFont); //
        createToolLayerOnNewEditorBtn.setEnabled(false);
        createToolLayerOnNewEditorBtn
                .addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        createToolLayerOnNewEditor = ((Button) e.getSource())
                                .getSelection();
                    }

                });

    }

    private void fillInfoTab(TabFolder lowerSashTbFldr) {

        resourceInfoTabItem = new TabItem(lowerSashTbFldr, SWT.NONE);
        // tabResourceInfo.setText("  Info  ");
        resourceInfoTabItem
                .setImage(EnsembleToolViewerImageStore.TAB_INFO_ENABLED_IMG);
        GridData lowerSashTbFldr_gd = new GridData(GridData.FILL_BOTH);
        lowerSashTbFldr.setLayoutData(lowerSashTbFldr_gd);

        createTimeSeriesInfo();
        createPlanViewInfo();
    }

    public void updateInfoTab() {

        VizApp.runAsync(new Runnable() {
            public void run() {
                if (isViewerTreeReady()) {
                    if (editorResourceType == ResourceType.PLAN_VIEW) {
                        updatePlanViewInfo(EnsembleTool.getInstance()
                                .getTimeBasisResourceName(), EnsembleTool
                                .getInstance().getTimeBasisLegendTime());
                        resourceInfoTabItem
                                .setControl(planViewInfoTabComposite);
                        timeSeriesInfoTabComposite.setVisible(false);
                        planViewInfoTabComposite.setVisible(true);
                    } else if (editorResourceType == ResourceType.TIME_SERIES) {
                        updateTimeSeriesInfo(EnsembleTool.getInstance()
                                .getTimeSeriesPoint());
                        resourceInfoTabItem
                                .setControl(timeSeriesInfoTabComposite);
                        planViewInfoTabComposite.setVisible(false);
                        timeSeriesInfoTabComposite.setVisible(true);
                    }
                    lowerSashTabFolder.setSelection(resourceInfoTabItem);
                    // tabFolder_lowerSash.redraw();
                    // tabFolder_lowerSash.update();
                }
            }
        });
    }

    private void createPlanViewInfo() {

        planViewInfoTabComposite = new Composite(lowerSashTabFolder, SWT.BORDER);
        GridLayout infoComposite_gl = new GridLayout(3, false);
        infoComposite_gl.horizontalSpacing = 2;
        infoComposite_gl.verticalSpacing = 3;
        planViewInfoTabComposite.setLayout(infoComposite_gl);

        primaryRscTimeLbl = new Label(planViewInfoTabComposite, SWT.BORDER);
        primaryRscTimeLbl.setFont(SWTResourceManager.getFont("Dialog", 10,
                SWT.NONE));
        primaryRscTimeLbl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER,
                false, false, 1, 1));
        primaryRscTimeLbl.setText(" Time: ");

        frameTimeUsingBasisLbl = new Label(planViewInfoTabComposite, SWT.BORDER);
        frameTimeUsingBasisLbl.setFont(SWTResourceManager.getFont("Dialog", 10,
                SWT.BOLD));
        frameTimeUsingBasisLbl.setAlignment(SWT.CENTER);
        frameTimeUsingBasisLbl.setForeground(SWTResourceManager.getColor(0, 0,
                0));
        frameTimeUsingBasisLbl.setBackground(SWTResourceManager.LIGHT_YELLOW);
        GridData frameTimeUsingBasisLbl_gd = new GridData(SWT.FILL, SWT.CENTER,
                true, false, 2, 1);
        frameTimeUsingBasisLbl_gd.heightHint = 22;
        frameTimeUsingBasisLbl_gd.widthHint = 150;
        frameTimeUsingBasisLbl.setLayoutData(frameTimeUsingBasisLbl_gd);

        primaryRscLbl = new Label(planViewInfoTabComposite, SWT.BORDER);
        primaryRscLbl.setFont(SWTResourceManager.getFont("Dialog", 10,
                SWT.NORMAL));
        primaryRscLbl.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false,
                false, 1, 1));
        primaryRscLbl.setText(" Basis: ");

        timeMatchResourceLbl = new Label(planViewInfoTabComposite, SWT.BORDER);
        timeMatchResourceLbl.setFont(SWTResourceManager.getFont("Dialog", 9,
                SWT.BOLD));
        timeMatchResourceLbl.setBackground(SWTResourceManager.LIGHT_YELLOW);
        timeMatchResourceLbl.setAlignment(SWT.CENTER);
        GridData timeMatchResourceLbl_gd = new GridData(SWT.FILL, SWT.CENTER,
                true, false, 2, 1);
        timeMatchResourceLbl_gd.widthHint = 130;
        timeMatchResourceLbl_gd.heightHint = 20;
        timeMatchResourceLbl.setLayoutData(timeMatchResourceLbl_gd);

        // need to fill some space
        Composite fillerComposite = new Composite(planViewInfoTabComposite,
                SWT.NONE);
        fillerComposite.setSize(20, 150);
        GridData filler_gd = new GridData(SWT.FILL, SWT.CENTER, true, false, 3,
                1);
        filler_gd.widthHint = 150;
        filler_gd.heightHint = 22;
        fillerComposite.setLayoutData(filler_gd);

    }

    private void createTimeSeriesInfo() {

        timeSeriesInfoTabComposite = new Composite(lowerSashTabFolder,
                SWT.BORDER);
        GridLayout timeSeriesInfoComposite_gl = new GridLayout(3, false);
        timeSeriesInfoComposite_gl.horizontalSpacing = 2;
        timeSeriesInfoComposite_gl.verticalSpacing = 3;
        timeSeriesInfoTabComposite.setLayout(timeSeriesInfoComposite_gl);

        timeSeriesPointLabelLbl = new Label(timeSeriesInfoTabComposite,
                SWT.BORDER);
        timeSeriesPointLabelLbl.setFont(SWTResourceManager.getFont("Dialog",
                10, SWT.NONE));
        timeSeriesPointLabelLbl.setLayoutData(new GridData(SWT.FILL,
                SWT.CENTER, false, false, 1, 1));
        timeSeriesPointLabelLbl.setText(" Point: ");

        timeSeriesPointValueLbl = new Label(timeSeriesInfoTabComposite,
                SWT.BORDER);
        timeSeriesPointValueLbl.setFont(SWTResourceManager.getFont("Dialog",
                10, SWT.BOLD));
        timeSeriesPointValueLbl.setAlignment(SWT.CENTER);
        timeSeriesPointValueLbl.setForeground(SWTResourceManager.getColor(0, 0,
                0));
        timeSeriesPointValueLbl.setBackground(SWTResourceManager.LIGHT_YELLOW);
        GridData timeSeriesPointValueLbl_gd = new GridData(SWT.FILL,
                SWT.CENTER, true, false, 2, 1);
        timeSeriesPointValueLbl_gd.heightHint = 22;
        timeSeriesPointValueLbl_gd.widthHint = 150;
        timeSeriesPointValueLbl.setLayoutData(timeSeriesPointValueLbl_gd);

        // need to fill some space
        Composite fillerComposite = new Composite(timeSeriesInfoTabComposite,
                SWT.NONE);
        fillerComposite.setSize(20, 150);
        GridData fillerComposite_gd = new GridData(SWT.FILL, SWT.CENTER, true,
                false, 3, 1);
        fillerComposite_gd.widthHint = 150;
        fillerComposite_gd.heightHint = 22;
        fillerComposite.setLayoutData(fillerComposite_gd);
    }

    protected void updatePlanViewInfo(String timeBasisRscName, String datatime) {
        if (!timeMatchResourceLbl.isDisposed()) {
            timeMatchResourceLbl.setText(timeBasisRscName);
            frameTimeUsingBasisLbl.setText(datatime);
        }
    }

    protected void updateTimeSeriesInfo(String pointValue) {
        if (!timeSeriesPointValueLbl.isDisposed()) {
            timeSeriesPointValueLbl.setText(pointValue);
        }
    }

    /*
     * Called from a class that is listening to the frameChange event.
     */
    public void frameChanged(FramesInfo framesInfo) {

        currentFramesInfo = framesInfo;
        updateLegendTimeInfo();

        VizApp.runAsync(new Runnable() {
            public void run() {
                if (isViewerTreeReady()) {
                    ensembleTreeViewer.refresh(true);
                }
            }
        });

    }

    public void updateLegendTimeInfo() {
        VizApp.runAsync(new Runnable() {
            public void run() {
                if (EnsembleTool.getInstance().getTimeBasisResourceName() != null) {
                    updatePlanViewInfo(EnsembleTool.getInstance()
                            .getTimeBasisResourceName(), EnsembleTool
                            .getInstance().getTimeBasisLegendTime());
                }
            }
        });
    }

    /*
     * Keep track of the expansion state of the tree.
     */
    private class EnsembleTreeExpandCollapseListener implements
            ITreeViewerListener {

        @Override
        public void treeCollapsed(TreeExpansionEvent event) {

            VizApp.runAsync(new Runnable() {
                @Override
                public void run() {
                    updateExpansionState();
                }
            });
        }

        @Override
        public void treeExpanded(TreeExpansionEvent event) {
            VizApp.runAsync(new Runnable() {
                @Override
                public void run() {
                    updateExpansionState();
                }
            });
        }
    }

    protected void updateCursor(final Cursor c) {
        VizApp.runAsync(new Runnable() {

            @Override
            public void run() {
                if (isViewerTreeReady()) {
                    ensembleTree.setCursor(c);
                }
            }

        });
    }

    protected class UpperTabSelectionListener extends SelectionAdapter {

        @Override
        public void widgetSelected(SelectionEvent e) {

            CTabItem ti = tabEnsemblesMainTabFolder.getSelection();
            if (viewEditable) {
                if (ti == itemLegendsTabItem) {
                    itemLegendsTabItem
                            .setImage(EnsembleToolViewerImageStore.TAB_LEGENDS_ENABLED_SELECTED_IMG);
                    itemMatrixTabItem
                            .setImage(EnsembleToolViewerImageStore.TAB_MATRIX_ENABLED_UNSELECTED_IMG);
                }
                if (ti == itemMatrixTabItem) {
                    itemLegendsTabItem
                            .setImage(EnsembleToolViewerImageStore.TAB_LEGENDS_ENABLED_UNSELECTED_IMG);
                    itemMatrixTabItem
                            .setImage(EnsembleToolViewerImageStore.TAB_MATRIX_ENABLED_SELECTED_IMG);
                }
            } else {
                itemLegendsTabItem
                        .setImage(EnsembleToolViewerImageStore.TAB_LEGENDS_DISABLED_IMG);
                itemMatrixTabItem
                        .setImage(EnsembleToolViewerImageStore.TAB_MATRIX_DISABLED_IMG);
            }

        }
    }

    /*
     * Allow user to change SREF member colors based on a chosen color pattern
     * map.
     */
    protected class SREFMembersColorChangeJob extends Job {

        public SREFMembersColorChangeJob(String name) {
            super(name);
        }

        @Override
        protected IStatus run(IProgressMonitor monitor) {
            IStatus status = null;

            Color currColor = null;
            for (GenericResourceHolder gRsc : getPerturbationMembers()) {
                if ((gRsc instanceof GridResourceHolder)
                        || (gRsc instanceof TimeSeriesResourceHolder)) {
                    AbstractVizResource<?, ?> rsc = gRsc.getRsc();
                    String ensId = gRsc.getEnsembleIdRaw();
                    if ((ensId != null) && (ensId.length() > 1)) {
                        currColor = ChosenSREFColors.getInstance()
                                .getGradientByEnsembleId(ensId);
                        rsc.getCapability(ColorableCapability.class).setColor(
                                currColor.getRGB());
                    }
                }
            }
            status = Status.OK_STATUS;

            return status;
        }

    }

    /*
     * Allow user to change GEFS member colors based on a chosen color pattern
     * map.
     */
    protected class GEFSMembersColorChangeJob extends Job {

        public GEFSMembersColorChangeJob(String name) {
            super(name);
        }

        @Override
        protected IStatus run(IProgressMonitor monitor) {
            IStatus status = null;

            Color currColor = null;
            int count = 0;

            for (GenericResourceHolder gRsc : getPerturbationMembers()) {
                if ((gRsc instanceof GridResourceHolder)
                        || (gRsc instanceof TimeSeriesResourceHolder)) {
                    count++;
                    AbstractVizResource<?, ?> rsc = gRsc.getRsc();
                    if (count == 1) {
                        currentEnsembleRsc = rsc;
                    }
                    String ensId = gRsc.getEnsembleIdRaw();
                    if ((ensId != null) && (ensId.length() > 1)) {
                        currColor = ChosenGEFSColors.getInstance()
                                .getGradientByEnsembleId(ensId);
                        rsc.getCapability(ColorableCapability.class).setColor(
                                currColor.getRGB());
                        rsc.getCapability(OutlineCapability.class);
                    }
                }
            }

            status = Status.OK_STATUS;
            return status;
        }

    }

    /*
     * This job toggles visibility for an individual product or ensemble member.
     */
    protected class ToggleProductVisiblityJob extends Job {

        private TreeItem treeItem = null;

        public ToggleProductVisiblityJob(String name) {
            super(name);
        }

        public void setTargetTreeItem(TreeItem ti) {
            treeItem = ti;
        }

        @Override
        protected IStatus run(IProgressMonitor monitor) {
            IStatus status = null;

            if (treeItem == null) {
                status = Status.CANCEL_STATUS;
            } else {
                toggleItemVisible(treeItem);

                /*
                 * if this was the last item to be toggled off, for example, in
                 * an ensemble group, then you need to toggle the parent tree
                 * item off also
                 */

                matchParentToChildrenVisibility(treeItem);

                status = Status.OK_STATUS;
            }
            return status;
        }

    }

    /*
     * This job toggles visibility for all members of an ensemble product.
     */
    protected class ToggleEnsembleVisiblityJob extends Job {

        private String ensembleName = null;

        public ToggleEnsembleVisiblityJob(String name) {
            super(name);
        }

        public void setTargetEnsembleProduct(String en) {
            ensembleName = en;
        }

        @Override
        protected IStatus run(IProgressMonitor monitor) {
            IStatus status = null;

            if (ensembleName == null) {
                status = Status.CANCEL_STATUS;
            } else {
                updateCursor(waitCursor);
                TreeItem ensembleRootItem = findTreeItemByLabelName(ensembleName);

                TreeItem[] descendants = getDirectDescendants(ensembleRootItem);
                TreeItem ti = null;
                int numDescendants = descendants.length;
                for (int i = 0; i < numDescendants; i++) {
                    ti = descendants[i];
                    toggleItemVisible(ti);

                    if (i == numDescendants - 1) {
                        /*
                         * if this was the last item to be toggled off, for the
                         * ensemble group, then you need to toggle the parent
                         * tree item off also
                         */
                        matchParentToChildrenVisibility(ti);
                    }

                }
                updateCursor(normalCursor);
                status = Status.OK_STATUS;
            }
            return status;
        }

    }

    private boolean isViewerTreeReady() {
        boolean isReady = false;

        if (ensembleTreeViewer != null && ensembleTree != null
                && ensembleTreeViewer.getTree() != null
                && !ensembleTreeViewer.getTree().isDisposed()) {
            isReady = true;
        }

        return isReady;
    }

    @Override
    public void setFocus() {
        ensembleTreeViewer.getControl().setFocus();
    }

}
