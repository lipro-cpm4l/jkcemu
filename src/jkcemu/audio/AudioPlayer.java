/*
 * (c) 2008-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Abspielen einer Sound-Datei oder eines Audio-Streams
 */

package jkcemu.audio;

import java.awt.Component;
import java.io.BufferedInputStream;
import java.io.File;
import java.lang.*;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import javax.swing.ProgressMonitor;
import javax.swing.ProgressMonitorInputStream;
import jkcemu.Main;
import jkcemu.base.EmuUtil;


public class AudioPlayer implements Runnable
{
  private Component     owner;
  private PCMDataSource pcm;
  private File          file;
  private String        title;


  public static void play(
			Component     owner,
			PCMDataSource pcm,
			String        title )
  {
    if( (owner != null) && (pcm != null) && (title != null) ) {
      play( owner, pcm, null, title );
    }
  }


  public static void play( Component owner, File file )
  {
    if( (owner != null) && (file != null) ) {
      play( owner, null, file, "Wiedergabe von " + file.getName() + "..." );
    }
  }


	/* --- Runnable --- */

  @Override
  public void run()
  {
    BufferedInputStream        inBuf = null;
    ProgressMonitorInputStream inPM  = null;
    SourceDataLine             line  = null;
    AudioInputStream           ais   = null;
    try {
      if( (this.pcm == null) && (this.file != null) ) {
	this.pcm = AudioUtil.openAudioOrTapeFile( file );
      }
      if( this.pcm != null ) {
	boolean signed           = this.pcm.isSigned();
	int     channels         = this.pcm.getChannels();
	int     sampleSizeInBits = this.pcm.getSampleSizeInBits();
	if( sampleSizeInBits < 8 ) {
	  sampleSizeInBits = 8;
	}
	ais = new AudioInputStream(
			new PCMConverterInputStream(
					this.pcm,
					signed,
					false ),
			new AudioFormat(
					(float) this.pcm.getFrameRate(),
					sampleSizeInBits,
					channels,
					signed,
					false ),
			this.pcm.getFrameCount() );
	AudioFormat fmt      = ais.getFormat();
	long        frameLen = ais.getFrameLength();
	if( (fmt != null) && (frameLen > 0) ) {
	  inPM = new ProgressMonitorInputStream(
			this.owner,
			this.title != null ? this.title : "Wiedergabe...",
			ais );
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

    EmuUtil.closeSilent( inPM );
    EmuUtil.closeSilent( ais );
    EmuUtil.closeSilent( inBuf );
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
		Component     owner,
		PCMDataSource pcm,
		File          file,
		String        title )
  {
    this.owner = owner;
    this.pcm   = pcm;
    this.file  = file;
    this.title = title;
  }


  public static void play(
			Component     owner,
			PCMDataSource pcm,
			File          file,
			String        title )
  {
    (new Thread(
	Main.getThreadGroup(),
	new AudioPlayer( owner, pcm, file, title ),
	"JKCEMU audio player" )).start();
  }
}
