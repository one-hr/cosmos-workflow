package jp.co.onehr.workflow.constant;


public enum DatabaseErrors implements ErrorInterface {

    ID_CONFLICT(409), //

    UNIQUE_KEY_CONFLICT(409),
    /**
     * json from db process error, can't be deserialized
     */
    JSON_FROM_DB_PROCESS_ERROR(500); //

    private final int httpStatus;

    DatabaseErrors(int httpStatus) {
        this.httpStatus = httpStatus;
    }

    public int httpStatus() {
        return httpStatus;
    }
}
