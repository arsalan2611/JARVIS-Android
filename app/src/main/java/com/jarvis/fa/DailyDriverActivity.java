package com.jarvis.fa;

import android.Manifest;
import android.app.*;
import android.app.role.RoleManager;
import android.content.*;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;

public class DailyDriverActivity extends Activity {
    private static final int REQ_ROLE=9421;
    private final int bg=Color.rgb(3,8,15),surface=Color.rgb(8,17,29),surface2=Color.rgb(11,24,39),stroke=Color.rgb(28,63,82),accent=Color.rgb(69,226,255),text=Color.WHITE,muted=Color.rgb(139,165,183),good=Color.rgb(114,236,184),warn=Color.rgb(244,194,91);
    private TextView score,status,checklist,links,assistantDiag;
    private Button complete;
    private AssistantReadiness.Report report;
    private ServiceHealth serviceHealth;
    private SharedPreferences prefs;

    private int dp(int v){return(int)(v*getResources().getDisplayMetrics().density+.5f);}
    private GradientDrawable shape(int c,int r){GradientDrawable g=new GradientDrawable();g.setColor(c);g.setCornerRadius(dp(r));g.setStroke(dp(1),stroke);return g;}
    private TextView tv(String s,int z){TextView v=new TextView(this);v.setText(s);v.setTextColor(text);v.setTextSize(z);v.setPadding(dp(12),dp(8),dp(12),dp(8));return v;}
    private Button btn(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextColor(text);b.setBackground(shape(surface2,18));return b;}
    private LinearLayout card(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(16),dp(14),dp(16),dp(14));c.setBackground(shape(surface,22));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(7),0,dp(7));c.setLayoutParams(p);return c;}

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        if(Intent.ACTION_ASSIST.equals(getIntent().getAction())){
            startActivity(new Intent(this,JarvisVoiceActivity.class));finish();return;
        }
        prefs=getSharedPreferences("jarvis_daily_driver",MODE_PRIVATE);
        if(prefs.getBoolean("setup_complete",false) && !getIntent().getBooleanExtra("force_check",false)){
            startActivity(new Intent(this,JarvisHomeActivity.class));finish();return;
        }
        serviceHealth=new ServiceHealth(this);
        build();
    }

    @Override protected void onResume(){super.onResume();if(score!=null)refresh();}
    @Override protected void onDestroy(){if(serviceHealth!=null)serviceHealth.shutdown();super.onDestroy();}

    private void build(){
        ScrollView sc=new ScrollView(this);sc.setFillViewport(true);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(18),dp(20),dp(18),dp(30));root.setBackgroundColor(bg);root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        TextView title=tv("JARVIS • ASSISTANT SETUP",25);title.setTextColor(accent);title.setLetterSpacing(.08f);root.addView(title);
        TextView sub=tv("تنظیم دستیار اصلی Android با مسیر جایگزین خودکار",12);sub.setTextColor(muted);root.addView(sub);

        LinearLayout hero=card();score=tv("",36);score.setGravity(Gravity.CENTER);hero.addView(score);status=tv("",14);status.setGravity(Gravity.CENTER);hero.addView(status);root.addView(hero);

        LinearLayout assist=card();TextView al=tv("SYSTEM ASSISTANT",10);al.setTextColor(muted);al.setLetterSpacing(.08f);assist.addView(al);assistantDiag=tv("در حال بررسی وضعیت Assistant…",12);assistantDiag.setTextColor(muted);assist.addView(assistantDiag);
        Button assistantBtn=btn("انتخاب JARVIS به‌عنوان Assistant اصلی");assistantBtn.setTextColor(Color.rgb(0,24,31));assistantBtn.setBackground(shape(accent,22));assistantBtn.setOnClickListener(v->requestRole());assist.addView(assistantBtn,new LinearLayout.LayoutParams(-1,dp(56)));
        Button defaultApps=btn("باز کردن Default Apps اندروید");defaultApps.setOnClickListener(v->openDefaultAppsSettings(true));assist.addView(defaultApps,new LinearLayout.LayoutParams(-1,dp(48)));
        root.addView(assist);

        LinearLayout checks=card();TextView lab=tv("READINESS CHECKLIST",10);lab.setTextColor(muted);lab.setLetterSpacing(.08f);checks.addView(lab);checklist=tv("",13);checks.addView(checklist);root.addView(checks);

        LinearLayout live=card();TextView liveLab=tv("LIVE CONNECTIONS",10);liveLab.setTextColor(muted);liveLab.setLetterSpacing(.08f);live.addView(liveLab);links=tv("در حال بررسی اتصال واقعی Backend و PC…",12);links.setTextColor(muted);live.addView(links);root.addView(live);

        Button fix=btn("رفع مهم‌ترین مورد باقی‌مانده");fix.setOnClickListener(v->fixNext());root.addView(fix,new LinearLayout.LayoutParams(-1,dp(52)));
        complete=btn("تأیید Daily Driver و ورود به Command Center");complete.setOnClickListener(v->completeSetup());root.addView(complete,new LinearLayout.LayoutParams(-1,dp(52)));
        Button setup=btn("تنظیمات کامل JARVIS");setup.setOnClickListener(v->startActivity(new Intent(this,JarvisSetupActivity.class)));root.addView(setup,new LinearLayout.LayoutParams(-1,dp(48)));
        Button voice=btn("تست مکالمه فارسی");voice.setOnClickListener(v->startActivity(new Intent(this,JarvisVoiceActivity.class)));root.addView(voice,new LinearLayout.LayoutParams(-1,dp(48)));

        TextView note=tv("اگر پنجره انتخاب Assistant روی مدل گوشی باز نشود، Build 44 به‌صورت خودکار صفحه Default Apps یا Voice Input را باز می‌کند و وضعیت را همین‌جا نمایش می‌دهد.",11);note.setTextColor(muted);root.addView(note);
        sc.addView(root);setContentView(sc);
    }

    private void refresh(){
        report=new AssistantReadiness(this).inspect();
        score.setText(report.score+"%");
        score.setTextColor(report.score>=90?good:(report.score>=70?accent:warn));
        status.setText(report.score>=90?"موارد اصلی Daily Driver آماده‌اند":report.score>=70?"چند تنظیم نهایی باقی مانده":"چند مورد ضروری باید آماده شود");
        StringBuilder b=new StringBuilder();
        line(b,"دستیار اصلی Android",report.roleHeld&&report.activeVoiceService);line(b,"میکروفون",report.micGranted);line(b,"تشخیص گفتار",report.speechAvailable);line(b,"اعلان‌ها",report.notificationsGranted);line(b,"یادآوری دقیق",report.exactAlarmsReady);line(b,"Backend امن HTTPS",report.secureBackendReady);line(b,"بدون Crash بررسی‌نشده",report.noPendingCrash);
        b.append(report.onDeviceSpeech?"✓ گفتار محلی روی دستگاه\n":"• گفتار محلی موجود نیست؛ Accuracy Mode فعال است\n");checklist.setText(b.toString());
        boolean ready=report.score>=90;complete.setEnabled(ready);complete.setAlpha(ready?1f:.45f);
        assistantDiag.setText(buildAssistantDiagnostic());
        assistantDiag.setTextColor((report.roleHeld&&report.activeVoiceService)?good:warn);
        checkLiveConnections();
    }

    private String buildAssistantDiagnostic(){
        boolean roleAvailable=false,roleHeld=false;
        if(Build.VERSION.SDK_INT>=29){try{RoleManager rm=(RoleManager)getSystemService(ROLE_SERVICE);roleAvailable=rm!=null&&rm.isRoleAvailable(RoleManager.ROLE_ASSISTANT);roleHeld=roleAvailable&&rm.isRoleHeld(RoleManager.ROLE_ASSISTANT);}catch(Exception ignored){}}
        boolean assistResolvable=!getPackageManager().queryIntentActivities(new Intent(Intent.ACTION_ASSIST),0).isEmpty();
        return "Role Assistant: "+(roleAvailable?"موجود":"در این گوشی گزارش نشده")+"\n"+
               "JARVIS Role: "+(roleHeld?"انتخاب شده":"انتخاب نشده")+"\n"+
               "VoiceInteractionService: "+(report!=null&&report.activeVoiceService?"فعال":"غیرفعال")+"\n"+
               "ACTION_ASSIST: "+(assistResolvable?"قابل Resolve":"قابل Resolve نیست");
    }

    private void checkLiveConnections(){if(serviceHealth==null)return;links.setText("در حال بررسی اتصال واقعی Backend و PC…");links.setTextColor(muted);serviceHealth.check(r->{links.setText(r.summary());links.setTextColor((r.backendOnline||r.pcOnline)?good:muted);});}
    private void line(StringBuilder b,String s,boolean ok){b.append(ok?"✓ ":"✕ ").append(s).append('\n');}

    private void completeSetup(){if(report==null||report.score<90){Toast.makeText(this,"اول موارد ضروری Daily Driver را کامل کن.",Toast.LENGTH_SHORT).show();return;}prefs.edit().putBoolean("setup_complete",true).putLong("completed_at",System.currentTimeMillis()).apply();Intent i=new Intent(this,JarvisHomeActivity.class);i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);startActivity(i);finish();}

    private void fixNext(){if(report==null){refresh();return;}if(!report.roleHeld||!report.activeVoiceService){requestRole();return;}if(!report.micGranted){requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},9422);return;}if(!report.notificationsGranted&&Build.VERSION.SDK_INT>=33){requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},9423);return;}if(!report.exactAlarmsReady&&Build.VERSION.SDK_INT>=31){try{startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,android.net.Uri.parse("package:"+getPackageName())));return;}catch(Exception ignored){}}if(!report.secureBackendReady){startActivity(new Intent(this,JarvisSetupActivity.class));return;}if(!report.noPendingCrash){startActivity(new Intent(this,DiagnosticsActivity.class));return;}Toast.makeText(this,"موارد اصلی آماده‌اند؛ Daily Driver را تأیید کن.",Toast.LENGTH_SHORT).show();}

    private boolean canOpen(Intent i){try{ResolveInfo r=getPackageManager().resolveActivity(i,0);return r!=null;}catch(Exception e){return false;}}
    private void requestRole(){
        assistantDiag.setText("در حال باز کردن انتخاب Assistant…");assistantDiag.setTextColor(accent);
        if(Build.VERSION.SDK_INT>=29){try{RoleManager rm=(RoleManager)getSystemService(ROLE_SERVICE);if(rm!=null&&rm.isRoleAvailable(RoleManager.ROLE_ASSISTANT)){if(rm.isRoleHeld(RoleManager.ROLE_ASSISTANT)){Toast.makeText(this,"JARVIS همین حالا Assistant انتخاب‌شده است.",Toast.LENGTH_LONG).show();refresh();return;}Intent role=rm.createRequestRoleIntent(RoleManager.ROLE_ASSISTANT);if(canOpen(role)){startActivityForResult(role,REQ_ROLE);return;}}}catch(Exception e){assistantDiag.setText("RoleManager خطا داد؛ مسیر جایگزین باز می‌شود.\n"+e.getClass().getSimpleName());}}
        openDefaultAppsSettings(false);
    }

    private void openDefaultAppsSettings(boolean manual){
        Intent i=new Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS);
        if(canOpen(i)){assistantDiag.setText("صفحه Default Apps باز شد. وارد Digital assistant app شو و JARVIS را انتخاب کن.");startActivity(i);return;}
        Intent voice=new Intent(Settings.ACTION_VOICE_INPUT_SETTINGS);
        if(canOpen(voice)){assistantDiag.setText("صفحه Voice Input باز شد. بخش Assistant / Voice assistant را بررسی کن.");startActivity(voice);return;}
        Intent app=new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,android.net.Uri.parse("package:"+getPackageName()));
        if(canOpen(app)){assistantDiag.setText("این گوشی صفحه مستقیم Assistant را در اختیار برنامه نگذاشت؛ App settings باز شد.");startActivity(app);return;}
        assistantDiag.setText("Android هیچ‌کدام از مسیرهای استاندارد Assistant را Resolve نکرد. مدل گوشی و نسخه Android را ارسال کن.");assistantDiag.setTextColor(warn);Toast.makeText(this,"صفحه Assistant روی این گوشی پیدا نشد.",Toast.LENGTH_LONG).show();
    }

    @Override protected void onActivityResult(int r,int c,Intent d){super.onActivityResult(r,c,d);if(r==REQ_ROLE){Toast.makeText(this,c==RESULT_OK?"درخواست Assistant تأیید شد.":"انتخاب Assistant تأیید نشد؛ وضعیت را دوباره بررسی کن.",Toast.LENGTH_LONG).show();}refresh();}
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);refresh();}
}
