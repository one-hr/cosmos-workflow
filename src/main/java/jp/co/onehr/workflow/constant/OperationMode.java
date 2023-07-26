package jp.co.onehr.workflow.constant;

/**
 * operation mode
 */
public enum OperationMode {

    /**
     * operator
     */
    OPERATOR_MODE,

    /**
     * admin
     */
    ADMIN_MODE;


    public boolean isAdminMode() {
        return ADMIN_MODE.equals(this);
    }
}
