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

package com.cleversafe.oom.cli.json;

import com.cleversafe.oom.cli.json.enums.AuthType;

public class AuthConfig
{
   private final AuthType authType;
   private String username;
   private String password;

   public AuthConfig()
   {
      this.authType = AuthType.BASIC;
   }

   /**
    * @return the authType
    */
   public AuthType getAuthType()
   {
      return this.authType;
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
