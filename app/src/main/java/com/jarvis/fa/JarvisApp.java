package com.jarvis.fa;

import android.app.Application;

public class JarvisApp extends Application {
    @Override public void onCreate(){
        super.onCreate();
        DiagnosticStore store=new DiagnosticStore(this);
        store.info("APP","Application started");
        Thread.UncaughtExceptionHandler previous=Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread,error)->{
            try{store.error("UNCAUGHT:"+thread.getName(),error);}catch(Exception ignored){}
            if(previous!=null)previous.uncaughtException(thread,error);
        });
    }
}
