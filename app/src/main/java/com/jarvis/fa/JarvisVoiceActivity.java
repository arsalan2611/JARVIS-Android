package com.jarvis.fa;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.speech.*;
import android.speech.tts.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public class JarvisVoiceActivity extends Activity implements TextToSpeech.OnInitListener {
    private final int bg=Color.rgb(2,8,16), panel=Color.rgb(7,20,34), accent=Color.rgb(56,225,255), text=Color.WHITE, muted=Color.rgb(145,177,198), good=Color.rgb(107,235,181);
    private TextView state, transcript, modeLabel; private Button listenBtn,modeBtn; private SpeechRecognizer sr; private TextToSpeech tts; private V12AgentCore agent; private boolean listening=false,continuous=true,privateMode=false; private SharedPreferences prefs;

    @Override public void onCreate(Bundle b){super.onCreate(b);prefs=getSharedPreferences("jarvis_voice",MODE_PRIVATE);privateMode=prefs.getBoolean("private_mode",false);agent=new V12AgentCore(this);build();tts=new TextToSpeech(this,this);permission();new Handler(Looper.getMainLooper()).postDelayed(this::listen,500);}
    @Override protected void onDestroy(){super.onDestroy();continuous=false;if(sr!=null){try{sr.cancel();sr.destroy();}catch(Exception ignored){}}if(tts!=null){tts.stop();tts.shutdown();}if(agent!=null)agent.shutdown();}

    private int dp(int v){return(int)(v*getResources().getDisplayMetrics().density+.5f);} private GradientDrawable shape(int c,int r){GradientDrawable g=new GradientDrawable();g.setColor(c);g.setCornerRadius(dp(r));g.setStroke(dp(1),Color.rgb(17,67,91));return g;} private TextView tv(String s,int z){TextView v=new TextView(this);v.setText(s);v.setTextColor(text);v.setTextSize(z);v.setPadding(dp(14),dp(10),dp(14),dp(10));return v;} private Button btn(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextColor(text);b.setBackground(shape(panel,20));return b;}
    private void build(){LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(18),dp(24),dp(18),dp(28));root.setBackgroundColor(bg);root.setGravity(Gravity.CENTER_HORIZONTAL);root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);TextView title=tv("J A R V I S  //  VOICE",25);title.setTextColor(accent);title.setLetterSpacing(.12f);root.addView(title);state=tv("VOICE CORE • INITIALIZING",14);state.setTextColor(accent);state.setGravity(Gravity.CENTER);root.addView(state);modeLabel=tv("",12);modeLabel.setGravity(Gravity.CENTER);modeLabel.setTextColor(muted);root.addView(modeLabel);TextView core=tv("◉",96);core.setTextColor(accent);core.setGravity(Gravity.CENTER);root.addView(core,new LinearLayout.LayoutParams(-1,dp(180)));transcript=tv("بگو چه کاری انجام بدهم.",18);transcript.setGravity(Gravity.CENTER);transcript.setBackground(shape(panel,22));root.addView(transcript,new LinearLayout.LayoutParams(-1,dp(150)));listenBtn=btn("🎙 گوش دادن");listenBtn.setTextColor(Color.rgb(0,20,28));listenBtn.setBackground(shape(accent,28));listenBtn.setOnClickListener(v->{if(tts!=null&&tts.isSpeaking())tts.stop();listen();});root.addView(listenBtn,new LinearLayout.LayoutParams(-1,dp(58)));modeBtn=btn("");modeBtn.setOnClickListener(v->{privateMode=!privateMode;prefs.edit().putBoolean("private_mode",privateMode).apply();destroyRecognizer();updateMode();applyTtsVoice();toast(privateMode?"حالت Private فعال شد":"حالت دقت فارسی فعال شد");});root.addView(modeBtn,new LinearLayout.LayoutParams(-1,dp(52)));Button stop=btn("■ پایان مکالمه");stop.setOnClickListener(v->{continuous=false;if(sr!=null)try{sr.cancel();}catch(Exception ignored){}if(tts!=null)tts.stop();finish();});root.addView(stop,new LinearLayout.LayoutParams(-1,dp(50)));TextView note=tv("Accuracy: بهترین Speech/TTS فارسی موجود روی گوشی انتخاب می‌شود و ممکن است از شبکه استفاده کند. Private: فقط در صورت وجود موتور on-device و Voice آفلاین استفاده می‌شود.",11);note.setTextColor(muted);root.addView(note);setContentView(root);updateMode();}
    private void updateMode(){boolean local=Build.VERSION.SDK_INT>=31&&SpeechRecognizer.isOnDeviceRecognitionAvailable(this);if(privateMode){modeLabel.setText(local?"PRIVACY MODE • ON-DEVICE AVAILABLE":"PRIVACY MODE • ON-DEVICE NOT AVAILABLE");modeLabel.setTextColor(local?good:muted);modeBtn.setText("تغییر به حالت دقت فارسی");}else{modeLabel.setText("PERSIAN ACCURACY MODE • BEST AVAILABLE SPEECH/TTS");modeLabel.setTextColor(accent);modeBtn.setText("تغییر به حالت Private / Local");}}
    private void permission(){if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},2201);}
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);if(r==2201&&g.length>0&&g[0]==PackageManager.PERMISSION_GRANTED)listen();}
    private void destroyRecognizer(){if(sr!=null){try{sr.cancel();sr.destroy();}catch(Exception ignored){}sr=null;}listening=false;}
    private boolean makeRecognizer(){destroyRecognizer();if(!SpeechRecognizer.isRecognitionAvailable(this)){state.setText("Speech Service روی گوشی موجود نیست");return false;}try{boolean local=privateMode&&Build.VERSION.SDK_INT>=31&&SpeechRecognizer.isOnDeviceRecognitionAvailable(this);if(privateMode&&!local){state.setText("Private Mode: موتور گفتار On-device فارسی روی این گوشی در دسترس نیست");return false;}sr=local?SpeechRecognizer.createOnDeviceSpeechRecognizer(this):SpeechRecognizer.createSpeechRecognizer(this);sr.setRecognitionListener(new RecognitionListener(){public void onReadyForSpeech(Bundle b){listening=true;state.setText("LISTENING • گوش می‌دهم");}public void onBeginningOfSpeech(){}public void onRmsChanged(float r){}public void onBufferReceived(byte[] b){}public void onEndOfSpeech(){state.setText("THINKING • در حال پردازش");}public void onError(int e){listening=false;state.setText("VOICE ERROR • "+e);if(continuous&&(e==SpeechRecognizer.ERROR_NO_MATCH||e==SpeechRecognizer.ERROR_SPEECH_TIMEOUT||e==SpeechRecognizer.ERROR_CLIENT))new Handler(Looper.getMainLooper()).postDelayed(JarvisVoiceActivity.this::listen,700);}public void onResults(Bundle b){listening=false;ArrayList<String>x=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);if(x!=null&&!x.isEmpty()){String q=PersianSpeechRanker.pick(x);transcript.setText("شما: "+q);state.setText("THINKING • JARVIS");agent.run(q,JarvisVoiceActivity.this::answer);}else retry();}public void onPartialResults(Bundle b){ArrayList<String>x=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);if(x!=null&&!x.isEmpty())transcript.setText("… "+PersianSpeechRanker.normalize(x.get(0)));}public void onEvent(int e,Bundle b){}});return true;}catch(Exception e){state.setText("راه‌اندازی Speech ناموفق بود");return false;}}
    private void listen(){if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){permission();return;}if(listening)return;if(tts!=null&&tts.isSpeaking())tts.stop();if(sr==null&&!makeRecognizer())return;Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"fa-IR");i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,"fa-IR");i.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,true);i.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,5);if(privateMode)i.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE,true);try{sr.startListening(i);}catch(Exception e){destroyRecognizer();state.setText("شروع شنود ناموفق بود");retry();}}
    private void answer(String out){runOnUiThread(()->{transcript.setText("JARVIS: "+out);state.setText("SPEAKING • در حال پاسخ");speak(out);});}
    private void speak(String s){if(tts==null){retry();return;}tts.speak(s,TextToSpeech.QUEUE_FLUSH,null,"jarvis_voice");}
    private void retry(){if(continuous)new Handler(Looper.getMainLooper()).postDelayed(this::listen,600);}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}

    private void applyTtsVoice(){
        if(tts==null)return;
        try{
            Locale fa=new Locale("fa","IR");tts.setLanguage(fa);tts.setSpeechRate(.94f);tts.setPitch(.98f);
            Voice best=null;int bestScore=Integer.MIN_VALUE;
            Set<Voice> voices=tts.getVoices();if(voices==null)return;
            for(Voice v:voices){
                if(v.getLocale()==null||!"fa".equals(v.getLocale().getLanguage()))continue;
                if(privateMode&&v.isNetworkConnectionRequired())continue;
                int score=v.getQuality()*10-v.getLatency();
                if(!privateMode&&!v.isNetworkConnectionRequired())score+=10;
                if(best==null||score>bestScore){best=v;bestScore=score;}
            }
            if(best!=null)tts.setVoice(best);
        }catch(Exception ignored){}
    }
    @Override public void onInit(int statusCode){if(statusCode!=TextToSpeech.SUCCESS){state.setText("TTS آماده نیست");return;}applyTtsVoice();tts.setOnUtteranceProgressListener(new UtteranceProgressListener(){public void onStart(String id){}public void onDone(String id){runOnUiThread(()->{state.setText("READY • آماده");retry();});}public void onError(String id){runOnUiThread(JarvisVoiceActivity.this::retry);}});}
}
