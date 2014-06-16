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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.cleversafe.oom.http.Scheme;
import com.cleversafe.oom.operation.EntityType;
import com.cleversafe.oom.operation.OperationType;

public class JSONConfiguration
{
   Scheme scheme;
   List<String> hosts;
   Integer port;
   API api;
   String uriRoot;
   String container;
   Map<String, String> headers;
   double write;
   double read;
   double delete;
   List<FileSize> filesizes;
   EntityType source;
   Concurrency concurrency;
   String username;
   String password;
   Long operatons;
   RunTime runtime;
   String objectLocation;
   ClientConfig client;

   Map<OperationType, OperationConfig> operationConfig;

   public JSONConfiguration()
   {
      this.scheme = Scheme.HTTP;
      this.hosts = new ArrayList<String>();
      this.api = API.SOH;
      this.headers = new LinkedHashMap<String, String>();
      this.filesizes = new ArrayList<FileSize>();
      this.filesizes.add(new FileSize());
      this.source = EntityType.RANDOM;
      this.concurrency = new Concurrency();
      this.objectLocation = ".";
      // TODO OperationType is not being lowercased when serialized
      this.operationConfig = new LinkedHashMap<OperationType, OperationConfig>();
      this.client = new ClientConfig();
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
    * @return the port
    */
   public Integer getPort()
   {
      return this.port;
   }

   /**
    * @return the api
    */
   public API getApi()
   {
      return this.api;
   }

   /**
    * @return the uriRoot
    */
   public String getUriRoot()
   {
      return this.uriRoot;
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
    * @return the source
    */
   public EntityType getSource()
   {
      return this.source;
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
    * @return the objectLocation
    */
   public String getObjectLocation()
   {
      return this.objectLocation;
   }

   /**
    * @return the operationConfig
    */
   public Map<OperationType, OperationConfig> getOperationConfig()
   {
      return this.operationConfig;
   }

   /**
    * @return the client
    */
   public ClientConfig getClient()
   {
      return this.client;
   }
}
