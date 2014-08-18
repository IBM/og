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
// Date: Jul 12, 2014
// ---------------------

package com.cleversafe.og.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.apache.logging.log4j.core.lookup.StrLookup;
import org.junit.Test;

public class OGDatetimeLookupTest
{
   @Test
   public void testOGDatetimeLookup()
   {
      final StrLookup lookup = new OGDatetimeLookup();
      final String s1 = lookup.lookup(null);
      final String s2 = lookup.lookup(null);
      final String s3 = lookup.lookup(null, null);

      assertThat(s1, is(s2));
      assertThat(s2, is(s3));
   }
}
