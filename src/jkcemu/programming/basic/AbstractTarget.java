/*
 * (c) 2012-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Basisklasse fuer die System-spezifische Code-Erzeugung des BASIC-Compiler
 */

package jkcemu.programming.basic;

import java.lang.*;
import java.util.HashMap;
import java.util.Map;
import jkcemu.base.EmuSys;
import jkcemu.programming.basic.target.AC1Target;
import jkcemu.programming.basic.target.CPMTarget;
import jkcemu.programming.basic.target.HueblerGraphicsMCTarget;
import jkcemu.programming.basic.target.KC854Target;
import jkcemu.programming.basic.target.KC85Target;
import jkcemu.programming.basic.target.KramerMCTarget;
import jkcemu.programming.basic.target.LLC2HIRESTarget;
import jkcemu.programming.basic.target.SCCHTarget;
import jkcemu.programming.basic.target.Z1013PetersTarget;
import jkcemu.programming.basic.target.Z1013Target;
import jkcemu.programming.basic.target.Z9001KRTTarget;
import jkcemu.programming.basic.target.Z9001Target;


public abstract class AbstractTarget
{
  protected boolean xoutchAppended;
  protected boolean usesX_M_PEN;

  private Map<String,Integer> namedValues;


  protected AbstractTarget()
  {
    this.namedValues = new HashMap<>();
    setNamedValue( CPMTarget.BASIC_TARGET_NAME, 100 );
    setNamedValue( HueblerGraphicsMCTarget.BASIC_TARGET_NAME, 300 );
    setNamedValue( KC85Target.BASIC_TARGET_NAME, 400 );
    setNamedValue( KC854Target.BASIC_TARGET_NAME, 410 );
    setNamedValue( KramerMCTarget.BASIC_TARGET_NAME, 600 );
    setNamedValue( SCCHTarget.BASIC_TARGET_NAME, 700 );
    setNamedValue( AC1Target.BASIC_TARGET_NAME, 710 );
    setNamedValue( LLC2HIRESTarget.BASIC_TARGET_NAME, 720 );
    setNamedValue( Z1013Target.BASIC_TARGET_NAME, 800 );
    setNamedValue( Z1013PetersTarget.BASIC_TARGET_NAME, 810 );
    setNamedValue( Z9001Target.BASIC_TARGET_NAME, 900 );
    setNamedValue( Z9001KRTTarget.BASIC_TARGET_NAME, 910 );
    setNamedValue( "GRAPHICSCREEN", -1 );
    for( String name : new String[] {
				"LASTSCREEN", "BLACK",
				"JOYST_LEFT", "JOYST_RIGHT",
				"JOYST_UP", "JOYST_DOWN",
				"JOYST_BUTTON1", "JOYST_BUTTON2" } )
    {
      setNamedValue( name, 0 );
    }
    for( String name : new String[] {
				"BLINKING", "BLUE", "CYAN", "GREEN",
				"MAGENTA", "RED", "WHITE", "YELLOW" } )
    {
      setNamedValue( name, 1 );
    }
    reset();
  }


  protected static String[] add( String[] a, String s )
  {
    String[] rv = null;
    if( a != null ) {
      rv      = new String[ a.length + 1 ];
      rv[ 0 ] = s;
      System.arraycopy( a, 0, rv, 1, a.length );
    } else {
      rv = new String[] { s };
    }
    return rv;
  }


  public void appendDataTo( AsmCodeBuf buf )
  {
    // leer
  }


  public void appendBssTo( AsmCodeBuf buf )
  {
    if( this.usesX_M_PEN ) {
      buf.append( "X_M_PEN:\tDS\t1\n" );
    }
  }


  public void appendEnableVdipTo( AsmCodeBuf buf )
  {
    // leer
  }


  public void appendEtcPreXOutTo( AsmCodeBuf buf )
  {
    // leer
  }


  public void appendEtcPastXOutTo( AsmCodeBuf buf )
  {
    // leer
  }


  public abstract void appendExitTo( AsmCodeBuf buf );


  public void appendPreExitTo( AsmCodeBuf buf )
  {
    // leer
  }


  protected void appendExitNoGraphicsScreenTo(
				AsmCodeBuf    buf,
				BasicCompiler compiler )
  {
    appendSwitchToTextScreenTo( buf );
    buf.append( "\tCALL\tXOUTST\n" );
    if( compiler.isLangCode( "DE" ) ) {
      buf.append( "\tDB\t\'Kein Grafik-SCREEN\'\n" );
    } else {
      buf.append( "\tDB\t\'No graphics screen\'\n" );
    }
    buf.append( "\tDB\t00H\n" );
    if( compiler.getBasicOptions().getPrintLineNumOnAbort()
	&& compiler.usesLibItem( BasicLibrary.LibItem.E_EXIT ) )
    {
      buf.append( "\tJP\tE_EXIT\n" );
    } else {
      buf.append( "\tCALL\tXOUTNL\n"
		+ "\tJP\tXEXIT\n" );
      compiler.getLibItems().add( BasicLibrary.LibItem.XOUTNL );
    }
    compiler.getLibItems().add( BasicLibrary.LibItem.XOUTST );
  }


  public void appendFileHandler( BasicCompiler compiler )
  {
    // leer
  }


  public void appendHCharTo( AsmCodeBuf buf )
  {
    buf.append( "\tLD\tHL,0FFFFH\n" );	// -1: Anzahl Zeilen unbekannt
  }


  public void appendHPixelTo( AsmCodeBuf buf )
  {
    buf.append( "\tLD\tHL,0000H\n" );	// 0: keine Grafik
  }


  public void appendInitTo( AsmCodeBuf buf )
  {
    if( this.usesX_M_PEN ) {
      buf.append( "\tLD\tA,01H\n"
		+ "\tLD\t(X_M_PEN),A\n" );
    }
  }


  /*
   * Die Methode fuegt die Routinen zur Abfrage der Tastatur hinzu.
   *
   * Parameter:
   *   xckbrk: Routine XCKBRK (Test auf CTRL-C) muss hinzugefuegt werden.
   *           kein Rueckgabewert
   *   XINKEY: Routine XINKEY (Tastaturabfrage ohne warten)
   *           muss hinzugefuegt werden.
   *           Rueckgabewert: Tastencode in A, 0: keine Taste gedrueckt
   *   XINCH : Routine XINCH (Tastaturabfrage mit warten)
   *           muss hinzugefuegt werden.
   *           Rueckgabewert: Tastencode in A
   *
   * Allgemein gilt:
   * Wenn die Parameter xckbrk oder canBreakOnInput gesetzt sind,
   * muss beim Druecken von CTRL-C das Programm abgebrochen werden.
   */
  public abstract void appendInputTo(
				AsmCodeBuf buf,
				boolean    xckbrk,
				boolean    xinkey,
				boolean    xinch,
				boolean    canBreakOnInput );


  public void appendPrologTo(
			AsmCodeBuf    buf,
			BasicCompiler compiler,
			String        appName )
  {
    // leer
  }


  public void appendSwitchToTextScreenTo( AsmCodeBuf buf )
  {
    // leer
  }


  public void appendWCharTo( AsmCodeBuf buf )
  {
    buf.append( "\tLD\tHL,0FFFFH\n" );	// -1: Anzahl Spalten unbekannt
  }


  public void appendWPixelTo( AsmCodeBuf buf )
  {
    buf.append( "\tLD\tHL,0000H\n" );	// 0: keine Grafik
  }


  /*
   * Einstellen der Randfarbe
   * Parameter:
   *   HL: Farbe
   */
  public void appendXBorderTo( AsmCodeBuf buf )
  {
    // leer
  }


  public void appendXClsTo( AsmCodeBuf buf )
  {
    // leer
  }


  /*
   * Gleichzeitiges Setzen der Vorder- und Hintergrundfarbe
   * Parameter:
   *   HL: Vordergrundfarbe
   *   DE: Hintergrundfarbe
   */
  public void appendXColorTo( AsmCodeBuf buf )
  {
    // leer
  }


  /*
   * Ein- und Ausschalten des Cursors
   * Parameter:
   *   HL: 0:   Cursor ausschalten
   *       <>0: Cursor einschalten
   */
  public void appendXCursTo( AsmCodeBuf buf )
  {
    // leer
  }


  /*
   * Zeichnen einer horizontalen Linie
   * Parameter:
   *   BC: Laenge - 1
   *   DE: linke X-Koordinate, nicht kleiner 0
   *   HL: Y-Koordinate
   */
  public void appendXHLineTo( AsmCodeBuf buf, BasicCompiler compiler )
  {
    // leer
  }


  /*
   * Einstellen der Vordergrundfarbe
   * Parameter:
   *   HL: Farbe
   */
  public void appendXInkTo( AsmCodeBuf buf )
  {
    // leer
  }


  /*
   * Abfrage des Joystick-Status
   * Parameter:
   *   HL: Nummer des Joysticks (0: erster Joystick)
   * Rueckgabewert:
   *   HL: Joystickstatus
   */
  public void appendXJoyTo( AsmCodeBuf buf )
  {
    // leer
  }


  /*
   * Setzen der Cursor-Position auf dem Bildschirm
   * Parameter:
   *   DE: Zeile, >= 0
   *   HL: Spalte, >= 0
   */
  public void appendXLocateTo( AsmCodeBuf buf )
  {
    // leer
  }


  /*
   * Ausgabe eines Zeichens auf dem Drucker
   */
  public void appendXLPtchTo( AsmCodeBuf buf )
  {
    buf.append( "XLPTCH:\tRET\n" );
  }


  /*
   * Ausgabe des Zeichens in A auf dem Bildschirm
   */
  public abstract void appendXOutchTo( AsmCodeBuf buf );


  /*
   * Ausgabe eines Zeilenumbruchs
   */
  public void appendXOutnlTo( AsmCodeBuf buf )
  {
    buf.append( "XOUTNL:\tLD\tA,0DH\n"
		+ "\tCALL\tXOUTCH\n"
		+ "\tLD\tA,0AH\n" );
    if( this.xoutchAppended ) {
      buf.append( "\tJP\tXOUTCH\n" );
    } else {
      appendXOutchTo( buf );
    }
  }


  /*
   * Einstellen der Hintergrundfarbe
   * Parameter:
   *   HL: Farbe
   */
  public void appendXPaperTo( AsmCodeBuf buf )
  {
    // leer
  }


  /*
   * Einstellen des Stiftes
   * Parameter:
   *   A: Stift (0: Ignorieren, 1: Normal, 2: Loeschen, 3: XOR-Mode)
   */
  public void appendXPenTo( AsmCodeBuf buf )
  {
    buf.append( "XPEN:\tLD\t(X_M_PEN),A\n"
		+ "\tRET\n" );
    this.usesX_M_PEN = true;
  }


  /*
   * Wenn die Methode supportsXPAINT_LEFT_RIGHT() true liefert,
   * muss diese Methode hier die Routinen XPAINT_LEFT und XPAINT_RIGHT
   * implementieren, anderenfalls XPAINT,
   * sofern das Zielsystem Grafik unterstuetzt.
   *
   * XPAINT:
   *   Setzen eines Pixels ohne Beruecksichtigung des eingestellten Stiftes
   *   bei gleichzeitigem Test, ob dieser schon gesetzt ist
   *   Parameter:
   *     DE: X-Koordinate
   *     HL: Y-Koordinate
   *   Rueckgabe:
   *     CY=1: Pixel bereits gesetzt oder ausserhalb des sichtbaren Bereichs
   *
   * XPAINT_LEFT:
   *   Fuellen einer Linie ab dem uebergebenen Punkt nach links,
   *   Der Startpunkt selbst wird nicht geprueft
   *   Parameter:
   *     PAINT_M_X:    X-Koordinate Startpunkt
   *     PAINT_M_Y:    Y-Koordinate Startpunkt
   *   Rueckgabe:
   *     (PAINT_M_X1): X-Endkoordinate der gefuellten Linie,
   *                   kleiner oder gleich X-Startpunkt
   *
   * XPAINT_RIGHT:
   *   Fuellen einer Linie ab dem uebergebenen Punkt nach rechts
   *   Parameter:
   *     PAINT_M_X:    X-Koordinate Startpunkt
   *     PAINT_M_Y:    Y-Koordinate Startpunkt
   *   Rueckgabe:
   *     CY=1:         Pixel im Startpunkt bereits gesetzt (gefuellt)
   *                   oder ausserhalb des sichtbaren Bereichs
   *     (PAINT_M_X2): X-Endkoordinate der gefuellten Linie, nur bei CY=0
   */
  public void appendXPaintTo( AsmCodeBuf buf, BasicCompiler compiler )
  {
    // leer
  }


  /*
   * Farbe eines Pixels ermitteln
   * Parameter:
   *   DE: X-Koordinate (0...255)
   *   HL: Y-Koordinate (0...255)
   * Rueckgabe:
   *   HL >= 0: Farbcode des Pixels
   *   HL=-1:   Pixel exisitiert nicht
   */
  public void appendXPointTo( AsmCodeBuf buf, BasicCompiler compiler )
  {
    buf.append( "XPOINT:\tLD\tHL,0FFFFH\n"
		+ "\tRET\n" );
  }


  /*
   * Zuruecksetzen eines Pixels
   * Parameter:
   *   DE: X-Koordinate
   *   HL: Y-Koordinate
   */
  public void appendXPResTo( AsmCodeBuf buf, BasicCompiler compiler )
  {
    // leer
  }


  /*
   * Setzen eines Pixels unter Beruecksichtigung des eingestellten Stiftes
   * Parameter:
   *   DE: X-Koordinate
   *   HL: Y-Koordinate
   */
  public void appendXPSetTo( AsmCodeBuf buf, BasicCompiler compiler )
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
  public void appendXPTestTo( AsmCodeBuf buf, BasicCompiler compiler )
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
  public void appendXScreenTo( AsmCodeBuf buf )
  {
    buf.append( "XSCREEN:\n"
		+ "\tLD\tA,H\n"
		+ "\tOR\tL\n"			// CY=0
		+ "\tRET\tZ\n"
		+ "\tSCF\n"
		+ "\tRET\n" );
  }


  public abstract int      get100msLoopCount();
  public abstract String[] getBasicTargetNames();
  public abstract int      getDefaultBegAddr();


  /*
   * Die Methode besagt, inwieweit der von dem Zielsystem erzeugte
   * Programmcode auf dem uebergebenen emulierten System lauffaehig ist:
   *   0: nicht lauffaehig
   *   1: nur Basisfunktionen lauffaehig
   *   2: bis auf ein paar spezielle Funktionen weitgehend lauffaehig
   *   3: voll lauffaehig
   */
  public int getCompatibilityLevel( EmuSys emuSys )
  {
    return 0;
  }


  public String getFileHandlerLabel()
  {
    return null;
  }


  public int getFileIOChannelSize()
  {
    return 0;
  }


  public int getMaxAppNameLen()
  {
    return 0;
  }


  public int getNamedValue( String name )
  {
    Integer value = this.namedValues.get( name );
    return value != null ? value.intValue() : 0;
  }


  public String getStartCmd( EmuSys emuSys, String appName, int begAddr )
  {
    return null;
  }


  public int[] getTargetIDs()
  {
    int[]    rv          = null;
    String[] targetNames = getBasicTargetNames();
    if( targetNames != null ) {
      rv = new int[ targetNames.length ];
      for( int i = 0; i < targetNames.length; i++ ) {
	rv[ i ] = getNamedValue( targetNames[ i ] );
      }
    } else {
      rv = new int[ 0 ];
    }
    return rv;
  }


  public int[] getVdipBaseIOAddresses()
  {
    return null;
  }


  public boolean isReservedWord( String name )
  {
    return this.namedValues.containsKey( name.toUpperCase() );
  }


  public boolean needsEnableVdip()
  {
    return false;
  }


  public void preAppendLibraryCode( BasicCompiler compiler )
  {
    // leer
  }


  /*
   * Diese Methode wird vor dem Compilieren aufgerufen,
   * um das Object in einen definierten Ausgangszustand zu versetzen.
   */
  public void reset()
  {
    this.xoutchAppended = false;
    this.usesX_M_PEN    = false;
  }


  protected void setNamedValue( String name, int value )
  {
    this.namedValues.put( name, value );
  }


  public boolean startsWithFileDevice( String fileName )
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


  public boolean supportsXPAINT_LEFT_RIGHT()
  {
    return false;
  }


  public boolean supportsXLPTCH()
  {
    return false;
  }
}
