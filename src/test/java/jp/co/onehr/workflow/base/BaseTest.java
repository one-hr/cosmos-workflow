package jp.co.onehr.workflow.base;

import com.github.javafaker.Faker;
import jp.co.onehr.workflow.base.faker.SafeFaker;
import jp.co.onehr.workflow.dao.ContainerUtil;
import jp.co.onehr.workflow.util.TestOrder;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(TestOrder.class)
public class BaseTest implements TestIdGeneratable {

    public static Logger staticLogger = LoggerFactory.getLogger(BaseTest.class);

    protected Logger log = LoggerFactory.getLogger(this.getClass());

    public static final String host = "localhost";

    protected final Faker faker = new SafeFaker(Locale.JAPANESE);

    static {
        try {
            // Initialization of test data for unit testing
            assertThat(ContainerUtil.judgeAndRecreated).isTrue();

        } catch (Exception e) {
            BaseTest.staticLogger.error("test init failed", e);
            // Terminating the unit test due to initialization failure
            System.exit(1);
        }
    }

}
