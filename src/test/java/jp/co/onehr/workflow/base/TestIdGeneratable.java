package jp.co.onehr.workflow.base;

import java.util.UUID;

public interface TestIdGeneratable extends FakerCapable {


    default public String getUuid() {
        return UUID.randomUUID().toString();
    }

    default public String getTestId() {
        return String.join("_", this.getClass().getSimpleName(), Thread.currentThread().getStackTrace()[2].getMethodName(), getFaker().idNumber().valid());
    }

    default public String getTestId(String name) {
        return String.join("_", this.getClass().getSimpleName(), Thread.currentThread().getStackTrace()[2].getMethodName(), name, getFaker().idNumber().valid());
    }
    
}
