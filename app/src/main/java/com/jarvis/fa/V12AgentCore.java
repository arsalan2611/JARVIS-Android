package com.jarvis.fa;

import android.app.Activity;
import java.util.Locale;

public class V12AgentCore {
    public interface Callback { void onResult(String text); }
    public interface StateCallback { void onState(VoiceStateMachine.State state); }

    private final Activity activity;
    private final AgentCore delegate;
    private final StructuredMemory structured;
    private final WorkTools work;
    private final SelfTestEngine selfTest;
    private volatile StateCallback stateCallback;

    public V12AgentCore(Activity a){
        activity=a;
        delegate=new AgentCore(a);
        structured=new StructuredMemory(a);
        work=new WorkTools(a);
        selfTest=new SelfTestEngine(a);
        delegate.setStateCallback(s->{StateCallback cb=stateCallback;if(cb!=null)cb.onState(s);});
    }

    public void setStateCallback(StateCallback cb){stateCallback=cb;}

    public void run(String raw, Callback cb){
        String q=raw==null?"":raw.trim();
        if(q.isEmpty()){cb.onResult("فرمانی دریافت نشد.");return;}
        String n=normalize(q);

        if(n.contains("سلامت سیستم")||n.contains("وضعیت سلامت")||n.contains("self test")||n.contains("self-test")||n.equals("health")||n.contains("تست کامل سیستم")){
            state(VoiceStateMachine.State.EXECUTING);
            selfTest.run(r->{state(VoiceStateMachine.State.IDLE);cb.onResult(r);});
            return;
        }

        if(n.startsWith("یادت باشه")||n.startsWith("یادت باشد")||n.startsWith("به خاطر بسپار")){
            String v=q.replaceFirst("^(یادت باشه|یادت باشد|به خاطر بسپار)\\s*","").trim();
            if(v.isEmpty()){cb.onResult("چه چیزی را در حافظه نگه دارم؟");return;}
            state(VoiceStateMachine.State.EXECUTING);
            structured.put("general",keyOf(v),v);
            cb.onResult("در حافظه ساختاریافته ذخیره شد.");
            return;
        }

        if(n.contains("از حافظه")||n.startsWith("یادت هست")||n.startsWith("چی یادت")){
            state(VoiceStateMachine.State.EXECUTING);
            String query=q.replace("از حافظه","").replace("یادت هست","").replace("چی یادت","").trim();
            String r=structured.find(query);
            cb.onResult(r.isEmpty()?"چیزی مرتبط در حافظه ساختاریافته پیدا نکردم.":r);
            return;
        }

        if(n.contains("فایل")||n.contains("پی دی اف")||n.contains("pdf")||n.contains("اکسل")||n.contains("excel")){
            state(VoiceStateMachine.State.EXECUTING);
            String mime=n.contains("پی دی اف")||n.contains("pdf")?"application/pdf":(n.contains("اکسل")||n.contains("excel")?"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet":"*/*");
            activity.runOnUiThread(()->{
                boolean ok=work.openFiles(mime);
                cb.onResult(ok?"انتخاب‌گر فایل را باز کردم.":"باز کردن فایل‌ها ممکن نشد.");
            });
            return;
        }

        if(n.startsWith("ایمیل بنویس")||n.startsWith("ایمیل باز کن")||n.startsWith("ایمیل بساز")){
            state(VoiceStateMachine.State.EXECUTING);
            String body=q.replaceFirst("^(ایمیل بنویس|ایمیل باز کن|ایمیل بساز)\\s*","").trim();
            activity.runOnUiThread(()->cb.onResult(work.composeEmail("","",body)?"برنامه ایمیل را با متن پیشنهادی باز کردم.":"برنامه ایمیل باز نشد."));
            return;
        }

        delegate.run(q,cb::onResult);
    }

    public String memoryContext(){return structured.context();}
    public void shutdown(){selfTest.shutdown();delegate.shutdown();}

    private void state(VoiceStateMachine.State s){StateCallback cb=stateCallback;if(cb!=null)activity.runOnUiThread(()->cb.onState(s));}
    private String normalize(String s){return s.toLowerCase(Locale.ROOT).replace('ي','ی').replace('ك','ک').replace('‌',' ').replaceAll("\\s+"," ").trim();}
    private String keyOf(String v){String n=normalize(v);return n.length()<=48?n:n.substring(0,48);}
}
