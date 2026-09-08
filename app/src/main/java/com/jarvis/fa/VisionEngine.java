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
    public interface AgentCallback { void onResult(VisionAgentResult result); }
    private static final String OPENAI_ENDPOINT="https://api.openai.com/v1/responses";
    private static final String MODEL="gpt-5.6-luna";
    private final ExecutorService ex=Executors.newSingleThreadExecutor();
    private final SecureStore secrets;
    private final ProductionConfig prod;

    public VisionEngine(Context c){secrets=new SecureStore(c);prod=new ProductionConfig(c);}

    public void analyze(Bitmap bitmap,String question,Callback cb){
        analyzeAgent(bitmap,question,r->cb.onResult(r.display()));
    }

    public void analyzeAgent(Bitmap bitmap,String question,AgentCallback cb){
        if(bitmap==null){cb.onResult(errorResult("اول یک تصویر از دوربین یا گالری انتخاب کن."));return;}
        String token=prod.productionMode()?prod.backendToken():secrets.get("openai_key");
        if(token.isEmpty()){cb.onResult(errorResult(prod.productionMode()?"Backend امن برای Vision تنظیم نشده است.":"برای Vision ابتدا OpenAI یا Backend امن را تنظیم کن."));return;}
        ex.submit(()->{
            VisionAgentResult result;
            try{
                String q=question==null||question.trim().isEmpty()?"این تصویر را بررسی کن، اطلاعات مهم را استخراج کن و اگر اقدام مفیدی وجود دارد پیشنهاد بده.":question.trim();
                String raw=call(bitmap,q,token);
                result=VisionAgentResult.fromJson(raw);
            }catch(Exception e){result=errorResult(pretty(e.getMessage()));}
            final VisionAgentResult out=result;new Handler(Looper.getMainLooper()).post(()->cb.onResult(out));
        });
    }

    private VisionAgentResult errorResult(String text){VisionAgentResult r=new VisionAgentResult();r.summary=text;r.confidence=0;r.suggestedAction="none";return r;}

    private String call(Bitmap source,String q,String token)throws Exception{
        Bitmap b=scale(source,1280);
        ByteArrayOutputStream jpg=new ByteArrayOutputStream();b.compress(Bitmap.CompressFormat.JPEG,82,jpg);
        String data="data:image/jpeg;base64,"+Base64.encodeToString(jpg.toByteArray(),Base64.NO_WRAP);
        JSONArray content=new JSONArray();
        content.put(new JSONObject().put("type","input_text").put("text",q));
        content.put(new JSONObject().put("type","input_image").put("image_url",data).put("detail","auto"));
        JSONArray input=new JSONArray().put(new JSONObject().put("role","user").put("content",content));
        String instructions="تو JARVIS Vision Agent هستی. تصویر را دقیق تحلیل کن و فقط اطلاعات قابل استنباط را گزارش کن. خروجی باید فقط یک JSON معتبر و بدون markdown باشد با کلیدهای: summary, extracted_text, entity_type, confidence, suggested_action, action_payload, rationale. confidence عدد 0 تا 100 است. suggested_action فقط یکی از none, web_search, maps, reminder, calendar, email, save_memory باشد. اگر اطلاعات کافی نیست none بگذار. برای reminder payload باید JSON با title و time_text باشد. برای calendar payload باید title,time_text,duration_minutes,location,description باشد. برای web_search/maps payload باید query باشد. برای email payload باید to,subject,body باشد. برای save_memory payload باید key,value باشد. هرگز اقدام خطرناک یا غیرقابل برگشت پیشنهاد نده. متن پاسخ‌ها فارسی باشد ولی نام actionها دقیقاً انگلیسی بماند.";
        JSONObject body=new JSONObject().put("model",MODEL).put("instructions",instructions).put("input",input);
        HttpURLConnection c=(HttpURLConnection)new URL(endpoint()).openConnection();c.setRequestMethod("POST");c.setConnectTimeout(20000);c.setReadTimeout(90000);c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json; charset=utf-8");c.setRequestProperty("Authorization","Bearer "+token);
        try(OutputStream os=c.getOutputStream()){os.write(body.toString().getBytes(StandardCharsets.UTF_8));}
        int code=c.getResponseCode();String txt=read(code>=200&&code<300?c.getInputStream():c.getErrorStream());if(code<200||code>=300)throw new IOException("HTTP "+code+" "+txt);
        String t=parseText(new JSONObject(txt));return t.isEmpty()?"{\"summary\":\"تحلیل تصویری پاسخی برنگرداند.\",\"extracted_text\":\"\",\"entity_type\":\"unknown\",\"confidence\":0,\"suggested_action\":\"none\",\"action_payload\":{},\"rationale\":\"\"}":t;
    }

    private Bitmap scale(Bitmap src,int max){int w=src.getWidth(),h=src.getHeight();if(w<=max&&h<=max)return src;float r=Math.min((float)max/w,(float)max/h);return Bitmap.createScaledBitmap(src,Math.max(1,Math.round(w*r)),Math.max(1,Math.round(h*r)),true);}
    private String endpoint(){if(!prod.productionMode())return OPENAI_ENDPOINT;String u=prod.backendUrl();if(u.endsWith("/responses"))return u;return u.replaceAll("/$","")+"/v1/responses";}
    private String parseText(JSONObject root){StringBuilder s=new StringBuilder();JSONArray out=root.optJSONArray("output");if(out==null)return "";for(int i=0;i<out.length();i++){JSONObject item=out.optJSONObject(i);JSONArray cc=item==null?null:item.optJSONArray("content");if(cc==null)continue;for(int j=0;j<cc.length();j++){JSONObject p=cc.optJSONObject(j);if(p!=null&&"output_text".equals(p.optString("type"))){String t=p.optString("text","");if(!t.isEmpty()){if(s.length()>0)s.append('\n');s.append(t);}}}}return s.toString().trim();}
    private String read(InputStream in)throws Exception{if(in==null)return "";ByteArrayOutputStream o=new ByteArrayOutputStream();byte[] x=new byte[4096];for(int n;(n=in.read(x))!=-1;)o.write(x,0,n);return o.toString("UTF-8");}
    private String pretty(String m){if(m==null)m="";if(m.startsWith("HTTP 401"))return "Vision: توکن یا API Key معتبر نیست.";if(m.startsWith("HTTP 429"))return "Vision: سقف مصرف یا اعتبار API مشکل دارد.";if(m.startsWith("HTTP 400"))return "Vision: درخواست تصویر توسط API رد شد.";if(m.toLowerCase().contains("timeout"))return "Vision: ارتباط با سرور Timeout شد.";return "Vision متصل نشد: "+(m.length()>180?m.substring(0,180):m);}
    public void shutdown(){ex.shutdownNow();}
}
