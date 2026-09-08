package com.jarvis.fa;

import android.app.Activity;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.util.*;

public class JarvisHomeActivity extends Activity {
    private final int bg=Color.rgb(2,8,16),panel=Color.rgb(7,20,34),panel2=Color.rgb(9,27,45),accent=Color.rgb(56,225,255),text=Color.WHITE,muted=Color.rgb(145,177,198),good=Color.rgb(107,235,181);
    private TextView assistant,backend,pc,today,lastAction;
    private ReminderEngine reminders; private ProductionConfig prod; private PcBridge bridge; private HotwordCoordinator hotword; private ActionReceiptStore receipts;

    @Override public void onCreate(Bundle b){super.onCreate(b);reminders=new ReminderEngine(this);prod=new ProductionConfig(this);bridge=new PcBridge(this);hotword=new HotwordCoordinator(this);receipts=new ActionReceiptStore(this);build();}
    @Override protected void onResume(){super.onResume();hotword.reconcile();refresh();}

    private int dp(int v){return(int)(v*getResources().getDisplayMetrics().density+.5f);} 
    private GradientDrawable shape(int color,int radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius));g.setStroke(dp(1),Color.rgb(17,67,91));return g;}
    private TextView tv(String s,int size){TextView v=new TextView(this);v.setText(s);v.setTextColor(text);v.setTextSize(size);v.setPadding(dp(12),dp(9),dp(12),dp(9));return v;}
    private Button btn(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextColor(text);b.setBackground(shape(panel2,18));return b;}
    private LinearLayout card(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(14),dp(12),dp(14),dp(12));c.setBackground(shape(panel,22));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,dp(7),0,dp(7));c.setLayoutParams(lp);return c;}

    private void build(){
        ScrollView sc=new ScrollView(this);sc.setFillViewport(true);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(16),dp(18),dp(16),dp(28));root.setBackgroundColor(bg);root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        LinearLayout head=new LinearLayout(this);head.setGravity(Gravity.CENTER_VERTICAL);head.setOrientation(LinearLayout.HORIZONTAL);
        TextView title=tv("J A R V I S",28);title.setTextColor(accent);title.setLetterSpacing(.14f);head.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        TextView ver=tv("BUILD 28",11);ver.setTextColor(muted);head.addView(ver);root.addView(head);
        TextView sub=tv("PERSONAL AI COMMAND CENTER",11);sub.setTextColor(muted);root.addView(sub);

        LinearLayout core=card();core.setGravity(Gravity.CENTER);
        PulseView pulse=new PulseView();core.addView(pulse,new LinearLayout.LayoutParams(-1,dp(250)));
        TextView ready=tv("ONLINE • آماده فرمان",14);ready.setTextColor(accent);ready.setGravity(Gravity.CENTER);core.addView(ready);
        assistant=tv("",12);assistant.setGravity(Gravity.CENTER);assistant.setTextColor(muted);core.addView(assistant);
        Button speak=btn("◉  صحبت با JARVIS");speak.setTextColor(Color.rgb(0,20,28));speak.setBackground(shape(accent,28));speak.setOnClickListener(v->openConsole(true));core.addView(speak,new LinearLayout.LayoutParams(-1,dp(58)));
        root.addView(core);

        TextView quickTitle=tv("دسترسی سریع",13);quickTitle.setTextColor(muted);root.addView(quickTitle);
        LinearLayout q1=new LinearLayout(this);
        Button vision=btn("◈ Vision");vision.setOnClickListener(v->startActivity(new Intent(this,VisionActivity.class)));q1.addView(vision,new LinearLayout.LayoutParams(0,dp(54),1));
        Button files=btn("▣ Files");files.setOnClickListener(v->new WorkTools(this).openFiles("*/*"));q1.addView(files,new LinearLayout.LayoutParams(0,dp(54),1));
        Button chat=btn("⌁ Console");chat.setOnClickListener(v->openConsole(false));q1.addView(chat,new LinearLayout.LayoutParams(0,dp(54),1));root.addView(q1);

        LinearLayout q2=new LinearLayout(this);
        Button health=btn("✓ Health");health.setOnClickListener(v->startActivity(new Intent(this,DiagnosticsActivity.class)));q2.addView(health,new LinearLayout.LayoutParams(0,dp(50),1));
        Button assist=btn("◇ Assistant");assist.setOnClickListener(v->assistantSettings());q2.addView(assist,new LinearLayout.LayoutParams(0,dp(50),1));
        Button refresh=btn("↻ Refresh");refresh.setOnClickListener(v->refresh());q2.addView(refresh,new LinearLayout.LayoutParams(0,dp(50),1));root.addView(q2);

        LinearLayout todayCard=card();TextView tt=tv("TODAY",12);tt.setTextColor(accent);todayCard.addView(tt);today=tv("",14);todayCard.addView(today);root.addView(todayCard);

        LinearLayout statusCard=card();TextView st=tv("SYSTEM",12);st.setTextColor(accent);statusCard.addView(st);backend=tv("",13);pc=tv("",13);statusCard.addView(backend);statusCard.addView(pc);root.addView(statusCard);

        LinearLayout receiptCard=card();TextView rt=tv("LAST ACTION",12);rt.setTextColor(accent);receiptCard.addView(rt);lastAction=tv("",13);lastAction.setTextColor(muted);receiptCard.addView(lastAction);Button history=btn("مشاهده سوابق اقدامات");history.setOnClickListener(v->startActivity(new Intent(this,ActionHistoryActivity.class)));receiptCard.addView(history,new LinearLayout.LayoutParams(-1,dp(46)));root.addView(receiptCard);

        TextView foot=tv("JARVIS • PRIVATE BY DESIGN • LOCAL CONTROL",10);foot.setTextColor(Color.rgb(80,118,139));foot.setGravity(Gravity.CENTER);root.addView(foot);
        sc.addView(root);setContentView(sc);
    }

    private void refresh(){
        boolean active=hotword.isSystemAssistant();assistant.setText(active?"SYSTEM ASSISTANT • ACTIVE":"SYSTEM ASSISTANT • "+hotword.status());assistant.setTextColor(active?good:muted);
        String r=reminders.summary();today.setText(r==null||r.trim().isEmpty()?"یادآوری فعالی ثبت نشده است.":r);
        backend.setText("Backend  •  "+(prod.productionMode()?"PRODUCTION":"DEVELOPMENT")+"  •  "+(prod.backendUrl().isEmpty()?"Not configured":"Configured"));
        backend.setTextColor(prod.productionMode()&&!prod.backendUrl().isEmpty()?good:muted);
        pc.setText("PC Bridge  •  "+(bridge.configured()?"READY":"NOT CONFIGURED"));pc.setTextColor(bridge.configured()?good:muted);
        lastAction.setText(receipts.latest());
    }
    private void openConsole(boolean listen){Intent i=new Intent(this,MainActivity.class);if(listen)i.putExtra("auto_listen",true);startActivity(i);}
    private void assistantSettings(){try{startActivity(new Intent("android.settings.VOICE_INPUT_SETTINGS"));}catch(Exception e){startActivity(new Intent(Settings.ACTION_SETTINGS));}}

    private class PulseView extends View{
        private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG),p2=new Paint(Paint.ANTI_ALIAS_FLAG);private float phase=0f;private ValueAnimator anim;
        PulseView(){super(JarvisHomeActivity.this);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(2));p.setColor(accent);p2.setStyle(Paint.Style.FILL);p2.setColor(Color.rgb(7,31,49));anim=ValueAnimator.ofFloat(0f,1f);anim.setDuration(2200);anim.setRepeatCount(ValueAnimator.INFINITE);anim.addUpdateListener(a->{phase=(float)a.getAnimatedValue();invalidate();});anim.start();}
        @Override protected void onDraw(Canvas c){super.onDraw(c);float cx=getWidth()/2f,cy=getHeight()/2f;float base=Math.min(getWidth(),getHeight())*.22f;c.drawCircle(cx,cy,base,p2);for(int i=0;i<3;i++){float f=(phase+i/3f)%1f;int alpha=(int)(190*(1f-f));p.setAlpha(Math.max(20,alpha));c.drawCircle(cx,cy,base+f*dp(70),p);}p.setAlpha(255);p.setStrokeWidth(dp(3));c.drawCircle(cx,cy,base,p);p.setStrokeWidth(dp(1));c.drawCircle(cx,cy,base-dp(12),p);Paint dot=new Paint(Paint.ANTI_ALIAS_FLAG);dot.setColor(accent);c.drawCircle(cx,cy,dp(7),dot);}
        @Override protected void onDetachedFromWindow(){super.onDetachedFromWindow();if(anim!=null)anim.cancel();}
    }
}
