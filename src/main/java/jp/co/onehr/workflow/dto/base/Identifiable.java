package jp.co.onehr.workflow.dto.base;

/**
 * Whether an ID exists
 */
public interface Identifiable {

    /**
     * Returning the ID
     *
     * <p>
     * All data to be stored in CosmosDB requires an ID
     * </p>
     *
     * @return
     */
    public String getId();

    public void setId(String id);

}