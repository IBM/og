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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.cleversafe.og.json.FilesizeConfig;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

public class FilesizeConfigListTypeAdapterFactoryTest {
  private FilesizeConfigTypeAdapterFactory filesizeTypeAdapterFactory;
  private FilesizeConfigListTypeAdapterFactory filesizeListTypeAdapterFactory;
  private TypeToken<List<FilesizeConfig>> typeToken;
  private Gson gson;

  @Before
  public void before() {
    this.filesizeTypeAdapterFactory = new FilesizeConfigTypeAdapterFactory();
    this.filesizeListTypeAdapterFactory = new FilesizeConfigListTypeAdapterFactory();
    this.typeToken = new TypeToken<List<FilesizeConfig>>() {};
    this.gson =
        new GsonBuilder().registerTypeAdapterFactory(this.filesizeTypeAdapterFactory)
            .registerTypeAdapterFactory(this.filesizeListTypeAdapterFactory).create();
  }

  @Test
  public void nonFilesizeConfigList() {
    assertThat(this.filesizeListTypeAdapterFactory.create(this.gson, TypeToken.get(String.class)),
        nullValue());
  }

  @Test
  public void fullFilesizeConfigList() {
    final String json = "[{\"average\": 15.0, \"weight\": 3.5}, 25.0]";
    final List<FilesizeConfig> config = this.gson.fromJson(json, this.typeToken.getType());

    assertThat(config, hasSize(2));

    final FilesizeConfig f1 = config.get(0);
    assertThat(f1.getAverage(), is(15.0));
    assertThat(f1.getWeight(), is(3.5));

    final FilesizeConfig f2 = config.get(1);
    assertThat(f2.getAverage(), is(25.0));
    assertThat(f2.getWeight(), is(1.0));
  }

  @Test
  public void objectFilesizeConfig() {
    final String json = "{\"average\": 65.0}";
    final List<FilesizeConfig> config = this.gson.fromJson(json, this.typeToken.getType());

    assertThat(config, hasSize(1));

    final FilesizeConfig f1 = config.get(0);
    assertThat(f1.getAverage(), is(65.0));
    assertThat(f1.getWeight(), is(1.0));
  }

  @Test
  public void decimalFilesizeConfig() {
    final String json = "45.0";
    final List<FilesizeConfig> config = this.gson.fromJson(json, this.typeToken.getType());

    assertThat(config, hasSize(1));

    final FilesizeConfig f1 = config.get(0);
    assertThat(f1.getAverage(), is(45.0));
    assertThat(f1.getWeight(), is(1.0));
  }

  @Test
  public void serialization() {
    final List<FilesizeConfig> config = Lists.newArrayList();
    config.add(new FilesizeConfig(100.0));

    assertThat(this.gson.toJson(config), is(new GsonBuilder().create().toJson(config)));
  }
}
