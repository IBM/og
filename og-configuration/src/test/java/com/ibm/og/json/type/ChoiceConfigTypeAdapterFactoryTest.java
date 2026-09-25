/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.json.type;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import org.junit.Before;
import org.junit.Test;

import com.ibm.og.json.ChoiceConfig;
import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

public class ChoiceConfigTypeAdapterFactoryTest {
  private ChoiceConfigTypeAdapterFactory typeAdapterFactory;
  private Gson gson;

  @Before
  public void before() {
    this.typeAdapterFactory = new ChoiceConfigTypeAdapterFactory();
    this.gson = new GsonBuilder().registerTypeAdapterFactory(this.typeAdapterFactory).create();
  }

  @Test
  public void nonChoiceConfig() {
    assertThat(this.typeAdapterFactory.create(this.gson, TypeToken.get(String.class)), nullValue());
  }

  @Test
  public void fullPrimitiveChoiceConfig() {
    final String json = "{\"choice\": 75.0, \"weight\": 3.5}";
    final ChoiceConfig<Double> config =
        this.gson.fromJson(json, new TypeToken<ChoiceConfig<Double>>() {}.getType());

    assertThat(config.choice, is(75.0));
    assertThat(config.weight, is(3.5));
  }

  @Test
  public void compactPrimitiveChoiceConfig() {
    final String json = "75.0";
    final ChoiceConfig<Double> config =
        this.gson.fromJson(json, new TypeToken<ChoiceConfig<Double>>() {}.getType());

    assertThat(config.choice, is(75.0));
    assertThat(config.weight, is(1.0));
  }

  @Test
  public void fullObjectChoiceConfig() {
    final String json = "{\"choice\": {\"enabled\": \"true\"}, \"weight\": 3.5}";
    final ChoiceConfig<MyConfig> config =
        this.gson.fromJson(json, new TypeToken<ChoiceConfig<MyConfig>>() {}.getType());

    assertThat(config.choice.enabled, is(true));
    assertThat(config.weight, is(3.5));
  }

  @Test
  public void compactObjectChoiceConfig() {
    final String json = "{\"enabled\": \"true\"}";
    final ChoiceConfig<MyConfig> config =
        this.gson.fromJson(json, new TypeToken<ChoiceConfig<MyConfig>>() {}.getType());

    assertThat(config.choice.enabled, is(true));
    assertThat(config.weight, is(1.0));
  }

  // ChoiceConfigTypeAdapter applies heuristics to determine what to do; this test covers an
  // auxillary branch in that code for when field count > 2
  @Test
  public void compactObjectChoiceConfig2() {
    final String json = "{\"enabled\": \"true\", \"count\": 1, \"total\": 100}";
    final ChoiceConfig<MyConfig> config =
        this.gson.fromJson(json, new TypeToken<ChoiceConfig<MyConfig>>() {}.getType());

    assertThat(config.choice.enabled, is(true));
    assertThat(config.choice.count, is(1L));
    assertThat(config.choice.total, is(100L));
  }

  public static class MyConfig {
    public boolean enabled;
    public long count;
    public long total;
  }

  @Test
  public void serialization() {
    final ChoiceConfig<Double> config = new ChoiceConfig<Double>(15.0);
    JsonParser parser = new JsonParser();
    assertThat(parser.parse(this.gson.toJson(config)), is(parser.parse(new GsonBuilder().create().toJson(config))));
  }
}
