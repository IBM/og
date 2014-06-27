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

package com.cleversafe.og.cli.json;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.cleversafe.og.cli.json.enums.CollectionAlgorithmType;
import com.cleversafe.og.http.Scheme;
import com.cleversafe.og.http.util.Api;
import com.cleversafe.og.operation.EntityType;

public class JsonConfig
{
   Scheme scheme;
   CollectionAlgorithmType hostAlgorithm;
   List<String> hosts;
   Integer port;
   Api api;
   String uriRoot;
   String container;
   Map<String, String> headers;
   OperationConfig write;
   OperationConfig read;
   OperationConfig delete;
   List<FilesizeConfig> filesizes;
   EntityType source;
   ConcurrencyConfig concurrency;
   AuthenticationConfig authentication;
   StoppingConditionsConfig stoppingConditions;
   String objectLocation;
   ClientConfig client;

   public JsonConfig()
   {
      this.scheme = Scheme.HTTP;
      this.hostAlgorithm = CollectionAlgorithmType.ROUNDROBIN;
      this.hosts = new ArrayList<String>();
      this.api = Api.SOH;
      this.headers = new LinkedHashMap<String, String>();
      this.write = new OperationConfig();
      this.read = new OperationConfig();
      this.delete = new OperationConfig();
      this.filesizes = new ArrayList<FilesizeConfig>();
      this.filesizes.add(new FilesizeConfig());
      this.source = EntityType.RANDOM;
      this.concurrency = new ConcurrencyConfig();
      this.authentication = new AuthenticationConfig();
      this.stoppingConditions = new StoppingConditionsConfig();
      this.objectLocation = "./object";
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
    * @return the hostAlgorithm
    */
   public CollectionAlgorithmType getHostAlgorithm()
   {
      return this.hostAlgorithm;
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
   public Api getApi()
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
   public OperationConfig getWrite()
   {
      return this.write;
   }

   /**
    * @return the read
    */
   public OperationConfig getRead()
   {
      return this.read;
   }

   /**
    * @return the delete
    */
   public OperationConfig getDelete()
   {
      return this.delete;
   }

   /**
    * @return the filesizes
    */
   public List<FilesizeConfig> getFilesizes()
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
   public ConcurrencyConfig getConcurrency()
   {
      return this.concurrency;
   }

   /**
    * @return the authentication
    */
   public AuthenticationConfig getAuthentication()
   {
      return this.authentication;
   }

   /**
    * @return the stoppingConditions
    */
   public StoppingConditionsConfig getStoppingConditions()
   {
      return this.stoppingConditions;
   }

   /**
    * @return the objectLocation
    */
   public String getObjectLocation()
   {
      return this.objectLocation;
   }

   /**
    * @return the client
    */
   public ClientConfig getClient()
   {
      return this.client;
   }
}
