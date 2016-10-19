/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.cleversafe.og.http;

public class Credential {
    private String username;
    private String password;
    private String keystoneToken;

    public Credential() {
        this.username = null;
        this.password = null;
        this.keystoneToken = null;
    }

    public Credential(String username, String password, String keystoneToken) {
        this.username = username;
        this.password = password;
        this.keystoneToken = keystoneToken;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getKeystoneToken() {
        return keystoneToken;
    }
}
