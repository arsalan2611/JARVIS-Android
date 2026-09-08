package com.jarvis.fa;
import android.content.*;
public class BootReceiver extends BroadcastReceiver{
 @Override public void onReceive(Context c,Intent i){String a=i==null?null:i.getAction();if(Intent.ACTION_BOOT_COMPLETED.equals(a)||Intent.ACTION_MY_PACKAGE_REPLACED.equals(a))new ReminderEngine(c).rescheduleAll();}
}
