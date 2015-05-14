/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.json.type;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import org.junit.Before;
import org.junit.Test;

import com.cleversafe.og.json.FilesizeConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

public class FilesizeConfigTypeAdapterFactoryTest {
  private FilesizeConfigTypeAdapterFactory typeAdapterFactory;
  private Gson gson;

  @Before
  public void before() {
    this.typeAdapterFactory = new FilesizeConfigTypeAdapterFactory();
    this.gson = new GsonBuilder().registerTypeAdapterFactory(this.typeAdapterFactory).create();
  }

  @Test
  public void nonFilesizeConfig() {
    assertThat(this.typeAdapterFactory.create(this.gson, TypeToken.get(String.class)), nullValue());
  }

  @Test
  public void fullFilesizeConfig() {
    final String json = "{\"average\": 75.0}";
    final FilesizeConfig config = this.gson.fromJson(json, FilesizeConfig.class);

    assertThat(config.average, is(75.0));
  }

  @Test
  public void stringFilesizeConfig() {
    final String json = "80.0";
    final FilesizeConfig config = this.gson.fromJson(json, FilesizeConfig.class);

    assertThat(config.average, is(80.0));
  }

  @Test
  public void serialization() {
    final FilesizeConfig config = new FilesizeConfig(15.0);

    assertThat(this.gson.toJson(config), is(new GsonBuilder().create().toJson(config)));
  }
}
