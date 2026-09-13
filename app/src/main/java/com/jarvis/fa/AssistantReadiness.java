package com.jarvis.fa;

import android.Manifest;
import android.app.AlarmManager;
import android.app.role.RoleManager;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Build;
import android.service.voice.VoiceInteractionService;
import android.speech.SpeechRecognizer;

public class AssistantReadiness {
    public static class Report {
        public boolean roleAvailable, roleHeld, activeVoiceService, micGranted, speechAvailable, onDeviceSpeech;
        public boolean notificationsGranted, exactAlarmsReady, secureBackendReady, noPendingCrash;
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

        if(Build.VERSION.SDK_INT>=33){
            r.notificationsGranted=c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED;
        }else r.notificationsGranted=true;

        try{
            AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
            r.exactAlarmsReady=Build.VERSION.SDK_INT<31||am==null||am.canScheduleExactAlarms();
        }catch(Exception ignored){r.exactAlarmsReady=Build.VERSION.SDK_INT<31;}

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

        try{
            ProductionConfig p=new ProductionConfig(c);
            r.secureBackendReady=p.productionMode()&&p.backendReady()&&p.backendTls();
        }catch(Exception ignored){}

        try{r.noPendingCrash=!new DiagnosticStore(c).hasPendingCrash();}
        catch(Exception ignored){r.noPendingCrash=true;}

        int s=0;
        if(r.micGranted)s+=15;
        if(r.speechAvailable)s+=15;
        if(r.roleAvailable)s+=5;
        if(r.roleHeld)s+=20;
        if(r.activeVoiceService)s+=15;
        if(r.notificationsGranted)s+=10;
        if(r.exactAlarmsReady)s+=5;
        if(r.secureBackendReady)s+=10;
        if(r.noPendingCrash)s+=5;
        r.score=s;

        StringBuilder b=new StringBuilder();
        b.append("Daily-driver readiness: ").append(r.score).append("/100\n\n");
        line(b,"نقش Assistant روی دستگاه",r.roleAvailable);
        line(b,"JARVIS دستیار اصلی گوشی",r.roleHeld);
        line(b,"VoiceInteractionService فعال",r.activeVoiceService);
        line(b,"میکروفون",r.micGranted);
        line(b,"تشخیص گفتار فارسی",r.speechAvailable);
        line(b,"اعلان‌ها",r.notificationsGranted);
        line(b,"یادآوری دقیق",r.exactAlarmsReady);
        line(b,"Backend امن HTTPS",r.secureBackendReady);
        line(b,"Crash بررسی‌نشده ندارد",r.noPendingCrash);
        b.append(r.onDeviceSpeech?"✓ گفتار On-device روی این گوشی موجود است":"• گفتار On-device موجود نیست؛ Accuracy Mode قابل استفاده است");

        if(!r.roleHeld) b.append("\n\nاولویت ۱: JARVIS را به‌عنوان Assistant اصلی Android انتخاب کن.");
        else if(!r.activeVoiceService) b.append("\n\nاولویت ۱: نقش ثبت شده ولی VoiceInteractionService فعال نیست؛ Assistant را یک بار عوض کن و دوباره JARVIS را انتخاب کن.");
        else if(!r.micGranted) b.append("\n\nاولویت ۱: مجوز میکروفون را فعال کن.");
        else if(!r.secureBackendReady) b.append("\n\nبرای AI کامل: Production Backend امن HTTPS را در Setup تنظیم کن.");
        else if(!r.noPendingCrash) b.append("\n\nیک Crash قبلی ثبت شده؛ Diagnostics را باز و بررسی کن.");
        else if(r.score>=90) b.append("\n\nJARVIS برای استفاده روزمره تقریباً آماده است.");

        r.text=b.toString();
        return r;
    }

    private static void line(StringBuilder b,String title,boolean ok){b.append(ok?"✓ ":"✕ ").append(title).append('\n');}
}
