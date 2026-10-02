package com.georgeslebatoon.frenchvo;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
import java.io.*;
import java.nio.*;
public class VoiceTests {
 @Test public void directionsNeverBecomeSpokenWords(){List<ScriptPlan.Turn> turns=ScriptPlan.parse("[mystère] Bonjour. [pause] [amusé] Salut !","French");assertEquals(2,turns.size());assertEquals("Bonjour. <short pause>",turns.get(0).text);assertTrue(turns.get(0).style.contains("mysterious"));assertFalse(turns.get(1).text.contains("amusé"));}
 @Test(expected=IllegalArgumentException.class) public void unknownTagsAreRejected(){ScriptPlan.parse("[inconnu] Bonjour.","French");}
 @Test public void longTextKeepsWords(){String text=String.join(" ",Collections.nCopies(5000,"bonjour"));List<List<ScriptPlan.Turn>> batches=ScriptPlan.batches(ScriptPlan.parse(text,"French"));List<String> parts=new ArrayList<>();for(List<ScriptPlan.Turn> batch:batches)for(ScriptPlan.Turn t:batch){assertTrue(t.text.length()<=1200);parts.add(t.text);}assertEquals(text,String.join(" ",parts));}
 private byte[] wav(int rate){ByteBuffer b=ByteBuffer.allocate(48).order(ByteOrder.LITTLE_ENDIAN);b.put("RIFF".getBytes()).putInt(40).put("WAVEfmt ".getBytes()).putInt(16).putShort((short)1).putShort((short)1).putInt(rate).putInt(rate*2).putShort((short)2).putShort((short)16).put("data".getBytes()).putInt(4).putInt(100);return b.array();}
 @Test public void audioPartsMergeWithCorrectHeader()throws Exception{File f=File.createTempFile("voice",".wav");try(WaveFile w=new WaveFile(f)){w.append(wav(24000));w.append(wav(24000));w.finish();}byte[] bytes=java.nio.file.Files.readAllBytes(f.toPath());assertEquals(52,bytes.length);assertEquals(8,ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getInt(40));f.delete();}
 @Test(expected=IOException.class) public void changedSampleRateIsRejected()throws Exception{File f=File.createTempFile("voice",".wav");try(WaveFile w=new WaveFile(f)){w.append(wav(24000));w.append(wav(48000));}finally{f.delete();}}
}
