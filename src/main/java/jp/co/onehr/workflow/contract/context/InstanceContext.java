package jp.co.onehr.workflow.contract.context;

/**
 * Business param to save business value, like applicant information or other information <br/>
 * <p>
 * they need to extend this class.
 */
public interface InstanceContext {

    Class getClazz();

}
