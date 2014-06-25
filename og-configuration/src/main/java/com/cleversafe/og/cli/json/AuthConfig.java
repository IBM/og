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
// Date: Jun 15, 2014
// ---------------------

package com.cleversafe.og.cli.json;

import com.cleversafe.og.cli.json.enums.AuthType;

public class AuthConfig
{
   private final AuthType type;
   private String username;
   private String password;

   public AuthConfig()
   {
      this.type = AuthType.BASIC;
   }

   /**
    * @return the type
    */
   public AuthType getType()
   {
      return this.type;
   }

   /**
    * @return the username
    */
   public String getUsername()
   {
      return this.username;
   }

   /**
    * @return the password
    */
   public String getPassword()
   {
      return this.password;
   }
}
