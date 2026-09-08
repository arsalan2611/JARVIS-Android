package com.jarvis.fa;

import android.content.*;
import android.os.Bundle;
import android.service.voice.VoiceInteractionService;

public class JarvisVoiceInteractionService extends VoiceInteractionService {
    @Override public void onReady(){
        super.onReady();
        getSharedPreferences("jarvis_data",MODE_PRIVATE).edit()
            .putBoolean("systemAssistantReady",true)
            .putString("assistantMode","SYSTEM_ASSISTANT")
            .apply();
        new HotwordCoordinator(this).reconcile();
        try{setInvocationEffectEnabled(true);}catch(Exception ignored){}
    }
    @Override public void onPrepareToShowSession(Bundle args,int flags){
        super.onPrepareToShowSession(args,flags);
        getSharedPreferences("jarvis_data",MODE_PRIVATE).edit().putLong("lastAssistantInvocation",System.currentTimeMillis()).apply();
    }
    @Override public void onShutdown(){
        getSharedPreferences("jarvis_data",MODE_PRIVATE).edit()
            .putBoolean("systemAssistantReady",false)
            .putString("assistantMode","FALLBACK_WAKE")
            .apply();
        super.onShutdown();
    }
    @Override public void onLaunchVoiceAssistFromKeyguard(){
        Intent i=new Intent(this,MainActivity.class)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra("auto_listen",true)
            .putExtra("from_keyguard_assist",true);
        startActivity(i);
    }
}
