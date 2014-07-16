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
// Date: Jul 16, 2014
// ---------------------

package com.cleversafe.og.json.type;

import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.cleversafe.og.json.ConcurrencyConfig;
import com.cleversafe.og.json.enums.ConcurrencyType;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

public class ConcurrencyConfigTypeAdapterFactoryTest
{
   private static final double ERR = Math.pow(0.1, 6);

   private ConcurrencyConfigTypeAdapterFactory typeAdapterFactory;
   private Gson gson;

   @Before
   public void before()
   {
      this.typeAdapterFactory = new ConcurrencyConfigTypeAdapterFactory();
      this.gson = new GsonBuilder().registerTypeAdapterFactory(this.typeAdapterFactory).create();
   }

   @Test
   public void testNonConcurrencyConfig()
   {
      Assert.assertNull(this.typeAdapterFactory.create(this.gson, TypeToken.get(String.class)));
   }

   @Test
   public void testConcurrencyConfig()
   {
      Assert.assertNotNull(this.typeAdapterFactory.create(this.gson,
            TypeToken.get(ConcurrencyConfig.class)));
   }

   @Test
   public void testFullConcurrencyConfig()
   {
      final String json = "{\"count\": 5.0}";
      final ConcurrencyConfig config = this.gson.fromJson(json, ConcurrencyConfig.class);

      Assert.assertEquals(ConcurrencyType.THREADS, config.getType());
      Assert.assertEquals(5.0, config.getCount(), ERR);
      Assert.assertEquals(TimeUnit.SECONDS, config.getUnit());
      Assert.assertEquals(0.0, config.getRampup(), ERR);
      Assert.assertEquals(TimeUnit.SECONDS, config.getRampupUnit());
   }

   @Test
   public void testStringFilesizeConfig()
   {
      final String json = "10.0";
      final ConcurrencyConfig config = this.gson.fromJson(json, ConcurrencyConfig.class);

      Assert.assertEquals(ConcurrencyType.THREADS, config.getType());
      Assert.assertEquals(10.0, config.getCount(), ERR);
      Assert.assertEquals(TimeUnit.SECONDS, config.getUnit());
      Assert.assertEquals(0.0, config.getRampup(), ERR);
      Assert.assertEquals(TimeUnit.SECONDS, config.getRampupUnit());
   }

   @Test
   public void testSerialization()
   {
      final ConcurrencyConfig config = new ConcurrencyConfig(15.0);

      final String typeAdapterSerialization = this.gson.toJson(config);
      final String defaultSerialization = new GsonBuilder().create().toJson(config);

      Assert.assertEquals(defaultSerialization, typeAdapterSerialization);
   }
}
