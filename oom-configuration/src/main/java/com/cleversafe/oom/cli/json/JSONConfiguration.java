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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cleversafe.oom.http.Scheme;

public class JSONConfiguration
{
   Scheme scheme;
   List<String> hosts;
   API api;
   String container;
   Map<String, String> headers;
   double write;
   double read;
   double delete;
   List<FileSize> filesizes;
   Concurrency concurrency;
   String username;
   String password;
   double floor;
   double ceiling;
   long capacity;
   Long operatons;
   RunTime runtime;
   Long fillFloor;
   Long fillCeiling;
   String idFile;
   int port;

   public JSONConfiguration()
   {
      this.scheme = Scheme.HTTP;
      this.hosts = new ArrayList<String>();
      this.api = API.SOH;
      this.headers = new HashMap<String, String>();
      this.filesizes = new ArrayList<FileSize>();
      this.filesizes.add(new FileSize());
      this.concurrency = new Concurrency();
      this.ceiling = 100.0;
      this.capacity = 9223372036854775807L;
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
    * @return the hosts
    */
   public List<String> getHosts()
   {
      return this.hosts;
   }

   /**
    * @return the api
    */
   public API getApi()
   {
      return this.api;
   }

   /**
    * @return the container
    */
   public String getContainer()
   {
      return this.container;
   }

   /**
    * @return the headers
    */
   public Map<String, String> getHeaders()
   {
      return this.headers;
   }

   /**
    * @return the write
    */
   public double getWrite()
   {
      return this.write;
   }

   /**
    * @return the read
    */
   public double getRead()
   {
      return this.read;
   }

   /**
    * @return the delete
    */
   public double getDelete()
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
   public double getFloor()
   {
      return this.floor;
   }

   /**
    * @return the ceiling
    */
   public double getCeiling()
   {
      return this.ceiling;
   }

   /**
    * @return the capacity
    */
   public long getCapacity()
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
