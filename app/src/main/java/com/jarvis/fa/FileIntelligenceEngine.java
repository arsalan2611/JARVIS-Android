package com.jarvis.fa;

import android.content.Context;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.database.Cursor;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;
import java.util.regex.*;

public class FileIntelligenceEngine {
    public static class Result {
        public boolean ok;
        public String name="";
        public String type="";
        public String summary="";
        public String extracted="";
        public int rows;
        public int columns;
        public int sheets;
        public String error="";
    }

    private final Context context;
    public FileIntelligenceEngine(Context c){context=c.getApplicationContext();}

    public Result analyze(Uri uri){
        Result r=new Result();
        try{
            r.name=fileName(uri);
            String lower=r.name.toLowerCase(Locale.ROOT);
            if(lower.endsWith(".xlsx")) return xlsx(uri,r);
            if(lower.endsWith(".csv")) return delimited(uri,r,",");
            if(lower.endsWith(".tsv")) return delimited(uri,r,"\t");
            if(lower.endsWith(".txt")||lower.endsWith(".json")||lower.endsWith(".xml")||lower.endsWith(".md")||lower.endsWith(".log")) return text(uri,r);
            if(lower.endsWith(".pdf")){
                r.type="PDF";r.ok=false;r.error="برای PDF در این نسخه استخراج متنی محلی فعال نیست. فایل را با JARVIS Vision یا Backend تحلیل کن.";return r;
            }
            r.type="FILE";r.ok=false;r.error="این فرمت هنوز توسط File Intelligence پشتیبانی نمی‌شود.";return r;
        }catch(Exception e){r.ok=false;r.error="خطا در خواندن فایل: "+safe(e.getMessage());return r;}
    }

    private Result text(Uri uri,Result r)throws Exception{
        r.type="TEXT";
        String s=readLimited(uri,180000);
        r.extracted=trimForAgent(s,50000);
        String[] lines=s.split("\\R",-1);
        r.rows=lines.length;
        r.columns=1;
        r.summary="نوع: متن\nخطوط: "+r.rows+"\nکاراکترها: "+s.length()+"\n\nپیش‌نمایش:\n"+preview(s,1400);
        r.ok=true;return r;
    }

    private Result delimited(Uri uri,Result r,String delimiter)throws Exception{
        r.type=",".equals(delimiter)?"CSV":"TSV";
        String s=readLimited(uri,220000);
        String[] lines=s.split("\\R");
        int maxCols=0,nonEmpty=0;
        for(String line:lines){if(line.trim().isEmpty())continue;nonEmpty++;maxCols=Math.max(maxCols,splitDelimited(line,delimiter.charAt(0)).size());}
        r.rows=nonEmpty;r.columns=maxCols;r.extracted=trimForAgent(s,50000);r.ok=true;
        r.summary="نوع: "+r.type+"\nردیف‌ها: "+r.rows+"\nحداکثر ستون‌ها: "+r.columns+"\n\nپیش‌نمایش:\n"+preview(s,1600);
        return r;
    }

    private Result xlsx(Uri uri,Result r)throws Exception{
        r.type="XLSX";
        byte[] bytes=readBytesLimited(uri,14*1024*1024);
        Map<String,byte[]> entries=new HashMap<>();
        ZipInputStream zin=new ZipInputStream(new ByteArrayInputStream(bytes));
        ZipEntry e;while((e=zin.getNextEntry())!=null){
            String n=e.getName();
            if(n.equals("xl/sharedStrings.xml")||n.matches("xl/worksheets/sheet\\d+\\.xml")||n.equals("xl/workbook.xml")) entries.put(n,readEntry(zin,5*1024*1024));
        }
        List<String> shared=parseShared(entries.get("xl/sharedStrings.xml"));
        List<String> sheetNames=parseSheetNames(entries.get("xl/workbook.xml"));
        List<String> sheetKeys=new ArrayList<>();
        for(String k:entries.keySet())if(k.matches("xl/worksheets/sheet\\d+\\.xml"))sheetKeys.add(k);
        Collections.sort(sheetKeys,(a,b)->Integer.compare(sheetNo(a),sheetNo(b)));
        r.sheets=sheetKeys.size();
        StringBuilder out=new StringBuilder();int totalRows=0,maxCols=0;
        for(int i=0;i<sheetKeys.size();i++){
            String name=i<sheetNames.size()?sheetNames.get(i):("Sheet "+(i+1));
            SheetData sd=parseSheet(entries.get(sheetKeys.get(i)),shared,220);
            totalRows+=sd.rows;maxCols=Math.max(maxCols,sd.columns);
            out.append("\n### ").append(name).append("\n").append(sd.text);
            if(out.length()>52000)break;
        }
        r.rows=totalRows;r.columns=maxCols;r.extracted=trimForAgent(out.toString(),50000);r.ok=true;
        r.summary="نوع: Excel XLSX\nشیت‌ها: "+r.sheets+"\nردیف‌های استخراج‌شده: "+r.rows+"\nحداکثر ستون مشاهده‌شده: "+r.columns+"\n\nپیش‌نمایش:\n"+preview(out.toString(),1800);
        return r;
    }

    private static class SheetData{int rows,columns;String text="";}
    private SheetData parseSheet(byte[] xml,List<String> shared,int maxRows){
        SheetData sd=new SheetData();if(xml==null)return sd;String s=new String(xml,StandardCharsets.UTF_8);
        Pattern rowP=Pattern.compile("<row[^>]*>(.*?)</row>",Pattern.DOTALL);Matcher rm=rowP.matcher(s);StringBuilder out=new StringBuilder();
        while(rm.find()&&sd.rows<maxRows){String row=rm.group(1);Pattern cP=Pattern.compile("<c([^>]*)>(.*?)</c>",Pattern.DOTALL);Matcher cm=cP.matcher(row);List<String> vals=new ArrayList<>();
            while(cm.find()){String attrs=cm.group(1),body=cm.group(2);String v=tag(body,"v");String inline=tag(body,"t");String value=!inline.isEmpty()?decodeXml(inline):decodeXml(v);if(attrs.contains("t=\"s\"")&&!v.isEmpty())try{int idx=Integer.parseInt(v);if(idx>=0&&idx<shared.size())value=shared.get(idx);}catch(Exception ignored){}vals.add(value);}
            if(!vals.isEmpty()){sd.rows++;sd.columns=Math.max(sd.columns,vals.size());out.append(String.join(" | ",vals)).append('\n');}
        }sd.text=out.toString();return sd;
    }

    private List<String> parseShared(byte[] xml){List<String> out=new ArrayList<>();if(xml==null)return out;String s=new String(xml,StandardCharsets.UTF_8);Matcher m=Pattern.compile("<si>(.*?)</si>",Pattern.DOTALL).matcher(s);while(m.find()){String block=m.group(1);Matcher tm=Pattern.compile("<t[^>]*>(.*?)</t>",Pattern.DOTALL).matcher(block);StringBuilder x=new StringBuilder();while(tm.find())x.append(decodeXml(tm.group(1)));out.add(x.toString());}return out;}
    private List<String> parseSheetNames(byte[] xml){List<String> out=new ArrayList<>();if(xml==null)return out;Matcher m=Pattern.compile("<sheet[^>]*name=\"([^\"]+)\"",Pattern.DOTALL).matcher(new String(xml,StandardCharsets.UTF_8));while(m.find())out.add(decodeXml(m.group(1)));return out;}
    private int sheetNo(String s){Matcher m=Pattern.compile("sheet(\\d+)\\.xml").matcher(s);return m.find()?Integer.parseInt(m.group(1)):9999;}
    private String tag(String body,String tag){Matcher m=Pattern.compile("<"+tag+"[^>]*>(.*?)</"+tag+">",Pattern.DOTALL).matcher(body);return m.find()?m.group(1):"";}
    private String decodeXml(String s){return s.replace("&lt;","<").replace("&gt;",">").replace("&quot;","\"").replace("&apos;","'").replace("&amp;","&");}
    private byte[] readEntry(InputStream in,int max)throws IOException{ByteArrayOutputStream o=new ByteArrayOutputStream();byte[] b=new byte[8192];int n,total=0;while((n=in.read(b))!=-1){total+=n;if(total>max)break;o.write(b,0,n);}return o.toByteArray();}
    private String fileName(Uri uri){String n="file";Cursor c=null;try{c=context.getContentResolver().query(uri,null,null,null,null);if(c!=null&&c.moveToFirst()){int i=c.getColumnIndex(OpenableColumns.DISPLAY_NAME);if(i>=0)n=c.getString(i);}}finally{if(c!=null)c.close();}return n==null?"file":n;}
    private String readLimited(Uri uri,int max)throws IOException{return new String(readBytesLimited(uri,max),StandardCharsets.UTF_8);}
    private byte[] readBytesLimited(Uri uri,int max)throws IOException{InputStream in=context.getContentResolver().openInputStream(uri);if(in==null)throw new IOException("stream unavailable");try{ByteArrayOutputStream o=new ByteArrayOutputStream();byte[] b=new byte[8192];int n,total=0;while((n=in.read(b))!=-1){total+=n;if(total>max)throw new IOException("فایل برای تحلیل محلی بیش از حد بزرگ است");o.write(b,0,n);}return o.toByteArray();}finally{in.close();}}
    private List<String> splitDelimited(String line,char d){List<String> out=new ArrayList<>();StringBuilder x=new StringBuilder();boolean q=false;for(int i=0;i<line.length();i++){char c=line.charAt(i);if(c=='\"'){q=!q;continue;}if(c==d&&!q){out.add(x.toString());x.setLength(0);}else x.append(c);}out.add(x.toString());return out;}
    private String preview(String s,int max){String x=s==null?"":s.trim();return x.length()<=max?x:x.substring(0,max)+"\n…";}
    private String trimForAgent(String s,int max){String x=s==null?"":s;return x.length()<=max?x:x.substring(0,max);}
    private String safe(String s){return s==null||s.trim().isEmpty()?"نامشخص":s.trim();}
}
