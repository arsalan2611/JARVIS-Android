package com.jarvis.fa;

import java.util.*;

public final class PersianSpeechRanker {
    private static final String[] COMMAND_HINTS={"جارویس","یادم","یادآوری","باز کن","ببند","زنگ","تماس","نقشه","فایل","اکسل","پی دی اف","دوربین","چراغ","امروز","فردا","ساعت","ایمیل","پیام","کامپیوتر","وضعیت","بررسی","جستجو"};
    private PersianSpeechRanker(){}
    public static String pick(List<String> candidates){
        if(candidates==null||candidates.isEmpty())return "";
        String best="";double bestScore=-1e9;
        for(int i=0;i<candidates.size();i++){
            String raw=candidates.get(i);String s=normalize(raw);if(s.isEmpty())continue;
            double score=0;
            int fa=0,latin=0,digits=0;
            for(int j=0;j<s.length();j++){char c=s.charAt(j);if((c>='\u0600'&&c<='\u06ff')||c=='‌')fa++;else if((c>='a'&&c<='z')||(c>='A'&&c<='Z'))latin++;else if(Character.isDigit(c))digits++;}
            score+=fa*1.4-latin*.7+Math.min(digits,8)*.15;
            for(String h:COMMAND_HINTS)if(s.contains(h))score+=5;
            if(s.length()>=3&&s.length()<=140)score+=3;
            score-=i*.35;
            if(score>bestScore){bestScore=score;best=s;}
        }
        return best.isEmpty()?normalize(candidates.get(0)):best;
    }
    public static String normalize(String s){return s==null?"":s.replace('ي','ی').replace('ك','ک').replace('ۀ','ه').replace('ة','ه').replaceAll("\\s+"," ").trim();}
}
