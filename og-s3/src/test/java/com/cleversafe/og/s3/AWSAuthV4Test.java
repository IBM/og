/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.s3;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.cleversafe.og.api.Method;
import com.cleversafe.og.http.HttpRequest;


@RunWith(Parameterized.class)
public class AWSAuthV4Test {

  private static final String AWS_TEST_SCOPE = "AKIDEXAMPLE/20110909/us-east-1/host/aws4_request";
  private static final String AWS_TEST_SECRET_KEY = "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY";
  private static final String AWS_TEST_DATE = "Mon, 09 Sep 2011 23:36:00 GMT";

  private final String testName;
  private final String req;
  private final String creq;
  private final String sts;
  private final String authz;
  private final String sreq;
  private final AWSAuthV4 authV4;

  /**
   * 
   * @return Array of {testName, .req, .creq, .sts, .authz, .sreq}
   * @throws IOException
   */
  @Parameterized.Parameters(name = "testName={0}")
  public static Collection<Object[]> data() throws IOException {
    final File dir = new File(AWSAuthV4Test.class.getResource("/aws4_testsuite/").getPath());
    final File[] reqFiles = dir.listFiles(new FilenameFilter() {
      @Override
      public boolean accept(File arg0, String arg1) {
        return arg1.endsWith(".req") ? true : false;
      }
    });

    // reqFiles = new File[1];
    // reqFiles[0] = new File(dir, "get-slash.req");

    final Collection<Object[]> tests = new ArrayList<Object[]>();
    for (int i = 0; i < reqFiles.length; i++) {
      Object[] test = new Object[6];
      String base = reqFiles[i].getPath().replaceFirst("[.][^.]+$", "");
      test[0] = new File(base).getName();
      test[1] = new String(Files.readAllBytes(new File(base + ".req").toPath()));
      test[2] = new String(Files.readAllBytes(new File(base + ".creq").toPath()));
      test[3] = new String(Files.readAllBytes(new File(base + ".sts").toPath()));
      test[4] = new String(Files.readAllBytes(new File(base + ".authz").toPath()));
      test[5] = new String(Files.readAllBytes(new File(base + ".sreq").toPath()));
      tests.add(test);
    }
    return tests;
  }

  public AWSAuthV4Test(final String testName, final String req, final String creq,
      final String sts, final String authz, final String sreq) {
    this.testName = testName;
    this.req = req;
    this.creq = creq;
    this.sts = sts;
    this.authz = authz;
    this.sreq = sreq;
    this.authV4 = new AWSAuthV4();
  }

  @Test
  public void test() throws IOException, URISyntaxException {
    final String[] reqSplit = this.req.split(" ");
    final String method = reqSplit[0];
    final String[] uriSplit = reqSplit[1].split("\\?");
    final URI uri = new URI(uriSplit[0]);
    final String queryParameters = uriSplit.length > 1 ? uriSplit[1] : "";

    HttpRequest.Builder builder =
        new HttpRequest.Builder(Method.fromString(method), uri, AWS_TEST_DATE);

    final String[] headers = this.req.split("\n");
    // Skip the first line since that doesn't have headers
    for (int i = 1; i < headers.length; i++) {
      String line = headers[i].trim();
      if (line.isEmpty())
        break;
      String[] split = line.split(":");
      builder.withHeader(split[0], split[1]);
    }

    final HttpRequest httpRequest = builder.build();
    System.out.println(httpRequest);
    // Assert.assertEquals(this.creq, this.authV4.getCanonicalRequest());
    // Assert.assertEquals(this.sts, this.authV4.getStringToSign());
    // Assert.assertEquals(this.sreq,
    this.authV4.getAuthorizationHeaders(httpRequest);

    // System.out.println("authz: " + this.authz);
  }
}
