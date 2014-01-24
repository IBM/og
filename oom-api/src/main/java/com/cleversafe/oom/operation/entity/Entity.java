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

package com.cleversafe.oom.operation.entity;

import java.io.InputStream;

/**
 * A container representing a request or response body.
 */
public interface Entity
{
   /**
    * Gets the input stream for this entity
    * 
    * @return this entity's input stream
    */
   InputStream getInputStream();

   /**
    * Gets the size of this entity.
    * 
    * @return the size of this entity, or -1 if the size is not known
    */
   long getSize();
}
