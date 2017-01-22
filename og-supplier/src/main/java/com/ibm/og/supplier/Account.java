package com.ibm.og.supplier;

/**
 * Represents account json object in the accounts json file
 */


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
    private ArrayList<String> containers;
    private Api api;

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

    public ArrayList<String> getContainers() {
        return containers;
    }

}

