package com.jarvis.fa;

import android.app.Application;

public class JarvisApp extends Application {
    @Override public void onCreate(){
        super.onCreate();
        DiagnosticStore store=new DiagnosticStore(this);
        if(store.hasPendingCrash()) store.info("RECOVERY","Previous process ended with an uncaught crash; diagnostics available.");
        store.info("APP","Application started");
        Thread.UncaughtExceptionHandler previous=Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread,error)->{
            try{store.markCrash("UNCAUGHT:"+thread.getName(),error);}catch(Exception ignored){}
            if(previous!=null)previous.uncaughtException(thread,error);
        });
    }
}
