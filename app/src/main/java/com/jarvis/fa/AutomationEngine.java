package com.jarvis.fa;

import android.content.*;
import org.json.*;
import java.util.*;

public class AutomationEngine {
    public static final String TYPE_MORNING="morning_brief", TYPE_GMAIL="gmail_check", TYPE_PC="pc_check";
    private static final String PREF="jarvis_automations", KEY="items";
    private final Context c; private final SharedPreferences p; private final ReminderEngine reminders;
    public AutomationEngine(Context x){c=x.getApplicationContext();p=c.getSharedPreferences(PREF,Context.MODE_PRIVATE);reminders=new ReminderEngine(c);ensureDefaults();}
    private void ensureDefaults(){JSONArray a=raw();if(a.length()>0)return;JSONArray n=new JSONArray();n.put(item(TYPE_MORNING,"گزارش صبح",8,0,false));n.put(item(TYPE_GMAIL,"بررسی دوره‌ای Gmail",0,0,false));n.put(item(TYPE_PC,"بررسی دوره‌ای PC Bridge",0,0,false));save(n);}
    private JSONObject item(String type,String title,int hour,int minute,boolean enabled){try{return new JSONObject().put("type",type).put("title",title).put("hour",hour).put("minute",minute).put("enabled",enabled).put("updated_at",System.currentTimeMillis());}catch(Exception e){return new JSONObject();}}
    public synchronized JSONArray list(){return raw();}
    public synchronized boolean enabled(String type){JSONObject o=find(type);return o!=null&&o.optBoolean("enabled");}
    public synchronized void setEnabled(String type,boolean on){cancelKind(type);JSONArray a=raw();for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null&&type.equals(o.optString("type")))try{o.put("enabled",on).put("updated_at",System.currentTimeMillis());}catch(Exception ignored){}}save(a);if(on)schedule(type);}
    public synchronized void setMorningTime(int hour,int minute){hour=Math.max(0,Math.min(23,hour));minute=Math.max(0,Math.min(59,minute));JSONArray a=raw();for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null&&TYPE_MORNING.equals(o.optString("type")))try{o.put("hour",hour).put("minute",minute);}catch(Exception ignored){}}save(a);if(enabled(TYPE_MORNING)){cancelKind(TYPE_MORNING);schedule(TYPE_MORNING);}}
    public synchronized int morningHour(){JSONObject o=find(TYPE_MORNING);return o==null?8:o.optInt("hour",8);} public synchronized int morningMinute(){JSONObject o=find(TYPE_MORNING);return o==null?0:o.optInt("minute",0);}
    public synchronized void rescheduleAll(){reminders.rescheduleAll();}
    public String summary(){return "Morning Brief: "+(enabled(TYPE_MORNING)?String.format(Locale.US,"ON • %02d:%02d",morningHour(),morningMinute()):"OFF")+"\nGmail Check: "+(enabled(TYPE_GMAIL)?"ON • هر 3 ساعت":"OFF")+"\nPC Check: "+(enabled(TYPE_PC)?"ON • هر 3 ساعت":"OFF");}
    private void schedule(String type){long first;long interval;String title;if(TYPE_MORNING.equals(type)){Calendar cal=Calendar.getInstance();cal.set(Calendar.HOUR_OF_DAY,morningHour());cal.set(Calendar.MINUTE,morningMinute());cal.set(Calendar.SECOND,0);cal.set(Calendar.MILLISECOND,0);if(cal.getTimeInMillis()<=System.currentTimeMillis())cal.add(Calendar.DAY_OF_YEAR,1);first=cal.getTimeInMillis();interval=86400000L;title="JARVIS Automation • گزارش صبح و اولویت‌های امروز";}else if(TYPE_GMAIL.equals(type)){first=System.currentTimeMillis()+3*3600000L;interval=3*3600000L;title="JARVIS Automation • ایمیل‌های مهم را بررسی کن";}else{first=System.currentTimeMillis()+3*3600000L;interval=3*3600000L;title="JARVIS Automation • وضعیت PC Bridge را بررسی کن";}reminders.scheduleRecurring(title,first,interval,type);}
    private void cancelKind(String type){JSONArray a=reminders.list();for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null&&type.equals(o.optString("kind")))reminders.cancel(o.optInt("id"));}}
    private JSONObject find(String t){JSONArray a=raw();for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null&&t.equals(o.optString("type")))return o;}return null;}
    private JSONArray raw(){try{return new JSONArray(p.getString(KEY,"[]"));}catch(Exception e){return new JSONArray();}} private void save(JSONArray a){p.edit().putString(KEY,a.toString()).apply();}
}
