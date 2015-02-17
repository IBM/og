/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.supplier;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

import java.util.Map;
import java.util.Random;

import org.junit.Test;

import com.google.common.base.Supplier;
import com.google.common.collect.Maps;

public class RandomSupplierTest {
  @Test(expected = IllegalArgumentException.class)
  public void noChoice() {
    new RandomSupplier.Builder<Integer>().build();
  }

  @Test(expected = NullPointerException.class)
  public void nullChoice() {
    new RandomSupplier.Builder<Integer>().withChoice(null).build();
  }

  @Test(expected = NullPointerException.class)
  public void nullRandom() {
    new RandomSupplier.Builder<Integer>().withChoice(1).withRandom(null).build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void negativeWeight() {
    new RandomSupplier.Builder<Integer>().withChoice(1, -1.0).build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void zeroWeight() {
    new RandomSupplier.Builder<Integer>().withChoice(1, 0.0).build();
  }

  @Test
  public void oneChoice() {
    final Supplier<Integer> s = new RandomSupplier.Builder<Integer>().withChoice(1).build();
    for (int i = 0; i < 10; i++) {
      assertThat(s.get(), is(1));
    }
  }

  @Test
  public void multipleChoices() {
    final Supplier<Integer> s =
        new RandomSupplier.Builder<Integer>().withChoice(1, 33).withChoice(2, Suppliers.of(33.5))
            .withChoice(3, Suppliers.of(33)).withRandom(new Random()).build();

    final Map<Integer, Integer> counts = Maps.newHashMap();
    counts.put(1, 0);
    counts.put(2, 0);
    counts.put(3, 0);

    for (int i = 0; i < 100; i++) {
      final int value = s.get();
      counts.put(value, counts.get(value) + 1);
    }

    for (final int count : counts.values()) {
      assertThat(count, greaterThan(0));
    }
  }
}
