package jp.co.onehr.workflow.contract.context;

import java.util.Map;

public class TestOperatorLogContext implements OperatorLogContext {

    public Map<String, String> operator;

    public Class clazz = this.getClass();

    @Override
    public Class getClazz() {
        return this.clazz;
    }
}
