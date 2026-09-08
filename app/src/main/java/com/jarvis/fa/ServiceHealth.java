package com.jarvis.fa;

import android.app.Activity;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

public class ServiceHealth {
    public interface Callback { void onResult(Report report); }
    public static class Report {
        public final boolean backendConfigured,backendOnline,pcConfigured,pcOnline;
        public final long backendMs,pcMs;
        public final String backendDetail,pcDetail;
        Report(boolean bc,boolean bo,long bm,String bd,boolean pc,boolean po,long pm,String pd){backendConfigured=bc;backendOnline=bo;backendMs=bm;backendDetail=bd;pcConfigured=pc;pcOnline=po;pcMs=pm;pcDetail=pd;}
        public String summary(){
            String b=backendConfigured?(backendOnline?"✓ Backend ONLINE • "+backendMs+" ms":"✕ Backend OFFLINE • "+backendDetail):"• Backend NOT CONFIGURED";
            String p=pcConfigured?(pcOnline?"✓ PC Bridge ONLINE • "+pcMs+" ms":"✕ PC Bridge OFFLINE • "+pcDetail):"• PC Bridge NOT CONFIGURED";
            return b+"\n"+p;
        }
    }
    private static class Probe { boolean ok; long ms; String detail; Probe(boolean o,long m,String d){ok=o;ms=m;detail=d;} }
    private final Activity activity;
    private final ProductionConfig prod;
    private final PcBridge pc;
    private final ExecutorService ex=Executors.newFixedThreadPool(2);
    public ServiceHealth(Activity a){activity=a;prod=new ProductionConfig(a);pc=new PcBridge(a);}
    public void check(Callback cb){
        ex.submit(()->{
            boolean bc=prod.productionMode()&&prod.backendReady();
            boolean pcConfigured=pc.configured();
            Probe bp=bc?probe(backendHealthUrl(),prod.backendToken()):new Probe(false,0,"not configured");
            Probe pp=pcConfigured?probe(pcHealthUrl(),""):new Probe(false,0,"not configured");
            Report r=new Report(bc,bp.ok,bp.ms,bp.detail,pcConfigured,pp.ok,pp.ms,pp.detail);
            activity.runOnUiThread(()->cb.onResult(r));
        });
    }
    private String backendHealthUrl(){
        String u=prod.backendUrl().trim().replaceAll("/$","");
        if(u.endsWith("/v1/responses"))u=u.substring(0,u.length()-"/v1/responses".length());
        else if(u.endsWith("/responses"))u=u.substring(0,u.length()-"/responses".length());
        return u+"/health";
    }
    private String pcHealthUrl(){return pc.endpoint().trim().replaceAll("/$","")+"/health";}
    private Probe probe(String url,String token){
        long start=System.currentTimeMillis();HttpURLConnection c=null;
        try{
            c=(HttpURLConnection)new URL(url).openConnection();c.setRequestMethod("GET");c.setConnectTimeout(3500);c.setReadTimeout(5000);c.setUseCaches(false);c.setRequestProperty("Accept","application/json,text/plain");if(token!=null&&!token.isEmpty())c.setRequestProperty("Authorization","Bearer "+token);
            int code=c.getResponseCode();long ms=System.currentTimeMillis()-start;InputStream in=code>=200&&code<300?c.getInputStream():c.getErrorStream();String body=read(in);if(code>=200&&code<300)return new Probe(true,ms,body.isEmpty()?"HTTP "+code:shortText(body));return new Probe(false,ms,"HTTP "+code+(body.isEmpty()?"":" • "+shortText(body)));
        }catch(Exception e){return new Probe(false,System.currentTimeMillis()-start,e.getClass().getSimpleName());}finally{if(c!=null)c.disconnect();}
    }
    private String read(InputStream in)throws Exception{if(in==null)return "";ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] x=new byte[2048];for(int n;(n=in.read(x))>0&&b.size()<4096;)b.write(x,0,n);return b.toString(StandardCharsets.UTF_8.name()).trim();}
    private String shortText(String s){s=s.replaceAll("\\s+"," ").trim();return s.length()<=120?s:s.substring(0,120);}
    public void shutdown(){ex.shutdownNow();}
}
