package jp.co.onehr.workflow.contract.plugin;

import jp.co.onehr.workflow.dto.base.SimpleData;

/**
 * Business param to save business value, like applicant information or other information <br/>
 * <p>
 * they need to extend this class.
 */
public abstract class BusinessParam extends SimpleData {
    public Class clazz = this.getClass();
}
