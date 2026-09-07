package com.jarvis.fa;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public class SecureStore {
    private static final String ALIAS="jarvis_v7_master_key";
    private static final String PREFS="jarvis_secure";
    private final SharedPreferences p;
    public SecureStore(Context c){p=c.getSharedPreferences(PREFS,Context.MODE_PRIVATE);}
    private SecretKey key() throws Exception{
        KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);
        if(!ks.containsAlias(ALIAS)){
            KeyGenerator kg=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
            kg.init(new KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
            kg.generateKey();
        }
        return ((KeyStore.SecretKeyEntry)ks.getEntry(ALIAS,null)).getSecretKey();
    }
    public void put(String name,String value){
        try{
            Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,key());
            byte[] enc=c.doFinal((value==null?"":value).getBytes(StandardCharsets.UTF_8));
            p.edit().putString(name+"_iv",Base64.encodeToString(c.getIV(),Base64.NO_WRAP))
                    .putString(name+"_ct",Base64.encodeToString(enc,Base64.NO_WRAP)).apply();
        }catch(Exception e){throw new IllegalStateException("Secure storage unavailable",e);}
    }
    public String get(String name){
        try{
            String iv=p.getString(name+"_iv",null),ct=p.getString(name+"_ct",null);if(iv==null||ct==null)return "";
            Cipher c=Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,Base64.decode(iv,Base64.NO_WRAP)));
            return new String(c.doFinal(Base64.decode(ct,Base64.NO_WRAP)),StandardCharsets.UTF_8);
        }catch(Exception e){return "";}
    }
    public void remove(String name){p.edit().remove(name+"_iv").remove(name+"_ct").apply();}
}
