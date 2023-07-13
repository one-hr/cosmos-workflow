package jp.co.onehr.workflow.contract.plugin;


import java.util.HashMap;
import java.util.Map;

public class TestPluginResult extends PluginResult {
    public Integer resultNum = 0;

    public String resultStr = "";

    public Map<String, String> resultMap = new HashMap<>();

    public String nodeType = "";

    public TestPluginResult() {
    }
}
