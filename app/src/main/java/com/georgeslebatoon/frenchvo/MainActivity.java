package com.georgeslebatoon.frenchvo;
import android.app.*;
import android.content.*;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.widget.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
 private EditText script,key,directions;private Spinner voice,style;private TextView status;
 private Button generate,listen,save;private volatile int job=0;private volatile boolean destroyed=false;
 private final ExecutorService worker=Executors.newSingleThreadExecutor();private final GeminiVoice api=new GeminiVoice();
 private MediaPlayer player;private File finished;
 private static final String[] VOICES={"Charon","Puck","Kore","Aoede"};
 private static final String[] STYLES={"natural conversational storytelling, varied intonation, warm and engaging","energetic and enthusiastic, lively but not shouting","intriguing mysterious storytelling with suspense","amused playful storytelling with a smile","serious documentary narration","calm and reassuring delivery","whispering softly"};
 @Override public void onCreate(Bundle state){
  super.onCreate(state);ScrollView scroll=new ScrollView(this);LinearLayout layout=new LinearLayout(this);layout.setOrientation(1);layout.setPadding(32,24,32,30);scroll.addView(layout);setContentView(scroll);
  label(layout,"French VO — Expressif",24);
  label(layout,"Voix et émotions avec Gemini • Internet requis • Quotas Google. Le texte est envoyé à Google lorsque vous générez.",14);
  Button setup=button(layout,"1. Obtenir une clé Google AI Studio");setup.setOnClickListener(v->startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://aistudio.google.com/apikey"))));
  key=edit(layout,"Collez votre clé API ici (elle reste dans cette session)",1);key.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
  label(layout,"Utilisez un projet Google sans facturation pour rester dans le quota gratuit. La clé n’est ni intégrée à l’APK ni enregistrée sur le téléphone.",14);
  label(layout,"Voix",17);voice=spinner(layout,new String[]{"Charon — homme","Puck — homme","Kore — femme","Aoede — femme"});
  label(layout,"Style de narration",17);style=spinner(layout,new String[]{"Naturel / YouTube","Énergique","Mystère","Amusé","Documentaire","Calme","Chuchoté"});
  directions=edit(layout,"Direction supplémentaire (facultatif) : ton curieux, accent français, rythme vivant…",2);
  script=edit(layout,"Collez le texte français. Exemple : [mystère] Tu entends ce bruit ? [pause] [amusé] C’est juste ton frigo !",9);
  label(layout,"Balises : [naturel] [énergique] [mystère] [chuchote] [surpris] [amusé] [sérieux] [calme] [pause]. Elles guident la voix et ne sont pas envoyées comme mots à lire. Maximum : 30 000 caractères. Gardez l’app ouverte.",14);
  generate=button(layout,"2. Générer la voix expressive");generate.setOnClickListener(v->generate());
  listen=button(layout,"3. Écouter");listen.setEnabled(false);listen.setOnClickListener(v->play());
  save=button(layout,"4. Enregistrer WAV");save.setEnabled(false);save.setOnClickListener(v->export());
  Button stop=button(layout,"Stop");stop.setOnClickListener(v->{job++;api.cancel();stopPlayer();busy(false);status.setText("Arrêté. Les parties incomplètes ne sont pas enregistrées.");});
  status=label(layout,"Commencez avec 2–3 phrases pour vérifier votre clé et choisir une voix. Les émotions sont des instructions au modèle, pas une garantie de résultat identique à chaque génération.",15);
 }
 private TextView label(LinearLayout l,String value,int size){TextView v=new TextView(this);v.setText(value);v.setTextSize(size);v.setPadding(0,14,0,8);l.addView(v);return v;}
 private EditText edit(LinearLayout l,String hint,int lines){EditText v=new EditText(this);v.setHint(hint);v.setMinLines(lines);v.setGravity(48);v.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE);l.addView(v);return v;}
 private Button button(LinearLayout l,String text){Button b=new Button(this);b.setText(text);b.setAllCaps(false);l.addView(b);return b;}
 private Spinner spinner(LinearLayout l,String[] values){Spinner s=new Spinner(this);s.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,values));l.addView(s);return s;}
 private void busy(boolean busy){generate.setEnabled(!busy);voice.setEnabled(!busy);style.setEnabled(!busy);listen.setEnabled(!busy&&finished!=null);save.setEnabled(!busy&&finished!=null);getWindow().getDecorView().setKeepScreenOn(busy);}
 private void generate(){
  String text=script.getText().toString().trim(),apiKey=key.getText().toString().trim();
  if(apiKey.isEmpty()){status.setText("Obtenez une clé Google AI Studio, puis collez-la dans le premier champ.");return;}
  if(text.length()>30000){status.setText("Maximum : 30 000 caractères, espaces inclus.");return;}
  final List<List<ScriptPlan.Turn>> batches;
  try{batches=ScriptPlan.batches(ScriptPlan.parse(text,STYLES[style.getSelectedItemPosition()]+". "+directions.getText().toString()));}catch(Exception e){status.setText(e.getMessage());return;}
  job++;api.cancel();stopPlayer();final int current=job;final String chosen=VOICES[voice.getSelectedItemPosition()];busy(true);
  status.setText("Génération : "+batches.size()+" partie(s). Chaque partie consomme le quota Google.");
  worker.execute(()->{
   File result=new File(getCacheDir(),"Expressif_"+current+".wav");
   try(WaveFile wav=new WaveFile(result)){
    for(int i=0;i<batches.size();i++){
     if(current!=job)throw new CancellationException();final int number=i+1;
     runOnUiThread(()->{if(current==job)status.setText("Génération "+number+"/"+batches.size()+"…");});
     byte[] bytes=api.generate(apiKey,chosen,batches.get(i));if(current!=job)throw new CancellationException();wav.append(bytes);
    }
    wav.finish();
    runOnUiThread(()->{if(current==job&&!destroyed){if(finished!=null)finished.delete();finished=result;busy(false);status.setText("Prêt : écoutez puis enregistrez votre WAV.");}else result.delete();});
   }catch(Exception e){result.delete();runOnUiThread(()->{if(current==job&&!destroyed){busy(false);status.setText("Génération interrompue : "+e.getMessage());}});}
  });
 }
 private void stopPlayer(){if(player!=null){player.release();player=null;}}
 private void play(){if(finished==null)return;stopPlayer();try{player=new MediaPlayer();player.setDataSource(finished.getPath());player.prepare();player.setOnCompletionListener(p->{stopPlayer();status.setText("Lecture terminée.");});player.start();status.setText("Lecture…");}catch(Exception e){stopPlayer();status.setText("Erreur de lecture : "+e.getMessage());}}
 private void export(){
  final File file=finished;if(file==null)return;busy(true);final int current=job;
  worker.execute(()->{
   Uri uri=null;
   try{
    ContentValues values=new ContentValues();String name="FrenchVO_Expressif_"+System.currentTimeMillis()+".wav";
    values.put(MediaStore.Downloads.DISPLAY_NAME,name);values.put(MediaStore.Downloads.MIME_TYPE,"audio/wav");values.put(MediaStore.Downloads.RELATIVE_PATH,"Download/FrenchVO");values.put(MediaStore.Downloads.IS_PENDING,1);
    uri=getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,values);if(uri==null)throw new IOException("Stockage indisponible");
    try(InputStream in=new FileInputStream(file);OutputStream out=getContentResolver().openOutputStream(uri)){if(out==null)throw new IOException("Stockage indisponible");byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1){if(current!=job)throw new CancellationException();out.write(buf,0,n);}}
    values.clear();values.put(MediaStore.Downloads.IS_PENDING,0);getContentResolver().update(uri,values,null,null);
    runOnUiThread(()->{if(!destroyed){busy(false);status.setText("Enregistré : Downloads/FrenchVO/"+name);}});
   }catch(Exception e){if(uri!=null)getContentResolver().delete(uri,null,null);runOnUiThread(()->{if(current==job&&!destroyed){busy(false);status.setText("Erreur de sauvegarde : "+e.getMessage());}});}
  });
 }
 @Override protected void onDestroy(){destroyed=true;job++;api.cancel();stopPlayer();worker.shutdown();super.onDestroy();}
}
