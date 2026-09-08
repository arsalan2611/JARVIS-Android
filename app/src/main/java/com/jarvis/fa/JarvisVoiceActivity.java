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
    private final int bg=Color.rgb(2,8,16),panel=Color.rgb(7,20,34),accent=Color.rgb(56,225,255),text=Color.WHITE,muted=Color.rgb(145,177,198),good=Color.rgb(107,235,181),warn=Color.rgb(244,194,91);
    private TextView state,transcript,modeLabel,latencyLabel; private Button listenBtn,modeBtn;
    private SpeechRecognizer sr; private TextToSpeech tts; private V12AgentCore agent; private SharedPreferences prefs;
    private final Handler main=new Handler(Looper.getMainLooper());
    private boolean listening=false,continuous=true,privateMode=false,agentBusy=false,ttsReady=false,userSpeaking=false;
    private int retryCount=0; private long speechStartMs=0,requestStartMs=0,lastResultMs=0;
    private Runnable retryRunnable;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        prefs=getSharedPreferences("jarvis_voice",MODE_PRIVATE);
        privateMode=prefs.getBoolean("private_mode",false);
        agent=new V12AgentCore(this);
        agent.setStateCallback(s->runOnUiThread(()->renderAgentState(s)));
        build();
        tts=new TextToSpeech(this,this);
        permission();
        main.postDelayed(this::listen,350);
    }

    @Override protected void onDestroy(){
        continuous=false; cancelRetry(); destroyRecognizer();
        if(tts!=null){try{tts.stop();tts.shutdown();}catch(Exception ignored){}}
        if(agent!=null)agent.shutdown();
        super.onDestroy();
    }

    private int dp(int v){return(int)(v*getResources().getDisplayMetrics().density+.5f);}
    private GradientDrawable shape(int c,int r){GradientDrawable g=new GradientDrawable();g.setColor(c);g.setCornerRadius(dp(r));g.setStroke(dp(1),Color.rgb(17,67,91));return g;}
    private TextView tv(String s,int z){TextView v=new TextView(this);v.setText(s);v.setTextColor(text);v.setTextSize(z);v.setPadding(dp(14),dp(9),dp(14),dp(9));return v;}
    private Button btn(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextColor(text);b.setBackground(shape(panel,20));return b;}

    private void build(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(18),dp(22),dp(18),dp(24));root.setBackgroundColor(bg);root.setGravity(Gravity.CENTER_HORIZONTAL);root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        TextView title=tv("J A R V I S  //  NATURAL VOICE",23);title.setTextColor(accent);title.setLetterSpacing(.10f);root.addView(title);
        state=tv("VOICE CORE • INITIALIZING",14);state.setTextColor(accent);state.setGravity(Gravity.CENTER);root.addView(state);
        modeLabel=tv("",11);modeLabel.setGravity(Gravity.CENTER);modeLabel.setTextColor(muted);root.addView(modeLabel);
        latencyLabel=tv("CONVERSATION LOOP • READY",10);latencyLabel.setGravity(Gravity.CENTER);latencyLabel.setTextColor(muted);root.addView(latencyLabel);

        TextView core=tv("◉",92);core.setTextColor(accent);core.setGravity(Gravity.CENTER);core.setOnClickListener(v->bargeIn());root.addView(core,new LinearLayout.LayoutParams(-1,dp(160)));
        transcript=tv("بگو چه کاری انجام بدهم. برای قطع پاسخ، دکمه صحبت یا هسته را لمس کن.",17);transcript.setGravity(Gravity.CENTER);transcript.setBackground(shape(panel,22));root.addView(transcript,new LinearLayout.LayoutParams(-1,dp(145)));

        listenBtn=btn("🎙 صحبت / قطع پاسخ");listenBtn.setTextColor(Color.rgb(0,20,28));listenBtn.setBackground(shape(accent,28));listenBtn.setOnClickListener(v->bargeIn());root.addView(listenBtn,new LinearLayout.LayoutParams(-1,dp(58)));
        modeBtn=btn("");modeBtn.setOnClickListener(v->{privateMode=!privateMode;prefs.edit().putBoolean("private_mode",privateMode).apply();destroyRecognizer();updateMode();applyTtsVoice();toast(privateMode?"حالت Private فعال شد":"حالت دقت فارسی فعال شد");scheduleListen(180);});root.addView(modeBtn,new LinearLayout.LayoutParams(-1,dp(50)));
        Button stop=btn("■ پایان مکالمه");stop.setOnClickListener(v->{continuous=false;cancelRetry();destroyRecognizer();if(tts!=null)tts.stop();finish();});root.addView(stop,new LinearLayout.LayoutParams(-1,dp(48)));
        TextView note=tv("Natural Voice: شنود سریع‌تر، قطع فوری پاسخ با لمس، بازگشت خودکار به شنود و بازیابی خطای هوشمند. این نسخه هنوز full-duplex سخت‌افزاری نیست.",11);note.setTextColor(muted);root.addView(note);
        setContentView(root);updateMode();
    }

    private void renderAgentState(VoiceStateMachine.State s){
        if(s==VoiceStateMachine.State.THINKING){state.setText("THINKING • در حال فکر");state.setTextColor(accent);}
        else if(s==VoiceStateMachine.State.EXECUTING){state.setText("ACTING • در حال اجرا");state.setTextColor(warn);}
        else if(s==VoiceStateMachine.State.IDLE&&!listening){state.setText("READY • آماده");state.setTextColor(good);}
    }

    private void updateMode(){
        boolean local=Build.VERSION.SDK_INT>=31&&SpeechRecognizer.isOnDeviceRecognitionAvailable(this);
        if(privateMode){modeLabel.setText(local?"PRIVATE • ON-DEVICE SPEECH AVAILABLE":"PRIVATE • ON-DEVICE SPEECH UNAVAILABLE");modeLabel.setTextColor(local?good:warn);modeBtn.setText("تغییر به دقت فارسی");}
        else{modeLabel.setText("ACCURACY • BEST AVAILABLE PERSIAN SPEECH/TTS");modeLabel.setTextColor(accent);modeBtn.setText("تغییر به Private / Local");}
    }

    private void permission(){if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},2201);}
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);if(r==2201&&g.length>0&&g[0]==PackageManager.PERMISSION_GRANTED)scheduleListen(150);}

    private void bargeIn(){
        if(!continuous)return;
        cancelRetry();
        if(tts!=null&&tts.isSpeaking()){try{tts.stop();}catch(Exception ignored){}state.setText("INTERRUPTED • گوش می‌دهم");}
        if(listening){try{sr.cancel();}catch(Exception ignored){}listening=false;}
        scheduleListen(80);
    }

    private void destroyRecognizer(){
        if(sr!=null){try{sr.cancel();sr.destroy();}catch(Exception ignored){}sr=null;}
        listening=false;userSpeaking=false;
    }

    private boolean makeRecognizer(){
        destroyRecognizer();
        if(!SpeechRecognizer.isRecognitionAvailable(this)){state.setText("Speech Service روی گوشی موجود نیست");state.setTextColor(warn);return false;}
        try{
            boolean local=privateMode&&Build.VERSION.SDK_INT>=31&&SpeechRecognizer.isOnDeviceRecognitionAvailable(this);
            if(privateMode&&!local){state.setText("Private Mode: موتور گفتار On-device در دسترس نیست");state.setTextColor(warn);return false;}
            sr=local?SpeechRecognizer.createOnDeviceSpeechRecognizer(this):SpeechRecognizer.createSpeechRecognizer(this);
            sr.setRecognitionListener(new RecognitionListener(){
                public void onReadyForSpeech(Bundle b){listening=true;userSpeaking=false;state.setText("LISTENING • گوش می‌دهم");state.setTextColor(good);listenBtn.setText("🎙 گوش می‌دهم…");}
                public void onBeginningOfSpeech(){userSpeaking=true;speechStartMs=SystemClock.elapsedRealtime();if(tts!=null&&tts.isSpeaking())try{tts.stop();}catch(Exception ignored){}state.setText("HEARING • ادامه بده");}
                public void onRmsChanged(float r){}
                public void onBufferReceived(byte[] b){}
                public void onEndOfSpeech(){userSpeaking=false;state.setText("UNDERSTANDING • در حال تشخیص");state.setTextColor(accent);}
                public void onError(int e){handleSpeechError(e);}
                public void onResults(Bundle b){
                    listening=false;userSpeaking=false;retryCount=0;lastResultMs=SystemClock.elapsedRealtime();listenBtn.setText("🎙 صحبت / قطع پاسخ");
                    ArrayList<String>x=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if(x!=null&&!x.isEmpty()){
                        String q=PersianSpeechRanker.pick(x);if(q.trim().isEmpty()){scheduleListen(250);return;}
                        transcript.setText("شما: "+q);state.setText("THINKING • JARVIS");state.setTextColor(accent);agentBusy=true;requestStartMs=SystemClock.elapsedRealtime();agent.run(q,JarvisVoiceActivity.this::answer);
                    }else scheduleListen(250);
                }
                public void onPartialResults(Bundle b){
                    ArrayList<String>x=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if(x!=null&&!x.isEmpty()){if(tts!=null&&tts.isSpeaking())try{tts.stop();}catch(Exception ignored){}transcript.setText("… "+PersianSpeechRanker.normalize(x.get(0)));}
                }
                public void onEvent(int e,Bundle b){}
            });
            return true;
        }catch(Exception e){state.setText("راه‌اندازی Speech ناموفق بود");state.setTextColor(warn);return false;}
    }

    private void handleSpeechError(int e){
        listening=false;userSpeaking=false;listenBtn.setText("🎙 صحبت / قطع پاسخ");
        boolean transientError=e==SpeechRecognizer.ERROR_NO_MATCH||e==SpeechRecognizer.ERROR_SPEECH_TIMEOUT||e==SpeechRecognizer.ERROR_CLIENT||e==SpeechRecognizer.ERROR_RECOGNIZER_BUSY||e==SpeechRecognizer.ERROR_SERVER_DISCONNECTED;
        if(e==SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS){state.setText("مجوز میکروفون لازم است");state.setTextColor(warn);permission();return;}
        if(e==SpeechRecognizer.ERROR_NETWORK||e==SpeechRecognizer.ERROR_NETWORK_TIMEOUT){state.setText("VOICE • مشکل شبکه؛ تلاش مجدد");state.setTextColor(warn);}
        else if(transientError){state.setText("READY • دوباره گوش می‌دهم");state.setTextColor(muted);}
        else{state.setText("VOICE ERROR • "+e);state.setTextColor(warn);}
        if(continuous){retryCount=Math.min(retryCount+1,5);long delay=Math.min(1400,180+(retryCount*180L));scheduleListen(delay);}
    }

    private void listen(){
        if(!continuous||agentBusy)return;
        if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){permission();return;}
        if(listening)return;
        if(tts!=null&&tts.isSpeaking())return;
        if(sr==null&&!makeRecognizer())return;
        Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"fa-IR");i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,"fa-IR");
        i.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,true);i.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,5);
        i.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,650L);
        i.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,420L);
        i.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS,350L);
        if(privateMode)i.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE,true);
        try{sr.startListening(i);}catch(Exception e){destroyRecognizer();state.setText("شروع شنود ناموفق بود");state.setTextColor(warn);scheduleListen(500);}
    }

    private void answer(String out){
        runOnUiThread(()->{
            agentBusy=false;
            long agentMs=requestStartMs>0?SystemClock.elapsedRealtime()-requestStartMs:0;
            latencyLabel.setText(agentMs>0?"AGENT • "+agentMs+" ms  |  AUTO-RELISTEN ON":"CONVERSATION LOOP • AUTO-RELISTEN ON");
            transcript.setText("JARVIS: "+out);
            if(out==null||out.trim().isEmpty()){state.setText("READY • آماده");scheduleListen(180);return;}
            state.setText("SPEAKING • برای قطع، لمس کن");state.setTextColor(accent);speak(out);
        });
    }

    private void speak(String s){
        if(tts==null||!ttsReady){scheduleListen(200);return;}
        try{tts.speak(s,TextToSpeech.QUEUE_FLUSH,null,"jarvis_voice_"+SystemClock.elapsedRealtime());}catch(Exception e){scheduleListen(250);}
    }

    private void scheduleListen(long delay){
        if(!continuous)return;cancelRetry();
        retryRunnable=()->{retryRunnable=null;if(continuous&&!agentBusy&&(tts==null||!tts.isSpeaking()))listen();};
        main.postDelayed(retryRunnable,delay);
    }
    private void cancelRetry(){if(retryRunnable!=null){main.removeCallbacks(retryRunnable);retryRunnable=null;}}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}

    private void applyTtsVoice(){
        if(tts==null)return;
        try{
            Locale fa=new Locale("fa","IR");tts.setLanguage(fa);tts.setSpeechRate(.97f);tts.setPitch(.98f);
            Voice best=null;int bestScore=Integer.MIN_VALUE;Set<Voice> voices=tts.getVoices();if(voices==null)return;
            for(Voice v:voices){if(v.getLocale()==null||!"fa".equals(v.getLocale().getLanguage()))continue;if(privateMode&&v.isNetworkConnectionRequired())continue;int score=v.getQuality()*10-v.getLatency();if(!privateMode&&!v.isNetworkConnectionRequired())score+=8;if(best==null||score>bestScore){best=v;bestScore=score;}}
            if(best!=null)tts.setVoice(best);
        }catch(Exception ignored){}
    }

    @Override public void onInit(int statusCode){
        if(statusCode!=TextToSpeech.SUCCESS){ttsReady=false;state.setText("TTS آماده نیست");state.setTextColor(warn);scheduleListen(200);return;}
        ttsReady=true;applyTtsVoice();
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener(){
            public void onStart(String id){runOnUiThread(()->{state.setText("SPEAKING • برای قطع، لمس کن");state.setTextColor(accent);});}
            public void onDone(String id){runOnUiThread(()->{state.setText("READY • آماده");state.setTextColor(good);scheduleListen(140);});}
            public void onError(String id){runOnUiThread(()->{state.setText("READY • پاسخ صوتی قطع شد");scheduleListen(180);});}
            @Override public void onStop(String id,boolean interrupted){runOnUiThread(()->{if(interrupted){state.setText("INTERRUPTED • گوش می‌دهم");scheduleListen(80);}});}
        });
    }
}
