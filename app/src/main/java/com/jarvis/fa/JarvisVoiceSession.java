package com.jarvis.fa;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.service.voice.VoiceInteractionSession;

public class JarvisVoiceSession extends VoiceInteractionSession {
    private final Context context;
    public JarvisVoiceSession(Context c){super(c);context=c;}
    @Override public void onCreate(){super.onCreate();setKeepAwake(true);}
    @Override public void onShow(Bundle args,int flags){
        super.onShow(args,flags);
        Intent i=new Intent(context,MainActivity.class)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra("auto_listen",true)
            .putExtra("from_system_assist",true);
        context.startActivity(i);
        hide();
    }
    @Override public void onHide(){setKeepAwake(false);super.onHide();}
}
