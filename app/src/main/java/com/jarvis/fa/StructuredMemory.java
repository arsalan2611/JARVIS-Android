package com.jarvis.fa;

import android.content.*;
import org.json.*;
import java.util.*;

public class StructuredMemory {
    private static final String PREF="jarvis_structured_memory";
    private static final String KEY="items";
    private final SharedPreferences p;

    public StructuredMemory(Context c){p=c.getSharedPreferences(PREF,Context.MODE_PRIVATE);}

    public synchronized void put(String category,String key,String value){
        category=clean(category); key=clean(key); value=value==null?"":value.trim();
        if(category.isEmpty()||key.isEmpty()||value.isEmpty())return;
        JSONArray a=all(); JSONArray n=new JSONArray(); boolean replaced=false;
        for(int i=0;i<a.length();i++){
            JSONObject o=a.optJSONObject(i); if(o==null)continue;
            if(category.equals(o.optString("category"))&&key.equalsIgnoreCase(o.optString("key"))){
                if(!replaced){n.put(item(category,key,value));replaced=true;}
            }else n.put(o);
        }
        if(!replaced)n.put(item(category,key,value));
        while(n.length()>120)n=dropFirst(n);
        p.edit().putString(KEY,n.toString()).apply();
    }

    public synchronized String find(String query){
        query=clean(query).toLowerCase(Locale.ROOT); if(query.isEmpty())return "";
        JSONArray a=all(); StringBuilder s=new StringBuilder();
        for(int i=a.length()-1;i>=0;i--){JSONObject o=a.optJSONObject(i);if(o==null)continue;String hay=(o.optString("category")+" "+o.optString("key")+" "+o.optString("value")).toLowerCase(Locale.ROOT);if(hay.contains(query)){s.append(o.optString("category")).append(" | ").append(o.optString("key")).append(": ").append(o.optString("value")).append('\n');if(s.length()>1200)break;}}
        return s.toString().trim();
    }

    public synchronized String context(){
        JSONArray a=all(); if(a.length()==0)return "حافظه ساختاریافته خالی است.";
        StringBuilder s=new StringBuilder(); int start=Math.max(0,a.length()-35);
        for(int i=start;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null)s.append("- [").append(o.optString("category")).append("] ").append(o.optString("key")).append(": ").append(o.optString("value")).append('\n');}
        return s.toString().trim();
    }

    private JSONObject item(String c,String k,String v){JSONObject o=new JSONObject();try{o.put("category",c);o.put("key",k);o.put("value",v);o.put("updated_at",System.currentTimeMillis());}catch(Exception ignored){}return o;}
    private JSONArray all(){try{return new JSONArray(p.getString(KEY,"[]"));}catch(Exception e){return new JSONArray();}}
    private JSONArray dropFirst(JSONArray a){JSONArray n=new JSONArray();for(int i=1;i<a.length();i++)n.put(a.opt(i));return n;}
    private String clean(String s){return s==null?"":s.trim();}
}
