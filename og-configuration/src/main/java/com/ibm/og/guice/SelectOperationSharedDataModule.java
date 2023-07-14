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
import com.ibm.og.guice.annotation.SelectSuffixMap;

import java.io.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SelectOperationSharedDataModule extends AbstractModule {

    private static Gson gson;
    private static TypeToken<ConcurrentHashMap<String, Integer>> mapType = new TypeToken<ConcurrentHashMap<String, Integer>>(){};
    public SuffixManager manager;
    @Singleton
    static public class SuffixManager {

        public ConcurrentHashMap<String, Integer> selectObjectSuffixMap;

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
    }

    @Inject
    public SelectOperationSharedDataModule() {
        System.out.println("SelectObjectContentModule constructor...");
        this.gson = new GsonBuilder().create();
        //this.manager = new SuffixManager();
    }

//    @Provides
//    @Singleton
//    @SelectSuffixMap
//    public SuffixManager provideSuffixManager() {
//        return new SuffixManager();
//    }

    @Override
    protected void configure() {
        bind(SuffixManager.class)
                .annotatedWith(SelectSuffixMap.class).to(SuffixManager.class).in(Singleton.class);
    }
}
