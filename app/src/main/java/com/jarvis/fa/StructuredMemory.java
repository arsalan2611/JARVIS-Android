package com.jarvis.fa;

import android.content.*;
import org.json.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class StructuredMemory {
    private static final String PREF="jarvis_structured_memory", KEY="items";
    private static final int MAX_ITEMS=500;
    private final SharedPreferences p;
    public StructuredMemory(Context c){p=c.getSharedPreferences(PREF,Context.MODE_PRIVATE);}

    public synchronized void put(String category,String key,String value){
        category=clean(category);key=clean(key);value=clean(value);if(category.isEmpty()||key.isEmpty()||value.isEmpty())return;
        JSONArray a=all(),n=new JSONArray();boolean replaced=false;
        for(int i=0;i<a.length();i++){
            JSONObject o=a.optJSONObject(i);if(o==null)continue;
            if(category.equals(o.optString("category"))&&key.equalsIgnoreCase(o.optString("key"))){
                if(!replaced){JSONObject x=item(category,key,value);try{x.put("hits",o.optInt("hits",0));x.put("created_at",o.optLong("created_at",System.currentTimeMillis()));}catch(Exception ignored){}n.put(x);replaced=true;}
            }else n.put(o);
        }
        if(!replaced)n.put(item(category,key,value));while(n.length()>MAX_ITEMS)n=dropFirst(n);save(n);
    }

    public synchronized String find(String query){return find(query,8,true);}
    public synchronized String find(String query,int limit,boolean countHit){
        String q=norm(query);if(q.isEmpty())return "";JSONArray a=all();ArrayList<Scored> m=matches(a,q);
        StringBuilder s=new StringBuilder();for(int i=0;i<Math.min(Math.max(1,limit),m.size());i++){
            JSONObject o=m.get(i).o;s.append(o.optString("category")).append(" | ").append(o.optString("key")).append(": ").append(o.optString("value")).append('\n');
            if(countHit)try{o.put("hits",o.optInt("hits",0)+1);o.put("last_used_at",System.currentTimeMillis());}catch(Exception ignored){}
        }
        if(countHit)save(a);return s.toString().trim();
    }

    public synchronized int forget(String query){
        String q=norm(query);if(q.isEmpty())return 0;JSONArray a=all(),n=new JSONArray();int removed=0;
        for(int i=0;i<a.length();i++){
            JSONObject o=a.optJSONObject(i);if(o==null)continue;
            String hay=norm(o.optString("category")+" "+o.optString("key")+" "+o.optString("value"));
            boolean exact=hay.contains(q);int wordHits=0,total=0;for(String w:q.split(" "))if(w.length()>1){total++;if(hay.contains(w))wordHits++;}
            boolean strong=total>0&&wordHits>=Math.max(1,(int)Math.ceil(total*.7));
            if(exact||strong)removed++;else n.put(o);
        }
        if(removed>0)save(n);return removed;
    }

    public synchronized String recent(int limit){
        JSONArray a=all();ArrayList<JSONObject> items=new ArrayList<>();for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null)items.add(o);}
        Collections.sort(items,(x,y)->Long.compare(y.optLong("updated_at",0),x.optLong("updated_at",0)));
        StringBuilder s=new StringBuilder();SimpleDateFormat f=new SimpleDateFormat("MM/dd HH:mm",Locale.US);
        for(int i=0;i<Math.min(Math.max(1,limit),items.size());i++){JSONObject o=items.get(i);if(i>0)s.append("\n\n");s.append("[").append(o.optString("category")).append("] ").append(o.optString("key")).append("\n").append(o.optString("value")).append("\n").append(f.format(new Date(o.optLong("updated_at",System.currentTimeMillis()))));}
        return s.length()==0?"حافظه هنوز خالی است.":s.toString();
    }

    public synchronized String stats(){
        JSONArray a=all();Map<String,Integer> m=new LinkedHashMap<>();String[] order={"person","project","place","preference","work","general"};for(String k:order)m.put(k,0);
        for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o==null)continue;String c=o.optString("category","general");m.put(c,m.containsKey(c)?m.get(c)+1:1);}
        return "کل حافظه: "+a.length()+"\nافراد: "+m.get("person")+" • پروژه‌ها: "+m.get("project")+" • مکان‌ها: "+m.get("place")+"\nترجیحات: "+m.get("preference")+" • کاری: "+m.get("work")+" • عمومی: "+m.get("general");
    }

    public synchronized String context(){
        JSONArray a=all();if(a.length()==0)return "حافظه ساختاریافته خالی است.";ArrayList<Scored> l=new ArrayList<>();long now=System.currentTimeMillis();
        for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null){int rec=(int)Math.max(0,30-((now-o.optLong("updated_at",now))/86400000L));l.add(new Scored(o,o.optInt("hits",0)*3+rec));}}
        Collections.sort(l,(x,y)->Integer.compare(y.score,x.score));StringBuilder s=new StringBuilder();for(int i=0;i<Math.min(36,l.size());i++){JSONObject o=l.get(i).o;s.append("- [").append(o.optString("category")).append("] ").append(o.optString("key")).append(": ").append(o.optString("value")).append('\n');}return s.toString().trim();
    }

    public synchronized int size(){return all().length();}

    public String inferCategory(String value){
        String n=norm(value);
        if(n.matches(".*(اسمش|نامش|شماره|تلفن|همسر|پدر|مادر|دوست|همکار|مدیر|آقای|خانم).*"))return "person";
        if(n.matches(".*(پروژه|پروژه‌|نسخه|build|بیلد|فاز|تسک|کار روی).*"))return "project";
        if(n.matches(".*(آدرس|خیابان|کوچه|محله|شهر|مکان|دفتر|کارخانه|خانه).*"))return "place";
        if(n.matches(".*(دوست دارم|ترجیح میدم|ترجیح می‌دم|نمیخوام|نمی‌خوام|همیشه|سبک من|مورد علاقه).*"))return "preference";
        if(n.matches(".*(شرکت|خرید|فروش|انبار|سفارش|تامین|تأمین|پیمانکار|فاکتور|مدیرعامل|جلسه کاری).*"))return "work";
        return "general";
    }

    private ArrayList<Scored> matches(JSONArray a,String q){ArrayList<Scored> m=new ArrayList<>();for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o==null)continue;String hay=norm(o.optString("category")+" "+o.optString("key")+" "+o.optString("value"));int score=score(q,hay,o.optLong("updated_at",0),o.optInt("hits",0));if(score>0)m.add(new Scored(o,score));}Collections.sort(m,(x,y)->Integer.compare(y.score,x.score));return m;}
    private int score(String q,String hay,long updated,int hits){if(hay.contains(q))return 120+hits*2;int s=0,total=0;for(String w:q.split(" "))if(w.length()>1){total++;if(hay.contains(w))s+=14;}if(total>0&&s==0)return 0;long age=(System.currentTimeMillis()-updated)/86400000L;if(age<7)s+=8;else if(age<30)s+=3;return s;}
    private JSONObject item(String c,String k,String v){JSONObject o=new JSONObject();try{long now=System.currentTimeMillis();o.put("category",c);o.put("key",k);o.put("value",v);o.put("created_at",now);o.put("updated_at",now);o.put("last_used_at",0);o.put("hits",0);}catch(Exception ignored){}return o;}
    private JSONArray all(){try{return new JSONArray(p.getString(KEY,"[]"));}catch(Exception e){return new JSONArray();}}
    private void save(JSONArray a){p.edit().putString(KEY,a.toString()).apply();}
    private JSONArray dropFirst(JSONArray a){JSONArray n=new JSONArray();for(int i=1;i<a.length();i++)n.put(a.opt(i));return n;}
    private String clean(String s){return s==null?"":s.trim();}
    private String norm(String s){return clean(s).toLowerCase(Locale.ROOT).replace('ي','ی').replace('ك','ک').replace('‌',' ').replaceAll("\\s+"," ");}
    private static class Scored{final JSONObject o;final int score;Scored(JSONObject x,int s){o=x;score=s;}}
}
