package jp.co.onehr.workflow.base;

import java.util.Locale;

import com.github.javafaker.Faker;
import jp.co.onehr.workflow.ProcessConfiguration;
import jp.co.onehr.workflow.ProcessDesign;
import jp.co.onehr.workflow.ProcessEngine;
import jp.co.onehr.workflow.base.faker.SafeFaker;
import jp.co.onehr.workflow.contract.context.TestContextParamService;
import jp.co.onehr.workflow.contract.log.TestOperateLogService;
import jp.co.onehr.workflow.contract.notification.TestNotificationSender;
import jp.co.onehr.workflow.contract.operator.TestOperatorService;
import jp.co.onehr.workflow.contract.plugin.TestPlugin;
import jp.co.onehr.workflow.contract.restriction.TestActionRestriction;
import jp.co.onehr.workflow.contract.restriction.TestAdminActionRestriction;
import jp.co.onehr.workflow.contract.validation.TestValidation;
import jp.co.onehr.workflow.dao.ContainerUtil;
import jp.co.onehr.workflow.util.TestOrder;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(TestOrder.class)
public class BaseTest implements TestIdGeneratable {

    public static Logger staticLogger = LoggerFactory.getLogger(BaseTest.class);

    protected Logger log = LoggerFactory.getLogger(this.getClass());

    public static final String host = "localhost";

    protected final Faker faker = new SafeFaker(Locale.JAPANESE);

    protected static ProcessEngine processEngine;
    protected static ProcessDesign processDesign;

    static {
        try {
            // Initialization of test data for unit testing
            assertThat(ContainerUtil.judgeAndRecreated).isTrue();
            insertTestData();
        } catch (Exception e) {
            BaseTest.staticLogger.error("test init failed", e);
            // Terminating the unit test due to initialization failure
            System.exit(1);
        }
    }

    private static void insertTestData() throws Exception {
        var configuration = ProcessConfiguration.getConfiguration();
        configuration.setPartitionSuffix("Test");
        configuration.registerPlugin(new TestPlugin());
        configuration.registerOperatorService(TestOperatorService.singleton);
        configuration.registerNotificationSender(TestNotificationSender.singleton);
        configuration.registerActionRestriction(TestActionRestriction.singleton);
        configuration.registerAdminActionRestriction(TestAdminActionRestriction.singleton);
        configuration.registerOperatorLogService(TestOperateLogService.singleton);
        configuration.registerContextParamService(TestContextParamService.singleton);
        configuration.registerValidationsService(TestValidation.singleton);
        processDesign = configuration.buildProcessDesign();
        processEngine = configuration.buildProcessEngine();
    }
}
