/* Copyright (c) IBM Corporation 2023. All Rights Reserved.
 * Project name: Object Generator
 * This project is licensed under the Apache License 2.0, see LICENSE.
 */

package com.ibm.og.guice;

import com.google.common.collect.ImmutableMap;
import com.google.common.eventbus.EventBus;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.ibm.og.guice.annotation.SelectSuffixMap;
import com.ibm.og.json.OGConfig;
import com.ibm.og.test.LoadTestSubscriberExceptionHandler;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;

/**
 * A guice configuration module for wiring up list operation components
 *
 * @since 1.15.0
 */
public class SelectObjectContentModule extends AbstractModule {


    //private final OGConfig config;
    private Gson gson;

    private LinkedHashMap<String, String> selectQueryMap;
    @SelectSuffixMap
    SelectOperationSharedDataModule.SuffixManager suffixMap;

    private final LoadTestSubscriberExceptionHandler handler;
    private final EventBus eventBus;


    @Inject
    public SelectObjectContentModule(@SelectSuffixMap SelectOperationSharedDataModule.SuffixManager manager) {
//        checkNotNull(config);
//        this.config = config;
        this.suffixMap = manager;
        this.handler = new LoadTestSubscriberExceptionHandler();
        this.eventBus = new EventBus(this.handler);
        this.gson = new GsonBuilder().create();
    }

    @Override
    protected void configure() {
        bindConstant().annotatedWith(Names.named("writeSelectObject.weight")).to(1.0);

        int i = 0;
        System.out.println("test" + i);
    }

    public void loadSuffixes() {
        this.suffixMap.initMap();
    }

    public void persistSuffixes() {
        this.suffixMap.persistMap();
    }

    public void loadSelectContent() {
        try {
            Path path = FileSystems.getDefault().getPath("/var/log/og/selectContent.json");
            if (Files.exists(path)) {
                byte[] bytes = Files.readAllBytes(path);
                this.selectQueryMap = this.gson.fromJson(new String(bytes), this.selectQueryMap.getClass());
            } else {
                this.selectQueryMap = new LinkedHashMap<>();
            }
        } catch (IOException ioe) {

        }
    }

}
