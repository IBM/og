/* Copyright (c) IBM Corporation 2018. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.s3;


import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.ibm.og.http.ResponseBodyConsumer;
import com.ibm.og.util.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A response body consumer which processes the body of list operation response
 * Pulls istruncated and .... from Initiate response and errors from Complete response
 *
 * @since 1.8.4
 */
public class S3ListResponseBodyConsumer implements ResponseBodyConsumer {

  private static final Logger _logger = LoggerFactory.getLogger(S3ListResponseBodyConsumer.class);

  @Override
  public Map<String, String> consume(final int statusCode, final InputStream response)
          throws IOException {
    checkNotNull(response);

    DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder documentBuilder = null;
    Document document = null;
    Map<String, String> context = new LinkedHashMap<String, String>();

    try {
      documentBuilder = documentBuilderFactory.newDocumentBuilder();
    } catch (ParserConfigurationException e) {
      throw new RuntimeException(e);
    }
    try {
      if(response.available() > 0) {
        document = documentBuilder.parse(response);
      }
    } catch (SAXException e) {
      _logger.error(e.getMessage());
    }

    if(document != null) {
      NodeList truncatedNode = document.getElementsByTagName("IsTruncated");
      if (truncatedNode != null) {
        if (truncatedNode.getLength() > 0) {
          String isTruncated = truncatedNode.item(0).getTextContent();
          context.put(Context.X_OG_LIST_IS_TRUNCATED, isTruncated);

          if (isTruncated.equalsIgnoreCase("true")) {
            NodeList nextMarkerNode = document.getElementsByTagName("NextMarker");
            if (nextMarkerNode != null) {
              if(nextMarkerNode.getLength() > 0) {
                String nextMarker = nextMarkerNode.item(0).getTextContent();
                context.put(Context.X_OG_LIST_NEXT_MARKER, nextMarker);
              }
            }

            NodeList nextContinuationTokenNode = document.getElementsByTagName("NextContinuationToken");
            if (nextContinuationTokenNode != null) {
              if(nextContinuationTokenNode.getLength() > 0) {
                String nextContinuationToken = nextContinuationTokenNode.item(0).getTextContent();
                context.put(Context.X_OG_LIST_NEXT_CONTINUATION_TOKEN, nextContinuationToken);
              }
            }

            NodeList contents = document.getElementsByTagName("Contents");
            if (contents != null) {
              Integer numContents = contents.getLength();
              context.put(Context.X_OG_NUM_LIST_CONTENTS, numContents.toString());
            } else {
              context.put(Context.X_OG_NUM_LIST_CONTENTS, "0");
            }

          }
        }
      }
    }

    return ImmutableMap.copyOf(context);
  }

  @Override
  public String toString() {
    return "S3ListResponseBodyConsumer []";
  }


}
