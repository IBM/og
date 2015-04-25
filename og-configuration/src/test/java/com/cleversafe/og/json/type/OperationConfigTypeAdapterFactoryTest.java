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

import com.cleversafe.og.json.OperationConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

public class OperationConfigTypeAdapterFactoryTest {
  private OperationConfigTypeAdapterFactory typeAdapterFactory;
  private Gson gson;

  @Before
  public void before() {
    this.typeAdapterFactory = new OperationConfigTypeAdapterFactory();
    this.gson = new GsonBuilder().registerTypeAdapterFactory(this.typeAdapterFactory).create();
  }

  @Test
  public void nonOperationConfig() {
    assertThat(this.typeAdapterFactory.create(this.gson, TypeToken.get(String.class)), nullValue());
  }

  @Test
  public void fullOperationConfig() {
    final String json = "{\"weight\": 35.0}";
    final OperationConfig config = this.gson.fromJson(json, OperationConfig.class);

    assertThat(config.weight, is(35.0));
  }

  @Test
  public void numberOperationConfig() {
    final String json = "45.0";
    final OperationConfig config = this.gson.fromJson(json, OperationConfig.class);

    assertThat(config.weight, is(45.0));
  }

  @Test
  public void serialization() {
    final OperationConfig config = new OperationConfig(75.0);

    assertThat(this.gson.toJson(config), is(new GsonBuilder().create().toJson(config)));
  }
}
