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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.cleversafe.og.http.Api;
import com.cleversafe.og.http.BasicAuth;
import com.cleversafe.og.http.HttpAuth;
import com.cleversafe.og.http.Scheme;
import com.cleversafe.og.json.AuthType;
import com.cleversafe.og.json.AuthenticationConfig;
import com.cleversafe.og.json.CollectionAlgorithmType;
import com.cleversafe.og.json.ConcurrencyConfig;
import com.cleversafe.og.json.ConcurrencyType;
import com.cleversafe.og.json.DistributionType;
import com.cleversafe.og.json.FilesizeConfig;
import com.cleversafe.og.json.HostConfig;
import com.cleversafe.og.json.ObjectManagerConfig;
import com.cleversafe.og.json.OperationConfig;
import com.cleversafe.og.json.TestConfig;
import com.cleversafe.og.operation.Entity;
import com.cleversafe.og.operation.EntityType;
import com.cleversafe.og.producer.Producer;
import com.cleversafe.og.producer.Producers;
import com.cleversafe.og.s3.AWSAuthV2;
import com.cleversafe.og.scheduling.ConcurrentRequestScheduler;
import com.cleversafe.og.scheduling.RequestRateScheduler;
import com.cleversafe.og.scheduling.Scheduler;
import com.cleversafe.og.util.SizeUnit;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.google.common.io.Files;

public class TestModuleTest
{
   private static final double ERR = Math.pow(0.1, 6);
   private TestConfig config;
   private TestModule module;

   @Before
   public void before()
   {
      this.config = mock(TestConfig.class);
      this.module = new TestModule(this.config);
   }

   @Test(expected = NullPointerException.class)
   public void testNullTestConfig()
   {
      new TestModule(null);
   }

   @Test
   public void testProvideIdProducer()
   {
      Assert.assertNotNull(this.module.provideIdProducer());
   }

   @Test(expected = NullPointerException.class)
   public void testProvideSchemeNullScheme()
   {
      this.module.provideScheme();
   }

   @Test
   public void testProvideScheme()
   {
      when(this.config.getScheme()).thenReturn(Scheme.HTTP);
      final Producer<Scheme> p = this.module.provideScheme();
      Assert.assertEquals(Scheme.HTTP, p.produce());
   }

   @Test(expected = NullPointerException.class)
   public void testProvideHostNullHostSelection()
   {
      when(this.config.getHostSelection()).thenReturn(null);
      final List<HostConfig> hostConfig = Lists.newArrayList();
      hostConfig.add(new HostConfig());
      when(this.config.getHost()).thenReturn(hostConfig);
      this.module.provideHost();
   }

   @Test(expected = NullPointerException.class)
   public void testProvideHostNullHost()
   {
      when(this.config.getHostSelection()).thenReturn(CollectionAlgorithmType.RANDOM);
      when(this.config.getHost()).thenReturn(null);
      this.module.provideHost();
   }

   @Test(expected = IllegalArgumentException.class)
   public void testProvideHostEmptyHost()
   {
      when(this.config.getHostSelection()).thenReturn(CollectionAlgorithmType.RANDOM);
      final List<HostConfig> hostConfig = Lists.newArrayList();
      when(this.config.getHost()).thenReturn(hostConfig);
      this.module.provideHost();
   }

   @Test(expected = NullPointerException.class)
   public void testProvideHostNullHostConfig()
   {
      when(this.config.getHostSelection()).thenReturn(CollectionAlgorithmType.RANDOM);
      final List<HostConfig> hostConfig = Lists.newArrayList();
      hostConfig.add(null);
      when(this.config.getHost()).thenReturn(hostConfig);
      this.module.provideHost();
   }

   @Test(expected = IllegalArgumentException.class)
   public void testProvideHostSingleHostEmptyHost()
   {
      when(this.config.getHostSelection()).thenReturn(CollectionAlgorithmType.ROUNDROBIN);
      final List<HostConfig> hostConfig = Lists.newArrayList();
      hostConfig.add(new HostConfig(""));
      when(this.config.getHost()).thenReturn(hostConfig);
      this.module.provideHost();
   }

   @Test
   public void testProvideHostSingleHostRoundRobin()
   {
      when(this.config.getHostSelection()).thenReturn(CollectionAlgorithmType.ROUNDROBIN);
      final List<HostConfig> hostConfig = Lists.newArrayList();
      hostConfig.add(new HostConfig("192.168.8.1"));
      when(this.config.getHost()).thenReturn(hostConfig);
      final Producer<String> p = this.module.provideHost();
      for (int i = 0; i < 100; i++)
      {
         Assert.assertEquals("192.168.8.1", p.produce());
      }
   }

   @Test
   public void testProvideHostSingleHostRandom()
   {
      when(this.config.getHostSelection()).thenReturn(CollectionAlgorithmType.RANDOM);
      final List<HostConfig> hostConfig = Lists.newArrayList();
      hostConfig.add(new HostConfig("192.168.8.1"));
      when(this.config.getHost()).thenReturn(hostConfig);
      final Producer<String> p = this.module.provideHost();
      for (int i = 0; i < 100; i++)
      {
         Assert.assertEquals("192.168.8.1", p.produce());
      }
   }

   @Test
   public void testProvideHostMultipleHostRoundRobin()
   {
      when(this.config.getHostSelection()).thenReturn(CollectionAlgorithmType.ROUNDROBIN);
      final List<HostConfig> hostConfig = Lists.newArrayList();
      hostConfig.add(new HostConfig("192.168.8.1"));
      hostConfig.add(new HostConfig("192.168.8.2"));
      when(this.config.getHost()).thenReturn(hostConfig);
      final Producer<String> p = this.module.provideHost();
      for (int i = 0; i < 100; i++)
      {
         Assert.assertEquals("192.168.8.1", p.produce());
         Assert.assertEquals("192.168.8.2", p.produce());
      }
   }

   @Test(expected = AssertionError.class)
   public void testProvideHostMultipleHostRandom()
   {
      when(this.config.getHostSelection()).thenReturn(CollectionAlgorithmType.RANDOM);
      final List<HostConfig> hostConfig = Lists.newArrayList();
      hostConfig.add(new HostConfig("192.168.8.1"));
      when(this.config.getHost()).thenReturn(hostConfig);
      final Producer<String> p = this.module.provideHost();
      for (int i = 0; i < 100; i++)
      {
         // Should not exhibit roundrobin behavior over a large sample, expect assertion error
         Assert.assertEquals("192.168.8.1", p.produce());
         Assert.assertEquals("192.168.8.2", p.produce());
      }
   }

   @Test(expected = NullPointerException.class)
   public void testProvideWriteHostNullOperationConfig()
   {
      when(this.config.getWrite()).thenReturn(null);
      this.module.provideWriteHost(Producers.of("192.168.8.1"));
   }

   @Test(expected = NullPointerException.class)
   public void testProvideWriteHostNullTestHost()
   {
      when(this.config.getWrite()).thenReturn(new OperationConfig());
      this.module.provideWriteHost(null);
   }

   @Test
   public void testProvideWriteHostDefault()
   {
      when(this.config.getWrite()).thenReturn(new OperationConfig());
      final Producer<String> p = this.module.provideWriteHost(Producers.of("192.168.8.1"));
      Assert.assertEquals("192.168.8.1", p.produce());
   }

   @Test
   public void testProvideWriteHostOverride()
   {
      final OperationConfig operationConfig = mock(OperationConfig.class);
      final List<HostConfig> host = Lists.newArrayList();
      host.add(new HostConfig("10.1.1.1"));
      when(operationConfig.getHostSelection()).thenReturn(CollectionAlgorithmType.RANDOM);
      when(operationConfig.getHost()).thenReturn(host);
      when(this.config.getWrite()).thenReturn(operationConfig);

      final Producer<String> p = this.module.provideWriteHost(Producers.of("192.168.8.1"));
      Assert.assertEquals("10.1.1.1", p.produce());
   }

   @Test(expected = NullPointerException.class)
   public void testProvideReadHostNullOperationConfig()
   {
      when(this.config.getRead()).thenReturn(null);
      this.module.provideReadHost(Producers.of("192.168.8.1"));
   }

   @Test(expected = NullPointerException.class)
   public void testProvideReadHostNullTestHost()
   {
      when(this.config.getRead()).thenReturn(new OperationConfig());
      this.module.provideReadHost(null);
   }

   @Test
   public void testProvideReadHostDefault()
   {
      when(this.config.getRead()).thenReturn(new OperationConfig());
      final Producer<String> p = this.module.provideReadHost(Producers.of("192.168.8.1"));
      Assert.assertEquals("192.168.8.1", p.produce());
   }

   @Test
   public void testProvideReadHostOverride()
   {
      final OperationConfig operationConfig = mock(OperationConfig.class);
      final List<HostConfig> host = Lists.newArrayList();
      host.add(new HostConfig("10.1.1.1"));
      when(operationConfig.getHostSelection()).thenReturn(CollectionAlgorithmType.RANDOM);
      when(operationConfig.getHost()).thenReturn(host);
      when(this.config.getRead()).thenReturn(operationConfig);

      final Producer<String> p = this.module.provideReadHost(Producers.of("192.168.8.1"));
      Assert.assertEquals("10.1.1.1", p.produce());
   }

   @Test(expected = NullPointerException.class)
   public void testProvideDeleteHostNullOperationConfig()
   {
      when(this.config.getDelete()).thenReturn(null);
      this.module.provideDeleteHost(Producers.of("192.168.8.1"));
   }

   @Test(expected = NullPointerException.class)
   public void testProvideDeleteHostNullTestHost()
   {
      when(this.config.getDelete()).thenReturn(new OperationConfig());
      this.module.provideDeleteHost(null);
   }

   @Test
   public void testProvideDeleteHostDefault()
   {
      when(this.config.getDelete()).thenReturn(new OperationConfig());
      final Producer<String> p = this.module.provideDeleteHost(Producers.of("192.168.8.1"));
      Assert.assertEquals("192.168.8.1", p.produce());
   }

   @Test
   public void testProvideDeleteHostOverride()
   {
      final OperationConfig operationConfig = mock(OperationConfig.class);
      final List<HostConfig> host = Lists.newArrayList();
      host.add(new HostConfig("10.1.1.1"));
      when(operationConfig.getHostSelection()).thenReturn(CollectionAlgorithmType.RANDOM);
      when(operationConfig.getHost()).thenReturn(host);
      when(this.config.getDelete()).thenReturn(operationConfig);

      final Producer<String> p = this.module.provideDeleteHost(Producers.of("192.168.8.1"));
      Assert.assertEquals("10.1.1.1", p.produce());
   }

   @Test
   public void testProvidePortAbsentPort()
   {
      when(this.config.getPort()).thenReturn(null);
      final Optional<Producer<Integer>> p = this.module.providePort();
      Assert.assertTrue(!p.isPresent());
   }

   @Test
   public void testProvidePort()
   {
      when(this.config.getPort()).thenReturn(80);
      final Optional<Producer<Integer>> p = this.module.providePort();
      Assert.assertTrue(p.isPresent());
      Assert.assertEquals(Integer.valueOf(80), p.get().produce());
   }

   @Test(expected = NullPointerException.class)
   public void testProvideApiNullApi()
   {
      when(this.config.getApi()).thenReturn(null);
      this.module.provideApi();
   }

   @Test
   public void testProvideApi()
   {
      when(this.config.getApi()).thenReturn(Api.S3);
      final Api api = this.module.provideApi();
      Assert.assertEquals(Api.S3, api);
   }

   @Test
   public void testProvideUriRootNullUriRoot()
   {
      when(this.config.getUriRoot()).thenReturn(null);
      when(this.config.getApi()).thenReturn(Api.S3);
      final Optional<Producer<String>> p = this.module.provideUriRoot();
      Assert.assertEquals("s3", p.get().produce());
   }

   @Test
   public void testProvideUriRootSlash()
   {
      when(this.config.getUriRoot()).thenReturn("/");
      when(this.config.getApi()).thenReturn(Api.S3);
      Assert.assertTrue(!this.module.provideUriRoot().isPresent());
   }

   @Test
   public void testProvideUriRootCustomRoot()
   {
      when(this.config.getUriRoot()).thenReturn("foo");
      when(this.config.getApi()).thenReturn(Api.S3);
      final Optional<Producer<String>> p = this.module.provideUriRoot();
      Assert.assertEquals("foo", p.get().produce());
   }

   @Test
   public void testProvideUriRootCustomRoot2()
   {
      when(this.config.getUriRoot()).thenReturn("/foo");
      when(this.config.getApi()).thenReturn(Api.S3);
      final Optional<Producer<String>> p = this.module.provideUriRoot();
      Assert.assertEquals("foo", p.get().produce());
   }

   @Test
   public void testProvideUriRootCustomRoot3()
   {
      when(this.config.getUriRoot()).thenReturn("foo/");
      when(this.config.getApi()).thenReturn(Api.S3);
      final Optional<Producer<String>> p = this.module.provideUriRoot();
      Assert.assertEquals("foo", p.get().produce());
   }

   @Test
   public void testProvideUriRootCustomRoot4()
   {
      when(this.config.getUriRoot()).thenReturn("/foo/");
      when(this.config.getApi()).thenReturn(Api.S3);
      final Optional<Producer<String>> p = this.module.provideUriRoot();
      Assert.assertEquals("foo", p.get().produce());
   }

   @Test
   public void testProvideUriRootCustomRoot5()
   {
      when(this.config.getUriRoot()).thenReturn("//foo///");
      when(this.config.getApi()).thenReturn(Api.S3);
      final Optional<Producer<String>> p = this.module.provideUriRoot();
      Assert.assertEquals("foo", p.get().produce());
   }

   @Test(expected = NullPointerException.class)
   public void testProvideContainerNullContainer()
   {
      when(this.config.getContainer()).thenReturn(null);
      this.module.provideContainer();
   }

   @Test(expected = IllegalArgumentException.class)
   public void testProvideContainerEmptyContainer()
   {
      when(this.config.getContainer()).thenReturn("");
      this.module.provideContainer();
   }

   @Test
   public void testProvideContainer()
   {
      when(this.config.getContainer()).thenReturn("container");
      final Producer<String> p = this.module.provideContainer();
      Assert.assertEquals("container", p.produce());
   }

   @Test
   public void testProvideUsernameNullUsername()
   {
      when(this.config.getAuthentication()).thenReturn(new AuthenticationConfig());
      Assert.assertTrue(!this.module.provideUsername().isPresent());
   }

   @Test(expected = IllegalArgumentException.class)
   public void testProvideUsernameEmptyUsername()
   {
      final AuthenticationConfig authConfig = mock(AuthenticationConfig.class);
      when(authConfig.getUsername()).thenReturn("");
      when(this.config.getAuthentication()).thenReturn(authConfig);
      this.module.provideUsername();
   }

   @Test
   public void testProvideUsername()
   {
      final AuthenticationConfig authConfig = mock(AuthenticationConfig.class);
      when(authConfig.getUsername()).thenReturn("user");
      when(this.config.getAuthentication()).thenReturn(authConfig);
      final Optional<Producer<String>> p = this.module.provideUsername();
      Assert.assertTrue(p.isPresent());
      Assert.assertEquals("user", p.get().produce());
   }

   @Test
   public void testProvidePasswordNullPassword()
   {
      when(this.config.getAuthentication()).thenReturn(new AuthenticationConfig());
      Assert.assertTrue(!this.module.providePassword().isPresent());
   }

   @Test(expected = IllegalArgumentException.class)
   public void testProvidePasswordEmptyPassword()
   {
      final AuthenticationConfig authConfig = mock(AuthenticationConfig.class);
      when(authConfig.getPassword()).thenReturn("");
      when(this.config.getAuthentication()).thenReturn(authConfig);
      this.module.providePassword();
   }

   @Test
   public void testProvidePassword()
   {
      final AuthenticationConfig authConfig = mock(AuthenticationConfig.class);
      when(authConfig.getPassword()).thenReturn("password");
      when(this.config.getAuthentication()).thenReturn(authConfig);
      final Optional<Producer<String>> p = this.module.providePassword();
      Assert.assertTrue(p.isPresent());
      Assert.assertEquals("password", p.get().produce());
   }

   @Test(expected = NullPointerException.class)
   public void testProvideAuthenticationNullAuthType()
   {
      final AuthenticationConfig authConfig = mock(AuthenticationConfig.class);
      when(authConfig.getType()).thenReturn(null);
      when(this.config.getAuthentication()).thenReturn(authConfig);
      this.module.provideAuthentication(Optional.of(Producers.of("username")),
            Optional.of(Producers.of("password")));
   }

   @Test(expected = IllegalArgumentException.class)
   public void testProvideAuthenticationAbsentUsername()
   {
      final AuthenticationConfig authConfig = mock(AuthenticationConfig.class);
      when(authConfig.getType()).thenReturn(AuthType.BASIC);
      when(this.config.getAuthentication()).thenReturn(authConfig);
      this.module.provideAuthentication(Optional.fromNullable((Producer<String>) null),
            Optional.of(Producers.of("password")));
   }

   @Test(expected = IllegalArgumentException.class)
   public void testProvideAuthenticationAbsentPassword()
   {
      final AuthenticationConfig authConfig = mock(AuthenticationConfig.class);
      when(authConfig.getType()).thenReturn(AuthType.BASIC);
      when(this.config.getAuthentication()).thenReturn(authConfig);
      this.module.provideAuthentication(Optional.of(Producers.of("username")),
            Optional.fromNullable((Producer<String>) null));
   }

   @Test
   public void testProvideAuthenticationAbsentBoth()
   {
      final AuthenticationConfig authConfig = mock(AuthenticationConfig.class);
      when(authConfig.getType()).thenReturn(AuthType.BASIC);
      when(this.config.getAuthentication()).thenReturn(authConfig);
      final Optional<Producer<String>> o = Optional.absent();
      Assert.assertTrue(!this.module.provideAuthentication(o, o).isPresent());
   }

   @Test
   public void testProvideAuthenticationBasicAuth()
   {
      final AuthenticationConfig authConfig = mock(AuthenticationConfig.class);
      when(authConfig.getType()).thenReturn(AuthType.BASIC);
      when(this.config.getAuthentication()).thenReturn(authConfig);
      final Optional<HttpAuth> auth =
            this.module.provideAuthentication(Optional.of(Producers.of("username")),
                  Optional.of(Producers.of("password")));

      Assert.assertTrue(auth.isPresent());
      Assert.assertTrue(auth.get() instanceof BasicAuth);
   }

   @Test
   public void testProvideAuthenticationAWSAuthV2()
   {
      final AuthenticationConfig authConfig = mock(AuthenticationConfig.class);
      when(authConfig.getType()).thenReturn(AuthType.AWSV2);
      when(this.config.getAuthentication()).thenReturn(authConfig);
      final Optional<HttpAuth> auth =
            this.module.provideAuthentication(Optional.of(Producers.of("username")),
                  Optional.of(Producers.of("password")));

      Assert.assertTrue(auth.isPresent());
      Assert.assertTrue(auth.get() instanceof AWSAuthV2);
   }

   @Test(expected = NullPointerException.class)
   public void testProvideHeadersNullHeaders()
   {
      when(this.config.getHeaders()).thenReturn(null);
      this.module.provideHeaders();
   }

   @Test
   public void testProvideHeadersEmptyHeaders()
   {
      when(this.config.getHeaders()).thenReturn(new LinkedHashMap<String, String>());
      final Map<Producer<String>, Producer<String>> m = this.module.provideHeaders();
      Assert.assertNotNull(m);
      Assert.assertTrue(m.isEmpty());
   }

   @Test
   public void testProvideHeaders()
   {
      final Map<String, String> inMap = Maps.newLinkedHashMap();
      for (int i = 0; i < 10; i++)
      {
         inMap.put(String.valueOf(10 - i), String.valueOf(10 - i));
      }
      when(this.config.getHeaders()).thenReturn(inMap);
      final Map<Producer<String>, Producer<String>> m = this.module.provideHeaders();
      Assert.assertNotNull(m);
      final Iterator<Entry<Producer<String>, Producer<String>>> it = m.entrySet().iterator();
      int i = 0;
      while (it.hasNext())
      {
         final Entry<Producer<String>, Producer<String>> e = it.next();
         final String key = e.getKey().produce();
         final String value = e.getValue().produce();
         final String iKey = String.valueOf(10 - i);
         Assert.assertEquals(iKey, key);
         Assert.assertEquals(inMap.get(iKey), value);
         i++;
      }
   }

   @Test(expected = NullPointerException.class)
   public void testProvideWriteHeadersNullOperationConfig()
   {
      when(this.config.getWrite()).thenReturn(null);
      final Map<Producer<String>, Producer<String>> testHeaders = ImmutableMap.of();
      this.module.provideWriteHeaders(testHeaders);
   }

   @Test(expected = NullPointerException.class)
   public void testProvideWriteHeadersNullTestHeaders()
   {
      when(this.config.getWrite()).thenReturn(new OperationConfig());
      this.module.provideWriteHeaders(null);
   }

   @Test
   public void testProvideWriteHeadersDefault()
   {
      when(this.config.getWrite()).thenReturn(new OperationConfig());
      final Map<Producer<String>, Producer<String>> testHeaders = Maps.newLinkedHashMap();
      testHeaders.put(Producers.of("key"), Producers.of("value"));

      final Map<Producer<String>, Producer<String>> p =
            this.module.provideWriteHeaders(testHeaders);
      Assert.assertNotNull(p);
      Assert.assertEquals(1, p.size());
      final Entry<Producer<String>, Producer<String>> e = p.entrySet().iterator().next();
      Assert.assertEquals("key", e.getKey().produce());
      Assert.assertEquals("value", e.getValue().produce());
   }

   @Test
   public void testProvideWriteHeadersOverride()
   {
      final OperationConfig operationConfig = mock(OperationConfig.class);
      final Map<String, String> operationHeaders = Maps.newLinkedHashMap();
      operationHeaders.put("opKey", "opValue");
      when(operationConfig.getHeaders()).thenReturn(operationHeaders);
      when(this.config.getWrite()).thenReturn(operationConfig);

      final Map<Producer<String>, Producer<String>> testHeaders = Maps.newLinkedHashMap();
      testHeaders.put(Producers.of("key"), Producers.of("value"));

      final Map<Producer<String>, Producer<String>> p =
            this.module.provideWriteHeaders(testHeaders);
      Assert.assertNotNull(p);
      Assert.assertEquals(1, p.size());
      final Entry<Producer<String>, Producer<String>> e = p.entrySet().iterator().next();
      Assert.assertEquals("opKey", e.getKey().produce());
      Assert.assertEquals("opValue", e.getValue().produce());
   }

   @Test(expected = NullPointerException.class)
   public void testProvideReadHeadersNullOperationConfig()
   {
      when(this.config.getRead()).thenReturn(null);
      final Map<Producer<String>, Producer<String>> testHeaders = ImmutableMap.of();
      this.module.provideReadHeaders(testHeaders);
   }

   @Test(expected = NullPointerException.class)
   public void testProvideReadHeadersNullTestHeaders()
   {
      when(this.config.getRead()).thenReturn(new OperationConfig());
      this.module.provideReadHeaders(null);
   }

   @Test
   public void testProvideReadHeadersDefault()
   {
      when(this.config.getRead()).thenReturn(new OperationConfig());
      final Map<Producer<String>, Producer<String>> testHeaders = Maps.newLinkedHashMap();
      testHeaders.put(Producers.of("key"), Producers.of("value"));

      final Map<Producer<String>, Producer<String>> p = this.module.provideReadHeaders(testHeaders);
      Assert.assertNotNull(p);
      Assert.assertEquals(1, p.size());
      final Entry<Producer<String>, Producer<String>> e = p.entrySet().iterator().next();
      Assert.assertEquals("key", e.getKey().produce());
      Assert.assertEquals("value", e.getValue().produce());
   }

   @Test
   public void testProvideReadHeadersOverride()
   {
      final OperationConfig operationConfig = mock(OperationConfig.class);
      final Map<String, String> operationHeaders = Maps.newLinkedHashMap();
      operationHeaders.put("opKey", "opValue");
      when(operationConfig.getHeaders()).thenReturn(operationHeaders);
      when(this.config.getRead()).thenReturn(operationConfig);

      final Map<Producer<String>, Producer<String>> testHeaders = Maps.newLinkedHashMap();
      testHeaders.put(Producers.of("key"), Producers.of("value"));

      final Map<Producer<String>, Producer<String>> p = this.module.provideReadHeaders(testHeaders);
      Assert.assertNotNull(p);
      Assert.assertEquals(1, p.size());
      final Entry<Producer<String>, Producer<String>> e = p.entrySet().iterator().next();
      Assert.assertEquals("opKey", e.getKey().produce());
      Assert.assertEquals("opValue", e.getValue().produce());
   }

   @Test(expected = NullPointerException.class)
   public void testProvideDeleteHeadersNullOperationConfig()
   {
      when(this.config.getDelete()).thenReturn(null);
      final Map<Producer<String>, Producer<String>> testHeaders = ImmutableMap.of();
      this.module.provideDeleteHeaders(testHeaders);
   }

   @Test(expected = NullPointerException.class)
   public void testProvideDeleteHeadersNullTestHeaders()
   {
      when(this.config.getDelete()).thenReturn(new OperationConfig());
      this.module.provideDeleteHeaders(null);
   }

   @Test
   public void testProvideDeleteHeadersDefault()
   {
      when(this.config.getDelete()).thenReturn(new OperationConfig());
      final Map<Producer<String>, Producer<String>> testHeaders = Maps.newLinkedHashMap();
      testHeaders.put(Producers.of("key"), Producers.of("value"));

      final Map<Producer<String>, Producer<String>> p =
            this.module.provideDeleteHeaders(testHeaders);
      Assert.assertNotNull(p);
      Assert.assertEquals(1, p.size());
      final Entry<Producer<String>, Producer<String>> e = p.entrySet().iterator().next();
      Assert.assertEquals("key", e.getKey().produce());
      Assert.assertEquals("value", e.getValue().produce());
   }

   @Test
   public void testProvideDeleteHeadersOverride()
   {
      final OperationConfig operationConfig = mock(OperationConfig.class);
      final Map<String, String> operationHeaders = Maps.newLinkedHashMap();
      operationHeaders.put("opKey", "opValue");
      when(operationConfig.getHeaders()).thenReturn(operationHeaders);
      when(this.config.getDelete()).thenReturn(operationConfig);

      final Map<Producer<String>, Producer<String>> testHeaders = Maps.newLinkedHashMap();
      testHeaders.put(Producers.of("key"), Producers.of("value"));

      final Map<Producer<String>, Producer<String>> p =
            this.module.provideDeleteHeaders(testHeaders);
      Assert.assertNotNull(p);
      Assert.assertEquals(1, p.size());
      final Entry<Producer<String>, Producer<String>> e = p.entrySet().iterator().next();
      Assert.assertEquals("opKey", e.getKey().produce());
      Assert.assertEquals("opValue", e.getValue().produce());
   }

   @Test(expected = NullPointerException.class)
   public void testProvideEntityNullFilesizeSelection()
   {
      when(this.config.getFilesizeSelection()).thenReturn(null);
      this.module.provideEntity();
   }

   @Test(expected = NullPointerException.class)
   public void testProvideEntityNullFilesize()
   {
      when(this.config.getFilesizeSelection()).thenReturn(CollectionAlgorithmType.ROUNDROBIN);
      when(this.config.getFilesize()).thenReturn(null);
      this.module.provideEntity();
   }

   @Test(expected = IllegalArgumentException.class)
   public void testProvideEntityEmptyFilesize()
   {
      when(this.config.getFilesizeSelection()).thenReturn(CollectionAlgorithmType.ROUNDROBIN);
      final List<FilesizeConfig> filesize = Lists.newArrayList();
      when(this.config.getFilesize()).thenReturn(filesize);
      this.module.provideEntity();
   }

   @Test(expected = IllegalArgumentException.class)
   public void testProvideEntityPoissonDistribution()
   {
      when(this.config.getFilesizeSelection()).thenReturn(CollectionAlgorithmType.ROUNDROBIN);
      final FilesizeConfig f = mock(FilesizeConfig.class);
      when(f.getDistribution()).thenReturn(DistributionType.POISSON);
      when(f.getAverageUnit()).thenReturn(SizeUnit.MEBIBYTES);
      when(f.getSpreadUnit()).thenReturn(SizeUnit.MEBIBYTES);
      final List<FilesizeConfig> filesize = Lists.newArrayList();
      filesize.add(f);
      when(this.config.getFilesize()).thenReturn(filesize);
      this.module.provideEntity();
   }

   @Test(expected = NullPointerException.class)
   public void testProvideEntityNullSource()
   {
      when(this.config.getFilesizeSelection()).thenReturn(CollectionAlgorithmType.ROUNDROBIN);
      final List<FilesizeConfig> filesize = Lists.newArrayList();
      filesize.add(new FilesizeConfig(10.0));
      when(this.config.getSource()).thenReturn(null);
      when(this.config.getFilesize()).thenReturn(filesize);
      this.module.provideEntity();
   }

   @Test(expected = IllegalArgumentException.class)
   public void testProvideEntityNoneSource()
   {
      when(this.config.getFilesizeSelection()).thenReturn(CollectionAlgorithmType.ROUNDROBIN);
      final List<FilesizeConfig> filesize = Lists.newArrayList();
      filesize.add(new FilesizeConfig(10.0));
      when(this.config.getSource()).thenReturn(EntityType.NONE);
      when(this.config.getFilesize()).thenReturn(filesize);
      this.module.provideEntity();
   }

   @Test
   public void testProvideEntitySingleFilesizeZeroesSource()
   {
      when(this.config.getFilesizeSelection()).thenReturn(CollectionAlgorithmType.ROUNDROBIN);
      final List<FilesizeConfig> filesize = Lists.newArrayList();
      filesize.add(new FilesizeConfig(10.0));
      when(this.config.getSource()).thenReturn(EntityType.ZEROES);
      when(this.config.getFilesize()).thenReturn(filesize);
      final Producer<Entity> p = this.module.provideEntity();

      Assert.assertNotNull(p);
      final Entity e = p.produce();
      Assert.assertEquals(EntityType.ZEROES, e.getType());
      Assert.assertEquals(SizeUnit.MEBIBYTES.toBytes(10), e.getSize());
   }

   @Test
   public void testProvideEntitySingleFilesizeRandomSource()
   {
      when(this.config.getFilesizeSelection()).thenReturn(CollectionAlgorithmType.ROUNDROBIN);
      final List<FilesizeConfig> filesize = Lists.newArrayList();
      filesize.add(new FilesizeConfig(10.0));
      when(this.config.getSource()).thenReturn(EntityType.RANDOM);
      when(this.config.getFilesize()).thenReturn(filesize);
      final Producer<Entity> p = this.module.provideEntity();

      Assert.assertNotNull(p);
      final Entity e = p.produce();
      Assert.assertEquals(EntityType.RANDOM, e.getType());
      Assert.assertEquals(SizeUnit.MEBIBYTES.toBytes(10), e.getSize());
   }

   @Test
   public void testProvideEntityMultipleFilesizeRoundRobin()
   {
      when(this.config.getFilesizeSelection()).thenReturn(CollectionAlgorithmType.ROUNDROBIN);
      final List<FilesizeConfig> filesize = Lists.newArrayList();
      filesize.add(new FilesizeConfig(10.0));
      filesize.add(new FilesizeConfig(25.0));
      when(this.config.getSource()).thenReturn(EntityType.RANDOM);
      when(this.config.getFilesize()).thenReturn(filesize);
      final Producer<Entity> p = this.module.provideEntity();

      Assert.assertNotNull(p);
      for (int i = 0; i < 100; i++)
      {
         Assert.assertEquals(SizeUnit.MEBIBYTES.toBytes(10), p.produce().getSize());
         Assert.assertEquals(SizeUnit.MEBIBYTES.toBytes(25), p.produce().getSize());
      }
   }

   @Test(expected = AssertionError.class)
   public void testProvideEntityMultipleFilesizeRandom()
   {
      when(this.config.getFilesizeSelection()).thenReturn(CollectionAlgorithmType.RANDOM);
      final List<FilesizeConfig> filesize = Lists.newArrayList();
      filesize.add(new FilesizeConfig(10.0));
      filesize.add(new FilesizeConfig(25.0));
      when(this.config.getSource()).thenReturn(EntityType.RANDOM);
      when(this.config.getFilesize()).thenReturn(filesize);
      final Producer<Entity> p = this.module.provideEntity();

      Assert.assertNotNull(p);
      for (int i = 0; i < 100; i++)
      {
         // Should not exhibit roundrobin behavior over a large sample, expect assertion error
         Assert.assertEquals(SizeUnit.MEBIBYTES.toBytes(10), p.produce().getSize());
         Assert.assertEquals(SizeUnit.MEBIBYTES.toBytes(25), p.produce().getSize());
      }
   }

   @Test(expected = NullPointerException.class)
   public void testProvideObjectFileLocationNullLocation() throws IOException
   {
      final ObjectManagerConfig objectManagerConfig = mock(ObjectManagerConfig.class);
      when(objectManagerConfig.getObjectFileLocation()).thenReturn(null);
      when(this.config.getObjectManager()).thenReturn(objectManagerConfig);

      this.module.provideObjectFileLocation();
   }

   @Test(expected = IllegalArgumentException.class)
   public void testProvideObjectFileLocationEmptyLocation() throws IOException
   {
      final ObjectManagerConfig objectManagerConfig = mock(ObjectManagerConfig.class);
      when(objectManagerConfig.getObjectFileLocation()).thenReturn("");
      when(this.config.getObjectManager()).thenReturn(objectManagerConfig);

      this.module.provideObjectFileLocation();
   }

   @Test(expected = IllegalArgumentException.class)
   public void testProvideObjectFileLocationExistingIsFile() throws IOException
   {
      final ObjectManagerConfig objectManagerConfig = mock(ObjectManagerConfig.class);
      final File existing = File.createTempFile("existing", null);
      when(objectManagerConfig.getObjectFileLocation()).thenReturn(existing.toString());
      when(this.config.getObjectManager()).thenReturn(objectManagerConfig);

      this.module.provideObjectFileLocation();
   }

   @Test
   public void testProvideObjectFileLocationNonExisting() throws IOException
   {
      final ObjectManagerConfig objectManagerConfig = mock(ObjectManagerConfig.class);
      final File existing = Files.createTempDir();
      final File nonExisting = new File(existing, String.valueOf(System.nanoTime()));
      when(objectManagerConfig.getObjectFileLocation()).thenReturn(nonExisting.toString());
      when(this.config.getObjectManager()).thenReturn(objectManagerConfig);

      this.module.provideObjectFileLocation();
   }

   @Test(expected = NullPointerException.class)
   public void testProvideObjectFileNameNullContainer()
   {
      this.module.provideObjectFileName(null, Api.S3);
   }

   @Test(expected = NullPointerException.class)
   public void testProvideObjectFileNameNullApi()
   {
      this.module.provideObjectFileName(Producers.of("container"), null);
   }

   @Test
   public void testProvideObjectFileNameNullObjectFileName()
   {
      final ObjectManagerConfig objectManagerConfig = mock(ObjectManagerConfig.class);
      when(objectManagerConfig.getObjectFileName()).thenReturn(null);
      when(this.config.getObjectManager()).thenReturn(objectManagerConfig);
      final String name = this.module.provideObjectFileName(Producers.of("container"), Api.S3);

      Assert.assertEquals("container-s3", name);
   }

   @Test
   public void testProvideObjectFileNameEmptyObjectFileName()
   {
      final ObjectManagerConfig objectManagerConfig = mock(ObjectManagerConfig.class);
      when(objectManagerConfig.getObjectFileName()).thenReturn("");
      when(this.config.getObjectManager()).thenReturn(objectManagerConfig);
      final String name = this.module.provideObjectFileName(Producers.of("container"), Api.S3);

      Assert.assertEquals("container-s3", name);
   }

   @Test
   public void testProvideObjectFileName()
   {
      final ObjectManagerConfig objectManagerConfig = mock(ObjectManagerConfig.class);
      when(objectManagerConfig.getObjectFileName()).thenReturn("myObjectFileName");
      when(this.config.getObjectManager()).thenReturn(objectManagerConfig);
      final String name = this.module.provideObjectFileName(Producers.of("container"), Api.S3);

      Assert.assertEquals("myObjectFileName", name);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testProvideWriteWeightNegative()
   {
      when(this.config.getWrite()).thenReturn(new OperationConfig(-1.0));
      when(this.config.getRead()).thenReturn(new OperationConfig(50.0));
      when(this.config.getDelete()).thenReturn(new OperationConfig(50.0));

      this.module.provideWriteWeight();
   }

   @Test
   public void testProvideWriteWeightZero()
   {
      when(this.config.getWrite()).thenReturn(new OperationConfig(0.0));
      when(this.config.getRead()).thenReturn(new OperationConfig(50.0));
      when(this.config.getDelete()).thenReturn(new OperationConfig(50.0));

      Assert.assertEquals(0.0, this.module.provideWriteWeight(), ERR);
   }

   @Test
   public void testProvideWriteWeightPositive()
   {
      when(this.config.getWrite()).thenReturn(new OperationConfig(50.0));
      when(this.config.getRead()).thenReturn(new OperationConfig(50.0));
      when(this.config.getDelete()).thenReturn(new OperationConfig(50.0));

      Assert.assertEquals(50.0, this.module.provideWriteWeight(), ERR);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testProvideWriteWeightGreaterThan100()
   {
      when(this.config.getWrite()).thenReturn(new OperationConfig(101.0));
      when(this.config.getRead()).thenReturn(new OperationConfig(50.0));
      when(this.config.getDelete()).thenReturn(new OperationConfig(50.0));

      this.module.provideWriteWeight();
   }

   @Test
   public void testProvideWriteWeightAllZero()
   {
      when(this.config.getWrite()).thenReturn(new OperationConfig(0.0));
      when(this.config.getRead()).thenReturn(new OperationConfig(0.0));
      when(this.config.getDelete()).thenReturn(new OperationConfig(0.0));

      Assert.assertEquals(100.0, this.module.provideWriteWeight(), ERR);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testProvideReadWeightNegative()
   {
      when(this.config.getRead()).thenReturn(new OperationConfig(-1.0));
      this.module.provideReadWeight();
   }

   @Test
   public void testProvideReadWeightZero()
   {
      when(this.config.getRead()).thenReturn(new OperationConfig(0.0));
      Assert.assertEquals(0.0, this.module.provideReadWeight(), ERR);
   }

   @Test
   public void testProvideReadWeightPositive()
   {
      when(this.config.getRead()).thenReturn(new OperationConfig(50.0));
      Assert.assertEquals(50.0, this.module.provideReadWeight(), ERR);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testProvideReadWeightGreaterThan100()
   {
      when(this.config.getRead()).thenReturn(new OperationConfig(101.0));
      this.module.provideReadWeight();
   }

   @Test(expected = IllegalArgumentException.class)
   public void testProvideDeleteWeightNegative()
   {
      when(this.config.getDelete()).thenReturn(new OperationConfig(-1.0));
      this.module.provideDeleteWeight();
   }

   @Test
   public void testProvideDeleteWeightZero()
   {
      when(this.config.getDelete()).thenReturn(new OperationConfig(0.0));
      Assert.assertEquals(0.0, this.module.provideDeleteWeight(), ERR);
   }

   @Test
   public void testProvideDeleteWeightPositive()
   {
      when(this.config.getDelete()).thenReturn(new OperationConfig(50.0));
      Assert.assertEquals(50.0, this.module.provideDeleteWeight(), ERR);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testProvideDeleteWeightGreaterThan100()
   {
      when(this.config.getDelete()).thenReturn(new OperationConfig(101.0));
      this.module.provideDeleteWeight();
   }

   @Test(expected = NullPointerException.class)
   public void testProvideSchedulerNullEventBus()
   {
      this.module.provideScheduler(null);
   }

   @Test(expected = NullPointerException.class)
   public void testProvideSchedulerNullConcurrencyConfig()
   {
      when(this.config.getConcurrency()).thenReturn(null);
      this.module.provideScheduler(new EventBus());
   }

   @Test(expected = NullPointerException.class)
   public void testProvideSchedulerNullConcurrencyType()
   {
      final ConcurrencyConfig concurrencyConfig = mock(ConcurrencyConfig.class);
      when(concurrencyConfig.getType()).thenReturn(null);
      when(this.config.getConcurrency()).thenReturn(concurrencyConfig);
      this.module.provideScheduler(new EventBus());
   }

   @Test(expected = NullPointerException.class)
   public void testProvideSchedulerNullDistributionType()
   {
      final ConcurrencyConfig concurrencyConfig = mock(ConcurrencyConfig.class);
      when(concurrencyConfig.getType()).thenReturn(ConcurrencyType.THREADS);
      when(concurrencyConfig.getDistribution()).thenReturn(null);
      when(this.config.getConcurrency()).thenReturn(concurrencyConfig);
      this.module.provideScheduler(new EventBus());
   }

   @Test(expected = IllegalArgumentException.class)
   public void testProvideSchedulerInvalidDistributionType()
   {
      final ConcurrencyConfig concurrencyConfig = mock(ConcurrencyConfig.class);
      when(concurrencyConfig.getType()).thenReturn(ConcurrencyType.IOPS);
      when(concurrencyConfig.getDistribution()).thenReturn(DistributionType.NORMAL);
      when(this.config.getConcurrency()).thenReturn(concurrencyConfig);
      this.module.provideScheduler(new EventBus());
   }

   @Test
   public void testProvideSchedulerThreads()
   {
      final ConcurrencyConfig concurrencyConfig = mock(ConcurrencyConfig.class);
      when(concurrencyConfig.getType()).thenReturn(ConcurrencyType.THREADS);
      when(concurrencyConfig.getDistribution()).thenReturn(DistributionType.UNIFORM);
      when(concurrencyConfig.getCount()).thenReturn(1.0);
      when(this.config.getConcurrency()).thenReturn(concurrencyConfig);
      final Scheduler s = this.module.provideScheduler(new EventBus());

      Assert.assertTrue(s instanceof ConcurrentRequestScheduler);
   }

   @Test
   public void testProvideSchedulerIOps()
   {
      final ConcurrencyConfig concurrencyConfig = mock(ConcurrencyConfig.class);
      when(concurrencyConfig.getType()).thenReturn(ConcurrencyType.IOPS);
      when(concurrencyConfig.getDistribution()).thenReturn(DistributionType.UNIFORM);
      when(concurrencyConfig.getCount()).thenReturn(1.0);
      when(concurrencyConfig.getUnit()).thenReturn(TimeUnit.SECONDS);
      when(this.config.getConcurrency()).thenReturn(concurrencyConfig);
      final Scheduler s = this.module.provideScheduler(new EventBus());

      Assert.assertTrue(s instanceof RequestRateScheduler);
   }
}
