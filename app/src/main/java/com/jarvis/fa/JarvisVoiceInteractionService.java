package com.jarvis.fa;

import android.os.Bundle;
import android.service.voice.VoiceInteractionService;

public class JarvisVoiceInteractionService extends VoiceInteractionService {
    @Override public void onReady(){
        super.onReady();
        getSharedPreferences("jarvis_data",MODE_PRIVATE).edit().putBoolean("systemAssistantReady",true).apply();
    }
    @Override public void onShutdown(){
        getSharedPreferences("jarvis_data",MODE_PRIVATE).edit().putBoolean("systemAssistantReady",false).apply();
        super.onShutdown();
    }
    @Override public void onLaunchVoiceAssistFromKeyguard(){
        showSession(new Bundle(),0);
    }
}
