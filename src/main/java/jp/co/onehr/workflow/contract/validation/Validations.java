package jp.co.onehr.workflow.contract.validation;

import jp.co.onehr.workflow.dto.Definition;

/**
 * Custom validations for the workflow
 */
public interface Validations {

    /**
     * Validation of the Definition
     *
     * @throws Exception
     */
    void definitionValidation(Definition definition) throws Exception;
}
