package com.jarvis.fa;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.service.voice.VoiceInteractionSession;

public class JarvisVoiceSession extends VoiceInteractionSession {
    private final Context context;
    public JarvisVoiceSession(Context c){super(c);context=c;}
    @Override public void onShow(Bundle args,int flags){
        super.onShow(args,flags);
        Intent i=new Intent(context,MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(i);
    }
}
