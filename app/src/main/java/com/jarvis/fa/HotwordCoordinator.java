package com.jarvis.fa;

import android.content.*;
import android.os.Build;
import android.service.voice.VoiceInteractionService;

public class HotwordCoordinator {
    private final Context context;
    private final SharedPreferences prefs;
    public HotwordCoordinator(Context c){context=c.getApplicationContext();prefs=context.getSharedPreferences("jarvis_data",Context.MODE_PRIVATE);}
    public boolean isSystemAssistant(){
        try{return VoiceInteractionService.isActiveService(context,new ComponentName(context,JarvisVoiceInteractionService.class));}
        catch(Exception e){return false;}
    }
    public String mode(){return isSystemAssistant()?"SYSTEM_ASSISTANT":"FALLBACK_WAKE";}
    public String status(){
        if(isSystemAssistant())return "SYSTEM ASSISTANT • Assist Gesture آماده";
        if(prefs.getBoolean("wakeEnabled",false)){
            String engine=prefs.getString("wakeEngine","در حال تشخیص");
            return "WAKE • "+engine+" • شنود پس‌زمینه فعال";
        }
        return "WAKE • غیرفعال";
    }
    public boolean setFallbackWake(boolean on){
        if(isSystemAssistant()){
            stopFallback();
            prefs.edit().putBoolean("wakeEnabled",false).apply();
            return true;
        }
        prefs.edit().putBoolean("wakeEnabled",on).apply();
        Intent i=new Intent(context,WakeWordService.class).setAction(on?WakeWordService.ACTION_START:WakeWordService.ACTION_STOP);
        try{
            if(on && Build.VERSION.SDK_INT>=26)context.startForegroundService(i); else context.startService(i);
            return true;
        }catch(Exception e){prefs.edit().putBoolean("wakeEnabled",false).apply();return false;}
    }
    public void reconcile(){if(isSystemAssistant())stopFallback();}
    private void stopFallback(){
        try{context.startService(new Intent(context,WakeWordService.class).setAction(WakeWordService.ACTION_STOP));}catch(Exception ignored){}
    }
}
