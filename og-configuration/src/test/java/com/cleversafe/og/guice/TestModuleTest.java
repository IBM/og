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
// Date: Jul 22, 2014
// ---------------------

package com.cleversafe.og.guice;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import com.cleversafe.og.api.Body;
import com.cleversafe.og.api.Data;
import com.cleversafe.og.http.Api;
import com.cleversafe.og.http.BasicAuth;
import com.cleversafe.og.http.HttpAuth;
import com.cleversafe.og.http.Scheme;
import com.cleversafe.og.json.AuthType;
import com.cleversafe.og.json.AuthenticationConfig;
import com.cleversafe.og.json.ConcurrencyConfig;
import com.cleversafe.og.json.ConcurrencyType;
import com.cleversafe.og.json.DistributionType;
import com.cleversafe.og.json.FilesizeConfig;
import com.cleversafe.og.json.HostConfig;
import com.cleversafe.og.json.ObjectManagerConfig;
import com.cleversafe.og.json.OperationConfig;
import com.cleversafe.og.json.SelectionType;
import com.cleversafe.og.json.OGConfig;
import com.cleversafe.og.s3.AWSAuthV2;
import com.cleversafe.og.scheduling.ConcurrentRequestScheduler;
import com.cleversafe.og.scheduling.RequestRateScheduler;
import com.cleversafe.og.scheduling.Scheduler;
import com.cleversafe.og.supplier.Suppliers;
import com.cleversafe.og.util.SizeUnit;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import com.google.common.io.Files;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;

@RunWith(DataProviderRunner.class)
public class TestModuleTest {
  @Rule
  public ExpectedException thrown = ExpectedException.none();
  private OGConfig config;
  private TestModule module;
  private Supplier<String> id;

  @Before
  public void before() {
    this.config = mock(OGConfig.class);
    this.module = new TestModule(this.config);
    this.id = Suppliers.of("1");
  }

  @Test(expected = NullPointerException.class)
  public void nullTestConfig() {
    new TestModule(null);
  }

  @Test
  public void provideIdSupplier() {
    assertThat(this.module.provideIdSupplier(), notNullValue());
  }

  @Test(expected = NullPointerException.class)
  public void provideSchemeNullScheme() {
    this.module.provideScheme();
  }

  @Test
  public void provideScheme() {
    when(this.config.getScheme()).thenReturn(Scheme.HTTP);
    assertThat(this.module.provideScheme().get(), is(Scheme.HTTP));
  }

  @DataProvider
  public static Object[][] provideInvalidProvideHost() {
    final SelectionType selection = SelectionType.RANDOM;
    final List<HostConfig> nullElement = Lists.newArrayList();
    nullElement.add(null);

    return new Object[][] { {null, ImmutableList.of(new HostConfig()), NullPointerException.class},
        {selection, null, NullPointerException.class},
        {selection, ImmutableList.<HostConfig>of(), IllegalArgumentException.class},
        {selection, nullElement, NullPointerException.class},
        {selection, ImmutableList.of(new HostConfig("")), IllegalArgumentException.class},};
  }

  @Test
  @UseDataProvider("provideInvalidProvideHost")
  public void invalidprovideHost(final SelectionType selection, final List<HostConfig> hostConfig,
      final Class<Exception> expectedException) {
    when(this.config.getHostSelection()).thenReturn(selection);
    when(this.config.getHost()).thenReturn(hostConfig);

    this.thrown.expect(expectedException);
    this.module.provideHost();
  }

  @Test
  public void provideHostSingleHostRoundRobin() {
    when(this.config.getHostSelection()).thenReturn(SelectionType.ROUNDROBIN);
    when(this.config.getHost()).thenReturn(ImmutableList.of(new HostConfig("192.168.8.1")));
    final Supplier<String> s = this.module.provideHost();
    for (int i = 0; i < 100; i++) {
      assertThat(s.get(), is("192.168.8.1"));
    }
  }

  @Test
  public void provideHostSingleHostRandom() {
    when(this.config.getHostSelection()).thenReturn(SelectionType.RANDOM);
    when(this.config.getHost()).thenReturn(ImmutableList.of(new HostConfig("192.168.8.1")));
    final Supplier<String> s = this.module.provideHost();
    for (int i = 0; i < 100; i++) {
      assertThat(s.get(), is("192.168.8.1"));
    }
  }

  @Test
  public void provideHostMultipleHostRoundRobin() {
    when(this.config.getHostSelection()).thenReturn(SelectionType.ROUNDROBIN);
    final List<HostConfig> hostConfig =
        ImmutableList.of(new HostConfig("192.168.8.1"), new HostConfig("192.168.8.2"));
    when(this.config.getHost()).thenReturn(hostConfig);
    final Supplier<String> s = this.module.provideHost();
    for (int i = 0; i < 100; i++) {
      assertThat(s.get(), is("192.168.8.1"));
      assertThat(s.get(), is("192.168.8.2"));
    }
  }

  @Test(expected = AssertionError.class)
  public void provideHostMultipleHostRandom() {
    when(this.config.getHostSelection()).thenReturn(SelectionType.RANDOM);
    final List<HostConfig> hostConfig =
        ImmutableList.of(new HostConfig("192.168.8.1"), new HostConfig("192.168.8.2"));
    when(this.config.getHost()).thenReturn(hostConfig);
    final Supplier<String> s = this.module.provideHost();
    for (int i = 0; i < 100; i++) {
      // Should not exhibit roundrobin behavior over a large sample, expect assertion error
      assertThat(s.get(), is("192.168.8.1"));
      assertThat(s.get(), is("192.168.8.2"));
    }
  }

  @Test(expected = NullPointerException.class)
  public void provideWriteHostNullOperationConfig() {
    when(this.config.getWrite()).thenReturn(null);
    this.module.provideWriteHost(Suppliers.of("192.168.8.1"));
  }

  @Test(expected = NullPointerException.class)
  public void provideWriteHostNullTestHost() {
    when(this.config.getWrite()).thenReturn(new OperationConfig());
    this.module.provideWriteHost(null);
  }

  @Test
  public void provideWriteHostDefault() {
    when(this.config.getWrite()).thenReturn(new OperationConfig());
    final Supplier<String> s = this.module.provideWriteHost(Suppliers.of("192.168.8.1"));
    assertThat(s.get(), is("192.168.8.1"));
  }

  @Test
  public void provideWriteHostOverride() {
    final OperationConfig operationConfig = mock(OperationConfig.class);
    final List<HostConfig> hostConfig = ImmutableList.of(new HostConfig("10.1.1.1"));
    when(operationConfig.getHostSelection()).thenReturn(SelectionType.RANDOM);
    when(operationConfig.getHost()).thenReturn(hostConfig);
    when(this.config.getWrite()).thenReturn(operationConfig);

    final Supplier<String> s = this.module.provideWriteHost(Suppliers.of("192.168.8.1"));
    assertThat(s.get(), is("10.1.1.1"));
  }

  @Test(expected = NullPointerException.class)
  public void provideReadHostNullOperationConfig() {
    when(this.config.getRead()).thenReturn(null);
    this.module.provideReadHost(Suppliers.of("192.168.8.1"));
  }

  @Test(expected = NullPointerException.class)
  public void provideReadHostNullTestHost() {
    when(this.config.getRead()).thenReturn(new OperationConfig());
    this.module.provideReadHost(null);
  }

  @Test
  public void provideReadHostDefault() {
    when(this.config.getRead()).thenReturn(new OperationConfig());
    final Supplier<String> s = this.module.provideReadHost(Suppliers.of("192.168.8.1"));
    assertThat(s.get(), is("192.168.8.1"));
  }

  @Test
  public void provideReadHostOverride() {
    final OperationConfig operationConfig = mock(OperationConfig.class);
    final List<HostConfig> hostConfig = ImmutableList.of(new HostConfig("10.1.1.1"));
    when(operationConfig.getHostSelection()).thenReturn(SelectionType.RANDOM);
    when(operationConfig.getHost()).thenReturn(hostConfig);
    when(this.config.getRead()).thenReturn(operationConfig);

    final Supplier<String> s = this.module.provideReadHost(Suppliers.of("192.168.8.1"));
    assertThat(s.get(), is("10.1.1.1"));
  }

  @Test(expected = NullPointerException.class)
  public void provideDeleteHostNullOperationConfig() {
    when(this.config.getDelete()).thenReturn(null);
    this.module.provideDeleteHost(Suppliers.of("192.168.8.1"));
  }

  @Test(expected = NullPointerException.class)
  public void provideDeleteHostNullTestHost() {
    when(this.config.getDelete()).thenReturn(new OperationConfig());
    this.module.provideDeleteHost(null);
  }

  @Test
  public void provideDeleteHostDefault() {
    when(this.config.getDelete()).thenReturn(new OperationConfig());
    final Supplier<String> s = this.module.provideDeleteHost(Suppliers.of("192.168.8.1"));
    assertThat(s.get(), is("192.168.8.1"));
  }

  @Test
  public void provideDeleteHostOverride() {
    final OperationConfig operationConfig = mock(OperationConfig.class);
    final List<HostConfig> hostConfig = ImmutableList.of(new HostConfig("10.1.1.1"));
    when(operationConfig.getHostSelection()).thenReturn(SelectionType.RANDOM);
    when(operationConfig.getHost()).thenReturn(hostConfig);
    when(this.config.getDelete()).thenReturn(operationConfig);

    final Supplier<String> s = this.module.provideDeleteHost(Suppliers.of("192.168.8.1"));
    assertThat(s.get(), is("10.1.1.1"));
  }

  @Test
  public void providePortNullPort() {
    when(this.config.getPort()).thenReturn(null);
    assertThat(this.module.providePort(), nullValue());
  }

  @Test
  public void providePort() {
    when(this.config.getPort()).thenReturn(80);
    assertThat(this.module.providePort().get(), is(80));
  }

  @Test(expected = NullPointerException.class)
  public void provideApiNullApi() {
    when(this.config.getApi()).thenReturn(null);
    this.module.provideApi();
  }

  @Test
  public void provideApi() {
    when(this.config.getApi()).thenReturn(Api.S3);
    assertThat(this.module.provideApi(), is(Api.S3));
  }

  @DataProvider
  public static Object[][] provideUriRoot() {
    final Api api = Api.S3;
    final String result = "foo";

    return new Object[][] { {null, api, "s3"}, {"foo", api, result}, {"/foo", api, result},
        {"foo/", api, result}, {"/foo/", api, result}, {"//foo///", api, result}};
  }

  @Test
  @UseDataProvider("provideUriRoot")
  public void provideUriRootNullUriRoot(final String uriRoot, final Api api, final String path) {
    when(this.config.getUriRoot()).thenReturn(uriRoot);
    when(this.config.getApi()).thenReturn(api);
    assertThat(this.module.provideUriRoot().get(), is(path));
  }

  @Test
  public void provideUriRootSlash() {
    when(this.config.getUriRoot()).thenReturn("/");
    when(this.config.getApi()).thenReturn(Api.S3);
    assertThat(this.module.provideUriRoot(), nullValue());
  }

  @Test(expected = NullPointerException.class)
  public void provideContainerNullContainer() {
    when(this.config.getContainer()).thenReturn(null);
    this.module.provideContainer();
  }

  @Test(expected = IllegalArgumentException.class)
  public void provideContainerEmptyContainer() {
    when(this.config.getContainer()).thenReturn("");
    this.module.provideContainer();
  }

  @Test
  public void provideContainer() {
    when(this.config.getContainer()).thenReturn("container");
    assertThat(this.module.provideContainer().get(), is("container"));
  }

  @Test
  public void provideUsernameNullUsername() {
    when(this.config.getAuthentication()).thenReturn(new AuthenticationConfig());
    assertThat(this.module.provideUsername(), nullValue());
  }

  @Test(expected = IllegalArgumentException.class)
  public void provideUsernameEmptyUsername() {
    final AuthenticationConfig authConfig = mock(AuthenticationConfig.class);
    when(authConfig.getUsername()).thenReturn("");
    when(this.config.getAuthentication()).thenReturn(authConfig);
    this.module.provideUsername();
  }

  @Test
  public void provideUsername() {
    final AuthenticationConfig authConfig = mock(AuthenticationConfig.class);
    when(authConfig.getUsername()).thenReturn("user");
    when(this.config.getAuthentication()).thenReturn(authConfig);
    assertThat(this.module.provideUsername().get(), is("user"));
  }

  @Test
  public void providePasswordNullPassword() {
    when(this.config.getAuthentication()).thenReturn(new AuthenticationConfig());
    assertThat(this.module.providePassword(), nullValue());
  }

  @Test(expected = IllegalArgumentException.class)
  public void providePasswordEmptyPassword() {
    final AuthenticationConfig authConfig = mock(AuthenticationConfig.class);
    when(authConfig.getPassword()).thenReturn("");
    when(this.config.getAuthentication()).thenReturn(authConfig);
    this.module.providePassword();
  }

  @Test
  public void providePassword() {
    final AuthenticationConfig authConfig = mock(AuthenticationConfig.class);
    when(authConfig.getPassword()).thenReturn("password");
    when(this.config.getAuthentication()).thenReturn(authConfig);
    assertThat(this.module.providePassword().get(), is("password"));
  }

  @DataProvider
  public static Object[][] provideInvalidAuthentication() {
    final Supplier<String> username = Suppliers.of("username");
    final Supplier<String> password = Suppliers.of("password");

    return new Object[][] { {null, username, password, NullPointerException.class},
        {AuthType.BASIC, null, password, IllegalArgumentException.class},
        {AuthType.BASIC, username, null, IllegalArgumentException.class},};
  }

  @Test
  @UseDataProvider("provideInvalidAuthentication")
  public void invalidProvideAuthentication(final AuthType authType,
      final Supplier<String> username, final Supplier<String> password,
      final Class<Exception> expectedException) {
    final AuthenticationConfig authConfig = mock(AuthenticationConfig.class);
    when(authConfig.getType()).thenReturn(authType);
    when(this.config.getAuthentication()).thenReturn(authConfig);

    this.thrown.expect(expectedException);
    this.module.provideAuthentication(username, password);
  }

  @Test
  public void provideAuthenticationNullBoth() {
    final AuthenticationConfig authConfig = mock(AuthenticationConfig.class);
    when(authConfig.getType()).thenReturn(AuthType.BASIC);
    when(this.config.getAuthentication()).thenReturn(authConfig);
    assertThat(this.module.provideAuthentication(null, null), nullValue());
  }

  @DataProvider
  public static Object[][] provideProvideAuthentication() {
    return new Object[][] { {AuthType.BASIC, BasicAuth.class}, {AuthType.AWSV2, AWSAuthV2.class}};
  }

  @Test
  @UseDataProvider("provideProvideAuthentication")
  public void provideAuthentication(final AuthType authType,
      final Class<? extends HttpAuth> authClass) {
    final AuthenticationConfig authConfig = mock(AuthenticationConfig.class);
    when(authConfig.getType()).thenReturn(authType);
    when(this.config.getAuthentication()).thenReturn(authConfig);

    final HttpAuth auth =
        this.module.provideAuthentication(Suppliers.of("username"), Suppliers.of("password"));

    assertThat(authClass.isInstance(auth), is(true));
  }

  @DataProvider
  public static Object[][] provideInvalidBody() {
    final SelectionType selection = SelectionType.ROUNDROBIN;
    final List<FilesizeConfig> config = ImmutableList.of(new FilesizeConfig());
    final Data data = Data.ZEROES;

    final FilesizeConfig poisson = mock(FilesizeConfig.class);
    when(poisson.getDistribution()).thenReturn(DistributionType.POISSON);
    when(poisson.getAverageUnit()).thenReturn(SizeUnit.MEBIBYTES);
    when(poisson.getSpreadUnit()).thenReturn(SizeUnit.MEBIBYTES);
    final List<FilesizeConfig> poissonConfig = ImmutableList.of(poisson);

    return new Object[][] { {null, config, data, NullPointerException.class},
        {selection, null, data, NullPointerException.class},
        {selection, ImmutableList.of(), data, IllegalArgumentException.class},
        {selection, config, null, NullPointerException.class},
        {selection, config, Data.NONE, IllegalArgumentException.class},
        {selection, poissonConfig, data, IllegalArgumentException.class}};
  }

  @Test
  @UseDataProvider("provideInvalidBody")
  public void invalidProvideBody(final SelectionType selection, final List<FilesizeConfig> config,
      final Data data, final Class<Exception> expectedException) {
    when(this.config.getFilesizeSelection()).thenReturn(selection);
    when(this.config.getFilesize()).thenReturn(config);
    when(this.config.getData()).thenReturn(data);

    this.thrown.expect(expectedException);
    this.module.provideBody();
  }

  @DataProvider
  public static Object[][] provideBodyData() {
    return new Object[][] { {SelectionType.ROUNDROBIN, Data.ZEROES, 10},
        {SelectionType.ROUNDROBIN, Data.RANDOM, 10}};
  }

  @Test
  @UseDataProvider("provideBodyData")
  public void provideBody(final SelectionType selection, final Data data, final long size) {
    when(this.config.getFilesizeSelection()).thenReturn(selection);
    when(this.config.getFilesize()).thenReturn(ImmutableList.of(new FilesizeConfig(size)));
    when(this.config.getData()).thenReturn(data);
    final Body body = this.module.provideBody().get();

    assertThat(body.getData(), is(data));
    assertThat(body.getSize(), is(SizeUnit.MEBIBYTES.toBytes(size)));
  }

  @DataProvider
  public static Object[][] provideSelection() {
    return new Object[][] { {SelectionType.ROUNDROBIN, null},
        {SelectionType.RANDOM, IllegalStateException.class}};
  }

  @Test
  @UseDataProvider("provideSelection")
  public void provideBodyMultipleFilesizeRoundRobin(final SelectionType selection,
      final Class<Exception> expectedException) {
    when(this.config.getFilesizeSelection()).thenReturn(selection);
    final List<FilesizeConfig> filesize =
        ImmutableList.of(new FilesizeConfig(10.0), new FilesizeConfig(25.0));
    when(this.config.getData()).thenReturn(Data.RANDOM);
    when(this.config.getFilesize()).thenReturn(filesize);
    final Supplier<Body> s = this.module.provideBody();

    // if selection is random, expect an assertion error in the below loop
    if (expectedException != null)
      this.thrown.expect(expectedException);

    for (int i = 0; i < 100; i++) {
      // TODO ugly hack to rethrow assertion error; defect in junit 4.11 which ignores
      // AssertionError in ExpectedException rule, expected to be fixed in 4.12. See
      // https://github.com/junit-team/junit/issues/687
      try {
        assertThat(s.get().getSize(), is(SizeUnit.MEBIBYTES.toBytes(10)));
        assertThat(s.get().getSize(), is(SizeUnit.MEBIBYTES.toBytes(25)));
      } catch (final AssertionError e) {
        throw new IllegalStateException(e);
      }
    }
  }

  @DataProvider
  public static Object[][] provideInvalideProvideObjectFile() throws IOException {
    return new Object[][] { {null, NullPointerException.class},
        {"", IllegalArgumentException.class},
        {File.createTempFile("existing", null).toString(), IllegalArgumentException.class},};
  }

  @Test
  @UseDataProvider("provideInvalideProvideObjectFile")
  public void invalidProvideObjectFileLocation(final String location,
      final Class<Exception> expectedException) throws IOException {
    final ObjectManagerConfig objectManagerConfig = mock(ObjectManagerConfig.class);
    when(objectManagerConfig.getObjectFileLocation()).thenReturn(location);
    when(this.config.getObjectManager()).thenReturn(objectManagerConfig);

    this.thrown.expect(expectedException);
    this.module.provideObjectFileLocation();
  }

  @Test
  public void provideObjectFileLocationNonExisting() throws IOException {
    final ObjectManagerConfig objectManagerConfig = mock(ObjectManagerConfig.class);
    final File existing = Files.createTempDir();
    final File nonExisting = new File(existing, String.valueOf(System.nanoTime()));
    when(objectManagerConfig.getObjectFileLocation()).thenReturn(nonExisting.toString());
    when(this.config.getObjectManager()).thenReturn(objectManagerConfig);

    this.module.provideObjectFileLocation();
  }

  @Test(expected = NullPointerException.class)
  public void provideObjectFileNameNullContainer() {
    this.module.provideObjectFileName(null, Api.S3);
  }

  @Test(expected = NullPointerException.class)
  public void provideObjectFileNameNullApi() {
    this.module.provideObjectFileName(Suppliers.of("container"), null);
  }

  @DataProvider
  public static Object[][] provideObjectFileNameData() {
    final Api api = Api.S3;
    final String container = "container";
    final String defaultName = String.format("%s-%s", container, api.toString().toLowerCase());

    return new Object[][] { {api, container, null, defaultName}, {api, container, "", defaultName},
        {api, container, "objectFile", "objectFile"}};
  }

  @Test
  @UseDataProvider("provideObjectFileNameData")
  public void provideObjectFileName(final Api api, final String container, final String prefix,
      final String expected) {
    final ObjectManagerConfig objectManagerConfig = mock(ObjectManagerConfig.class);
    when(objectManagerConfig.getObjectFileName()).thenReturn(prefix);
    when(this.config.getObjectManager()).thenReturn(objectManagerConfig);
    final String name = this.module.provideObjectFileName(Suppliers.of(container), api);

    assertThat(name, is(expected));
  }

  @DataProvider
  public static Object[][] provideWriteWeightData() {
    return new Object[][] { {-1.0, 50.0, 50.0, -1.0, IllegalArgumentException.class},
        {101.0, 0.0, 0.0, 101.0, IllegalArgumentException.class}, {0.0, 50.0, 50.0, 0.0, null},
        {50.0, 25.0, 25.0, 50.0, null}, {0.0, 0.0, 0.0, 100.0, null}};
  }

  @Test
  @UseDataProvider("provideWriteWeightData")
  public void provideWriteWeight(final double write, final double read, final double delete,
      final double expectedWrite, final Class<Exception> expectedException) {
    when(this.config.getWrite()).thenReturn(new OperationConfig(write));
    if (expectedException != null)
      this.thrown.expect(expectedException);

    this.module.provideWriteWeight(read, delete);
    assertThat(this.module.provideWriteWeight(read, delete), is(expectedWrite));
  }

  @DataProvider
  public static Object[][] provideWeightData() {
    return new Object[][] { {-1.0, IllegalArgumentException.class}, {0.0, null}, {50.0, null},
        {101.0, IllegalArgumentException.class}};
  }

  @Test
  @UseDataProvider("provideWeightData")
  public void provideReadWeight(final double read, final Class<Exception> expectedException) {
    when(this.config.getRead()).thenReturn(new OperationConfig(read));
    if (expectedException != null)
      this.thrown.expect(expectedException);

    assertThat(this.module.provideReadWeight(), is(read));
  }

  @Test
  @UseDataProvider("provideWeightData")
  public void provideDeleteWeight(final double delete, final Class<Exception> expectedException) {
    when(this.config.getDelete()).thenReturn(new OperationConfig(delete));
    if (expectedException != null)
      this.thrown.expect(expectedException);

    assertThat(this.module.provideDeleteWeight(), is(delete));
  }

  @Test(expected = NullPointerException.class)
  public void provideSchedulerNullEventBus() {
    this.module.provideScheduler(null);
  }

  @Test(expected = NullPointerException.class)
  public void provideSchedulerNullConcurrencyConfig() {
    when(this.config.getConcurrency()).thenReturn(null);
    this.module.provideScheduler(new EventBus());
  }

  @DataProvider
  public static Object[][] provideInvalidProvideScheduler() {
    return new Object[][] { {null, DistributionType.UNIFORM, NullPointerException.class},
        {ConcurrencyType.THREADS, null, NullPointerException.class},
        {ConcurrencyType.THREADS, DistributionType.LOGNORMAL, IllegalArgumentException.class}};
  }

  @Test
  @UseDataProvider("provideInvalidProvideScheduler")
  public void invalidProvideScheduler(final ConcurrencyType type,
      final DistributionType distribution, final Class<Exception> expectedException) {
    final ConcurrencyConfig concurrencyConfig = mock(ConcurrencyConfig.class);
    when(concurrencyConfig.getType()).thenReturn(type);
    when(concurrencyConfig.getDistribution()).thenReturn(distribution);
    when(this.config.getConcurrency()).thenReturn(concurrencyConfig);

    this.thrown.expect(expectedException);
    this.module.provideScheduler(new EventBus());
  }

  @DataProvider
  public static Object[][] provideSchedulerData() {
    return new Object[][] {
        {ConcurrencyType.THREADS, DistributionType.UNIFORM, ConcurrentRequestScheduler.class},
        {ConcurrencyType.OPS, DistributionType.UNIFORM, RequestRateScheduler.class}};
  }

  @Test
  @UseDataProvider("provideSchedulerData")
  public void provideScheduler(final ConcurrencyType type, final DistributionType distribution,
      final Class<Scheduler> expected) {
    final ConcurrencyConfig concurrencyConfig = mock(ConcurrencyConfig.class);
    when(concurrencyConfig.getType()).thenReturn(type);
    when(concurrencyConfig.getDistribution()).thenReturn(distribution);
    when(concurrencyConfig.getCount()).thenReturn(1.0);
    when(concurrencyConfig.getUnit()).thenReturn(TimeUnit.SECONDS);
    when(concurrencyConfig.getRampup()).thenReturn(0.0);
    when(concurrencyConfig.getRampupUnit()).thenReturn(TimeUnit.SECONDS);
    when(this.config.getConcurrency()).thenReturn(concurrencyConfig);

    final Scheduler scheduler = this.module.provideScheduler(new EventBus());
    assertThat(expected.isInstance(scheduler), is(true));
  }
}
