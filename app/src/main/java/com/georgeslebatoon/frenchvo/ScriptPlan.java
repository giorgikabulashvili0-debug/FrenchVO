package com.georgeslebatoon.frenchvo;
import java.util.*;
import java.util.regex.*;

public final class ScriptPlan {
 public static final class Turn {
  public final String text, style;
  Turn(String text,String style){this.text=text;this.style=style;}
 }
 private static final Map<String,String> STYLES=new HashMap<>();
 static {
  STYLES.put("naturel","natural conversational storytelling with varied intonation");
  STYLES.put("énergique","energetic and enthusiastic, lively but not shouting");
  STYLES.put("energique",STYLES.get("énergique"));
  STYLES.put("mystère","intriguing and mysterious, suspenseful");
  STYLES.put("mystere",STYLES.get("mystère"));
  STYLES.put("chuchote","whispering softly");
  STYLES.put("surpris","surprised and curious");
  STYLES.put("amusé","amused, playful delivery with a smile");
  STYLES.put("amuse",STYLES.get("amusé"));
  STYLES.put("sérieux","serious documentary narration");
  STYLES.put("serieux",STYLES.get("sérieux"));
  STYLES.put("calme","calm and reassuring");
 }
 public static List<Turn> parse(String script,String base) {
  List<Turn> turns=new ArrayList<>();
  Matcher matcher=Pattern.compile("\\[([^\\]]+)\\]").matcher(script);
  int start=0; String style=base;
  while(matcher.find()){
   add(turns,script.substring(start,matcher.start()),style);
   String tag=matcher.group(1).trim().toLowerCase(Locale.ROOT);
   if(tag.equals("pause")){
    if(!turns.isEmpty()){Turn previous=turns.remove(turns.size()-1);turns.add(new Turn(previous.text+" <short pause>",previous.style));}
   } else if(STYLES.containsKey(tag)) style=base+". "+STYLES.get(tag);
   else throw new IllegalArgumentException("Balise inconnue : ["+tag+"]. Utilisez les balises proposées dans l’aide.");
   start=matcher.end();
  }
  add(turns,script.substring(start),style);
  if(turns.isEmpty())throw new IllegalArgumentException("Ajoutez du texte à lire.");
  return turns;
 }
 private static void add(List<Turn> turns,String text,String style){
  text=text.trim();if(!text.isEmpty())turns.add(new Turn(text,style));
 }
 public static List<List<Turn>> batches(List<Turn> turns){
  List<List<Turn>> batches=new ArrayList<>();List<Turn> batch=new ArrayList<>();int total=0;
  for(Turn turn:turns){
   String remaining=turn.text;
   while(!remaining.isEmpty()){
    int end=Math.min(remaining.length(),1200);
    if(end<remaining.length()){
     int boundary=-1;
     for(int i=end-1;i>=200;i--)if(".!?\n".indexOf(remaining.charAt(i))>=0){boundary=i+1;break;}
     if(boundary<0)boundary=remaining.lastIndexOf(' ',end-1);
     if(boundary>0)end=boundary;
     if(end>0&&Character.isHighSurrogate(remaining.charAt(end-1)))end--;
    }
    String part=remaining.substring(0,end).trim();remaining=remaining.substring(end).trim();
    if(part.isEmpty())continue;
    if(total+part.length()>1200&&!batch.isEmpty()){batches.add(batch);batch=new ArrayList<>();total=0;}
    batch.add(new Turn(part,turn.style));total+=part.length();
   }
  }
  if(!batch.isEmpty())batches.add(batch);return batches;
 }
}
