package jp.co.onehr.workflow.dto.base;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;


public class BaseData extends SimpleData implements Identifiable {

    public String id = "";

    public String createdAt = "";

    public String updatedAt = "";

    @Override
    @JsonGetter
    public String getId() {
        return this.id;
    }

    @Override
    @JsonSetter
    public void setId(String id) {
        this.id = id;
    }
}
