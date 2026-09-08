package com.jarvis.fa;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ActionReceiptStore {
    private static final String PREF="jarvis_action_receipts";
    private static final String KEY="items";
    private static final int MAX=80;
    private final SharedPreferences p;

    public ActionReceiptStore(Context c){p=c.getSharedPreferences(PREF,Context.MODE_PRIVATE);}

    public synchronized void add(String command,String result){record(command,result,false);}
    public synchronized void record(String command,String result){record(command,result,false);}
    public synchronized void recordVerified(String command,String result){record(command,result,true);}

    private synchronized void record(String command,String result,boolean verified){
        try{
            JSONArray old=new JSONArray(p.getString(KEY,"[]"));
            JSONArray fresh=new JSONArray();
            JSONObject x=new JSONObject();
            x.put("ts",System.currentTimeMillis());
            x.put("command",safe(command,500));
            x.put("result",safe(result,1200));
            x.put("verified",verified);
            fresh.put(x);
            for(int i=0;i<old.length()&&fresh.length()<MAX;i++)fresh.put(old.get(i));
            p.edit().putString(KEY,fresh.toString()).apply();
        }catch(Exception ignored){}
    }

    public synchronized String latest(){
        try{
            JSONArray a=new JSONArray(p.getString(KEY,"[]"));
            if(a.length()==0)return "هنوز اقدامی ثبت نشده است.";
            return format(a.getJSONObject(0));
        }catch(Exception e){return "خواندن آخرین اقدام ممکن نشد.";}
    }

    public synchronized String recent(int n){
        try{
            JSONArray a=new JSONArray(p.getString(KEY,"[]"));
            if(a.length()==0)return "هنوز اقدامی ثبت نشده است.";
            StringBuilder b=new StringBuilder();
            for(int i=0;i<a.length()&&i<Math.max(1,n);i++){
                if(i>0)b.append("\n\n");
                b.append(format(a.getJSONObject(i)));
            }
            return b.toString();
        }catch(Exception e){return "خواندن سوابق اقدام ممکن نشد.";}
    }

    private String format(JSONObject x){
        long ts=x.optLong("ts",0);
        String when=ts>0?new SimpleDateFormat("MM/dd HH:mm",Locale.US).format(new Date(ts)):"";
        String v=x.optBoolean("verified",false)?"تأییدشده":"گزارش‌شده";
        return "• "+when+" • "+v+"\nفرمان: "+x.optString("command")+"\nنتیجه: "+x.optString("result");
    }
    private String safe(String s,int max){if(s==null)return "";s=s.trim();return s.length()<=max?s:s.substring(0,max);}
}
