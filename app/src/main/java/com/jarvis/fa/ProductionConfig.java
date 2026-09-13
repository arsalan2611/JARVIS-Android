package com.jarvis.fa;

import android.content.*;

public class ProductionConfig {
    private final SharedPreferences p; private final SecureStore s;
    public ProductionConfig(Context c){p=c.getSharedPreferences("jarvis_prod",Context.MODE_PRIVATE);s=new SecureStore(c);}
    public void setBackend(String url,String token){String u=url==null?"":url.trim();if(!u.isEmpty()&&!isSecureUrl(u))u="";p.edit().putString("backend_url",u).apply();if(token!=null&&!token.trim().isEmpty())s.put("backend_token",token.trim());}
    public String backendUrl(){return p.getString("backend_url","").trim();}
    public String backendToken(){return s.get("backend_token");}
    public boolean backendReady(){return isSecureUrl(backendUrl())&&!backendToken().isEmpty();}
    public boolean backendTls(){return isSecureUrl(backendUrl());}
    public void setProductionMode(boolean on){p.edit().putBoolean("production_mode",on).apply();}
    public boolean productionMode(){return p.getBoolean("production_mode",false);}
    public String status(){if(productionMode()&&backendReady())return "PRODUCTION • HTTPS Backend آماده";if(productionMode())return "PRODUCTION • Backend امن تنظیم نشده";return "DEVELOPMENT • کلید محلی فقط برای تست";}
    private boolean isSecureUrl(String u){return u!=null&&u.toLowerCase(java.util.Locale.ROOT).startsWith("https://");}
}
