/*
 * (c) 2008-2017 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Klasse fuer die Emulation
 * des Anschlusses des Magnettonbandgeraetes (Eingang),
 * indem die Audiodaten von einer Datei gelesen werden.
 */

package jkcemu.audio;

import java.io.File;
import java.io.IOException;
import java.lang.*;
import java.util.Random;
import java.util.zip.GZIPInputStream;
import jkcemu.base.EmuUtil;
import jkcemu.base.FileInfo;
import jkcemu.emusys.kc85.KCAudioCreator;
import jkcemu.emusys.zxspectrum.ZXSpectrumAudioCreator;
import jkcemu.text.TextUtil;
import z80emu.Z80CPU;


public class AudioInFile extends AudioIn
{
  private File             file;
  private int              offs;
  private boolean          progressEnabled;
  private PCMDataSource    pcmIn;
  private byte[]           frameBuf;
  private long             frameCnt;
  private long             framePos;
  private int              progressStepSize;
  private int              progressStepCnt;
  private int              speedKHz;
  private int              eofNoiseFrames;
  private Random           eofNoiseRandom;
  private volatile boolean pause;


  public AudioInFile(
		AudioIOObserver observer,
		Z80CPU          z80cpu,
		int             speedKHz,
		File            file,
		byte[]          fileBytes,	// kann null sein
		int             offs ) throws IOException
  {
    super( observer, z80cpu );
    this.file             = file;
    this.offs             = offs;
    this.progressEnabled  = false;
    this.pcmIn            = null;
    this.frameBuf         = null;
    this.frameCnt         = 0L;
    this.framePos         = 0L;
    this.progressStepSize = 0;
    this.progressStepCnt  = 0;
    this.speedKHz         = speedKHz;
    this.eofNoiseFrames   = 0;
    this.eofNoiseRandom   = null;
    this.pause            = true;

    String fileFmtText = null;
    try {
      boolean isTAP = false;
      if( (fileBytes == null) && (file != null) ) {
	if( file.isFile() ) {
	  String fName = file.getName();
	  if( fName != null ) {
	    fName = fName.toLowerCase();
	    isTAP = fName.endsWith( ".tap" ) || fName.endsWith( ".tap.gz" );
	    fileBytes = EmuUtil.readFile(
				file,
				true,
				AudioUtil.FILE_READ_MAX );
	  }
	}
      }
      if( fileBytes != null ) {
	/*
	 * Wird in der Mitte einer Multi-Tape-Datei begonnen,
	 * soll auch die Fortschrittsanzeige in der Mitte beginnen.
	 * Aus diesem Grund wird in dem Fall sowohl die Gesamtlaenge
	 * als auch die Restlaenge der Multi-Tape-Datei ermittelt.
	 */
	if( FileInfo.isCswMagicAt( fileBytes, this.offs ) ) {
	  this.pcmIn    = CSWFile.getPCMDataSource( fileBytes, 0 );
	  this.frameCnt = this.pcmIn.getFrameCount();
	  this.framePos = 0;
	  if( this.offs > 0 ) {
	    // Resetlaenge ermitteln und Fotschrittsanzeige anpassen
	    this.pcmIn = CSWFile.getPCMDataSource(
					fileBytes,
					this.offs );
	    this.framePos = this.frameCnt - this.pcmIn.getFrameCount();
	  }
	  fileFmtText = "CSW-Datei";
	} else if( FileInfo.isKCTapMagicAt( fileBytes, this.offs ) ) {
	  // Gesamtlaenge der Datei ermitteln
	  this.pcmIn = new KCAudioCreator(
				true,
				0,
				fileBytes,
				0,
				fileBytes.length ).newReader();
	  this.frameCnt = this.pcmIn.getFrameCount();
	  this.framePos = 0;
	  if( this.offs > 0 ) {
	    // Resetlaenge ermitteln und Fotschrittsanzeige anpassen
	    this.pcmIn = new KCAudioCreator(
				true,
				0,
				fileBytes,
				this.offs,
				fileBytes.length - this.offs ).newReader();
	    this.framePos = this.frameCnt - this.pcmIn.getFrameCount();
	  }
	  fileFmtText = "KC-TAP-Datei";
	} else {
	  boolean isTZX = FileInfo.isTzxMagicAt( fileBytes, this.offs );
	  if( isTAP || isTZX ) {
	    // Gesamtlaenge der Datei ermitteln
	    this.pcmIn = new ZXSpectrumAudioCreator(
				fileBytes,
				0,
				fileBytes.length ).newReader();
	    this.frameCnt = this.pcmIn.getFrameCount();
	    this.framePos = 0;
	    if( this.offs > 0 ) {
	      // Resetlaenge ermitteln und Fotschrittsanzeige anpassen
	      this.pcmIn = new ZXSpectrumAudioCreator(
				fileBytes,
				this.offs,
				fileBytes.length - this.offs ).newReader();
	      this.framePos = this.frameCnt - this.pcmIn.getFrameCount();
	    }
	    if( isTZX ) {
	      fileFmtText = "CDT/TZX-Datei";
	    } else {
	      fileFmtText = "ZX-TAP-Datei";
	    }
	  }
	}
      }
      if( this.pcmIn == null ) {
	this.pcmIn    = AudioFile.open( file, fileBytes );
	this.frameCnt = this.pcmIn.getFrameCount();
	this.framePos = 0;
      }
      if( this.pcmIn == null ) {
	throw new IOException();
      }
      if( this.frameCnt <= 0 ) {
	throw new IOException( "Die Datei enth\u00E4lt keine Daten" );
      }
      setFormat(
		this.pcmIn.getFrameRate(),
		this.pcmIn.getSampleSizeInBits(),
		this.pcmIn.getChannels(),
		this.pcmIn.isSigned(),
		this.pcmIn.isBigEndian() );
      if( fileFmtText != null ) {
	if( this.formatText != null ) {
	  this.formatText = fileFmtText + ": " + this.formatText;
	} else {
	  this.formatText = fileFmtText;
	}
      }
      if( this.framePos < 0 ) {
	this.framePos = 0;
      }
      this.progressStepSize = (int) this.frameCnt / 200;
      this.progressStepCnt  = this.progressStepSize;
      this.progressEnabled  = true;
      this.firstCall        = true;
      this.observer.fireProgressUpdate( this );
      int sampleSize = (this.pcmIn.getSampleSizeInBits() + 7) / 8;
      this.frameBuf  = new byte[ sampleSize * this.pcmIn.getChannels() ];
      this.tStatesPerFrame = (int) (((float) speedKHz) * 1000.0F
				/ (float) this.pcmIn.getFrameRate());
    }
    catch( IOException ex ) {
      closeStreams();
      String msg = ex.getMessage();
      if( msg != null ) {
	if( msg.isEmpty() ) {
	  msg = null;
	}
      }
      if( msg != null ) {
	throw ex;
      } else {
	throw new IOException( "Die Datei kann nicht ge\u00F6ffnet werden." );
      }
    }
  }


  public File getFile()
  {
    return this.file;
  }


  public long getFrameCount()
  {
    return this.frameCnt;
  }


  public long getFramePos()
  {
    return this.framePos;
  }


  public int getSpeedKHz()
  {
    return this.speedKHz;
  }


  public void setFramePos( long pos ) throws IOException
  {
    PCMDataSource in = this.pcmIn;
    if( in != null ) {
      if( pos < 0 ) {
	pos = 0;
      } else if( pos > this.frameCnt ) {
	pos = this.frameCnt;
      }
      in.setFramePos( pos );
      this.framePos = pos;
    }
  }


  public void setPause( boolean state )
  {
    this.pause = state;
  }


  public boolean supportsSetFramePos()
  {
    PCMDataSource in = this.pcmIn;
    return in != null ? in.supportsSetFramePos() : false;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public boolean isPause()
  {
    return this.pause;
  }


  @Override
  public boolean isProgressUpdateEnabled()
  {
    return this.progressEnabled;
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
    byte[] buf = this.frameBuf;
    if( (this.eofNoiseRandom != null) && (buf != null) ) {
      this.eofNoiseRandom.nextBytes( buf );
      if( this.eofNoiseFrames > 0 ) {
	--this.eofNoiseFrames;
      } else {
	this.observer.fireFinished( this, null );
      }
    } else {
      PCMDataSource in = this.pcmIn;
      if( (in != null) && (buf != null) ) {
	try {
	  if( in.read( buf, 0, buf.length ) == buf.length ) {
	    if( isMonitorActive() ) {
	      writeMonitorLine( buf );
	    }
	    this.framePos++;
	    if( this.progressStepCnt > 0 ) {
	      --this.progressStepCnt;
	    } else {
	      this.progressStepCnt = this.progressStepSize;
	      this.observer.fireProgressUpdate( this );
	    }
	  } else {
	    closeStreams();
	    this.eofNoiseFrames = this.frameRate / 20;
	    this.eofNoiseRandom = new Random( System.currentTimeMillis() );
	  }
	}
	catch( IOException ex ) {
	  stopAudio();
	  this.observer.fireFinished( this, ex.getMessage() );
	}
      }
    }
    return buf;
  }


	/* --- private Methoden --- */

  private void closeStreams()
  {
    EmuUtil.closeSilent( this.pcmIn );
    this.pcmIn = null;
  }
}
