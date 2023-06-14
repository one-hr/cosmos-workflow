package jp.co.onehr.workflow.base.faker;

import com.github.javafaker.Faker;
import com.github.javafaker.Name;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class SafeName extends Name {

    // The collection with the name that was used
    private Map<String, List<String>> used = new HashMap<>();

    // The maximum number of unique names is 9900.
    private final int maxUniqueSize = 9900;

    public SafeName(Faker faker) {
        super(faker);
    }

    /**
     * The maximum number of unique names within a test case is 9900.
     * Once this limit is exceeded, it becomes impossible to maintain uniqueness.
     */
    @Override
    public String name() {
        var callerClassName = SafeFaker.getCallerClassName();
        List<String> usedData = used.computeIfAbsent(callerClassName, v -> new LinkedList<>());

        var name = super.name();
        if (usedData.size() < maxUniqueSize) {
            while (usedData.contains(name)) {
                name = super.name();
            }
        } else {
            usedData = new LinkedList<>();
            used.put(callerClassName, usedData);
        }
        usedData.add(name);

        return name;
    }
}