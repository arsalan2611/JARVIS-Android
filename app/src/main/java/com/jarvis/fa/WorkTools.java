package com.jarvis.fa;

import android.app.Activity;
import android.content.*;
import android.net.Uri;
import android.provider.Settings;

public class WorkTools {
    private final Activity a;
    public WorkTools(Activity activity){a=activity;}

    public boolean openFiles(String mime){
        try{
            Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType(mime==null||mime.trim().isEmpty()?"*/*":mime);
            a.startActivity(i);
            return true;
        }catch(Exception e){return false;}
    }

    public boolean openDownloads(){
        try{
            Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("*/*");
            a.startActivity(i);
            return true;
        }catch(Exception e){return false;}
    }

    public boolean composeEmail(String to,String subject,String body){
        try{
            Intent i=new Intent(Intent.ACTION_SENDTO);
            i.setData(Uri.parse("mailto:"+(to==null?"":Uri.encode(to.trim()))));
            i.putExtra(Intent.EXTRA_SUBJECT,subject==null?"":subject);
            i.putExtra(Intent.EXTRA_TEXT,body==null?"":body);
            a.startActivity(i);
            return true;
        }catch(Exception e){return false;}
    }

    public boolean openNotificationSettings(){
        try{
            Intent i=new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            i.putExtra(Settings.EXTRA_APP_PACKAGE,a.getPackageName());
            a.startActivity(i);
            return true;
        }catch(Exception e){return false;}
    }
}
