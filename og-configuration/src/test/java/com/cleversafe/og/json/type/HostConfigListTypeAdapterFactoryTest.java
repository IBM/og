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

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.cleversafe.og.json.HostConfig;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

public class HostConfigListTypeAdapterFactoryTest
{
   private static final double ERR = Math.pow(0.1, 6);

   private HostConfigTypeAdapterFactory hostTypeAdapterFactory;
   private HostConfigListTypeAdapterFactory hostListTypeAdapterFactory;
   private TypeToken<List<HostConfig>> typeToken;
   private Gson gson;

   @Before
   public void before()
   {
      this.hostTypeAdapterFactory = new HostConfigTypeAdapterFactory();
      this.hostListTypeAdapterFactory = new HostConfigListTypeAdapterFactory();
      this.typeToken = new TypeToken<List<HostConfig>>()
      {};
      this.gson = new GsonBuilder()
            .registerTypeAdapterFactory(this.hostTypeAdapterFactory)
            .registerTypeAdapterFactory(this.hostListTypeAdapterFactory)
            .create();
   }

   @Test
   public void testNonHostConfigList()
   {
      Assert.assertNull(this.hostListTypeAdapterFactory.create(this.gson,
            TypeToken.get(String.class)));
   }

   @Test
   public void testHostConfigList()
   {
      Assert.assertNotNull(this.hostListTypeAdapterFactory.create(this.gson, this.typeToken));
   }

   @Test
   public void testFullHostConfigList()
   {
      final String json = "[{\"host\": \"127.0.0.1\", \"weight\": 3.5}, \"192.168.8.1\"]";
      final List<HostConfig> config = this.gson.fromJson(json, this.typeToken.getType());

      Assert.assertEquals(2, config.size());
      final HostConfig h1 = config.get(0);
      Assert.assertEquals("127.0.0.1", h1.getHost());
      Assert.assertEquals(3.5, h1.getWeight(), ERR);
      final HostConfig h2 = config.get(1);
      Assert.assertEquals("192.168.8.1", h2.getHost());
      Assert.assertEquals(1.0, h2.getWeight(), ERR);
   }

   @Test
   public void testStringHostConfig()
   {
      final String json = "10.10.1.1";
      final List<HostConfig> config = this.gson.fromJson(json, this.typeToken.getType());

      Assert.assertEquals(1, config.size());
      final HostConfig h1 = config.get(0);
      Assert.assertEquals("10.10.1.1", h1.getHost());
      Assert.assertEquals(1.0, h1.getWeight(), ERR);
   }

   @Test
   public void testSerialization()
   {
      final List<HostConfig> config = Lists.newArrayList();
      config.add(new HostConfig("127.0.0.1"));

      final String typeAdapterSerialization = this.gson.toJson(config);
      final String defaultSerialization = new GsonBuilder().create().toJson(config);

      Assert.assertEquals(defaultSerialization, typeAdapterSerialization);
   }
}
