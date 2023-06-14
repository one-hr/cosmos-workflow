package jp.co.onehr.workflow.dto.base;

import io.github.thunderz99.cosmos.util.JsonUtil;

/**
 * base class for the Data class
 * provide convenient methods such as toString(), equals(), and more by default
 */
public abstract class SimpleData {

    public SimpleData() {
    }

    @Override
    public String toString() {
        return JsonUtil.toJson(this);
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

    @Override
    public boolean equals(Object obj2) {
        if (obj2 == null) {
            return false;
        }
        return this.toString().equals(obj2.toString());
    }

    /**
     * generate a copy using JSON
     *
     * @return
     */
    public <T> T copy() {
        return JsonUtil.fromJson(JsonUtil.toJson(this), this.getClass().getName());
    }

}