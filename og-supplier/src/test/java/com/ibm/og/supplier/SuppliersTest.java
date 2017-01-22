/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.supplier;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.List;

import org.junit.Test;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class SuppliersTest {
  @Test(expected = NullPointerException.class)
  public void nullOf() {
    Suppliers.of(null);
  }

  @Test
  public void of() {
    final Supplier<Integer> s = Suppliers.of(1);
    for (int i = 0; i < 10; i++) {
      assertThat(s.get(), is(1));
    }
  }

  @Test(expected = NullPointerException.class)
  public void nullCycle() {
    Suppliers.cycle(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void emptyCycle() {
    Suppliers.cycle(ImmutableList.of());
  }

  @Test(expected = NullPointerException.class)
  public void nullCycleElement() {
    final List<Integer> list = Lists.newArrayList();
    list.add(null);
    Suppliers.cycle(list);
  }

  @Test
  public void cycleOneElement() {
    final Supplier<Integer> p = Suppliers.cycle(ImmutableList.of(1));
    for (int i = 0; i < 10; i++) {
      assertThat(p.get(), is(1));
    }
  }

  @Test
  public void cycleMultipleElements() {
    final Supplier<Integer> p = Suppliers.cycle(ImmutableList.of(1, 2, 3));
    for (int i = 0; i < 10; i++) {
      assertThat(p.get(), is(1));
      assertThat(p.get(), is(2));
      assertThat(p.get(), is(3));
    }
  }

  @Test
  public void cycleModification() {
    final List<Integer> list = Lists.newArrayList();
    list.add(1);
    list.add(2);
    final Supplier<Integer> p = Suppliers.cycle(list);
    list.add(3);
    for (int i = 0; i < 10; i++) {
      assertThat(p.get(), is(1));
      assertThat(p.get(), is(2));
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void cycleNegativeMinValue() {
    Suppliers.cycle(-1, 100);
  }

  @Test(expected = IllegalArgumentException.class)
  public void cycleMaxValueLessThanMinValue() {
    Suppliers.cycle(10, 9);
  }

  @Test
  public void cycleMinValueMaxValue() {
    final Supplier<Long> cycle = Suppliers.cycle(0, 3);
    assertThat(cycle.get(), is(0L));
    assertThat(cycle.get(), is(1L));
    assertThat(cycle.get(), is(2L));
    assertThat(cycle.get(), is(3L));
    assertThat(cycle.get(), is(0L));
    assertThat(cycle.get(), is(1L));
  }

  @Test(expected = IllegalArgumentException.class)
  public void randomNegativeMinValue() {
    Suppliers.random(-1, 100);
  }

  @Test(expected = IllegalArgumentException.class)
  public void randomMaxValueLessThanMinValue() {
    Suppliers.random(10, 9);
  }
}
