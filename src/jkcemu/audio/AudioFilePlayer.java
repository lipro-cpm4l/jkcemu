/*
 * (c) 2008-2010 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Abspielen einer Audio-Datei
 */

package jkcemu.audio;

import java.awt.Component;
import java.io.*;
import java.lang.*;
import javax.sound.sampled.*;
import javax.swing.*;


public class AudioFilePlayer implements Runnable
{
  private Component owner;
  private File      file;


  public static void play( Component owner, File file )
  {
    if( (owner != null) && (file != null) )
      (new Thread(
		new AudioFilePlayer( owner, file ),
		"JKCEMU audio file player" )).start();
  }


	/* --- Runnable --- */

  @Override
  public void run()
  {
    AudioInputStream           inAudio = null;
    ProgressMonitorInputStream inPM    = null;
    SourceDataLine             line    = null;
    try {
      inPM = new ProgressMonitorInputStream(
		this.owner,
		"Wiedergabe von " + this.file.getName() + "...",
		new BufferedInputStream( new FileInputStream( this.file ) ) );

      inAudio = AudioSystem.getAudioInputStream( inPM );
      if( inAudio != null ) {
	AudioFormat fmt = inAudio.getFormat();
	if( fmt != null ) {
	  ProgressMonitor pm = inPM.getProgressMonitor();
	  if( pm != null ) {
	    pm.setMinimum( 0 );
	    pm.setMaximum(
		(int) inAudio.getFrameLength() * fmt.getFrameSize() );
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
	      line.open( fmt );
	      byte[] audioBuf = new byte[ bufSize ];
	      int    nBytes   = inAudio.read( audioBuf );
	      while( nBytes > 0 ) {
		line.write( audioBuf, 0, nBytes );
		nBytes = inAudio.read( audioBuf );
	      }
	      line.drain();
	    }
	  }
	}
      }
    }
    catch( Exception ex ) {}

    closeStream( inAudio );
    closeStream( inPM );
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

  private AudioFilePlayer( Component owner, File file )
  {
    this.owner = owner;
    this.file  = file;
  }


  private static void closeStream( Closeable stream )
  {
    if( stream != null ) {
      try {
	stream.close();
      }
      catch( IOException ex ) {}
    }
  }
}
