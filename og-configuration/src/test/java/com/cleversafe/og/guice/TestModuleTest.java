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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.cleversafe.og.http.Scheme;
import com.cleversafe.og.http.auth.BasicAuth;
import com.cleversafe.og.http.auth.HttpAuth;
import com.cleversafe.og.http.util.Api;
import com.cleversafe.og.json.AuthenticationConfig;
import com.cleversafe.og.json.ConcurrencyConfig;
import com.cleversafe.og.json.FilesizeConfig;
import com.cleversafe.og.json.HostConfig;
import com.cleversafe.og.json.ObjectManagerConfig;
import com.cleversafe.og.json.OperationConfig;
import com.cleversafe.og.json.TestConfig;
import com.cleversafe.og.json.enums.AuthType;
import com.cleversafe.og.json.enums.CollectionAlgorithmType;
import com.cleversafe.og.json.enums.ConcurrencyType;
import com.cleversafe.og.json.enums.DistributionType;
import com.cleversafe.og.operation.Entity;
import com.cleversafe.og.operation.EntityType;
import com.cleversafe.og.producer.Producer;
import com.cleversafe.og.producer.Producers;
import com.cleversafe.og.s3.auth.AWSAuthV2;
import com.cleversafe.og.scheduling.ConcurrentRequestScheduler;
import com.cleversafe.og.scheduling.RequestRateScheduler;
import com.cleversafe.og.scheduling.Scheduler;
import com.cleversafe.og.util.SizeUnit;
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
   public void testTestIdProducer()
   {
      Assert.assertNotNull(this.module.testIdProducer());
   }

   @Test(expected = NullPointerException.class)
   public void testTestSchemeNullScheme()
   {
      this.module.testScheme();
   }

   @Test
   public void testTestScheme()
   {
      when(this.config.getScheme()).thenReturn(Scheme.HTTP);
      final Producer<Scheme> p = this.module.testScheme();
      Assert.assertEquals(Scheme.HTTP, p.produce());
   }

   @Test(expected = NullPointerException.class)
   public void testTestHostNullHostSelection()
   {
      when(this.config.getHostSelection()).thenReturn(null);
      final List<HostConfig> hostConfig = new ArrayList<HostConfig>();
      hostConfig.add(new HostConfig());
      when(this.config.getHost()).thenReturn(hostConfig);
      this.module.testHost();
   }

   @Test(expected = NullPointerException.class)
   public void testTestHostNullHost()
   {
      when(this.config.getHostSelection()).thenReturn(CollectionAlgorithmType.RANDOM);
      when(this.config.getHost()).thenReturn(null);
      this.module.testHost();
   }

   @Test(expected = IllegalArgumentException.class)
   public void testTestHostEmptyHost()
   {
      when(this.config.getHostSelection()).thenReturn(CollectionAlgorithmType.RANDOM);
      final List<HostConfig> hostConfig = new ArrayList<HostConfig>();
      when(this.config.getHost()).thenReturn(hostConfig);
      this.module.testHost();
   }

   @Test(expected = NullPointerException.class)
   public void testTestHostNullHostConfig()
   {
      when(this.config.getHostSelection()).thenReturn(CollectionAlgorithmType.RANDOM);
      final List<HostConfig> hostConfig = new ArrayList<HostConfig>();
      hostConfig.add(null);
      when(this.config.getHost()).thenReturn(hostConfig);
      this.module.testHost();
   }

   @Test(expected = IllegalArgumentException.class)
   public void testTestHostSingleHostEmptyHost()
   {
      when(this.config.getHostSelection()).thenReturn(CollectionAlgorithmType.ROUNDROBIN);
      final List<HostConfig> hostConfig = new ArrayList<HostConfig>();
      hostConfig.add(new HostConfig(""));
      when(this.config.getHost()).thenReturn(hostConfig);
      this.module.testHost();
   }

   @Test
   public void testTestHostSingleHostRoundRobin()
   {
      when(this.config.getHostSelection()).thenReturn(CollectionAlgorithmType.ROUNDROBIN);
      final List<HostConfig> hostConfig = new ArrayList<HostConfig>();
      hostConfig.add(new HostConfig("192.168.8.1"));
      when(this.config.getHost()).thenReturn(hostConfig);
      final Producer<String> p = this.module.testHost();
      for (int i = 0; i < 100; i++)
      {
         Assert.assertEquals("192.168.8.1", p.produce());
      }
   }

   @Test
   public void testTestHostSingleHostRandom()
   {
      when(this.config.getHostSelection()).thenReturn(CollectionAlgorithmType.RANDOM);
      final List<HostConfig> hostConfig = new ArrayList<HostConfig>();
      hostConfig.add(new HostConfig("192.168.8.1"));
      when(this.config.getHost()).thenReturn(hostConfig);
      final Producer<String> p = this.module.testHost();
      for (int i = 0; i < 100; i++)
      {
         Assert.assertEquals("192.168.8.1", p.produce());
      }
   }

   @Test
   public void testTestHostMultipleHostRoundRobin()
   {
      when(this.config.getHostSelection()).thenReturn(CollectionAlgorithmType.ROUNDROBIN);
      final List<HostConfig> hostConfig = new ArrayList<HostConfig>();
      hostConfig.add(new HostConfig("192.168.8.1"));
      hostConfig.add(new HostConfig("192.168.8.2"));
      when(this.config.getHost()).thenReturn(hostConfig);
      final Producer<String> p = this.module.testHost();
      for (int i = 0; i < 100; i++)
      {
         Assert.assertEquals("192.168.8.1", p.produce());
         Assert.assertEquals("192.168.8.2", p.produce());
      }
   }

   @Test(expected = AssertionError.class)
   public void testTestHostMultipleHostRandom()
   {
      when(this.config.getHostSelection()).thenReturn(CollectionAlgorithmType.RANDOM);
      final List<HostConfig> hostConfig = new ArrayList<HostConfig>();
      hostConfig.add(new HostConfig("192.168.8.1"));
      when(this.config.getHost()).thenReturn(hostConfig);
      final Producer<String> p = this.module.testHost();
      for (int i = 0; i < 100; i++)
      {
         // Should not exhibit roundrobin behavior over a large sample, expect assertion error
         Assert.assertEquals("192.168.8.1", p.produce());
         Assert.assertEquals("192.168.8.2", p.produce());
      }
   }

   @Test(expected = NullPointerException.class)
   public void testTestWriteHostNullOperationConfig()
   {
      when(this.config.getWrite()).thenReturn(null);
      this.module.testWriteHost(Producers.of("192.168.8.1"));
   }

   @Test(expected = NullPointerException.class)
   public void testTestWriteHostNullTestHost()
   {
      when(this.config.getWrite()).thenReturn(new OperationConfig());
      this.module.testWriteHost(null);
   }

   @Test
   public void testTestWriteHostDefault()
   {
      when(this.config.getWrite()).thenReturn(new OperationConfig());
      final Producer<String> p = this.module.testWriteHost(Producers.of("192.168.8.1"));
      Assert.assertEquals("192.168.8.1", p.produce());
   }

   @Test
   public void testTestWriteHostOverride()
   {
      final OperationConfig operationConfig = mock(OperationConfig.class);
      final List<HostConfig> host = new ArrayList<HostConfig>();
      host.add(new HostConfig("10.1.1.1"));
      when(operationConfig.getHostSelection()).thenReturn(CollectionAlgorithmType.RANDOM);
      when(operationConfig.getHost()).thenReturn(host);
      when(this.config.getWrite()).thenReturn(operationConfig);

      final Producer<String> p = this.module.testWriteHost(Producers.of("192.168.8.1"));
      Assert.assertEquals("10.1.1.1", p.produce());
   }

   @Test(expected = NullPointerException.class)
   public void testTestReadHostNullOperationConfig()
   {
      when(this.config.getRead()).thenReturn(null);
      this.module.testReadHost(Producers.of("192.168.8.1"));
   }

   @Test(expected = NullPointerException.class)
   public void testTestReadHostNullTestHost()
   {
      when(this.config.getRead()).thenReturn(new OperationConfig());
      this.module.testReadHost(null);
   }

   @Test
   public void testTestReadHostDefault()
   {
      when(this.config.getRead()).thenReturn(new OperationConfig());
      final Producer<String> p = this.module.testReadHost(Producers.of("192.168.8.1"));
      Assert.assertEquals("192.168.8.1", p.produce());
   }

   @Test
   public void testTestReadHostOverride()
   {
      final OperationConfig operationConfig = mock(OperationConfig.class);
      final List<HostConfig> host = new ArrayList<HostConfig>();
      host.add(new HostConfig("10.1.1.1"));
      when(operationConfig.getHostSelection()).thenReturn(CollectionAlgorithmType.RANDOM);
      when(operationConfig.getHost()).thenReturn(host);
      when(this.config.getRead()).thenReturn(operationConfig);

      final Producer<String> p = this.module.testReadHost(Producers.of("192.168.8.1"));
      Assert.assertEquals("10.1.1.1", p.produce());
   }

   @Test(expected = NullPointerException.class)
   public void testTestDeleteHostNullOperationConfig()
   {
      when(this.config.getDelete()).thenReturn(null);
      this.module.testDeleteHost(Producers.of("192.168.8.1"));
   }

   @Test(expected = NullPointerException.class)
   public void testTestDeleteHostNullTestHost()
   {
      when(this.config.getDelete()).thenReturn(new OperationConfig());
      this.module.testDeleteHost(null);
   }

   @Test
   public void testTestDeleteHostDefault()
   {
      when(this.config.getDelete()).thenReturn(new OperationConfig());
      final Producer<String> p = this.module.testDeleteHost(Producers.of("192.168.8.1"));
      Assert.assertEquals("192.168.8.1", p.produce());
   }

   @Test
   public void testTestDeleteHostOverride()
   {
      final OperationConfig operationConfig = mock(OperationConfig.class);
      final List<HostConfig> host = new ArrayList<HostConfig>();
      host.add(new HostConfig("10.1.1.1"));
      when(operationConfig.getHostSelection()).thenReturn(CollectionAlgorithmType.RANDOM);
      when(operationConfig.getHost()).thenReturn(host);
      when(this.config.getDelete()).thenReturn(operationConfig);

      final Producer<String> p = this.module.testDeleteHost(Producers.of("192.168.8.1"));
      Assert.assertEquals("10.1.1.1", p.produce());
   }

   @Test
   public void testTestPortNullPort()
   {
      when(this.config.getPort()).thenReturn(null);
      final Producer<Integer> p = this.module.testPort();
      Assert.assertNull(p);
   }

   @Test
   public void testTestPort()
   {
      when(this.config.getPort()).thenReturn(80);
      final Producer<Integer> p = this.module.testPort();
      Assert.assertNotNull(p);
      Assert.assertEquals(Integer.valueOf(80), p.produce());
   }

   @Test(expected = NullPointerException.class)
   public void testTestApiNullApi()
   {
      when(this.config.getApi()).thenReturn(null);
      this.module.testApi();
   }

   @Test
   public void testTestApi()
   {
      when(this.config.getApi()).thenReturn(Api.S3);
      final Api api = this.module.testApi();
      Assert.assertEquals(Api.S3, api);
   }

   @Test
   public void testUriRootNullUriRoot()
   {
      when(this.config.getUriRoot()).thenReturn(null);
      when(this.config.getApi()).thenReturn(Api.S3);
      final Producer<String> p = this.module.testUriRoot();
      Assert.assertEquals("s3", p.produce());
   }

   @Test
   public void testUriRootSlash()
   {
      when(this.config.getUriRoot()).thenReturn("/");
      when(this.config.getApi()).thenReturn(Api.S3);
      Assert.assertNull(this.module.testUriRoot());
   }

   @Test
   public void testUriRootCustomRoot()
   {
      when(this.config.getUriRoot()).thenReturn("foo");
      when(this.config.getApi()).thenReturn(Api.S3);
      final Producer<String> p = this.module.testUriRoot();
      Assert.assertEquals("foo", p.produce());
   }

   @Test
   public void testUriRootCustomRoot2()
   {
      when(this.config.getUriRoot()).thenReturn("/foo");
      when(this.config.getApi()).thenReturn(Api.S3);
      final Producer<String> p = this.module.testUriRoot();
      Assert.assertEquals("foo", p.produce());
   }

   @Test
   public void testUriRootCustomRoot3()
   {
      when(this.config.getUriRoot()).thenReturn("foo/");
      when(this.config.getApi()).thenReturn(Api.S3);
      final Producer<String> p = this.module.testUriRoot();
      Assert.assertEquals("foo", p.produce());
   }

   @Test
   public void testUriRootCustomRoot4()
   {
      when(this.config.getUriRoot()).thenReturn("/foo/");
      when(this.config.getApi()).thenReturn(Api.S3);
      final Producer<String> p = this.module.testUriRoot();
      Assert.assertEquals("foo", p.produce());
   }

   @Test
   public void testUriRootCustomRoot5()
   {
      when(this.config.getUriRoot()).thenReturn("//foo///");
      when(this.config.getApi()).thenReturn(Api.S3);
      final Producer<String> p = this.module.testUriRoot();
      Assert.assertEquals("foo", p.produce());
   }

   @Test(expected = NullPointerException.class)
   public void testTestContainerNullContainer()
   {
      when(this.config.getContainer()).thenReturn(null);
      this.module.testContainer();
   }

   @Test(expected = IllegalArgumentException.class)
   public void testTestContainerEmptyContainer()
   {
      when(this.config.getContainer()).thenReturn("");
      this.module.testContainer();
   }

   @Test
   public void testTestContainer()
   {
      when(this.config.getContainer()).thenReturn("container");
      final Producer<String> p = this.module.testContainer();
      Assert.assertEquals("container", p.produce());
   }

   @Test
   public void testTestUsernameNullUsername()
   {
      when(this.config.getAuthentication()).thenReturn(new AuthenticationConfig());
      Assert.assertNull(this.module.testUsername());
   }

   @Test(expected = IllegalArgumentException.class)
   public void testTestUsernameEmptyUsername()
   {
      final AuthenticationConfig authConfig = mock(AuthenticationConfig.class);
      when(authConfig.getUsername()).thenReturn("");
      when(this.config.getAuthentication()).thenReturn(authConfig);
      this.module.testUsername();
   }

   @Test
   public void testTestUsername()
   {
      final AuthenticationConfig authConfig = mock(AuthenticationConfig.class);
      when(authConfig.getUsername()).thenReturn("user");
      when(this.config.getAuthentication()).thenReturn(authConfig);
      final Producer<String> p = this.module.testUsername();
      Assert.assertNotNull(p);
      Assert.assertEquals("user", p.produce());
   }

   @Test
   public void testTestPasswordNullPassword()
   {
      when(this.config.getAuthentication()).thenReturn(new AuthenticationConfig());
      Assert.assertNull(this.module.testPassword());
   }

   @Test(expected = IllegalArgumentException.class)
   public void testTestPasswordEmptyPassword()
   {
      final AuthenticationConfig authConfig = mock(AuthenticationConfig.class);
      when(authConfig.getPassword()).thenReturn("");
      when(this.config.getAuthentication()).thenReturn(authConfig);
      this.module.testPassword();
   }

   @Test
   public void testTestPassword()
   {
      final AuthenticationConfig authConfig = mock(AuthenticationConfig.class);
      when(authConfig.getPassword()).thenReturn("password");
      when(this.config.getAuthentication()).thenReturn(authConfig);
      final Producer<String> p = this.module.testPassword();
      Assert.assertNotNull(p);
      Assert.assertEquals("password", p.produce());
   }

   @Test(expected = NullPointerException.class)
   public void testTestAuthenticationNullAuthType()
   {
      final AuthenticationConfig authConfig = mock(AuthenticationConfig.class);
      when(authConfig.getType()).thenReturn(null);
      when(this.config.getAuthentication()).thenReturn(authConfig);
      this.module.testAuthentication(Producers.of("username"), Producers.of("password"));
   }

   @Test(expected = IllegalArgumentException.class)
   public void testTestAuthenticationNullUsername()
   {
      final AuthenticationConfig authConfig = mock(AuthenticationConfig.class);
      when(authConfig.getType()).thenReturn(AuthType.BASIC);
      when(this.config.getAuthentication()).thenReturn(authConfig);
      this.module.testAuthentication(null, Producers.of("password"));
   }

   @Test(expected = IllegalArgumentException.class)
   public void testTestAuthenticationNullPassword()
   {
      final AuthenticationConfig authConfig = mock(AuthenticationConfig.class);
      when(authConfig.getType()).thenReturn(AuthType.BASIC);
      when(this.config.getAuthentication()).thenReturn(authConfig);
      this.module.testAuthentication(Producers.of("username"), null);
   }

   @Test
   public void testTestAuthenticationNullBoth()
   {
      final AuthenticationConfig authConfig = mock(AuthenticationConfig.class);
      when(authConfig.getType()).thenReturn(AuthType.BASIC);
      when(this.config.getAuthentication()).thenReturn(authConfig);
      Assert.assertNull(this.module.testAuthentication(null, null));
   }

   @Test
   public void testTestAuthenticationBasicAuth()
   {
      final AuthenticationConfig authConfig = mock(AuthenticationConfig.class);
      when(authConfig.getType()).thenReturn(AuthType.BASIC);
      when(this.config.getAuthentication()).thenReturn(authConfig);
      final HttpAuth auth =
            this.module.testAuthentication(Producers.of("username"), Producers.of("password"));

      Assert.assertNotNull(auth);
      Assert.assertTrue(auth instanceof BasicAuth);
   }

   @Test
   public void testTestAuthenticationAWSAuthV2()
   {
      final AuthenticationConfig authConfig = mock(AuthenticationConfig.class);
      when(authConfig.getType()).thenReturn(AuthType.AWSV2);
      when(this.config.getAuthentication()).thenReturn(authConfig);
      final HttpAuth auth =
            this.module.testAuthentication(Producers.of("username"), Producers.of("password"));

      Assert.assertNotNull(auth);
      Assert.assertTrue(auth instanceof AWSAuthV2);
   }

   @Test(expected = NullPointerException.class)
   public void testTestHeadersNullHeaders()
   {
      when(this.config.getHeaders()).thenReturn(null);
      this.module.testHeaders();
   }

   @Test
   public void testTestHeadersEmptyHeaders()
   {
      when(this.config.getHeaders()).thenReturn(new LinkedHashMap<String, String>());
      final Map<Producer<String>, Producer<String>> m = this.module.testHeaders();
      Assert.assertNotNull(m);
      Assert.assertTrue(m.isEmpty());
   }

   @Test
   public void testTestHeaders()
   {
      final Map<String, String> inMap = new LinkedHashMap<String, String>();
      for (int i = 0; i < 10; i++)
      {
         inMap.put(String.valueOf(10 - i), String.valueOf(10 - i));
      }
      when(this.config.getHeaders()).thenReturn(inMap);
      final Map<Producer<String>, Producer<String>> m = this.module.testHeaders();
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
   public void testTestWriteHeadersNullOperationConfig()
   {
      when(this.config.getWrite()).thenReturn(null);
      final Map<Producer<String>, Producer<String>> testHeaders = Collections.emptyMap();
      this.module.testWriteHeaders(testHeaders);
   }

   @Test(expected = NullPointerException.class)
   public void testTestWriteHeadersNullTestHeaders()
   {
      when(this.config.getWrite()).thenReturn(new OperationConfig());
      this.module.testWriteHeaders(null);
   }

   @Test
   public void testTestWriteHeadersDefault()
   {
      when(this.config.getWrite()).thenReturn(new OperationConfig());
      final Map<Producer<String>, Producer<String>> testHeaders =
            new LinkedHashMap<Producer<String>, Producer<String>>();
      testHeaders.put(Producers.of("key"), Producers.of("value"));

      final Map<Producer<String>, Producer<String>> p = this.module.testWriteHeaders(testHeaders);
      Assert.assertNotNull(p);
      Assert.assertEquals(1, p.size());
      final Entry<Producer<String>, Producer<String>> e = p.entrySet().iterator().next();
      Assert.assertEquals("key", e.getKey().produce());
      Assert.assertEquals("value", e.getValue().produce());
   }

   @Test
   public void testTestWriteHeadersOverride()
   {
      final OperationConfig operationConfig = mock(OperationConfig.class);
      final Map<String, String> operationHeaders = new LinkedHashMap<String, String>();
      operationHeaders.put("opKey", "opValue");
      when(operationConfig.getHeaders()).thenReturn(operationHeaders);
      when(this.config.getWrite()).thenReturn(operationConfig);

      final Map<Producer<String>, Producer<String>> testHeaders =
            new LinkedHashMap<Producer<String>, Producer<String>>();
      testHeaders.put(Producers.of("key"), Producers.of("value"));

      final Map<Producer<String>, Producer<String>> p = this.module.testWriteHeaders(testHeaders);
      Assert.assertNotNull(p);
      Assert.assertEquals(1, p.size());
      final Entry<Producer<String>, Producer<String>> e = p.entrySet().iterator().next();
      Assert.assertEquals("opKey", e.getKey().produce());
      Assert.assertEquals("opValue", e.getValue().produce());
   }

   @Test(expected = NullPointerException.class)
   public void testTestReadHeadersNullOperationConfig()
   {
      when(this.config.getRead()).thenReturn(null);
      final Map<Producer<String>, Producer<String>> testHeaders = Collections.emptyMap();
      this.module.testReadHeaders(testHeaders);
   }

   @Test(expected = NullPointerException.class)
   public void testTestReadHeadersNullTestHeaders()
   {
      when(this.config.getRead()).thenReturn(new OperationConfig());
      this.module.testReadHeaders(null);
   }

   @Test
   public void testTestReadHeadersDefault()
   {
      when(this.config.getRead()).thenReturn(new OperationConfig());
      final Map<Producer<String>, Producer<String>> testHeaders =
            new LinkedHashMap<Producer<String>, Producer<String>>();
      testHeaders.put(Producers.of("key"), Producers.of("value"));

      final Map<Producer<String>, Producer<String>> p = this.module.testReadHeaders(testHeaders);
      Assert.assertNotNull(p);
      Assert.assertEquals(1, p.size());
      final Entry<Producer<String>, Producer<String>> e = p.entrySet().iterator().next();
      Assert.assertEquals("key", e.getKey().produce());
      Assert.assertEquals("value", e.getValue().produce());
   }

   @Test
   public void testTestReadHeadersOverride()
   {
      final OperationConfig operationConfig = mock(OperationConfig.class);
      final Map<String, String> operationHeaders = new LinkedHashMap<String, String>();
      operationHeaders.put("opKey", "opValue");
      when(operationConfig.getHeaders()).thenReturn(operationHeaders);
      when(this.config.getRead()).thenReturn(operationConfig);

      final Map<Producer<String>, Producer<String>> testHeaders =
            new LinkedHashMap<Producer<String>, Producer<String>>();
      testHeaders.put(Producers.of("key"), Producers.of("value"));

      final Map<Producer<String>, Producer<String>> p = this.module.testReadHeaders(testHeaders);
      Assert.assertNotNull(p);
      Assert.assertEquals(1, p.size());
      final Entry<Producer<String>, Producer<String>> e = p.entrySet().iterator().next();
      Assert.assertEquals("opKey", e.getKey().produce());
      Assert.assertEquals("opValue", e.getValue().produce());
   }

   @Test(expected = NullPointerException.class)
   public void testTestDeleteHeadersNullOperationConfig()
   {
      when(this.config.getDelete()).thenReturn(null);
      final Map<Producer<String>, Producer<String>> testHeaders = Collections.emptyMap();
      this.module.testDeleteHeaders(testHeaders);
   }

   @Test(expected = NullPointerException.class)
   public void testTestDeleteHeadersNullTestHeaders()
   {
      when(this.config.getDelete()).thenReturn(new OperationConfig());
      this.module.testDeleteHeaders(null);
   }

   @Test
   public void testTestDeleteHeadersDefault()
   {
      when(this.config.getDelete()).thenReturn(new OperationConfig());
      final Map<Producer<String>, Producer<String>> testHeaders =
            new LinkedHashMap<Producer<String>, Producer<String>>();
      testHeaders.put(Producers.of("key"), Producers.of("value"));

      final Map<Producer<String>, Producer<String>> p = this.module.testDeleteHeaders(testHeaders);
      Assert.assertNotNull(p);
      Assert.assertEquals(1, p.size());
      final Entry<Producer<String>, Producer<String>> e = p.entrySet().iterator().next();
      Assert.assertEquals("key", e.getKey().produce());
      Assert.assertEquals("value", e.getValue().produce());
   }

   @Test
   public void testTestDeleteHeadersOverride()
   {
      final OperationConfig operationConfig = mock(OperationConfig.class);
      final Map<String, String> operationHeaders = new LinkedHashMap<String, String>();
      operationHeaders.put("opKey", "opValue");
      when(operationConfig.getHeaders()).thenReturn(operationHeaders);
      when(this.config.getDelete()).thenReturn(operationConfig);

      final Map<Producer<String>, Producer<String>> testHeaders =
            new LinkedHashMap<Producer<String>, Producer<String>>();
      testHeaders.put(Producers.of("key"), Producers.of("value"));

      final Map<Producer<String>, Producer<String>> p = this.module.testDeleteHeaders(testHeaders);
      Assert.assertNotNull(p);
      Assert.assertEquals(1, p.size());
      final Entry<Producer<String>, Producer<String>> e = p.entrySet().iterator().next();
      Assert.assertEquals("opKey", e.getKey().produce());
      Assert.assertEquals("opValue", e.getValue().produce());
   }

   @Test(expected = NullPointerException.class)
   public void testTestEntityNullFilesizeSelection()
   {
      when(this.config.getFilesizeSelection()).thenReturn(null);
      this.module.testEntity();
   }

   @Test(expected = NullPointerException.class)
   public void testTestEntityNullFilesize()
   {
      when(this.config.getFilesizeSelection()).thenReturn(CollectionAlgorithmType.ROUNDROBIN);
      when(this.config.getFilesize()).thenReturn(null);
      this.module.testEntity();
   }

   @Test(expected = IllegalArgumentException.class)
   public void testTestEntityEmptyFilesize()
   {
      when(this.config.getFilesizeSelection()).thenReturn(CollectionAlgorithmType.ROUNDROBIN);
      final List<FilesizeConfig> filesize = new ArrayList<FilesizeConfig>();
      when(this.config.getFilesize()).thenReturn(filesize);
      this.module.testEntity();
   }

   @Test(expected = IllegalArgumentException.class)
   public void testTestEntityPoissonDistribution()
   {
      when(this.config.getFilesizeSelection()).thenReturn(CollectionAlgorithmType.ROUNDROBIN);
      final FilesizeConfig f = mock(FilesizeConfig.class);
      when(f.getDistribution()).thenReturn(DistributionType.POISSON);
      when(f.getAverageUnit()).thenReturn(SizeUnit.MEBIBYTES);
      when(f.getSpreadUnit()).thenReturn(SizeUnit.MEBIBYTES);
      final List<FilesizeConfig> filesize = new ArrayList<FilesizeConfig>();
      filesize.add(f);
      when(this.config.getFilesize()).thenReturn(filesize);
      this.module.testEntity();
   }

   @Test(expected = NullPointerException.class)
   public void testTestEntityNullSource()
   {
      when(this.config.getFilesizeSelection()).thenReturn(CollectionAlgorithmType.ROUNDROBIN);
      final List<FilesizeConfig> filesize = new ArrayList<FilesizeConfig>();
      filesize.add(new FilesizeConfig(10.0));
      when(this.config.getSource()).thenReturn(null);
      when(this.config.getFilesize()).thenReturn(filesize);
      this.module.testEntity();
   }

   @Test(expected = IllegalArgumentException.class)
   public void testTestEntityNoneSource()
   {
      when(this.config.getFilesizeSelection()).thenReturn(CollectionAlgorithmType.ROUNDROBIN);
      final List<FilesizeConfig> filesize = new ArrayList<FilesizeConfig>();
      filesize.add(new FilesizeConfig(10.0));
      when(this.config.getSource()).thenReturn(EntityType.NONE);
      when(this.config.getFilesize()).thenReturn(filesize);
      this.module.testEntity();
   }

   @Test
   public void testTestEntitySingleFilesizeZeroesSource()
   {
      when(this.config.getFilesizeSelection()).thenReturn(CollectionAlgorithmType.ROUNDROBIN);
      final List<FilesizeConfig> filesize = new ArrayList<FilesizeConfig>();
      filesize.add(new FilesizeConfig(10.0));
      when(this.config.getSource()).thenReturn(EntityType.ZEROES);
      when(this.config.getFilesize()).thenReturn(filesize);
      final Producer<Entity> p = this.module.testEntity();

      Assert.assertNotNull(p);
      final Entity e = p.produce();
      Assert.assertEquals(EntityType.ZEROES, e.getType());
      Assert.assertEquals(SizeUnit.MEBIBYTES.toBytes(10), e.getSize());
   }

   @Test
   public void testTestEntitySingleFilesizeRandomSource()
   {
      when(this.config.getFilesizeSelection()).thenReturn(CollectionAlgorithmType.ROUNDROBIN);
      final List<FilesizeConfig> filesize = new ArrayList<FilesizeConfig>();
      filesize.add(new FilesizeConfig(10.0));
      when(this.config.getSource()).thenReturn(EntityType.RANDOM);
      when(this.config.getFilesize()).thenReturn(filesize);
      final Producer<Entity> p = this.module.testEntity();

      Assert.assertNotNull(p);
      final Entity e = p.produce();
      Assert.assertEquals(EntityType.RANDOM, e.getType());
      Assert.assertEquals(SizeUnit.MEBIBYTES.toBytes(10), e.getSize());
   }

   @Test
   public void testTestEntityMultipleFilesizeRoundRobin()
   {
      when(this.config.getFilesizeSelection()).thenReturn(CollectionAlgorithmType.ROUNDROBIN);
      final List<FilesizeConfig> filesize = new ArrayList<FilesizeConfig>();
      filesize.add(new FilesizeConfig(10.0));
      filesize.add(new FilesizeConfig(25.0));
      when(this.config.getSource()).thenReturn(EntityType.RANDOM);
      when(this.config.getFilesize()).thenReturn(filesize);
      final Producer<Entity> p = this.module.testEntity();

      Assert.assertNotNull(p);
      for (int i = 0; i < 100; i++)
      {
         Assert.assertEquals(SizeUnit.MEBIBYTES.toBytes(10), p.produce().getSize());
         Assert.assertEquals(SizeUnit.MEBIBYTES.toBytes(25), p.produce().getSize());
      }
   }

   @Test(expected = AssertionError.class)
   public void testTestEntityMultipleFilesizeRandom()
   {
      when(this.config.getFilesizeSelection()).thenReturn(CollectionAlgorithmType.RANDOM);
      final List<FilesizeConfig> filesize = new ArrayList<FilesizeConfig>();
      filesize.add(new FilesizeConfig(10.0));
      filesize.add(new FilesizeConfig(25.0));
      when(this.config.getSource()).thenReturn(EntityType.RANDOM);
      when(this.config.getFilesize()).thenReturn(filesize);
      final Producer<Entity> p = this.module.testEntity();

      Assert.assertNotNull(p);
      for (int i = 0; i < 100; i++)
      {
         // Should not exhibit roundrobin behavior over a large sample, expect assertion error
         Assert.assertEquals(SizeUnit.MEBIBYTES.toBytes(10), p.produce().getSize());
         Assert.assertEquals(SizeUnit.MEBIBYTES.toBytes(25), p.produce().getSize());
      }
   }

   @Test(expected = NullPointerException.class)
   public void testTestObjectFileLocationNullLocation() throws IOException
   {
      final ObjectManagerConfig objectManagerConfig = mock(ObjectManagerConfig.class);
      when(objectManagerConfig.getObjectFileLocation()).thenReturn(null);
      when(this.config.getObjectManager()).thenReturn(objectManagerConfig);

      this.module.testObjectFileLocation();
   }

   @Test(expected = IllegalArgumentException.class)
   public void testTestObjectFileLocationEmptyLocation() throws IOException
   {
      final ObjectManagerConfig objectManagerConfig = mock(ObjectManagerConfig.class);
      when(objectManagerConfig.getObjectFileLocation()).thenReturn("");
      when(this.config.getObjectManager()).thenReturn(objectManagerConfig);

      this.module.testObjectFileLocation();
   }

   @Test(expected = IllegalArgumentException.class)
   public void testTestObjectFileLocationExistingIsFile() throws IOException
   {
      final ObjectManagerConfig objectManagerConfig = mock(ObjectManagerConfig.class);
      final File existing = File.createTempFile("existing", null);
      when(objectManagerConfig.getObjectFileLocation()).thenReturn(existing.toString());
      when(this.config.getObjectManager()).thenReturn(objectManagerConfig);

      this.module.testObjectFileLocation();
   }

   @Test
   public void testTestObjectFileLocationNonExisting() throws IOException
   {
      final ObjectManagerConfig objectManagerConfig = mock(ObjectManagerConfig.class);
      final File existing = Files.createTempDir();
      final File nonExisting = new File(existing, String.valueOf(System.nanoTime()));
      when(objectManagerConfig.getObjectFileLocation()).thenReturn(nonExisting.toString());
      when(this.config.getObjectManager()).thenReturn(objectManagerConfig);

      this.module.testObjectFileLocation();
   }

   @Test(expected = NullPointerException.class)
   public void testTestObjectFileNameNullContainer()
   {
      this.module.testObjectFileName(null, Api.S3);
   }

   @Test(expected = NullPointerException.class)
   public void testTestObjectFileNameNullApi()
   {
      this.module.testObjectFileName(Producers.of("container"), null);
   }

   @Test
   public void testTestObjectFileNameNullObjectFileName()
   {
      final ObjectManagerConfig objectManagerConfig = mock(ObjectManagerConfig.class);
      when(objectManagerConfig.getObjectFileName()).thenReturn(null);
      when(this.config.getObjectManager()).thenReturn(objectManagerConfig);
      final String name = this.module.testObjectFileName(Producers.of("container"), Api.S3);

      Assert.assertEquals("container-s3", name);
   }

   @Test
   public void testTestObjectFileNameEmptyObjectFileName()
   {
      final ObjectManagerConfig objectManagerConfig = mock(ObjectManagerConfig.class);
      when(objectManagerConfig.getObjectFileName()).thenReturn("");
      when(this.config.getObjectManager()).thenReturn(objectManagerConfig);
      final String name = this.module.testObjectFileName(Producers.of("container"), Api.S3);

      Assert.assertEquals("container-s3", name);
   }

   @Test
   public void testTestObjectFileName()
   {
      final ObjectManagerConfig objectManagerConfig = mock(ObjectManagerConfig.class);
      when(objectManagerConfig.getObjectFileName()).thenReturn("myObjectFileName");
      when(this.config.getObjectManager()).thenReturn(objectManagerConfig);
      final String name = this.module.testObjectFileName(Producers.of("container"), Api.S3);

      Assert.assertEquals("myObjectFileName", name);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testTestWriteWeightNegative()
   {
      when(this.config.getWrite()).thenReturn(new OperationConfig(-1.0));
      when(this.config.getRead()).thenReturn(new OperationConfig(50.0));
      when(this.config.getDelete()).thenReturn(new OperationConfig(50.0));

      this.module.testWriteWeight();
   }

   @Test
   public void testTestWriteWeightZero()
   {
      when(this.config.getWrite()).thenReturn(new OperationConfig(0.0));
      when(this.config.getRead()).thenReturn(new OperationConfig(50.0));
      when(this.config.getDelete()).thenReturn(new OperationConfig(50.0));

      Assert.assertEquals(0.0, this.module.testWriteWeight(), ERR);
   }

   @Test
   public void testTestWriteWeightPositive()
   {
      when(this.config.getWrite()).thenReturn(new OperationConfig(50.0));
      when(this.config.getRead()).thenReturn(new OperationConfig(50.0));
      when(this.config.getDelete()).thenReturn(new OperationConfig(50.0));

      Assert.assertEquals(50.0, this.module.testWriteWeight(), ERR);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testTestWriteWeightGreaterThan100()
   {
      when(this.config.getWrite()).thenReturn(new OperationConfig(101.0));
      when(this.config.getRead()).thenReturn(new OperationConfig(50.0));
      when(this.config.getDelete()).thenReturn(new OperationConfig(50.0));

      this.module.testWriteWeight();
   }

   @Test
   public void testTestWriteWeightAllZero()
   {
      when(this.config.getWrite()).thenReturn(new OperationConfig(0.0));
      when(this.config.getRead()).thenReturn(new OperationConfig(0.0));
      when(this.config.getDelete()).thenReturn(new OperationConfig(0.0));

      Assert.assertEquals(100.0, this.module.testWriteWeight(), ERR);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testTestReadWeightNegative()
   {
      when(this.config.getRead()).thenReturn(new OperationConfig(-1.0));
      this.module.testReadWeight();
   }

   @Test
   public void testTestReadWeightZero()
   {
      when(this.config.getRead()).thenReturn(new OperationConfig(0.0));
      Assert.assertEquals(0.0, this.module.testReadWeight(), ERR);
   }

   @Test
   public void testTestReadWeightPositive()
   {
      when(this.config.getRead()).thenReturn(new OperationConfig(50.0));
      Assert.assertEquals(50.0, this.module.testReadWeight(), ERR);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testTestReadWeightGreaterThan100()
   {
      when(this.config.getRead()).thenReturn(new OperationConfig(101.0));
      this.module.testReadWeight();
   }

   @Test(expected = IllegalArgumentException.class)
   public void testTestDeleteWeightNegative()
   {
      when(this.config.getDelete()).thenReturn(new OperationConfig(-1.0));
      this.module.testDeleteWeight();
   }

   @Test
   public void testTestDeleteWeightZero()
   {
      when(this.config.getDelete()).thenReturn(new OperationConfig(0.0));
      Assert.assertEquals(0.0, this.module.testDeleteWeight(), ERR);
   }

   @Test
   public void testTestDeleteWeightPositive()
   {
      when(this.config.getDelete()).thenReturn(new OperationConfig(50.0));
      Assert.assertEquals(50.0, this.module.testDeleteWeight(), ERR);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testTestDeleteWeightGreaterThan100()
   {
      when(this.config.getDelete()).thenReturn(new OperationConfig(101.0));
      this.module.testDeleteWeight();
   }

   @Test(expected = NullPointerException.class)
   public void testTestSchedulerNullEventBus()
   {
      this.module.testScheduler(null);
   }

   @Test(expected = NullPointerException.class)
   public void testTestSchedulerNullConcurrencyConfig()
   {
      when(this.config.getConcurrency()).thenReturn(null);
      this.module.testScheduler(new EventBus());
   }

   @Test(expected = NullPointerException.class)
   public void testTestSchedulerNullConcurrencyType()
   {
      final ConcurrencyConfig concurrencyConfig = mock(ConcurrencyConfig.class);
      when(concurrencyConfig.getType()).thenReturn(null);
      when(this.config.getConcurrency()).thenReturn(concurrencyConfig);
      this.module.testScheduler(new EventBus());
   }

   @Test(expected = NullPointerException.class)
   public void testTestSchedulerNullDistributionType()
   {
      final ConcurrencyConfig concurrencyConfig = mock(ConcurrencyConfig.class);
      when(concurrencyConfig.getType()).thenReturn(ConcurrencyType.THREADS);
      when(concurrencyConfig.getDistribution()).thenReturn(null);
      when(this.config.getConcurrency()).thenReturn(concurrencyConfig);
      this.module.testScheduler(new EventBus());
   }

   @Test(expected = IllegalArgumentException.class)
   public void testTestSchedulerInvalidDistributionType()
   {
      final ConcurrencyConfig concurrencyConfig = mock(ConcurrencyConfig.class);
      when(concurrencyConfig.getType()).thenReturn(ConcurrencyType.IOPS);
      when(concurrencyConfig.getDistribution()).thenReturn(DistributionType.NORMAL);
      when(this.config.getConcurrency()).thenReturn(concurrencyConfig);
      this.module.testScheduler(new EventBus());
   }

   @Test
   public void testTestSchedulerThreads()
   {
      final ConcurrencyConfig concurrencyConfig = mock(ConcurrencyConfig.class);
      when(concurrencyConfig.getType()).thenReturn(ConcurrencyType.THREADS);
      when(concurrencyConfig.getDistribution()).thenReturn(DistributionType.UNIFORM);
      when(concurrencyConfig.getCount()).thenReturn(1.0);
      when(this.config.getConcurrency()).thenReturn(concurrencyConfig);
      final Scheduler s = this.module.testScheduler(new EventBus());

      Assert.assertTrue(s instanceof ConcurrentRequestScheduler);
   }

   @Test
   public void testTestSchedulerIOps()
   {
      final ConcurrencyConfig concurrencyConfig = mock(ConcurrencyConfig.class);
      when(concurrencyConfig.getType()).thenReturn(ConcurrencyType.IOPS);
      when(concurrencyConfig.getDistribution()).thenReturn(DistributionType.UNIFORM);
      when(concurrencyConfig.getCount()).thenReturn(1.0);
      when(concurrencyConfig.getUnit()).thenReturn(TimeUnit.SECONDS);
      when(this.config.getConcurrency()).thenReturn(concurrencyConfig);
      final Scheduler s = this.module.testScheduler(new EventBus());

      Assert.assertTrue(s instanceof RequestRateScheduler);
   }
}
