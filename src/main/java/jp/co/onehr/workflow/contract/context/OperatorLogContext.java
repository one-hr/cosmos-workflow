package jp.co.onehr.workflow.contract.context;

/**
 * Recording operator's context in operation history.
 * <p>
 * they need to extend this class.
 */
public interface OperatorLogContext {

    Class getClazz();

}
