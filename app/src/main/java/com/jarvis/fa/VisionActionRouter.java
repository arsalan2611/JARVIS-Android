package com.jarvis.fa;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import org.json.JSONObject;

public class VisionActionRouter {
    private final Activity a;
    private final JarvisActions actions;
    private final ReminderEngine reminders;
    private final PersianTimeParser timeParser;
    private final PersonalActions personal;
    private final StructuredMemory memory;
    private final WorkTools work;

    public VisionActionRouter(Activity activity){a=activity;actions=new JarvisActions(a);reminders=new ReminderEngine(a);timeParser=new PersianTimeParser();personal=new PersonalActions(a);memory=new StructuredMemory(a);work=new WorkTools(a);}

    public String label(String action){
        if(action==null)return "";
        switch(action){
            case "web_search":return "جستجو در وب";
            case "maps":return "باز کردن روی نقشه";
            case "reminder":return "ساخت یادآوری";
            case "calendar":return "ساخت رویداد تقویم";
            case "email":return "ساخت ایمیل";
            case "save_memory":return "ذخیره در حافظه";
            default:return "";
        }
    }

    public String execute(VisionAgentResult r){
        try{
            String action=r==null?"none":r.suggestedAction;
            String payload=r==null?"":r.actionPayload;
            JSONObject p;try{p=new JSONObject(payload);}catch(Exception e){p=new JSONObject().put("text",payload);}
            switch(action){
                case "web_search":{String q=p.optString("query",payload);return actions.webSearch(q)?"جستجو باز شد.":"باز کردن جستجو ممکن نشد.";}
                case "maps":{String q=p.optString("query",payload);return actions.openMaps(q)?"نقشه باز شد.":"باز کردن نقشه ممکن نشد.";}
                case "reminder":{
                    String title=p.optString("title",r.summary.isEmpty()?"یادآوری Vision":r.summary);
                    String time=p.optString("time_text","");PersianTimeParser.Result pr=timeParser.parse(time);
                    if(!pr.ok)return "زمان یادآوری از تصویر با اطمینان کافی قابل تشخیص نیست.";
                    return reminders.schedule(title,pr.epochMs)?"یادآوری ثبت شد.":"ثبت یادآوری ممکن نشد.";
                }
                case "calendar":{
                    String title=p.optString("title",r.summary.isEmpty()?"رویداد Vision":r.summary);
                    String time=p.optString("time_text","");PersianTimeParser.Result pr=timeParser.parse(time);
                    if(!pr.ok)return "زمان رویداد قابل تشخیص نیست.";
                    long end=pr.epochMs+Math.max(1,p.optInt("duration_minutes",60))*60000L;
                    return personal.insertCalendarEvent(title,pr.epochMs,end,p.optString("location",""),p.optString("description",r.extractedText))?"فرم رویداد باز شد.":"تقویم باز نشد.";
                }
                case "email":{
                    return work.composeEmail(p.optString("to",""),p.optString("subject",r.summary),p.optString("body",r.extractedText))?"برنامه ایمیل باز شد.":"برنامه ایمیل باز نشد.";
                }
                case "save_memory":{
                    String key=p.optString("key",r.entityType+"-vision");String value=p.optString("value",r.summary+" | "+r.extractedText);memory.put("vision-agent",key,value);return "در حافظه JARVIS ذخیره شد.";
                }
                default:return "برای این تصویر اقدام اجرایی مطمئنی پیشنهاد نشده است.";
            }
        }catch(Exception e){return "اجرای اقدام Vision خطا داد: "+e.getClass().getSimpleName();}
    }
}
