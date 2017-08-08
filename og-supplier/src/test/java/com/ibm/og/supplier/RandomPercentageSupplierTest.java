/* Copyright (c) IBM Corporation 2017. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.supplier;

import com.google.common.base.Supplier;
import com.google.common.collect.Maps;
import org.junit.Test;

import java.util.Map;
import java.util.Random;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

public class RandomPercentageSupplierTest {

    @Test(expected = IllegalArgumentException.class)
    public void moreThanHundredPercent() {
        new RandomPercentageSupplier.Builder<Integer>().withChoice(1, 50.0).withChoice(2, 50.0).
                withChoice(3, 30.0).build();
    }

    @Test
    public void oneChoice() {
        final Supplier<Integer> s = new RandomPercentageSupplier.Builder<Integer>().withChoice(1, 100.00).build();
        int count = 0;
        for (int i = 0; i < 100; i++) {
            Integer val = s.get();
            if (val != null && val == 1) {
                count++;
            }
        }
        assertThat(count, comparesEqualTo(100));
    }

    @Test
    public void multipleChoices() {
        final Supplier<Integer> s =
                new RandomPercentageSupplier.Builder<Integer>().withChoice(1, 25.0).withChoice(2, 25.0)
                        .withChoice(3, 25.0).withRandom(new Random()).build();

        final Map<Integer, Integer> counts = Maps.newHashMap();
        counts.put(1, 0);
        counts.put(2, 0);
        counts.put(3, 0);
        //counts.put(4, 0);

        for (int i = 0; i < 100; i++) {
            final Integer value = s.get();
            if (value != null) {
                counts.put(value, counts.get(value) + 1);
            }
        }

        for (final int count : counts.values()) {
            assertThat(count, greaterThan(0));
        }
    }
}
