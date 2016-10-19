/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
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
        "{\"prefix\": \"vault\", \"selection\": \"RANDOM\", \"minSuffix\": 0, \"maxSuffix\": 2, \"weights\": [1.0, 2.0, 3.0]}";
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
