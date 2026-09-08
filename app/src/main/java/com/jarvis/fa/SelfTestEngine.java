package com.jarvis.fa;

import android.Manifest;
import android.app.AlarmManager;
import android.content.*;
import android.content.pm.PackageManager;
import android.net.*;
import android.os.Build;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.service.voice.VoiceInteractionService;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class SelfTestEngine {
    public interface Callback { void onResult(String report); }
    private final Context c;
    private final ProductionConfig prod;
    private final PcBridge pc;
    private final ExecutorService ex=Executors.newSingleThreadExecutor();

    public SelfTestEngine(Context context){
        c=context.getApplicationContext();
        prod=new ProductionConfig(c);
        pc=new PcBridge(c);
    }

    public void run(Callback cb){
        ex.submit(()->{
            String report=buildReport();
            new android.os.Handler(android.os.Looper.getMainLooper()).post(()->cb.onResult(report));
        });
    }

    private String buildReport(){
        ArrayList<String> rows=new ArrayList<>();
        int ok=0,warn=0,fail=0;

        Result mic=perm(Manifest.permission.RECORD_AUDIO,"Microphone"); rows.add(mic.line); if(mic.ok)ok++; else fail++;
        Result contacts=perm(Manifest.permission.READ_CONTACTS,"Contacts"); rows.add(contacts.line); if(contacts.ok)ok++; else warn++;
        if(Build.VERSION.SDK_INT>=33){Result n=perm(Manifest.permission.POST_NOTIFICATIONS,"Notifications");rows.add(n.line);if(n.ok)ok++;else warn++;}

        boolean speech=SpeechRecognizer.isRecognitionAvailable(c);
        rows.add(mark(speech,"Speech recognizer",speech?"available":"unavailable")); if(speech)ok++;else fail++;
        if(Build.VERSION.SDK_INT>=31){boolean local=SpeechRecognizer.isOnDeviceRecognitionAvailable(c);rows.add(mark(local,"On-device speech",local?"available":"fallback to system"));if(local)ok++;else warn++;}

        boolean tts=!c.getPackageManager().queryIntentServices(new Intent(TextToSpeech.Engine.INTENT_ACTION_TTS_SERVICE),0).isEmpty();
        rows.add(mark(tts,"TTS engine",tts?"available":"not found"));if(tts)ok++;else fail++;

        boolean assistant=false;
        try{assistant=VoiceInteractionService.isActiveService(c,new ComponentName(c,JarvisVoiceInteractionService.class));}catch(Exception ignored){}
        boolean wake=c.getSharedPreferences("jarvis_data",Context.MODE_PRIVATE).getBoolean("wakeEnabled",false);
        rows.add(mark(assistant||wake,"Wake / Assistant",assistant?"system assistant active":wake?"fallback wake enabled":"inactive"));if(assistant||wake)ok++;else warn++;

        boolean exact=true;
        if(Build.VERSION.SDK_INT>=31){try{exact=c.getSystemService(AlarmManager.class).canScheduleExactAlarms();}catch(Exception ignored){exact=false;}}
        rows.add(mark(exact,"Exact reminders",exact?"ready":"exact-alarm permission unavailable"));if(exact)ok++;else warn++;

        boolean net=networkAvailable();
        rows.add(mark(net,"Internet",net?"online":"offline"));if(net)ok++;else warn++;

        String mem=new StructuredMemory(c).context();
        boolean memory=mem!=null;
        rows.add(mark(memory,"Structured memory",memory?"storage readable":"storage error"));if(memory)ok++;else fail++;

        if(prod.productionMode()){
            if(!prod.backendReady()){rows.add("✗ Production backend: configuration incomplete");fail++;}
            else {boolean ping=ping(prod.backendUrl(),"/health");rows.add(mark(ping,"Production backend",ping?"health OK":"unreachable"));if(ping)ok++;else fail++;}
        }else {rows.add("! Production backend: development mode");warn++;}

        if(pc.configured()){
            boolean ping=ping(pc.endpoint(),"/health");rows.add(mark(ping,"PC Bridge",ping?"health OK":"unreachable"));if(ping)ok++;else warn++;
        }else {rows.add("! PC Bridge: not configured");warn++;}

        int total=ok+warn+fail;
        int score=total==0?0:(int)Math.round((ok+warn*0.5)*100.0/total);
        StringBuilder s=new StringBuilder();
        s.append("JARVIS SELF-TEST\n");
        s.append("Score: ").append(score).append("/100  •  OK ").append(ok).append("  WARN ").append(warn).append("  FAIL ").append(fail).append("\n\n");
        for(String r:rows)s.append(r).append('\n');
        s.append("\nAndroid ").append(Build.VERSION.RELEASE).append(" • API ").append(Build.VERSION.SDK_INT);
        return s.toString().trim();
    }

    private Result perm(String permission,String name){boolean x=c.checkSelfPermission(permission)==PackageManager.PERMISSION_GRANTED;return new Result(x,mark(x,name,x?"granted":"permission required"));}
    private String mark(boolean ok,String name,String detail){return (ok?"✓ ":"! ")+name+": "+detail;}
    private boolean networkAvailable(){try{ConnectivityManager m=(ConnectivityManager)c.getSystemService(Context.CONNECTIVITY_SERVICE);Network n=m.getActiveNetwork();if(n==null)return false;NetworkCapabilities cap=m.getNetworkCapabilities(n);return cap!=null&&cap.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);}catch(Exception e){return false;}}
    private boolean ping(String base,String path){try{String u=(base==null?"":base.trim()).replaceAll("/$","");if(u.isEmpty())return false;HttpURLConnection h=(HttpURLConnection)new URL(u+path).openConnection();h.setRequestMethod("GET");h.setConnectTimeout(3500);h.setReadTimeout(3500);h.setUseCaches(false);int code=h.getResponseCode();h.disconnect();return code>=200&&code<300;}catch(Exception e){return false;}}
    public void shutdown(){ex.shutdownNow();}
    private static class Result{final boolean ok;final String line;Result(boolean x,String l){ok=x;line=l;}}
}
