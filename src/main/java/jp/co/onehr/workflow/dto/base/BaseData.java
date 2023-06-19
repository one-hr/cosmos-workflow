package jp.co.onehr.workflow.dto.base;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;


public class BaseData extends SimpleData implements Identifiable {

    public String id = "";

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
