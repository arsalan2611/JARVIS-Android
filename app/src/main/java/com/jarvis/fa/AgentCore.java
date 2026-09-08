package com.jarvis.fa;

import android.app.Activity;
import android.provider.Settings;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

public class AgentCore {
    public interface Callback { void onResult(String text); }
    private static final String ENDPOINT="https://api.openai.com/v1/responses";
    private static final String MODEL="gpt-5.6-luna";
    private static final int MAX_TOOL_ROUNDS=6;
    private final Activity activity;
    private final JarvisActions actions;
    private final ReminderEngine reminders;
    private final MemoryStore memory;
    private final SecureStore secrets;
    private final ExecutorService ex=Executors.newSingleThreadExecutor();

    public AgentCore(Activity a){activity=a;actions=new JarvisActions(a);reminders=new ReminderEngine(a);memory=new MemoryStore(a);secrets=new SecureStore(a);}

    public void run(String userText, Callback cb){
        ex.submit(()->{
            String answer;
            try{answer=agentLoop(userText);}catch(Exception e){answer=prettyError(e.getMessage());}
            final String out=answer;
            activity.runOnUiThread(()->cb.onResult(out));
        });
    }

    private String agentLoop(String userText)throws Exception{
        String key=secrets.get("openai_key");
        if(key.isEmpty()) return "اول از بخش OpenAI، API Key را وارد و تست کن.";
        memory.addTurn("user",userText);
        JSONObject current=request(baseRequest(userText),key);
        String lastToolResult="";
        for(int round=0;round<MAX_TOOL_ROUNDS;round++){
            JSONArray calls=functionCalls(current);
            if(calls.length()==0){
                String t=text(current);
                if(t.isEmpty()) t=lastToolResult.isEmpty()?"پاسخی دریافت نشد.":lastToolResult;
                memory.addTurn("assistant",t);
                return t;
            }
            JSONArray outputs=new JSONArray();
            for(int i=0;i<calls.length();i++){
                JSONObject call=calls.optJSONObject(i);if(call==null)continue;
                String callId=call.optString("call_id");
                String name=call.optString("name");
                JSONObject args;
                try{args=new JSONObject(call.optString("arguments","{}"));}catch(Exception e){args=new JSONObject();}
                lastToolResult=execute(name,args);
                outputs.put(new JSONObject().put("type","function_call_output").put("call_id",callId).put("output",lastToolResult));
            }
            JSONObject follow=new JSONObject();
            follow.put("model",MODEL);
            follow.put("previous_response_id",current.optString("id"));
            follow.put("input",outputs);
            follow.put("instructions",instructions());
            follow.put("tools",tools());
            follow.put("tool_choice","auto");
            current=request(follow,key);
        }
        String t=text(current);
        if(t.isEmpty()) t=lastToolResult.isEmpty()?"فرمان بیش از حد چندمرحله‌ای شد؛ بخشی از کار انجام شد.":lastToolResult;
        memory.addTurn("assistant",t);
        return t;
    }

    private JSONArray functionCalls(JSONObject root){
        JSONArray found=new JSONArray();JSONArray output=root.optJSONArray("output");if(output==null)return found;
        for(int i=0;i<output.length();i++){JSONObject o=output.optJSONObject(i);if(o!=null&&"function_call".equals(o.optString("type")))found.put(o);}return found;
    }

    private JSONObject baseRequest(String q)throws Exception{
        JSONObject b=new JSONObject();b.put("model",MODEL);b.put("instructions",instructions());b.put("input",q);b.put("tools",tools());b.put("tool_choice","auto");return b;
    }

    private String instructions(){
        String now=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z",Locale.US).format(new Date());
        return "تو JARVIS هستی؛ دستیار عامل‌محور فارسی. زمان فعلی گوشی: "+now+". درخواست را تا جای ممکن کامل انجام بده. اگر چند اقدام لازم است می‌توانی چند ابزار را در یک مرحله یا مراحل متوالی فراخوانی کنی. هیچ اقدامی را جعل نکن. اگر ابزار شکست خورد، صریح بگو. برای تماس فقط شماره‌گیر را باز کن. برای یادآوری epoch_ms دقیق تولید کن. پاسخ نهایی کوتاه، طبیعی و فارسی باشد.\n\nحافظه:\n"+memory.context();
    }

    private JSONArray tools()throws Exception{
        JSONArray t=new JSONArray();
        t.put(fn("open_maps","باز کردن نقشه برای مکان یا مقصد",props(new String[][]{{"query","string","مکان یا مقصد"}}),new String[]{"query"}));
        t.put(fn("web_search","جستجوی وب",props(new String[][]{{"query","string","عبارت جستجو"}}),new String[]{"query"}));
        t.put(fn("open_app","باز کردن اپ شناخته‌شده",props(new String[][]{{"app","string","whatsapp|telegram|instagram"}}),new String[]{"app"}));
        t.put(fn("dial_number","باز کردن شماره‌گیر",props(new String[][]{{"number","string","شماره تلفن"}}),new String[]{"number"}));
        t.put(fn("flashlight","روشن یا خاموش کردن چراغ‌قوه",props(new String[][]{{"on","boolean","true روشن false خاموش"}}),new String[]{"on"}));
        t.put(fn("wifi_settings","باز کردن تنظیمات وای‌فای",emptyProps(),new String[]{}));
        t.put(fn("bluetooth_settings","باز کردن تنظیمات بلوتوث",emptyProps(),new String[]{}));
        t.put(fn("assistant_settings","باز کردن تنظیمات دستیار صوتی اندروید برای انتخاب JARVIS",emptyProps(),new String[]{}));
        t.put(fn("create_reminder","ثبت یادآوری واقعی",props(new String[][]{{"title","string","متن یادآوری"},{"epoch_ms","integer","زمان اجرا به میلی‌ثانیه Unix"}}),new String[]{"title","epoch_ms"}));
        t.put(fn("list_reminders","نمایش یادآوری‌ها",emptyProps(),new String[]{}));
        t.put(fn("remember_fact","ذخیره یک واقعیت در حافظه جارویس",props(new String[][]{{"fact","string","واقعیت برای حافظه"}}),new String[]{"fact"}));
        return t;
    }

    private JSONObject emptyProps()throws Exception{return new JSONObject().put("type","object").put("properties",new JSONObject()).put("additionalProperties",false);}
    private JSONObject props(String[][] defs)throws Exception{
        JSONObject p=new JSONObject();for(String[] d:defs){JSONObject x=new JSONObject().put("type",d[1]).put("description",d[2]);p.put(d[0],x);}return new JSONObject().put("type","object").put("properties",p).put("additionalProperties",false);
    }
    private JSONObject fn(String name,String desc,JSONObject params,String[] req)throws Exception{
        JSONArray r=new JSONArray();for(String x:req)r.put(x);params.put("required",r);return new JSONObject().put("type","function").put("name",name).put("description",desc).put("parameters",params);
    }

    private String execute(String name,JSONObject a){
        try{
            switch(name){
                case "open_maps": return actions.openMaps(a.optString("query"))?"نقشه باز شد.":"باز کردن نقشه شکست خورد.";
                case "web_search": return actions.webSearch(a.optString("query"))?"جستجو باز شد.":"جستجو باز نشد.";
                case "open_app":{String app=a.optString("app").toLowerCase(Locale.ROOT);String pkg=app.contains("whatsapp")?"com.whatsapp":app.contains("telegram")?"org.telegram.messenger":app.contains("instagram")?"com.instagram.android":"";return !pkg.isEmpty()&&actions.openApp(pkg)?"برنامه باز شد.":"برنامه پیدا نشد.";}
                case "dial_number": return actions.dial(a.optString("number"))?"شماره‌گیر باز شد.":"شماره‌گیر باز نشد.";
                case "flashlight": return actions.flashlight(a.optBoolean("on"))?"چراغ‌قوه تغییر وضعیت داد.":"کنترل چراغ‌قوه شکست خورد.";
                case "wifi_settings": return actions.settings(Settings.ACTION_WIFI_SETTINGS)?"تنظیمات وای‌فای باز شد.":"تنظیمات وای‌فای باز نشد.";
                case "bluetooth_settings": return actions.settings(Settings.ACTION_BLUETOOTH_SETTINGS)?"تنظیمات بلوتوث باز شد.":"تنظیمات بلوتوث باز نشد.";
                case "assistant_settings": return actions.settings("android.settings.VOICE_INPUT_SETTINGS")?"تنظیمات دستیار صوتی باز شد.":"تنظیمات دستیار صوتی باز نشد.";
                case "create_reminder":{long at=a.optLong("epoch_ms",0);String title=a.optString("title","یادآوری جارویس");if(at<=System.currentTimeMillis())return "زمان یادآوری معتبر نیست یا در گذشته است.";return reminders.schedule(title,at)?"یادآوری با موفقیت ثبت شد.":"ثبت یادآوری شکست خورد.";}
                case "list_reminders": return reminders.summary();
                case "remember_fact":{String f=a.optString("fact").trim();if(f.isEmpty())return "چیزی برای ذخیره نبود.";memory.remember(f);return "در حافظه ذخیره شد.";}
                default:return "ابزار ناشناخته و اجرا نشد.";
            }
        }catch(Exception e){return "اجرای ابزار خطا داد: "+e.getClass().getSimpleName();}
    }

    private JSONObject request(JSONObject body,String key)throws Exception{
        HttpURLConnection c=(HttpURLConnection)new URL(ENDPOINT).openConnection();c.setRequestMethod("POST");c.setDoOutput(true);c.setConnectTimeout(20000);c.setReadTimeout(90000);c.setRequestProperty("Content-Type","application/json");c.setRequestProperty("Authorization","Bearer "+key);
        try(OutputStream os=c.getOutputStream()){os.write(body.toString().getBytes(StandardCharsets.UTF_8));}
        int code=c.getResponseCode();String txt=read(code>=200&&code<300?c.getInputStream():c.getErrorStream());if(code<200||code>=300)throw new IOException("HTTP "+code+" "+txt);return new JSONObject(txt);
    }
    private String text(JSONObject root){StringBuilder s=new StringBuilder();JSONArray out=root.optJSONArray("output");if(out==null)return "";for(int i=0;i<out.length();i++){JSONObject item=out.optJSONObject(i);JSONArray content=item==null?null:item.optJSONArray("content");if(content==null)continue;for(int j=0;j<content.length();j++){JSONObject part=content.optJSONObject(j);if(part!=null&&"output_text".equals(part.optString("type"))){String x=part.optString("text","");if(!x.isEmpty()){if(s.length()>0)s.append('\n');s.append(x);}}}}return s.toString().trim();}
    private String prettyError(String m){if(m==null)m="";if(m.startsWith("HTTP 401"))return "API Key نامعتبر است.";if(m.startsWith("HTTP 429"))return "اعتبار API یا سقف مصرف مشکل دارد.";if(m.startsWith("HTTP 403"))return "دسترسی API مجاز نیست.";if(m.startsWith("HTTP 400"))return "درخواست Agent توسط OpenAI رد شد.";return "هسته Agent متصل نشد: "+(m.length()>160?m.substring(0,160):m);}
    private String read(InputStream is)throws Exception{if(is==null)return "";ByteArrayOutputStream o=new ByteArrayOutputStream();byte[] b=new byte[4096];int n;while((n=is.read(b))!=-1)o.write(b,0,n);return o.toString("UTF-8");}
    public void shutdown(){ex.shutdownNow();}
}
