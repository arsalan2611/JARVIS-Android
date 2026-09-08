package com.jarvis.fa;

import android.content.*;
import org.json.*;
import java.util.*;

public class StructuredMemory {
    private static final String PREF="jarvis_structured_memory", KEY="items";
    private final SharedPreferences p;
    public StructuredMemory(Context c){p=c.getSharedPreferences(PREF,Context.MODE_PRIVATE);}

    public synchronized void put(String category,String key,String value){
        category=clean(category);key=clean(key);value=clean(value);if(category.isEmpty()||key.isEmpty()||value.isEmpty())return;
        JSONArray a=all(),n=new JSONArray();boolean replaced=false;
        for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o==null)continue;if(category.equals(o.optString("category"))&&key.equalsIgnoreCase(o.optString("key"))){if(!replaced){JSONObject x=item(category,key,value);try{x.put("hits",o.optInt("hits",0));}catch(Exception ignored){}n.put(x);replaced=true;}}else n.put(o);}if(!replaced)n.put(item(category,key,value));while(n.length()>300)n=dropFirst(n);save(n);
    }
    public synchronized String find(String query){
        String q=norm(query);if(q.isEmpty())return "";JSONArray a=all();ArrayList<Scored> m=new ArrayList<>();
        for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o==null)continue;String hay=norm(o.optString("category")+" "+o.optString("key")+" "+o.optString("value"));int score=score(q,hay,o.optLong("updated_at",0),o.optInt("hits",0));if(score>0)m.add(new Scored(o,score));}
        Collections.sort(m,(x,y)->Integer.compare(y.score,x.score));StringBuilder s=new StringBuilder();for(int i=0;i<Math.min(8,m.size());i++){JSONObject o=m.get(i).o;s.append(o.optString("category")).append(" | ").append(o.optString("key")).append(": ").append(o.optString("value")).append('\n');try{o.put("hits",o.optInt("hits",0)+1);}catch(Exception ignored){}}
        save(a);return s.toString().trim();
    }
    public synchronized String context(){JSONArray a=all();if(a.length()==0)return "حافظه ساختاریافته خالی است.";ArrayList<Scored> l=new ArrayList<>();long now=System.currentTimeMillis();for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null){int rec=(int)Math.max(0,30-((now-o.optLong("updated_at",now))/86400000L));l.add(new Scored(o,o.optInt("hits",0)*3+rec));}}Collections.sort(l,(x,y)->Integer.compare(y.score,x.score));StringBuilder s=new StringBuilder();for(int i=0;i<Math.min(45,l.size());i++){JSONObject o=l.get(i).o;s.append("- [").append(o.optString("category")).append("] ").append(o.optString("key")).append(": ").append(o.optString("value")).append('\n');}return s.toString().trim();}
    public synchronized int size(){return all().length();}
    private int score(String q,String hay,long updated,int hits){if(hay.contains(q))return 100+hits*2;int s=0;for(String w:q.split(" "))if(w.length()>1&&hay.contains(w))s+=10;long age=(System.currentTimeMillis()-updated)/86400000L;if(age<7)s+=8;else if(age<30)s+=3;return s;}
    private JSONObject item(String c,String k,String v){JSONObject o=new JSONObject();try{o.put("category",c);o.put("key",k);o.put("value",v);o.put("updated_at",System.currentTimeMillis());o.put("hits",0);}catch(Exception ignored){}return o;}
    private JSONArray all(){try{return new JSONArray(p.getString(KEY,"[]"));}catch(Exception e){return new JSONArray();}}
    private void save(JSONArray a){p.edit().putString(KEY,a.toString()).apply();}
    private JSONArray dropFirst(JSONArray a){JSONArray n=new JSONArray();for(int i=1;i<a.length();i++)n.put(a.opt(i));return n;}
    private String clean(String s){return s==null?"":s.trim();}
    private String norm(String s){return clean(s).toLowerCase(Locale.ROOT).replace('ي','ی').replace('ك','ک').replace('‌',' ').replaceAll("\\s+"," ");}
    private static class Scored{final JSONObject o;final int score;Scored(JSONObject x,int s){o=x;score=s;}}
}
