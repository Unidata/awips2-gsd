package gov.noaa.gsd.viz.ensemble.navigator.ui.viewer;

import gov.noaa.gsd.viz.ensemble.display.calculate.Calculation;
import gov.noaa.gsd.viz.ensemble.display.calculate.Range;
import gov.noaa.gsd.viz.ensemble.display.calculate.RangeType;
import gov.noaa.gsd.viz.ensemble.navigator.ui.layer.EnsembleTool;
import gov.noaa.gsd.viz.ensemble.util.GlobalColor;
import gov.noaa.gsd.viz.ensemble.util.SWTResourceManager;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.raytheon.viz.ui.dialogs.CaveJFACEDialog;

/*
 * A dialog which allows the user to choose create an ensemble relative 
 * frequency product from different probability ranges.
 * 
 * @author polster
 * @author jing
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jun 4, 2015               polster     Initial creation
 * 
 * </pre>
 * 
 * @version 1.0
 */

public class ERFProductDialog_Modal extends CaveJFACEDialog {

    public static enum ProbabilityRanges {
        INNER_RANGE, OUTER_RANGE, ABOVE, BELOW
    }

    private Text probabilityOfXInsideRangeLeftEntryTxt = null;

    private Text probabilityOfXInsideRangeRightEntryTxt = null;

    private Text probabilityOfXOutsideRangeLeftEntryTxt = null;

    private Text probabilityOfXOutsideRangeRightEntryTxt = null;

    private Text probabilityOfXAboveRangeEntryTxt = null;

    private Text probabilityOfXBelowRangeEntryTxt = null;

    private Button chooserRangeRdo_1 = null;

    private Button chooserRangeRdo_2 = null;

    private Button chooserRangeRdo_3 = null;

    private Button chooserRangeRdo_4 = null;

    private Label ensembleProductNameLbl = null;

    private String ensembleProductName = null;

    private ERFProductDialog_Modal.ProbabilityRanges probabilityRange = ERFProductDialog_Modal.ProbabilityRanges.INNER_RANGE;

    private double setProbabilityLowerValue = 0;

    private double setProbabilityUpperValue = 0;

    private ERFProductDialog_Modal(Shell parentShell) {
        super(parentShell);
    }

    protected ERFProductDialog_Modal(Shell parentShell, String rscName) {
        this(parentShell);
        ensembleProductName = rscName;
    }

    /**
     * Create contents of the dialog.
     * 
     * @param parent
     */
    @Override
    protected Control createDialogArea(Composite parent) {

        setBlockOnOpen(true);

        Composite rootComposite = (Composite) super.createDialogArea(parent);
        Composite mainPanelComposite = new Composite(rootComposite, SWT.BORDER);

        GridData rootCalculatorPanel_gd = new GridData(SWT.FILL, SWT.FILL,
                false, true, 1, 1);
        rootCalculatorPanel_gd.widthHint = 278;
        rootCalculatorPanel_gd.heightHint = 278;
        mainPanelComposite.setLayoutData(rootCalculatorPanel_gd);
        mainPanelComposite.setLayout(new GridLayout(5, false));

        Label dummySpacerLbl_A = new Label(mainPanelComposite, SWT.NONE);
        GridData dummySpacer_gd_A = new GridData(SWT.CENTER, SWT.CENTER, false,
                false, 5, 1);
        dummySpacer_gd_A.heightHint = 5;
        dummySpacerLbl_A.setLayoutData(dummySpacer_gd_A);

        this.createTitleWidget(mainPanelComposite);

        // height spacer
        Label dummySpacerLbl_B = new Label(mainPanelComposite, SWT.NONE);
        GridData dummySpacer_gd_B = new GridData(SWT.CENTER, SWT.CENTER, false,
                false, 5, 1);
        dummySpacer_gd_B.heightHint = 5;
        dummySpacerLbl_B.setLayoutData(dummySpacer_gd_B);

        this.createWithinRangeWidget(mainPanelComposite);

        this.createOutsideOfRangeWidget(mainPanelComposite);

        this.createAboveThresholdWidget(mainPanelComposite);

        this.createBelowThresholdWidget(mainPanelComposite);

        // Height spacer
        Label dummySpacerLbl_0 = new Label(mainPanelComposite, SWT.NONE);
        GridData dummySpacerLbl_gd_0 = new GridData(SWT.CENTER, SWT.CENTER,
                false, false, 3, 1);
        dummySpacerLbl_gd_0.heightHint = 5;
        dummySpacerLbl_0.setLayoutData(dummySpacerLbl_gd_0);

        Label dummySpacerLbl_1 = new Label(mainPanelComposite, SWT.NONE);
        GridData dummySpacerLbl_gd_1 = new GridData(SWT.LEFT, SWT.CENTER,
                false, false, 2, 1);
        // dummySpacerLbl_gd_1.widthHint = 78;
        dummySpacerLbl_1.setLayoutData(dummySpacerLbl_gd_1);

        // only one range-filter row-widget is enabled at a time
        probabilityOfXInsideRangeLeftEntryTxt.setEnabled(true);
        probabilityOfXInsideRangeRightEntryTxt.setEnabled(true);

        probabilityOfXOutsideRangeLeftEntryTxt.setEnabled(false);
        probabilityOfXOutsideRangeRightEntryTxt.setEnabled(false);

        probabilityOfXAboveRangeEntryTxt.setEnabled(false);
        probabilityOfXBelowRangeEntryTxt.setEnabled(false);

        this.addWidgetSelectionBehavior();

        this.addThresholdValueModifyBehavior();

        return parent;

    }

    private void createTitleWidget(Composite mainPanelComposite) {
        // this is the title box containing the ensemble name ... it has a
        // a slightly different background to highlight the title. It is also
        // centered.
        ensembleProductNameLbl = new Label(mainPanelComposite, SWT.CENTER
                | SWT.BORDER);
        ensembleProductNameLbl.setFont(SWTResourceManager.getFont("Dialog", 10,
                SWT.BOLD));
        ensembleProductNameLbl.setAlignment(SWT.CENTER);
        ensembleProductNameLbl.setForeground(SWTResourceManager.getColor(0, 0,
                0));
        ensembleProductNameLbl.setBackground(GlobalColor
                .get(GlobalColor.LIGHT_YELLOW));
        GridData frameTimeUsingBasisLbl_gd = new GridData(SWT.FILL, SWT.CENTER,
                true, false, 5, 1);
        ensembleProductNameLbl.setLayoutData(frameTimeUsingBasisLbl_gd);

    }

    private void createBelowThresholdWidget(Composite mainPanelComposite) {

        // This is the beginning of the BELOW A THRESHOLD row-widget.
        // Select this widget row by choosing the radio button which
        // precedes it.
        Composite rangeToolRootComposite_4 = new Composite(mainPanelComposite,
                SWT.BORDER);
        GridData rangeToolRootComposite_gd_4 = new GridData(SWT.FILL,
                SWT.CENTER, false, false, 5, 1);
        rangeToolRootComposite_4.setLayoutData(rangeToolRootComposite_gd_4);
        rangeToolRootComposite_4.setLayout(new GridLayout(7, false));
        rangeToolRootComposite_4
                .setToolTipText("ERF probability P(x) is below (%)");

        // Are you choosing the BELOW A THRESHOLD row-widget?
        chooserRangeRdo_4 = new Button(rangeToolRootComposite_4, SWT.RADIO);
        chooserRangeRdo_4.setSelection(false);

        // All the ui components for BELOW A THRESHOLD will have tool tip hints.

        // Put the "probability of x" label ...
        Label probabilityOfX_lbl_4 = new Label(rangeToolRootComposite_4,
                SWT.NONE);
        GridData probabilityOfX_lbl_gd_4 = new GridData(SWT.RIGHT, SWT.CENTER,
                false, false, 1, 1);
        probabilityOfX_lbl_gd_4.widthHint = 42;
        probabilityOfX_lbl_4.setLayoutData(probabilityOfX_lbl_gd_4);
        probabilityOfX_lbl_4.setText("P(x): ");
        probabilityOfX_lbl_4.setFont(SWTResourceManager.getFont("Serif", 11,
                SWT.BOLD | SWT.ITALIC));
        probabilityOfX_lbl_4.setToolTipText("Probability P(x) is below");

        // There's a lower bound text entry to this BELOW A THRESHOLD row-
        // widget.
        Label valueOfX_lbl_4 = new Label(rangeToolRootComposite_4, SWT.NONE);
        valueOfX_lbl_4.setAlignment(SWT.CENTER);
        GridData valueOfX_lbl_gd_4 = new GridData(SWT.RIGHT, SWT.CENTER, false,
                false, 2, 1);
        valueOfX_lbl_gd_4.widthHint = 45;
        valueOfX_lbl_4.setLayoutData(valueOfX_lbl_gd_4);
        valueOfX_lbl_4.setFont(SWTResourceManager.getFont("Serif", 12, SWT.BOLD
                | SWT.ITALIC));
        valueOfX_lbl_4.setText("x   < ");
        valueOfX_lbl_4.setToolTipText("ERF probability P(x) is below (%)");

        probabilityOfXBelowRangeEntryTxt = new Text(rangeToolRootComposite_4,
                SWT.BORDER | SWT.CENTER);
        GridData lowerRangeEntryTxt_gd_4 = new GridData(SWT.CENTER, SWT.CENTER,
                false, false, 1, 1);
        lowerRangeEntryTxt_gd_4.widthHint = 30;
        probabilityOfXBelowRangeEntryTxt.setLayoutData(lowerRangeEntryTxt_gd_4);
        probabilityOfXBelowRangeEntryTxt
                .setToolTipText("The threshold that 'x' is below");

        new Label(rangeToolRootComposite_4, SWT.NONE);
        new Label(rangeToolRootComposite_4, SWT.NONE);

    }

    private void createAboveThresholdWidget(Composite mainPanelComposite) {

        // This is the beginning of the ABOVE A THRESHOLD row-widget.
        // Select this widget row by choosing the radio button which
        // precedes it.
        Composite rangeToolRootComposite_3 = new Composite(mainPanelComposite,
                SWT.BORDER);
        GridData rangeToolRootComposite_gd_3 = new GridData(SWT.FILL,
                SWT.CENTER, false, false, 5, 1);
        rangeToolRootComposite_3.setLayoutData(rangeToolRootComposite_gd_3);
        rangeToolRootComposite_3.setLayout(new GridLayout(7, false));
        rangeToolRootComposite_3
                .setToolTipText("ERF probability P(x) is above (%)");

        // Are you choosing the ABOVE A THRESHOLD widget row?
        chooserRangeRdo_3 = new Button(rangeToolRootComposite_3, SWT.RADIO);
        chooserRangeRdo_3.setSelection(false);

        // All the ui components for ABOVE A THRESHOLD will have tool tip hints.

        // Put the "probability of x" label ...
        Label probabilityOfX_lbl_3 = new Label(rangeToolRootComposite_3,
                SWT.NONE);
        GridData probabilityOfX_lbl_gd_3 = new GridData(SWT.RIGHT, SWT.CENTER,
                false, false, 1, 1);
        probabilityOfX_lbl_gd_3.widthHint = 42;
        probabilityOfX_lbl_3.setLayoutData(probabilityOfX_lbl_gd_3);
        probabilityOfX_lbl_3.setText("P(x): ");
        probabilityOfX_lbl_3.setFont(SWTResourceManager.getFont("Serif", 11,
                SWT.BOLD | SWT.ITALIC));

        // There's an upper bound text entry to this ABOVE A THRESHOLD row-
        // widget.
        Label valueOfX_lbl_3 = new Label(rangeToolRootComposite_3, SWT.NONE);
        valueOfX_lbl_3.setAlignment(SWT.CENTER);
        GridData valueOfX_lbl_gd_3 = new GridData(SWT.RIGHT, SWT.CENTER, false,
                false, 2, 1);
        valueOfX_lbl_gd_3.widthHint = 45;
        valueOfX_lbl_3.setLayoutData(valueOfX_lbl_gd_3);
        valueOfX_lbl_3.setFont(SWTResourceManager.getFont("Serif", 12, SWT.BOLD
                | SWT.ITALIC));
        valueOfX_lbl_3.setText("x   > ");
        valueOfX_lbl_3.setToolTipText("ERF probability P(x) is above (%)");

        probabilityOfXAboveRangeEntryTxt = new Text(rangeToolRootComposite_3,
                SWT.BORDER | SWT.CENTER);
        GridData lowerRangeEntryTxt_gd_3 = new GridData(SWT.CENTER, SWT.CENTER,
                false, false, 1, 1);
        lowerRangeEntryTxt_gd_3.widthHint = 30;
        probabilityOfXAboveRangeEntryTxt.setLayoutData(lowerRangeEntryTxt_gd_3);
        probabilityOfXAboveRangeEntryTxt
                .setToolTipText("The threshold that 'x' is above");

        new Label(rangeToolRootComposite_3, SWT.NONE);
        new Label(rangeToolRootComposite_3, SWT.NONE);

    }

    private void createOutsideOfRangeWidget(Composite mainPanelComposite) {

        // This is the beginning of the OUTSIDE A RANGE widget row.
        // Select this widget row by choosing the radio button which
        // precedes it.
        Composite rangeToolRootComposite_2 = new Composite(mainPanelComposite,
                SWT.BORDER);
        GridData rangeToolRootComposite_gd_2 = new GridData(SWT.FILL,
                SWT.CENTER, false, false, 5, 1);
        rangeToolRootComposite_2.setLayoutData(rangeToolRootComposite_gd_2);
        rangeToolRootComposite_2.setLayout(new GridLayout(7, false));
        rangeToolRootComposite_2
                .setToolTipText("ERF probability P(x) is outside a range (%)");

        // Are you choosing the OUTSIDE A RANGE row-widget?
        chooserRangeRdo_2 = new Button(rangeToolRootComposite_2, SWT.RADIO);
        chooserRangeRdo_2.setSelection(false);

        // All the ui components for OUTSIDE A RANGE will have tool tip hints.

        // Put the "probability of x" label ...
        Label probabilityOfX_lbl_2 = new Label(rangeToolRootComposite_2,
                SWT.NONE);
        GridData probabilityOfX_lbl_gd_2 = new GridData(SWT.CENTER, SWT.CENTER,
                false, false, 1, 1);
        probabilityOfX_lbl_gd_2.widthHint = 50;
        probabilityOfX_lbl_2.setLayoutData(probabilityOfX_lbl_gd_2);
        probabilityOfX_lbl_2.setText("P(x):");
        probabilityOfX_lbl_2.setFont(SWTResourceManager.getFont("Serif", 11,
                SWT.BOLD | SWT.ITALIC));
        probabilityOfX_lbl_2
                .setToolTipText("ERF probability P(x) is outside a range (%)");

        // There's a lower bound text entry to this OUTSIDE A RANGE row-widget.
        probabilityOfXOutsideRangeLeftEntryTxt = new Text(
                rangeToolRootComposite_2, SWT.BORDER | SWT.CENTER);
        GridData lowerRangeEntryTxt_gd_2 = new GridData(SWT.CENTER, SWT.CENTER,
                false, false, 1, 1);
        lowerRangeEntryTxt_gd_2.widthHint = 30;
        probabilityOfXOutsideRangeLeftEntryTxt
                .setLayoutData(lowerRangeEntryTxt_gd_2);
        probabilityOfXOutsideRangeLeftEntryTxt
                .setToolTipText("This must be the minimum value for 'x'");

        Label lowerConditionalLbl_2 = new Label(rangeToolRootComposite_2,
                SWT.NONE);
        GridData lowerConditionalLbl_gd_2 = new GridData(SWT.CENTER,
                SWT.CENTER, false, false, 3, 1);
        lowerConditionalLbl_2.setFont(SWTResourceManager.getFont("Serif", 11,
                SWT.BOLD));
        lowerConditionalLbl_gd_2.widthHint = 75;
        lowerConditionalLbl_2.setLayoutData(lowerConditionalLbl_gd_2);
        lowerConditionalLbl_2.setText("  >  x  > ");
        lowerConditionalLbl_2
                .setToolTipText("ERF probability P(x) is outside a range (%)");

        // There's an upper bound text entry to this OUTSIDE A RANGE row-widget.
        probabilityOfXOutsideRangeRightEntryTxt = new Text(
                rangeToolRootComposite_2, SWT.BORDER | SWT.CENTER);
        GridData upperRangeEntryTxt_gd_2 = new GridData(SWT.LEFT, SWT.CENTER,
                false, false, 1, 1);
        upperRangeEntryTxt_gd_2.widthHint = 30;
        probabilityOfXOutsideRangeRightEntryTxt
                .setLayoutData(upperRangeEntryTxt_gd_2);
        probabilityOfXOutsideRangeRightEntryTxt
                .setToolTipText("This must be the maximum value for 'x'");

    }

    private void createWithinRangeWidget(Composite mainPanelComposite) {
        //
        // This is the beginning of the WITHIN A RANGE row-widget
        // Select this widget row by choosing the radio button which
        // precedes it.
        Composite rangeToolRootComposite_1 = new Composite(mainPanelComposite,
                SWT.BORDER);
        rangeToolRootComposite_1.setLayout(new GridLayout(7, false));
        rangeToolRootComposite_1
                .setToolTipText("ERF probability P(x) is within a range (%)");
        GridData rangeToolRootComposite_gd_1 = new GridData(SWT.FILL,
                SWT.CENTER, true, false, 5, 1);
        rangeToolRootComposite_gd_1.verticalIndent = 1;
        rangeToolRootComposite_gd_1.horizontalIndent = 1;
        rangeToolRootComposite_1.setLayoutData(rangeToolRootComposite_gd_1);

        // Are you choosing the WITHIN A RANGE row-widget?
        chooserRangeRdo_1 = new Button(rangeToolRootComposite_1, SWT.RADIO);
        chooserRangeRdo_1.setSelection(true);

        // All the ui components for WITHIN A RANGE will have tool tip hints.

        // Put the "probability of x" label ...
        Label probabilityOfX_lbl_1 = new Label(rangeToolRootComposite_1,
                SWT.NONE);
        GridData probabilityOfX_lbl_gd_1 = new GridData(SWT.CENTER, SWT.CENTER,
                false, false, 1, 1);
        probabilityOfX_lbl_gd_1.widthHint = 50;
        probabilityOfX_lbl_1.setLayoutData(probabilityOfX_lbl_gd_1);
        probabilityOfX_lbl_1.setFont(SWTResourceManager.getFont("Serif", 11,
                SWT.BOLD | SWT.ITALIC));
        probabilityOfX_lbl_1.setText("P(x):   ");
        probabilityOfX_lbl_1
                .setToolTipText("ERF probability P(x) is within a range (%)");

        // There's a lower bound text entry to this WITHIN A RANGE row-widget.
        probabilityOfXInsideRangeLeftEntryTxt = new Text(
                rangeToolRootComposite_1, SWT.CENTER | SWT.BORDER);
        GridData lowerRangeEntryTxt_gd_1 = new GridData(SWT.CENTER, SWT.CENTER,
                false, false, 1, 1);
        lowerRangeEntryTxt_gd_1.widthHint = 30;
        probabilityOfXInsideRangeLeftEntryTxt
                .setLayoutData(lowerRangeEntryTxt_gd_1);
        probabilityOfXInsideRangeLeftEntryTxt
                .setToolTipText("This must be the minimum value for 'x'");

        Label lowerConditionalLbl_1 = new Label(rangeToolRootComposite_1,
                SWT.NONE);
        GridData lowerConditionalLbl_gd_1 = new GridData(SWT.CENTER,
                SWT.CENTER, false, false, 3, 1);
        lowerConditionalLbl_1.setFont(SWTResourceManager.getFont("Serif", 11,
                SWT.BOLD));
        lowerConditionalLbl_gd_1.widthHint = 75;
        lowerConditionalLbl_1.setLayoutData(lowerConditionalLbl_gd_1);
        lowerConditionalLbl_1.setText("  <  x  < ");
        lowerConditionalLbl_1
                .setToolTipText("ERF probability P(x) is within a range (%)");

        // There's an upper bound text entry to this WITHIN A RANGE row-widget.
        probabilityOfXInsideRangeRightEntryTxt = new Text(
                rangeToolRootComposite_1, SWT.CENTER | SWT.BORDER);
        GridData upperRangeEntryTxt_gd_1 = new GridData(SWT.LEFT, SWT.CENTER,
                false, false, 1, 1);
        upperRangeEntryTxt_gd_1.widthHint = 30;
        probabilityOfXInsideRangeRightEntryTxt
                .setLayoutData(upperRangeEntryTxt_gd_1);
        probabilityOfXInsideRangeRightEntryTxt
                .setToolTipText("This must be the maximum value for 'x'");

    }

    private void addWidgetSelectionBehavior() {

        // select the WITHIN A RANGE row-widget and deselect the others.
        chooserRangeRdo_1.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {

                chooserRangeRdo_1.setSelection(true);
                chooserRangeRdo_2.setSelection(false);
                chooserRangeRdo_3.setSelection(false);
                chooserRangeRdo_4.setSelection(false);

                clearProbabilityFields();

                probabilityOfXInsideRangeLeftEntryTxt.setEnabled(true);
                probabilityOfXOutsideRangeLeftEntryTxt.setEnabled(false);
                probabilityOfXAboveRangeEntryTxt.setEnabled(false);
                probabilityOfXBelowRangeEntryTxt.setEnabled(false);
                probabilityOfXInsideRangeRightEntryTxt.setEnabled(true);
                probabilityOfXOutsideRangeRightEntryTxt.setEnabled(false);

                probabilityOfXInsideRangeLeftEntryTxt.forceFocus();
                probabilityRange = ProbabilityRanges.INNER_RANGE;
            }
        });

        // select the OUTSIDE A RANGE row-widget and deselect the others.
        chooserRangeRdo_2.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {

                chooserRangeRdo_1.setSelection(false);
                chooserRangeRdo_2.setSelection(true);
                chooserRangeRdo_3.setSelection(false);
                chooserRangeRdo_4.setSelection(false);

                clearProbabilityFields();

                probabilityOfXInsideRangeLeftEntryTxt.setEnabled(false);
                probabilityOfXOutsideRangeLeftEntryTxt.setEnabled(true);
                probabilityOfXAboveRangeEntryTxt.setEnabled(false);
                probabilityOfXBelowRangeEntryTxt.setEnabled(false);
                probabilityOfXInsideRangeRightEntryTxt.setEnabled(false);
                probabilityOfXOutsideRangeRightEntryTxt.setEnabled(true);

                probabilityOfXOutsideRangeLeftEntryTxt.forceFocus();
                probabilityRange = ProbabilityRanges.OUTER_RANGE;

            }
        });

        // select the ABOVE A THRESHOLD and deselect the others.
        chooserRangeRdo_3.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {

                chooserRangeRdo_1.setSelection(false);
                chooserRangeRdo_2.setSelection(false);
                chooserRangeRdo_3.setSelection(true);
                chooserRangeRdo_4.setSelection(false);

                clearProbabilityFields();

                probabilityOfXInsideRangeLeftEntryTxt.setEnabled(false);
                probabilityOfXOutsideRangeLeftEntryTxt.setEnabled(false);
                probabilityOfXAboveRangeEntryTxt.setEnabled(true);
                probabilityOfXBelowRangeEntryTxt.setEnabled(false);
                probabilityOfXInsideRangeRightEntryTxt.setEnabled(false);
                probabilityOfXOutsideRangeRightEntryTxt.setEnabled(false);

                probabilityOfXAboveRangeEntryTxt.forceFocus();
                probabilityRange = ProbabilityRanges.ABOVE;

            }
        });

        // select the BELOW A THRESHOLD and deselect the others.
        chooserRangeRdo_4.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {

                chooserRangeRdo_1.setSelection(false);
                chooserRangeRdo_2.setSelection(false);
                chooserRangeRdo_3.setSelection(false);
                chooserRangeRdo_4.setSelection(true);

                clearProbabilityFields();

                probabilityOfXInsideRangeLeftEntryTxt.setEnabled(false);
                probabilityOfXOutsideRangeLeftEntryTxt.setEnabled(false);
                probabilityOfXAboveRangeEntryTxt.setEnabled(false);
                probabilityOfXBelowRangeEntryTxt.setEnabled(true);
                probabilityOfXInsideRangeRightEntryTxt.setEnabled(false);
                probabilityOfXOutsideRangeRightEntryTxt.setEnabled(false);

                probabilityOfXBelowRangeEntryTxt.forceFocus();
                probabilityRange = ProbabilityRanges.BELOW;
            }
        });

    }

    private void addThresholdValueModifyBehavior() {

        probabilityOfXInsideRangeLeftEntryTxt
                .addModifyListener(new ModifyListener() {

                    @Override
                    public void modifyText(ModifyEvent e) {

                        Text text = (Text) e.widget;
                        setProbabilityLowerValue = handleTextEntry(text
                                .getText());
                    }

                });

        probabilityOfXOutsideRangeLeftEntryTxt
                .addModifyListener(new ModifyListener() {

                    @Override
                    public void modifyText(ModifyEvent e) {

                        Text text = (Text) e.widget;
                        setProbabilityLowerValue = handleTextEntry(text
                                .getText());
                    }
                });

        probabilityOfXAboveRangeEntryTxt
                .addModifyListener(new ModifyListener() {

                    @Override
                    public void modifyText(ModifyEvent e) {

                        Text text = (Text) e.widget;
                        setProbabilityLowerValue = handleTextEntry(text
                                .getText());
                    }
                });

        probabilityOfXBelowRangeEntryTxt
                .addModifyListener(new ModifyListener() {

                    @Override
                    public void modifyText(ModifyEvent e) {

                        Text text = (Text) e.widget;
                        setProbabilityLowerValue = handleTextEntry(text
                                .getText());
                    }
                });

        probabilityOfXInsideRangeRightEntryTxt
                .addModifyListener(new ModifyListener() {

                    @Override
                    public void modifyText(ModifyEvent e) {

                        Text text = (Text) e.widget;
                        setProbabilityUpperValue = handleTextEntry(text
                                .getText());
                    }
                });

        probabilityOfXOutsideRangeRightEntryTxt
                .addModifyListener(new ModifyListener() {

                    @Override
                    public void modifyText(ModifyEvent e) {

                        Text text = (Text) e.widget;
                        setProbabilityUpperValue = handleTextEntry(text
                                .getText());
                    }
                });

    }

    protected double handleTextEntry(String entry) {

        if ((entry == null) || (entry.length() == 0)) {
            return 0;
        }
        double value = 0;
        try {
            value = Double.parseDouble(entry);
        } catch (NumberFormatException nfe) {
            MessageDialog.openError(getParentShell().getShell(),
                    "Invalid Number", "Entry must be a valid number.");
            return 0;
        }
        return value;
    }

    // enable the ERF widgets default state
    protected void enableDefaultERFTabWidgetState() {

        clearProbabilityFields();
        ensembleProductNameLbl.setText(ensembleProductName);

        chooserRangeRdo_1.setEnabled(true);
        chooserRangeRdo_2.setEnabled(true);
        chooserRangeRdo_3.setEnabled(true);
        chooserRangeRdo_4.setEnabled(true);

        probabilityOfXInsideRangeLeftEntryTxt.setEnabled(true);
        probabilityOfXOutsideRangeLeftEntryTxt.setEnabled(false);
        probabilityOfXAboveRangeEntryTxt.setEnabled(false);
        probabilityOfXBelowRangeEntryTxt.setEnabled(false);
        probabilityOfXInsideRangeRightEntryTxt.setEnabled(true);
        probabilityOfXOutsideRangeRightEntryTxt.setEnabled(false);

        chooserRangeRdo_1.setSelection(true);
        chooserRangeRdo_2.setSelection(false);
        chooserRangeRdo_3.setSelection(false);
        chooserRangeRdo_4.setSelection(false);

        probabilityOfXInsideRangeLeftEntryTxt.insert("");
        probabilityOfXInsideRangeLeftEntryTxt.forceFocus();

    }

    // clear all user-entered probability field ranges.
    private void clearProbabilityFields() {
        probabilityOfXInsideRangeLeftEntryTxt.setText("");
        probabilityOfXOutsideRangeLeftEntryTxt.setText("");
        probabilityOfXAboveRangeEntryTxt.setText("");
        probabilityOfXBelowRangeEntryTxt.setText("");
        probabilityOfXInsideRangeRightEntryTxt.setText("");
        probabilityOfXOutsideRangeRightEntryTxt.setText("");
    }

    // extract and validate the values from the chosen ERF range

    protected void computeERF() {

        Range range = null;
        switch (probabilityRange) {
        case ABOVE:
            range = new Range(RangeType.ABOVE_THRESHOLD);
            range.setThreshold(setProbabilityLowerValue);
            EnsembleTool.getInstance().calculate(
                    Calculation.ENSEMBLE_RELATIVE_FREQUENCY, range);
            break;
        case BELOW:
            range = new Range(RangeType.BELOW_THRESHOLD);
            range.setThreshold(setProbabilityLowerValue);
            EnsembleTool.getInstance().calculate(
                    Calculation.ENSEMBLE_RELATIVE_FREQUENCY, range);
            break;
        case INNER_RANGE:
            range = new Range(RangeType.INNER_RANGE);
            range.setRange(setProbabilityLowerValue, setProbabilityUpperValue);

            EnsembleTool.getInstance().calculate(
                    Calculation.ENSEMBLE_RELATIVE_FREQUENCY, range);
            break;
        case OUTER_RANGE:
            range = new Range(RangeType.OUTER_RANGE);
            range.setRange(setProbabilityLowerValue, setProbabilityUpperValue);
            EnsembleTool.getInstance().calculate(
                    Calculation.ENSEMBLE_RELATIVE_FREQUENCY, range);
            break;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.dialogs.Dialog#getInitialSize()
     */
    @Override
    protected Point getInitialSize() {
        int width = 310;
        int height = 362;
        return new Point(width, height);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.dialogs.Dialog#isResizable()
     */
    @Override
    protected boolean isResizable() {
        return true;
    }

    /**
     * Create contents of the button bar.
     * 
     * @param parent
     */

    @Override
    protected void createButtonsForButtonBar(Composite parent) {

        Button computeButton = createButton(parent, IDialogConstants.OK_ID,
                "Compute ERF", true);

        computeButton.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                computeERF();
            }

        });

        createButton(parent, IDialogConstants.CANCEL_ID,
                IDialogConstants.CANCEL_LABEL, false);
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText("ERF Product Constraints");
    }

    @Override
    protected Control createContents(Composite parent) {
        Control contents = super.createContents(parent);
        enableDefaultERFTabWidgetState();
        return contents;
    }

}
