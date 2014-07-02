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
// Date: Jun 16, 2014
// ---------------------

package com.cleversafe.og.s3.auth;

import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cleversafe.og.http.auth.HttpAuth;
import com.cleversafe.og.operation.Metadata;
import com.cleversafe.og.operation.Request;
import com.google.common.io.BaseEncoding;

public class AWSAuthV2 implements HttpAuth
{
   private static final Logger _logger = LoggerFactory.getLogger(AWSAuthV2.class);
   private static final String HMAC_SHA1 = "HmacSHA1";

   public AWSAuthV2()
   {}

   @Override
   public String nextAuthorizationHeader(final Request request)
   {
      final String awsAccessKeyId = request.getMetadata(Metadata.USERNAME);
      final String awsSecretAccessKey = request.getMetadata(Metadata.PASSWORD);
      return authenticate(request, awsAccessKeyId, awsSecretAccessKey);
   }

   private String authenticate(
         final Request request,
         final String awsAccessKeyId,
         final String awsSecretAccessKey)
   {
      return new StringBuilder()
            .append("AWS ")
            .append(awsAccessKeyId)
            .append(":")
            .append(signature(request, awsSecretAccessKey))
            .toString();
   }

   private String signature(final Request request, final String awsSecretAccessKey)
   {
      return BaseEncoding.base64().encode(
            hmacSha1(awsSecretAccessKey, stringToSign(request).getBytes(StandardCharsets.UTF_8)));
   }

   public byte[] hmacSha1(final String awsSecretAccessKey, final byte[] data)
   {
      final SecretKeySpec signingKey =
            new SecretKeySpec(awsSecretAccessKey.getBytes(StandardCharsets.UTF_8), HMAC_SHA1);
      Mac mac;
      try
      {
         mac = Mac.getInstance(HMAC_SHA1);
         mac.init(signingKey);
         return mac.doFinal(data);
      }
      // FIXME what to do in case of exception?
      catch (final Exception e)
      {
         return new byte[0];
      }
   }

   public String stringToSign(final Request request)
   {
      return new StringBuilder()
            .append(request.getMethod())
            .append("\n")
            .append(getHeader(request, "Content-MD5", ""))
            .append("\n")
            .append(getHeader(request, "Content-Type", ""))
            .append("\n")
            .append(getHeader(request, "X-Amz-Date", getHeader(request, "Date", "")))
            .append("\n")
            .append(canonicalizedAmzHeaders(request))
            .append(canonicalizedResource(request))
            .toString();
   }

   // lookup by key and lower(key)
   private String getHeader(
         final Request request,
         final String key,
         final String defaultValue)
   {
      String value = request.getHeader(key);
      if (value != null)
         return value;

      value = request.getHeader(key.toLowerCase());
      if (value != null)
         return value;

      return defaultValue;
   }

   // TODO steps 3 and 4 not supported, see canonicalizing request headers
   // http://docs.aws.amazon.com/AmazonS3/latest/dev/RESTAuthentication.html
   // TODO documentation doesn't say to, but examples illustrate that x-amz-date
   // should be ignored when computing canonicalAmzHeaders. Is this correct?
   public String canonicalizedAmzHeaders(final Request request)
   {
      // create canonicalHeaders lazily, usually no x-amz- headers will be present
      SortedMap<String, String> canonicalHeaders = null;
      final Iterator<Entry<String, String>> headers = request.headers();
      while (headers.hasNext())
      {
         final Entry<String, String> header = headers.next();
         final String keyLower = header.getKey().toLowerCase();
         // ignoring x-amz-date, is this correct?
         if (keyLower.startsWith("x-amz-") && !keyLower.equals("x-amz-date"))
         {
            if (canonicalHeaders == null)
               canonicalHeaders = new TreeMap<String, String>();

            canonicalHeaders.put(keyLower, header.getValue());
         }
      }
      final StringBuilder s = new StringBuilder();
      if (canonicalHeaders != null)
      {
         for (final Entry<String, String> header : canonicalHeaders.entrySet())
         {
            s.append(header.getKey())
                  .append(":")
                  .append(header.getValue())
                  .append("\n");
         }
      }
      return s.toString();
   }

   // TODO only supports path-style requests, virtual hosts not supported
   // TODO subresources not supported: acl, lifecycle, location, logging, notification, partNumber,
   // policy, requestPayment, torrent, uploadId, uploads, versionId, versioning, versions, website
   // TODO other query params not supported, see step 4 of canonicalizing a request:
   // http://docs.aws.amazon.com/AmazonS3/latest/dev/RESTAuthentication.html
   public String canonicalizedResource(final Request request)
   {
      return request.getUri().getRawPath();
   }
}
