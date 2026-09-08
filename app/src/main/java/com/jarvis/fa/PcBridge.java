package com.jarvis.fa;

import android.content.*;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class PcBridge {
    private final SecureStore secrets;
    private final SharedPreferences prefs;
    public PcBridge(Context c){secrets=new SecureStore(c);prefs=c.getSharedPreferences("jarvis_pc_bridge",Context.MODE_PRIVATE);}
    public void configure(String baseUrl,String token){prefs.edit().putString("url",baseUrl==null?"":baseUrl.trim()).apply();if(token!=null&&!token.trim().isEmpty())secrets.put("pc_bridge_token",token.trim());}
    public boolean configured(){return !prefs.getString("url","").trim().isEmpty()&&!secrets.get("pc_bridge_token").isEmpty();}
    public String endpoint(){return prefs.getString("url","");}
    public String send(String command){if(!configured())return "PC Bridge هنوز تنظیم نشده است.";try{URL u=new URL(endpoint().replaceAll("/$","")+"/jarvis/command");HttpURLConnection c=(HttpURLConnection)u.openConnection();c.setRequestMethod("POST");c.setConnectTimeout(5000);c.setReadTimeout(12000);c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");c.setRequestProperty("Authorization","Bearer "+secrets.get("pc_bridge_token"));byte[] body=new JSONObject().put("command",command).toString().getBytes(StandardCharsets.UTF_8);try(OutputStream o=c.getOutputStream()){o.write(body);}InputStream in=c.getResponseCode()>=200&&c.getResponseCode()<300?c.getInputStream():c.getErrorStream();String text=read(in);if(c.getResponseCode()<200||c.getResponseCode()>=300)return "PC Bridge پاسخ خطا داد: HTTP "+c.getResponseCode();try{return new JSONObject(text).optString("message",text);}catch(Exception e){return text.isEmpty()?"فرمان به PC ارسال شد.":text;}}catch(Exception e){return "ارتباط با PC برقرار نشد: "+e.getClass().getSimpleName();}}
    private String read(InputStream in)throws Exception{if(in==null)return "";ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] x=new byte[4096];for(int n;(n=in.read(x))>0;)b.write(x,0,n);return b.toString("UTF-8");}
}
