package gov.noaa.gsd.viz.ensemble.util.diagnostic;

import gov.noaa.gsd.viz.ensemble.navigator.ui.layer.EnsembleTool;
import gov.noaa.gsd.viz.ensemble.util.SWTResourceManager;

import java.util.List;

import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import com.raytheon.uf.viz.core.VizApp;
import com.raytheon.viz.ui.dialogs.CaveJFACEDialog;
import com.raytheon.viz.ui.dialogs.ICloseCallback;

/**
 * Used as an Ensemble Tool diagnostic trace dialog.
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

public class EnsembleToolDiagnosticStateDialog extends CaveJFACEDialog
        implements ICloseCallback {

    public static final int DIALOG_WIDTH = 950;

    public static final int DIALOG_HEIGHT = 1041;

    private TableColumnLayout diagnosticTraceColumnLayout = null;

    private TableViewer diagnosticTraceViewer = null;

    private TableColumnLayout toolLayerDataColumnLayout = null;

    private TableViewer toolLayerDataViewer = null;

    private final DialogMovedOrResizedListener saveShellLocationAndSizeListener = new DialogMovedOrResizedListener();

    private Shell thisShell = null;

    private Text stackTraceTextArea = null;

    public EnsembleToolDiagnosticStateDialog(Shell parentShell) {
        super(parentShell);
        setBlockOnOpen(false);
        setCloseCallback(this);
    }

    /**
     * Create contents of the dialog.
     * 
     * @param parent
     */
    @Override
    protected Control createDialogArea(Composite parent) {

        parent.setLayout(new GridLayout(1, true));

        Composite upperRootContainer = new Composite(parent, SWT.BORDER);
        upperRootContainer.setBackground(SWTResourceManager.PALE_WEAK_GREEN);
        GridLayout upperRootContainer_gl = new GridLayout();
        upperRootContainer_gl.marginTop = 2;
        upperRootContainer_gl.marginBottom = 0;
        upperRootContainer_gl.marginWidth = 3;
        upperRootContainer.setLayout(upperRootContainer_gl);

        Composite upperUpperRootContainer = new Composite(upperRootContainer,
                SWT.None);
        GridData upperUpperRootcontainer_gd = new GridData();
        upperUpperRootcontainer_gd.horizontalAlignment = SWT.FILL;
        upperUpperRootcontainer_gd.verticalAlignment = SWT.FILL;
        upperUpperRootcontainer_gd.grabExcessHorizontalSpace = true;
        upperUpperRootcontainer_gd.grabExcessVerticalSpace = false;
        upperUpperRootcontainer_gd.heightHint = 450;
        upperUpperRootcontainer_gd.widthHint = 927;
        upperUpperRootContainer.setLayoutData(upperUpperRootcontainer_gd);

        // define the TableViewer
        diagnosticTraceViewer = new TableViewer(upperUpperRootContainer,
                SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);

        diagnosticTraceViewer
                .addSelectionChangedListener(new ISelectionChangedListener() {
                    public void selectionChanged(
                            final SelectionChangedEvent event) {
                        IStructuredSelection selection = (IStructuredSelection) event
                                .getSelection();
                        Object o = selection.getFirstElement();
                        if (o instanceof EnsembleToolDiagnosticTraceMessage) {
                            EnsembleToolDiagnosticTraceMessage msg = (EnsembleToolDiagnosticTraceMessage) o;
                            showStackTrace(msg);
                        }
                    }
                });
        // Add TableColumnLayout
        diagnosticTraceColumnLayout = new TableColumnLayout();
        upperUpperRootContainer.setLayout(diagnosticTraceColumnLayout);
        createDiagnosticTraceColumns(diagnosticTraceViewer);

        // define layout for the viewer
        GridData diagnosticTrace_gd = new GridData();
        diagnosticTrace_gd.horizontalAlignment = SWT.FILL;
        diagnosticTrace_gd.verticalAlignment = SWT.TOP;
        diagnosticTrace_gd.grabExcessHorizontalSpace = true;
        diagnosticTrace_gd.grabExcessVerticalSpace = false;
        diagnosticTraceViewer.getControl().setLayoutData(diagnosticTrace_gd);

        // make lines and header visible
        final Table diagnosticTraceTable = diagnosticTraceViewer.getTable();
        diagnosticTraceTable.setHeaderVisible(true);
        diagnosticTraceTable.setLinesVisible(true);

        diagnosticTraceViewer.setContentProvider(ArrayContentProvider
                .getInstance());

        diagnosticTraceViewer
                .setLabelProvider(new VLTDiagnosticTableLabelProvider());

        diagnosticTraceViewer.setInput(EnsembleTool.getInstance()
                .getDiagnosticTraceMessages());

        Composite middleUpperRootContainer = new Composite(upperRootContainer,
                SWT.None);
        GridLayout middleContainer_gl = new GridLayout(1, true);
        middleContainer_gl.marginHeight = 0;
        middleContainer_gl.marginWidth = 0;
        middleUpperRootContainer.setLayout(middleContainer_gl);

        GridData middleContainer_gd = new GridData();
        middleContainer_gd.horizontalAlignment = SWT.FILL;
        middleContainer_gd.verticalAlignment = SWT.FILL;
        middleContainer_gd.grabExcessHorizontalSpace = true;
        middleContainer_gd.grabExcessVerticalSpace = true;
        middleContainer_gd.heightHint = 320;
        middleUpperRootContainer.setLayoutData(middleContainer_gd);

        stackTraceTextArea = new Text(middleUpperRootContainer, SWT.BORDER
                | SWT.H_SCROLL | SWT.V_SCROLL | SWT.WRAP);

        GridData stackTrace_gd = new GridData();
        stackTrace_gd.grabExcessHorizontalSpace = true;
        stackTrace_gd.grabExcessVerticalSpace = true;
        stackTrace_gd.horizontalAlignment = SWT.FILL;
        stackTrace_gd.verticalAlignment = SWT.FILL;
        stackTraceTextArea.setLayoutData(stackTrace_gd);

        Composite lowerRootContainer = new Composite(parent, SWT.None);
        lowerRootContainer.setBackground(SWTResourceManager.ATOMIC_TANGERINE);
        GridLayout lowerRootContainer_gl = new GridLayout();
        lowerRootContainer_gl.marginTop = 3;
        lowerRootContainer_gl.marginBottom = 9;
        lowerRootContainer_gl.marginWidth = 11;
        lowerRootContainer.setLayout(upperRootContainer_gl);

        Composite lowerContainer = new Composite(lowerRootContainer, SWT.None);

        GridData lowerContainer_gd = new GridData();
        lowerContainer_gd.verticalAlignment = GridData.FILL;
        lowerContainer_gd.grabExcessHorizontalSpace = true;
        lowerContainer_gd.grabExcessVerticalSpace = false;
        lowerContainer_gd.horizontalAlignment = GridData.FILL;
        lowerContainer_gd.heightHint = 189;
        lowerContainer_gd.widthHint = DIALOG_WIDTH - 23;
        lowerContainer.setLayoutData(lowerContainer_gd);

        // define the TableViewer
        toolLayerDataViewer = new TableViewer(lowerContainer, SWT.SINGLE
                | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);

        // Add TableColumnLayout
        toolLayerDataColumnLayout = new TableColumnLayout();
        lowerContainer.setLayout(toolLayerDataColumnLayout);
        createToolLayerDataColumns(toolLayerDataViewer);

        // make lines and header visible
        final Table toolLayerDataTable = toolLayerDataViewer.getTable();
        toolLayerDataTable.setHeaderVisible(true);
        toolLayerDataTable.setLinesVisible(true);

        // define layout for the viewer
        GridData toolLayerData_gd = new GridData();
        toolLayerData_gd.verticalAlignment = GridData.FILL;
        toolLayerData_gd.grabExcessHorizontalSpace = true;
        toolLayerData_gd.grabExcessVerticalSpace = true;
        toolLayerData_gd.horizontalAlignment = GridData.FILL;
        toolLayerData_gd.heightHint = 265;
        toolLayerData_gd.widthHint = DIALOG_WIDTH;
        toolLayerDataViewer.getControl().setLayoutData(toolLayerData_gd);

        toolLayerDataViewer.setContentProvider(ArrayContentProvider
                .getInstance());

        toolLayerDataViewer
                .setLabelProvider(new VLTToolLayerDataTableLabelProvider());

        toolLayerDataViewer.setInput(EnsembleTool.getInstance()
                .getDiagnosticToolLayerDataMessages());

        return upperUpperRootContainer;

    }

    private void createToolLayerDataColumns(TableViewer v) {

        final int columnWidth = 150;

        TableViewerColumn colDummySpacer_1 = new TableViewerColumn(v,
                SWT.CENTER);
        TableColumn dummySpacerColumn_1 = colDummySpacer_1.getColumn();
        dummySpacerColumn_1.setText("");
        dummySpacerColumn_1.setResizable(true);
        dummySpacerColumn_1.setMoveable(true);
        dummySpacerColumn_1.setWidth(75);
        toolLayerDataColumnLayout.setColumnData(dummySpacerColumn_1,
                new ColumnPixelData(75, true, false));

        TableViewerColumn colDisplayContainer = new TableViewerColumn(v,
                SWT.CENTER);
        TableColumn displayContainerColumn = colDisplayContainer.getColumn();
        displayContainerColumn.setText("Display Container");
        displayContainerColumn.setResizable(true);
        displayContainerColumn.setMoveable(true);
        displayContainerColumn.setWidth(columnWidth);
        toolLayerDataColumnLayout.setColumnData(displayContainerColumn,
                new ColumnPixelData(columnWidth, true, false));

        TableViewerColumn colToolLayer = new TableViewerColumn(v, SWT.CENTER);
        TableColumn toolLayerColumn = colToolLayer.getColumn();
        toolLayerColumn.setText("Tool Layer");
        toolLayerColumn.setResizable(true);
        toolLayerColumn.setMoveable(true);
        toolLayerColumn.setWidth(columnWidth + 50);
        toolLayerDataColumnLayout.setColumnData(toolLayerColumn,
                new ColumnPixelData(columnWidth + 50, true, false));

        TableViewerColumn colDummySpacer_2 = new TableViewerColumn(v,
                SWT.CENTER);
        TableColumn dummySpacerColumn_2 = colDummySpacer_2.getColumn();
        dummySpacerColumn_2.setText("");
        dummySpacerColumn_2.setResizable(true);
        dummySpacerColumn_2.setMoveable(true);
        dummySpacerColumn_2.setWidth(75);
        toolLayerDataColumnLayout.setColumnData(dummySpacerColumn_2,
                new ColumnPixelData(75, true, false));

    }

    private void createDiagnosticTraceColumns(TableViewer v) {

        TableViewerColumn colCount = new TableViewerColumn(v, SWT.CENTER);
        TableColumn countColumn = colCount.getColumn();
        countColumn.setText("Count");
        countColumn.setResizable(true);
        countColumn.setMoveable(true);
        countColumn.setWidth(50);
        diagnosticTraceColumnLayout.setColumnData(countColumn,
                new ColumnPixelData(50, true, false));

        TableViewerColumn colUserAction = new TableViewerColumn(v, SWT.CENTER);
        TableColumn actionColumn = colUserAction.getColumn();
        actionColumn.setText("Action");
        actionColumn.setResizable(true);
        actionColumn.setMoveable(true);
        actionColumn.setWidth(180);
        diagnosticTraceColumnLayout.setColumnData(actionColumn,
                new ColumnPixelData(180, true, false));

        TableViewerColumn colToolLayer = new TableViewerColumn(v, SWT.CENTER);
        TableColumn toolLayerColumn = colToolLayer.getColumn();
        toolLayerColumn.setText("Tool Layer");
        toolLayerColumn.setResizable(true);
        toolLayerColumn.setMoveable(true);
        toolLayerColumn.setWidth(180);
        diagnosticTraceColumnLayout.setColumnData(toolLayerColumn,
                new ColumnPixelData(180, true, false));

        TableViewerColumn colIsEditable = new TableViewerColumn(v, SWT.CENTER);
        TableColumn isEditableColumn = colIsEditable.getColumn();
        isEditableColumn.setText("Editable");
        isEditableColumn.setResizable(true);
        isEditableColumn.setMoveable(true);
        isEditableColumn.setWidth(67);
        diagnosticTraceColumnLayout.setColumnData(isEditableColumn,
                new ColumnPixelData(67, true, false));

        TableViewerColumn colEditor = new TableViewerColumn(v, SWT.CENTER);
        TableColumn editorColumn = colEditor.getColumn();
        editorColumn.setText("Editor");
        editorColumn.setResizable(true);
        editorColumn.setMoveable(true);
        editorColumn.setWidth(100);
        diagnosticTraceColumnLayout.setColumnData(editorColumn,
                new ColumnPixelData(100, true, false));

        TableViewerColumn colActiveEditor = new TableViewerColumn(v, SWT.CENTER);
        TableColumn activeEditorColumn = colActiveEditor.getColumn();
        activeEditorColumn.setText("Active Editor");
        activeEditorColumn.setResizable(true);
        activeEditorColumn.setMoveable(true);
        activeEditorColumn.setWidth(100);
        diagnosticTraceColumnLayout.setColumnData(activeEditorColumn,
                new ColumnPixelData(100, true, false));

        TableViewerColumn colMethodName = new TableViewerColumn(v, SWT.CENTER);
        TableColumn methodColumn = colMethodName.getColumn();
        methodColumn.setText("Method");
        methodColumn.setResizable(true);
        methodColumn.setMoveable(true);
        methodColumn.setWidth(200);
        diagnosticTraceColumnLayout.setColumnData(methodColumn,
                new ColumnPixelData(200, true, true));

    }

    private void showStackTrace(final EnsembleToolDiagnosticTraceMessage msg) {

        VizApp.runAsync(new Runnable() {

            @Override
            public void run() {
                if (!stackTraceTextArea.isDisposed()) {
                    stackTraceTextArea.setText("");

                    if (msg != null) {
                        for (StackTraceElement e : msg.getStackTrace()) {
                            // TODO Make this so these aren't hard-coded strings
                            if ((e.toString().indexOf(
                                    "EnsembleToolDiagnosticTraceMessage") == -1)
                                    && (e.toString()
                                            .indexOf(
                                                    "EnsembleToolManager.diagnosticTrace") == -1)) {
                                stackTraceTextArea.append(e.toString());
                            }
                        }
                        stackTraceTextArea.setSelection(0);
                    }
                }
            }

        });

    }

    public void setTraceMessages(
            final List<EnsembleToolDiagnosticTraceMessage> msgs) {
        VizApp.runAsync(new Runnable() {

            @Override
            public void run() {
                if (diagnosticTraceViewer != null
                        && !diagnosticTraceViewer.getTable().isDisposed()) {
                    diagnosticTraceViewer.setInput(msgs);
                    int count = diagnosticTraceViewer.getTable().getItemCount();
                    TableItem item = diagnosticTraceViewer.getTable().getItem(
                            count - 1);
                    if (!item.isDisposed()) {
                        diagnosticTraceViewer.getTable().showItem(item);
                        diagnosticTraceViewer.getTable().select(count - 1);
                        if (item.getData() instanceof EnsembleToolDiagnosticTraceMessage) {
                            EnsembleToolDiagnosticTraceMessage msg = (EnsembleToolDiagnosticTraceMessage) item
                                    .getData();
                            showStackTrace(msg);
                        }
                    }
                    diagnosticTraceViewer.refresh();
                }
            }
        });
    }

    public void setToolLayerDataMessages(
            final List<EnsembleToolLayerDataMessage> msgs) {
        VizApp.runAsync(new Runnable() {

            @Override
            public void run() {
                if (!toolLayerDataViewer.getControl().isDisposed()) {
                    toolLayerDataViewer.setInput(msgs);
                }
            }

        });
    }

    private class VLTDiagnosticTableLabelProvider extends LabelProvider
            implements ITableLabelProvider {
        public Image getColumnImage(Object element, int columnIndex) {
            return null;
        }

        public String getColumnText(Object element, int columnIndex) {
            EnsembleToolDiagnosticTraceMessage message = (EnsembleToolDiagnosticTraceMessage) element;
            String result = "";
            switch (columnIndex) {
            case 0:
                result = message.getCount();
                break;
            case 1:
                result = message.getAction();
                break;
            case 2:
                result = message.getToolLayerRef();
                break;
            case 3:
                result = message.getIsEditable();
                break;
            case 4:
                result = message.getEditorRef();
                break;
            case 5:
                result = message.getActiveEditorRef();
                break;
            case 6:
                result = message.getMethodName();
                break;
            default:
                // should not reach here
                result = "";
            }
            return result;
        }
    }

    private class VLTToolLayerDataTableLabelProvider extends LabelProvider
            implements ITableLabelProvider {
        public Image getColumnImage(Object element, int columnIndex) {
            return null;
        }

        public String getColumnText(Object element, int columnIndex) {
            EnsembleToolLayerDataMessage message = (EnsembleToolLayerDataMessage) element;
            String result = "";
            switch (columnIndex) {
            case 0:
                result = "";
                break;
            case 1:
                result = message.getDisplayContainerRef();
                break;
            case 2:
                result = message.getToolLayerName();
                break;
            case 3:
                result = "";
                break;
            default:
                // should not reach here
                result = "";
            }
            return result;
        }
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {

    }

    @Override
    protected Point getInitialLocation(Point initialSize) {
        return EnsembleTool.getInstance().getLastKnownShellLocation();
    }

    @Override
    protected Point getInitialSize() {
        return EnsembleTool.getInstance().getLastKnownShellSize();
    }

    @Override
    protected void setShellStyle(int arg) {
        super.setShellStyle(SWT.RESIZE);
    }

    @Override
    public void dialogClosed(Object returnValue) {
        EnsembleTool.getInstance().resetDiagnosticToggle();
        diagnosticTraceViewer = null;
    }

    @Override
    protected void configureShell(final Shell newShell) {
        super.configureShell(newShell);
        newShell.setText("View Tool Layer Diagnostic");
        thisShell = newShell;
        thisShell.addControlListener(saveShellLocationAndSizeListener);
    }

    private class DialogMovedOrResizedListener implements ControlListener {

        @Override
        public void controlMoved(ControlEvent e) {
            EnsembleTool.getInstance().setLastKnownShellLocation(
                    ((Shell) e.getSource()).getLocation());
        }

        @Override
        public void controlResized(ControlEvent e) {
            EnsembleTool.getInstance().setLastKnownShellSize(
                    ((Shell) e.getSource()).getSize());
        }

    }
}
