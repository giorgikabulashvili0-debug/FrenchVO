package com.georgeslebatoon.frenchvo;
import java.io.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;

public final class WaveFile implements AutoCloseable {
 private final RandomAccessFile output;private int rate=0,channels=0,bits=0;
 public WaveFile(File file)throws IOException{output=new RandomAccessFile(file,"rw");output.setLength(0);output.write(new byte[44]);}
 private static String tag(byte[] bytes,int index){return new String(bytes,index,4,StandardCharsets.US_ASCII);}
 public void append(byte[] bytes)throws IOException{
  if(bytes.length<44||!tag(bytes,0).equals("RIFF")||!tag(bytes,8).equals("WAVE"))throw new IOException("Réponse audio WAV invalide.");
  ByteBuffer buffer=ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
  int sampleRate=0,nChannels=0,nBits=0,encoding=0,start=-1,length=0;
  for(int offset=12;offset+8<=bytes.length;){
   int size=buffer.getInt(offset+4);if(size<0||((long)offset+8+size)>bytes.length)throw new IOException("WAV tronqué.");
   if(tag(bytes,offset).equals("fmt ")&&size>=16){encoding=buffer.getShort(offset+8)&65535;nChannels=buffer.getShort(offset+10)&65535;sampleRate=buffer.getInt(offset+12);nBits=buffer.getShort(offset+22)&65535;}
   if(tag(bytes,offset).equals("data")){start=offset+8;length=size;}
   offset+=8+size+(size&1);
  }
  if(encoding!=1||nChannels<1||nBits!=16||sampleRate<=0||start<0||length==0)throw new IOException("Format WAV non pris en charge (PCM 16 bits attendu).");
  if(rate==0){rate=sampleRate;channels=nChannels;bits=nBits;}
  if(rate!=sampleRate||channels!=nChannels||bits!=nBits)throw new IOException("Le format audio a changé entre les parties.");
  output.write(bytes,start,length);
 }
 public void finish()throws IOException{
  long size=output.length()-44;if(rate==0||size<1)throw new IOException("Aucun audio généré.");
  if(size>Integer.MAX_VALUE-36)throw new IOException("Audio trop long.");
  int align=channels*bits/8;
  ByteBuffer b=ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN);
  b.put("RIFF".getBytes(StandardCharsets.US_ASCII)).putInt((int)size+36).put("WAVEfmt ".getBytes(StandardCharsets.US_ASCII));
  b.putInt(16).putShort((short)1).putShort((short)channels).putInt(rate).putInt(rate*align).putShort((short)align).putShort((short)bits).put("data".getBytes(StandardCharsets.US_ASCII)).putInt((int)size);
  output.seek(0);output.write(b.array());
 }
 public void close()throws IOException{output.close();}
}
