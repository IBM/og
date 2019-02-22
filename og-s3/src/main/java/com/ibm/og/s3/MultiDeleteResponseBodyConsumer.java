/* Copyright (c) IBM Corporation 2019. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.s3;

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
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class MultiDeleteResponseBodyConsumer implements ResponseBodyConsumer {

  private static final Logger _logger = LoggerFactory.getLogger(MultiDeleteResponseBodyConsumer.class);

  @Override
  public Map<String, String> consume(final int statusCode, final InputStream response)
          throws IOException {

    checkNotNull(response);
    Map<String, String> context = new LinkedHashMap<String, String>();
    if (statusCode != 200) {
      context.put(Context.X_OG_MULTI_DELETE_SUCCESS_OBJECTS_COUNT, String.valueOf(0));
      return context;
    }
    DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder documentBuilder = null;
    Document document = null;

    try {
      documentBuilder = documentBuilderFactory.newDocumentBuilder();
    } catch (ParserConfigurationException e) {
      throw new RuntimeException(e);
    }
    try {
      if (response.available() > 0) {
        document = documentBuilder.parse(response);
      }
    } catch (SAXException e) {
      _logger.error(e.getMessage());
    }

    if (document != null) {
      NodeList result = document.getElementsByTagName("DeleteResult");
      if (result != null) {
        NodeList deleted = document.getElementsByTagName("Deleted");
        if (deleted != null) {
          // objects that were deleted
          // handle objects that could not be deleted
          int deletedCount = deleted.getLength();
          if (deletedCount > 0) {
            for (int count = 0; count < deletedCount; count++) {
              Node error = deleted.item(count);
              NodeList children = error.getChildNodes();
              for (int c = 0; c < children.getLength(); c++) {
                Node child = children.item(c);
                String name = child.getNodeName();
                if (name.equals("Key")) {
                  context.put(String.format("deleted-object-%d", count), child.getTextContent());
                }
              }
            }
            context.put(Context.X_OG_MULTI_DELETE_SUCCESS_OBJECTS_COUNT, String.valueOf(deletedCount));
          }


        }
        NodeList failed = document.getElementsByTagName("Error");
        if (failed != null) {
          // handle objects that could not be deleted
          int failedCount = failed.getLength();
          if (failedCount > 0) {
            for (int count = 0; count < failedCount; count++) {
              Node error = failed.item(count);
              NodeList children = error.getChildNodes();
              for (int c = 0; c < children.getLength(); c++) {
                Node child = children.item(c);
                String name = child.getNodeName();
                if (name.equals("Key")) {
                  context.put(String.format("failed-object-%d", count), child.getTextContent());
                }
              }
            }
            context.put(Context.X_OG_MULTI_DELETE_FAILED_OBJECTS_COUNT, String.valueOf(failedCount));
          }
        }

      } else {
        result = document.getElementsByTagName("Error");
        // nothing was deleted. Could be Malformed request or something on the server side
        if (((NodeList) result).getLength() > 0) {
          context.put(Context.X_OG_MULTI_DELETE_REQUST_FAILED, "true");
        }
      }
    }
    return context;
  }

}
