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
// Date: Mar 24, 2014
// ---------------------

package com.cleversafe.oom.cli.json;

import java.util.ArrayList;
import java.util.List;

public class JSONConfiguration
{
   List<String> accessers;
   String api;
   String vault;
   Double write;
   Double read;
   Double delete;
   List<FileSize> filesizes;
   Concurrency concurrency;
   String username;
   String password;
   Double floor;
   Double ceiling;
   Double capacity;
   Long operatons;
   RunTime runtime;
   Long fillFloor;
   Long fillCeiling;
   String idFile;
   int port;

   public JSONConfiguration()
   {
      this.accessers = new ArrayList<String>();
      this.api = "soh";
      this.filesizes = new ArrayList<FileSize>();
      this.filesizes.add(new FileSize());
      this.concurrency = new Concurrency();
      this.idFile = "";
      this.port = 80;
   }
}
