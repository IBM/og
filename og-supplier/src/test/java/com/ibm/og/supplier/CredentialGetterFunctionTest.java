/* Copyright (c) IBM Corporation 2020. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.supplier;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ibm.og.api.AuthType;
import com.ibm.og.http.Api;
import com.ibm.og.http.Credential;
import com.ibm.og.util.Context;
import com.ibm.og.util.json.type.CaseInsensitiveEnumTypeAdapterFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

public class CredentialGetterFunctionTest {

  private Account account0;
  private Account account1;
  private final String credentialsFilePath = "./credentials.json";

  @Before
  public void before() throws IOException {

    ArrayList<String> containers = new ArrayList<String>();
    containers.add("container0");
    containers.add("container1");
    this.account0 = new Account("account0", "user1", "password",null,
            null,  "gS2nuzatdztkeRhOm8kl", "ikGuemK3Q3HpeyAh72Ny47dH6ygGf3BhaMRwPZRx",
            containers, Api.S3);
    containers.clear();
    containers.add("container2");
    containers.add("container3");
    this.account1 = new Account("account1", null, null,null,
            null,  "gS2nuzatdztkeRhOm8kk", "ikGuemK3Q3HpeyAh72Ny47dH6ygGf3BhaMRwPZRy",
            containers, Api.S3);

    File file = new File(credentialsFilePath);
    if (file.exists()) {
      file.delete();
      file.createNewFile();
    }
  }


  @Test
  public void credentialGetterFunctionCompactTest() throws  Exception{


    Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .registerTypeAdapterFactory(new CaseInsensitiveEnumTypeAdapterFactory())
            .create();
    FileOutputStream fos = new FileOutputStream(new File(this.credentialsFilePath));
    String s = gson.toJson(this.account0);
    fos.write(s.getBytes());
    fos.write("\n".getBytes());
    s = gson.toJson(this.account1);
    fos.write(s.getBytes());
    fos.close();

    CredentialGetterFunction cgf = new CredentialGetterFunction(AuthType.AWSV4,
            new File(this.credentialsFilePath), Api.S3);
    HashMap<String, String> hashMap = new LinkedHashMap<String, String>();
    hashMap.put(Context.X_OG_CONTAINER_NAME, "container0");
    Credential credential = cgf.apply(hashMap);
    assertThat(credential.getUsername(), is("gS2nuzatdztkeRhOm8kl"));
    assertThat(credential.getPassword(), is("ikGuemK3Q3HpeyAh72Ny47dH6ygGf3BhaMRwPZRx"));

    hashMap.put(Context.X_OG_CONTAINER_NAME, "container3");
    credential = cgf.apply(hashMap);
    assertThat(credential.getUsername(), is("gS2nuzatdztkeRhOm8kk"));
    assertThat(credential.getPassword(), is("ikGuemK3Q3HpeyAh72Ny47dH6ygGf3BhaMRwPZRy"));
  }

  @Test
  public void credentialGetterFunctionLenientTest() throws  Exception{
    // this will print each object property in a separate line
    Gson gson = new GsonBuilder().setPrettyPrinting().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .registerTypeAdapterFactory(new CaseInsensitiveEnumTypeAdapterFactory())
            .create();
    FileOutputStream fos = new FileOutputStream(new File(this.credentialsFilePath));
    String s = gson.toJson(this.account0);
    fos.write(s.getBytes());
    fos.write("\n".getBytes());
    // add an extra blank line in the middle
    fos.write("\n".getBytes());
    fos.write("\n".getBytes());
    s = gson.toJson(this.account1);
    fos.write(s.getBytes());
    fos.write("\n".getBytes());
    // add an extra blank line at the end of file
    fos.write("\n".getBytes());
    fos.close();

    CredentialGetterFunction cgf = new CredentialGetterFunction(AuthType.AWSV4,
            new File(this.credentialsFilePath), Api.S3);
    HashMap<String, String> hashMap = new LinkedHashMap<String, String>();
    hashMap.put(Context.X_OG_CONTAINER_NAME, "container0");
    Credential credential = cgf.apply(hashMap);
    assertThat(credential.getUsername(), is("gS2nuzatdztkeRhOm8kl"));
    assertThat(credential.getPassword(), is("ikGuemK3Q3HpeyAh72Ny47dH6ygGf3BhaMRwPZRx"));

    hashMap.put(Context.X_OG_CONTAINER_NAME, "container3");
    credential = cgf.apply(hashMap);
    assertThat(credential.getUsername(), is("gS2nuzatdztkeRhOm8kk"));
    assertThat(credential.getPassword(), is("ikGuemK3Q3HpeyAh72Ny47dH6ygGf3BhaMRwPZRy"));


  }
}
