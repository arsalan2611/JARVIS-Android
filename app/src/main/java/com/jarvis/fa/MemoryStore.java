package com.jarvis.fa;

import android.content.*;
import org.json.*;
import java.util.*;

public class MemoryStore {
    private final SharedPreferences p;
    private static final String FACTS = "v6Facts";
    private static final String HISTORY = "v6History";

    public MemoryStore(Context c){ p=c.getSharedPreferences("jarvis_data",Context.MODE_PRIVATE); }

    public void remember(String fact){
        fact=fact==null?"":fact.trim();
        if(fact.isEmpty()) return;
        JSONArray a=array(FACTS);
        for(int i=0;i<a.length();i++) if(fact.equals(a.optString(i))) return;
        a.put(fact);
        while(a.length()>40) a=dropFirst(a);
        p.edit().putString(FACTS,a.toString()).apply();
    }

    public String facts(){
        JSONArray a=array(FACTS);
        if(a.length()==0) return "حافظه دائمی خالی است.";
        StringBuilder s=new StringBuilder();
        for(int i=0;i<a.length();i++) s.append("- ").append(a.optString(i)).append('\n');
        return s.toString().trim();
    }

    public void addTurn(String role,String text){
        if(text==null||text.trim().isEmpty()) return;
        JSONArray a=array(HISTORY);
        JSONObject o=new JSONObject();
        try{o.put("role",role);o.put("text",text.trim());a.put(o);}catch(Exception ignored){}
        while(a.length()>12) a=dropFirst(a);
        p.edit().putString(HISTORY,a.toString()).apply();
    }

    public String context(){
        StringBuilder s=new StringBuilder();
        s.append("حافظه دائمی:\n").append(facts()).append("\n\nمکالمات اخیر:\n");
        JSONArray a=array(HISTORY);
        for(int i=0;i<a.length();i++){
            JSONObject o=a.optJSONObject(i);
            if(o!=null) s.append(o.optString("role")).append(": ").append(o.optString("text")).append('\n');
        }
        return s.toString().trim();
    }

    public void clearHistory(){ p.edit().remove(HISTORY).apply(); }

    private JSONArray array(String key){ try{return new JSONArray(p.getString(key,"[]"));}catch(Exception e){return new JSONArray();} }
    private JSONArray dropFirst(JSONArray a){ JSONArray n=new JSONArray(); for(int i=1;i<a.length();i++) n.put(a.opt(i)); return n; }
}
