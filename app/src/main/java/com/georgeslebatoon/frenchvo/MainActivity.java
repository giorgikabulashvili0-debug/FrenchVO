package com.georgeslebatoon.frenchvo;

import android.app.Activity;
import android.content.*;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import android.speech.tts.*;
import android.widget.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
    private TextToSpeech tts;
    private TomEngine tom;
    private EditText input;
    private Spinner voices;
    private TextView status, speedLabel;
    private Button preview, save;
    private float rate=1f;
    private boolean androidReady=false, destroyed=false;
    private final List<Voice> systemVoices=new ArrayList<>();
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private volatile int generation=0;
    private MediaPlayer player;
    private File systemFile;
    private String systemId;
    private boolean systemSave;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b); setContentView(R.layout.activity_main);
        input=findViewById(R.id.textInput); voices=findViewById(R.id.voiceSpinner);
        status=findViewById(R.id.statusText); speedLabel=findViewById(R.id.speedLabel);
        preview=findViewById(R.id.previewButton); save=findViewById(R.id.saveButton);
        tom=new TomEngine(getApplicationContext());
        setVoices(); status.setText("Tom — voix masculine intégrée. Maximum : 30 000 caractères. Gardez l’application ouverte pendant la génération.");
        ((SeekBar)findViewById(R.id.speedSeek)).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar s,int p,boolean u){rate=.7f+p/100f;speedLabel.setText(String.format(Locale.FRANCE,"Vitesse : %.2f×",rate));}
            public void onStartTrackingTouch(SeekBar s){} public void onStopTrackingTouch(SeekBar s){}
        });
        preview.setOnClickListener(v->start(false)); save.setOnClickListener(v->start(true));
        findViewById(R.id.stopButton).setOnClickListener(v->{cancel();busy(false);status.setText("Arrêté.");});
        tts=new TextToSpeech(this, code->{
            if(code==TextToSpeech.SUCCESS) runOnUiThread(()->{
                androidReady=true;
                if(tts.getVoices()!=null) for(Voice voice:tts.getVoices())
                    if("fr".equals(voice.getLocale().getLanguage())) systemVoices.add(voice);
                systemVoices.sort(Comparator.comparing(Voice::getName)); setVoices();
                tts.setOnUtteranceProgressListener(new UtteranceProgressListener(){
                    public void onStart(String id){}
                    public void onDone(String id){
                        if(!id.equals(systemId))return;
                        final int job=generation; final File file=systemFile;
                        if(systemSave)worker.execute(()->{try{export(file,job);}catch(Exception e){fail(e,job);}finally{file.delete();}});
                        else runOnUiThread(()->{if(job==generation){busy(false);status.setText("Terminé.");}});
                    }
                    public void onError(String id){if(id.equals(systemId))fail(new Exception("Erreur du moteur Android"),generation);}
                });
            });
        });
    }
    private void setVoices(){
        int selection=Math.max(0,voices.getSelectedItemPosition());
        List<String> labels=new ArrayList<>();labels.add("Tom — homme • intégré • hors ligne");
        for(Voice voice:systemVoices)labels.add("Android — "+voice.getName()+(voice.isNetworkConnectionRequired()?" • en ligne":" • hors ligne"));
        voices.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,labels));
        voices.setSelection(Math.min(selection,labels.size()-1));
    }
    private void busy(boolean value){preview.setEnabled(!value);save.setEnabled(!value);voices.setEnabled(!value);getWindow().getDecorView().setKeepScreenOn(value);}
    private void cancel(){generation++; systemId=null;if(tts!=null)tts.stop();if(player!=null){player.release();player=null;}}
    private void start(boolean exporting){
        String text=input.getText().toString().trim();
        if(text.isEmpty()){status.setText("Ajoutez un texte français.");return;}
        int selected=voices.getSelectedItemPosition();int limit=selected==0?30000:TextToSpeech.getMaxSpeechInputLength();
        if(text.length()>limit){status.setText("Maximum pour cette voix : "+limit+" caractères (espaces inclus).");return;}
        cancel();final int job=generation;final float speed=rate;
        busy(true);status.setText("Génération…");
        if(selected==0){
            worker.execute(()->{
                File file=new File(getCacheDir(),"Tom_"+job+".wav");
                try{
                    tom.generate(text,speed,file,new TomEngine.Listener(){
                        public boolean cancelled(){return job!=generation;}
                        public void progress(String message){runOnUiThread(()->{if(job==generation)status.setText(message);});}
                    });
                    if(job!=generation)return;
                    if(exporting)export(file,job);
                    else runOnUiThread(()->play(file,job));
                }catch(CancellationException ignored){}catch(Exception e){fail(e,job);}
                finally{if(exporting||job!=generation)file.delete();}
            });
        }else{
            if(!androidReady){busy(false);status.setText("Moteur Android indisponible.");return;}
            if(tts.setVoice(systemVoices.get(selected-1))==TextToSpeech.ERROR){busy(false);status.setText("Cette voix est indisponible. Choisissez Tom.");return;}
            tts.setSpeechRate(speed);systemId="ANDROID_"+job;systemSave=exporting;
            int result;
            if(exporting){systemFile=new File(getCacheDir(),"Android_"+job+".wav");result=tts.synthesizeToFile(text,null,systemFile,systemId);}
            else result=tts.speak(text,TextToSpeech.QUEUE_FLUSH,null,systemId);
            if(result==TextToSpeech.ERROR)fail(new Exception("Impossible de générer cette voix"),job);
        }
    }
    private void play(File file,int job){
        if(job!=generation){file.delete();return;}
        try{
            player=new MediaPlayer();player.setDataSource(file.getPath());player.prepare();
            player.setOnCompletionListener(p->{p.release();player=null;file.delete();busy(false);status.setText("Terminé.");});
            player.start();status.setText("Lecture de Tom…");
        }catch(Exception e){file.delete();fail(e,job);}
    }
    private void export(File file,int job)throws Exception{
        if(job!=generation)return;
        String name="FrenchVO_"+System.currentTimeMillis()+".wav";
        ContentValues values=new ContentValues();values.put(MediaStore.Downloads.DISPLAY_NAME,name);
        values.put(MediaStore.Downloads.MIME_TYPE,"audio/wav");values.put(MediaStore.Downloads.RELATIVE_PATH,"Download/FrenchVO");values.put(MediaStore.Downloads.IS_PENDING,1);
        ContentResolver resolver=getContentResolver();Uri uri=resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,values);
        if(uri==null)throw new IOException("Impossible de créer le fichier");
        try{
            try(InputStream in=new FileInputStream(file);OutputStream out=resolver.openOutputStream(uri)){
                if(out==null)throw new IOException("Stockage indisponible");byte[] buf=new byte[8192];int n;
                while((n=in.read(buf))!=-1){if(job!=generation)throw new CancellationException();out.write(buf,0,n);}
            }
            values.clear();values.put(MediaStore.Downloads.IS_PENDING,0);resolver.update(uri,values,null,null);
            runOnUiThread(()->{if(job==generation&&!destroyed){busy(false);status.setText("Enregistré : Downloads/FrenchVO/"+name);}});
        }catch(Exception e){resolver.delete(uri,null,null);throw e;}
    }
    private void fail(Exception e,int job){runOnUiThread(()->{if(job==generation&&!destroyed){busy(false);status.setText("Erreur : "+e.getMessage());}});}
    @Override protected void onDestroy(){destroyed=true;cancel();if(tts!=null)tts.shutdown();worker.execute(()->tom.release());worker.shutdown();super.onDestroy();}
}
