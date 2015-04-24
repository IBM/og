/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.s3.v4;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.security.InvalidParameterException;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cleversafe.og.api.Body;
import com.cleversafe.og.api.Request;
import com.cleversafe.og.http.Headers;
import com.cleversafe.og.http.HttpAuth;
import com.cleversafe.og.util.io.Streams;
import com.google.common.net.HttpHeaders;

public class AWSAuthV4 extends AWSAuthV4Base implements HttpAuth {
  private static Logger _logger = LoggerFactory.getLogger(AWSAuthV4.class);

  public AWSAuthV4() {
    super("us-east-1", "s3", null);
  }

  public AWSAuthV4(String regionName, String serviceName, Long forcedDate) {
    super(regionName, serviceName, forcedDate);
  }

  @Override
  public Map<String, String> getAuthorizationHeaders(Request request) {
    final String keyId = checkNotNull(request.headers().get(Headers.X_OG_USERNAME));
    final String secretKey = checkNotNull(request.headers().get(Headers.X_OG_PASSWORD));

    try {
      final AWS4SignerBase signer =
          new AWS4SignerBase(request.getUri().toURL(), request.getMethod().toString(), serviceName,
              regionName);

      final Date date = forcedDate == null ? new Date() : new Date(forcedDate);
      return getAuthHeaders(signer, request.headers(), Collections.<String, String>emptyMap(),
          calculateFullBodyHash(request.getBody()), keyId, secretKey, date);

    } catch (MalformedURLException e) {
      throw new InvalidParameterException("Can't convert to request.URI(" + request.getUri()
          + ") to  URL:" + e.getMessage());
    }
  }

  private String calculateFullBodyHash(final Body body) {
    // TODO store a cash for body hashes here if performance is not adequate.
    if (body.getSize() == 0) {
      return AWS4SignerBase.EMPTY_BODY_SHA256;
    } else {
      try {
        InputStream s = Streams.create(body);

        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] buff = new byte[8192];
        int read = s.read(buff);
        while (read != -1) {
          md.update(buff, 0, read);
          read = s.read(buff);
        }
        return BinaryUtils.toHex(md.digest());
      } catch (Exception e) {
        throw new RuntimeException("Unable to compute hash while signing request: "
            + e.getMessage(), e);
      }
    }
  }

  @Override
  public InputStream wrapStream(InputStream stream) {
    return stream;
  }

  @Override
  public long getContentLength(Request request) {
    return request.getBody().getSize();
  }

  /**
   * Computes an AWS4 signature for a request, ready for inclusion as an 'Authorization' header.
   * 
   * @param headers The request headers; 'Host' and 'X-Amz-Date' will be added to this set.
   * @param queryParameters Any query parameters that will be added to the endpoint. The parameters
   *        should be specified in canonical format.
   * @param bodyHash Precomputed SHA256 hash of the request body content; this value should also be
   *        set as the header 'X-Amz-Content-SHA256' for non-streaming uploads.
   * @param awsAccessKey The user's AWS Access Key.
   * @param awsSecretKey The user's AWS Secret Key.
   * @param date date to put in the x-amz-date field
   * @return The computed authorization string for the request. This value needs to be set as the
   *         header 'Authorization' on the subsequent HTTP request.
   */
  public Map<String, String> getAuthHeaders(AWS4SignerBase signer, Map<String, String> headers,
      Map<String, String> queryParameters, String bodyHash, String awsAccessKey,
      String awsSecretKey, Date date) {

    // Don't sign the og headers
    final Map<String, String> authHeaders = filterOutOgHeaders(headers);

    authHeaders.put("x-amz-content-sha256", bodyHash);

    // first get the date and time for the subsequent request, and convert
    // to ISO 8601 format for use in signature generation
    final String dateTimeStamp = signer.dateTimeFormat.format(date);

    // update the headers with required 'x-amz-date' and 'host' values
    authHeaders.put("x-amz-date", dateTimeStamp);

    final String hostHeader = signer.endpointUrl.getHost();
    final int port = signer.endpointUrl.getPort();
    if (port > -1) {
      hostHeader.concat(":" + Integer.toString(port));
    }
    authHeaders.put("Host", hostHeader);

    // canonicalize the headers; we need the set of header names as well as the
    // names and values to go into the signature process
    final String canonicalizedHeaderNames = AWS4SignerBase.getCanonicalizeHeaderNames(authHeaders);
    final String canonicalizedHeaders = AWS4SignerBase.getCanonicalizedHeaderString(authHeaders);

    // if any query string parameters have been supplied, canonicalize them
    final String canonicalizedQueryParameters =
        AWS4SignerBase.getCanonicalizedQueryString(queryParameters);

    // canonicalize the various components of the request
    final String canonicalRequest =
        AWS4SignerBase.getCanonicalRequest(signer.endpointUrl, signer.httpMethod,
            canonicalizedQueryParameters, canonicalizedHeaderNames, canonicalizedHeaders, bodyHash);
    _logger.debug("--------- Canonical request --------");
    _logger.debug(canonicalRequest);
    _logger.debug("------------------------------------");

    // construct the string to be signed
    final String dateStamp = signer.dateStampFormat.format(date);
    final String scope =
        dateStamp + "/" + regionName + "/" + serviceName + "/" + AWS4SignerBase.TERMINATOR;
    final String stringToSign =
        AWS4SignerBase.getStringToSign(AWS4SignerBase.SCHEME, AWS4SignerBase.ALGORITHM,
            dateTimeStamp, scope, canonicalRequest);
    _logger.debug("--------- String to sign -----------");
    _logger.debug(stringToSign);
    _logger.debug("------------------------------------");

    // compute the signing key
    final byte[] kSecret = (AWS4SignerBase.SCHEME + awsSecretKey).getBytes();
    final byte[] kDate = AWS4SignerBase.sign(dateStamp, kSecret, "HmacSHA256");
    final byte[] kRegion = AWS4SignerBase.sign(regionName, kDate, "HmacSHA256");
    final byte[] kService = AWS4SignerBase.sign(serviceName, kRegion, "HmacSHA256");
    final byte[] kSigning = AWS4SignerBase.sign(AWS4SignerBase.TERMINATOR, kService, "HmacSHA256");
    final byte[] signature = AWS4SignerBase.sign(stringToSign, kSigning, "HmacSHA256");

    final String credentialsAuthorizationHeader = "Credential=" + awsAccessKey + "/" + scope;
    final String signedHeadersAuthorizationHeader = "SignedHeaders=" + canonicalizedHeaderNames;
    final String signatureAuthorizationHeader = "Signature=" + BinaryUtils.toHex(signature);

    final String authorizationHeader =
        AWS4SignerBase.SCHEME + "-" + AWS4SignerBase.ALGORITHM + " "
            + credentialsAuthorizationHeader + ", " + signedHeadersAuthorizationHeader + ", "
            + signatureAuthorizationHeader;

    authHeaders.put(HttpHeaders.AUTHORIZATION, authorizationHeader);
    return authHeaders;
  }
}
