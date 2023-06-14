package jp.co.onehr.workflow.dto.base;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Set;

/**
 * The interface for supporting unique fields other than ID
 * <p>
 * Considering the performance of DB read/write operations, limiting it to three for now.
 * Additional ones can be added if needed in the future.
 */
public interface UniqueKeyCapable extends Identifiable {

    String UNIQUE_KEY_1 = "_uniqueKey1";
    String UNIQUE_KEY_2 = "_uniqueKey2";
    String UNIQUE_KEY_3 = "_uniqueKey3";
    
    /**
     * The following three keys are set as unique indexes in CosmosDB, ensuring uniqueness.
     */
    public static final Set<String> uniqueKeys = Set.of(UNIQUE_KEY_1, UNIQUE_KEY_2, UNIQUE_KEY_3);

    public static final Set<String> uniqueKeysWithId = Set.of("id", UNIQUE_KEY_1, UNIQUE_KEY_2, UNIQUE_KEY_3);

    @JsonIgnore
    default public String getUniqueKey1() {
        return getId();
    }

    @JsonIgnore
    default public String getUniqueKey2() {
        return getId();
    }

    @JsonIgnore
    default public String getUniqueKey3() {
        return getId();
    }


}
