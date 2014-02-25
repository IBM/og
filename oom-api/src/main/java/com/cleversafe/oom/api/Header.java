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
// Date: Feb 25, 2014
// ---------------------

package com.cleversafe.oom.api;

/**
 * A key value pair that describes an http header.
 */
public interface Header
{
   /**
    * Gets the key of this header.
    * 
    * @return the key of this header
    */
   String getKey();

   /**
    * Gets the value of this header.
    * 
    * @return the value of this header
    */
   String getValue();
}
