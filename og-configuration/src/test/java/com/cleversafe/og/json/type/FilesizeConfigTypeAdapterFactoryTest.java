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

import com.cleversafe.og.json.FilesizeConfig;
import com.cleversafe.og.json.enums.DistributionType;
import com.cleversafe.og.util.SizeUnit;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

public class FilesizeConfigTypeAdapterFactoryTest
{
   private static final double ERR = Math.pow(0.1, 6);

   private FilesizeConfigTypeAdapterFactory typeAdapterFactory;
   private Gson gson;

   @Before
   public void before()
   {
      this.typeAdapterFactory = new FilesizeConfigTypeAdapterFactory();
      this.gson = new GsonBuilder().registerTypeAdapterFactory(this.typeAdapterFactory).create();
   }

   @Test
   public void testNonFilesizeConfig()
   {
      Assert.assertNull(this.typeAdapterFactory.create(this.gson, TypeToken.get(String.class)));
   }

   @Test
   public void testFilesizeConfig()
   {
      Assert.assertNotNull(this.typeAdapterFactory.create(this.gson,
            TypeToken.get(FilesizeConfig.class)));
   }

   @Test
   public void testFullFilesizeConfig()
   {
      final String json = "{\"average\": 75.0, \"weight\": 3.5}";
      final FilesizeConfig config = this.gson.fromJson(json, FilesizeConfig.class);

      Assert.assertEquals(DistributionType.UNIFORM, config.getDistribution());
      Assert.assertEquals(75.0, config.getAverage(), ERR);
      Assert.assertEquals(SizeUnit.MEBIBYTES, config.getAverageUnit());
      Assert.assertEquals(0.0, config.getSpread(), ERR);
      Assert.assertEquals(SizeUnit.MEBIBYTES, config.getSpreadUnit());
      Assert.assertEquals(3.5, config.getWeight(), ERR);
   }

   @Test
   public void testStringFilesizeConfig()
   {
      final String json = "80.0";
      final FilesizeConfig config = this.gson.fromJson(json, FilesizeConfig.class);

      Assert.assertEquals(DistributionType.UNIFORM, config.getDistribution());
      Assert.assertEquals(80.0, config.getAverage(), ERR);
      Assert.assertEquals(SizeUnit.MEBIBYTES, config.getAverageUnit());
      Assert.assertEquals(0.0, config.getSpread(), ERR);
      Assert.assertEquals(SizeUnit.MEBIBYTES, config.getSpreadUnit());
      Assert.assertEquals(1.0, config.getWeight(), ERR);
   }

   @Test
   public void testSerialization()
   {
      final FilesizeConfig config = new FilesizeConfig(15.0);

      final String typeAdapterSerialization = this.gson.toJson(config);
      final String defaultSerialization = new GsonBuilder().create().toJson(config);

      Assert.assertEquals(defaultSerialization, typeAdapterSerialization);
   }
}
