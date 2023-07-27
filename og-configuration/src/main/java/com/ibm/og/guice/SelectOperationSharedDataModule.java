/* Copyright (c) IBM Corporation 2023. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.guice;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.ibm.og.guice.annotation.SelectFileBodies;
import com.ibm.og.guice.annotation.SelectSuffixMap;
import com.ibm.og.http.Bodies;
import com.ibm.og.json.ChoiceConfig;
import com.ibm.og.json.OperationConfig;
import com.ibm.og.json.SelectionConfig;
import com.ibm.og.json.WriteSelectBodyConfig;
import com.ibm.og.object.ListObjectNameConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SelectOperationSharedDataModule extends AbstractModule {

    private static Gson gson;
    private static TypeToken<ConcurrentHashMap<String, Integer>> mapType = new TypeToken<ConcurrentHashMap<String, Integer>>(){};
    private static TypeToken<List<LinkedHashMap<String, String>>> queryMapType = new TypeToken<List<LinkedHashMap<String, String>>>(){};

    private static final Logger _logger = LoggerFactory.getLogger(SelectOperationSharedDataModule.class);
    private static final Logger _consoleLogger = LoggerFactory.getLogger("ConsoleLogger");

    public SuffixManager manager;
    public FileBodies fileBodies;
    @Singleton
    static public class SuffixManager {

        public ConcurrentHashMap<String, Integer> selectObjectSuffixMap;

        public ArrayList<LinkedHashMap<String, String>> selectQueryMap;

        public SuffixManager() {
        }

        public void initMap() {
            if (this.selectObjectSuffixMap == null) {
                this.selectObjectSuffixMap = new ConcurrentHashMap<String, Integer>();
                File file = new File("/var/log/og/selectObjectSuffix.json");
                if (file.exists()) {
                    int sz = (int) file.length();
                    byte[] buffer = new byte[sz];
                    try {
                        FileInputStream fis = new FileInputStream(file);
                        fis.read(buffer);
                        this.selectObjectSuffixMap = gson.fromJson(new String(buffer), mapType.getType());
                    } catch (FileNotFoundException fne) {
                        _logger.warn("File /var/log/og/selectObjectSuffix.json Not found");
                        _consoleLogger.warn("File /var/log/og/selectObjectSuffix.json Not found");
                    } catch (IOException ioe) {
                        _logger.warn("File /var/log/og/selectObjectSuffix.json Not found");
                    } catch (SecurityException se) {
                        throw new RuntimeException("Security Exception occurred when reading /var/log/og/selectObjectSuffix.json");
                    }
                }
            }
        }

        public void persistMap() {

            // write the map the filesystem
            final String filename = "/var/log/og/selectObjectSuffix.json";
            File file = new File(filename);
            try {
                if (file.exists()) {
                    file.delete();
                }
                file = new File(filename);
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(gson.toJson(selectObjectSuffixMap, LinkedHashMap.class).getBytes());
            }  catch (IOException ioe) {
                throw new RuntimeException(String.format("IOException writing to file '%s'", filename));

            }  catch (SecurityException se) {

            }

        }
        public void initSelectBodyContent() {
            final String filename = "/var/log/og/selectContent.json";
            try {
                if (this.selectQueryMap == null) {
                    Path path = FileSystems.getDefault().getPath(filename);
                    if (Files.exists(path)) {
                        byte[] bytes = Files.readAllBytes(path);
                        this.selectQueryMap = gson.fromJson(new String(bytes), queryMapType.getType());
                    } else {
                        this.selectQueryMap = new ArrayList<>();
                    }
                } else {
                        this.selectQueryMap = new ArrayList<>();
                }
            } catch (IOException ioe) {
                throw new RuntimeException(String.format("IOException writing to file '%s'", filename));
            }
        }

        public String getFileNameForSuffix(int i) {
            for (String fp: this.selectObjectSuffixMap.keySet()) {
                if (this.selectObjectSuffixMap.get(fp) == i) {
                    return fp;
                }
            }
            return "";
        }

        public final String getBody(final String fp) {
            for (Map<String, String> elem: this.selectQueryMap) {
                if (elem.get("filepath").equals(fp)) {
                    return elem.get("selectbody");
                }
            }
            throw new RuntimeException(String.format("Could not find the Select Expression for file '%s'", fp));
        }
    }

    @Inject
    public SelectOperationSharedDataModule() {
        this.gson = new GsonBuilder().create();
    }


    @Singleton
    static public class FileBodies {
        public void createFileBodies(final OperationConfig querySelectConfig) {
            final SelectionConfig<WriteSelectBodyConfig> selectionConfig = querySelectConfig.writeSelectBodyConfig;
            final List<ChoiceConfig<WriteSelectBodyConfig>> choices = selectionConfig.choices;
            for (ChoiceConfig<WriteSelectBodyConfig> choice: choices) {
                String filepath = choice.choice.filepath;
                Bodies.file(filepath);
            }
        }
    }

    @Override
    protected void configure() {
        bind(SuffixManager.class)
                .annotatedWith(SelectSuffixMap.class).to(SuffixManager.class).in(Singleton.class);
        bind(FileBodies.class)
                .annotatedWith(SelectFileBodies.class).to(FileBodies.class).in(Singleton.class);
    }
}
