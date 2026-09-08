package com.jarvis.fa;

import java.util.*;

public class AgentPolicy {
    public enum Risk { SAFE, CONFIRM, BLOCKED }
    public static class Decision {
        public final Risk risk; public final String reason;
        Decision(Risk r,String s){risk=r;reason=s;}
    }
    public Decision assess(String tool, Map<String,String> args){
        String t=tool==null?"":tool.toLowerCase(Locale.ROOT);
        if(t.contains("delete")||t.contains("factory")||t.contains("root")||t.contains("install_apk"))return new Decision(Risk.BLOCKED,"این عملیات در JARVIS 1.0 مجاز نیست.");
        if(t.contains("send_email")||t.contains("send_message")||t.contains("purchase")||t.contains("payment"))return new Decision(Risk.CONFIRM,"این عملیات اثر بیرونی دارد و نیاز به تأیید نهایی کاربر دارد.");
        return new Decision(Risk.SAFE,"");
    }
}
