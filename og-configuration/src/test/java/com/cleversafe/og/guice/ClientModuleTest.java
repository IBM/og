/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.guice;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;

import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.cleversafe.og.http.HttpAuth;
import com.cleversafe.og.http.ResponseBodyConsumer;
import com.cleversafe.og.json.ClientConfig;
import com.google.common.collect.ImmutableMap;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;

@RunWith(DataProviderRunner.class)
public class ClientModuleTest {
  @Test(expected = NullPointerException.class)
  public void nullClientConfig() {
    new ClientModule(null);
  }

  @Test
  public void clientModule() {
    assertThat(new ClientModule(new ClientConfig()), notNullValue());
  }

  @Test(expected = NullPointerException.class)
  public void nullResponseBodyConsumers() {
    new ClientModule(new ClientConfig()).provideClient(null, null);
  }

  @DataProvider
  public static Object[][] provideClientData() {
    final Map<String, ResponseBodyConsumer> empty = ImmutableMap.of();
    final Map<String, ResponseBodyConsumer> nonEmpty =
        ImmutableMap.of("1", mock(ResponseBodyConsumer.class));
    final HttpAuth auth = mock(HttpAuth.class);

    return new Object[][] { {null, empty}, {null, empty}, {auth, empty}, {null, nonEmpty},
        {auth, nonEmpty}};
  }

  @Test
  @UseDataProvider("provideClientData")
  public void provideClient(final HttpAuth authentication,
      final Map<String, ResponseBodyConsumer> consumers) {
    new ClientModule(new ClientConfig()).provideClient(authentication, consumers);
  }
}
