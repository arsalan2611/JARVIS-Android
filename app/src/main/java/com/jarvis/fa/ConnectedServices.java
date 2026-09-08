package com.jarvis.fa;

import android.content.Context;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

public class ConnectedServices {
    public static class Status {
        public final boolean backendReady,googleConfigured,googleConnected,gmail,calendar;
        public final String detail;
        Status(boolean b,boolean gc,boolean gx,boolean gm,boolean ca,String d){backendReady=b;googleConfigured=gc;googleConnected=gx;gmail=gm;calendar=ca;detail=d;}
    }
    private final ProductionConfig prod;
    public ConnectedServices(Context c){prod=new ProductionConfig(c);}

    public Status status(){
        if(!prod.productionMode()||!prod.backendReady())return new Status(false,false,false,false,false,"Backend امن هنوز آماده نیست.");
        try{
            JSONObject x=request("GET","/integrations/status",null);
            JSONObject g=x.optJSONObject("google");
            if(g==null)return new Status(true,false,false,false,false,"پاسخ Google integration ناقص است.");
            return new Status(true,g.optBoolean("configured"),g.optBoolean("connected"),g.optBoolean("gmail"),g.optBoolean("calendar"),"OK");
        }catch(Exception e){return new Status(true,false,false,false,false,"اتصال Backend برقرار نشد: "+e.getClass().getSimpleName());}
    }
    public String startGoogleOAuth(){
        try{return request("POST","/integrations/google/start",new JSONObject()).optString("auth_url","");}
        catch(Exception e){return "";}
    }
    public String gmailSearch(String query,int max){
        try{
            JSONObject x=request("POST","/gmail/search",new JSONObject().put("query",query==null?"":query).put("max_results",Math.max(1,Math.min(max,12))));
            JSONArray a=x.optJSONArray("messages");if(a==null||a.length()==0)return "ایمیلی مطابق جستجو پیدا نشد.";
            StringBuilder b=new StringBuilder();
            for(int i=0;i<a.length();i++){JSONObject m=a.optJSONObject(i);if(m==null)continue;if(b.length()>0)b.append("\n\n");b.append(i+1).append(") ").append(m.optString("subject","(بدون موضوع)")).append("\nاز: ").append(m.optString("from","")).append("\n").append(m.optString("date","")).append("\n").append(m.optString("snippet",""));}
            return b.toString();
        }catch(Exception e){return "Gmail متصل نیست یا پاسخ نداد: "+e.getClass().getSimpleName();}
    }
    public String gmailDraft(String to,String subject,String body){
        try{
            JSONObject x=request("POST","/gmail/draft",new JSONObject().put("to",to).put("subject",subject).put("body",body));
            return x.optBoolean("ok")?"Draft ایمیل در Gmail ساخته شد. ارسال نشده است.":"ساخت Draft ایمیل ناموفق بود.";
        }catch(Exception e){return "ساخت Draft ایمیل ممکن نشد: "+e.getClass().getSimpleName();}
    }
    public String calendarUpcoming(int max){
        try{
            JSONObject x=request("POST","/calendar/upcoming",new JSONObject().put("max_results",Math.max(1,Math.min(max,15))));
            JSONArray a=x.optJSONArray("events");if(a==null||a.length()==0)return "رویداد آینده‌ای در Calendar پیدا نشد.";
            StringBuilder b=new StringBuilder();for(int i=0;i<a.length();i++){JSONObject e=a.optJSONObject(i);if(e==null)continue;if(b.length()>0)b.append("\n");b.append("• ").append(e.optString("summary","بدون عنوان")).append(" — ").append(e.optString("start",""));String l=e.optString("location","");if(!l.isEmpty())b.append(" — ").append(l);}return b.toString();
        }catch(Exception e){return "Calendar متصل نیست یا پاسخ نداد: "+e.getClass().getSimpleName();}
    }
    public String calendarCreate(String title,long startMs,int durationMinutes,String location,String description){
        try{
            if(startMs<=System.currentTimeMillis())return "زمان رویداد معتبر نیست.";
            int dur=Math.max(5,durationMinutes);TimeZone tz=TimeZone.getDefault();SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX",Locale.US);f.setTimeZone(tz);
            JSONObject body=new JSONObject().put("title",title).put("start",f.format(new Date(startMs))).put("end",f.format(new Date(startMs+dur*60000L))).put("timezone",tz.getID()).put("location",location==null?"":location).put("description",description==null?"":description);
            JSONObject x=request("POST","/calendar/events",body);return x.optBoolean("ok")?"رویداد در Google Calendar ثبت شد.":"ثبت رویداد در Google Calendar ناموفق بود.";
        }catch(Exception e){return "ثبت Calendar ممکن نشد: "+e.getClass().getSimpleName();}
    }

    private JSONObject request(String method,String path,JSONObject body)throws Exception{
        if(!prod.productionMode()||!prod.backendReady())throw new IOException("backend_not_ready");
        String base=prod.backendUrl().replaceAll("/$","");URL u=new URL(base+path);HttpURLConnection c=(HttpURLConnection)u.openConnection();c.setRequestMethod(method);c.setConnectTimeout(6000);c.setReadTimeout(25000);c.setRequestProperty("Accept","application/json");c.setRequestProperty("Authorization","Bearer "+prod.backendToken());
        if(body!=null){c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");byte[] raw=body.toString().getBytes(StandardCharsets.UTF_8);try(OutputStream o=c.getOutputStream()){o.write(raw);}}
        int code=c.getResponseCode();InputStream in=code>=200&&code<300?c.getInputStream():c.getErrorStream();String text=read(in);if(code<200||code>=300)throw new IOException("HTTP_"+code+" "+text);return new JSONObject(text.isEmpty()?"{}":text);
    }
    private String read(InputStream in)throws Exception{if(in==null)return "";ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] x=new byte[4096];for(int n;(n=in.read(x))>0;)b.write(x,0,n);return b.toString("UTF-8");}
}
