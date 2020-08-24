package com.ibm.og.supplier;

/**
 * Represents account json object in the accounts json file
 */


import com.google.common.collect.ImmutableList;
import com.ibm.og.http.Api;

import java.util.ArrayList;

public class Account {

    private String accountName;
    private String basicAuthUsername;
    private String basicAuthPassword;
    private String domainName;
    private String token;
    private String accessKey;
    private String secretKey;
    private ImmutableList<String> containers;
    private Api api;

    public Account(String accountName, String basicAuthUsername, String basicAuthPassword, String domainName,
                   String token, String accessKey, String secretKey, ArrayList<String> containers, Api api) {
        this.accountName = accountName;
        this.basicAuthUsername = basicAuthUsername;
        this.basicAuthPassword = basicAuthPassword;
        this.domainName = domainName;
        this.token = token;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.containers = ImmutableList.copyOf(containers);
        this.api = api;
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

    public ImmutableList<String> getContainers() {
        return containers;
    }

}

