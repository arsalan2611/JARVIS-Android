package com.jarvis.fa;

import android.app.*;
import android.content.*;
import android.os.Build;
import org.json.*;
import java.util.*;

public class AutomationEngine {
    public static final String TYPE_MORNING="morning_brief", TYPE_GMAIL="gmail_watch", TYPE_PC="pc_watch";
    private static final String PREF="jarvis_automations", KEY="items";
    private final Context c; private final SharedPreferences p;
    public AutomationEngine(Context x){c=x.getApplicationContext();p=c.getSharedPreferences(PREF,Context.MODE_PRIVATE);ensureDefaults();}

    private void ensureDefaults(){JSONArray a=raw();if(a.length()>0)return;JSONArray n=new JSONArray();n.put(item(TYPE_MORNING,"گزارش صبح",8,0,false));n.put(item(TYPE_GMAIL,"Gmail Watch",0,0,false));n.put(item(TYPE_PC,"PC Online Watch",0,0,false));save(n);}
    private JSONObject item(String type,String title,int hour,int minute,boolean enabled){try{return new JSONObject().put("type",type).put("title",title).put("hour",hour).put("minute",minute).put("enabled",enabled).put("last_hash","").put("updated_at",System.currentTimeMillis());}catch(Exception e){return new JSONObject();}}
    public synchronized JSONArray list(){return raw();}
    public synchronized boolean enabled(String type){JSONObject o=find(type);return o!=null&&o.optBoolean("enabled");}
    public synchronized void setEnabled(String type,boolean on){JSONArray a=raw();for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null&&type.equals(o.optString("type"))){try{o.put("enabled",on).put("updated_at",System.currentTimeMillis());}catch(Exception ignored){}}}save(a);if(on)arm(type);else cancel(type);}
    public synchronized void setMorningTime(int hour,int minute){hour=Math.max(0,Math.min(23,hour));minute=Math.max(0,Math.min(59,minute));JSONArray a=raw();for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null&&TYPE_MORNING.equals(o.optString("type"))){try{o.put("hour",hour).put("minute",minute);}catch(Exception ignored){}}}save(a);if(enabled(TYPE_MORNING)){cancel(TYPE_MORNING);arm(TYPE_MORNING);}}
    public synchronized int morningHour(){JSONObject o=find(TYPE_MORNING);return o==null?8:o.optInt("hour",8);} public synchronized int morningMinute(){JSONObject o=find(TYPE_MORNING);return o==null?0:o.optInt("minute",0);}
    public synchronized String lastHash(String type){JSONObject o=find(type);return o==null?"":o.optString("last_hash","");}
    public synchronized void setLastHash(String type,String hash){JSONArray a=raw();for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null&&type.equals(o.optString("type")))try{o.put("last_hash",hash==null?"":hash);}catch(Exception ignored){}}save(a);}
    public synchronized void rescheduleAll(){for(String t:new String[]{TYPE_MORNING,TYPE_GMAIL,TYPE_PC})if(enabled(t))arm(t);}
    public String summary(){return "Morning Brief: "+(enabled(TYPE_MORNING)?String.format(Locale.US,"ON • %02d:%02d",morningHour(),morningMinute()):"OFF")+"\nGmail Watch: "+(enabled(TYPE_GMAIL)?"ON • hourly":"OFF")+"\nPC Online Watch: "+(enabled(TYPE_PC)?"ON • hourly":"OFF");}

    private void arm(String type){try{AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);if(am==null)return;int id=id(type);Intent i=new Intent(c,AutomationReceiver.class).putExtra("type",type);PendingIntent pi=PendingIntent.getBroadcast(c,id,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);long first;long interval;
        if(TYPE_MORNING.equals(type)){Calendar cal=Calendar.getInstance();cal.set(Calendar.HOUR_OF_DAY,morningHour());cal.set(Calendar.MINUTE,morningMinute());cal.set(Calendar.SECOND,0);cal.set(Calendar.MILLISECOND,0);if(cal.getTimeInMillis()<=System.currentTimeMillis())cal.add(Calendar.DAY_OF_YEAR,1);first=cal.getTimeInMillis();interval=AlarmManager.INTERVAL_DAY;}
        else{first=System.currentTimeMillis()+5*60*1000L;interval=AlarmManager.INTERVAL_HOUR;}
        am.setInexactRepeating(AlarmManager.RTC_WAKEUP,first,interval,pi);
    }catch(Exception ignored){}}
    private void cancel(String type){try{AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);PendingIntent pi=PendingIntent.getBroadcast(c,id(type),new Intent(c,AutomationReceiver.class).putExtra("type",type),PendingIntent.FLAG_NO_CREATE|PendingIntent.FLAG_IMMUTABLE);if(am!=null&&pi!=null){am.cancel(pi);pi.cancel();}}catch(Exception ignored){}}
    private int id(String t){return 41000+Math.abs(t.hashCode()%8000);} private JSONObject find(String t){JSONArray a=raw();for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null&&t.equals(o.optString("type")))return o;}return null;}
    private JSONArray raw(){try{return new JSONArray(p.getString(KEY,"[]"));}catch(Exception e){return new JSONArray();}} private void save(JSONArray a){p.edit().putString(KEY,a.toString()).apply();}
}
