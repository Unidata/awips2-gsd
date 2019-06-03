package gov.noaa.gsd.viz.ensemble.display.control.contour;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;

import com.raytheon.uf.common.style.AbstractStylePreferences;
import com.raytheon.uf.common.style.ContourLabelingPreferences;
import com.raytheon.uf.common.style.IncrementLabelingPreferences;
import com.raytheon.uf.common.style.StyleException;
import com.raytheon.uf.common.style.ValuesLabelingPreferences;
import com.raytheon.uf.common.style.contour.ContourPreferences;
import com.raytheon.uf.viz.core.grid.rsc.AbstractGridResource;
import com.raytheon.uf.viz.core.rsc.DisplayType;
import com.raytheon.uf.viz.core.rsc.capabilities.DensityCapability;

/**
 * 
 * Supports interactive contour control display by changing the label increment
 * and a given value.
 * 
 * The implementation is to access the AbstractStylePreferences object of a grid
 * resource. A methods getStylePreferences() should be added in the
 * AbstractGridResource. The code is,
 * 
 * public AbstractStylePreferences getStylePreferences() { return
 * stylePreferences; }
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Feb 28, 2017   19598         jing     Initial creation
 * Jun 27, 2017   19325         jing     Upgrade to 17.3.1
 * Jun 03, 2019   64512         ksunil   changes to absorb new labelingPreferences
 * </pre>
 *
 * @author jing
 */

public class ContourControl {

    /*
     * Save the original contour values
     */
    private float[] valuesOrig;

    /*
     * Save the original contour increment
     */
    private float[] incrementOrig;

    /*
     * The labeling preferences of a grid resource
     */
    private ContourLabelingPreferences labelingPreferences = null;

    /*
     * The labeling preferences of a grid resource
     */
    private ContourPreferences contourPreferences = null;

    /*
     * The grid resource be controlled
     */
    private AbstractGridResource<?> rsc;

    /*
     * A flag with value 1 or -1, is used to change the density just a little to
     * trick contour redrawing. It can avoid add redrawing interface in the
     * baseline code.
     */
    private double densityFlag = 1;

    public ContourControl(AbstractGridResource<?> rsc) {
        this.rsc = rsc;
        labelingPreferences = initCountourLabeling(rsc);
    }

    /**
     * Initializes and returns the LabelingPreferences object of a grid
     * resource.
     * 
     * @param rsc
     *            A grid resource
     * @return The labelingPreferences object of the grid resource
     * @throws StyleException
     */
    private ContourLabelingPreferences initCountourLabeling(
            AbstractGridResource<?> rsc) {

        ContourLabelingPreferences labelingPreferences = null;
        AbstractStylePreferences stylePreferences = rsc.getStylePreferences();
        if (stylePreferences instanceof ContourPreferences) {
            contourPreferences = (ContourPreferences) stylePreferences;
            labelingPreferences = contourPreferences.getContourLabeling();
            for (ValuesLabelingPreferences labelPrefs : labelingPreferences
                    .getValues()) {
                valuesOrig = ArrayUtils.addAll(valuesOrig,
                        labelPrefs.getValues());
            }
            for (IncrementLabelingPreferences labelPrefs : labelingPreferences
                    .getIncrement()) {
                incrementOrig = ArrayUtils.addAll(valuesOrig,
                        labelPrefs.getValues());
            }

        }

        return labelingPreferences;
    }

    /**
     * Get the default value if the configured contour values are existing.
     * 
     * @return The contour value or Float.NaN
     */
    public float getDefaultValue() {

        if (labelingPreferences == null || valuesOrig == null
                || valuesOrig.length == 0) {
            return Float.NaN;
        }

        return valuesOrig[valuesOrig.length / 2];

    }

    /**
     * Change contour with user specified increment and value. The contours will
     * be as default if the value is "NaN", that only use the increment.
     * 
     * @param increment
     *            The contour increment
     * @param contourValue
     *            The contour value
     */
    public void changeContourValues(float increment, float contourValue) {

        /*
         * Converts the contourValue into a float array
         */
        float[] values = null;
        if (!Float.isNaN(contourValue)) {
            values = new float[1];
            values[0] = contourValue;
        }

        /*
         * Sets contour label increment and values.
         */
        IncrementLabelingPreferences incr = new IncrementLabelingPreferences();
        incr.setValues(new float[] { increment });
        labelingPreferences.setIncrement(Arrays.asList(incr));

        ValuesLabelingPreferences val = new ValuesLabelingPreferences();
        incr.setValues(new float[] { contourValue });
        labelingPreferences.setValues(Arrays.asList(val));

        rsc.setStylePreferences(contourPreferences);

        /*
         * Force redrawing the contour.
         */
        redrawContours(rsc);
    }

    /**
     * Change contour with user specified increment and value for a group grid
     * resources start from second resource.
     * 
     * @param rscList
     *            A group grid resources
     * @param increment
     *            The contour increment
     * @param value
     *            The contour value
     */
    public void changeContourGroup(List<AbstractGridResource<?>> rscList,
            float increment, float value) {

        if (rscList.isEmpty() || rscList.size() < 2) {
            return;
        }
        float[] values = null;
        if (!Float.isNaN(value)) {
            values = new float[1];
            values[0] = value;
        }

        for (int i = 1; i < rscList.size(); i++) {

            AbstractStylePreferences stylePreferences = rscList.get(i)
                    .getStylePreferences();
            if (stylePreferences instanceof ContourPreferences) {

                IncrementLabelingPreferences incr = new IncrementLabelingPreferences();
                incr.setValues(new float[] { increment });
                ((ContourPreferences) stylePreferences).getContourLabeling()
                        .setIncrement(Arrays.asList(incr));

                ValuesLabelingPreferences val = new ValuesLabelingPreferences();
                incr.setValues(new float[] { value });
                ((ContourPreferences) stylePreferences).getContourLabeling()
                        .setValues(Arrays.asList(val));

                rscList.get(i).setStylePreferences(contourPreferences);

                redrawContours(rscList.get(i));

            }
        }
    }

    /**
     * Reset the contour to original.
     */
    public void reset() {
        IncrementLabelingPreferences incr = new IncrementLabelingPreferences();
        incr.setValues(incrementOrig);
        labelingPreferences.setIncrement(Arrays.asList(incr));

        ValuesLabelingPreferences val = new ValuesLabelingPreferences();
        incr.setValues(valuesOrig);
        labelingPreferences.setValues(Arrays.asList(val));

        redrawContours(rsc);

    }

    /**
     * Force redrawing the contour of a grid resource. TODO:trick to force
     * redrawing by change density a little. Replace this code when there is any
     * other way.
     * 
     * @param gRsc
     *            The grid resource
     */
    private void redrawContours(AbstractGridResource<?> gRsc) {
        if (((AbstractGridResource<?>) rsc)
                .getDisplayType() != DisplayType.CONTOUR) {
            return;
        }
        /*
         * Ping-pong change the current density 0.01. if this time plus 0.01,
         * next time will minus 0.01.
         */
        double density = gRsc.getCapability(DensityCapability.class)
                .getDensity();
        gRsc.getCapability(DensityCapability.class)
                .setDensity(density + densityFlag * 0.01);
        densityFlag = -1 * densityFlag;

        gRsc.issueRefresh();
    }

    public float[] getValuesOrig() {
        return valuesOrig;
    }

    public float getIncrementOrig() {
        if (incrementOrig == null || incrementOrig.length == 0) {
            return 0;
        } else {
            return incrementOrig[0];
        }
    }

}
