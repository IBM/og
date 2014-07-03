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
// Date: Jun 25, 2014
// ---------------------

package com.cleversafe.og.guice;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cleversafe.og.guice.annotation.DeleteObjectName;
import com.cleversafe.og.guice.annotation.ReadObjectName;
import com.cleversafe.og.guice.annotation.TestObjectFileLocation;
import com.cleversafe.og.guice.annotation.TestObjectFileName;
import com.cleversafe.og.guice.annotation.WriteObjectName;
import com.cleversafe.og.http.util.Api;
import com.cleversafe.og.object.manager.ObjectManager;
import com.cleversafe.og.object.manager.RandomObjectPopulator;
import com.cleversafe.og.object.producer.DeleteObjectNameProducer;
import com.cleversafe.og.object.producer.ReadObjectNameProducer;
import com.cleversafe.og.object.producer.UUIDObjectNameProducer;
import com.cleversafe.og.util.producer.CachingProducer;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

public class ObjectManagerModule extends AbstractModule
{
   private static final Logger _logger = LoggerFactory.getLogger(ObjectManagerModule.class);

   public ObjectManagerModule()
   {}

   @Override
   protected void configure()
   {}

   @Provides
   @Singleton
   public ObjectManager provideObjectManager(
         @TestObjectFileLocation final String objectFileLocation,
         @TestObjectFileName final String objectFileName)
   {
      return new RandomObjectPopulator(UUID.randomUUID(), objectFileLocation, objectFileName);
   }

   @Provides
   @Singleton
   @WriteObjectName
   public CachingProducer<String> provideWriteObjectName(final Api api)
   {
      if (Api.SOH == api)
         return null;
      return new CachingProducer<String>(new UUIDObjectNameProducer());
   }

   @Provides
   @Singleton
   @ReadObjectName
   public CachingProducer<String> provideReadObjectName(final ObjectManager objectManager)
   {
      return new CachingProducer<String>(new ReadObjectNameProducer(objectManager));
   }

   @Provides
   @Singleton
   @DeleteObjectName
   public CachingProducer<String> provideDeleteObjectName(final ObjectManager objectManager)
   {
      return new CachingProducer<String>(new DeleteObjectNameProducer(objectManager));
   }
}
