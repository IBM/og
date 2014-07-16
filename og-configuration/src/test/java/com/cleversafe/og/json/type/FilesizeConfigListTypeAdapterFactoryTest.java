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

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.cleversafe.og.json.FilesizeConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

public class FilesizeConfigListTypeAdapterFactoryTest
{
   private static final double ERR = Math.pow(0.1, 6);

   private FilesizeConfigTypeAdapterFactory filesizeTypeAdapterFactory;
   private FilesizeConfigListTypeAdapterFactory filesizeListTypeAdapterFactory;
   private TypeToken<List<FilesizeConfig>> typeToken;
   private Gson gson;

   @Before
   public void before()
   {
      this.filesizeTypeAdapterFactory = new FilesizeConfigTypeAdapterFactory();
      this.filesizeListTypeAdapterFactory = new FilesizeConfigListTypeAdapterFactory();
      this.typeToken = new TypeToken<List<FilesizeConfig>>()
      {};
      this.gson = new GsonBuilder()
            .registerTypeAdapterFactory(this.filesizeTypeAdapterFactory)
            .registerTypeAdapterFactory(this.filesizeListTypeAdapterFactory)
            .create();
   }

   @Test
   public void testNonFilesizeConfigList()
   {
      Assert.assertNull(this.filesizeListTypeAdapterFactory.create(this.gson,
            TypeToken.get(String.class)));
   }

   @Test
   public void testHostConfigList()
   {
      Assert.assertNotNull(this.filesizeListTypeAdapterFactory.create(this.gson, this.typeToken));
   }

   @Test
   public void testFullFilesizeConfigList()
   {
      final String json = "[{\"average\": 15.0, \"weight\": 3.5}, 25.0]";
      final List<FilesizeConfig> config = this.gson.fromJson(json, this.typeToken.getType());

      Assert.assertEquals(2, config.size());
      final FilesizeConfig f1 = config.get(0);
      Assert.assertEquals(15.0, f1.getAverage(), ERR);
      Assert.assertEquals(3.5, f1.getWeight(), ERR);
      final FilesizeConfig f2 = config.get(1);
      Assert.assertEquals(25.0, f2.getAverage(), ERR);
      Assert.assertEquals(1.0, f2.getWeight(), ERR);
   }

   @Test
   public void testObjectFilesizeConfig()
   {
      final String json = "{\"average\": 65.0}";
      final List<FilesizeConfig> config = this.gson.fromJson(json, this.typeToken.getType());

      Assert.assertEquals(1, config.size());
      final FilesizeConfig f1 = config.get(0);
      Assert.assertEquals(65.0, f1.getAverage(), ERR);
      Assert.assertEquals(1.0, f1.getWeight(), ERR);
   }

   @Test
   public void testDecimalFilesizeConfig()
   {
      final String json = "45.0";
      final List<FilesizeConfig> config = this.gson.fromJson(json, this.typeToken.getType());

      Assert.assertEquals(1, config.size());
      final FilesizeConfig f1 = config.get(0);
      Assert.assertEquals(45.0, f1.getAverage(), ERR);
      Assert.assertEquals(1.0, f1.getWeight(), ERR);
   }

   @Test
   public void testSerialization()
   {
      final List<FilesizeConfig> config = new ArrayList<FilesizeConfig>();
      config.add(new FilesizeConfig(100.0));

      final String typeAdapterSerialization = this.gson.toJson(config);
      final String defaultSerialization = new GsonBuilder().create().toJson(config);

      Assert.assertEquals(defaultSerialization, typeAdapterSerialization);
   }
}
