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
// Date: Jul 9, 2014
// ---------------------

package com.cleversafe.og.s3;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.cleversafe.og.api.Method;
import com.cleversafe.og.api.Request;
import com.cleversafe.og.http.Headers;
import com.cleversafe.og.http.HttpRequest;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;

@RunWith(DataProviderRunner.class)
public class AWSAuthV2Test
{
   private static final String AWS_ACCESS_KEY_ID = "AKIAIOSFODNN7EXAMPLE";
   private static final String AWS_SECRET_ACCESS_KEY = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY";
   private Request request;
   private AWSAuthV2 auth;

   @Before
   public void before()
   {
      this.request = mock(Request.class);
      this.auth = new AWSAuthV2();
   }

   @DataProvider
   public static Object[][] provideCanonicalizedAmzHeaders()
   {
      return new Object[][]{
            {ImmutableMap.of(), ""},
            {ImmutableMap.of("Foo", "Bar", "Baz", "test"), ""},
            {ImmutableMap.of("x-amz-foo", "bar"), "x-amz-foo:bar\n"},
            {ImmutableMap.of("x-AmZ-Foo", "bar"), "x-amz-foo:bar\n"},
            {ImmutableMap.of("x-amz-foo", "Bar"), "x-amz-foo:Bar\n"},
            {ImmutableMap.of(" x-amz-foo ", "bar"), "x-amz-foo:bar\n"},
            {ImmutableMap.of("x-amz-foo", " bar "), "x-amz-foo:bar\n"},
            {ImmutableMap.of("x-amz-date", "datexyz"), ""},
            {ImmutableMap.builder()
                  .put("x-amz-date", "datexyz")
                  .put("x-AMz-foo", "foo")
                  .put("x-amz-Baz", " baz  ")
                  .put(" x-amz-bar  ", "bar")
                  .put("Date", "datexyz")
                  .put("Content-Length", "1024")
                  .build(),
                  "x-amz-bar:bar\nx-amz-baz:baz\nx-amz-foo:foo\n"}
      };
   }

   @Test
   @UseDataProvider("provideCanonicalizedAmzHeaders")
   public void canonicalizedAmzHeaders(
         final Map<String, String> headers,
         final String canonicalizedAmzHeaders)
   {
      when(this.request.headers()).thenReturn(headers);
      assertThat(this.auth.canonicalizedAmzHeaders(this.request), is(canonicalizedAmzHeaders));
   }

   @DataProvider
   public static Object[][] provideCanonicalizedResource()
   {
      return new Object[][]{
            {"/container/object", "/container/object"},
            {"/container/object?foo=bar&baz=test", "/container/object"},
            {"/container/object?torrent", "/container/object?torrent"},
            {"/container/object?uploadId=1", "/container/object?uploadId=1"},
            {"/container/object?uploadId=1&torrent&foo=bar&website",
                  "/container/object?torrent&uploadId=1&website"}
      };
   }

   @Test
   @UseDataProvider("provideCanonicalizedResource")
   public void canonicalizedResource(final String uri, final String canonicalizedResource)
         throws URISyntaxException
   {
      when(this.request.getUri()).thenReturn(new URI(uri));
      assertThat(this.auth.canonicalizedResource(this.request), is(canonicalizedResource));
   }

   @DataProvider
   public static Object[][] provideSplitQueryParameters()
   {
      final Map<String, String> single = Maps.newHashMap();
      single.put("torrent", null);
      final Map<String, String> multiple = Maps.newHashMap(single);
      multiple.put("uploadId", "1");

      return new Object[][]{
            {null, 0, ImmutableMap.of()},
            {"", 0, ImmutableMap.of()},
            {"torrent", 1, single},
            {"uploadId=", 1, ImmutableMap.of("uploadId", "")},
            {"uploadId=1", 1, ImmutableMap.of("uploadId", "1")},
            {"uploadId=1&torrent", 2, multiple}
      };
   }

   @Test
   @UseDataProvider("provideSplitQueryParameters")
   public void splitQueryParameters(
         final String query,
         final int size,
         final Map<String, String> queryParameters)
   {
      final Map<String, String> split = this.auth.splitQueryParameters(query);
      assertThat(split.size(), is(size));
      for (final Entry<String, String> e : queryParameters.entrySet())
      {
         assertThat(split, hasEntry(e.getKey(), e.getValue()));
      }
   }

   @DataProvider
   public static Object[][] provideJoinQueryParameters()
   {
      final SortedMap<String, String> single = Maps.newTreeMap();
      single.put("torrent", null);
      final Map<String, String> multiple = Maps.newTreeMap(single);
      multiple.put("uploadId", "1");

      return new Object[][]{
            {null, ""},
            {ImmutableMap.of(), ""},
            {single, "torrent"},
            {ImmutableMap.of("uploadId", "1"), "uploadId=1"},
            {multiple, "torrent&uploadId=1"}
      };
   }

   @Test
   @UseDataProvider("provideJoinQueryParameters")
   public void joinQueryParameters(final Map<String, String> queryParameters, final String query)
   {
      final String join = this.auth.joinQueryParameters(queryParameters);
      assertThat(join, is(query));
   }

   @DataProvider
   public static Object[][] provideExamples() throws URISyntaxException
   {
      return new Object[][]{get(), put(), list(), delete()};
   }

   public static Object[] get() throws URISyntaxException
   {
      final Request request =
            new HttpRequest.Builder(Method.GET, new URI("/johnsmith/photos/puppy.jpg"))
                  .withHeader("Date", "Tue, 27 Mar 2007 19:36:42 +0000")
                  .withMetadata(Headers.X_OG_USERNAME, AWS_ACCESS_KEY_ID)
                  .withMetadata(Headers.X_OG_PASSWORD, AWS_SECRET_ACCESS_KEY)
                  .build();
      final String toSign = "GET\n\n\nTue, 27 Mar 2007 19:36:42 +0000\n/johnsmith/photos/puppy.jpg";
      final String header = "AWS AKIAIOSFODNN7EXAMPLE:bWq2s1WEIj+Ydj0vQ697zp+IXMU=";
      return new Object[]{request, toSign, header};
   }

   public static Object[] put() throws URISyntaxException
   {
      final Request request =
            new HttpRequest.Builder(Method.PUT, new URI("/johnsmith/photos/puppy.jpg"))
                  .withHeader("Content-Type", "image/jpeg")
                  .withHeader("Content-Length", "94328")
                  .withHeader("Date", "Tue, 27 Mar 2007 21:15:45 +0000")
                  .withMetadata(Headers.X_OG_USERNAME, AWS_ACCESS_KEY_ID)
                  .withMetadata(Headers.X_OG_PASSWORD, AWS_SECRET_ACCESS_KEY)
                  .build();
      final String toSign =
            "PUT\n\nimage/jpeg\nTue, 27 Mar 2007 21:15:45 +0000\n/johnsmith/photos/puppy.jpg";
      final String header = "AWS AKIAIOSFODNN7EXAMPLE:MyyxeRY7whkBe+bq8fHCL/2kKUg=";
      return new Object[]{request, toSign, header};
   }

   public static Object[] list() throws URISyntaxException
   {
      final Request request =
            new HttpRequest.Builder(Method.GET, new URI(
                  "/johnsmith/?prefix=photos&max-keys=50&marker=puppy"))
                  .withHeader("User-Agent", "Mozilla/5.0")
                  .withHeader("Date", "Tue, 27 Mar 2007 19:42:41 +0000")
                  .withMetadata(Headers.X_OG_USERNAME, AWS_ACCESS_KEY_ID)
                  .withMetadata(Headers.X_OG_PASSWORD, AWS_SECRET_ACCESS_KEY)
                  .build();
      final String toSign = "GET\n\n\nTue, 27 Mar 2007 19:42:41 +0000\n/johnsmith/";
      final String header = "AWS AKIAIOSFODNN7EXAMPLE:htDYFYduRNen8P9ZfE/s9SuKy0U=";
      return new Object[]{request, toSign, header};
   }

   public static Object[] delete() throws URISyntaxException
   {
      final Request request =
            new HttpRequest.Builder(Method.DELETE, new URI("/johnsmith/photos/puppy.jpg"))
                  .withHeader("User-Agent", "dotnet")
                  .withHeader("Host", "s3.amazonaws.com")
                  .withHeader("Date", "Tue, 27 Mar 2007 21:20:27 +0000")
                  .withHeader("x-amz-date", "Tue, 27 Mar 2007 21:20:26 +0000")
                  .withMetadata(Headers.X_OG_USERNAME, AWS_ACCESS_KEY_ID)
                  .withMetadata(Headers.X_OG_PASSWORD, AWS_SECRET_ACCESS_KEY)
                  .build();
      final String toSign =
            "DELETE\n\n\nTue, 27 Mar 2007 21:20:26 +0000\n/johnsmith/photos/puppy.jpg";
      final String header = "AWS AKIAIOSFODNN7EXAMPLE:lx3byBScXR6KzyMaifNkardMwNk=";
      return new Object[]{request, toSign, header};
   }

   @Test
   @UseDataProvider("provideExamples")
   public void testSigning(final Request request, final String toSign, final String header)
   {
      assertThat(this.auth.stringToSign(request), is(toSign));
      assertThat(this.auth.nextAuthorizationHeader(request), is(header));
   }
}
