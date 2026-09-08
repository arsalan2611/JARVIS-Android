package com.jarvis.fa;

import android.Manifest;
import android.app.Activity;
import android.content.*;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import android.provider.ContactsContract;

public class PersonalActions {
    private final Activity activity;
    public PersonalActions(Activity a){activity=a;}

    public String findPhone(String name){
        if(activity.checkSelfPermission(Manifest.permission.READ_CONTACTS)!=PackageManager.PERMISSION_GRANTED)return "PERMISSION_REQUIRED";
        String best="";
        Cursor c=null;
        try{
            Uri uri=ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
            String[] projection={ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,ContactsContract.CommonDataKinds.Phone.NUMBER};
            c=activity.getContentResolver().query(uri,projection,ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME+" LIKE ?",new String[]{"%"+name+"%"},ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME+" ASC");
            if(c!=null&&c.moveToFirst())best=c.getString(1);
        }finally{if(c!=null)c.close();}
        return best==null?"":best;
    }

    public boolean openContactDial(String name){
        String n=findPhone(name);if(n.isEmpty()||"PERMISSION_REQUIRED".equals(n))return false;
        try{activity.startActivity(new Intent(Intent.ACTION_DIAL,Uri.parse("tel:"+Uri.encode(n))));return true;}catch(Exception e){return false;}
    }

    public boolean insertCalendarEvent(String title,long startMs,long endMs,String location,String description){
        try{
            Intent i=new Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI);
            i.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME,startMs);
            i.putExtra(CalendarContract.EXTRA_EVENT_END_TIME,endMs>startMs?endMs:startMs+3600000L);
            i.putExtra(CalendarContract.Events.TITLE,title==null?"JARVIS":title);
            if(location!=null&&!location.isEmpty())i.putExtra(CalendarContract.Events.EVENT_LOCATION,location);
            if(description!=null&&!description.isEmpty())i.putExtra(CalendarContract.Events.DESCRIPTION,description);
            activity.startActivity(i);return true;
        }catch(Exception e){return false;}
    }
}
