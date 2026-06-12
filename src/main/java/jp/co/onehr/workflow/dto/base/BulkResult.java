package jp.co.onehr.workflow.dto.base;

import java.util.ArrayList;
import java.util.List;

import io.github.thunderz99.cosmos.CosmosException;

/**
 * Results from a non-transactional bulk operation.
 *
 * <p>A bulk request may succeed for some items and fail for others. Check all
 * three lists before deciding whether the operation completed successfully.</p>
 *
 * @param <T> the document type returned for successful and retryable items
 */
public class BulkResult<T> {

    /**
     * Documents that were updated successfully.
     */
    public List<T> successList = new ArrayList<>();

    /**
     * Documents whose operations exhausted the SDK's automatic retries.
     *
     * <p>The caller may submit these items again when retrying is safe for the
     * patch being applied.</p>
     */
    public List<T> retryList = new ArrayList<>();

    /**
     * Errors that the SDK classified as non-retryable, such as a conflict or an
     * invalid request.
     */
    public List<CosmosException> fatalList = new ArrayList<>();
}
