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
// Date: Jun 18, 2014
// ---------------------

package com.cleversafe.oom.s3.auth;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cleversafe.oom.http.HttpRequest;
import com.cleversafe.oom.operation.Method;
import com.cleversafe.oom.operation.Request;
import com.cleversafe.oom.util.producer.Producers;

// test data pulled from examples aws auth signing v2 at:
// http://docs.aws.amazon.com/AmazonS3/latest/dev/RESTAuthentication.html
@RunWith(Parameterized.class)
public class AWSAuthV2Test
{
   private static Logger _logger = LoggerFactory.getLogger(AWSAuthV2Test.class);
   private static String AWS_ACCESS_KEY_ID = "AKIAIOSFODNN7EXAMPLE";
   private static String AWS_SECRET_ACCESS_KEY = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY";
   private final Request request;
   private final String stringToSign;
   private final String nextAuthorizationHeader;

   public AWSAuthV2Test(
         final Request request,
         final String stringToSign,
         final String nextAuthorizationHeader)
   {
      this.request = request;
      this.stringToSign = stringToSign;
      this.nextAuthorizationHeader = nextAuthorizationHeader;
   }

   @Parameters
   public static Collection<Object[]> generateData() throws URISyntaxException
   {
      return Arrays.asList(new Object[][]{generateGET(), generatePUT(), generateList(),
            generateDELETE()});
   }

   public static Object[] generateGET() throws URISyntaxException
   {
      final Request request = HttpRequest.custom()
            .withMethod(Method.GET)
            .withURI(new URI("/johnsmith/photos/puppy.jpg"))
            .withHeader("Date", "Tue, 27 Mar 2007 19:36:42 +0000")
            .build();
      final String stringToSign =
            "GET\n\n\nTue, 27 Mar 2007 19:36:42 +0000\n/johnsmith/photos/puppy.jpg";
      final String nextAuthorizationHeader =
            "AWS AKIAIOSFODNN7EXAMPLE:bWq2s1WEIj+Ydj0vQ697zp+IXMU=";
      return new Object[]{request, stringToSign, nextAuthorizationHeader};
   }

   public static Object[] generatePUT() throws URISyntaxException
   {
      final Request request = HttpRequest.custom()
            .withMethod(Method.PUT)
            .withURI(new URI("/johnsmith/photos/puppy.jpg"))
            .withHeader("Content-Type", "image/jpeg")
            .withHeader("Content-Length", "94328")
            .withHeader("Date", "Tue, 27 Mar 2007 21:15:45 +0000")
            .build();
      final String stringToSign =
            "PUT\n\nimage/jpeg\nTue, 27 Mar 2007 21:15:45 +0000\n/johnsmith/photos/puppy.jpg";
      final String nextAuthorizationHeader =
            "AWS AKIAIOSFODNN7EXAMPLE:MyyxeRY7whkBe+bq8fHCL/2kKUg=";
      return new Object[]{request, stringToSign, nextAuthorizationHeader};
   }

   public static Object[] generateList() throws URISyntaxException
   {
      final Request request = HttpRequest.custom()
            .withMethod(Method.GET)
            .withURI(new URI("/johnsmith/?prefix=photos&max-keys=50&marker=puppy"))
            .withHeader("User-Agent", "Mozilla/5.0")
            .withHeader("Date", "Tue, 27 Mar 2007 19:42:41 +0000")
            .build();
      final String stringToSign =
            "GET\n\n\nTue, 27 Mar 2007 19:42:41 +0000\n/johnsmith/";
      final String nextAuthorizationHeader =
            "AWS AKIAIOSFODNN7EXAMPLE:htDYFYduRNen8P9ZfE/s9SuKy0U=";
      return new Object[]{request, stringToSign, nextAuthorizationHeader};
   }

   public static Object[] generateDELETE() throws URISyntaxException
   {
      final Request request = HttpRequest.custom()
            .withMethod(Method.DELETE)
            .withURI(new URI("/johnsmith/photos/puppy.jpg"))
            .withHeader("User-Agent", "dotnet")
            .withHeader("Host", "s3.amazonaws.com")
            .withHeader("Date", "Tue, 27 Mar 2007 21:20:27 +0000")
            .withHeader("x-amz-date", "Tue, 27 Mar 2007 21:20:26 +0000")
            .build();
      final String stringToSign =
            "DELETE\n\n\nTue, 27 Mar 2007 21:20:26 +0000\n/johnsmith/photos/puppy.jpg";
      final String nextAuthorizationHeader =
            "AWS AKIAIOSFODNN7EXAMPLE:lx3byBScXR6KzyMaifNkardMwNk=";
      return new Object[]{request, stringToSign, nextAuthorizationHeader};
   }

   @Test
   public void testSigning()
   {
      final AWSAuthV2 auth =
            new AWSAuthV2(Producers.of(AWS_ACCESS_KEY_ID), Producers.of(AWS_SECRET_ACCESS_KEY));
      final String s = auth.stringToSign(this.request);
      final String s2 = auth.nextAuthorizationHeader(this.request).getValue();
      Assert.assertEquals(this.stringToSign, s);
      Assert.assertEquals(this.nextAuthorizationHeader, s2);
   }
}
