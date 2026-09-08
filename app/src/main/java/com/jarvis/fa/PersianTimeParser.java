package com.jarvis.fa;

import java.util.*;
import java.util.regex.*;

public class PersianTimeParser {
    public static class Result { public final boolean ok; public final long epochMs; public final String normalized; Result(boolean o,long e,String n){ok=o;epochMs=e;normalized=n;} }

    public Result parse(String raw){
        if(raw==null)return new Result(false,0,"");
        String s=normalize(raw);
        Calendar now=Calendar.getInstance();
        Matcher m=Pattern.compile("(\\d+)\\s*دقیقه\\s*(دیگه|بعد|دیگر)?").matcher(s);
        if(m.find()){Calendar c=(Calendar)now.clone();c.add(Calendar.MINUTE,Integer.parseInt(m.group(1)));return ok(c,s);}
        m=Pattern.compile("(\\d+)\\s*ساعت\\s*(دیگه|بعد|دیگر)?").matcher(s);
        if(m.find()){Calendar c=(Calendar)now.clone();c.add(Calendar.HOUR_OF_DAY,Integer.parseInt(m.group(1)));return ok(c,s);}
        m=Pattern.compile("(\\d+)\\s*روز\\s*(دیگه|بعد|دیگر)?").matcher(s);
        if(m.find()){Calendar c=(Calendar)now.clone();c.add(Calendar.DAY_OF_YEAR,Integer.parseInt(m.group(1)));return ok(c,s);}

        Calendar c=(Calendar)now.clone(); boolean dateSpecified=false;
        if(s.contains("پس فردا")||s.contains("پس‌فردا")){c.add(Calendar.DAY_OF_YEAR,2);dateSpecified=true;}
        else if(s.contains("فردا")){c.add(Calendar.DAY_OF_YEAR,1);dateSpecified=true;}
        else if(s.contains("امروز")){dateSpecified=true;}
        else {
            int dow=weekday(s); if(dow!=-1){int cur=c.get(Calendar.DAY_OF_WEEK);int delta=(dow-cur+7)%7;if(delta==0)delta=7;c.add(Calendar.DAY_OF_YEAR,delta);dateSpecified=true;}
        }

        int hour=-1,minute=0;
        m=Pattern.compile("(?:ساعت\\s*)?(\\d{1,2})(?::(\\d{1,2}))?").matcher(s);
        while(m.find()){
            int h=Integer.parseInt(m.group(1)); if(h<=24){hour=h; minute=m.group(2)==null?0:Integer.parseInt(m.group(2));}
        }
        if(hour<0){
            if(s.contains("صبح"))hour=8; else if(s.contains("ظهر"))hour=12; else if(s.contains("بعدازظهر")||s.contains("عصر"))hour=15; else if(s.contains("شب"))hour=20;
        } else {
            if((s.contains("عصر")||s.contains("بعدازظهر")||s.contains("شب"))&&hour<12)hour+=12;
            if(s.contains("صبح")&&hour==12)hour=0;
        }
        if(hour<0)return new Result(false,0,s);
        c.set(Calendar.HOUR_OF_DAY,Math.min(hour,23));c.set(Calendar.MINUTE,Math.min(minute,59));c.set(Calendar.SECOND,0);c.set(Calendar.MILLISECOND,0);
        if(!dateSpecified && c.getTimeInMillis()<=now.getTimeInMillis())c.add(Calendar.DAY_OF_YEAR,1);
        if(c.getTimeInMillis()<=now.getTimeInMillis())return new Result(false,0,s);
        return ok(c,s);
    }

    private Result ok(Calendar c,String n){return new Result(true,c.getTimeInMillis(),n);}
    private int weekday(String s){
        if(s.contains("شنبه")&&!s.contains("یکشنبه")&&!s.contains("دوشنبه")&&!s.contains("سه شنبه")&&!s.contains("سه‌شنبه")&&!s.contains("چهارشنبه")&&!s.contains("پنجشنبه"))return Calendar.SATURDAY;
        if(s.contains("یکشنبه")||s.contains("یک شنبه"))return Calendar.SUNDAY;
        if(s.contains("دوشنبه")||s.contains("دو شنبه"))return Calendar.MONDAY;
        if(s.contains("سه شنبه")||s.contains("سه‌شنبه"))return Calendar.TUESDAY;
        if(s.contains("چهارشنبه")||s.contains("چهار شنبه"))return Calendar.WEDNESDAY;
        if(s.contains("پنجشنبه")||s.contains("پنج شنبه"))return Calendar.THURSDAY;
        if(s.contains("جمعه"))return Calendar.FRIDAY; return -1;
    }
    public static String normalize(String s){
        char[] fa={'۰','۱','۲','۳','۴','۵','۶','۷','۸','۹'};char[] ar={'٠','١','٢','٣','٤','٥','٦','٧','٨','٩'};
        for(int i=0;i<10;i++){s=s.replace(fa[i],(char)('0'+i)).replace(ar[i],(char)('0'+i));}
        return s.replace('ي','ی').replace('ك','ک').replace("‌"," ").replaceAll("\\s+"," ").trim();
    }
}
