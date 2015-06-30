package gov.noaa.gsd.viz.ensemble.navigator.ui.viewer;

import gov.noaa.gsd.viz.ensemble.util.ImageResourceManager;

import org.eclipse.swt.graphics.Image;

/**
 * 
 * This is the image store cache for the EnsembleToolViewer.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jul 17, 2015            polster     Initial creation
 * 
 * </pre>
 * 
 * @author polster
 * @version 1.0
 */

public class EnsembleToolViewerImageStore {

    static protected Image GEAR_IMG = null;

    static protected Image ALPHANUMERIC_SORT_IMG = null;

    static protected Image VOLUME_BROWSER_IMG = null;

    static protected Image POWER_ON_IMG = null;

    static protected Image POWER_OFF_IMG = null;

    static protected Image DIAGNOSTICS_IMG = null;

    static protected Image TAB_LEGENDS_ENABLED_SELECTED_IMG = null;

    static protected Image TAB_LEGENDS_ENABLED_UNSELECTED_IMG = null;

    static protected Image TAB_LEGENDS_DISABLED_IMG = null;

    static protected Image TAB_MATRIX_ENABLED_SELECTED_IMG = null;

    static protected Image TAB_MATRIX_ENABLED_UNSELECTED_IMG = null;

    static protected Image TAB_MATRIX_DISABLED_IMG = null;

    static protected Image TAB_INFO_ENABLED_IMG = null;

    static protected Image TAB_INFO_DISABLED_IMG = null;

    static protected Image TAB_OPTIONS_ENABLED_IMG = null;

    static protected Image TAB_OPTIONS_DISABLED_IMG = null;

    static protected void constructImages() {

        if (DIAGNOSTICS_IMG == null) {
            DIAGNOSTICS_IMG = ImageResourceManager.getPluginImage(
                    "gov.noaa.gsd.viz.ensemble", "icons/diagnostic.gif");
        }

        if (ALPHANUMERIC_SORT_IMG == null) {
            ALPHANUMERIC_SORT_IMG = ImageResourceManager.getPluginImage(
                    "gov.noaa.gsd.viz.ensemble", "icons/a-to-z-sort.gif");
        }
        if (GEAR_IMG == null) {
            GEAR_IMG = ImageResourceManager.getPluginImage(
                    "gov.noaa.gsd.viz.ensemble",
                    "icons/calculation-gear-29x24.gif");
        }
        if (VOLUME_BROWSER_IMG == null) {
            VOLUME_BROWSER_IMG = ImageResourceManager.getPluginImage(
                    "gov.noaa.gsd.viz.ensemble",
                    "icons/volume-browser-29x24.gif");
        }

        if (TAB_INFO_ENABLED_IMG == null) {
            TAB_INFO_ENABLED_IMG = ImageResourceManager.getPluginImage(
                    "gov.noaa.gsd.viz.ensemble", "icons/tab-info-enabled.gif");
        }
        if (POWER_ON_IMG == null) {
            POWER_ON_IMG = ImageResourceManager.getPluginImage(
                    "gov.noaa.gsd.viz.ensemble", "icons/power-on-29x24.gif");
        }

        if (POWER_OFF_IMG == null) {
            POWER_OFF_IMG = ImageResourceManager.getPluginImage(
                    "gov.noaa.gsd.viz.ensemble", "icons/power-off-29x24.gif");
        }

        if (TAB_LEGENDS_ENABLED_UNSELECTED_IMG == null) {
            TAB_LEGENDS_ENABLED_UNSELECTED_IMG = ImageResourceManager
                    .getPluginImage("gov.noaa.gsd.viz.ensemble",
                            "icons/tab-legends-enabled-unselected.gif");
        }

        if (TAB_LEGENDS_ENABLED_SELECTED_IMG == null) {
            TAB_LEGENDS_ENABLED_SELECTED_IMG = ImageResourceManager
                    .getPluginImage("gov.noaa.gsd.viz.ensemble",
                            "icons/tab-legends-enabled-selected.gif");
        }

        if (TAB_LEGENDS_DISABLED_IMG == null) {
            TAB_LEGENDS_DISABLED_IMG = ImageResourceManager.getPluginImage(
                    "gov.noaa.gsd.viz.ensemble",
                    "icons/tab-legends-disabled.gif");
        }

        if (TAB_MATRIX_ENABLED_UNSELECTED_IMG == null) {
            TAB_MATRIX_ENABLED_UNSELECTED_IMG = ImageResourceManager
                    .getPluginImage("gov.noaa.gsd.viz.ensemble",
                            "icons/tab-matrix-enabled-unselected.gif");
        }

        if (TAB_MATRIX_ENABLED_SELECTED_IMG == null) {
            TAB_MATRIX_ENABLED_SELECTED_IMG = ImageResourceManager
                    .getPluginImage("gov.noaa.gsd.viz.ensemble",
                            "icons/tab-matrix-enabled-selected.gif");
        }

        if (TAB_MATRIX_DISABLED_IMG == null) {
            TAB_MATRIX_DISABLED_IMG = ImageResourceManager.getPluginImage(
                    "gov.noaa.gsd.viz.ensemble",
                    "icons/tab-matrix-disabled.gif");
        }

        if (TAB_INFO_ENABLED_IMG == null) {
            TAB_INFO_ENABLED_IMG = ImageResourceManager.getPluginImage(
                    "gov.noaa.gsd.viz.ensemble", "icons/tab-info-enabled.gif");
        }

        if (TAB_INFO_DISABLED_IMG == null) {
            TAB_INFO_DISABLED_IMG = ImageResourceManager.getPluginImage(
                    "gov.noaa.gsd.viz.ensemble", "icons/tab-info-disabled.gif");
        }

        if (TAB_OPTIONS_ENABLED_IMG == null) {
            TAB_OPTIONS_ENABLED_IMG = ImageResourceManager.getPluginImage(
                    "gov.noaa.gsd.viz.ensemble", "icons/tab-prefs-enabled.gif");
        }

        if (TAB_OPTIONS_DISABLED_IMG == null) {
            TAB_OPTIONS_DISABLED_IMG = ImageResourceManager
                    .getPluginImage("gov.noaa.gsd.viz.ensemble",
                            "icons/tab-prefs-disabled.gif");
        }
    }

}
