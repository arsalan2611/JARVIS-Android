package com.jarvis.fa;

import android.app.*;import android.content.*;import android.os.Build;import org.json.*;import java.util.*;
public class ReminderEngine{
 private final Context c; private final android.content.SharedPreferences p;
 public ReminderEngine(Context x){c=x.getApplicationContext();p=c.getSharedPreferences("jarvis_data",Context.MODE_PRIVATE);}
 public boolean schedule(String title,long at){try{int id=(int)(System.currentTimeMillis()&0x7fffffff);Intent i=new Intent(c,ReminderReceiver.class).putExtra("title",title).putExtra("id",id);PendingIntent pi=PendingIntent.getBroadcast(c,id,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);if(Build.VERSION.SDK_INT>=31&&am.canScheduleExactAlarms())am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,pi);else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,pi);JSONArray a=list();a.put(new JSONObject().put("id",id).put("title",title).put("at",at));p.edit().putString("reminders",a.toString()).apply();return true;}catch(Exception e){return false;}}
 public JSONArray list(){try{return new JSONArray(p.getString("reminders","[]"));}catch(Exception e){return new JSONArray();}}
 public String summary(){JSONArray a=list();if(a.length()==0)return "یادآوری فعالی ثبت نشده است.";StringBuilder s=new StringBuilder();for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null)s.append("• ").append(o.optString("title")).append(" — ").append(new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm",Locale.getDefault()).format(new Date(o.optLong("at")))).append('\n');}return s.toString().trim();}
}
