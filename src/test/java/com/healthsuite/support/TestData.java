package com.healthsuite.support;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/** Unique emails / Bangladeshi phone numbers, so tests sharing one database never collide. */
public final class TestData {

    private static final AtomicInteger SEQ = new AtomicInteger();
    private static final int RUN = ThreadLocalRandom.current().nextInt(1_000, 10_000);

    private TestData() {
    }

    public static String email(String prefix) {
        return prefix + "-" + RUN + "-" + SEQ.incrementAndGet() + "@test.healthsuite";
    }

    /** Matches the @BangladeshPhone pattern: 01[3-9] followed by 8 digits. */
    public static String phone() {
        return String.format("017%04d%04d", RUN, SEQ.incrementAndGet() % 10_000);
    }
}
