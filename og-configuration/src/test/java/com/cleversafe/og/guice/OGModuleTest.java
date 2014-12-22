//
// Copyright (C) 2005-2011 Cleversafe, Inc. All rights reserved.
//
// Contact Information:
// Cleversafe, Inc.
// 222 South Riverside Plaza
// Suite 1700
// Chicago, IL 60606, USA
//
// licensing@cleversafe.com
//
// END-OF-HEADER
//
// -----------------------
// @author: rveitch
//
// Date: Jul 28, 2014
// ---------------------

package com.cleversafe.og.guice;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import com.cleversafe.og.api.Request;
import com.cleversafe.og.http.Api;
import com.cleversafe.og.object.AbstractObjectNameConsumer;
import com.cleversafe.og.object.ObjectManager;
import com.cleversafe.og.statistic.Statistics;
import com.cleversafe.og.supplier.CachingSupplier;
import com.google.common.base.Supplier;
import com.google.common.eventbus.EventBus;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;

@RunWith(DataProviderRunner.class)
public class OGModuleTest {
  @Rule
  public ExpectedException thrown = ExpectedException.none();
  private OGModule module;
  private ObjectManager objectManager;
  private EventBus eventBus;

  @Before
  public void before() {
    this.module = new OGModule();
    this.objectManager = mock(ObjectManager.class);
    this.eventBus = new EventBus();
  }

  @Test(expected = NullPointerException.class)
  public void provideStatisticsNullEventBus() {
    this.module.provideStatistics(null);
  }

  @Test
  public void provideStatistics() {
    final Statistics stats = this.module.provideStatistics(this.eventBus);
    assertThat(stats, notNullValue());
  }

  @DataProvider
  public static Object[][] provideInvalidProvideRequestSupplier() {
    @SuppressWarnings("unchecked")
    final Supplier<Request> supplier = mock(Supplier.class);

    return new Object[][] { {null, supplier, supplier, 100, 0, 0, NullPointerException.class},
        {supplier, null, supplier, 100, 0, 0, NullPointerException.class},
        {supplier, supplier, null, 100, 0, 0, NullPointerException.class},
        {supplier, supplier, supplier, 99, 0, 0, IllegalArgumentException.class},
        {supplier, supplier, supplier, 101, 0, 0, IllegalArgumentException.class},
        {supplier, supplier, supplier, 50, 50, 50, IllegalArgumentException.class},};
  }

  @Test
  @UseDataProvider("provideInvalidProvideRequestSupplier")
  public void provideRequestSupplier(final Supplier<Request> write, final Supplier<Request> read,
      final Supplier<Request> delete, final double writeWeight, final double readWeight,
      final double deleteWeight, final Class<Exception> expectedException) {
    this.thrown.expect(expectedException);
    this.module.provideRequestSupplier(write, read, delete, writeWeight, readWeight, deleteWeight);
  }

  @Test
  public void provideRequestSupplier() {
    @SuppressWarnings("unchecked")
    final Supplier<Request> supplier = mock(Supplier.class);
    final Supplier<Request> s =
        this.module.provideRequestSupplier(supplier, supplier, supplier, 100, 0, 0);

    assertThat(s, notNullValue());
  }

  @Test(expected = NullPointerException.class)
  public void provideObjectManagerNullObjectFileLocation() {
    this.module.provideObjectManager(null, "name");
  }

  @Test
  public void provideObjectManagerNullObjectFileName() {
    final ObjectManager objectManager = this.module.provideObjectManager("location/", null);
    assertThat(objectManager, notNullValue());
  }

  @Test
  public void provideObjectManager() {
    final ObjectManager objectManager = this.module.provideObjectManager("location/", "name");
    assertThat(objectManager, notNullValue());
  }

  @Test(expected = NullPointerException.class)
  public void provideWriteObjectNameNullApi() {
    this.module.provideWriteObjectName(null);
  }

  @Test
  public void provideWriteObjectNameSOH() {
    assertThat(this.module.provideWriteObjectName(Api.SOH), nullValue());
  }

  @Test
  public void provideWriteObjectNameS3() {
    assertThat(this.module.provideWriteObjectName(Api.S3), notNullValue());
  }

  @Test(expected = NullPointerException.class)
  public void provideReadObjectNameNullObjectManager() {
    this.module.provideReadObjectName(null);
  }

  @Test
  public void provideReadObjectName() {
    final CachingSupplier<String> s = this.module.provideReadObjectName(this.objectManager);
    assertThat(s, notNullValue());
  }

  @Test(expected = NullPointerException.class)
  public void provideDeleteObjectNameNullObjectManager() {
    this.module.provideDeleteObjectName(null);
  }

  @Test
  public void provideDeleteObjectName() {
    final CachingSupplier<String> s = this.module.provideDeleteObjectName(this.objectManager);
    assertThat(s, notNullValue());
  }

  @Test(expected = NullPointerException.class)
  public void provideObjectNameConsumersNullObjectManager() {
    this.module.provideObjectNameConsumers(null, this.eventBus);
  }

  @Test(expected = NullPointerException.class)
  public void provideObjectNameConsumersNullEventBus() {
    this.module.provideObjectNameConsumers(this.objectManager, null);
  }

  @Test
  public void provideObjectNameConsumers() {
    final List<AbstractObjectNameConsumer> c =
        this.module.provideObjectNameConsumers(this.objectManager, this.eventBus);

    assertThat(c, notNullValue());
    assertThat(c, not(empty()));
  }
}
