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
// Date: Jul 15, 2014
// ---------------------

package com.cleversafe.og.soh;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.cleversafe.og.operation.Metadata;
import com.cleversafe.og.util.ResponseBodyConsumer;

public class SOHWriteResponseBodyConsumer implements ResponseBodyConsumer
{
   @Override
   public Iterator<Entry<String, String>> consume(final int statusCode, final InputStream response)
         throws IOException
   {
      if (statusCode != 201)
         return Collections.emptyIterator();
      checkNotNull(response);

      final BufferedReader reader =
            new BufferedReader(new InputStreamReader(response, StandardCharsets.UTF_8));

      final Map<String, String> metadata = new HashMap<String, String>(1);
      metadata.put(Metadata.OBJECT_NAME.toString(), reader.readLine());

      return metadata.entrySet().iterator();
   }
}
