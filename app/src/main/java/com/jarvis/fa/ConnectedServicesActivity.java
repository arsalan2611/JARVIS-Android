package com.jarvis.fa;

import android.app.Activity;
import android.content.*;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.util.concurrent.*;

public class ConnectedServicesActivity extends Activity {
    private final int bg=Color.rgb(3,8,15),surface=Color.rgb(8,17,29),surface2=Color.rgb(11,24,39),stroke=Color.rgb(28,63,82),accent=Color.rgb(69,226,255),text=Color.WHITE,muted=Color.rgb(139,165,183),good=Color.rgb(114,236,184),warn=Color.rgb(244,194,91);
    private TextView backendState,googleState,gmailState,calendarState,detail;
    private Button connect,refresh;
    private ConnectedServices services;
    private ExecutorService ex=Executors.newSingleThreadExecutor();

    @Override public void onCreate(Bundle b){super.onCreate(b);services=new ConnectedServices(this);build();refreshStatus();}
    @Override protected void onResume(){super.onResume();refreshStatus();}
    @Override protected void onDestroy(){super.onDestroy();ex.shutdownNow();}

    private int dp(int v){return(int)(v*getResources().getDisplayMetrics().density+.5f);} 
    private GradientDrawable shape(int color,int radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius));g.setStroke(dp(1),stroke);return g;}
    private TextView tv(String s,int size){TextView v=new TextView(this);v.setText(s);v.setTextColor(text);v.setTextSize(size);v.setPadding(dp(12),dp(8),dp(12),dp(8));return v;}
    private Button btn(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextColor(text);b.setBackground(shape(surface2,18));return b;}
    private LinearLayout card(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(16),dp(14),dp(16),dp(14));c.setBackground(shape(surface,22));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,dp(7),0,dp(7));c.setLayoutParams(lp);return c;}

    private void build(){ScrollView sc=new ScrollView(this);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(18),dp(20),dp(18),dp(28));root.setBackgroundColor(bg);root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        TextView title=tv("CONNECTED JARVIS",26);title.setTextColor(accent);title.setLetterSpacing(.11f);title.setPadding(0,0,0,0);root.addView(title);
        TextView sub=tv("اتصال واقعی Backend، Gmail و Google Calendar",12);sub.setTextColor(muted);sub.setPadding(0,0,0,dp(10));root.addView(sub);
        LinearLayout c=card();backendState=tv("Backend",14);googleState=tv("Google OAuth",14);gmailState=tv("Gmail",14);calendarState=tv("Calendar",14);detail=tv("",11);detail.setTextColor(muted);c.addView(backendState);c.addView(googleState);c.addView(gmailState);c.addView(calendarState);c.addView(detail);root.addView(c);
        connect=btn("اتصال Google به JARVIS");connect.setOnClickListener(v->connectGoogle());root.addView(connect,new LinearLayout.LayoutParams(-1,dp(52)));
        refresh=btn("بررسی دوباره وضعیت اتصال");refresh.setOnClickListener(v->refreshStatus());root.addView(refresh,new LinearLayout.LayoutParams(-1,dp(48)));
        TextView note=tv("Gmail فقط خواندن، جستجو و ساخت Draft دارد؛ JARVIS از این صفحه ایمیل را خودکار Send نمی‌کند. Google Client Secret فقط روی Backend نگهداری می‌شود و نباید داخل APK قرار بگیرد.",11);note.setTextColor(muted);note.setBackground(shape(surface,18));root.addView(note);sc.addView(root);setContentView(sc);
    }
    private void set(TextView v,String label,boolean ok,boolean available){v.setText((ok?"✓ ":available?"• ":"✕ ")+label);v.setTextColor(ok?good:(available?warn:muted));}
    private void refreshStatus(){refresh.setEnabled(false);detail.setText("در حال بررسی اتصال زنده...");ex.submit(()->{ConnectedServices.Status s=services.status();runOnUiThread(()->{set(backendState,"Backend امن",s.backendReady,s.backendReady);set(googleState,"Google OAuth",s.googleConnected,s.googleConfigured);set(gmailState,"Gmail",s.gmail,s.googleConfigured);set(calendarState,"Google Calendar",s.calendar,s.googleConfigured);detail.setText(s.detail);connect.setEnabled(s.backendReady&&s.googleConfigured&&!s.googleConnected);connect.setText(s.googleConnected?"Google متصل است":"اتصال Google به JARVIS");refresh.setEnabled(true);});});}
    private void connectGoogle(){connect.setEnabled(false);detail.setText("در حال ایجاد مسیر امن OAuth...");ex.submit(()->{String u=services.startGoogleOAuth();runOnUiThread(()->{if(u.isEmpty()){detail.setText("OAuth شروع نشد. Backend یا تنظیمات Google را بررسی کن.");connect.setEnabled(true);return;}try{startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(u)));detail.setText("ورود Google در مرورگر باز شد. بعد از تأیید، به JARVIS برگرد و وضعیت را Refresh کن.");}catch(Exception e){detail.setText("مرورگر برای OAuth باز نشد.");connect.setEnabled(true);}});});}
}
