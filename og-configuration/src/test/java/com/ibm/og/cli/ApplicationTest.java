/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.cli;

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

import com.ibm.og.cli.Application.Cli;
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
  private static final String VALUE = "value";
  private Gson gson;

  @Before
  public void before() {
    this.gson = new GsonBuilder().create();
  }

  @DataProvider
  public static Object[][] provideInvalidCli() {
    final String app = "application";
    final String[] args = new String[] {};


    return new Object[][] {{null, args, NullPointerException.class},
        {app, null, NullPointerException.class}};

  }

  @Test
  @UseDataProvider("provideInvalidCli")
  public void invalidCli(final String name, final String[] args,
      final Class<Exception> expectedException) {
    this.thrown.expect(expectedException);
    final GetOpt getopt = new GetOpt();
    Application.cli(name, getopt, args);
  }

  @DataProvider
  public static Object[][] provideCli() {

    return new Object[][] {
            // args, shouldStop, error, help, version
            {new String[] {}, false, false, false, false},
            {new String[] {"-h"}, true, false, true, false},
            {new String[] {"-v"}, true, false, false, true}};
  }


  @Test
  @UseDataProvider("provideCli")
  public void cli(final String[] args, final boolean shouldStop, final boolean error,
                  final boolean help, final boolean version) {
    final GetOpt getopt = new GetOpt();
    final Cli cli = Application.cli("application", getopt, args);
    assertThat(cli.shouldStop(), is(shouldStop));
    assertThat(cli.error(), is(error));
    assertThat(cli.help(), is(help));
    assertThat(cli.version(), is(version));

    // not a good way to validate these so just call them and make sure they don't throw
    cli.printUsage();
    cli.printErrors();
    cli.printVersion();
  }

  @DataProvider
  public static Object[][] provideOGCli() {
    return new Object[][] {
            // args, shouldStop, error, help, version
            {new String[] {}, true, true, false, false},
            {new String[] {"og-file.json"}, false, false, false, false}};
  }



  @Test
  @UseDataProvider("provideOGCli")
  public void ogcli(final String[] args, final boolean shouldStop, final boolean error,
                  final boolean help, final boolean version) {

    final GetOpt getopt = new OGGetOpt();
    final Cli cli = Application.cli("og", getopt, args);
    assertThat(cli.shouldStop(), is(shouldStop));
    assertThat(cli.error(), is(error));
    assertThat(cli.help(), is(help));
    assertThat(cli.version(), is(version));

    // not a good way to validate these so just call them and make sure they don't throw
    cli.printUsage();
    cli.printErrors();
    cli.printVersion();
  }

  @DataProvider
  public static Object[][] provideObjectFileCli() {
    return new Object[][] {
            // args, shouldStop, error, help, version
            {new String[] {}, false, false, false, false},
            {new String[] {"input-file"}, false, false, false, false},
            {new String[] {"--help"}, true, false, true, false},
            {new String[] {"--version"}, true, false, false, true}};
  }



  @Test
  @UseDataProvider("provideObjectFileCli")
  public void objectfilecli(final String[] args, final boolean shouldStop, final boolean error,
                    final boolean help, final boolean version) {
    final ObjectFileGetOpt getopt = new ObjectFileGetOpt();
    final Cli cli = Application.cli("object-file", getopt, args);
    assertThat(cli.shouldStop(), is(shouldStop));
    assertThat(cli.error(), is(error));
    assertThat(cli.help(), is(help));
    assertThat(cli.version(), is(version));

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
    final File json = new File("nonExistent");
    final Gson gson = new GsonBuilder().create();

    return new Object[][] {{null, Item.class, gson, NullPointerException.class},
        {json, null, gson, NullPointerException.class},
        {json, Item.class, null, NullPointerException.class},
        {json, Item.class, gson, FileNotFoundException.class}};
  }

  static class Item {
    public String key;
  }

  @Test
  @UseDataProvider("provideInvalidFromJson")
  public void invalidFromJson(final File json, final Class<?> cls, final Gson gson,
      final Class<Exception> expectedException) throws FileNotFoundException {
    this.thrown.expect(expectedException);
    Application.fromJson(json, cls, gson);
  }

  @Test
  public void fromJson() throws FileNotFoundException {
    final File json = new File(Application.getResource(APPLICATION_JSON));
    final Item item = Application.fromJson(json, Item.class, this.gson);
    assertThat(item.key, is(VALUE));
  }
}
