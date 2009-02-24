/*
 * (c) 2009 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Manager fuer die Uebergabe von Text an das im Emulator laufende Programm.
 * Dazu wird fuer jedes einzelne Zeichen das Druecken der entsprechende
 * Taste/Tastenkombination nachgebildet.
 */

package jkcemu.base;

import java.awt.event.KeyEvent;
import java.lang.*;
import java.util.Properties;
import javax.swing.JOptionPane;
import jkcemu.Main;


public class PasteTextMngr extends Thread
{
  private ScreenFrm screenFrm;
  private EmuSys    emuSys;
  private String    text;
  private int       len;
  private int       curPos;
  private boolean   stopFired;


  public PasteTextMngr( ScreenFrm screenFrm, String text )
  {
    super( "Paste Text Thread" );
    this.screenFrm = screenFrm;
    this.emuSys    = screenFrm.getEmuThread().getEmuSys();
    this.text      = text;
    this.len       = this.text.length();
    this.curPos    = 0;
    this.stopFired = false;
  }


  public void fireStop()
  {
    this.stopFired = true;
  }


	/* --- ueberschriebene Methoden --- */

  public void run()
  {
    long delay = 0L;
    while( !this.stopFired ) {

      // kurze Wartezeit vor dem naechsten Zeichen
      if( delay > 0L ) {
	try {
	  Thread.sleep( delay );
	}
	catch( InterruptedException ex ) {}
      }

      // Abbrechen, wenn sich das emulierte System geaendert hat
      if( screenFrm.getEmuThread().getEmuSys() != this.emuSys ) {
	break;
      }

      // naechstes Zeichen holen
      char ch = '\u0000';
      if( this.curPos < this.len ) {
	ch = EmuUtil.toISO646DE( this.text.charAt( this.curPos++ ) );
	if( ch == '\r' ) {
	  if( this.curPos < this.len ) {
	    if( this.text.charAt( this.curPos ) == '\n' )
	      this.curPos++;
	  }
	  ch = '\n';
	}
      }
      if( ch == '\u0000' )
	break;

      // Zeichen uebergeben
      if( this.emuSys.pasteChar( ch ) ) {
	if( (ch == '\n') || (ch == '\r') ) {
	  delay = 300;
	} else {
	  delay = 150;
	}
      } else {
	if( JOptionPane.showConfirmDialog(
		this.screenFrm,
		String.format(
			"Das Zeichen mit dem hexadezimalen Code %02X\n"
				+ "kann nicht eingef\u00FCgt werden.",
			(int) ch ),
		"Text einf\u00FCgen",
		JOptionPane.OK_CANCEL_OPTION,
		JOptionPane.WARNING_MESSAGE ) != JOptionPane.OK_OPTION )
	{
	  this.stopFired = true;
	}
      }
    }
    this.screenFrm.firePasteFinished(
			this.curPos < this.len ?
				this.text.substring( this.curPos )
				: null );
  }
}

