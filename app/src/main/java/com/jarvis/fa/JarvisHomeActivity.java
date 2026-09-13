package com.jarvis.fa;

import android.app.Activity;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

public class JarvisHomeActivity extends Activity {
    private final int bg=Color.rgb(3,8,15),surface=Color.rgb(8,17,29),surface2=Color.rgb(11,24,39),stroke=Color.rgb(28,63,82),accent=Color.rgb(69,226,255),text=Color.WHITE,muted=Color.rgb(139,165,183),good=Color.rgb(114,236,184),warn=Color.rgb(244,194,91);
    private TextView assistantPill,readinessPill,today,lastAction,systemLine;
    private ReminderEngine reminders; private ProductionConfig prod; private PcBridge bridge; private HotwordCoordinator hotword; private ActionReceiptStore receipts;

    @Override public void onCreate(Bundle b){super.onCreate(b);reminders=new ReminderEngine(this);prod=new ProductionConfig(this);bridge=new PcBridge(this);hotword=new HotwordCoordinator(this);receipts=new ActionReceiptStore(this);build();}
    @Override protected void onResume(){super.onResume();hotword.reconcile();refresh();}

    private int dp(int v){return(int)(v*getResources().getDisplayMetrics().density+.5f);}
    private String buildCode(){try{android.content.pm.PackageInfo p=getPackageManager().getPackageInfo(getPackageName(),0);return String.valueOf(android.os.Build.VERSION.SDK_INT>=28?p.getLongVersionCode():p.versionCode);}catch(Exception e){return "?";}}
    private GradientDrawable shape(int color,int radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius));g.setStroke(dp(1),stroke);return g;}
    private TextView tv(String s,int size){TextView v=new TextView(this);v.setText(s);v.setTextColor(text);v.setTextSize(size);v.setPadding(dp(12),dp(8),dp(12),dp(8));return v;}
    private TextView label(String s){TextView v=tv(s,10);v.setTextColor(muted);v.setLetterSpacing(.09f);return v;}
    private Button btn(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextColor(text);b.setTextSize(13);b.setBackground(shape(surface2,18));return b;}
    private LinearLayout card(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(16),dp(14),dp(16),dp(14));c.setBackground(shape(surface,22));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,dp(7),0,dp(7));c.setLayoutParams(lp);return c;}
    private Space gap(int h){Space s=new Space(this);s.setLayoutParams(new LinearLayout.LayoutParams(1,dp(h)));return s;}

    private void build(){
        ScrollView sc=new ScrollView(this);sc.setFillViewport(true);sc.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(18),dp(18),dp(18),dp(30));root.setBackgroundColor(bg);root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        LinearLayout head=new LinearLayout(this);head.setOrientation(LinearLayout.HORIZONTAL);head.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout brand=new LinearLayout(this);brand.setOrientation(LinearLayout.VERTICAL);
        TextView title=tv("J A R V I S",29);title.setTextColor(accent);title.setLetterSpacing(.16f);title.setPadding(0,0,0,0);brand.addView(title);
        TextView subtitle=tv("PERSONAL AI",10);subtitle.setTextColor(muted);subtitle.setLetterSpacing(.12f);subtitle.setPadding(0,0,0,0);brand.addView(subtitle);
        head.addView(brand,new LinearLayout.LayoutParams(0,-2,1));
        TextView build=tv(buildCode(),12);build.setGravity(Gravity.CENTER);build.setTextColor(accent);build.setBackground(shape(surface2,14));head.addView(build,new LinearLayout.LayoutParams(dp(42),dp(36)));root.addView(head);root.addView(gap(12));

        LinearLayout hero=card();hero.setGravity(Gravity.CENTER_HORIZONTAL);hero.setPadding(dp(14),dp(6),dp(14),dp(16));
        PulseView pulse=new PulseView();hero.addView(pulse,new LinearLayout.LayoutParams(-1,dp(220)));
        TextView ready=tv("آماده‌ام.",21);ready.setGravity(Gravity.CENTER);ready.setPadding(0,0,0,dp(3));hero.addView(ready);
        TextView hint=tv("فقط بگو چه می‌خواهی",12);hint.setGravity(Gravity.CENTER);hint.setTextColor(muted);hint.setPadding(0,0,0,dp(12));hero.addView(hint);
        LinearLayout pills=new LinearLayout(this);pills.setGravity(Gravity.CENTER);pills.setOrientation(LinearLayout.HORIZONTAL);
        assistantPill=tv("",11);assistantPill.setGravity(Gravity.CENTER);assistantPill.setBackground(shape(surface2,14));readinessPill=tv("",11);readinessPill.setGravity(Gravity.CENTER);readinessPill.setBackground(shape(surface2,14));LinearLayout.LayoutParams pl=new LinearLayout.LayoutParams(0,dp(38),1);pl.setMargins(dp(3),0,dp(3),0);pills.addView(assistantPill,pl);pills.addView(readinessPill,pl);hero.addView(pills);hero.addView(gap(10));
        Button talk=btn("صحبت با JARVIS");talk.setTextColor(Color.rgb(0,24,31));talk.setTextSize(16);talk.setBackground(shape(accent,24));talk.setOnClickListener(v->startActivity(new Intent(this,JarvisVoiceActivity.class)));hero.addView(talk,new LinearLayout.LayoutParams(-1,dp(58)));root.addView(hero);

        root.addView(label("WORKSPACE"));
        LinearLayout tools=new LinearLayout(this);tools.setOrientation(LinearLayout.HORIZONTAL);
        tools.addView(tool("VISION","دوربین و تصویر",()->startActivity(new Intent(this,VisionActivity.class))),toolLp());
        tools.addView(tool("FILES","Excel • PDF • CSV",()->startActivity(new Intent(this,FileIntelligenceActivity.class))),toolLp());root.addView(tools);
        LinearLayout tools2=new LinearLayout(this);tools2.setOrientation(LinearLayout.HORIZONTAL);
        tools2.addView(tool("ASK","گفتگو و فرمان",()->startActivity(new Intent(this,MainActivity.class))),toolLp());
        tools2.addView(tool("HISTORY","سوابق اقدامات",()->startActivity(new Intent(this,ActionHistoryActivity.class))),toolLp());root.addView(tools2);

        LinearLayout todayCard=card();todayCard.addView(label("TODAY"));today=tv("",14);today.setPadding(0,dp(4),0,dp(8));todayCard.addView(today);Button plan=btn("خلاصه و اولویت‌بندی امروز");plan.setOnClickListener(v->{String q="برنامه و یادآوری‌های امروز من را بررسی کن و مهم‌ترین اولویت‌ها را کوتاه بگو";getSharedPreferences("jarvis_data",MODE_PRIVATE).edit().putString("pendingWakeCommand",q).apply();startActivity(new Intent(this,MainActivity.class));});todayCard.addView(plan,new LinearLayout.LayoutParams(-1,dp(46)));root.addView(todayCard);

        LinearLayout system=card();system.addView(label("JARVIS SYSTEM"));systemLine=tv("",12);systemLine.setTextColor(muted);systemLine.setPadding(0,dp(4),0,dp(8));system.addView(systemLine);
        Button setup=btn("تنظیمات و آماده‌سازی JARVIS");setup.setOnClickListener(v->startActivity(new Intent(this,JarvisSetupActivity.class)));system.addView(setup,new LinearLayout.LayoutParams(-1,dp(48)));
        Button recheck=btn("بررسی دوباره Daily Driver");recheck.setOnClickListener(v->{Intent i=new Intent(this,DailyDriverActivity.class);i.putExtra("force_check",true);startActivity(i);});system.addView(recheck,new LinearLayout.LayoutParams(-1,dp(46)));
        root.addView(system);

        LinearLayout receipt=card();receipt.addView(label("LAST ACTION"));lastAction=tv("",12);lastAction.setTextColor(muted);lastAction.setPadding(0,dp(3),0,0);receipt.addView(lastAction);root.addView(receipt);
        TextView foot=tv("JARVIS  •  PRIVATE BY DESIGN  •  USER CONTROLLED",9);foot.setTextColor(Color.rgb(72,100,118));foot.setGravity(Gravity.CENTER);foot.setLetterSpacing(.05f);root.addView(foot);
        sc.addView(root);setContentView(sc);
    }

    private LinearLayout tool(String title,String sub,Runnable action){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(14),dp(14),dp(14),dp(14));c.setGravity(Gravity.CENTER_VERTICAL);c.setBackground(shape(surface,20));c.setOnClickListener(v->action.run());TextView t=tv(title,14);t.setTextColor(accent);t.setPadding(0,0,0,dp(2));TextView s=tv(sub,11);s.setTextColor(muted);s.setPadding(0,0,0,0);c.addView(t);c.addView(s);return c;}
    private LinearLayout.LayoutParams toolLp(){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(82),1);p.setMargins(dp(4),dp(4),dp(4),dp(4));return p;}

    private void refresh(){boolean active=hotword.isSystemAssistant();AssistantReadiness.Report ar=new AssistantReadiness(this).inspect();assistantPill.setText(active?"ASSISTANT • ACTIVE":"ASSISTANT • SETUP");assistantPill.setTextColor(active?good:warn);readinessPill.setText("READY • "+ar.score+"%");readinessPill.setTextColor(ar.score>=80?good:(ar.score>=55?warn:muted));String r=reminders.summary();today.setText(r==null||r.trim().isEmpty()?"امروز یادآوری فعالی نداری.":r);boolean backendOk=prod.productionMode()&&prod.backendReady();boolean pcOk=bridge.configured();systemLine.setText((backendOk?"AI امن آماده":"AI نیاز به تنظیم")+"   •   "+(pcOk?"PC تنظیم شده":"PC تنظیم نشده"));systemLine.setTextColor(backendOk?good:muted);String last=receipts.latest();lastAction.setText(last==null||last.trim().isEmpty()?"هنوز اقدامی ثبت نشده است.":last);}

    private class PulseView extends View{
        private final Paint ring=new Paint(Paint.ANTI_ALIAS_FLAG),fill=new Paint(Paint.ANTI_ALIAS_FLAG),dot=new Paint(Paint.ANTI_ALIAS_FLAG);private float phase;private ValueAnimator anim;
        PulseView(){super(JarvisHomeActivity.this);ring.setStyle(Paint.Style.STROKE);ring.setStrokeWidth(dp(1));ring.setColor(accent);fill.setStyle(Paint.Style.FILL);fill.setColor(Color.rgb(8,31,46));dot.setColor(accent);anim=ValueAnimator.ofFloat(0f,1f);anim.setDuration(2600);anim.setRepeatCount(ValueAnimator.INFINITE);anim.addUpdateListener(a->{phase=(float)a.getAnimatedValue();invalidate();});anim.start();}
        @Override protected void onDraw(Canvas c){super.onDraw(c);float cx=getWidth()/2f,cy=getHeight()/2f;float base=Math.min(getWidth(),getHeight())*.22f;c.drawCircle(cx,cy,base,fill);for(int i=0;i<2;i++){float f=(phase+i*.5f)%1f;ring.setAlpha((int)(145*(1f-f)));c.drawCircle(cx,cy,base+f*dp(60),ring);}ring.setAlpha(255);ring.setStrokeWidth(dp(2));c.drawCircle(cx,cy,base,ring);ring.setStrokeWidth(dp(1));c.drawCircle(cx,cy,base-dp(12),ring);c.drawCircle(cx,cy,dp(6),dot);}
        @Override protected void onDetachedFromWindow(){super.onDetachedFromWindow();if(anim!=null)anim.cancel();}
    }
}
