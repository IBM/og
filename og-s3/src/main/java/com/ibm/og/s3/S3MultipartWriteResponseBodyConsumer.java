/* Copyright (c) IBM Corporation 2016. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.s3;

import com.ibm.og.http.ResponseBodyConsumer;
import com.ibm.og.util.Context;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A response body consumer which processes the body of multipart responses
 * Pulls UploadId from Initiate response and errors from Complete response
 * 
 * @since 1.0
 */
public class S3MultipartWriteResponseBodyConsumer implements ResponseBodyConsumer {
  private static final Logger _logger = LoggerFactory.getLogger(S3MultipartWriteResponseBodyConsumer.class);

  @Override
  public Map<String, String> consume(final int statusCode, final InputStream response)
      throws IOException {
    checkNotNull(response);

    final BufferedReader reader =
        new BufferedReader(new InputStreamReader(response, Charsets.UTF_8));

    DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder documentBuilder = null;
    Document document = null;

    try {
      documentBuilder = documentBuilderFactory.newDocumentBuilder();
    } catch (ParserConfigurationException e) {
      throw new RuntimeException(e);
    }

    try {
      if(response.available() > 0) {
        document = documentBuilder.parse(response);
      } else {
        // handle chunked transfer-encoding
        byte[] buf = new byte[4096];
        int bytesRead;
        int offset = 0;
        while((bytesRead = response.read(buf, offset, 4096)) > 0) {
          offset += bytesRead;
        }
        if (offset > 0) {
          ByteArrayInputStream bis = new ByteArrayInputStream(buf, 0, offset);
          document = documentBuilder.parse(bis);
        }
      }
    } catch (SAXException e) {
        _logger.error(e.getMessage());
    }

    if(document != null) {
      NodeList uploadIdNodeList = document.getElementsByTagName("UploadId");
      if (uploadIdNodeList != null) {
        if (uploadIdNodeList.getLength() > 0) {
          String uploadId = uploadIdNodeList.item(0).getTextContent();
          return ImmutableMap.of(Context.X_OG_MULTIPART_UPLOAD_ID, uploadId);
        }
      }
      NodeList errorBodyList = document.getElementsByTagName("Error");
      if (errorBodyList != null) {
        if(errorBodyList.getLength() > 0) {
          String errorBody = errorBodyList.item(0).getTextContent();
          _logger.error(errorBody);
        }
      }
    }

    return ImmutableMap.of();
  }

  @Override
  public String toString() {
    return "S3MultipartWriteResponseBodyConsumer []";
  }
}
