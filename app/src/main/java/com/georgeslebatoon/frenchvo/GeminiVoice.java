package com.georgeslebatoon.frenchvo;
import org.json.*;
import android.util.Base64;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CancellationException;

public final class GeminiVoice {
 private volatile HttpURLConnection active;
 public void cancel(){HttpURLConnection connection=active;if(connection!=null)connection.disconnect();}
 public byte[] generate(String key,String voice,List<ScriptPlan.Turn> turns)throws Exception{
  JSONArray parts=new JSONArray();
  for(ScriptPlan.Turn turn:turns)parts.put(new JSONObject().put("text",turn.text).put("speech_metadata",new JSONObject().put("style","Speak in French from France. Keep a consistent narrator voice. "+turn.style)));
  JSONObject request=new JSONObject().put("contents",new JSONArray().put(new JSONObject().put("role","user").put("parts",parts)))
   .put("generationConfig",new JSONObject().put("responseModalities",new JSONArray().put("AUDIO")).put("speechConfig",new JSONObject().put("voiceConfig",new JSONObject().put("voice",voice))));
  HttpURLConnection connection=(HttpURLConnection)new URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash-tts:generateContent").openConnection();active=connection;
  try{
   connection.setRequestMethod("POST");connection.setConnectTimeout(20000);connection.setReadTimeout(180000);connection.setDoOutput(true);
   connection.setRequestProperty("Content-Type","application/json; charset=UTF-8");connection.setRequestProperty("x-goog-api-key",key);
   try(OutputStream out=connection.getOutputStream()){out.write(request.toString().getBytes(StandardCharsets.UTF_8));}
   int code=connection.getResponseCode();
   if(code!=200){
    if(code==429)throw new IOException("Quota Google atteint. Réessayez plus tard ou utilisez un texte plus court. Aucun passage automatique vers un modèle payant.");
    if(code==400||code==401||code==403)throw new IOException("Google refuse la requête ("+code+"). Vérifiez la clé API et l’accès au modèle TTS dans AI Studio.");
    if(code==404)throw new IOException("Le modèle TTS n’est pas disponible pour cette clé.");
    throw new IOException("Erreur Google "+code+". Réessayez plus tard.");
   }
   String body;
   try(InputStream in=connection.getInputStream();ByteArrayOutputStream bytes=new ByteArrayOutputStream()){
    byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1){bytes.write(buf,0,n);if(bytes.size()>50000000)throw new IOException("Réponse trop volumineuse.");}body=bytes.toString("UTF-8");
   }
   JSONObject response=new JSONObject(body);JSONArray candidates=response.optJSONArray("candidates");
   if(candidates==null||candidates.length()==0)throw new IOException("Google n’a pas renvoyé d’audio. Essayez un court texte neutre.");
   JSONArray audioParts=candidates.getJSONObject(0).getJSONObject("content").getJSONArray("parts");
   for(int i=0;i<audioParts.length();i++){
    JSONObject inline=audioParts.getJSONObject(i).optJSONObject("inlineData");
    if(inline!=null&&inline.has("data"))return Base64.decode(inline.getString("data"),Base64.DEFAULT);
   }
   throw new IOException("Réponse sans audio.");
  }finally{connection.disconnect();if(active==connection)active=null;}
 }
}
