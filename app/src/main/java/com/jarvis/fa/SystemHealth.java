package com.jarvis.fa;

import android.Manifest;import android.content.*;import android.content.pm.PackageManager;import android.os.Build;import android.speech.SpeechRecognizer;import android.service.voice.VoiceInteractionService;

public class SystemHealth {
    private final Context c;
    public SystemHealth(Context x){c=x.getApplicationContext();}
    public String report(){StringBuilder s=new StringBuilder();boolean mic=c.checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED;boolean contacts=c.checkSelfPermission(Manifest.permission.READ_CONTACTS)==PackageManager.PERMISSION_GRANTED;boolean speech=SpeechRecognizer.isRecognitionAvailable(c);boolean assistant=false;try{assistant=VoiceInteractionService.isActiveService(c,new ComponentName(c,JarvisVoiceInteractionService.class));}catch(Exception ignored){}s.append("JARVIS Health\n");s.append("Android: ").append(Build.VERSION.RELEASE).append(" / API ").append(Build.VERSION.SDK_INT).append('\n');s.append("Microphone: ").append(mic?"OK":"PERMISSION").append('\n');s.append("Contacts: ").append(contacts?"OK":"PERMISSION").append('\n');s.append("Speech: ").append(speech?"OK":"UNAVAILABLE").append('\n');s.append("System Assistant: ").append(assistant?"ACTIVE":"INACTIVE").append('\n');return s.toString().trim();}
}
