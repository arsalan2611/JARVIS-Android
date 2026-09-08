package com.jarvis.fa;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.util.*;

public class SecurityCenterActivity extends Activity {
    private final int bg=Color.rgb(2,8,16),panel=Color.rgb(7,20,34),accent=Color.rgb(56,225,255),text=Color.WHITE,muted=Color.rgb(145,177,198),good=Color.rgb(107,235,181);
    private TextView report;
    private int dp(int v){return(int)(v*getResources().getDisplayMetrics().density+.5f);} private GradientDrawable shape(int c,int r){GradientDrawable g=new GradientDrawable();g.setColor(c);g.setCornerRadius(dp(r));g.setStroke(dp(1),Color.rgb(17,67,91));return g;} private TextView tv(String s,int z){TextView v=new TextView(this);v.setText(s);v.setTextColor(text);v.setTextSize(z);v.setPadding(dp(14),dp(10),dp(14),dp(10));return v;} private Button btn(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextColor(text);b.setBackground(shape(panel,18));return b;}
    @Override public void onCreate(Bundle b){super.onCreate(b);build();refresh();}
    @Override protected void onResume(){super.onResume();refresh();}
    private void build(){ScrollView sc=new ScrollView(this);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(18),dp(24),dp(18),dp(28));root.setBackgroundColor(bg);root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);TextView title=tv("JARVIS // SECURITY CENTER",24);title.setTextColor(accent);root.addView(title);TextView note=tv("هدف: کمترین مجوز لازم، Secret داخل Android Keystore، و شفافیت اینکه چه چیزی Local است و چه چیزی ممکن است از شبکه استفاده کند.",12);note.setTextColor(muted);root.addView(note);report=tv("",14);report.setBackground(shape(panel,22));root.addView(report,new LinearLayout.LayoutParams(-1,-2));Button mic=btn("مدیریت مجوز میکروفون");mic.setOnClickListener(v->requestMic());root.addView(mic,new LinearLayout.LayoutParams(-1,dp(50)));Button contacts=btn("مدیریت مجوز مخاطبین");contacts.setOnClickListener(v->requestContacts());root.addView(contacts,new LinearLayout.LayoutParams(-1,dp(50)));Button appSettings=btn("باز کردن App Permissions در Android");appSettings.setOnClickListener(v->openAppSettings());root.addView(appSettings,new LinearLayout.LayoutParams(-1,dp(50)));Button assistant=btn("بررسی آمادگی System Assistant");assistant.setOnClickListener(v->{AssistantReadiness.Report r=new AssistantReadiness(this).inspect();new AlertDialog.Builder(this).setTitle("Assistant Readiness").setMessage(r.text).setPositiveButton("باشه",null).show();});root.addView(assistant,new LinearLayout.LayoutParams(-1,dp(50)));TextView warning=tv("JARVIS هیچ‌وقت نباید برای convenience، permission حساس اضافی بگیرد. عملیات حساس باید با تأیید کاربر یا handoff سیستم انجام شود.",11);warning.setTextColor(muted);root.addView(warning);sc.addView(root);setContentView(sc);}
    private void refresh(){boolean mic=checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED;boolean contacts=checkSelfPermission(Manifest.permission.READ_CONTACTS)==PackageManager.PERMISSION_GRANTED;boolean notif=Build.VERSION.SDK_INT<33||checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED;ProductionConfig p=new ProductionConfig(this);PcBridge bridge=new PcBridge(this);AssistantReadiness.Report ar=new AssistantReadiness(this).inspect();StringBuilder b=new StringBuilder();b.append("SECURITY POSTURE\n\n");b.append(mic?"✓":"✕").append(" Microphone\n");b.append(contacts?"✓":"•").append(" Contacts (اختیاری، برای تماس با نام)\n");b.append(notif?"✓":"•").append(" Notifications\n");b.append("✓ Backup disabled\n✓ Cleartext traffic disabled\n✓ Secrets encrypted with Android Keystore\n");b.append(p.productionMode()?"✓ Production backend mode":"• Development mode فعال است").append('\n');b.append(bridge.configured()?"✓ PC Bridge configured":"• PC Bridge not configured").append('\n');b.append(ar.roleHeld&&ar.activeVoiceService?"✓ System Assistant active":"• System Assistant هنوز کامل فعال نیست");report.setText(b.toString());report.setTextColor(mic?good:muted);}
    private void requestMic(){if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},3301);else openAppSettings();}
    private void requestContacts(){if(checkSelfPermission(Manifest.permission.READ_CONTACTS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.READ_CONTACTS},3302);else openAppSettings();}
    private void openAppSettings(){try{Intent i=new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);i.setData(android.net.Uri.parse("package:"+getPackageName()));startActivity(i);}catch(Exception e){startActivity(new Intent(Settings.ACTION_SETTINGS));}}
}
