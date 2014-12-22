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
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import org.junit.Before;
import org.junit.Test;

import com.cleversafe.og.json.HostConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

public class HostConfigTypeAdapterFactoryTest {
  private HostConfigTypeAdapterFactory typeAdapterFactory;
  private Gson gson;

  @Before
  public void before() {
    this.typeAdapterFactory = new HostConfigTypeAdapterFactory();
    this.gson = new GsonBuilder().registerTypeAdapterFactory(this.typeAdapterFactory).create();
  }

  @Test
  public void nonHostConfig() {
    assertThat(this.typeAdapterFactory.create(this.gson, TypeToken.get(String.class)), nullValue());
  }

  @Test
  public void fullHostConfig() {
    final String json = "{\"host\": \"127.0.0.1\", \"weight\": 3.5}";
    final HostConfig config = this.gson.fromJson(json, HostConfig.class);

    assertThat(config.getHost(), is("127.0.0.1"));
    assertThat(config.getWeight(), is(3.5));
  }

  @Test
  public void stringHostConfig() {
    final String json = "192.168.8.1";
    final HostConfig config = this.gson.fromJson(json, HostConfig.class);

    assertThat(config.getHost(), is("192.168.8.1"));
    assertThat(config.getWeight(), is(1.0));
  }

  @Test
  public void serialization() {
    final HostConfig config = new HostConfig("127.0.0.1");

    assertThat(this.gson.toJson(config), is(new GsonBuilder().create().toJson(config)));
  }
}
