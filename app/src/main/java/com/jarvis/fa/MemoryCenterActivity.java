package com.jarvis.fa;

import android.app.*;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

public class MemoryCenterActivity extends Activity {
    private final int bg=Color.rgb(3,8,15),surface=Color.rgb(8,17,29),surface2=Color.rgb(11,24,39),stroke=Color.rgb(28,63,82),accent=Color.rgb(69,226,255),text=Color.WHITE,muted=Color.rgb(139,165,183),danger=Color.rgb(255,116,116);
    private StructuredMemory memory; private TextView stats,results; private EditText search,value;
    @Override public void onCreate(Bundle b){super.onCreate(b);memory=new StructuredMemory(this);build();refresh();}
    private int dp(int v){return(int)(v*getResources().getDisplayMetrics().density+.5f);} private GradientDrawable shape(int c,int r){GradientDrawable g=new GradientDrawable();g.setColor(c);g.setCornerRadius(dp(r));g.setStroke(dp(1),stroke);return g;}
    private TextView tv(String s,int z){TextView v=new TextView(this);v.setText(s);v.setTextSize(z);v.setTextColor(text);v.setPadding(dp(12),dp(9),dp(12),dp(9));return v;}
    private Button btn(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextColor(text);b.setTextSize(13);b.setBackground(shape(surface2,18));return b;}
    private EditText field(String hint){EditText e=new EditText(this);e.setHint(hint);e.setHintTextColor(muted);e.setTextColor(text);e.setTextSize(13);e.setSingleLine(false);e.setPadding(dp(12),dp(8),dp(12),dp(8));e.setBackground(shape(surface2,14));return e;}
    private void build(){ScrollView sc=new ScrollView(this);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(18),dp(20),dp(18),dp(28));root.setBackgroundColor(bg);root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        TextView title=tv("JARVIS MEMORY",26);title.setTextColor(accent);title.setLetterSpacing(.10f);root.addView(title);TextView sub=tv("حافظه شخصی، پروژه‌ای و کاری — قابل جستجو و کنترل",12);sub.setTextColor(muted);root.addView(sub);
        stats=tv("",13);stats.setBackground(shape(surface,20));root.addView(stats);
        search=field("جستجو در حافظه؛ مثال: پروژه جارویس یا آدرس کارخانه");root.addView(search,new LinearLayout.LayoutParams(-1,dp(62)));LinearLayout sr=new LinearLayout(this);sr.setOrientation(LinearLayout.HORIZONTAL);Button find=btn("جستجو");find.setOnClickListener(v->doSearch());sr.addView(find,new LinearLayout.LayoutParams(0,dp(48),1));Button recent=btn("آخرین حافظه‌ها");recent.setOnClickListener(v->{results.setText(memory.recent(20));});sr.addView(recent,new LinearLayout.LayoutParams(0,dp(48),1));root.addView(sr);
        results=tv("",13);results.setTextColor(text);results.setBackground(shape(surface,18));root.addView(results);
        TextView addLabel=tv("افزودن یا اصلاح حافظه",13);addLabel.setTextColor(accent);root.addView(addLabel);value=field("مثال: پروژه JARVIS الان در Build 36 است");root.addView(value,new LinearLayout.LayoutParams(-1,dp(82)));Button save=btn("ذخیره هوشمند");save.setOnClickListener(v->saveValue());root.addView(save,new LinearLayout.LayoutParams(-1,dp(48)));
        Button forget=btn("فراموش کردن موارد مرتبط با عبارت جستجو");forget.setTextColor(danger);forget.setOnClickListener(v->confirmForget());root.addView(forget,new LinearLayout.LayoutParams(-1,dp(48)));
        TextView note=tv("حذف حافظه فقط روی موارد مرتبط با عبارت جستجو انجام می‌شود. پاک‌سازی کورکورانه کل حافظه در این صفحه وجود ندارد.",11);note.setTextColor(muted);root.addView(note);sc.addView(root);setContentView(sc);}
    private void refresh(){stats.setText(memory.stats());results.setText(memory.recent(12));}
    private void doSearch(){String q=search.getText().toString().trim();results.setText(q.isEmpty()?memory.recent(20):memory.find(q,20,false));if(results.getText().toString().trim().isEmpty())results.setText("مورد مرتبطی پیدا نشد.");}
    private void saveValue(){String v=value.getText().toString().trim();if(v.isEmpty()){toast("متن حافظه خالی است.");return;}String c=memory.inferCategory(v);String k=v.length()<=52?v:v.substring(0,52);memory.put(c,k,v);value.setText("");toast("در حافظه "+c+" ذخیره شد.");refresh();}
    private void confirmForget(){String q=search.getText().toString().trim();if(q.length()<2){toast("اول عبارت مشخصی برای فراموش کردن وارد کن.");return;}new AlertDialog.Builder(this).setTitle("فراموش کردن حافظه").setMessage("موارد مرتبط با «"+q+"» حذف شوند؟").setNegativeButton("لغو",null).setPositiveButton("حذف",(d,w)->{int n=memory.forget(q);toast(n+" مورد حذف شد.");refresh();}).show();}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
}
