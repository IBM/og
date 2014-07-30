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
// Date: Jan 24, 2014
// ---------------------

package com.cleversafe.og.operation;

/**
 * A description of an http request or response body
 * 
 * @since 1.0
 */
public interface Entity
{
   /**
    * Gets the type of this entity.
    * 
    * @return the type of this entity
    * @see EntityType
    */
   EntityType getType();

   /**
    * Gets the size of this entity.
    * 
    * @return the size of this entity
    */
   long getSize();
}
