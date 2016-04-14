/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 *
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 *
 * licensing@cleversafe.com
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
