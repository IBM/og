/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.io.File;
import java.io.FileNotFoundException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import com.cleversafe.og.cli.Application.Cli;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;

@RunWith(DataProviderRunner.class)
public class ApplicationTest {
  @Rule
  public ExpectedException thrown = ExpectedException.none();
  private static final String APPLICATION_JSAP = "application.jsap";
  private static final String APPLICATION_JSON = "application.json";
  private static final String DEFAULT_APPLICATION_JSON = "defaultApplication.json";
  private static final String VALUE = "value";
  private static final String DEFAULT_VALUE = "defaultValue";
  private String defaultJson;
  private Gson gson;

  @Before
  public void before() {
    this.defaultJson = DEFAULT_APPLICATION_JSON;
    this.gson = new GsonBuilder().create();
  }

  @DataProvider
  public static Object[][] provideInvalidCli() {
    final String app = "application";
    final String jsap = APPLICATION_JSAP;
    final String[] args = new String[] {};

    return new Object[][] {{null, jsap, args, NullPointerException.class},
        {app, null, args, NullPointerException.class},
        {app, jsap, null, NullPointerException.class},
        {app, "nonexistent.jsap", args, IllegalArgumentException.class}};
  }

  @Test
  @UseDataProvider("provideInvalidCli")
  public void invalidCli(final String name, final String jsapResourceName, final String[] args,
      final Class<Exception> expectedException) {
    this.thrown.expect(expectedException);
    Application.cli(name, jsapResourceName, args);
  }

  @DataProvider
  public static Object[][] provideCli() {
    return new Object[][] {
        // args, shouldStop, error, help, version
        {new String[] {}, false, false, false, false},
        {new String[] {"--nonexistent"}, true, true, false, false},
        {new String[] {"-h"}, true, false, true, false},
        {new String[] {"-v"}, true, false, false, true}};
  }

  @Test
  @UseDataProvider("provideCli")
  public void cli(final String[] args, final boolean shouldStop, final boolean error,
      final boolean help, final boolean version) {
    final Cli cli = Application.cli("application", APPLICATION_JSAP, args);
    assertThat(cli.shouldStop(), is(shouldStop));
    assertThat(cli.error(), is(error));
    assertThat(cli.help(), is(help));
    assertThat(cli.version(), is(version));
    assertThat(cli.flags(), notNullValue());

    // not a good way to validate these so just call them and make sure they don't throw
    cli.printUsage();
    cli.printErrors();
    cli.printVersion();
  }

  @Test(expected = NullPointerException.class)
  public void getResourceNullResource() {
    Application.getResource(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void getResourceMissingResource() {
    Application.getResource("nonexistent.jsap");
  }

  @Test
  public void getResource() {
    assertThat(Application.getResource(APPLICATION_JSAP), notNullValue());
  }

  @DataProvider
  public static Object[][] provideInvalidFromJson() {
    final String defaultJson = DEFAULT_APPLICATION_JSON;
    final String nonExistent = "nonexistent";
    final Gson gson = new GsonBuilder().create();

    return new Object[][] {{null, null, Item.class, gson, NullPointerException.class},
        {null, defaultJson, null, gson, NullPointerException.class},
        {null, defaultJson, Item.class, null, NullPointerException.class},
        {null, nonExistent, Item.class, gson, IllegalArgumentException.class},
        {new File(nonExistent), defaultJson, Item.class, gson, FileNotFoundException.class}};
  }

  static class Item {
    public String key;
  }

  @Test
  @UseDataProvider("provideInvalidFromJson")
  public void invalidFromJson(final File userJson, final String defaultJson, final Class<?> cls,
      final Gson gson, final Class<Exception> expectedException) throws FileNotFoundException {
    this.thrown.expect(expectedException);
    Application.fromJson(userJson, defaultJson, cls, gson);
  }

  @Test
  public void fromJsonDefault() throws FileNotFoundException {
    final Item item = Application.fromJson(null, this.defaultJson, Item.class, this.gson);
    assertThat(item.key, is(DEFAULT_VALUE));
  }

  @Test
  public void fromJson() throws FileNotFoundException {
    final File json = new File(Application.getResource(APPLICATION_JSON));
    final Item item = Application.fromJson(json, this.defaultJson, Item.class, this.gson);
    assertThat(item.key, is(VALUE));
  }
}
