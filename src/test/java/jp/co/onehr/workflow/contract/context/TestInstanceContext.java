package jp.co.onehr.workflow.contract.context;

import com.fasterxml.jackson.annotation.JsonIgnore;


public class TestInstanceContext implements InstanceContext {

    public boolean generatorOperator = true;

    public boolean resetParallelOperator = false;

    @JsonIgnore
    public Class clazz = this.getClass();

    @JsonIgnore
    @Override
    public Class getClazz() {
        return this.clazz;
    }
}
