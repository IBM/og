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
// Date: Jul 8, 2014
// ---------------------

package com.cleversafe.og.client;

import java.net.URI;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolException;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.protocol.HttpContext;

public class CustomRedirectStrategy extends DefaultRedirectStrategy
{
   private static final String[] REDIRECT_METHODS = new String[]{
         HttpGet.METHOD_NAME,
         HttpHead.METHOD_NAME,
         HttpPost.METHOD_NAME,
         HttpPut.METHOD_NAME,
         HttpDelete.METHOD_NAME
   };

   @Override
   protected boolean isRedirectable(final String method)
   {
      for (final String m : REDIRECT_METHODS)
      {
         if (m.equalsIgnoreCase(method))
         {
            return true;
         }
      }
      return false;
   }

   @Override
   public HttpUriRequest getRedirect(
         final HttpRequest request,
         final HttpResponse response,
         final HttpContext context) throws ProtocolException
   {
      final URI uri = getLocationURI(request, response, context);
      final RequestBuilder builder = RequestBuilder.copy(request)
            .setUri(uri)
            // must remove any applied content headers as they will be re-applied
            .removeHeaders("Content-Length")
            .removeHeaders("Transfer-Encoding");

      if (request instanceof HttpEntityEnclosingRequest)
      {
         try
         {
            // reset InputStream so it can be reused for redirected request
            ((HttpEntityEnclosingRequest) request).getEntity().getContent().reset();
         }
         catch (final Exception e)
         {
            throw new ProtocolException("Unable to reset InputStream for redirected request", e);
         }
      }
      return builder.build();
   }
}
