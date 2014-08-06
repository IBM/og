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

import static org.mockito.Mockito.mock;

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.cleversafe.og.consumer.ObjectNameConsumer;
import com.cleversafe.og.http.Api;
import com.cleversafe.og.object.ObjectManager;
import com.cleversafe.og.operation.Request;
import com.cleversafe.og.producer.CachingProducer;
import com.cleversafe.og.statistic.Statistics;
import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import com.google.common.eventbus.EventBus;

public class OGModuleTest
{
   private OGModule module;
   private ObjectManager objectManager;
   private EventBus eventBus;
   private Supplier<Request> request;

   @Before
   @SuppressWarnings("unchecked")
   public void before()
   {
      this.module = new OGModule();
      this.objectManager = mock(ObjectManager.class);
      this.eventBus = new EventBus();
      this.request = mock(Supplier.class);
   }

   @Test(expected = NullPointerException.class)
   public void testProvideStatisticsNullEventBus()
   {
      this.module.provideStatistics(null);
   }

   @Test
   public void testProvideStatistics()
   {
      final Statistics stats = this.module.provideStatistics(this.eventBus);
      Assert.assertNotNull(stats);
   }

   @Test(expected = NullPointerException.class)
   public void testProvideRequestSupplierNullWrite()
   {
      this.module.provideRequestSupplier(null, this.request, this.request, 100, 0, 0);
   }

   @Test(expected = NullPointerException.class)
   public void testProvideRequestSupplierNullRead()
   {
      this.module.provideRequestSupplier(this.request, null, this.request, 100, 0, 0);
   }

   @Test(expected = NullPointerException.class)
   public void testProvideRequestSupplierNullDelete()
   {
      this.module.provideRequestSupplier(this.request, this.request, null, 100, 0, 0);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testProvideRequestSupplierWeightsNotEqual100()
   {
      this.module.provideRequestSupplier(this.request, this.request, this.request, 101, 0, 0);
   }

   @Test
   public void testProvideRequestSupplier()
   {
      final Supplier<Supplier<Request>> p =
            this.module.provideRequestSupplier(this.request, this.request, this.request, 100, 0, 0);
      Assert.assertNotNull(p);
   }

   @Test(expected = NullPointerException.class)
   public void provideObjectManagerNullObjectFileLocation()
   {
      this.module.provideObjectManager(null, "objectFileName");
   }

   @Test
   public void provideObjectManagerNullObjectFileName()
   {
      final ObjectManager objectManager =
            this.module.provideObjectManager("objectFileLocation/", null);
      Assert.assertNotNull(objectManager);
   }

   @Test
   public void provideObjectManager()
   {
      final ObjectManager objectManager =
            this.module.provideObjectManager("objectFileLocation/", "objectFileName");
      Assert.assertNotNull(objectManager);
   }

   @Test(expected = NullPointerException.class)
   public void testProvideWriteObjectNameNullApi()
   {
      this.module.provideWriteObjectName(null);
   }

   @Test
   public void testProvideWriteObjectNameSOH()
   {
      final Optional<CachingProducer<String>> p = this.module.provideWriteObjectName(Api.SOH);
      Assert.assertTrue(!p.isPresent());
   }

   @Test
   public void testProvideWriteObjectNameS3()
   {
      final Optional<CachingProducer<String>> p = this.module.provideWriteObjectName(Api.S3);
      Assert.assertTrue(p.isPresent());
   }

   @Test(expected = NullPointerException.class)
   public void testProvideReadObjectNameNullObjectManager()
   {
      this.module.provideReadObjectName(null);
   }

   @Test
   public void testProvideReadObjectName()
   {
      final CachingProducer<String> p = this.module.provideReadObjectName(this.objectManager);
      Assert.assertNotNull(p);
   }

   @Test(expected = NullPointerException.class)
   public void testProvideDeleteObjectNameNullObjectManager()
   {
      this.module.provideDeleteObjectName(null);
   }

   @Test
   public void testProvideDeleteObjectName()
   {
      final CachingProducer<String> p = this.module.provideDeleteObjectName(this.objectManager);
      Assert.assertNotNull(p);
   }

   @Test(expected = NullPointerException.class)
   public void testProvideObjectNameConsumersNullObjectManager()
   {
      this.module.provideObjectNameConsumers(null, this.eventBus);
   }

   @Test(expected = NullPointerException.class)
   public void testProvideObjectNameConsumersNullEventBus()
   {
      this.module.provideObjectNameConsumers(this.objectManager, null);
   }

   @Test
   public void testProvideObjectNameConsumers()
   {
      final List<ObjectNameConsumer> c =
            this.module.provideObjectNameConsumers(this.objectManager, this.eventBus);

      Assert.assertNotNull(c);
      Assert.assertTrue(!c.isEmpty());
   }
}
