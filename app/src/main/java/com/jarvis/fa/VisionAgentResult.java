package com.jarvis.fa;

import org.json.JSONObject;

public class VisionAgentResult {
    public String summary="";
    public String extractedText="";
    public String entityType="unknown";
    public int confidence=0;
    public String suggestedAction="none";
    public String actionPayload="";
    public String rationale="";

    public static VisionAgentResult fromJson(String raw){
        VisionAgentResult r=new VisionAgentResult();
        try{
            String s=raw==null?"":raw.trim();
            if(s.startsWith("```")){int a=s.indexOf('{');int b=s.lastIndexOf('}');if(a>=0&&b>a)s=s.substring(a,b+1);}
            JSONObject o=new JSONObject(s);
            r.summary=o.optString("summary","");
            r.extractedText=o.optString("extracted_text","");
            r.entityType=o.optString("entity_type","unknown");
            r.confidence=Math.max(0,Math.min(100,o.optInt("confidence",0)));
            r.suggestedAction=o.optString("suggested_action","none");
            Object p=o.opt("action_payload");r.actionPayload=p==null?"":(p instanceof String?(String)p:p.toString());
            r.rationale=o.optString("rationale","");
        }catch(Exception e){r.summary=raw==null?"":raw;r.suggestedAction="none";}
        return r;
    }

    public String display(){
        StringBuilder s=new StringBuilder();
        if(!summary.isEmpty())s.append(summary);
        if(!extractedText.isEmpty())s.append("\n\nمتن/اطلاعات استخراج‌شده:\n").append(extractedText);
        s.append("\n\nنوع: ").append(entityType).append(" • اطمینان: ").append(confidence).append("٪");
        if(!rationale.isEmpty())s.append("\nدلیل پیشنهاد: ").append(rationale);
        return s.toString().trim();
    }
}
