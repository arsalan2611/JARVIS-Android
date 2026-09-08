package com.jarvis.fa;

import android.Manifest;
import android.app.role.RoleManager;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Build;
import android.service.voice.VoiceInteractionService;
import android.speech.SpeechRecognizer;
import java.util.*;

public class AssistantReadiness {
    public static class Report {
        public boolean roleAvailable, roleHeld, activeVoiceService, micGranted, speechAvailable, onDeviceSpeech;
        public int score;
        public String text="";
    }
    private final Context c;
    public AssistantReadiness(Context context){c=context.getApplicationContext();}
    public Report inspect(){
        Report r=new Report();
        r.micGranted=c.checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED;
        r.speechAvailable=SpeechRecognizer.isRecognitionAvailable(c);
        r.onDeviceSpeech=Build.VERSION.SDK_INT>=31&&SpeechRecognizer.isOnDeviceRecognitionAvailable(c);
        try{
            r.activeVoiceService=VoiceInteractionService.isActiveService(c,new ComponentName(c,JarvisVoiceInteractionService.class));
        }catch(Exception ignored){}
        if(Build.VERSION.SDK_INT>=29){
            try{
                RoleManager rm=(RoleManager)c.getSystemService(Context.ROLE_SERVICE);
                r.roleAvailable=rm!=null&&rm.isRoleAvailable(RoleManager.ROLE_ASSISTANT);
                r.roleHeld=r.roleAvailable&&rm.isRoleHeld(RoleManager.ROLE_ASSISTANT);
            }catch(Exception ignored){}
        }else{
            r.roleAvailable=true;
            r.roleHeld=r.activeVoiceService;
        }
        int s=0;if(r.micGranted)s+=20;if(r.speechAvailable)s+=20;if(r.roleAvailable)s+=15;if(r.roleHeld)s+=25;if(r.activeVoiceService)s+=20;r.score=s;
        StringBuilder b=new StringBuilder();
        b.append("Assistant readiness: ").append(r.score).append("/100\n\n");
        line(b,"نقش Assistant روی دستگاه",r.roleAvailable);
        line(b,"JARVIS دارنده نقش Assistant",r.roleHeld);
        line(b,"VoiceInteractionService فعال سیستم",r.activeVoiceService);
        line(b,"مجوز میکروفون",r.micGranted);
        line(b,"Speech Recognizer",r.speechAvailable);
        b.append(r.onDeviceSpeech?"✓ گفتار On-device روی این گوشی موجود است":"• گفتار On-device روی این گوشی موجود نیست؛ Accuracy Mode قابل استفاده است");
        if(!r.roleHeld) b.append("\n\nاقدام: از Command Center روی «دستیار اصلی گوشی» بزن و JARVIS را تأیید کن.");
        else if(!r.activeVoiceService) b.append("\n\nنقش ثبت شده اما VoiceInteractionService هنوز active نیست؛ یک بار Assistant را تغییر بده و دوباره JARVIS را انتخاب کن.");
        r.text=b.toString();return r;
    }
    private static void line(StringBuilder b,String title,boolean ok){b.append(ok?"✓ ":"✕ ").append(title).append('\n');}
}
