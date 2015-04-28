package com.cleversafe.og.util;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;

@RunWith(DataProviderRunner.class)
public class DistributionsTest {
  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @DataProvider
  public static Object[][] provideInvalidReal() {
    return new Object[][] { {-1, 1}, {1, -1}, {1, 10}};
  }

  @Test
  @UseDataProvider("provideInvalidReal")
  public void invalidUniform(final double average, final double spread) {
    this.thrown.expect(IllegalArgumentException.class);
    Distributions.uniform(average, spread);
  }

  @Test
  @UseDataProvider("provideInvalidReal")
  public void invalidNormal(final double average, final double spread) {
    this.thrown.expect(IllegalArgumentException.class);
    Distributions.normal(average, spread);
  }

  @Test
  @UseDataProvider("provideInvalidReal")
  public void invalidLogNormal(final double average, final double spread) {
    this.thrown.expect(IllegalArgumentException.class);
    Distributions.lognormal(average, spread);
  }

  @Test(expected = IllegalArgumentException.class)
  public void invalidPoisson() {
    Distributions.poisson(-1);
  }

  @Test
  public void uniform() {
    validate(Distributions.uniform(10, 1));
  }

  @Test
  public void normal() {
    validate(Distributions.normal(10, 1));
  }

  @Test
  public void lognormal() {
    validate(Distributions.lognormal(10, 1));
  }

  @Test
  public void poisson() {
    validate(Distributions.poisson(10));
  }

  private static void validate(final Distribution d) {
    // TODO more thorough validation of each distribution type
    // just validate that nextSample, getAverage, and toString execute without exception
    for (int i = 0; i < 100; i++) {
      d.nextSample();
    }
    d.getAverage();
    d.toString();
  }
}
