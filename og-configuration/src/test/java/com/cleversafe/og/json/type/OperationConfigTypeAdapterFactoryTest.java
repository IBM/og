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

import com.cleversafe.og.json.OperationConfig;
import com.cleversafe.og.json.enums.CollectionAlgorithmType;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

public class OperationConfigTypeAdapterFactoryTest
{
   private static final double ERR = Math.pow(0.1, 6);

   private OperationConfigTypeAdapterFactory typeAdapterFactory;
   private Gson gson;

   @Before
   public void before()
   {
      this.typeAdapterFactory = new OperationConfigTypeAdapterFactory();
      this.gson = new GsonBuilder().registerTypeAdapterFactory(this.typeAdapterFactory).create();
   }

   @Test
   public void testNonOperationConfig()
   {
      Assert.assertNull(this.typeAdapterFactory.create(this.gson, TypeToken.get(String.class)));
   }

   @Test
   public void testOperationConfig()
   {
      Assert.assertNotNull(this.typeAdapterFactory.create(this.gson,
            TypeToken.get(OperationConfig.class)));
   }

   @Test
   public void testFullOperationConfig()
   {
      final String json = "{\"weight\": 35.0}";
      final OperationConfig config = this.gson.fromJson(json, OperationConfig.class);

      Assert.assertEquals(35.0, config.getWeight(), ERR);
      Assert.assertEquals(CollectionAlgorithmType.ROUNDROBIN, config.getHostSelection());
      Assert.assertNotNull(config.getHost());
      Assert.assertNotNull(config.getHeaders());
   }

   @Test
   public void testNumberOperationConfig()
   {
      final String json = "45.0";
      final OperationConfig config = this.gson.fromJson(json, OperationConfig.class);

      Assert.assertEquals(45.0, config.getWeight(), ERR);
      Assert.assertEquals(CollectionAlgorithmType.ROUNDROBIN, config.getHostSelection());
      Assert.assertNotNull(config.getHost());
      Assert.assertNotNull(config.getHeaders());
   }

   @Test
   public void testSerialization()
   {
      final OperationConfig config = new OperationConfig(75.0);

      final String typeAdapterSerialization = this.gson.toJson(config);
      final String defaultSerialization = new GsonBuilder().create().toJson(config);

      Assert.assertEquals(defaultSerialization, typeAdapterSerialization);
   }
}
