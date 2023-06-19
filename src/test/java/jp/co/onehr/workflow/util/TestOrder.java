package jp.co.onehr.workflow.util;

import com.google.common.collect.Lists;
import io.github.thunderz99.cosmos.util.JsonUtil;
import org.junit.jupiter.api.MethodDescriptor;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.MethodOrdererContext;
import org.junit.jupiter.api.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static java.util.Comparator.comparingInt;


public class TestOrder extends MethodOrderer.OrderAnnotation {

    private static int index = 1;

    protected Logger log = LoggerFactory.getLogger(this.getClass());

    @Override
    public void orderMethods(MethodOrdererContext context) {
        context.getMethodDescriptors().sort(comparingInt(this::getOrder));
        List<String> methodNames = Lists.newLinkedList();
        for (var method : context.getMethodDescriptors()) {
            methodNames.add(method.getMethod().getName());
        }
        log.info("package:{}, class: {}[{}] , test method sort: {}", context.getTestClass().getPackageName(), context.getTestClass().getSimpleName(), index, JsonUtil.toJson(methodNames));
        index++;
    }

    private int getOrder(MethodDescriptor descriptor) {
        return descriptor.findAnnotation(Order.class).map(Order::value).orElse(-1);
    }
}
