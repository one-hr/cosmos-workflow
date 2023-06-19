package jp.co.onehr.workflow.exception;

import jp.co.onehr.workflow.constant.ErrorInterface;


public class WorkflowException extends RuntimeException{

    static final long serialVersionUID = 1L;

    private final ErrorInterface error;

    private Object errorValue;

    public WorkflowException(ErrorInterface error) {
        super(error.toString());
        this.error = error;
    }

    public WorkflowException(ErrorInterface error, Object errorValue) {
        super(error.name()+ ". errorValue: " + errorValue);
        this.error = error;
        this.errorValue = errorValue;
    }


    public WorkflowException(ErrorInterface error, String message, Object errorValue) {
        super(error.name() + ". " + message + ". errorValue: " + errorValue);
        this.error = error;
        this.errorValue = errorValue;
    }

    public WorkflowException(ErrorInterface error, String messageFormat, Object... args) {
        super(error.name() + ". " + String.format(messageFormat, args));
        this.error = error;
    }

    public WorkflowException(ErrorInterface error, Throwable cause, String message) {
        super(error.name() + ". " + message, cause);
        this.error = error;
    }

    public WorkflowException(ErrorInterface error, Throwable cause, String messageFormat, Object... args) {
        super(error.name() + ". " + String.format(messageFormat, args), cause);
        this.error = error;
    }

    public ErrorInterface getError() {
        return error;
    }

    public Object getErrorValue() {
        return errorValue;
    }
}
