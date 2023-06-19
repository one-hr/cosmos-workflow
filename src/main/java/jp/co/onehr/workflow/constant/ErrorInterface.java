package jp.co.onehr.workflow.constant;

/**
 * Define an interface for the names and codes of errors in the exception.
 * All errors need to implement this interface in order to be used in the Exception.
 */
public interface ErrorInterface {

    int httpStatus();

    String name();

}
