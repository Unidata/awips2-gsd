package gov.noaa.gsd.viz.ensemble.navigator.ui.viewer.matrix;

/***
 * 
 * This enumeration contains a list of model sources that are available to be
 * associated with certain model families.
 * 
 * TODO: Storing configuration information into enumerations is not acceptable
 * for the longer term. Since the model family raw files are stable for now, we
 * will plan to change this in the next release.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Dec 20, 2016   12371     polster     Initial creation
 * Dec 01, 2017  41520      polster     ModelSources renamed to ModelSourceKind
 * 
 * </pre>
 * 
 * @author polster
 * @version 1.0
 */
public enum ModelSourceKind {

    ARW_EAST("ARWmodel1", "ARW East", "HiResW-ARW-East", false), //
    ARW_WEST("ARWmodel2", "ARW West", "HiResW-ARW-West", false), //
    CANADIAN_NH("NO_VARIABLE_NEEDED", "Canadian-NH", "Canadian-NH", false), //
    CANADIAN_REG("NO_VARIABLE_NEEDED", "Canadian-Reg", "Canadian-Reg", false), //
    ECMWF_HIRES("NO_VARIABLE_NEEDED", "ECMWF-HiRes", "ECMWF-HiRes", true), //
    GEM_NH("GEMNHmodel", "GEM-NHem", "Canadian-NH", false), //
    GEM_REG("GEMRegmodel", "GEM-Regional", "Canadian-Reg", false), //
    GFS_GLOBAL("GFSmodel", "GFS Global", "GEFS", true), //
    HRRR("NO_VARIABLE_NEEDED", "HRRR", "HRRR", true), //
    LAPS("NO_VARIABLE_NEEDED", "LAPS", "LAPS", true), //
    NAM12("NAM12model", "NAM12", "NAM12", true), //
    NAM40("NAM40model", "NAM40", "NAM40", true), //
    NAM_Nest("NAMNestmodel", "NAMNest 4km", "CR-NAMNest", false), //
    NMM_EAST("MMMmodel1", "HiResW-NMM-East", "HiResW-NMM-East", false), //
    NMM_WEST("MMMmodel2", "HiResW-NMM-West", "HiResW-NMM-West", false), //
    RAP13("RAP13model", "RAP13", "RAP13", true), //
    RAP40("RAPmodel", "RAP40", "RAP40", true);

    private String variableName = null;

    private String modelName = null;

    private String modelId = null;

    private boolean active = false;

    ModelSourceKind(String varName, String name, String id,
            boolean visibleInMenu) {
        setVariableName(varName);
        setModelId(id);
        setModelName(name);
        setActive(visibleInMenu);
    }

    public boolean isActive() {
        return active;
    }

    private void setActive(boolean active) {
        this.active = active;
    }

    public String getModelId() {
        return modelId;
    }

    private void setModelId(String modelId) {
        this.modelId = modelId;
    }

    public String getModelName() {
        return modelName;
    }

    private void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getVariableName() {
        return variableName;
    }

    private void setVariableName(String variableName) {
        this.variableName = variableName;
    }

    public String toString() {
        return modelName;
    }

    public boolean isSameAs(ModelSourceKind operand) {
        boolean isEqual = false;
        if (getModelId().equals(operand.getModelId())
                && getModelName().equals(operand.getModelName())) {
            isEqual = true;
        }
        return isEqual;
    }
}
