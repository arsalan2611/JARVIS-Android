package com.jarvis.fa;

import android.app.*;
import android.graphics.Color;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

public class RoutineCenterActivity extends Activity {
    private AutomationEngine engine; private Switch morning,inbox,computer; private TextView status;
    @Override public void onCreate(Bundle b){super.onCreate(b);engine=new AutomationEngine(this);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(36,36,36,36);root.setBackgroundColor(Color.rgb(3,8,15));root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);TextView title=new TextView(this);title.setText("JARVIS • ROUTINES");title.setTextColor(Color.rgb(69,226,255));title.setTextSize(22);root.addView(title);morning=make("Morning Brief");inbox=make("Inbox Check Prompt");computer=make("PC Check Prompt");root.addView(morning);root.addView(inbox);root.addView(computer);Button save=new Button(this);save.setText("ذخیره");save.setOnClickListener(v->save());root.addView(save);status=new TextView(this);status.setTextColor(Color.WHITE);status.setTextSize(13);root.addView(status);setContentView(root);refresh();}
    private Switch make(String s){Switch x=new Switch(this);x.setText(s);x.setTextColor(Color.WHITE);x.setPadding(0,20,0,20);return x;}
    private void refresh(){morning.setChecked(engine.enabled(AutomationEngine.TYPE_MORNING));inbox.setChecked(engine.enabled(AutomationEngine.TYPE_GMAIL));computer.setChecked(engine.enabled(AutomationEngine.TYPE_PC));status.setText(engine.summary());}
    private void save(){engine.setMorningTime(8,0);engine.setEnabled(AutomationEngine.TYPE_MORNING,morning.isChecked());engine.setEnabled(AutomationEngine.TYPE_GMAIL,inbox.isChecked());engine.setEnabled(AutomationEngine.TYPE_PC,computer.isChecked());Toast.makeText(this,"ذخیره شد",Toast.LENGTH_SHORT).show();refresh();}
}
