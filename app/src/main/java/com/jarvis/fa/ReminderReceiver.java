package com.jarvis.fa;
import android.app.*;import android.content.*;import android.os.*;
public class ReminderReceiver extends BroadcastReceiver{
 @Override public void onReceive(Context c,Intent i){String title=i.getStringExtra("title");if(title==null)title="یادآوری جارویس";NotificationManager nm=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);if(Build.VERSION.SDK_INT>=26){NotificationChannel ch=new NotificationChannel("jarvis_reminders","یادآوری‌های جارویس",NotificationManager.IMPORTANCE_HIGH);nm.createNotificationChannel(ch);}Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(c,"jarvis_reminders"):new Notification.Builder(c);b.setContentTitle("JARVIS").setContentText(title).setSmallIcon(android.R.drawable.ic_dialog_info).setAutoCancel(true);nm.notify((int)(System.currentTimeMillis()%100000),b.build());}
}
