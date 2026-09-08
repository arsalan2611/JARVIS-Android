package com.jarvis.fa;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

public class VisionEngine {
    public interface Callback { void onResult(String text); }
    private static final String OPENAI_ENDPOINT="https://api.openai.com/v1/responses";
    private static final String MODEL="gpt-5.6-luna";
    private final ExecutorService ex=Executors.newSingleThreadExecutor();
    private final SecureStore secrets;
    private final ProductionConfig prod;

    public VisionEngine(Context c){secrets=new SecureStore(c);prod=new ProductionConfig(c);}

    public void analyze(Bitmap bitmap,String question,Callback cb){
        if(bitmap==null){cb.onResult("اول یک تصویر از دوربین یا گالری انتخاب کن.");return;}
        String token=prod.productionMode()?prod.backendToken():secrets.get("openai_key");
        if(token.isEmpty()){cb.onResult(prod.productionMode()?"Backend امن برای Vision تنظیم نشده است.":"برای Vision ابتدا OpenAI یا Backend امن را تنظیم کن.");return;}
        ex.submit(()->{
            String result;
            try{result=call(bitmap,question==null||question.trim().isEmpty()?"این تصویر را دقیق بررسی کن و مهم‌ترین نکاتش را به فارسی توضیح بده.":question.trim(),token);}catch(Exception e){result=pretty(e.getMessage());}
            final String out=result;new Handler(Looper.getMainLooper()).post(()->cb.onResult(out));
        });
    }

    private String call(Bitmap source,String q,String token)throws Exception{
        Bitmap b=scale(source,1280);
        ByteArrayOutputStream jpg=new ByteArrayOutputStream();b.compress(Bitmap.CompressFormat.JPEG,82,jpg);
        String data="data:image/jpeg;base64,"+Base64.encodeToString(jpg.toByteArray(),Base64.NO_WRAP);
        JSONArray content=new JSONArray();
        content.put(new JSONObject().put("type","input_text").put("text",q));
        content.put(new JSONObject().put("type","input_image").put("image_url",data).put("detail","auto"));
        JSONArray input=new JSONArray().put(new JSONObject().put("role","user").put("content",content));
        JSONObject body=new JSONObject().put("model",MODEL).put("instructions","تو JARVIS Vision هستی. تصویر را دقیق تحلیل کن، فقط چیزهایی را بگو که واقعاً از تصویر قابل استنباط است، اگر مطمئن نیستی صریح بگو. پاسخ را فارسی، کاربردی و نسبتاً کوتاه بده.").put("input",input);
        HttpURLConnection c=(HttpURLConnection)new URL(endpoint()).openConnection();c.setRequestMethod("POST");c.setConnectTimeout(20000);c.setReadTimeout(90000);c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json; charset=utf-8");c.setRequestProperty("Authorization","Bearer "+token);
        try(OutputStream os=c.getOutputStream()){os.write(body.toString().getBytes(StandardCharsets.UTF_8));}
        int code=c.getResponseCode();String txt=read(code>=200&&code<300?c.getInputStream():c.getErrorStream());if(code<200||code>=300)throw new IOException("HTTP "+code+" "+txt);
        String t=parseText(new JSONObject(txt));return t.isEmpty()?"تحلیل تصویری پاسخی برنگرداند.":t;
    }

    private Bitmap scale(Bitmap src,int max){int w=src.getWidth(),h=src.getHeight();if(w<=max&&h<=max)return src;float r=Math.min((float)max/w,(float)max/h);return Bitmap.createScaledBitmap(src,Math.max(1,Math.round(w*r)),Math.max(1,Math.round(h*r)),true);}
    private String endpoint(){if(!prod.productionMode())return OPENAI_ENDPOINT;String u=prod.backendUrl();if(u.endsWith("/responses"))return u;return u.replaceAll("/$","")+"/v1/responses";}
    private String parseText(JSONObject root){StringBuilder s=new StringBuilder();JSONArray out=root.optJSONArray("output");if(out==null)return "";for(int i=0;i<out.length();i++){JSONObject item=out.optJSONObject(i);JSONArray cc=item==null?null:item.optJSONArray("content");if(cc==null)continue;for(int j=0;j<cc.length();j++){JSONObject p=cc.optJSONObject(j);if(p!=null&&"output_text".equals(p.optString("type"))){String t=p.optString("text","");if(!t.isEmpty()){if(s.length()>0)s.append('\n');s.append(t);}}}}return s.toString().trim();}
    private String read(InputStream in)throws Exception{if(in==null)return "";ByteArrayOutputStream o=new ByteArrayOutputStream();byte[] x=new byte[4096];for(int n;(n=in.read(x))!=-1;)o.write(x,0,n);return o.toString("UTF-8");}
    private String pretty(String m){if(m==null)m="";if(m.startsWith("HTTP 401"))return "Vision: توکن یا API Key معتبر نیست.";if(m.startsWith("HTTP 429"))return "Vision: سقف مصرف یا اعتبار API مشکل دارد.";if(m.startsWith("HTTP 400"))return "Vision: درخواست تصویر توسط API رد شد.";if(m.toLowerCase().contains("timeout"))return "Vision: ارتباط با سرور Timeout شد.";return "Vision متصل نشد: "+(m.length()>180?m.substring(0,180):m);}
    public void shutdown(){ex.shutdownNow();}
}
