/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.json.type;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import org.junit.Before;
import org.junit.Test;

import com.cleversafe.og.json.ChoiceConfig;
import com.cleversafe.og.json.SelectionConfig;
import com.cleversafe.og.json.SelectionType;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

public class SelectionConfigTypeAdapterFactoryTest {
  private SelectionConfigTypeAdapterFactory typeAdapterFactory;
  private Gson gson;

  @Before
  public void before() {
    this.typeAdapterFactory = new SelectionConfigTypeAdapterFactory();
    this.gson =
        new GsonBuilder().registerTypeAdapterFactory(this.typeAdapterFactory)
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

    assertThat(config.selection, is(SelectionType.ROUNDROBIN));
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

    MyConfig choice = config.choices.get(0).choice;
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
    assertThat(this.gson.toJson(config),
        equalToIgnoringCase(new GsonBuilder().create().toJson(config)));
  }
}
