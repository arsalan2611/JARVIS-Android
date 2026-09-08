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
    public interface StateCallback { void onState(VoiceStateMachine.State state); }
    private static final String OPENAI_ENDPOINT="https://api.openai.com/v1/responses";
    private static final String MODEL="gpt-5.6-luna";
    private static final int MAX_TOOL_ROUNDS=8;
    private final Activity activity;
    private final JarvisActions actions;
    private final ReminderEngine reminders;
    private final MemoryStore memory;
    private final SecureStore secrets;
    private final PersonalActions personal;
    private final PersianTimeParser timeParser;
    private final WorkTools work;
    private final PcBridge pc;
    private final SystemHealth health;
    private final AgentPolicy policy;
    private final ProductionConfig prod;
    private final ConnectedServices connected;
    private final ExecutorService ex=Executors.newSingleThreadExecutor();
    private volatile StateCallback stateCallback;

    public AgentCore(Activity a){activity=a;actions=new JarvisActions(a);reminders=new ReminderEngine(a);memory=new MemoryStore(a);secrets=new SecureStore(a);personal=new PersonalActions(a);timeParser=new PersianTimeParser();work=new WorkTools(a);pc=new PcBridge(a);health=new SystemHealth(a);policy=new AgentPolicy();prod=new ProductionConfig(a);connected=new ConnectedServices(a);}
    public void setStateCallback(StateCallback cb){stateCallback=cb;}
    private void state(VoiceStateMachine.State s){StateCallback cb=stateCallback;if(cb!=null)activity.runOnUiThread(()->cb.onState(s));}
    public void run(String userText, Callback cb){ex.submit(()->{String answer;try{state(VoiceStateMachine.State.THINKING);answer=agentLoop(userText);}catch(Exception e){answer=prettyError(e.getMessage());}final String out=answer;activity.runOnUiThread(()->cb.onResult(out));});}

    private String agentLoop(String userText)throws Exception{
        String key=authToken();if(key.isEmpty())return prod.productionMode()?"حالت Production فعال است اما Backend امن تنظیم نشده.":"اول از بخش OpenAI، API Key را وارد و تست کن.";
        memory.addTurn("user",userText);
        JSONObject current=request(baseRequest(userText),key);String lastToolResult="";
        for(int round=0;round<MAX_TOOL_ROUNDS;round++){
            JSONArray calls=functionCalls(current);
            if(calls.length()==0){String t=text(current);if(t.isEmpty())t=lastToolResult.isEmpty()?"پاسخی دریافت نشد.":lastToolResult;memory.addTurn("assistant",t);return t;}
            state(VoiceStateMachine.State.EXECUTING);JSONArray outputs=new JSONArray();
            for(int i=0;i<calls.length();i++){
                JSONObject call=calls.optJSONObject(i);if(call==null)continue;String callId=call.optString("call_id"),name=call.optString("name");JSONObject args;try{args=new JSONObject(call.optString("arguments","{}"));}catch(Exception e){args=new JSONObject();}
                AgentPolicy.Decision d=policy.assess(name,jsonMap(args));
                if(d.risk==AgentPolicy.Risk.BLOCKED)lastToolResult=d.reason;else if(d.risk==AgentPolicy.Risk.CONFIRM)lastToolResult="نیاز به تأیید کاربر: "+d.reason;else lastToolResult=execute(name,args);
                outputs.put(new JSONObject().put("type","function_call_output").put("call_id",callId).put("output",lastToolResult));
            }
            state(VoiceStateMachine.State.THINKING);
            JSONObject follow=new JSONObject().put("model",MODEL).put("previous_response_id",current.optString("id")).put("input",outputs).put("instructions",instructions()).put("tools",tools()).put("tool_choice","auto");
            current=request(follow,key);
        }
        String t=text(current);if(t.isEmpty())t=lastToolResult.isEmpty()?"فرمان بیش از حد چندمرحله‌ای شد؛ بخشی از کار انجام شد.":lastToolResult;memory.addTurn("assistant",t);return t;
    }

    private String authToken(){return prod.productionMode()?prod.backendToken():secrets.get("openai_key");}
    private String endpoint(){if(!prod.productionMode())return OPENAI_ENDPOINT;String u=prod.backendUrl();if(u.endsWith("/responses"))return u;return u.replaceAll("/$","")+"/v1/responses";}
    private JSONArray functionCalls(JSONObject root){JSONArray found=new JSONArray(),output=root.optJSONArray("output");if(output==null)return found;for(int i=0;i<output.length();i++){JSONObject o=output.optJSONObject(i);if(o!=null&&"function_call".equals(o.optString("type")))found.put(o);}return found;}
    private JSONObject baseRequest(String q)throws Exception{return new JSONObject().put("model",MODEL).put("instructions",instructions()).put("input",q).put("tools",tools()).put("tool_choice","auto");}
    private String instructions(){String now=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z",Locale.US).format(new Date());return "تو JARVIS 1.0 هستی؛ دستیار عامل‌محور فارسی. زمان فعلی گوشی: "+now+". درخواست را تا جای ممکن کامل انجام بده و بعد از ابزار نتیجه را بررسی کن. برای زمان فارسی از create_reminder_fa استفاده کن. شماره مخاطب را حدس نزن. برای Gmail و Google Calendar در حالت Production از ابزارهای google_* استفاده کن؛ اگر اتصال آماده نبود صریح بگو. gmail_draft فقط Draft می‌سازد و هرگز معادل Send نیست؛ هرگز ادعا نکن ایمیل ارسال شده. اگر Google Calendar متصل باشد برای ثبت مستقیم رویداد از google_calendar_create استفاده کن؛ create_calendar_event فقط فرم سیستم را باز می‌کند. اگر چند اقدام لازم است مرحله‌ای اجرا کن. هیچ اقدامی را جعل نکن. عملیات اثرگذار باید طبق نتیجه ابزار و Policy باشد. برای کار روی کامپیوتر فقط از pc_command استفاده کن و اگر Bridge تنظیم نیست صریح بگو. پاسخ نهایی کوتاه و طبیعی باشد.\n\nحافظه گفتگو:\n"+memory.context();}

    private JSONArray tools()throws Exception{
        JSONArray t=new JSONArray();
        t.put(fn("open_maps","باز کردن نقشه برای مکان یا مقصد",props(new String[][]{{"query","string","مکان یا مقصد"}}),new String[]{"query"}));
        t.put(fn("web_search","جستجوی وب",props(new String[][]{{"query","string","عبارت جستجو"}}),new String[]{"query"}));
        t.put(fn("open_app","باز کردن اپ شناخته‌شده",props(new String[][]{{"app","string","whatsapp|telegram|instagram"}}),new String[]{"app"}));
        t.put(fn("dial_number","باز کردن شماره‌گیر",props(new String[][]{{"number","string","شماره تلفن"}}),new String[]{"number"}));
        t.put(fn("call_contact","پیدا کردن مخاطب و باز کردن شماره‌گیر",props(new String[][]{{"name","string","نام مخاطب"}}),new String[]{"name"}));
        t.put(fn("flashlight","کنترل چراغ‌قوه",props(new String[][]{{"on","boolean","true روشن false خاموش"}}),new String[]{"on"}));
        t.put(fn("wifi_settings","باز کردن تنظیمات وای‌فای",emptyProps(),new String[]{}));
        t.put(fn("bluetooth_settings","باز کردن تنظیمات بلوتوث",emptyProps(),new String[]{}));
        t.put(fn("assistant_settings","باز کردن تنظیمات دستیار صوتی",emptyProps(),new String[]{}));
        t.put(fn("open_camera","باز کردن دوربین",emptyProps(),new String[]{}));
        t.put(fn("share_text","باز کردن پنل اشتراک متن",props(new String[][]{{"text","string","متن برای اشتراک"}}),new String[]{"text"}));
        t.put(fn("pc_command","ارسال فرمان به JARVIS روی کامپیوتر متصل",props(new String[][]{{"command","string","فرمان برای PC"}}),new String[]{"command"}));
        t.put(fn("connected_services_status","بررسی زنده اتصال Backend، Gmail و Google Calendar",emptyProps(),new String[]{}));
        t.put(fn("google_gmail_search","جستجو و خواندن خلاصه ایمیل‌های Gmail متصل",props(new String[][]{{"query","string","Gmail search query یا عبارت جستجو"},{"max_results","integer","حداکثر تعداد نتیجه"}}),new String[]{"query","max_results"}));
        t.put(fn("google_gmail_draft","ساخت Draft در Gmail؛ این ابزار ایمیل را Send نمی‌کند",props(new String[][]{{"to","string","آدرس دقیق گیرنده"},{"subject","string","موضوع"},{"body","string","متن Draft"}}),new String[]{"to","subject","body"}));
        t.put(fn("google_calendar_upcoming","خواندن رویدادهای آینده Google Calendar",props(new String[][]{{"max_results","integer","حداکثر تعداد رویداد"}}),new String[]{"max_results"}));
        t.put(fn("google_calendar_create","ثبت مستقیم رویداد در Google Calendar متصل",props(new String[][]{{"title","string","عنوان"},{"time_text","string","زمان فارسی مثل فردا ساعت 10"},{"duration_minutes","integer","مدت دقیقه"},{"location","string","مکان"},{"description","string","توضیح"}}),new String[]{"title","time_text","duration_minutes","location","description"}));
        t.put(fn("system_health","گزارش وضعیت مجوزها و سرویس‌های JARVIS",emptyProps(),new String[]{}));
        t.put(fn("create_reminder","ثبت یادآوری با epoch دقیق",props(new String[][]{{"title","string","متن یادآوری"},{"epoch_ms","integer","زمان اجرا"}}),new String[]{"title","epoch_ms"}));
        t.put(fn("create_reminder_fa","ثبت یادآوری با عبارت زمان فارسی",props(new String[][]{{"title","string","متن یادآوری"},{"time_text","string","مثال: فردا ساعت 8"}}),new String[]{"title","time_text"}));
        t.put(fn("list_reminders","نمایش یادآوری‌ها",emptyProps(),new String[]{}));
        t.put(fn("cancel_reminder","حذف یادآوری",props(new String[][]{{"id","integer","شناسه"}}),new String[]{"id"}));
        t.put(fn("create_calendar_event","باز کردن فرم رویداد تقویم سیستم؛ ثبت نهایی توسط کاربر/اپ تقویم",props(new String[][]{{"title","string","عنوان"},{"time_text","string","زمان فارسی"},{"duration_minutes","integer","مدت دقیقه"},{"location","string","مکان"},{"description","string","توضیح"}}),new String[]{"title","time_text","duration_minutes","location","description"}));
        t.put(fn("remember_fact","ذخیره واقعیت در حافظه",props(new String[][]{{"fact","string","واقعیت"}}),new String[]{"fact"}));
        return t;
    }
    private JSONObject emptyProps()throws Exception{return new JSONObject().put("type","object").put("properties",new JSONObject()).put("additionalProperties",false);}
    private JSONObject props(String[][] defs)throws Exception{JSONObject p=new JSONObject();for(String[] d:defs)p.put(d[0],new JSONObject().put("type",d[1]).put("description",d[2]));return new JSONObject().put("type","object").put("properties",p).put("additionalProperties",false);}
    private JSONObject fn(String name,String desc,JSONObject params,String[] req)throws Exception{JSONArray r=new JSONArray();for(String x:req)r.put(x);params.put("required",r);return new JSONObject().put("type","function").put("name",name).put("description",desc).put("parameters",params);}

    private String execute(String name,JSONObject a){try{switch(name){
        case "open_maps":return ui(()->actions.openMaps(a.optString("query")))?"نقشه باز شد.":"باز کردن نقشه شکست خورد.";
        case "web_search":return ui(()->actions.webSearch(a.optString("query")))?"جستجو باز شد.":"جستجو باز نشد.";
        case "open_app":{String app=a.optString("app").toLowerCase(Locale.ROOT),pkg=app.contains("whatsapp")?"com.whatsapp":app.contains("telegram")?"org.telegram.messenger":app.contains("instagram")?"com.instagram.android":"";return !pkg.isEmpty()&&ui(()->actions.openApp(pkg))?"برنامه باز شد.":"برنامه پیدا نشد.";}
        case "dial_number":return ui(()->actions.dial(a.optString("number")))?"شماره‌گیر باز شد.":"شماره‌گیر باز نشد.";
        case "call_contact":{String number=personal.findPhone(a.optString("name").trim());if("PERMISSION_REQUIRED".equals(number))return "مجوز Contacts لازم است.";if(number.isEmpty())return "مخاطب پیدا نشد.";return ui(()->actions.dial(number))?"مخاطب پیدا شد و شماره‌گیر باز شد.":"شماره‌گیر باز نشد.";}
        case "flashlight":return actions.flashlight(a.optBoolean("on"))?"چراغ‌قوه تغییر وضعیت داد.":"کنترل چراغ‌قوه شکست خورد.";
        case "wifi_settings":return ui(()->actions.settings(Settings.ACTION_WIFI_SETTINGS))?"تنظیمات وای‌فای باز شد.":"باز نشد.";
        case "bluetooth_settings":return ui(()->actions.settings(Settings.ACTION_BLUETOOTH_SETTINGS))?"تنظیمات بلوتوث باز شد.":"باز نشد.";
        case "assistant_settings":return ui(()->actions.settings("android.settings.VOICE_INPUT_SETTINGS"))?"تنظیمات دستیار باز شد.":"باز نشد.";
        case "open_camera":return ui(work::openCamera)?"دوربین باز شد.":"دوربین باز نشد.";
        case "share_text":return ui(()->work.shareText(a.optString("text")))?"پنل اشتراک باز شد.":"اشتراک باز نشد.";
        case "pc_command":return pc.send(a.optString("command"));
        case "connected_services_status":{ConnectedServices.Status s=connected.status();return "Backend: "+(s.backendReady?"READY":"NOT READY")+"\nGoogle OAuth: "+(s.googleConnected?"CONNECTED":s.googleConfigured?"READY TO CONNECT":"NOT CONFIGURED")+"\nGmail: "+(s.gmail?"CONNECTED":"NOT CONNECTED")+"\nCalendar: "+(s.calendar?"CONNECTED":"NOT CONNECTED")+"\n"+s.detail;}
        case "google_gmail_search":return connected.gmailSearch(a.optString("query"),a.optInt("max_results",8));
        case "google_gmail_draft":return connected.gmailDraft(a.optString("to"),a.optString("subject"),a.optString("body"));
        case "google_calendar_upcoming":return connected.calendarUpcoming(a.optInt("max_results",10));
        case "google_calendar_create":{PersianTimeParser.Result r=timeParser.parse(a.optString("time_text"));if(!r.ok)return "زمان رویداد را تشخیص ندادم.";return connected.calendarCreate(a.optString("title","رویداد JARVIS"),r.epochMs,a.optInt("duration_minutes",60),a.optString("location"),a.optString("description"));}
        case "system_health":return health.report()+"\n"+prod.status()+"\nPC Bridge: "+(pc.configured()?"READY":"NOT CONFIGURED");
        case "create_reminder":{long at=a.optLong("epoch_ms",0);String title=a.optString("title","یادآوری جارویس");if(at<=System.currentTimeMillis())return "زمان معتبر نیست.";return reminders.schedule(title,at)?"یادآوری ثبت شد.":"ثبت یادآوری شکست خورد.";}
        case "create_reminder_fa":{PersianTimeParser.Result r=timeParser.parse(a.optString("time_text"));if(!r.ok)return "زمان فارسی را تشخیص ندادم.";return reminders.schedule(a.optString("title","یادآوری جارویس"),r.epochMs)?"یادآوری ثبت شد.":"ثبت یادآوری شکست خورد.";}
        case "list_reminders":return reminders.summary();
        case "cancel_reminder":return reminders.cancel(a.optInt("id",-1))?"یادآوری حذف شد.":"یادآوری پیدا نشد.";
        case "create_calendar_event":{PersianTimeParser.Result r=timeParser.parse(a.optString("time_text"));if(!r.ok)return "زمان رویداد را تشخیص ندادم.";long end=r.epochMs+Math.max(1,a.optInt("duration_minutes",60))*60000L;return ui(()->personal.insertCalendarEvent(a.optString("title","رویداد JARVIS"),r.epochMs,end,a.optString("location"),a.optString("description")))?"فرم تقویم باز شد؛ ثبت نهایی هنوز Handoff به اپ تقویم است.":"تقویم باز نشد.";}
        case "remember_fact":{String f=a.optString("fact").trim();if(f.isEmpty())return "چیزی برای ذخیره نبود.";memory.remember(f);return "در حافظه ذخیره شد.";}
        default:return "ابزار ناشناخته و اجرا نشد.";
    }}catch(Exception e){return "اجرای ابزار خطا داد: "+e.getClass().getSimpleName();}}

    private Map<String,String> jsonMap(JSONObject o){Map<String,String> m=new HashMap<>();Iterator<String> it=o.keys();while(it.hasNext()){String k=it.next();m.put(k,o.optString(k));}return m;}
    private interface UiAction{boolean run();}
    private boolean ui(UiAction action)throws Exception{FutureTask<Boolean> f=new FutureTask<>(action::run);activity.runOnUiThread(f);return f.get(5,TimeUnit.SECONDS);}
    private JSONObject request(JSONObject body,String key)throws Exception{HttpURLConnection c=(HttpURLConnection)new URL(endpoint()).openConnection();c.setRequestMethod("POST");c.setDoOutput(true);c.setConnectTimeout(20000);c.setReadTimeout(90000);c.setRequestProperty("Content-Type","application/json");c.setRequestProperty("Authorization","Bearer "+key);try(OutputStream os=c.getOutputStream()){os.write(body.toString().getBytes(StandardCharsets.UTF_8));}int code=c.getResponseCode();String txt=read(code>=200&&code<300?c.getInputStream():c.getErrorStream());if(code<200||code>=300)throw new IOException("HTTP "+code+" "+txt);return new JSONObject(txt);}
    private String text(JSONObject root){StringBuilder s=new StringBuilder();JSONArray out=root.optJSONArray("output");if(out==null)return "";for(int i=0;i<out.length();i++){JSONObject item=out.optJSONObject(i);JSONArray content=item==null?null:item.optJSONArray("content");if(content==null)continue;for(int j=0;j<content.length();j++){JSONObject part=content.optJSONObject(j);if(part!=null&&"output_text".equals(part.optString("type"))){String x=part.optString("text","");if(!x.isEmpty()){if(s.length()>0)s.append('\n');s.append(x);}}}}return s.toString().trim();}
    private String prettyError(String m){if(m==null)m="";if(m.startsWith("HTTP 401"))return "مجوز AI/Backend نامعتبر است.";if(m.startsWith("HTTP 429"))return "سقف مصرف AI رسیده است.";if(m.startsWith("HTTP 403"))return "دسترسی AI/Backend مجاز نیست.";if(m.startsWith("HTTP 400"))return "درخواست Agent رد شد.";return "هسته Agent متصل نشد: "+(m.length()>160?m.substring(0,160):m);}
    private String read(InputStream is)throws Exception{if(is==null)return "";ByteArrayOutputStream o=new ByteArrayOutputStream();byte[] b=new byte[4096];int n;while((n=is.read(b))!=-1)o.write(b,0,n);return o.toString("UTF-8");}
    public void shutdown(){ex.shutdownNow();}
}
