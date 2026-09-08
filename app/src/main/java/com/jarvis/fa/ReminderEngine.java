package com.jarvis.fa;

import android.app.*;import android.content.*;import android.os.Build;import org.json.*;import java.util.*;
public class ReminderEngine{
 private final Context c;private final android.content.SharedPreferences p;
 public ReminderEngine(Context x){c=x.getApplicationContext();p=c.getSharedPreferences("jarvis_data",Context.MODE_PRIVATE);cleanup();}
 private boolean arm(int id,String title,long at){try{Intent i=new Intent(c,ReminderReceiver.class).putExtra("title",title).putExtra("id",id);PendingIntent pi=PendingIntent.getBroadcast(c,id,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);if(am==null)return false;if(Build.VERSION.SDK_INT>=31&&am.canScheduleExactAlarms())am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,pi);else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,pi);return true;}catch(Exception e){return false;}}
 public synchronized boolean schedule(String title,long at){if(at<=System.currentTimeMillis())return false;int id=(int)((System.currentTimeMillis()^at)&0x7fffffff);if(!arm(id,title,at))return false;JSONArray a=listRaw();a.put(obj(id,title,at));save(a);return true;}
 public synchronized void rescheduleAll(){cleanup();JSONArray a=listRaw();long now=System.currentTimeMillis();for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null&&o.optLong("at")>now)arm(o.optInt("id"),o.optString("title","یادآوری جارویس"),o.optLong("at"));}}
 public synchronized boolean cancel(int id){try{PendingIntent pi=PendingIntent.getBroadcast(c,id,new Intent(c,ReminderReceiver.class),PendingIntent.FLAG_NO_CREATE|PendingIntent.FLAG_IMMUTABLE);AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);if(pi!=null&&am!=null){am.cancel(pi);pi.cancel();}JSONArray old=listRaw(),n=new JSONArray();for(int i=0;i<old.length();i++){JSONObject o=old.optJSONObject(i);if(o!=null&&o.optInt("id")!=id)n.put(o);}save(n);return true;}catch(Exception e){return false;}}
 public synchronized void markFired(int id){JSONArray old=listRaw(),n=new JSONArray();for(int i=0;i<old.length();i++){JSONObject o=old.optJSONObject(i);if(o!=null&&o.optInt("id")!=id)n.put(o);}save(n);}
 public synchronized JSONArray list(){cleanup();return listRaw();}
 private JSONArray listRaw(){try{return new JSONArray(p.getString("reminders","[]"));}catch(Exception e){return new JSONArray();}}
 private JSONObject obj(int id,String title,long at){try{return new JSONObject().put("id",id).put("title",title).put("at",at);}catch(Exception e){return new JSONObject();}}
 private void save(JSONArray a){p.edit().putString("reminders",a.toString()).apply();}
 private synchronized void cleanup(){JSONArray old=listRaw(),n=new JSONArray();long cutoff=System.currentTimeMillis()-300000;for(int i=0;i<old.length();i++){JSONObject o=old.optJSONObject(i);if(o!=null&&o.optLong("at",0)>cutoff)n.put(o);}save(n);}
 public String summary(){JSONArray a=list();if(a.length()==0)return "یادآوری فعالی ثبت نشده است.";StringBuilder s=new StringBuilder();for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null)s.append("• ").append(o.optString("title")).append(" — ").append(new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm",Locale.getDefault()).format(new Date(o.optLong("at")))).append(" [").append(o.optInt("id")).append("]\n");}return s.toString().trim();}
}
