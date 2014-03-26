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

import com.cleversafe.oom.http.Scheme;

public class JSONConfiguration
{
   Scheme scheme;
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
      this.scheme = Scheme.HTTP;
      this.accessers = new ArrayList<String>();
      this.api = "soh";
      this.filesizes = new ArrayList<FileSize>();
      this.filesizes.add(new FileSize());
      this.concurrency = new Concurrency();
      this.idFile = "";
      this.port = 80;
   }

   /**
    * @return the scheme
    */
   public Scheme getScheme()
   {
      return this.scheme;
   }

   /**
    * @return the accessers
    */
   public List<String> getAccessers()
   {
      return this.accessers;
   }

   /**
    * @return the api
    */
   public String getApi()
   {
      return this.api;
   }

   /**
    * @return the vault
    */
   public String getVault()
   {
      return this.vault;
   }

   /**
    * @return the write
    */
   public Double getWrite()
   {
      return this.write;
   }

   /**
    * @return the read
    */
   public Double getRead()
   {
      return this.read;
   }

   /**
    * @return the delete
    */
   public Double getDelete()
   {
      return this.delete;
   }

   /**
    * @return the filesizes
    */
   public List<FileSize> getFilesizes()
   {
      return this.filesizes;
   }

   /**
    * @return the concurrency
    */
   public Concurrency getConcurrency()
   {
      return this.concurrency;
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

   /**
    * @return the floor
    */
   public Double getFloor()
   {
      return this.floor;
   }

   /**
    * @return the ceiling
    */
   public Double getCeiling()
   {
      return this.ceiling;
   }

   /**
    * @return the capacity
    */
   public Double getCapacity()
   {
      return this.capacity;
   }

   /**
    * @return the operatons
    */
   public Long getOperatons()
   {
      return this.operatons;
   }

   /**
    * @return the runtime
    */
   public RunTime getRuntime()
   {
      return this.runtime;
   }

   /**
    * @return the fillFloor
    */
   public Long getFillFloor()
   {
      return this.fillFloor;
   }

   /**
    * @return the fillCeiling
    */
   public Long getFillCeiling()
   {
      return this.fillCeiling;
   }

   /**
    * @return the idFile
    */
   public String getIdFile()
   {
      return this.idFile;
   }

   /**
    * @return the port
    */
   public int getPort()
   {
      return this.port;
   }
}