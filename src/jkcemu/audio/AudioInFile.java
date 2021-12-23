/*
 * (c) 2008-2021 Jens Mueller
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
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import jkcemu.base.EmuUtil;
import jkcemu.emusys.kc85.KCAudioCreator;
import jkcemu.emusys.zxspectrum.ZXSpectrumAudioCreator;
import jkcemu.file.FileInfo;
import jkcemu.file.FileUtil;
import jkcemu.text.TextUtil;
import z80emu.Z80CPU;


public class AudioInFile extends AudioIn
{
  private File                    file;
  private byte[]                  fileBytes;
  private int                     offs;
  private PCMDataSource           pcmIn;
  private boolean                 pcmRequested;
  private byte[]                  frameBuf;
  private long                    fileFrameCnt;
  private long                    fileFramePos;
  private int                     progressStepSize;
  private int                     progressStepCnt;
  private volatile boolean        pause;
  private volatile boolean        monitorRequested;
  private volatile boolean        monitorState;
  private Mixer.Info              monitorMixerInfo;
  private Mixer.Info              monitorMixerUsed;
  private volatile SourceDataLine monitorLine;
  private byte[]                  monitorBuf;
  private int                     monitorPos;


  public AudioInFile(
		AudioIOObserver observer,
		Z80CPU          z80cpu,
		int             speedKHz,
		File            file,
		byte[]          fileBytes,	// kann null sein
		int             offs )
  {
    super( observer, z80cpu );
    this.file             = file;
    this.fileBytes        = fileBytes;
    this.offs             = offs;
    this.pcmRequested     = true;
    this.pcmIn            = null;
    this.frameBuf         = null;
    this.fileFrameCnt     = 0L;
    this.fileFramePos     = 0L;
    this.progressStepSize = 0;
    this.progressStepCnt  = 0;
    this.speedHz          = speedKHz * 1000;
    this.pause            = true;
    this.monitorRequested = false;
    this.monitorState     = false;
    this.monitorMixerInfo = null;
    this.monitorMixerUsed = null;
    this.monitorLine      = null;
    this.monitorBuf       = null;
    this.monitorPos       = 0;
  }


  public long getFrameCount()
  {
    return this.fileFrameCnt;
  }


  public long getFramePos()
  {
    return this.fileFramePos;
  }


  public void setFramePos( long pos ) throws IOException
  {
    PCMDataSource in = this.pcmIn;
    if( in != null ) {
      if( pos < 0 ) {
	pos = 0;
      } else if( pos > this.fileFrameCnt ) {
	pos = this.fileFrameCnt;
      }
      in.setFramePos( pos );
      this.fileFramePos = pos;
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
  protected void checkCloseAndFinished()
  {
    if( this.stopRequested ) {
      this.stopRequested = false;
      closeStreams();
    }
    if( this.pcmIn == null ) {
      this.frameBuf = null;
      finished();
    }
  }


  @Override
  protected synchronized void checkOpen()
  {
    if( this.pcmRequested ) {
      this.pcmRequested = false;
      if( (this.pcmIn == null) && !this.stopRequested ) {
	openFile();
      }
    }
    boolean    monitorRequested = false;
    boolean    monitorState     = false;
    Mixer.Info monitorMixerInfo = null;
    synchronized( this ) {
      monitorState          = this.monitorState;
      monitorMixerInfo      = this.monitorMixerInfo;
      monitorRequested      = this.monitorRequested;
      this.monitorRequested = false;
    }
    if( monitorRequested && monitorState && (this.monitorLine != null) ) {
      Mixer.Info mixerUsed = this.monitorMixerUsed;
      if( (monitorMixerInfo != null) && (mixerUsed != null) ) {
	if( !monitorMixerInfo.equals( mixerUsed ) ) {
	  closeMonitor();
	}
      }
      else if( ((monitorMixerInfo != null) && (mixerUsed == null))
	       || ((monitorMixerInfo == null) && (mixerUsed != null)) )
      {
	closeMonitor();
      }
    }
    if( monitorRequested ) {
      if( monitorState ) {
	if( (this.monitorLine == null) && !this.stopRequested ) {
	  openMonitor();
	}
      } else {
	closeMonitor();
      }
    }
  }


  @Override
  public synchronized void closeLine()
  {
    closeMonitor();
  }


  @Override
  public synchronized boolean isMonitorActive()
  {
    boolean rv = false;
    if( this.monitorRequested ) {
      rv = this.monitorState;
    } else {
      rv = (this.monitorLine != null);
    }
    return rv;
  }


  @Override
  public boolean isPause()
  {
    return this.pause;
  }


  @Override
  protected byte[] readFrame()
  {
    byte[]        buf = this.frameBuf;
    PCMDataSource in  = this.pcmIn;
    if( (in != null) && (buf != null) ) {
      try {
	if( in.read( buf, 0, buf.length ) == buf.length ) {
	  if( isMonitorActive() ) {
	    writeMonitorLine( buf );
	  }
	  this.fileFramePos++;
	  if( this.progressStepCnt > 0 ) {
	    --this.progressStepCnt;
	  } else {
	    this.progressStepCnt = this.progressStepSize;
	    this.observer.fireProgressUpdate( this );
	  }
	} else {
	  closeStreams();
	}
      }
      catch( IOException ex ) {
	setErrorText( ex.getMessage() );
	closeStreams();
      }
    }
    return buf;
  }


  @Override
  public synchronized void setMonitorEnabled(
				boolean    state,
				Mixer.Info mixerInfo )
  {
    if( !this.stopRequested ) {
      this.monitorState     = state;
      this.monitorMixerInfo = mixerInfo;
      this.monitorRequested = true;
    }
  }


  @Override
  public boolean supportsMonitor()
  {
    return true;
  }


	/* --- private Methoden --- */

  private void closeMonitor()
  {
    closeDataLine( this.monitorLine );
    this.monitorLine = null;
  }


  private void closeStreams()
  {
    closeMonitor();
    EmuUtil.closeSilently( this.pcmIn );
    this.pcmIn = null;
  }


  private void openFile()
  {
    String fileFmtText = null;
    try {
      boolean isTAP = false;
      if( (this.fileBytes == null) && (this.file != null) ) {
	if( this.file.isFile() ) {
	  String fName = this.file.getName();
	  if( fName != null ) {
	    fName = fName.toLowerCase();
	    isTAP = fName.endsWith( ".tap" ) || fName.endsWith( ".tap.gz" );
	    this.fileBytes = FileUtil.readFile(
					this.file,
					true,
					AudioUtil.FILE_READ_MAX );
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
	if( FileInfo.isCswMagicAt( this.fileBytes, this.offs ) ) {
	  this.pcmIn        = CSWFile.getPCMDataSource( this.fileBytes, 0 );
	  this.fileFrameCnt = this.pcmIn.getFrameCount();
	  this.fileFramePos = 0;
	  if( this.offs > 0 ) {
	    // Restlaenge ermitteln und Fotschrittsanzeige anpassen
	    this.pcmIn = CSWFile.getPCMDataSource(
					this.fileBytes,
					this.offs );
	    this.fileFramePos = this.fileFrameCnt
					- this.pcmIn.getFrameCount();
	  }
	  fileFmtText = "CSW-Datei";
	} else if( FileInfo.isKCTapMagicAt( this.fileBytes, this.offs ) ) {
	  // Gesamtlaenge der Datei ermitteln
	  this.pcmIn = new KCAudioCreator(
				true,
				0,
				this.fileBytes,
				0,
				this.fileBytes.length ).newReader();
	  this.fileFrameCnt = this.pcmIn.getFrameCount();
	  this.fileFramePos = 0;
	  if( this.offs > 0 ) {
	    // Restlaenge ermitteln und Fotschrittsanzeige anpassen
	    this.pcmIn = new KCAudioCreator(
				true,
				0,
				this.fileBytes,
				this.offs,
				this.fileBytes.length - this.offs )
							.newReader();
	    this.fileFramePos = this.fileFrameCnt
					- this.pcmIn.getFrameCount();
	  }
	  fileFmtText = "KC-TAP-Datei";
	} else {
	  boolean isTZX = FileInfo.isTzxMagicAt( this.fileBytes, this.offs );
	  if( isTAP || isTZX ) {
	    // Gesamtlaenge der Datei ermitteln
	    this.pcmIn = new ZXSpectrumAudioCreator(
				this.fileBytes,
				0,
				this.fileBytes.length ).newReader();
	    this.fileFrameCnt = this.pcmIn.getFrameCount();
	    this.fileFramePos = 0;
	    if( this.offs > 0 ) {
	      // Restlaenge ermitteln und Fotschrittsanzeige anpassen
	      this.pcmIn = new ZXSpectrumAudioCreator(
				this.fileBytes,
				this.offs,
				this.fileBytes.length - this.offs )
							.newReader();
	      this.fileFramePos = this.fileFrameCnt
					- this.pcmIn.getFrameCount();
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
	this.pcmIn        = AudioFile.open( this.file, this.fileBytes );
	this.fileFrameCnt = this.pcmIn.getFrameCount();
	this.fileFramePos = 0;
      }
      if( this.pcmIn == null ) {
	throw new IOException();
      }
      if( this.fileFrameCnt <= 0 ) {
	throw new IOException( "Die Datei enth\u00E4lt keine Daten" );
      }
      if( fileFmtText != null ) {
	fileFmtText += ": ";
      }
      setFormat(
		fileFmtText,
		this.pcmIn.getFrameRate(),
		this.pcmIn.getSampleSizeInBits(),
		this.pcmIn.getChannels(),
		this.pcmIn.isSigned(),
		this.pcmIn.isBigEndian() );
      if( this.fileFramePos < 0 ) {
	this.fileFramePos = 0;
      }
      this.progressStepSize = (int) this.fileFrameCnt / 200;
      this.progressStepCnt  = this.progressStepSize;
      this.observer.fireProgressUpdate( this );
      int sampleSize = (this.pcmIn.getSampleSizeInBits() + 7) / 8;
      this.frameBuf  = new byte[ sampleSize * this.pcmIn.getChannels() ];
    }
    catch( IOException ex ) {
      closeStreams();
      String msg = ex.getMessage();
      if( msg != null ) {
	if( msg.isEmpty() ) {
	  msg = null;
	}
      }
      if( msg == null ) {
	msg = "Die Datei kann nicht ge\u00F6ffnet werden.";
      }
      setErrorText( msg );
    }
  }


  private void openMonitor()
  {
    AudioFormat fmt = new AudioFormat(
	(float) this.frameRate,
	this.sampleSizeInBits > 8 ? this.sampleSizeInBits : 8,
	this.channels,
	this.dataSigned,
	this.bigEndian );
    SourceDataLine line = null;
    try {
      Mixer.Info mixerInfo = this.monitorMixerInfo;
      if( mixerInfo != null ) {
	line = AudioSystem.getSourceDataLine( fmt, mixerInfo );
      } else {
	line = AudioSystem.getSourceDataLine( fmt );
      }
      if( line != null ) {
	registerCPUSynchronLine( line );
	line.open( fmt );
	line.start();

	// Buffer anlegen
	int r = Math.round( fmt.getSampleRate() );
	int n = line.getBufferSize() / 32;
	if( n < r / 8 ) {
	  n = r / 8;		// min. 1/8 Sekunde
	}
	else if( n > r / 2 ) {
	  n = r / 2;		// max. 1/2 Sekunde
	}
	if( n < 1 ) {
	  n = 1;
	}
	this.monitorBuf       = new byte[ n ];
	this.monitorMixerUsed = mixerInfo;
      }
    }
    catch( Exception ex ) {
      closeDataLine( line );
      line = null;
      this.observer.fireMonitorFailed(
		this,
		"Das Mith\u00F6ren ist nicht m\u00F6glich,\n"
			+ "da das \u00D6ffnen eines Audiokanals"
			+ " mit dem Format\n"
			+ "der Tape- bzw. Sound-Datei fehlgeschlagen ist." );
    }
    this.monitorLine = line;
  }


  private void writeMonitorLine( byte[] buf )
  {
    SourceDataLine line = this.monitorLine;
    if( (line != null) && (buf != null) ) {
      try {
	line.write( buf, 0, buf.length );
      }
      catch( Exception ex ) {
	closeMonitor();
	this.observer.fireMonitorFailed(
		this,
		"Das Mith\u00F6ren ist nicht mehr m\u00F6glich,\n"
			+ "da der Audiokanal keine Daten mehr annimmt." );
      }
    }
  }
}
