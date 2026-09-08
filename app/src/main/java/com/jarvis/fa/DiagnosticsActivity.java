package com.jarvis.fa;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.os.*;
import android.view.*;
import android.widget.*;

public class DiagnosticsActivity extends Activity {
    private DiagnosticStore store; private SelfTestEngine selfTest; private TextView report,status;
    private int bg=Color.rgb(3,10,20),panel=Color.rgb(8,21,36),text=Color.WHITE,muted=Color.rgb(150,180,200),accent=Color.rgb(54,224,255);
    int dp(int v){return(int)(v*getResources().getDisplayMetrics().density+.5f);} TextView tv(String s,int z){TextView v=new TextView(this);v.setText(s);v.setTextColor(text);v.setTextSize(z);v.setPadding(dp(12),dp(9),dp(12),dp(9));return v;} Button btn(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);return b;}
    @Override public void onCreate(Bundle b){super.onCreate(b);store=new DiagnosticStore(this);selfTest=new SelfTestEngine(this);store.info("DIAGNOSTICS","Screen opened");build();refresh();}
    void build(){ScrollView sc=new ScrollView(this);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(16),dp(18),dp(16),dp(24));root.setBackgroundColor(bg);root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        TextView title=tv("JARVIS DIAGNOSTICS",27);title.setTextColor(accent);title.setGravity(Gravity.CENTER);root.addView(title);
        TextView sub=tv("گزارش محلی برای تست واقعی گوشی • بدون ارسال خودکار اطلاعات",12);sub.setTextColor(muted);sub.setGravity(Gravity.CENTER);root.addView(sub);
        status=tv("DIAGNOSTICS • آماده",12);status.setTextColor(accent);root.addView(status);
        LinearLayout row=new LinearLayout(this);Button test=btn("✓ تست کامل");test.setOnClickListener(v->runTest());row.addView(test,new LinearLayout.LayoutParams(0,dp(52),1));Button refresh=btn("↻ تازه‌سازی");refresh.setOnClickListener(v->refresh());row.addView(refresh,new LinearLayout.LayoutParams(0,dp(52),1));root.addView(row);
        report=tv("",12);report.setTextIsSelectable(true);report.setBackgroundColor(panel);root.addView(report);
        LinearLayout actions=new LinearLayout(this);Button copy=btn("کپی گزارش");copy.setOnClickListener(v->copy());actions.addView(copy,new LinearLayout.LayoutParams(0,dp(50),1));Button share=btn("اشتراک گزارش");share.setOnClickListener(v->share());actions.addView(share,new LinearLayout.LayoutParams(0,dp(50),1));root.addView(actions);
        Button clear=btn("پاک کردن لاگ محلی");clear.setOnClickListener(v->{store.clear();store.info("DIAGNOSTICS","Log cleared by user");refresh();});root.addView(clear,new LinearLayout.LayoutParams(-1,dp(48)));
        sc.addView(root);setContentView(sc);
    }
    void runTest(){status.setText("DIAGNOSTICS • در حال تست...");store.info("SELFTEST","Started from diagnostics");selfTest.run(r->{store.info("SELFTEST",r);report.setText(r+"\n\n"+store.report());status.setText("DIAGNOSTICS • تست کامل شد");});}
    void refresh(){report.setText(store.report());}
    void copy(){ClipboardManager c=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);c.setPrimaryClip(ClipData.newPlainText("JARVIS Diagnostic Report",report.getText()));status.setText("DIAGNOSTICS • گزارش کپی شد");}
    void share(){try{Intent i=new Intent(Intent.ACTION_SEND);i.setType("text/plain");i.putExtra(Intent.EXTRA_SUBJECT,"JARVIS Diagnostic Report");i.putExtra(Intent.EXTRA_TEXT,report.getText().toString());startActivity(Intent.createChooser(i,"اشتراک گزارش JARVIS"));}catch(Exception e){store.error("SHARE",e);status.setText("اشتراک گزارش ممکن نشد");}}
    @Override protected void onDestroy(){super.onDestroy();selfTest.shutdown();}
}
