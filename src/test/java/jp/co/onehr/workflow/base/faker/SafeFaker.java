package jp.co.onehr.workflow.base.faker;

import com.github.javafaker.Faker;
import com.github.javafaker.Name;
import com.github.javafaker.service.FakeValuesService;
import com.github.javafaker.service.RandomService;

import java.util.Locale;
import java.util.Random;

public class SafeFaker extends Faker {
    private final Name name;

    public SafeFaker() {
        this(Locale.ENGLISH);
    }

    public SafeFaker(Locale locale) {
        this(locale, (Random) null);
    }

    public SafeFaker(Random random) {
        this(Locale.ENGLISH, random);
    }

    public SafeFaker(Locale locale, Random random) {
        this(locale, new RandomService(random));
    }

    public SafeFaker(Locale locale, RandomService randomService) {
        this(new FakeValuesService(locale, randomService), randomService);
    }

    public SafeFaker(FakeValuesService fakeValuesService, RandomService random) {
        super(fakeValuesService, random);
        this.name = new SafeName(this);
    }

    public static String getCallerClassName() {
        var stackTraces = Thread.currentThread().getStackTrace();
        if (stackTraces.length < 3) {
            return "simpleThreadName";
        }
        return stackTraces[3].getClassName();
    }

    @Override
    public Name name() {
        return this.name;
    }
}