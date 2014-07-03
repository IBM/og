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
   private final Scheme scheme;
   private final CollectionAlgorithmType hostSelection;
   private final List<String> host;
   private final Integer port;
   private final Api api;
   private final String uriRoot;
   private final String container;
   private final Map<String, String> headers;
   private final OperationConfig write;
   private final OperationConfig read;
   private final OperationConfig delete;
   private final List<FilesizeConfig> filesize;
   private final EntityType source;
   private final ConcurrencyConfig concurrency;
   private final AuthenticationConfig authentication;
   private final StoppingConditionsConfig stoppingConditions;
   private final ObjectManagerConfig objectManager;
   private final ClientConfig client;

   public JsonConfig()
   {
      this.scheme = Scheme.HTTP;
      this.hostSelection = CollectionAlgorithmType.ROUNDROBIN;
      this.host = new ArrayList<String>();
      this.port = null;
      this.api = Api.SOH;
      this.uriRoot = null;
      this.container = null;
      this.headers = new LinkedHashMap<String, String>();
      this.write = new OperationConfig();
      this.read = new OperationConfig();
      this.delete = new OperationConfig();
      this.filesize = new ArrayList<FilesizeConfig>();
      this.filesize.add(new FilesizeConfig());
      this.source = EntityType.RANDOM;
      this.concurrency = new ConcurrencyConfig();
      this.authentication = new AuthenticationConfig();
      this.stoppingConditions = new StoppingConditionsConfig();
      this.objectManager = new ObjectManagerConfig();
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
    * @return the hostSelection
    */
   public CollectionAlgorithmType getHostSelection()
   {
      return this.hostSelection;
   }

   /**
    * @return the host
    */
   public List<String> getHost()
   {
      return this.host;
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
    * @return the filesize
    */
   public List<FilesizeConfig> getFilesize()
   {
      return this.filesize;
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
    * @return the objectManager
    */
   public ObjectManagerConfig getObjectManager()
   {
      return this.objectManager;
   }

   /**
    * @return the client
    */
   public ClientConfig getClient()
   {
      return this.client;
   }
}
