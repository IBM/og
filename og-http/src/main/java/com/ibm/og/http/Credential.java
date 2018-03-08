/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.http;

public class Credential {
    private String username;
    private String password;
    private String keystoneToken;
    private String IAMToken;
    private String storageAccountName;

    public Credential() {
        this.username = null;
        this.password = null;
        this.keystoneToken = null;
        this.IAMToken = null;
        this.storageAccountName = null;
    }



    public Credential(String username, String password, String keystoneToken, String IAMToken, String storageAccountName) {
        this.username = username;
        this.password = password;
        this.keystoneToken = keystoneToken;
        this.IAMToken = IAMToken;
        this.storageAccountName = storageAccountName;
    }

    public String getUsername() {
        return this.username;
    }

    public String getPassword() {
        return this.password;
    }

    public String getKeystoneToken() {
        return this.keystoneToken;
    }

    public String getIAMToken() {
        return this.IAMToken;
    }

    public String getStorageAccountName() {
        return this.storageAccountName;
    }


}
