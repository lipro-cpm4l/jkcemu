/*
 * (c) 2012-2013 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Basisklasse fuer die System-spezifische Code-Erzeugung des BASIC-Compiler
 */

package jkcemu.programming.basic;

import java.lang.*;
import java.util.Set;
import jkcemu.base.EmuSys;


public abstract class AbstractTarget
{
  protected boolean xoutchAppended;
  protected boolean usesXOUTST;
  protected boolean usesX_MPEN;


  protected AbstractTarget()
  {
    reset();
  }


  /*
   * Farbcodes in HL
   */
  public void appendColorBlack( AsmCodeBuf buf )
  {
    buf.append( "\tLD\tHL,0000H\n" );
  }


  public void appendColorBlinking( AsmCodeBuf buf )
  {
    buf.append( "\tLD\tHL,0000H\n" );
  }


  public void appendColorBlue( AsmCodeBuf buf )
  {
    buf.append( "\tLD\tHL,0001H\n" );
  }


  public void appendColorCyan( AsmCodeBuf buf )
  {
    buf.append( "\tLD\tHL,0001H\n" );
  }


  public void appendColorGreen( AsmCodeBuf buf )
  {
    buf.append( "\tLD\tHL,0001H\n" );
  }


  public void appendColorMagenta( AsmCodeBuf buf )
  {
    buf.append( "\tLD\tHL,0001H\n" );
  }


  public void appendColorRed( AsmCodeBuf buf )
  {
    buf.append( "\tLD\tHL,0001H\n" );
  }


  public void appendColorWhite( AsmCodeBuf buf )
  {
    buf.append( "\tLD\tHL,0001H\n" );
  }


  public void appendColorYellow( AsmCodeBuf buf )
  {
    buf.append( "\tLD\tHL,0001H\n" );
  }


  public void appendData( AsmCodeBuf buf )
  {
    // leer
  }


  public void appendBSS( AsmCodeBuf buf )
  {
    if( this.usesX_MPEN ) {
      buf.append( "X_MPEN:\tDS\t1\n" );
    }
  }


  public void appendEtc( AsmCodeBuf buf )
  {
    // leer
  }


  public abstract void appendExit( AsmCodeBuf buf );


  protected void appendExitNoGraphicsScreen(
				AsmCodeBuf    buf,
				BasicCompiler compiler )
  {
    appendSwitchToTextScreen( buf );
    buf.append( "\tCALL\tXOUTST\n" );
    if( compiler.isLangCode( "DE" ) ) {
      buf.append( "\tDB\t\'Kein Grafik-SCREEN\'\n" );
    } else {
      buf.append( "\tDB\t\'No graphics screen\'\n" );
    }
    buf.append( "\tDB\t00H\n" );
    if( compiler.usesLibItem( BasicLibrary.LibItem.E_EXIT ) ) {
      buf.append( "\tJP\tE_EXIT\n" );
    } else {
      buf.append( "\tJP\tXEXIT\n" );
    }
    this.usesXOUTST = true;
  }


  public void appendHChar( AsmCodeBuf buf )
  {
    buf.append( "\tLD\tHL,0FFFFH\n" );	// -1: Anzahl Zeilen unbekannt
  }


  public void appendHPixel( AsmCodeBuf buf )
  {
    buf.append( "\tLD\tHL,0000H\n" );	// 0: keine Grafik
  }


  public void appendInit( AsmCodeBuf buf )
  {
    if( this.usesX_MPEN ) {
      buf.append( "\tLD\tA,01H\n"
		+ "\tLD\t(X_MPEN),A\n" );
    }
  }


  public abstract void appendInput(
				AsmCodeBuf buf,
				boolean    xckbrk,
				boolean    xinkey,
				boolean    xinchar,
				boolean    canBreakOnInput );


  public void appendMenuItem(
			BasicCompiler compiler,
			AsmCodeBuf    buf,
			String        appName )
  {
    // leer
  }


  public void appendProlog(
			BasicCompiler compiler,
			AsmCodeBuf    buf,
			String        appName )
  {
    // leer
  }


  public void appendLastScreenNum( AsmCodeBuf buf )
  {
    buf.append( "\tLD\tHL,0000H\n" );
  }


  public void appendSwitchToTextScreen( AsmCodeBuf buf )
  {
    // leer
  }


  public void appendWChar( AsmCodeBuf buf )
  {
    buf.append( "\tLD\tHL,0FFFFH\n" );	// -1: Anzahl Spalten unbekannt
  }


  public void appendWPixel( AsmCodeBuf buf )
  {
    buf.append( "\tLD\tHL,0000H\n" );	// 0: keine Grafik
  }


  /*
   * Einstellen der Randfarbe
   * Parameter:
   *   HL: Farbe
   */
  public void appendXBORDER( AsmCodeBuf buf )
  {
    // leer
  }


  public void appendXCLS( AsmCodeBuf buf )
  {
    // leer
  }


  /*
   * Gleichzeitiges Setzen der Vorder- und Hintergrundfarbe
   * Parameter:
   *   HL: Vordergrundfarbe
   *   DE: Hintergrundfarbe
   */
  public void appendXCOLOR( AsmCodeBuf buf )
  {
    // leer
  }


  /*
   * Ein- und Ausschalten des Cursors
   * Parameter:
   *   HL: 0:   Cursor ausschalten
   *       <>0: Cursor einschalten
   */
  public void appendXCURS( AsmCodeBuf buf )
  {
    // leer
  }


  /*
   * Zeichnen einer horizontaler Linie
   * Parameter:
   *   BC: Laenge - 1
   *   DE: linke X-Koordinate
   *   HL: Y-Koordinate
   */
  public void appendXHLINE( AsmCodeBuf buf, BasicCompiler compiler )
  {
    // leer
  }


  /*
   * Einstellen der Vordergrundfarbe
   * Parameter:
   *   HL: Farbe
   */
  public void appendXINK( AsmCodeBuf buf )
  {
    // leer
  }


  /*
   * Abfrage des Joystick-Status
   * Parameter:
   *   HL: Nummer des Joysticks (0: erster Joystick)
   * Rueckgabewert:
   *   HL: Joystickstatus
   *         Bit 0: links
   *         Bit 1: rechts
   *         Bit 2: runter
   *         Bit 3: hoch
   *         Bit 4: erster Aktionsknopf
   *         Bit 5: zweiter Aktionsknopf
   */
  public void appendXJOY( AsmCodeBuf buf )
  {
    // leer
  }


  /*
   * Setzen der Cursor-Position auf dem Bildschirm
   * Parameter:
   *   DE: Zeile, >= 0
   *   HL: Spalte, >= 0
   */
  public void appendXLOCATE( AsmCodeBuf buf )
  {
    // leer
  }


  /*
   * Ausgabe eines Zeichens auf dem Bildschirm
   */
  public abstract void appendXOUTCH( AsmCodeBuf buf );


  /*
   * Ausgabe eines Zeilenumbruchs
   */
  public void appendXOUTNL( AsmCodeBuf buf )
  {
    buf.append( "XOUTNL:\tLD\tA,0DH\n"
		+ "\tCALL\tXOUTCH\n"
		+ "\tLD\tA,0AH\n" );
    if( this.xoutchAppended ) {
      buf.append( "\tJP\tXOUTCH\n" );
    } else {
      appendXOUTCH( buf );
    }
  }


  /*
   * Einstellen der Hintergrundfarbe
   * Parameter:
   *   HL: Farbe
   */
  public void appendXPAPER( AsmCodeBuf buf )
  {
    // leer
  }


  /*
   * Einstellen des Stiftes
   * Parameter:
   *   A: Stift (0: Ignorieren, 1: Normal, 2: Loeschen, 3: XOR-Mode)
   */
  public void appendXPEN( AsmCodeBuf buf )
  {
    buf.append( "XPEN:\tLD\t(X_MPEN),A\n"
		+ "\tRET\n" );
    this.usesX_MPEN = true;
  }


  /*
   * Zuruecksetzen eines Pixels
   * Parameter:
   *   DE: X-Koordinate
   *   HL: Y-Koordinate
   */
  public void appendXPRES( AsmCodeBuf buf, BasicCompiler compiler )
  {
    // leer
  }


  /*
   * Setzen eines Pixels
   * Parameter:
   *   DE: X-Koordinate
   *   HL: Y-Koordinate
   */
  public void appendXPSET( AsmCodeBuf buf, BasicCompiler compiler )
  {
    // leer
  }


  /*
   * Testen eines Pixels
   * Parameter:
   *   DE: X-Koordinate (0...255)
   *   HL: Y-Koordinate (0...255)
   * Rueckgabe:
   *   HL=0:  Pixel nicht gesetzt
   *   HL=1:  Pixel gesetzt
   *   HL=-1: Pixel exisitiert nicht
   */
  public void appendXPTEST( AsmCodeBuf buf, BasicCompiler compiler )
  {
    buf.append( "XPTEST:\tLD\tHL,0FFFFH\n"
		+ "\tRET\n" );
  }


  /*
   * Bildschirmmode einstellen
   *   HL: Screen-Nummer (0: Standard)
   * Rueckgabe:
   *   CY=1: Screen-Nummer nicht unterstuetzt
   */
  public void appendXSCRS( AsmCodeBuf buf )
  {
    buf.append( "XSCRS:\tLD\tA,H\n"
		+ "\tOR\tL\n"			// CY=0
		+ "\tRET\tZ\n"
		+ "\tSCF\n"
		+ "\tRET\n" );
  }


  /*
   * Target-ID-String
   */
  public abstract void appendXTARID( AsmCodeBuf buf );


  public boolean createsCodeFor( EmuSys emuSys )
  {
    return false;
  }


  public abstract int get100msLoopCount();
  public abstract int getDefaultBegAddr();


  public boolean needsXOUTST()
  {
    return this.usesXOUTST;
  }


  /*
   * Diese Methode wird vor dem Compilieren aufgerufen,
   * um das Object in einen definierten Ausgangszustand zu versetzen.
   */
  public void reset()
  {
    this.xoutchAppended = false;
    this.usesXOUTST     = false;
    this.usesX_MPEN     = false;
  }


  public boolean supportsAppName()
  {
    return false;
  }


  public boolean supportsColors()
  {
    return false;
  }


  public boolean supportsBorderColor()
  {
    return false;
  }


  /*
   * Wenn die Methode true zurueckliefert,
   * muessen die Methoden appendXPRES(...), appendXPSET(...)
   * und appendXPTST(...) die entsprechenden Routinen anhaengen.
   */
  public boolean supportsGraphics()
  {
    return false;
  }


  public boolean supportsXCLS()
  {
    return false;
  }


  public boolean supportsXCURS()
  {
    return false;
  }


  public boolean supportsXHLINE()
  {
    return false;
  }


  public boolean supportsXJOY()
  {
    return false;
  }


  public boolean supportsXLOCAT()
  {
    return false;
  }
}
