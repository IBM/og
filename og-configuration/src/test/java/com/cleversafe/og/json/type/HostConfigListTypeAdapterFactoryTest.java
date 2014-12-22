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

import com.cleversafe.og.json.HostConfig;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

public class HostConfigListTypeAdapterFactoryTest {
  private HostConfigTypeAdapterFactory hostTypeAdapterFactory;
  private HostConfigListTypeAdapterFactory hostListTypeAdapterFactory;
  private TypeToken<List<HostConfig>> typeToken;
  private Gson gson;

  @Before
  public void before() {
    this.hostTypeAdapterFactory = new HostConfigTypeAdapterFactory();
    this.hostListTypeAdapterFactory = new HostConfigListTypeAdapterFactory();
    this.typeToken = new TypeToken<List<HostConfig>>() {};
    this.gson =
        new GsonBuilder().registerTypeAdapterFactory(this.hostTypeAdapterFactory)
            .registerTypeAdapterFactory(this.hostListTypeAdapterFactory).create();
  }

  @Test
  public void nonHostConfigList() {
    assertThat(this.hostListTypeAdapterFactory.create(this.gson, TypeToken.get(String.class)),
        nullValue());
  }

  @Test
  public void fullHostConfigList() {
    final String json = "[{\"host\": \"127.0.0.1\", \"weight\": 3.5}, \"192.168.8.1\"]";
    final List<HostConfig> config = this.gson.fromJson(json, this.typeToken.getType());

    assertThat(config, hasSize(2));

    final HostConfig h1 = config.get(0);
    assertThat(h1.getHost(), is("127.0.0.1"));
    assertThat(h1.getWeight(), is(3.5));

    final HostConfig h2 = config.get(1);
    assertThat(h2.getHost(), is("192.168.8.1"));
    assertThat(h2.getWeight(), is(1.0));
  }

  @Test
  public void stringHostConfig() {
    final String json = "10.10.1.1";
    final List<HostConfig> config = this.gson.fromJson(json, this.typeToken.getType());

    assertThat(config, hasSize(1));

    final HostConfig h1 = config.get(0);

    assertThat(h1.getHost(), is("10.10.1.1"));
    assertThat(h1.getWeight(), is(1.0));
  }

  @Test
  public void serialization() {
    final List<HostConfig> config = Lists.newArrayList();
    config.add(new HostConfig("127.0.0.1"));

    assertThat(this.gson.toJson(config), is(new GsonBuilder().create().toJson(config)));
  }
}
