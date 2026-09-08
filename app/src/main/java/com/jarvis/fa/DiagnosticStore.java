package com.jarvis.fa;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DiagnosticStore {
    private static final String PREF="jarvis_diagnostics";
    private static final String LOG="log";
    private static final int MAX=24000;
    private final SharedPreferences p;
    public DiagnosticStore(Context c){p=c.getSharedPreferences(PREF,Context.MODE_PRIVATE);}
    public synchronized void add(String level,String source,String message){
        String ts=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS",Locale.US).format(new Date());
        String line=ts+" | "+safe(level)+" | "+safe(source)+" | "+safe(message).replace('\n',' ')+"\n";
        String old=p.getString(LOG,"")+line;
        if(old.length()>MAX)old=old.substring(old.length()-MAX);
        p.edit().putString(LOG,old).apply();
    }
    public void info(String source,String message){add("INFO",source,message);}
    public void error(String source,Throwable t){add("ERROR",source,stack(t));}
    public String raw(){return p.getString(LOG,"");}
    public void clear(){p.edit().remove(LOG).apply();}
    public String report(){
        return "JARVIS DIAGNOSTIC REPORT\n"+
                "Android: "+Build.VERSION.RELEASE+" / API "+Build.VERSION.SDK_INT+"\n"+
                "Device: "+Build.MANUFACTURER+" "+Build.MODEL+"\n"+
                "Build fingerprint: "+Build.FINGERPRINT+"\n\n"+raw();
    }
    private String stack(Throwable t){if(t==null)return "unknown";StringWriter s=new StringWriter();t.printStackTrace(new PrintWriter(s));String x=s.toString();return x.length()>5000?x.substring(0,5000):x;}
    private String safe(String x){return x==null?"":x.replace('|','/');}
}
