package com.jarvis.fa;

import android.app.*;
import android.content.*;
import android.os.*;
import android.speech.*;
import android.speech.tts.TextToSpeech;
import java.util.*;

public class WakeWordService extends Service implements TextToSpeech.OnInitListener {
    public static final String ACTION_START="com.jarvis.fa.START_WAKE", ACTION_STOP="com.jarvis.fa.STOP_WAKE";
    private SpeechRecognizer r;
    private TextToSpeech tts;
    private final Handler h=new Handler(Looper.getMainLooper());
    private PowerManager.WakeLock wakeLock;
    private boolean running=false;
    private boolean onDevice=false;
    private String wake="جارویس";
    private long lastTrigger=0;
    private int failures=0;
    private String lastPartial="";

    @Override public void onCreate(){
        super.onCreate();
        channel();
        tts=new TextToSpeech(this,this);
        wake=getSharedPreferences("jarvis_data",MODE_PRIVATE).getString("wakeWord","جارویس");
        PowerManager pm=(PowerManager)getSystemService(POWER_SERVICE);
        if(pm!=null) wakeLock=pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"JARVIS:WakeListener");
        setupRecognizer();
    }

    @Override public int onStartCommand(Intent i,int flags,int id){
        String a=i==null?ACTION_START:i.getAction();
        if(ACTION_STOP.equals(a)){
            stopWake();
            return START_NOT_STICKY;
        }
        running=true;
        failures=0;
        acquireWakeLock();
        startForeground(2101,note("شنود فعال • "+engineLabel()+" • بگو «"+wake+"»"));
        listen(300);
        return START_STICKY;
    }

    private void stopWake(){
        running=false;
        h.removeCallbacksAndMessages(null);
        try{if(r!=null)r.cancel();}catch(Exception ignored){}
        releaseWakeLock();
        stopForeground(true);
        stopSelf();
    }

    private void channel(){
        if(Build.VERSION.SDK_INT>=26){
            NotificationChannel c=new NotificationChannel("jarvis_wake","شنود جارویس",NotificationManager.IMPORTANCE_LOW);
            c.setDescription("شنود محلی برای تشخیص کلمه جارویس");
            NotificationManager nm=getSystemService(NotificationManager.class);
            if(nm!=null)nm.createNotificationChannel(c);
        }
    }

    private Notification note(String s){
        Intent open=new Intent(this,MainActivity.class);
        PendingIntent pi=PendingIntent.getActivity(this,1,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Intent stop=new Intent(this,WakeWordService.class).setAction(ACTION_STOP);
        PendingIntent ps=PendingIntent.getService(this,2,stop,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,"jarvis_wake"):new Notification.Builder(this);
        return b.setContentTitle("JARVIS V14 • Wake Engine")
                .setContentText(s)
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setOngoing(true)
                .setContentIntent(pi)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel,"توقف",ps)
                .build();
    }

    private String engineLabel(){return onDevice?"ON-DEVICE":"SYSTEM FALLBACK";}

    private void setupRecognizer(){
        destroyRecognizer();
        try{
            if(Build.VERSION.SDK_INT>=31 && SpeechRecognizer.isOnDeviceRecognitionAvailable(this)){
                r=SpeechRecognizer.createOnDeviceSpeechRecognizer(this);
                onDevice=true;
            }else if(SpeechRecognizer.isRecognitionAvailable(this)){
                r=SpeechRecognizer.createSpeechRecognizer(this);
                onDevice=false;
            }else{
                r=null;
                onDevice=false;
                return;
            }
            r.setRecognitionListener(listener);
            getSharedPreferences("jarvis_data",MODE_PRIVATE).edit().putString("wakeEngine",engineLabel()).apply();
        }catch(Throwable first){
            try{
                r=SpeechRecognizer.createSpeechRecognizer(this);
                onDevice=false;
                r.setRecognitionListener(listener);
                getSharedPreferences("jarvis_data",MODE_PRIVATE).edit().putString("wakeEngine",engineLabel()).apply();
            }catch(Throwable ignored){r=null;}
        }
    }

    private final RecognitionListener listener=new RecognitionListener(){
        public void onReadyForSpeech(Bundle b){failures=0;}
        public void onBeginningOfSpeech(){}
        public void onRmsChanged(float x){}
        public void onBufferReceived(byte[]b){}
        public void onEndOfSpeech(){}
        public void onError(int e){
            if(!running)return;
            failures=Math.min(failures+1,8);
            if(failures>=5){setupRecognizer();failures=2;}
            listen(backoff());
        }
        public void onResults(Bundle b){
            ArrayList<String>x=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if(x!=null)for(String s:x){if(process(s,false))break;}
            if(running)listen(350);
        }
        public void onPartialResults(Bundle b){
            ArrayList<String>x=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if(x!=null&&!x.isEmpty()){
                String s=x.get(0);
                if(!s.equals(lastPartial)){lastPartial=s;process(s,true);}
            }
        }
        public void onEvent(int a,Bundle b){}
    };

    private long backoff(){return Math.min(6500L,450L+(long)failures*700L);}

    private void listen(long delay){
        h.postDelayed(()->{
            if(!running)return;
            if(r==null){setupRecognizer();if(r==null){listen(5000);return;}}
            try{
                Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"fa-IR");
                i.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,true);
                i.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,3);
                i.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE,onDevice);
                r.startListening(i);
            }catch(Exception e){
                failures=Math.min(failures+1,8);
                if(running)listen(backoff());
            }
        },delay);
    }

    private boolean process(String s,boolean partial){
        if(s==null)return false;
        String normalized=s.replace('ي','ی').replace('ك','ک').trim();
        int x=normalized.indexOf(wake);
        if(x<0)return false;
        long now=System.currentTimeMillis();
        if(now-lastTrigger<3200)return true;
        String q=normalized.substring(x+wake.length()).trim();
        if(partial&&q.length()<3)return false;
        lastTrigger=now;
        lastPartial="";
        try{if(r!=null)r.cancel();}catch(Exception ignored){}
        if(q.isEmpty()){
            speak("بله، گوش می‌دهم.");
            getSharedPreferences("jarvis_data",MODE_PRIVATE).edit().putBoolean("wakeTriggered",true).apply();
            Intent open=new Intent(this,MainActivity.class)
                    .putExtra("auto_listen",true)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP);
            try{startActivity(open);}catch(Exception ignored){}
            if(running)listen(1800);
            return true;
        }
        getSharedPreferences("jarvis_data",MODE_PRIVATE).edit().putString("pendingWakeCommand",q).apply();
        Intent i=new Intent("com.jarvis.fa.WAKE_COMMAND").setPackage(getPackageName()).putExtra("command",q);
        sendBroadcast(i);
        speak("انجام می‌دهم.");
        if(running)listen(1800);
        return true;
    }

    private void acquireWakeLock(){
        try{if(wakeLock!=null&&!wakeLock.isHeld())wakeLock.acquire(10*60*1000L);}catch(Exception ignored){}
        if(running)h.postDelayed(this::refreshWakeLock,8*60*1000L);
    }
    private void refreshWakeLock(){
        if(!running)return;
        try{if(wakeLock!=null&&wakeLock.isHeld())wakeLock.release();}catch(Exception ignored){}
        acquireWakeLock();
    }
    private void releaseWakeLock(){try{if(wakeLock!=null&&wakeLock.isHeld())wakeLock.release();}catch(Exception ignored){}}
    private void destroyRecognizer(){try{if(r!=null)r.destroy();}catch(Exception ignored){}r=null;}

    private void speak(String s){if(tts!=null)tts.speak(s,TextToSpeech.QUEUE_FLUSH,null,"wake");}
    @Override public void onInit(int s){if(s==TextToSpeech.SUCCESS){tts.setLanguage(new Locale("fa","IR"));tts.setSpeechRate(.98f);}}
    @Override public void onDestroy(){
        running=false;
        h.removeCallbacksAndMessages(null);
        destroyRecognizer();
        releaseWakeLock();
        if(tts!=null)tts.shutdown();
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent i){return null;}
}
