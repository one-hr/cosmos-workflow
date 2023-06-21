package jp.co.onehr.workflow.dto.node;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonGetter;
import jp.co.onehr.workflow.dto.base.SimpleData;

/**
 * The basic definition of a workflow node
 * all type nodes need to extend this class.
 */
public abstract class Node extends SimpleData {

    public String nodeId = UUID.randomUUID().toString();

    public String nodeName = "";

    @JsonGetter
    public String getType() {
        return this.getClass().getSimpleName();
    }

}
