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

import com.cleversafe.og.json.ContainerConfig;
import com.cleversafe.og.json.SelectionType;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

public class ContainerConfigTypeAdapterFactoryTest {
  private ContainerConfigTypeAdapterFactory typeAdapterFactory;
  private Gson gson;

  @Before
  public void before() {
    this.typeAdapterFactory = new ContainerConfigTypeAdapterFactory();
    this.gson = new GsonBuilder().registerTypeAdapterFactory(this.typeAdapterFactory).create();
  }

  @Test
  public void nonContainerConfig() {
    assertThat(this.typeAdapterFactory.create(this.gson, TypeToken.get(String.class)), nullValue());
  }

  @Test
  public void fullContainerConfig() {
    final String json =
        "{\"prefix\": \"vault\", \"selection\": \"RANDOM\", \"min_suffix\": 0, \"max_suffix\": 2, \"weights\": [1.0, 2.0, 3.0]}";
    final ContainerConfig config = this.gson.fromJson(json, ContainerConfig.class);
    assertThat(config.prefix, is("vault"));
    assertThat(config.selection, is(SelectionType.RANDOM));
    assertThat(config.minSuffix, is(0));
    assertThat(config.maxSuffix, is(2));
    assertThat(config.weights.size(), is(3));
    assertThat(config.weights.get(2), is(3.0));
  }

  @Test
  public void stringOperationConfig() {
    final String json = "vault";
    final ContainerConfig config = this.gson.fromJson(json, ContainerConfig.class);

    assertThat(config.prefix, is(json));
  }

  @Test
  public void serialization() {
    final ContainerConfig config = new ContainerConfig("vault");

    assertThat(this.gson.toJson(config), is(new GsonBuilder().create().toJson(config)));
  }
}
