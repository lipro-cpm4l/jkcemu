/*
 * (c) 2008-2011 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Abspielen einer Sound-Datei oder eines Audio-Streams
 */

package jkcemu.audio;

import java.awt.Component;
import java.io.*;
import java.lang.*;
import javax.sound.sampled.*;
import javax.swing.*;
import jkcemu.base.*;


public class AudioPlayer implements Runnable
{
  private Component             owner;
  private AudioInputStream      ais;
  private EmuSysAudioDataStream ads;
  private File                  file;
  private String                title;


  public static void play(
			Component        owner,
			AudioInputStream ais,
			String           title )
  {
    play( owner, ais, null, null, title );
  }


  public static void play(
			Component             owner,
			EmuSysAudioDataStream ads,
			String                title )
  {
    play( owner, null, ads, null, title );
  }


  public static void play( Component owner, File f )
  {
    play( owner, null, null, f, "Wiedergabe von " + f.getName() + "..." );
  }


	/* --- Runnable --- */

  @Override
  public void run()
  {
    BufferedInputStream        inBuf = null;
    ProgressMonitorInputStream inPM  = null;
    SourceDataLine             line  = null;
    try {
      if( this.ais == null ) {
	if( this.ads != null ) {
	  this.ais = new AudioInputStream(
				this.ads,
				this.ads.getAudioFormat(),
				this.ads.getFrameLength() );
	} else if( this.file != null ) {
	  this.ais   = AudioSystem.getAudioInputStream( this.file );
	  this.title = "Wiedergabe von " + this.file.getName() + "...";
	}
      }
      if( this.ais != null ) {
	AudioFormat fmt      = this.ais.getFormat();
	long        frameLen = this.ais.getFrameLength();
	if( (fmt != null) && (frameLen > 0) ) {
	  inPM = new ProgressMonitorInputStream(
			this.owner,
			this.title != null ? this.title : "Wiedergabe...",
			this.ais );
	  ProgressMonitor pm = inPM.getProgressMonitor();
	  if( pm != null ) {
	    pm.setMinimum( 0 );
	    pm.setMaximum( (int) frameLen * fmt.getFrameSize() );
	    pm.setMillisToDecideToPopup( 500 );
	    pm.setMillisToPopup( 1000 );
	  }
	  line = AudioSystem.getSourceDataLine( fmt );
	  if( line != null ) {
	    if( !line.isOpen() ) {
	      line.open();
	    }
	    if( !line.isActive() ) {
	      line.start();
	    }
	    int bufSize = (int) fmt.getFrameRate() / 4
					* fmt.getFrameSize()
					* fmt.getChannels();
	    if( bufSize > 0 ) {
	      inBuf = new BufferedInputStream( inPM );
	      line.open( fmt );
	      byte[] audioBuf = new byte[ bufSize ];
	      int    nBytes   = inBuf.read( audioBuf );
	      while( nBytes > 0 ) {
		line.write( audioBuf, 0, nBytes );
		nBytes = inBuf.read( audioBuf );
	      }
	      line.drain();
	    }
	  }
	}
      }
    }
    catch( Exception ex ) {}

    EmuUtil.doClose( inPM );
    EmuUtil.doClose( this.ais );
    EmuUtil.doClose( inBuf );
    if( line != null ) {
      try {
	line.stop();
      }
      catch( Exception ex ) {}
      try {
	line.close();
      }
      catch( Exception ex ) {}
    }
  }


	/* --- private Konstruktoren und Methoden --- */

  private AudioPlayer(
		Component             owner,
		AudioInputStream      ais,
		EmuSysAudioDataStream ads,
		File                  file,
		String                title )
  {
    this.owner = owner;
    this.ais   = ais;
    this.ads   = ads;
    this.file  = file;
    this.title = title;
  }


  private static void play(
			Component             owner,
			AudioInputStream      ais,
			EmuSysAudioDataStream ads,
			File                  file,
			String                title )
  {
    if( (owner != null)
	&& ((file != null) || (ais != null) || (ads != null))
	&& (title != null) )
    {
      (new Thread(
		new AudioPlayer( owner, ais, ads, file, title ),
		"JKCEMU audio player" )).start();
    }
  }
}

