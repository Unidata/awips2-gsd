package gov.noaa.gsd.viz.ensemble.util.diagnostic;

/**
 * Used as the message payload for the Ensemble Tool diagnostic trace dialog.
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

public class EnsembleToolDiagnosticTraceMessage {

    private String count = null;

    private String action = null;

    private String toolLayerRef = null;

    private String isEditable = null;

    private String editorRef = null;

    private String activeEditorRef = null;

    private String methodName = null;

    private static final String EMPTY = "***";

    private StackTraceElement[] stackTrace = null;

    public EnsembleToolDiagnosticTraceMessage(String count, String action,
            String toolLayer, boolean isEditable, String editor,
            String activeEditor, String method) {
        super();
        this.count = count;
        this.action = action;
        this.isEditable = Boolean.toString(isEditable);
        this.toolLayerRef = toolLayer;
        this.editorRef = editor;
        this.activeEditorRef = activeEditor;
        this.methodName = method;
        stackTrace = new Exception().getStackTrace();
    }

    public EnsembleToolDiagnosticTraceMessage(String count, String action) {
        super();
        this.count = count;
        this.action = action;
        this.isEditable = EMPTY;
        this.toolLayerRef = EMPTY;
        this.editorRef = EMPTY;
        this.activeEditorRef = EMPTY;
        this.methodName = EMPTY;
        stackTrace = new Exception().getStackTrace();

    }

    public String getCount() {
        return count;
    }

    public String getAction() {
        return action;
    }

    public String getToolLayerRef() {
        return toolLayerRef;
    }

    public String getIsEditable() {
        return isEditable;
    }

    public String getEditorRef() {
        return editorRef;
    }

    public String getActiveEditorRef() {
        return activeEditorRef;
    }

    public String getMethodName() {
        return methodName;
    }

    public StackTraceElement[] getStackTrace() {
        return stackTrace;
    }

}
