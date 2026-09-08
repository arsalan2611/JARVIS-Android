package com.jarvis.fa;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

public class ActionHistoryActivity extends Activity {
    private final int bg=Color.rgb(2,8,16),panel=Color.rgb(7,20,34),text=Color.WHITE,muted=Color.rgb(145,177,198),accent=Color.rgb(56,225,255);
    private TextView body;
    private int dp(int v){return(int)(v*getResources().getDisplayMetrics().density+.5f);}
    @Override public void onCreate(Bundle b){super.onCreate(b);build();}
    private TextView tv(String s,int size){TextView v=new TextView(this);v.setText(s);v.setTextColor(text);v.setTextSize(size);v.setPadding(dp(12),dp(10),dp(12),dp(10));return v;}
    private void build(){ScrollView sc=new ScrollView(this);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(16),dp(20),dp(16),dp(24));root.setBackgroundColor(bg);root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        TextView title=tv("ACTION HISTORY",24);title.setTextColor(accent);title.setGravity(Gravity.CENTER);root.addView(title);
        TextView sub=tv("ژورنال محلی فرمان‌ها و نتیجه گزارش‌شده توسط JARVIS",12);sub.setTextColor(muted);sub.setGravity(Gravity.CENTER);root.addView(sub);
        body=tv(new ActionReceiptStore(this).recent(30),14);body.setBackgroundColor(panel);root.addView(body,new LinearLayout.LayoutParams(-1,-2));
        Button refresh=new Button(this);refresh.setText("↻ تازه‌سازی");refresh.setAllCaps(false);refresh.setOnClickListener(v->body.setText(new ActionReceiptStore(this).recent(30)));root.addView(refresh,new LinearLayout.LayoutParams(-1,dp(50)));
        TextView note=tv("این بخش ثبت می‌کند JARVIS چه نتیجه‌ای گزارش کرده است؛ برای عملیات خارجی، نتیجه ثبت‌شده به معنی تأیید مستقل از سرویس مقصد نیست.",11);note.setTextColor(muted);root.addView(note);
        sc.addView(root);setContentView(sc);}
}
