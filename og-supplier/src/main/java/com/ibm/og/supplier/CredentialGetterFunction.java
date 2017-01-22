package com.ibm.og.supplier;

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
        Gson gson = createGson();
        Closer closer = Closer.create();
        accountsMap = Maps.newLinkedHashMap();
        containerAccountMap = Maps.newLinkedHashMap();
        try {
            FileInputStream accountStream = closer.register(new FileInputStream(credentialFile));
            final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(accountStream, Charsets.UTF_8));
            closer.register(bufferedReader);
            String line;
            while ((line = bufferedReader.readLine()) != null) {
              Account account =  gson.fromJson(line, Account.class);
              if (account.getApi() == api) {
                  accountsMap.put(account.getAccountName(), account);
              } else if (account.getApi() == null){
                  // vault mode account is not tied to api. we expect the account
                  // has access to the vault specified in the test input
                  accountsMap.put(account.getAccountName(), account);
              }
            }
            if (accountsMap.size() == 0) {
                throw new Exception("No credentials matched in the credential file");

            }
            populateContainerAccountMap();

            Set<Map.Entry<String,Account>> entrySet = accountsMap.entrySet();
            cyclicAccountsIterator = Iterators.cycle(entrySet);

        } catch (Throwable e) {
            throw closer.rethrow(e);
        } finally {
            closer.close();
        }

    }


    private void populateContainerAccountMap() throws Exception {
        for (String accountName : accountsMap.keySet()){
            Account account = accountsMap.get(accountName);
            ArrayList<String> containers = account.getContainers();
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
            credential = new Credential(null, null, account.getToken(), accountName);
        } else if (AuthType.AWSV2 == authType || AuthType.AWSV4 == authType) {
            if (api == Api.OPENSTACK) {
                credential = new Credential(account.getAccessKey(), account.getSecretKey(), null, accountName);
            } else {
                credential = new Credential(account.getAccessKey(), account.getSecretKey(), null, null);
            }
        } else if (AuthType.BASIC == authType) {
            credential = new Credential(account.getBasicAuthUsername(), account.getBasicAuthPassword(), null, accountName);
        }
        return credential;
   }


    private static Gson createGson() {
        return new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .registerTypeAdapterFactory(new CaseInsensitiveEnumTypeAdapterFactory())
                .create();
    }

}
