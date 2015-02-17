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

import com.cleversafe.og.json.ConcurrencyConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

public class ConcurrencyConfigTypeAdapterFactoryTest {
  private ConcurrencyConfigTypeAdapterFactory typeAdapterFactory;
  private Gson gson;

  @Before
  public void before() {
    this.typeAdapterFactory = new ConcurrencyConfigTypeAdapterFactory();
    this.gson = new GsonBuilder().registerTypeAdapterFactory(this.typeAdapterFactory).create();
  }

  @Test
  public void nonConcurrencyConfig() {
    assertThat(this.typeAdapterFactory.create(this.gson, TypeToken.get(String.class)), nullValue());
  }

  @Test
  public void fullConcurrencyConfig() {
    final String json = "{\"count\": 5.0}";
    final ConcurrencyConfig config = this.gson.fromJson(json, ConcurrencyConfig.class);

    assertThat(config.getCount(), is(5.0));
  }

  @Test
  public void stringFilesizeConfig() {
    final String json = "10.0";
    final ConcurrencyConfig config = this.gson.fromJson(json, ConcurrencyConfig.class);

    assertThat(config.getCount(), is(10.0));
  }

  @Test
  public void serialization() {
    final ConcurrencyConfig config = new ConcurrencyConfig(15.0);
    assertThat(this.gson.toJson(config), is(new GsonBuilder().create().toJson(config)));
  }
}
