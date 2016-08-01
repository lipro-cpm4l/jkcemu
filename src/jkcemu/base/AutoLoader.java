/*
 * (c) 2015-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Automatisches Laden von Dateien in den Arbeitsspeicher
 */

package jkcemu.base;

import java.io.*;
import java.lang.*;
import java.util.*;
import jkcemu.Main;
import jkcemu.audio.AudioUtil;


public class AutoLoader extends Thread
{
  private static final String TEXT_CANNOT_LOAD = "Kann nicht geladen werden";

  private EmuThread                     emuThread;
  private java.util.List<AutoLoadEntry> entries;


  public static void start( EmuThread emuThread, Properties props )
  {
    EmuSys emuSys = emuThread.getEmuSys();
    if( emuSys != null ) {
      java.util.List<AutoLoadEntry> entries = AutoLoadEntry.readEntries(
				props,
				emuSys.getPropPrefix() + "autoload." );
      if( entries != null ) {
	if( !entries.isEmpty() ) {
	  (new AutoLoader( emuThread, entries )).start();
	}
      }
    }
  }


	/* --- Runnable --- */

  public void run()
  {
    try {
      for( AutoLoadEntry entry : this.entries ) {
	String fileName = entry.getFileName();
	if( fileName != null ) {
	  if( !fileName.isEmpty() ) {
	    this.emuThread.getScreenFrm().showStatusText(
					"AutoLoad: " + fileName );
	    try {
	      File file = new File( fileName );

	      // Dateityp ermitteln
	      if( AudioUtil.isAudioFile( file ) ) {
		throw new IOException(
			"Sound-Datei bei AutoLoad nicht unterst\u00FCtzt" );
	      }
	      byte[] fileBuf  = EmuUtil.readFile( file, true, 0x10000 );
	      if( fileBuf == null ) {
		throw new IOException( TEXT_CANNOT_LOAD );
	      }
	      FileInfo fileInfo = FileInfo.analyzeFile( fileBuf, file );
	      if( fileInfo == null ) {
		throw new IOException( "Dateiformat unbekannt" );
	      }
	      if( fileInfo.isTapeFile() ) {
		throw new IOException(
			"Tape-Datei bei AutoLoad nicht unterst\u00FCtzt" );
	      }

	      // Ladeadresse ermitteln
	      Integer loadAddr = entry.getLoadAddr();
	      if( loadAddr == null ) {
		int begAddr = fileInfo.getBegAddr();
		if( begAddr >= 0 ) {
		  loadAddr = new Integer( begAddr );
		}
	      }
	      if( loadAddr == null ) {
		EmuSys emuSys = emuThread.getEmuSys();
		if( emuSys != null ) {
		  loadAddr = emuSys.getLoadAddr();
		}
	      }
	      if( loadAddr == null ) {
		throw new IOException( "Ladeadresse nicht angegeben"
				+ " und in der Datei auch nicht enthalten" );
	      }
	      LoadData loadData = fileInfo.createLoadData( fileBuf );
	      if( loadData == null ) {
		throw new IOException( TEXT_CANNOT_LOAD );
	      }
	      String msg = loadData.getInfoMsg();
	      if( msg != null ) {
		if( !msg.isEmpty() ) {
		  addMsg( fileName, msg );
		}
	      }
	      loadData.setBegAddr( loadAddr.intValue() );
	      loadData.setStartAddr( -1 );

	      // ggf. warten
	      int millis = entry.getMillisToWait();
	      if( millis > 0 ) {
		sleep( millis );
	      }

	      // Datei laden
	      emuThread.loadIntoMemory( loadData );
	    }
	    catch( IOException ex ) {
	      String msg = ex.getMessage();
	      if( msg != null ) {
		if( msg.isEmpty() ) {
		  msg = null;
		}
	      }
	      if( msg == null ) {
		msg = TEXT_CANNOT_LOAD;
	      }
	      addMsg( fileName, msg != null ? msg : TEXT_CANNOT_LOAD );
	    }
	  }
	}
      }
    }
    catch( InterruptedException ex ) {}
  }


	/* --- Konstruktor --- */

  private AutoLoader(
		EmuThread                     emuThread,
		java.util.List<AutoLoadEntry> entries )
  {
    super( Main.getThreadGroup(), "JKCEMU auto loader" );
    this.emuThread = emuThread;
    this.entries   = entries;
  }


	/* --- private Methoden --- */

  private void addMsg( String fileName, String msg )
  {
    this.emuThread.getScreenFrm().fireAppendMsg(
			"AutoLoad:\n" + fileName + ":\n" + msg );
  }
}
