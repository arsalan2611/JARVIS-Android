package com.jarvis.fa;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ActionReceiptStore {
    public enum Status { VERIFIED, HANDOFF, FAILED, REPORTED }
    private static final String PREF="jarvis_action_receipts";
    private static final String KEY="items";
    private static final int MAX=100;
    private final SharedPreferences p;

    public ActionReceiptStore(Context c){p=c.getSharedPreferences(PREF,Context.MODE_PRIVATE);}

    public synchronized void add(String command,String result){record(command,result,Status.REPORTED);}
    public synchronized void record(String command,String result){record(command,result,Status.REPORTED);}
    public synchronized void recordVerified(String command,String result){record(command,result,Status.VERIFIED);}
    public synchronized void recordHandoff(String command,String result){record(command,result,Status.HANDOFF);}
    public synchronized void recordFailed(String command,String result){record(command,result,Status.FAILED);}
    public synchronized void record(String command,String result,Status status){
        try{
            JSONArray old=new JSONArray(p.getString(KEY,"[]"));
            JSONArray fresh=new JSONArray();
            JSONObject x=new JSONObject();
            x.put("ts",System.currentTimeMillis());
            x.put("command",safe(command,500));
            x.put("result",safe(result,1400));
            Status st=status==null?Status.REPORTED:status;
            x.put("status",st.name());
            x.put("verified",st==Status.VERIFIED);
            fresh.put(x);
            for(int i=0;i<old.length()&&fresh.length()<MAX;i++)fresh.put(old.get(i));
            p.edit().putString(KEY,fresh.toString()).apply();
        }catch(Exception ignored){}
    }

    public synchronized String latest(){
        try{JSONArray a=new JSONArray(p.getString(KEY,"[]"));if(a.length()==0)return "هنوز اقدامی ثبت نشده است.";return format(a.getJSONObject(0));}
        catch(Exception e){return "خواندن آخرین اقدام ممکن نشد.";}
    }

    public synchronized String recent(int n){
        try{
            JSONArray a=new JSONArray(p.getString(KEY,"[]"));if(a.length()==0)return "هنوز اقدامی ثبت نشده است.";
            StringBuilder b=new StringBuilder();for(int i=0;i<a.length()&&i<Math.max(1,n);i++){if(i>0)b.append("\n\n");b.append(format(a.getJSONObject(i)));}return b.toString();
        }catch(Exception e){return "خواندن سوابق اقدام ممکن نشد.";}
    }

    private String format(JSONObject x){
        long ts=x.optLong("ts",0);String when=ts>0?new SimpleDateFormat("MM/dd HH:mm",Locale.US).format(new Date(ts)):"";
        String raw=x.optString("status","");if(raw.isEmpty())raw=x.optBoolean("verified",false)?Status.VERIFIED.name():Status.REPORTED.name();
        String v;
        try{switch(Status.valueOf(raw)){case VERIFIED:v="تأییدشده";break;case HANDOFF:v="تحویل‌شده به سیستم/برنامه";break;case FAILED:v="ناموفق";break;default:v="گزارش‌شده";}}
        catch(Exception e){v="گزارش‌شده";}
        return "• "+when+" • "+v+"\nفرمان: "+x.optString("command")+"\nنتیجه: "+x.optString("result");
    }
    private String safe(String s,int max){if(s==null)return "";s=s.trim();return s.length()<=max?s:s.substring(0,max);}
}
