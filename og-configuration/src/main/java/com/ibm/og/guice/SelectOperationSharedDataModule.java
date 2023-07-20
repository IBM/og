package com.ibm.og.guice;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.ibm.og.guice.annotation.SelectFileBodies;
import com.ibm.og.guice.annotation.SelectSuffixMap;
import com.ibm.og.http.Bodies;
import com.ibm.og.json.ChoiceConfig;
import com.ibm.og.json.OperationConfig;
import com.ibm.og.json.SelectionConfig;
import com.ibm.og.json.WriteSelectBodyConfig;

import java.awt.*;
import java.io.*;
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

    public SuffixManager manager;
    public FileBodies fileBodies;
    @Singleton
    static public class SuffixManager {

        public ConcurrentHashMap<String, Integer> selectObjectSuffixMap;

        public ArrayList<LinkedHashMap<String, String>> selectQueryMap;

        public SuffixManager() {
            //selectObjectSuffixMap = new ConcurrentHashMap<>();
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

                    } catch (IOException ioe) {

                    } catch (SecurityException se) {

                    }
                }
            }
        }

        public void persistMap() {

             // write the map the filesystem
             File file = new File("/var/log/og/selectObjectSuffix.json");
             try {
             if (file.exists()) {
             file.delete();
             }
             file = new File("/var/log/og/selectObjectSuffix.json");
             FileOutputStream fos = new FileOutputStream(file);
             fos.write(gson.toJson(selectObjectSuffixMap, LinkedHashMap.class).getBytes());
             } catch (FileNotFoundException fne) {

             } catch (IOException ioe) {

             } catch (SecurityException se) {

             }

        }
        public void initSelectBodyContent() {
            try {
                if (this.selectQueryMap == null) {
                    Path path = FileSystems.getDefault().getPath("/var/log/og/selectContent.json");
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
            return "";
        }
    }

    @Inject
    public SelectOperationSharedDataModule() {
        System.out.println("SelectObjectContentModule constructor...");
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
