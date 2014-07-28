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
import com.cleversafe.og.http.util.Api;
import com.cleversafe.og.object.manager.ObjectManager;
import com.cleversafe.og.statistic.Statistics;
import com.cleversafe.og.util.producer.CachingProducer;
import com.google.common.eventbus.EventBus;

public class OGModuleTest
{
   private OGModule module;
   private ObjectManager objectManager;
   private EventBus eventBus;

   @Before
   public void before()
   {
      this.module = new OGModule();
      this.objectManager = mock(ObjectManager.class);
      this.eventBus = new EventBus();
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
      final CachingProducer<String> p = this.module.provideWriteObjectName(Api.SOH);
      Assert.assertNull(p);
   }

   @Test
   public void testProvideWriteObjectNameS3()
   {
      final CachingProducer<String> p = this.module.provideWriteObjectName(Api.S3);
      Assert.assertNotNull(p);
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
