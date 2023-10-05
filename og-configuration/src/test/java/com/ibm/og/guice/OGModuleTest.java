/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.guice;

import static org.mockito.Mockito.mock;

import java.sql.Time;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.ibm.og.json.FailingConditionsConfig;
import com.ibm.og.json.ConcurrencyConfig;
import com.ibm.og.json.OGConfig;
import com.ibm.og.json.RetentionConfig;
import com.ibm.og.statistic.Statistics;
import com.ibm.og.test.LoadTest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import com.ibm.og.json.StoppingConditionsConfig;
import com.google.common.collect.ImmutableMap;
import com.google.common.eventbus.EventBus;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;

@RunWith(DataProviderRunner.class)
public class OGModuleTest {
  @Rule
  public ExpectedException thrown = ExpectedException.none();
  private LoadTest test;
  private EventBus eventBus;
  private ConcurrencyConfig concurrency;
  private Statistics stats;
  private OGConfig config;

  @Before
  public void before() {
    this.test = mock(LoadTest.class);
    this.eventBus = mock(EventBus.class);
    this.concurrency = new ConcurrencyConfig();
    this.stats = mock(Statistics.class);
    this.config = mock(OGConfig.class);
  }

  @DataProvider
  public static Object[][] provideInvalidStoppingConditions() {
    final Map<Integer, Integer> statusCodes = ImmutableMap.of();

    return new Object[][] {
        {-1, 0.0, TimeUnit.SECONDS, 1, statusCodes, IllegalArgumentException.class},
        {1, -1.0, TimeUnit.SECONDS, 1, statusCodes, IllegalArgumentException.class},
        {1, 0.0, null, 1, statusCodes, NullPointerException.class},
        {1, 0.0, TimeUnit.SECONDS, -1, statusCodes, IllegalArgumentException.class},
        {1, 0.0, TimeUnit.SECONDS, 1, null, NullPointerException.class},
        {1, 0.0, TimeUnit.SECONDS, 1, ImmutableMap.of(201, -1), IllegalArgumentException.class}};
  }

  @DataProvider
  public static Object[][] provideInvalidRetentionConfig() {
    final Map<Integer, Integer> statusCodes = ImmutableMap.of();
    RetentionConfig rc = new RetentionConfig(RetentionConfig.MAX_RETENTION_EXPIRY, TimeUnit.SECONDS);
    return new Object[][] {
            {rc}
    };
  }

  @DataProvider
  public static Object[][] provideInvalidRetentionExtensionConfig() {
    final Map<Integer, Integer> statusCodes = ImmutableMap.of();
    RetentionConfig rc = new RetentionConfig(RetentionConfig.MAX_RETENTION_EXPIRY-1000, TimeUnit.SECONDS);
    return new Object[][] {
            {86400L, rc},
            // 15 years from now
            {200000L, new RetentionConfig(473040000L, TimeUnit.SECONDS)}
    };
  }

  @DataProvider
  public static Object[][] providevalidRetentionExtensionConfig() {
    final Map<Integer, Integer> statusCodes = ImmutableMap.of();
    return new Object[][] {
            {86400L, new RetentionConfig(86400L, TimeUnit.SECONDS)},
    };
  }

  @Test
  @UseDataProvider("provideInvalidStoppingConditions")
  public void invalidStoppingConditions(final long operations, final double runtime,
      final TimeUnit runtimeUnit, final long concurrentRequests,
      final Map<Integer, Integer> statusCodes, final Class<Exception> expectedException) {

    final OGModule module = new OGModule(this.config);
    final StoppingConditionsConfig stoppingConditions = new StoppingConditionsConfig();
    stoppingConditions.operations = operations;
    stoppingConditions.runtime = runtime;
    stoppingConditions.runtimeUnit = runtimeUnit;
    stoppingConditions.statusCodes = statusCodes;

    final FailingConditionsConfig failingConditions = new FailingConditionsConfig();
    failingConditions.operations = operations;
    failingConditions.runtime = runtime;
    failingConditions.runtimeUnit = runtimeUnit;
    failingConditions.concurrentRequests = concurrentRequests;
    failingConditions.statusCodes = statusCodes;

    this.thrown.expect(expectedException);
    module.provideTestConditions(this.test, this.eventBus, this.stats, this.concurrency,
        stoppingConditions, failingConditions);
  }

  @Test
  @UseDataProvider("provideInvalidRetentionConfig")
  public void invalidRentionConfig(final RetentionConfig rc) {
    final OGModule module = new OGModule(this.config);
    this.thrown.expect(IllegalArgumentException.class);
    module.provideTestRetentionConfig(rc);
  }

  @Test
  @UseDataProvider("provideInvalidRetentionExtensionConfig")
  public void invalidRentionExtensionConfig(final long currentRetention, final RetentionConfig rc) {
    final OGModule module = new OGModule(this.config);
    this.thrown.expect(IllegalArgumentException.class);
    module.provideTestRetentionExtensionConfig(currentRetention, rc);
  }

  @Test
  @UseDataProvider("providevalidRetentionExtensionConfig")
  public void validRentionExtensionConfig(final long currentRetention, final RetentionConfig rc) {
    final OGModule module = new OGModule(this.config);
    module.provideTestRetentionExtensionConfig(currentRetention, rc);
  }
}
