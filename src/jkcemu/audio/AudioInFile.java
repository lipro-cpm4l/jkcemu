/*
 * (c) 2008-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Klasse fuer die Emulation
 * des Anschlusses des Magnettonbandgeraetes (Eingang),
 * indem die Audiodaten von einer Datei gelesen werden.
 */

package jkcemu.audio;

import java.io.*;
import java.lang.*;
import java.util.zip.GZIPInputStream;
import javax.sound.sampled.*;
import jkcemu.base.*;
import jkcemu.emusys.kc85.KCAudioDataStream;
import jkcemu.emusys.zxspectrum.ZXSpectrumAudioDataStream;
import jkcemu.text.TextUtil;
import z80emu.Z80CPU;


public class AudioInFile extends AudioIn
{
  private static final int MAX_MEM_FILE_SIZE = 0x100000;

  private File             file;
  private byte[]           fileBytes;
  private int              offs;
  private AudioInputStream audioIn;
  private InputStream      rawIn;
  private String           specialFmtText;
  private byte[]           frameBuf;
  private long             frameCnt;
  private long             framePos;
  private int              progressStepSize;
  private int              progressStepCnt;
  private int              speedKHz;
  private volatile boolean pause;


  public AudioInFile(
		AudioFrm audioFrm,
		Z80CPU   z80cpu,
		File     file,
		byte[]   fileBytes,
		int      offs )
  {
    super( audioFrm, z80cpu );
    this.file             = file;
    this.fileBytes        = fileBytes;
    this.offs             = offs;
    this.audioIn          = null;
    this.rawIn            = null;
    this.specialFmtText   = null;
    this.frameBuf         = null;
    this.frameCnt         = 0L;
    this.framePos         = 0L;
    this.progressStepSize = 0;
    this.progressStepCnt  = 0;
    this.speedKHz         = 0;
    this.pause            = false;
  }


  public File getFile()
  {
    return this.file;
  }


  public byte[] getFileBytes()
  {
    return this.fileBytes;
  }


  public int getSpeedKHz()
  {
    return this.speedKHz;
  }


  public void setPause( boolean state )
  {
    this.pause = state;
  }


  public AudioFormat startAudio( int speedKHz )
  {
    AudioFormat fmt = null;
    if( (this.audioIn == null) && (speedKHz > 0) ) {
      this.pause    = true;
      this.framePos = 0;
      try {
	boolean isTAP       = false;
	this.specialFmtText = null;
	if( (this.fileBytes == null) && (file != null) ) {
	  if( file.isFile() ) {
	    String fName = file.getName();
	    if( fName != null ) {
	      fName = fName.toLowerCase();
	      isTAP = fName.endsWith( ".tap" );
	      if( TextUtil.endsWith( fName, AudioUtil.tapeFileExtensions ) ) {
		this.fileBytes = EmuUtil.readFile(
						this.file,
						false,
						MAX_MEM_FILE_SIZE );
	      }
	    }
	  }
	}
	if( this.fileBytes != null ) {
	  /*
	   * Wird in der Mitte einer Multi-Tape-Datei begonnen,
	   * soll auch die Fortschrittsanzeige in der Mitte beginnen.
	   * Aus diesem Grund wird in dem Fall sowohl die Gesamtlaenge
	   * als auch die Restlaenge der Multi-Tape-Datei ermittelt.
	   */
	  EmuSysAudioDataStream ads = null;
	  if( FileInfo.isKCTapMagicAt(
				this.fileBytes,
				this.offs,
				this.fileBytes.length - this.offs ) )
	  {
	    ads = new KCAudioDataStream(
				true,
				0,
				this.fileBytes,
				0,
				this.fileBytes.length - this.offs );
	    this.frameCnt = ads.getFrameLength();
	    this.framePos = 0;
	    if( this.offs > 0 ) {
	      // Resetlaenge ermitteln und Fotschrittsanzeige anpassen
	      ads = new KCAudioDataStream(
				true,
				0,
				this.fileBytes,
				this.offs,
				this.fileBytes.length - this.offs );
	      this.framePos = this.frameCnt - ads.getFrameLength();
	    }
	    this.specialFmtText = "KC-TAP-Datei";
	  }
	  else if( FileInfo.isCswMagicAt(
				this.fileBytes,
				this.offs,
				this.fileBytes.length - this.offs ) )
	  {
	    this.audioIn = CSWFile.getAudioInputStream(
				this.fileBytes,
				0,
				this.fileBytes.length - this.offs );
	    this.frameCnt = this.audioIn.getFrameLength();
	    this.framePos = 0;
	    if( this.offs > 0 ) {
	      // Resetlaenge ermitteln und Fotschrittsanzeige anpassen
	      this.audioIn = CSWFile.getAudioInputStream(
				this.fileBytes,
				this.offs,
				this.fileBytes.length - this.offs );
	      this.framePos = this.frameCnt - ads.getFrameLength();
	    }
	    this.specialFmtText = String.format(
		"CSW-Datei, %d Hz",
		Math.round( this.audioIn.getFormat().getSampleRate() ) );
	  } else {
	    boolean isTZX = FileInfo.isTzxMagicAt(
				this.fileBytes,
				this.offs,
				this.fileBytes.length - this.offs );
	    if( isTAP || isTZX ) {
	      // Gesamtlaenge der Datei ermitteln
	      ads = new ZXSpectrumAudioDataStream(
				this.fileBytes,
				0,
				this.fileBytes.length - this.offs );
	      this.frameCnt = ads.getFrameLength();
	      this.framePos = 0;
	      if( this.offs > 0 ) {
		// Resetlaenge ermitteln und Fotschrittsanzeige anpassen
		ads = new ZXSpectrumAudioDataStream(
				this.fileBytes,
				this.offs,
				this.fileBytes.length - this.offs );
		this.framePos = this.frameCnt - ads.getFrameLength();
	      }
	      if( isTZX ) {
		this.specialFmtText = "CDT/TZX-Datei";
	      } else {
		this.specialFmtText = "ZX-TAP-Datei";
	      }
	    }
	  }
	  if( (this.audioIn == null) && (ads != null) ) {
	    if( this.framePos < 0 ) {
	      this.framePos = 0;
	    }
	    this.audioIn = new AudioInputStream(
				ads,
				ads.getAudioFormat(),
				ads.getFrameLength() );
	  }
	}
	if( this.audioIn == null ) {
	  if( this.fileBytes != null ) {
	    this.audioIn = AudioSystem.getAudioInputStream(
			new ByteArrayInputStream(
				this.fileBytes,
				this.offs,
				this.fileBytes.length - this.offs ) );
	  } else {
	    this.rawIn   = EmuUtil.openBufferedOptionalGZipFile( this.file );
	    this.audioIn = AudioSystem.getAudioInputStream( this.rawIn );
	  }
	  this.frameCnt = this.audioIn.getFrameLength();
	  this.framePos = 0;
	}
	fmt = this.audioIn.getFormat();
	if( this.frameCnt > 0 ) {
	  this.progressStepSize = (int) this.frameCnt / 100;
	  this.progressStepCnt  = this.progressStepSize;
	  this.progressEnabled  = true;
	  this.firstCall        = true;
	  this.speedKHz         = speedKHz;
	  this.audioFrm.fireProgressUpdate(
			(float) this.framePos / (float) this.frameCnt );
	}
      }
      catch( UnsupportedAudioFileException ex ) {
	this.errorText = "Das Dateiformat wird nicht unterst\u00FCtzt.";
      }
      catch( Exception ex ) {
	this.errorText = "Die Datei kann nicht ge\u00F6ffnet werden.";
	String msg     = ex.getMessage();
	if( msg != null ) {
	  if( !msg.isEmpty() ) {
	    this.errorText = msg;
	  }
	}
	closeStreams();
      }
      if( (this.audioIn != null) && (fmt != null) ) {
	this.frameBuf        = new byte[ fmt.getFrameSize() ];
	this.tStatesPerFrame = (int) (((float) speedKHz) * 1000.0F
						/ fmt.getFrameRate());
      } else {
	stopAudio();
      }
    }
    setAudioFormat( fmt );
    return this.audioFmt;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public boolean isPause()
  {
    return this.pause;
  }


  @Override
  public String getSpecialFormatText()
  {
    return this.specialFmtText;
  }


  @Override
  public void reset()
  {
    stopAudio();
    this.audioFrm.fireFinished();
  }


  @Override
  public void stopAudio()
  {
    closeMonitorLine();
    closeStreams();
    this.frameBuf        = null;
    this.progressEnabled = false;
  }


  @Override
  protected boolean supportsMonitor()
  {
    return true;
  }


  @Override
  protected byte[] readFrame()
  {
    AudioInputStream in  = this.audioIn;
    byte[]           buf = this.frameBuf;
    if( (in != null) && (buf != null) ) {
      try {
	if( in.read( buf ) == buf.length ) {
	  if( isMonitorActive() ) {
            writeMonitorLine( buf );
          }
	  this.framePos++;
	  if( this.progressStepCnt > 0 ) {
	    --this.progressStepCnt;
	  } else {
	    this.progressStepCnt = this.progressStepSize;
	    this.audioFrm.fireProgressUpdate(
			(float) this.framePos / (float) this.frameCnt );
	  }
	} else {
	  buf = null;
	  closeStreams();
	  this.audioFrm.fireFinished();
	}
      }
      catch( IOException ex ) {
	this.errorText = ex.getMessage();
      }
    }
    return buf;
  }


	/* --- private Methoden --- */

  private void closeStreams()
  {
    EmuUtil.doClose( this.audioIn );
    EmuUtil.doClose( this.rawIn );
    this.audioIn         = null;
    this.rawIn           = null;
  }
}
