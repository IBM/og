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
// Date: Apr 2, 2014
// ---------------------

package com.cleversafe.oom.http.producer;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.DatatypeConverter;

import org.junit.Assert;
import org.junit.Test;

import com.cleversafe.oom.api.Producer;
import com.cleversafe.oom.util.Pair;
import com.google.common.io.BaseEncoding;

public class BasicAuthProducerTest
{
   @Test(expected = NullPointerException.class)
   public void testNullUsername()
   {
      new BasicAuthProducer(null, "password");
   }

   @Test(expected = NullPointerException.class)
   public void testNullPassword()
   {
      new BasicAuthProducer("username", null);
   }

   @Test
   public void testBasicAuthProducer()
   {
      final List<Pair<String, String>> credentials = new ArrayList<Pair<String, String>>();
      credentials.add(new Pair<String, String>("username", ""));
      credentials.add(new Pair<String, String>("", "password"));
      credentials.add(new Pair<String, String>("username", "password"));
      for (final Pair<String, String> c : credentials)
      {
         assertValid(c);
      }
   }

   private void assertValid(final Pair<String, String> credentials)
   {
      final Producer<Pair<String, String>> b =
            new BasicAuthProducer(credentials.getKey(), credentials.getValue());
      final String cred = credentials.getKey() + ":" + credentials.getValue();
      final String java =
            DatatypeConverter.printBase64Binary(cred.getBytes(StandardCharsets.UTF_8));
      final String guava = BaseEncoding.base64().encode(cred.getBytes(StandardCharsets.UTF_8));
      final Pair<String, String> header = b.produce(null);
      Assert.assertEquals("Authorization", header.getKey());
      Assert.assertEquals("Basic " + java, header.getValue());
      Assert.assertEquals("Basic " + guava, header.getValue());
   }
}
