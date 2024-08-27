package jp.co.onehr.workflow.base;

import java.util.UUID;

public interface TestIdGeneratable {


    default String getUuid() {
        return UUID.randomUUID().toString();
    }

}
