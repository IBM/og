/* Copyright (c) IBM Corporation 2020. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.supplier;

import com.google.common.collect.ImmutableList;
import com.google.gson.stream.JsonToken;
import com.ibm.og.http.Api;
import com.ibm.og.http.Credential;
import com.ibm.og.util.Context;
import com.ibm.og.util.json.type.CaseInsensitiveEnumTypeAdapterFactory;
import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.ibm.og.api.AuthType;
import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import com.google.common.io.Closer;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.lang.Exception;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;


public class CredentialGetterFunction implements Function<Map<String, String>, Credential> {

    private AuthType authType;
    // file containing account details / credentials
    private File credentialFile;
    private Api api;
    private Map<String, Account> accountsMap;
    private Map<String, String> containerAccountMap;
    private Set<Map.Entry<String,Account>> entrySet;
    private Iterator<Map.Entry<String,Account>> cyclicAccountsIterator;

    public CredentialGetterFunction(AuthType authType, File credentialFile, final Api api)
            throws Exception {

        checkNotNull(authType);
        checkNotNull(credentialFile);
        checkNotNull(api);
        checkArgument(authType != AuthType.NONE, "AuthType cannot be None");
        this.authType = authType;
        this.credentialFile = credentialFile;
        this.api = api;
        init();

    }

    private void init() throws IOException {
        Closer closer = Closer.create();
        accountsMap = Maps.newLinkedHashMap();
        containerAccountMap = Maps.newLinkedHashMap();
        try {
            FileInputStream accountStream = closer.register(new FileInputStream(credentialFile));
            final JsonReader jsonReader = new JsonReader(new InputStreamReader(accountStream, Charsets.UTF_8));
            jsonReader.setLenient(true);
            while (jsonReader.peek() != JsonToken.END_DOCUMENT) {
                Account account = readAccount(jsonReader);
                if (account.getApi() == api) {
                    accountsMap.put(account.getAccountName(), account);
                } else if (account.getApi() == null) {
                        // vault mode account is not tied to api. we expect the account
                        // has access to the vault specified in the test input
                    accountsMap.put(account.getAccountName(), account);
                }
            }
            jsonReader.close();
            if (accountsMap.size() == 0) {
                throw new Exception("No credentials matched in the credential file");

            }
            populateContainerAccountMap();

            Set<Map.Entry<String,Account>> entrySet = accountsMap.entrySet();
            cyclicAccountsIterator = Iterators.cycle(entrySet);

        } catch (NullPointerException e) {
            StringBuffer sb = new StringBuffer().
                    append("Unexpected error occured parsing credential file").
                    append("Please make sure each credential is in single line json format");
            throw new IOException(sb.toString());
        } catch (Throwable e) {
            throw closer.rethrow(e);
        } finally {
            closer.close();
        }

    }


    private void populateContainerAccountMap() throws Exception {
        for (String accountName : accountsMap.keySet()){
            Account account = accountsMap.get(accountName);
            ImmutableList<String> containers = account.getContainers();
            if (containers != null) {
                for (String containerName : containers) {
                    containerAccountMap.put(containerName, accountName);
                }
            }
       }
    }


    @Override
    public Credential apply(final Map<String, String> context)  {

        String containerName = context.get(Context.X_OG_CONTAINER_NAME);
        String accountName = containerAccountMap.get(containerName);
        if (accountName == null) {
            accountName = cyclicAccountsIterator.next().getKey();
        }
        Account account = accountsMap.get(accountName);
        checkNotNull(account);

        Credential credential = null;
        if (AuthType.KEYSTONE == authType) {
            credential = new Credential(null, null, account.getToken(), null, accountName);
        } else if (AuthType.IAM == authType) {
            // TODO with full iam_token available, parse the structure
            credential = new Credential(null, null, null, account.getToken(), null);
        } else if (AuthType.AWSV2 == authType || AuthType.AWSV4 == authType) {
            if (api == Api.OPENSTACK) {
                credential = new Credential(account.getAccessKey(), account.getSecretKey(), null, null, accountName);
            } else {
                credential = new Credential(account.getAccessKey(), account.getSecretKey(), null, null, null);
            }
        } else if (AuthType.BASIC == authType) {
            credential = new Credential(account.getBasicAuthUsername(), account.getBasicAuthPassword(), null, null, accountName);
        }
        return credential;
   }


    private static Gson createGson() {
        return new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .registerTypeAdapterFactory(new CaseInsensitiveEnumTypeAdapterFactory())
                .create();
    }


    private Account readAccount(JsonReader reader) throws IOException {
        String accountName = null;
        String basicAuthUsername = null;
        String basicAuthPassword = null;
        String domainName = null;
        String token = null;
        String accessKey = null;
        String secretKey = null;
        ArrayList<String> containers = new ArrayList<String>();
        Api api = null;
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if (reader.peek() == JsonToken.NULL) {
                reader.skipValue();
                continue;
            } else if (name.equals("account_name")) {
                accountName = reader.nextString();
            } else if (name.equals("basic_auth_user_name")) {
                basicAuthUsername = reader.nextString();
            } else if (name.equals("basic_auth_password")) {
                basicAuthPassword = reader.nextString();
            } else if (name.equals("domain_name")) {
                domainName = reader.nextString();
            } else if (name.equals("token")) {
                token = reader.nextString();
            } else if (name.equals("access_key")) {
                accessKey = reader.nextString();
            } else if (name.equals("secret_key")) {
                secretKey = reader.nextString();
            } else if (name.equals("containers")) {
                reader.beginArray();
                while (reader.hasNext()) {
                    containers.add(reader.nextString());
                }
                reader.endArray();
            } else if (name.equals("api")) {
                api = Api.valueOf(reader.nextString().toUpperCase());
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
        Account account = new Account(accountName, basicAuthUsername, basicAuthPassword, domainName, token, accessKey, secretKey,
                containers, api);
        return account;
    }

}
