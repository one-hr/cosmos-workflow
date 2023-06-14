package jp.co.onehr.workflow.base;

import com.github.javafaker.Faker;
import jp.co.onehr.workflow.base.faker.SafeFaker;

import java.util.Locale;

public interface FakerCapable {


    public static Faker faker = new SafeFaker(Locale.ENGLISH);

    public default Faker getFaker() {
        return faker;
    }
    
}
