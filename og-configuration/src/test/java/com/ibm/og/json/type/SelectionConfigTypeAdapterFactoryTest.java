/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.json.type;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import com.google.gson.JsonParser;
import com.ibm.og.json.ChoiceConfig;
import com.ibm.og.json.SelectionConfig;
import com.ibm.og.json.SelectionType;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import com.ibm.og.util.json.type.CaseInsensitiveEnumTypeAdapterFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

public class SelectionConfigTypeAdapterFactoryTest {
  private SelectionConfigTypeAdapterFactory typeAdapterFactory;
  private Gson gson;

  @Before
  public void before() {
    this.typeAdapterFactory = new SelectionConfigTypeAdapterFactory();
    this.gson = new GsonBuilder().registerTypeAdapterFactory(this.typeAdapterFactory)
        .registerTypeAdapterFactory(new ChoiceConfigTypeAdapterFactory())
        .registerTypeAdapterFactory(new CaseInsensitiveEnumTypeAdapterFactory()).create();
  }

  @Test
  public void nonSelectionConfig() {
    assertThat(this.typeAdapterFactory.create(this.gson, TypeToken.get(String.class)), nullValue());
  }

  @Test
  public void fullPrimitiveSelectionConfig() {
    final String json = "{\"selection\": \"roundrobin\", \"choices\": [75.0]}";
    final SelectionConfig<Double> config =
        this.gson.fromJson(json, new TypeToken<SelectionConfig<Double>>() {}.getType());

    assertThat(config.selection, Matchers.is(SelectionType.ROUNDROBIN));
    assertThat(config.choices.get(0).choice, is(75.0));
  }

  @Test
  public void listPrimitiveSelectionConfig() {
    final String json = "[75.0]";
    final SelectionConfig<Double> config =
        this.gson.fromJson(json, new TypeToken<SelectionConfig<Double>>() {}.getType());

    assertThat(config.choices.get(0).choice, is(75.0));
  }

  @Test
  public void compactPrimitiveSelectionConfig() {
    final String json = "75.0";
    final SelectionConfig<Double> config =
        this.gson.fromJson(json, new TypeToken<SelectionConfig<Double>>() {}.getType());

    assertThat(config.choices.get(0).choice, is(75.0));
  }

  @Test
  public void fullObjectSelectionConfig() {
    final String json = "{\"selection\": \"roundrobin\", \"choices\": [{\"enabled\": \"true\"}]}";
    final SelectionConfig<MyConfig> config =
        this.gson.fromJson(json, new TypeToken<SelectionConfig<MyConfig>>() {}.getType());

    assertThat(config.choices.get(0).choice.enabled, is(true));
  }

  @Test
  public void listObjectSelectionConfig() {
    final String json = "[{\"enabled\": \"true\"}]";
    final SelectionConfig<MyConfig> config =
        this.gson.fromJson(json, new TypeToken<SelectionConfig<MyConfig>>() {}.getType());

    assertThat(config.choices.get(0).choice.enabled, is(true));
  }

  @Test
  public void compactObjectSelectionConfig() {
    final String json = "{\"enabled\": \"true\"}";
    final SelectionConfig<MyConfig> config =
        this.gson.fromJson(json, new TypeToken<SelectionConfig<MyConfig>>() {}.getType());

    assertThat(config.choices.get(0).choice.enabled, is(true));
  }

  // SelectionConfigTypeAdapter applies heuristics to determine what to do; this test covers an
  // auxillary branch in that code for when field count > 2
  @Test
  public void compactObjectSelectionConfig2() {
    final String json = "{\"enabled\": \"true\", \"count\": 1, \"total\": 100}";
    final SelectionConfig<MyConfig> config =
        this.gson.fromJson(json, new TypeToken<SelectionConfig<MyConfig>>() {}.getType());

    final MyConfig choice = config.choices.get(0).choice;
    assertThat(choice.enabled, is(true));
    assertThat(choice.count, is(1L));
    assertThat(choice.total, is(100L));
  }

  public static class MyConfig {
    public boolean enabled;
    public long count;
    public long total;
  }

  @Test
  public void serialization() {
    final SelectionConfig<Double> config = new SelectionConfig<Double>();
    config.choices.add(new ChoiceConfig<Double>(15.0));
    // currently gson serializes enums in ALL CAPS, so we do a case insensitive compare here
    JsonParser parser = new JsonParser();
    assertThat(parser.parse(this.gson.toJson(config).toUpperCase()),
        is(parser.parse((new GsonBuilder().create().toJson(config).toUpperCase()))));
  }
}
