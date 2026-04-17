package com.ibm.og.supplier;

/**
 * Represents account json object in the accounts json file
 */


import com.google.common.collect.ImmutableList;
import com.ibm.og.http.Api;

import java.util.ArrayList;


public class Account {

    public static class Container {
        final public String name;
        final public String accessKey;
        final public String secretKey;
        final public String sessionToken;

        public Container(String name, String accessKey, String secretKey, String sessionToken) {
            this.name = name;
            this.accessKey = accessKey;
            this.secretKey = secretKey;
            this.sessionToken = sessionToken;
        }

        public Container(String name) {
            this(name, null, null, null);
        }

    };

    private String accountName;
    private String basicAuthUsername;
    private String basicAuthPassword;
    private String domainName;
    private String token;
    private String accessKey;
    private String secretKey;
    private ImmutableList<Container> containers;
    private Api api;
    private String sessionToken;

    public Account(String accountName, String basicAuthUsername, String basicAuthPassword, String domainName,
                   String token, String accessKey, String secretKey, ArrayList<Container> containers, Api api, String sessionToken) {
        this.accountName = accountName;
        this.basicAuthUsername = basicAuthUsername;
        this.basicAuthPassword = basicAuthPassword;
        this.domainName = domainName;
        this.token = token;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.containers = ImmutableList.copyOf(containers);
        this.api = api;
        this.sessionToken = sessionToken;
    }

    public String getAccountName() {
        return accountName;
    }

    public Api getApi() {
        return api;
    }


    public String getBasicAuthUsername() {
        return basicAuthUsername;
    }

    public String getBasicAuthPassword() {
        return basicAuthPassword;
    }

    public String getToken() {
        return token;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public ImmutableList<Account.Container> getContainers() {
        return containers;
    }

    public String getSessionToken() {
        return sessionToken;
    }

}

