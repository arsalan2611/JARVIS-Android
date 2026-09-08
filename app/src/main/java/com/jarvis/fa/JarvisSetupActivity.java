package com.jarvis.fa;

import android.Manifest;
import android.app.*;
import android.app.role.RoleManager;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;

public class JarvisSetupActivity extends Activity {
    private static final int REQ_ROLE=9201;
    private final int bg=Color.rgb(3,8,15),surface=Color.rgb(8,17,29),surface2=Color.rgb(11,24,39),stroke=Color.rgb(28,63,82),accent=Color.rgb(69,226,255),text=Color.WHITE,muted=Color.rgb(139,165,183),good=Color.rgb(114,236,184),warn=Color.rgb(244,194,91);
    private TextView scoreText,assistantText,voiceText,backendText,pcText,privacyText;
    private EditText backendUrl,backendToken,pcUrl,pcToken;
    private Switch productionSwitch,privateVoiceSwitch;
    private ProductionConfig prod; private PcBridge bridge; private HotwordCoordinator hotword;

    @Override public void onCreate(Bundle b){super.onCreate(b);prod=new ProductionConfig(this);bridge=new PcBridge(this);hotword=new HotwordCoordinator(this);build();refresh();}
    @Override protected void onResume(){super.onResume();refresh();}

    private int dp(int v){return(int)(v*getResources().getDisplayMetrics().density+.5f);} 
    private GradientDrawable shape(int color,int radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius));g.setStroke(dp(1),stroke);return g;}
    private TextView tv(String s,int size){TextView v=new TextView(this);v.setText(s);v.setTextColor(text);v.setTextSize(size);v.setPadding(dp(12),dp(8),dp(12),dp(8));return v;}
    private TextView label(String s){TextView v=tv(s,11);v.setTextColor(muted);v.setLetterSpacing(.08f);return v;}
    private Button btn(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextColor(text);b.setTextSize(13);b.setBackground(shape(surface2,18));return b;}
    private LinearLayout card(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(16),dp(14),dp(16),dp(14));c.setBackground(shape(surface,22));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,dp(7),0,dp(7));c.setLayoutParams(lp);return c;}
    private EditText field(String hint){EditText e=new EditText(this);e.setHint(hint);e.setHintTextColor(muted);e.setTextColor(text);e.setTextSize(13);e.setSingleLine(true);e.setPadding(dp(12),0,dp(12),0);e.setBackground(shape(surface2,14));return e;}

    private void build(){
        ScrollView sc=new ScrollView(this);sc.setFillViewport(true);sc.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(18),dp(20),dp(18),dp(30));root.setBackgroundColor(bg);root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        TextView title=tv("JARVIS SETUP",26);title.setTextColor(accent);title.setLetterSpacing(.12f);title.setPadding(0,0,0,0);root.addView(title);
        TextView sub=tv("همه تنظیمات ضروری، فقط در یک جا",12);sub.setTextColor(muted);sub.setPadding(0,0,0,dp(10));root.addView(sub);

        LinearLayout readiness=card();readiness.addView(label("READINESS"));scoreText=tv("",24);scoreText.setTextColor(accent);scoreText.setPadding(0,dp(2),0,dp(4));readiness.addView(scoreText);assistantText=tv("",12);voiceText=tv("",12);privacyText=tv("",12);readiness.addView(assistantText);readiness.addView(voiceText);readiness.addView(privacyText);root.addView(readiness);

        LinearLayout assistant=card();assistant.addView(label("SYSTEM ASSISTANT"));TextView an=tv("JARVIS را دستیار اصلی گوشی کن تا از مسیر رسمی Android فراخوانی شود.",12);an.setTextColor(muted);an.setPadding(0,dp(2),0,dp(8));assistant.addView(an);Button role=btn("انتخاب JARVIS به‌عنوان Assistant");role.setOnClickListener(v->requestAssistantRole());assistant.addView(role,new LinearLayout.LayoutParams(-1,dp(48)));root.addView(assistant);

        LinearLayout voice=card();voice.addView(label("PERSIAN VOICE"));privateVoiceSwitch=new Switch(this);privateVoiceSwitch.setText("حالت Private / Local");privateVoiceSwitch.setTextColor(text);privateVoiceSwitch.setTextSize(13);privateVoiceSwitch.setPadding(0,dp(4),0,dp(4));voice.addView(privateVoiceSwitch);TextView vn=tv("خاموش: دقت فارسی بالاتر با Speech Service گوشی. روشن: فقط در صورت وجود موتور on-device از پردازش محلی استفاده می‌شود.",11);vn.setTextColor(muted);vn.setPadding(0,0,0,dp(8));voice.addView(vn);Button mic=btn("بررسی/دادن مجوز میکروفون");mic.setOnClickListener(v->requestMic());voice.addView(mic,new LinearLayout.LayoutParams(-1,dp(46)));root.addView(voice);

        LinearLayout backend=card();backend.addView(label("SECURE AI BACKEND"));backendText=tv("",12);backendText.setPadding(0,dp(2),0,dp(6));backend.addView(backendText);productionSwitch=new Switch(this);productionSwitch.setText("Production Mode");productionSwitch.setTextColor(text);productionSwitch.setTextSize(13);backend.addView(productionSwitch);backendUrl=field("Backend URL  مثل https://... ");backend.addView(backendUrl,new LinearLayout.LayoutParams(-1,dp(48)));backendToken=field("Backend token — خالی بگذار تا قبلی حفظ شود");backend.addView(backendToken,new LinearLayout.LayoutParams(-1,dp(48)));Button saveBackend=btn("ذخیره Backend امن");saveBackend.setOnClickListener(v->saveBackend());backend.addView(saveBackend,new LinearLayout.LayoutParams(-1,dp(46)));root.addView(backend);

        LinearLayout connected=card();connected.addView(label("CONNECTED SERVICES"));TextView cn=tv("Gmail و Google Calendar فقط از طریق Backend امن و OAuth واقعی متصل می‌شوند.",11);cn.setTextColor(muted);cn.setPadding(0,0,0,dp(8));connected.addView(cn);Button connectedBtn=btn("مدیریت Gmail و Google Calendar");connectedBtn.setOnClickListener(v->startActivity(new Intent(this,ConnectedServicesActivity.class)));connected.addView(connectedBtn,new LinearLayout.LayoutParams(-1,dp(48)));root.addView(connected);

        LinearLayout pc=card();pc.addView(label("WINDOWS PC BRIDGE"));pcText=tv("",12);pcText.setPadding(0,dp(2),0,dp(6));pc.addView(pcText);pcUrl=field("PC Bridge URL  مثل http://192.168.1.10:8765");pc.addView(pcUrl,new LinearLayout.LayoutParams(-1,dp(48)));pcToken=field("PC token — خالی بگذار تا قبلی حفظ شود");pc.addView(pcToken,new LinearLayout.LayoutParams(-1,dp(48)));Button savePc=btn("ذخیره اتصال PC");savePc.setOnClickListener(v->savePc());pc.addView(savePc,new LinearLayout.LayoutParams(-1,dp(46)));root.addView(pc);

        LinearLayout security=card();security.addView(label("SECURITY & DIAGNOSTICS"));LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.HORIZONTAL);Button sec=btn("Security");sec.setOnClickListener(v->startActivity(new Intent(this,SecurityCenterActivity.class)));r.addView(sec,new LinearLayout.LayoutParams(0,dp(46),1));Button health=btn("Health");health.setOnClickListener(v->startActivity(new Intent(this,DiagnosticsActivity.class)));r.addView(health,new LinearLayout.LayoutParams(0,dp(46),1));security.addView(r);Button app=btn("تنظیمات مجوزهای Android");app.setOnClickListener(v->openAppSettings());security.addView(app,new LinearLayout.LayoutParams(-1,dp(46)));root.addView(security);

        TextView note=tv("Secretها در Android Keystore نگهداری می‌شوند. Google Client Secret فقط روی Backend است. برای استفاده روزمره، Production Mode با Backend امن توصیه می‌شود؛ API Key را داخل APK یا چت وارد نکن.",11);note.setTextColor(muted);note.setBackground(shape(surface,18));root.addView(note);
        sc.addView(root);setContentView(sc);
    }

    private void refresh(){
        hotword.reconcile();AssistantReadiness.Report ar=new AssistantReadiness(this).inspect();scoreText.setText("آمادگی سیستم  "+ar.score+"%");scoreText.setTextColor(ar.score>=80?good:(ar.score>=55?warn:muted));
        assistantText.setText(ar.roleHeld&&ar.activeVoiceService?"✓ System Assistant فعال":"• System Assistant نیاز به تنظیم دارد");assistantText.setTextColor(ar.roleHeld&&ar.activeVoiceService?good:warn);
        boolean mic=checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED;voiceText.setText(mic?"✓ Microphone آماده":"• مجوز Microphone داده نشده");voiceText.setTextColor(mic?good:warn);
        boolean local=Build.VERSION.SDK_INT>=31&&android.speech.SpeechRecognizer.isOnDeviceRecognitionAvailable(this);privacyText.setText(local?"✓ On-device Speech روی این گوشی موجود است":"• On-device Speech روی این گوشی موجود نیست");privacyText.setTextColor(local?good:muted);
        backendText.setText(prod.status());backendText.setTextColor(prod.productionMode()&&prod.backendReady()?good:muted);backendUrl.setText(prod.backendUrl());productionSwitch.setChecked(prod.productionMode());
        pcText.setText(bridge.configured()?"✓ PC Bridge آماده":"• PC Bridge هنوز تنظیم نشده");pcText.setTextColor(bridge.configured()?good:muted);pcUrl.setText(bridge.endpoint());
        privateVoiceSwitch.setChecked(getSharedPreferences("jarvis_voice",MODE_PRIVATE).getBoolean("private_mode",false));
    }

    private void saveBackend(){prod.setBackend(backendUrl.getText().toString(),backendToken.getText().toString());prod.setProductionMode(productionSwitch.isChecked());backendToken.setText("");Toast.makeText(this,"تنظیمات Backend ذخیره شد.",Toast.LENGTH_SHORT).show();refresh();}
    private void savePc(){bridge.configure(pcUrl.getText().toString(),pcToken.getText().toString());pcToken.setText("");Toast.makeText(this,"تنظیمات PC Bridge ذخیره شد.",Toast.LENGTH_SHORT).show();refresh();}
    private void requestMic(){if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},9202);else Toast.makeText(this,"مجوز میکروفون فعال است.",Toast.LENGTH_SHORT).show();}
    private void requestAssistantRole(){if(Build.VERSION.SDK_INT>=29){try{RoleManager rm=(RoleManager)getSystemService(ROLE_SERVICE);if(rm!=null&&rm.isRoleAvailable(RoleManager.ROLE_ASSISTANT)){if(rm.isRoleHeld(RoleManager.ROLE_ASSISTANT)){Toast.makeText(this,"JARVIS همین حالا Assistant است.",Toast.LENGTH_SHORT).show();refresh();return;}startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_ASSISTANT),REQ_ROLE);return;}}catch(Exception ignored){}}try{startActivity(new Intent("android.settings.VOICE_INPUT_SETTINGS"));}catch(Exception e){startActivity(new Intent(Settings.ACTION_SETTINGS));}}
    private void openAppSettings(){try{Intent i=new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);i.setData(android.net.Uri.parse("package:"+getPackageName()));startActivity(i);}catch(Exception e){startActivity(new Intent(Settings.ACTION_SETTINGS));}}
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){super.onActivityResult(requestCode,resultCode,data);if(requestCode==REQ_ROLE){hotword.reconcile();refresh();}}
    @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults){super.onRequestPermissionsResult(requestCode,permissions,grantResults);refresh();}
    @Override protected void onPause(){super.onPause();getSharedPreferences("jarvis_voice",MODE_PRIVATE).edit().putBoolean("private_mode",privateVoiceSwitch.isChecked()).apply();}
}
