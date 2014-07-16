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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.cleversafe.og.json.HostConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

public class HostConfigTypeAdapterFactoryTest
{
   private static final double ERR = Math.pow(0.1, 6);

   private HostConfigTypeAdapterFactory typeAdapterFactory;
   private Gson gson;

   @Before
   public void before()
   {
      this.typeAdapterFactory = new HostConfigTypeAdapterFactory();
      this.gson = new GsonBuilder().registerTypeAdapterFactory(this.typeAdapterFactory).create();
   }

   @Test
   public void testNonHostConfig()
   {
      Assert.assertNull(this.typeAdapterFactory.create(this.gson, TypeToken.get(String.class)));
   }

   @Test
   public void testHostConfig()
   {
      Assert.assertNotNull(this.typeAdapterFactory.create(this.gson,
            TypeToken.get(HostConfig.class)));
   }

   @Test
   public void testFullHostConfig()
   {
      final String json = "{\"host\": \"127.0.0.1\", \"weight\": 3.5}";
      final HostConfig config = this.gson.fromJson(json, HostConfig.class);

      Assert.assertEquals("127.0.0.1", config.getHost());
      Assert.assertEquals(3.5, config.getWeight(), ERR);
   }

   @Test
   public void testStringHostConfig()
   {
      final String json = "192.168.8.1";
      final HostConfig config = this.gson.fromJson(json, HostConfig.class);

      Assert.assertEquals("192.168.8.1", config.getHost());
      Assert.assertEquals(1.0, config.getWeight(), ERR);
   }

   @Test
   public void testSerialization()
   {
      final HostConfig config = new HostConfig("127.0.0.1");

      final String typeAdapterSerialization = this.gson.toJson(config);
      final String defaultSerialization = new GsonBuilder().create().toJson(config);

      Assert.assertEquals(defaultSerialization, typeAdapterSerialization);
   }
}
