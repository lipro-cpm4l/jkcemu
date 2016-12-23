/*
 * (c) 2015-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Erzeugen von automatischen Tastatureingaben
 */

package jkcemu.base;

import java.lang.*;
import java.util.Properties;
import jkcemu.Main;


public class AutoInputWorker extends Thread
{
  public static final String PROP_AUTOINPUT_PREFIX = "autoinput.";

  private EmuThread                      emuThread;
  private java.util.List<AutoInputEntry> entries;


  public static void start( EmuThread emuThread, Properties props )
  {
    EmuSys emuSys = emuThread.getEmuSys();
    if( emuSys != null ) {
      java.util.List<AutoInputEntry> entries = AutoInputEntry.readEntries(
			props,
			emuSys.getPropPrefix() + PROP_AUTOINPUT_PREFIX );
      if( entries != null ) {
	if( !entries.isEmpty() ) {
	  (new AutoInputWorker( emuThread, entries )).start();
	}
      }
    }
  }


	/* --- Runnable --- */

  public void run()
  {
    EmuSys emuSys = this.emuThread.getEmuSys();
    if( emuSys != null ) {
      try {
	for( AutoInputEntry entry : this.entries ) {

	  // ggf. warten
	  int millis = entry.getMillisToWait();
	  if( millis > 0 ) {
	    sleep( millis );
	  }

	  // Text einfuegen
	  emuSys.startPastingText( entry.getInputText() );
	  while( emuSys.isPastingText() ) {
	    sleep( 10 );
	  }
	}
      }
      catch( InterruptedException ex ) {}
    }
  }


	/* --- Konstruktor --- */

  private AutoInputWorker(
		EmuThread                      emuThread,
		java.util.List<AutoInputEntry> entries )
  {
    super( Main.getThreadGroup(), "JKCEMU auto input" );
    this.emuThread = emuThread;
    this.entries   = entries;
  }
}
