package com.cleversafe.og.s3.v4;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SimpleTimeZone;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.net.HttpHeaders;


/**
 * Common methods and properties for all AWS4 signer variants
 */
public class AWS4SignerBase {
  private static Logger _logger = LoggerFactory.getLogger(AWS4SignerBase.class);

  /** SHA256 hash of an empty request body **/
  public static final String EMPTY_BODY_SHA256 =
      "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
  public static final String UNSIGNED_PAYLOAD = "UNSIGNED-PAYLOAD";

  public static final String SCHEME = "AWS4";
  public static final String ALGORITHM = "HMAC-SHA256";
  public static final String TERMINATOR = "aws4_request";

  /** format strings for the date/time and date stamps required during signing **/
  public static final String ISO8601BasicFormat = "yyyyMMdd'T'HHmmss'Z'";
  public static final String DateStringFormat = "yyyyMMdd";

  protected final URL endpointUrl;
  protected final String httpMethod;
  protected final String serviceName;
  protected final String regionName;

  protected final SimpleDateFormat dateTimeFormat;
  protected final SimpleDateFormat dateStampFormat;

  /**
   * Tracks the previously computed signature value; for chunk 0 this will contain the signature
   * included in the Authorization header. For subsequent chunks it contains the computed signature
   * of the prior chunk.
   */
  protected String lastComputedSignature;

  /**
   * Date and time of the original signing computation, in ISO 8601 basic format, reused for each
   * chunk
   */
  protected String dateTimeStamp;

  /**
   * The scope value of the original signing computation, reused for each chunk
   */
  protected String scope;

  /**
   * The derived signing key used in the original signature computation and re-used for each chunk
   */
  protected byte[] signingKey;

  /**
   * Create a new AWS V4 signer.
   * 
   * @param endpointUri The service endpoint, including the path to any resource.
   * @param httpMethod The HTTP verb for the request, e.g. GET.
   * @param serviceName The signing name of the service, e.g. 's3'.
   * @param regionName The system name of the AWS region associated with the endpoint, e.g.
   *        us-east-1.
   */
  public AWS4SignerBase(final URL endpointUrl, final String httpMethod, final String serviceName,
      final String regionName) {
    this.endpointUrl = endpointUrl;
    this.httpMethod = httpMethod;
    this.serviceName = serviceName;
    this.regionName = regionName;

    this.dateTimeFormat = new SimpleDateFormat(ISO8601BasicFormat);
    this.dateTimeFormat.setTimeZone(new SimpleTimeZone(0, "UTC"));
    this.dateStampFormat = new SimpleDateFormat(DateStringFormat);
    this.dateStampFormat.setTimeZone(new SimpleTimeZone(0, "UTC"));
  }

  /**
   * Returns a set of required headers for authentication. Also defines {@link #signingKey}, and
   * {@link #scope}, and {@link #dateTimeStamp}.
   */
  public Map<String, String> getAuthHeaders(final Map<String, String> authHeaders,
      final Map<String, String> queryParameters, final String bodyHash, final String awsAccessKey,
      final String awsSecretKey, final Date date) {

    authHeaders.put("x-amz-content-sha256", bodyHash);

    // first get the date and time for the subsequent request, and convert
    // to ISO 8601 format for use in signature generation
    this.dateTimeStamp = this.dateTimeFormat.format(date);

    // update the headers with required 'x-amz-date' and 'host' values
    authHeaders.put("x-amz-date", this.dateTimeStamp);

    final String hostHeader = this.endpointUrl.getHost();
    final int port = this.endpointUrl.getPort();
    if (port > -1) {
      hostHeader.concat(":" + Integer.toString(port));
    }
    authHeaders.put("Host", hostHeader);

    // canonicalize the headers; we need the set of header names as well as the
    // names and values to go into the signature process
    final String canonicalizedHeaderNames = getCanonicalizeHeaderNames(authHeaders);
    final String canonicalizedHeaders = getCanonicalizedHeaderString(authHeaders);

    // if any query string parameters have been supplied, canonicalize them
    final String canonicalizedQueryParameters = getCanonicalizedQueryString(queryParameters);

    // canonicalize the various components of the request
    final String canonicalRequest = getCanonicalRequest(this.endpointUrl, this.httpMethod,
        canonicalizedQueryParameters, canonicalizedHeaderNames, canonicalizedHeaders, bodyHash);
    _logger.trace("--------- Canonical request --------");
    _logger.trace(canonicalRequest);
    _logger.trace("------------------------------------");

    // construct the string to be signed
    final String dateStamp = this.dateStampFormat.format(date);
    this.scope = dateStamp + "/" + this.regionName + "/" + this.serviceName + "/" + TERMINATOR;
    final String stringToSign =
        getStringToSign(SCHEME, ALGORITHM, this.dateTimeStamp, this.scope, canonicalRequest);
    _logger.trace("--------- String to sign -----------");
    _logger.trace(stringToSign);
    _logger.trace("------------------------------------");

    // compute the signing key
    final byte[] kSecret = (SCHEME + awsSecretKey).getBytes();
    final byte[] kDate = sign(dateStamp, kSecret, "HmacSHA256");
    final byte[] kRegion = sign(this.regionName, kDate, "HmacSHA256");
    final byte[] kService = sign(this.serviceName, kRegion, "HmacSHA256");
    this.signingKey = sign(TERMINATOR, kService, "HmacSHA256");
    final byte[] signature = sign(stringToSign, this.signingKey, "HmacSHA256");

    // cache the computed signature ready for chunk 0 upload
    this.lastComputedSignature = BinaryUtils.toHex(signature);

    final String credentialsAuthorizationHeader = "Credential=" + awsAccessKey + "/" + this.scope;
    final String signedHeadersAuthorizationHeader = "SignedHeaders=" + canonicalizedHeaderNames;
    final String signatureAuthorizationHeader = "Signature=" + this.lastComputedSignature;

    final String authorizationHeader =
        SCHEME + "-" + ALGORITHM + " " + credentialsAuthorizationHeader + ", "
            + signedHeadersAuthorizationHeader + ", " + signatureAuthorizationHeader;

    authHeaders.put(HttpHeaders.AUTHORIZATION, authorizationHeader);

    return authHeaders;
  }

  /**
   * Returns the canonical collection of header names that will be included in the signature. For
   * AWS4, all header names must be included in the process in sorted canonicalized order.
   */
  protected static String getCanonicalizeHeaderNames(final Map<String, String> headers) {
    final List<String> sortedHeaders = new ArrayList<String>();
    sortedHeaders.addAll(headers.keySet());
    Collections.sort(sortedHeaders, String.CASE_INSENSITIVE_ORDER);

    final StringBuilder buffer = new StringBuilder();
    for (final String header : sortedHeaders) {
      if (header.startsWith("x-og")) {
        continue;
      }
      if (buffer.length() > 0) {
        buffer.append(";");
      }
      buffer.append(header.toLowerCase());
    }

    return buffer.toString();
  }

  /**
   * Computes the canonical headers with values for the request. For AWS4, all headers must be
   * included in the signing process.
   */
  protected static String getCanonicalizedHeaderString(final Map<String, String> headers) {
    if (headers == null || headers.isEmpty()) {
      return "";
    }

    // step1: sort the headers by case-insensitive order
    final List<String> sortedHeaders = new ArrayList<String>();
    sortedHeaders.addAll(headers.keySet());
    Collections.sort(sortedHeaders, String.CASE_INSENSITIVE_ORDER);

    // step2: form the canonical header:value entries in sorted order.
    // Multiple white spaces in the values should be compressed to a single
    // space.
    final StringBuilder buffer = new StringBuilder();
    for (final String key : sortedHeaders) {
      buffer.append(key.toLowerCase().replaceAll("\\s+", " ") + ":"
          + headers.get(key).replaceAll("\\s+", " "));
      buffer.append("\n");
    }

    return buffer.toString();
  }

  /**
   * Returns the canonical request string to go into the signer process; this consists of several
   * canonical sub-parts.
   * 
   * @return
   */
  protected static String getCanonicalRequest(final URL endpoint, final String httpMethod,
      final String queryParameters, final String canonicalizedHeaderNames,
      final String canonicalizedHeaders, final String bodyHash) {
    final String canonicalRequest =
        httpMethod + "\n" + getCanonicalizedResourcePath(endpoint) + "\n" + queryParameters + "\n"
            + canonicalizedHeaders + "\n" + canonicalizedHeaderNames + "\n" + bodyHash;
    return canonicalRequest;
  }

  public static String urlEncode(final String url, final boolean keepPathSlash) {
    String encoded;
    try {
      encoded = URLEncoder.encode(url, "UTF-8");
    } catch (final UnsupportedEncodingException e) {
      throw new RuntimeException("UTF-8 encoding is not supported.", e);
    }
    if (keepPathSlash) {
      encoded = encoded.replace("%2F", "/");
    }
    return encoded;
  }

  /**
   * Returns the canonicalized resource path for the service endpoint.
   */
  protected static String getCanonicalizedResourcePath(final URL endpoint) {
    if (endpoint == null) {
      return "/";
    }
    final String path = endpoint.getPath();
    if (path == null || path.isEmpty()) {
      return "/";
    }

    final String encodedPath = urlEncode(path, true);
    if (encodedPath.startsWith("/")) {
      return encodedPath;
    } else {
      return "/".concat(encodedPath);
    }
  }

  /**
   * Examines the specified query string parameters and returns a canonicalized form.
   * <p>
   * The canonicalized query string is formed by first sorting all the query string parameters, then
   * URI encoding both the key and value and then joining them, in order, separating key value pairs
   * with an '&'.
   * 
   * @param parameters The query string parameters to be canonicalized.
   * 
   * @return A canonicalized form for the specified query string parameters.
   */
  public static String getCanonicalizedQueryString(final Map<String, String> parameters) {
    if (parameters == null || parameters.isEmpty()) {
      return "";
    }

    final SortedMap<String, String> sorted = new TreeMap<String, String>();

    Iterator<Map.Entry<String, String>> pairs = parameters.entrySet().iterator();
    while (pairs.hasNext()) {
      final Map.Entry<String, String> pair = pairs.next();
      final String key = pair.getKey();
      final String value = pair.getValue();
      sorted.put(urlEncode(key, false), urlEncode(value, false));
    }

    final StringBuilder builder = new StringBuilder();
    pairs = sorted.entrySet().iterator();
    while (pairs.hasNext()) {
      final Map.Entry<String, String> pair = pairs.next();
      builder.append(pair.getKey());
      builder.append("=");
      builder.append(pair.getValue());
      if (pairs.hasNext()) {
        builder.append("&");
      }
    }

    return builder.toString();
  }

  protected static String getStringToSign(final String scheme, final String algorithm,
      final String dateTime, final String scope, final String canonicalRequest) {
    final String stringToSign = scheme + "-" + algorithm + "\n" + dateTime + "\n" + scope + "\n"
        + BinaryUtils.toHex(hash(canonicalRequest));
    return stringToSign;
  }

  /**
   * Hashes the string contents (assumed to be UTF-8) using the SHA-256 algorithm.
   */
  public static byte[] hash(final String text) {
    try {
      final MessageDigest md = MessageDigest.getInstance("SHA-256");
      md.update(text.getBytes("UTF-8"));
      return md.digest();
    } catch (final Exception e) {
      throw new RuntimeException("Unable to compute hash while signing request: " + e.getMessage(),
          e);
    }
  }

  /**
   * Hashes the byte array using the SHA-256 algorithm.
   */
  public static byte[] hash(final byte[] data) {
    try {
      final MessageDigest md = MessageDigest.getInstance("SHA-256");
      md.update(data);
      return md.digest();
    } catch (final Exception e) {
      throw new RuntimeException("Unable to compute hash while signing request: " + e.getMessage(),
          e);
    }
  }

  protected static byte[] sign(final String stringData, final byte[] key, final String algorithm) {
    try {
      final byte[] data = stringData.getBytes("UTF-8");
      final Mac mac = Mac.getInstance(algorithm);
      mac.init(new SecretKeySpec(key, algorithm));
      return mac.doFinal(data);
    } catch (final Exception e) {
      throw new RuntimeException("Unable to calculate a request signature: " + e.getMessage(), e);
    }
  }
}
