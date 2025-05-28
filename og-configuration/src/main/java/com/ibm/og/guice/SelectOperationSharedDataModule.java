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
import com.google.inject.name.Named;
import com.ibm.og.guice.annotation.SelectFileBodies;
import com.ibm.og.guice.annotation.SelectSuffixMap;
import com.ibm.og.http.Bodies;
import com.ibm.og.json.ChoiceConfig;
import com.ibm.og.json.OperationConfig;
import com.ibm.og.json.SelectionConfig;
import com.ibm.og.json.WriteSelectBodyConfig;
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

        final String selectOperationsConfigDir;
        final String selectContentQueriesFile;
        final String selectObjectSuffixFile;

       @Inject
        public SuffixManager(@Named("selectOperationsConfigLocation") final String selectOperationsConfigDir,
                             @Named("selectContentQueryFile") final String selectContentQueriesFile,
                             @Named("selectObjectSuffixFile") final String selectObjectSuffixFile) {
            this.selectOperationsConfigDir = selectOperationsConfigDir;
            this.selectContentQueriesFile = String.format("%s/%s", this.selectOperationsConfigDir,
                    selectContentQueriesFile);
            this.selectObjectSuffixFile = String.format("%s/%s", this.selectOperationsConfigDir,
                    selectObjectSuffixFile);
        }

        public void initMap() {
            if (this.selectObjectSuffixMap == null) {
                this.selectObjectSuffixMap = new ConcurrentHashMap<String, Integer>();
                File file = new File(this.selectObjectSuffixFile);
                if (file.exists()) {
                    int sz = (int) file.length();
                    byte[] buffer = new byte[sz];
                    try {
                        FileInputStream fis = new FileInputStream(file);
                        fis.read(buffer);
                        this.selectObjectSuffixMap = gson.fromJson(new String(buffer), mapType.getType());
                    } catch (FileNotFoundException fne) {
                        String message = String.format("File %s Not found", this.selectObjectSuffixFile);
                        _logger.warn(message);
                        _consoleLogger.warn(message);
                    } catch (IOException ioe) {
                        _logger.warn(ioe.getMessage());
                    } catch (SecurityException se) {
                        throw new RuntimeException(String.format("Security Exception occurred when reading %s", this.selectObjectSuffixFile));
                    }
                }
            }
        }

        public void persistMap() {

            // write the map the filesystem
            File file = new File(this.selectObjectSuffixFile);
            try {
                if (file.exists()) {
                    file.delete();
                }
                file = new File(this.selectObjectSuffixFile);
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(gson.toJson(selectObjectSuffixMap, LinkedHashMap.class).getBytes());
            }  catch (IOException ioe) {
                throw new RuntimeException(String.format("IOException writing to file '%s'", this.selectObjectSuffixFile));

            }  catch (SecurityException se) {

            }

        }
        public void initSelectBodyContent() {
            try {
                if (this.selectQueryMap == null) {
                    Path path = FileSystems.getDefault().getPath(this.selectContentQueriesFile);
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
                throw new RuntimeException(String.format("IOException writing to file '%s'", this.selectContentQueriesFile));
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
