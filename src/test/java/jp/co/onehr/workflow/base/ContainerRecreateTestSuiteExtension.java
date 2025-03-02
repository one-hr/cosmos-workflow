package jp.co.onehr.workflow.base;

import jp.co.onehr.workflow.dao.ContainerUtil;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Junit5 extension class. This class is used to execute the code that must be executed before all unit tests are executed.
 *
 * <p>
 * Here, the decision and execution of whether to recreate the test container for the first time are performed.
 * </p>
 *
 * @see <a href="https://stackoverflow.com/questions/43282798/in-junit-5-how-to-run-code-before-all-tests">before all tests</a>
 */
public class ContainerRecreateTestSuiteExtension implements BeforeAllCallback {

    static final Logger log = LoggerFactory.getLogger(ContainerRecreateTestSuiteExtension.class);

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {
        log.info("beforeAll from ContainerRecreateTestSuiteExtension");
        assertThat(ContainerUtil.judgeAndRecreated).isTrue();
    }
}
