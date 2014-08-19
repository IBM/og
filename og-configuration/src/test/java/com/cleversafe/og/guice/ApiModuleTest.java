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
// Date: Aug 19, 2014
// ---------------------

package com.cleversafe.og.guice;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import com.cleversafe.og.api.Body;
import com.cleversafe.og.api.Request;
import com.cleversafe.og.http.Api;
import com.cleversafe.og.http.Bodies;
import com.cleversafe.og.http.Headers;
import com.cleversafe.og.http.Scheme;
import com.cleversafe.og.supplier.CachingSupplier;
import com.cleversafe.og.supplier.Suppliers;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;

@RunWith(DataProviderRunner.class)
public class ApiModuleTest
{
   @Rule
   public ExpectedException thrown = ExpectedException.none();
   private ApiModule module;
   private Supplier<Scheme> scheme;
   private Supplier<String> host;
   private Supplier<Integer> port;
   private Supplier<String> uriRoot;
   private Supplier<String> container;
   private Map<Supplier<String>, Supplier<String>> headers;
   private CachingSupplier<String> object;
   private Supplier<String> username;
   private Supplier<String> password;
   private Supplier<Body> body;

   @Before()
   public void before()
   {
      this.module = new ApiModule();
      this.scheme = Suppliers.of(Scheme.HTTP);
      this.host = Suppliers.of("127.0.0.1");
      this.port = Suppliers.of(80);
      this.uriRoot = Suppliers.of("soh");
      this.container = Suppliers.of("container");
      this.headers = Maps.newLinkedHashMap();
      this.headers.put(Suppliers.of("key"), Suppliers.of("value"));
      this.object = new CachingSupplier<String>(Suppliers.of("object"));
      this.username = Suppliers.of("username");
      this.password = Suppliers.of("password");
      this.body = Suppliers.of(Bodies.zeroes(1024));
   }

   @DataProvider
   public static Object[][] provideInvalidProvideRequest() throws URISyntaxException
   {
      return new Object[][]{
            {null, ImmutableMap.of()},
            {Suppliers.of(new URI("127.0.0.1/container/object")), null}
      };
   }

   @Test
   @UseDataProvider("provideInvalidProvideRequest")
   public void invalidProvideWrite(
         final Supplier<URI> uri,
         final Map<Supplier<String>, Supplier<String>> headers)
   {
      this.thrown.expect(NullPointerException.class);
      this.module.provideWrite(Api.S3, uri, this.object, headers, this.body, this.username,
            this.password);
   }

   @Test
   @UseDataProvider("provideInvalidProvideRequest")
   public void invalidProvideRead(
         final Supplier<URI> uri,
         final Map<Supplier<String>, Supplier<String>> headers)
   {
      this.thrown.expect(NullPointerException.class);
      this.module.provideRead(uri, this.object, headers, this.username, this.password);
   }

   @Test
   @UseDataProvider("provideInvalidProvideRequest")
   public void invalidProvideDelete(
         final Supplier<URI> uri,
         final Map<Supplier<String>, Supplier<String>> headers)
   {
      this.thrown.expect(NullPointerException.class);
      this.module.provideDelete(uri, this.object, headers, this.username, this.password);
   }

   @DataProvider
   @SuppressWarnings({"rawtypes", "unchecked"})
   public static Object[][] provideRequestData()
   {
      final CachingSupplier<String> object = new CachingSupplier<String>(Suppliers.of("object"));
      final Supplier<String> username = Suppliers.of("username");
      final Supplier<String> password = Suppliers.of("password");

      final Matcher apiMatch = hasEntry(Headers.X_OG_RESPONSE_BODY_CONSUMER, "soh.put_object");
      final Matcher objectMatch = is("object");
      final Matcher userMatch = is("username");
      final Matcher passMatch = is("password");

      return new Object[][]{
            {Api.SOH, apiMatch, object, objectMatch, username, userMatch, password, passMatch},
            {Api.S3, not(apiMatch), null, nullValue(), null, nullValue(), null, nullValue()}
      };
   }

   @Test
   @SuppressWarnings({"rawtypes", "unchecked"})
   @UseDataProvider("provideRequestData")
   public void provideWrite(
         final Api api,
         final Matcher apiMatch,
         final CachingSupplier<String> object,
         final Matcher objectMatch,
         final Supplier<String> username,
         final Matcher usernameMatch,
         final Supplier<String> password,
         final Matcher passwordMatch)
   {
      final Supplier<URI> uri = this.module.providWriteUri(this.scheme, this.host, this.port, null,
            this.container, object);
      final Request request =
            this.module.provideWrite(api, uri, object, this.headers, this.body, username, password).get();

      assertThat(request.headers(), apiMatch);
      assertThat(request.headers().get(Headers.X_OG_OBJECT_NAME), objectMatch);
      assertThat(request.headers().get(Headers.X_OG_USERNAME), usernameMatch);
      assertThat(request.headers().get(Headers.X_OG_PASSWORD), passwordMatch);
   }

   @Test
   @SuppressWarnings({"rawtypes", "unchecked"})
   @UseDataProvider("provideRequestData")
   public void provideRead(
         final Api api,
         final Matcher apiMatch,
         final CachingSupplier<String> object,
         final Matcher objectMatch,
         final Supplier<String> username,
         final Matcher usernameMatch,
         final Supplier<String> password,
         final Matcher passwordMatch)
   {
      final Supplier<URI> uri = this.module.providReadUri(this.scheme, this.host, this.port, null,
            this.container, object);
      final Request request =
            this.module.provideRead(uri, object, this.headers, username, password).get();

      assertThat(request.headers().get(Headers.X_OG_OBJECT_NAME), objectMatch);
      assertThat(request.headers().get(Headers.X_OG_USERNAME), usernameMatch);
      assertThat(request.headers().get(Headers.X_OG_PASSWORD), passwordMatch);
   }

   @Test
   @SuppressWarnings({"rawtypes", "unchecked"})
   @UseDataProvider("provideRequestData")
   public void provideDelete(
         final Api api,
         final Matcher apiMatch,
         final CachingSupplier<String> object,
         final Matcher objectMatch,
         final Supplier<String> username,
         final Matcher usernameMatch,
         final Supplier<String> password,
         final Matcher passwordMatch)
   {
      final Supplier<URI> uri =
            this.module.providDeleteUri(this.scheme, this.host, this.port, null,
                  this.container, object);
      final Request request =
            this.module.provideDelete(uri, object, this.headers, username, password).get();

      assertThat(request.headers().get(Headers.X_OG_OBJECT_NAME), objectMatch);
      assertThat(request.headers().get(Headers.X_OG_USERNAME), usernameMatch);
      assertThat(request.headers().get(Headers.X_OG_PASSWORD), passwordMatch);
   }

   @DataProvider
   public static Object[][] provideInvalidProvideUri()
   {
      @SuppressWarnings("rawtypes")
      final Supplier supplier = mock(Supplier.class);

      return new Object[][]{
            {null, supplier, supplier},
            {supplier, null, supplier},
            {supplier, supplier, null},
      };
   }

   @Test
   @UseDataProvider("provideInvalidProvideUri")
   public void invalidProvideWriteUri(
         final Supplier<Scheme> scheme,
         final Supplier<String> host,
         final Supplier<String> container)
   {
      this.thrown.expect(NullPointerException.class);
      this.module.providWriteUri(scheme, host, this.port, this.uriRoot, container, this.object);
   }

   @Test
   @UseDataProvider("provideInvalidProvideUri")
   public void invalidProvideReadUri(final Supplier<Scheme> scheme, final Supplier<String> host,
         final Supplier<String> container)
   {
      this.thrown.expect(NullPointerException.class);
      this.module.providReadUri(scheme, host, this.port, this.uriRoot, container, this.object);
   }

   @Test
   @UseDataProvider("provideInvalidProvideUri")
   public void invalidProvideDeleteUri(
         final Supplier<Scheme> scheme,
         final Supplier<String> host,
         final Supplier<String> container)
   {
      this.thrown.expect(NullPointerException.class);
      this.module.providDeleteUri(scheme, host, this.port, this.uriRoot, container, this.object);
   }

   @DataProvider
   public static Object[][] provideUriData()
   {
      final Supplier<Integer> port = Suppliers.of(8080);
      final Supplier<String> uriRoot = Suppliers.of("soh");
      final CachingSupplier<String> object = new CachingSupplier<String>(Suppliers.of("object"));

      return new Object[][]{
            {null, -1, null, null, "/container/object"},
            {port, 8080, uriRoot, object, "/soh/container/object"},
      };
   }

   @Test
   @UseDataProvider("provideUriData")
   public void provideWriteUri(
         final Supplier<Integer> port,
         final int portExpected,
         final Supplier<String> uriRoot,
         final CachingSupplier<String> object,
         final String pathExpected)
   {
      final URI uri = this.module.providWriteUri(this.scheme, this.host, port, uriRoot,
            this.container, this.object).get();
      assertThat(uri.getScheme().toUpperCase(), is(this.scheme.get().toString()));
      assertThat(uri.getHost(), is(this.host.get()));
      assertThat(uri.getPort(), is(portExpected));
      assertThat(uri.getPath(), is(pathExpected));
   }

   @Test
   @UseDataProvider("provideUriData")
   public void provideReadUri(
         final Supplier<Integer> port,
         final int portExpected,
         final Supplier<String> uriRoot,
         final CachingSupplier<String> object,
         final String pathExpected)
   {
      final URI uri = this.module.providReadUri(this.scheme, this.host, port, uriRoot,
            this.container, this.object).get();
      assertThat(uri.getScheme().toUpperCase(), is(this.scheme.get().toString()));
      assertThat(uri.getHost(), is(this.host.get()));
      assertThat(uri.getPort(), is(portExpected));
      assertThat(uri.getPath(), is(pathExpected));
   }

   @Test
   @UseDataProvider("provideUriData")
   public void provideDeleteUri(
         final Supplier<Integer> port,
         final int portExpected,
         final Supplier<String> uriRoot,
         final CachingSupplier<String> object,
         final String pathExpected)
   {
      final URI uri = this.module.providDeleteUri(this.scheme, this.host, port, uriRoot,
            this.container, this.object).get();
      assertThat(uri.getScheme().toUpperCase(), is(this.scheme.get().toString()));
      assertThat(uri.getHost(), is(this.host.get()));
      assertThat(uri.getPort(), is(portExpected));
      assertThat(uri.getPath(), is(pathExpected));
   }

   @Test
   public void provideResponseBodyConsumers()
   {
      assertThat(this.module.provideResponseBodyConsumers(), notNullValue());
   }
}
