/*
 * (c) 2012-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Z1013-spezifische Code-Erzeugung des BASIC-Compilers
 * mit Simulation einer 64x64-Pixel Vollgrafik
 */

package jkcemu.programming.basic.target;

import jkcemu.base.EmuSys;
import jkcemu.emusys.Z1013;
import jkcemu.programming.basic.AbstractTarget;
import jkcemu.programming.basic.AsmCodeBuf;
import jkcemu.programming.basic.BasicCompiler;


public class Z1013Target extends AbstractTarget
{
  public static final String BASIC_TARGET_NAME = "TARGET_Z1013";

  protected boolean xGetCrsPosAppended;
  protected boolean xptestAppended;

  private boolean pixUtilAppended;
  private boolean xpsetAppended;


  public Z1013Target()
  {
    setNamedValue( "GRAPHICSCREEN", 0 );
    setNamedValue( "JOYST_LEFT", 0x01 );
    setNamedValue( "JOYST_RIGHT", 0x02 );
    setNamedValue( "JOYST_DOWN", 0x04 );
    setNamedValue( "JOYST_UP", 0x08 );
    setNamedValue( "JOYST_BUTTON1", 0x10 );
  }


  /*
   * Ermittlung der Cursor-Position
   * Rueckgabe:
   *   HL: Bit 0..4: Spalte bei 32x32
   *       Bit 5..9: Zeile bei 32x32
   *   CY=1: Fehler -> HL=0FFFFH
   */
  protected void appendXGetCrsPosTo( AsmCodeBuf buf )
  {
    if( !this.xGetCrsPosAppended ) {
      buf.append( "X_GET_CRS_POS:\n"
		+ "\tLD\tHL,(002BH)\n"
		+ "\tLD\tA,H\n"
		+ "\tAND\t0FCH\n"
		+ "\tCP\t0ECH\n"
		+ "\tRET\tZ\n"			// CY=0
		+ "\tLD\tHL,0FFFFH\n"
		+ "\tSCF\n"
		+ "\tRET\n" );
      this.xGetCrsPosAppended = true;
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void appendExitTo( AsmCodeBuf buf, boolean isAppTypeSub )
  {
    if( isAppTypeSub ) {
      buf.append( "\tRET\n" );
    } else {
      buf.append( "\tJP\t0038H\n" );
    }
  }


  @Override
  public void appendHCharTo( AsmCodeBuf buf )
  {
    buf.append( "\tLD\tHL,0020H\n" );
  }


  @Override
  public void appendHPixelTo( AsmCodeBuf buf )
  {
    buf.append( "\tLD\tHL,0040H\n" );
  }


  @Override
  public void appendInputTo(
			AsmCodeBuf buf,
			boolean    xCheckBreak,
			boolean    xInkey,
			boolean    xInch,
			boolean    canBreakOnInput )
  {
    if( xCheckBreak ) {
      buf.append( "XCHECK_BREAK:\n" );
    }
    if( xCheckBreak || xInkey) {
      buf.append( "XINKEY:\tXOR\tA\n"
		+ "\tLD\t(0004H),A\n"
		+ "\tRST\t20H\n"
		+ "\tDB\t04H\n" );
      if( canBreakOnInput ) {
	buf.append( "\tCP\t03H\n"
		+ "\tJR\tZ,XBREAK\n" );
      }
      buf.append( "\tRET\n" );
    }
    if( xInch) {
      buf.append( "XINCH:\n"
		+ "\tRST\t20H\n"
		+ "\tDB\t01H\n" );
      if( canBreakOnInput ) {
	buf.append( "\tCP\t03H\n"
		+ "\tJR\tZ,XBREAK\n" );
      }
      buf.append( "\tRET\n" );
    }
  }


  @Override
  public void appendWCharTo( AsmCodeBuf buf )
  {
    buf.append( "\tLD\tHL,0020H\n" );
  }


  @Override
  public void appendWPixelTo( AsmCodeBuf buf )
  {
    buf.append( "\tLD\tHL,0040H\n" );
  }


  /*
   * Abfrage der Zeile der aktuellen Cursor-Position
   */
  @Override
  public void appendXCrsLinTo( AsmCodeBuf buf )
  {
    buf.append( "XCRSLIN:\n"
		+ "\tCALL\tX_GET_CRS_POS\n"
		+ "\tRET\tC\n"
		+ "\tLD\tA,H\n"
		+ "\tAND\t03H\n"
		+ "\tLD\tB,03H\n"
		+ "X_CRSLIN_1:\n"
		+ "\tSLA\tL\n"
		+ "\tRLA\n"
		+ "\tDJNZ\tX_CRSLIN_1\n"
		+ "\tLD\tL,A\n"
		+ "\tLD\tH,00H\n"
		+ "\tRET\n" );
    appendXGetCrsPosTo( buf );
  }


  /*
   * Abfrage der Spalte der aktuellen Cursor-Position
   */
  @Override
  public void appendXCrsPosTo( AsmCodeBuf buf )
  {
    buf.append( "XCRSPOS:\n"
		+ "\tCALL\tX_GET_CRS_POS\n"
		+ "\tRET\tC\n"
		+ "\tLD\tA,L\n"
		+ "\tAND\t1FH\n"
		+ "\tLD\tL,A\n"
		+ "\tLD\tH,00H\n"
		+ "\tRET\n" );
    appendXGetCrsPosTo( buf );
  }


  /*
   * Ein- und Ausschalten des Cursors
   * Parameter:
   *   HL: 0:   Cursor ausschalten
   *       <>0: Cursor einschalten
   */
  @Override
  public void appendXCursorTo( AsmCodeBuf buf )
  {
    buf.append( "XCURSOR:\n"
		+ "\tLD\tA,H\n"
                + "\tOR\tL\n"
		+ "\tJR\tZ,X_CURSOR_1\n"
		+ "\tLD\tHL,(002BH)\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tCP\t0FFH\n"
		+ "\tRET\tZ\n"
		+ "\tLD\t(001FH),A\n"
		+ "\tLD\t(HL),0FFH\n"
		+ "\tRET\n"
		+ "X_CURSOR_1:\n"
		+ "\tLD\tA,(001FH)\n"
		+ "\tLD\tHL,(002BH)\n"
		+ "\tLD\t(HL),A\n"
		+ "\tRET\n" );
  }


  /*
   * Lesen des Datums und der Uhrzeit
   * Rueckgabe:
   *   X_M_DATETIME:    Jahr Tausender
   *   X_M_DATETIME+1:  Jahr Hunderter
   *   X_M_DATETIME+2:  Jahr Zehner
   *   X_M_DATETIME+3:  Jahr Einser
   *   X_M_DATETIME+4:  Monat Zehner
   *   X_M_DATETIME+5:  Monat Einser
   *   X_M_DATETIME+6:  Tag Zehner
   *   X_M_DATETIME+7:  Tag Einser
   *   X_M_DATETIME+8:  Stunde Zehner (0...23)
   *   X_M_DATETIME+9:  Stunde Einser
   *   X_M_DATETIME+10: Minute Zehner
   *   X_M_DATETIME+11: Minute Einser
   *   X_M_DATETIME+12: Sekunde Zehner
   *   X_M_DATETIME+13: Sekunde Einser
   *   CY=1:            Fehler
   */
  @Override
  public void appendXDateTimeTo( AsmCodeBuf buf )
  {
    buf.append( "XDATETIME:\n"
                + "\tLD\tHL,x_M_DATETIME\n"
		+ "\tLD\t(HL),02H\n"	// Jahr Tausender
                + "\tINC\tHL\n"
                + "\tLD\t(HL),00H\n"	// Jahr Hunderter
                + "\tINC\tHL\n"
    // RTC-Modul lesen
		+ "\tLD\tA,04H\n"	// 24-Stunden-Modus einstellen
                + "\tOUT\t(7FH),A\n"
		+ "\tLD\tBC,0C7BH\n"	// von RTC-Modul lesen
                + "X_DATETIME_1:\n"
		+ "\tIN\tA,(C)\n"
		+ "\tAND\t0FH\n"
		+ "\tLD\t(HL),A\n"
                + "\tINC\tHL\n"
                + "\tDEC\tC\n"
		+ "\tDJNZ\tX_DATETIME_1\n"
		+ "\tCALL\tX_CHECK_DATETIME\n"
		+ "\tRET\tNC\n"
    // RTC im GIDE lesen
		+ "\tLD\tBC,0F85H\n"
		+ "\tLD\tA,04H\n"
                + "\tOUT\t(C),A\n"	// 24-Stunden-Modus einstellen
		+ "\tLD\tB,0BH\n"	// GIDE: Register fuer Zehner Jahr
		+ "\tLD\tHL,X_M_DATETIME+02H\n"
                + "X_DATETIME_2:\n"
                + "\tIN\tA,(C)\n"
                + "\tAND\t0FH\n"
                + "\tLD\t(HL),A\n"
                + "\tINC\tHL\n"
                + "\tDJNZ\tX_DATETIME_2\n"
                + "\tIN\tA,(C)\n"
                + "\tAND\t0FH\n"	// CY=0
                + "\tLD\t(HL),A\n" );
    appendXCheckDateTimeTo( buf );
    this.usesX_M_DATETIME = true;
  }


  /*
   * Abfrage des Joystick-Status
   * Parameter:
   *   HL: Nummer des Joysticks (0: erster Joystick)
   * Rueckgabewert:
   *   HL: Joystickstatus
   */
  @Override
  public void appendXJoyTo( AsmCodeBuf buf )
  {
    buf.append( "XJOY:\tLD\tA,H\n"
		+ "\tOR\tA\n"
		+ "\tJR\tNZ,X_JOY_2\n"
		+ "\tLD\tA,(0FFBBH)\n"
		+ "\tCP\t0C3H\n"
		+ "\tJR\tNZ,X_JOY_2\n"
		+ "\tLD\tA,L\n"
		+ "\tOR\tA\n"
		+ "\tJR\tNZ,X_JOY_1\n"
		+ "\tCALL\t0FFBBH\n"
		+ "\tLD\tL,C\n"
		+ "\tLD\tH,00H\n"
		+ "\tRET\n"
		+ "X_JOY_1:\n"
		+ "\tCP\t01H\n"
		+ "\tJR\tNZ,X_JOY_2\n"
		+ "\tCALL\t0FFBBH\n"
		+ "\tLD\tL,B\n"
		+ "\tLD\tH,00H\n"
		+ "\tRET\n"
		+ "X_JOY_2:\n"
		+ "\tLD\tHL,0000H\n"
		+ "\tRET\n" );
  }


  @Override
  public void appendXLocateTo( AsmCodeBuf buf )
  {
    buf.append( "XLOCATE:\n"
		+ "\tLD\tA,1FH\n"
		+ "\tCP\tE\n"
		+ "\tRET\tC\n"
		+ "\tCP\tL\n"
		+ "\tRET\tC\n"
		+ "\tPUSH\tHL\n"
		+ "\tLD\tHL,(002BH)\n"
		+ "\tLD\tA,(001FH)\n"
		+ "\tLD\t(HL),A\n"
		+ "\tPOP\tHL\n"
		+ "\tEX\tDE,HL\n"
		+ "\tADD\tHL,HL\n"
		+ "\tADD\tHL,HL\n"
		+ "\tADD\tHL,HL\n"
		+ "\tADD\tHL,HL\n"
		+ "\tADD\tHL,HL\n"
		+ "\tADD\tHL,DE\n"
		+ "\tLD\tBC,0EC00H\n"
		+ "\tADD\tHL,BC\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tLD\t(HL),0FFH\n"
		+ "\tLD\t(002BH),HL\n"
		+ "\tRET\n" );
  }


  @Override
  public void appendXLPtchTo( AsmCodeBuf buf )
  {
    buf.append( "XLPTCH:\tLD\tB,A\n"
		+ "\tLD\tA,(0FFE8H)\n"
		+ "\tCP\t0C3H\n"
		+ "\tRET\tNZ\n"
		+ "\tLD\tA,B\n"
		+ "\tJP\t0FFE8H\n" );
  }


  @Override
  public void appendXOutchTo( AsmCodeBuf buf )
  {
    if( !this.xOutchAppended ) {
      buf.append( "XOUTCH:\tCP\t0AH\n"
		+ "\tRET\tZ\n"
		+ "\tRST\t20H\n"
		+ "\tDB\t00H\n"
		+ "\tRET\n" );
      this.xOutchAppended = true;
    }
  }


  /*
   * Ausgabe eines Zeilenumbruchs
   */
  @Override
  public void appendXOutnlTo( AsmCodeBuf buf )
  {
    buf.append( "XOUTNL:\tLD\tA,0DH\n" );
    if( this.xOutchAppended ) {
      buf.append( "\tJP\tXOUTCH\n" );
    } else {
      appendXOutchTo( buf );
    }
  }


  /*
   * Setzen eines Pixels ohne Beruecksichtigung des eingestellten Stiftes
   * bei gleichzeitigem Test, ob dieser schon gesetzt ist
   * Parameter:
   *   DE: X-Koordinate
   *   HL: Y-Koordinate
   * Rueckgabe:
   *   CY=1: Pixel bereits gesetzt oder ausserhalb des sichtbaren Bereichs
   */
  @Override
  public void appendXPaintTo( AsmCodeBuf buf, BasicCompiler compiler )
  {
    buf.append( "XPAINT:\tCALL\tX_PINFO\n"
		+ "\tRET\tC\n"
		+ "\tLD\tA,B\n"
		+ "\tAND\tC\n"
		+ "\tSCF\n"
		+ "\tRET\tNZ\n"
		+ "\tCALL\tX_PSET_2\n"
		+ "\tOR\tA\n"		// CY=0
		+ "\tRET\n" );
    appendXPSetTo( buf, compiler );
  }


  /*
   * Farbe eines Pixels ermitteln,
   * Die Rueckgabewerte sind identisch zur Funktion XPTEST
   *
   * Parameter:
   *   DE: X-Koordinate
   *   HL: Y-Koordinate
   * Rueckgabe:
   *   HL >= 0: Farbcode des Pixels
   *   HL=-1:   Pixel exisitiert nicht
   */
  @Override
  public void appendXPointTo( AsmCodeBuf buf, BasicCompiler compiler )
  {
    appendXPTestTo( buf, compiler );
  }


  /*
   * Zuruecksetzen eines Pixels
   * Parameter:
   *   DE: X-Koordinate
   *   HL: Y-Koordinate
   */
  @Override
  public void appendXPResTo( AsmCodeBuf buf, BasicCompiler compiler )
  {
    buf.append( "XPRES:\tCALL\tX_PINFO\n"
		+ "\tRET\tC\n" );
    if( this.usesX_M_PEN ) {
      buf.append( "\tJR\tX_PSET_1\n" );
    } else {
      buf.append( "\tLD\tA,C\n"
		+ "\tCPL\n"
		+ "\tAND\tB\n"
		+ "\tJR\tX_PSET_3\n" );
    }
    appendXPSetTo( buf, compiler );
  }


  /*
   * Setzen eines Pixels unter Beruecksichtigung des eingestellten Stiftes
   * Parameter:
   *   DE: X-Koordinate
   *   HL: Y-Koordinate
   */
  @Override
  public void appendXPSetTo( AsmCodeBuf buf, BasicCompiler compiler )
  {
    if( !this.xpsetAppended ) {
      buf.append( "XPSET:\tCALL\tX_PINFO\n"
		+ "\tRET\tC\n" );
      if( this.usesX_M_PEN ) {
	buf.append( "\tLD\tA,(X_M_PEN)\n"
                + "\tDEC\tA\n"
                + "\tJR\tZ,X_PSET_2\n"		// Stift 1 (Normal)
                + "\tDEC\tA\n"
                + "\tJR\tZ,X_PSET_1\n"		// Stift 2 (Loeschen)
                + "\tDEC\tA\n"
                + "\tRET\tNZ\n"
		+ "\tLD\tA,B\n"			// Stift 3 (XOR-Mode)
		+ "\tXOR\tC\n"
		+ "\tJR\tX_PSET_3\n"
		+ "X_PSET_1:\n"			// Pixel loeschen
		+ "\tLD\tA,C\n"
		+ "\tCPL\n"
		+ "\tAND\tB\n"
		+ "\tJR\tX_PSET_3\n" );
      }
      buf.append( "X_PSET_2:\n"			// Pixel setzen
		+ "\tLD\tA,B\n"
		+ "\tOR\tC\n"
		+ "X_PSET_3:\n"
		+ "\tEX\tDE,HL\n"
		+ "\tLD\tB,0\n"
		+ "\tLD\tC,A\n"
		+ "\tLD\tHL,X_PSET_TAB\n"
		+ "\tADD\tHL,BC\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tLD\t(DE),A\n"
		+ "\tRET\n"
      // Umcodierungstabelle von Bitmuster zu Zeichen
		+ "X_PSET_TAB:\n"
		+ "\tDB\t20H,0B3H,0B2H,0B7H,0B0H,0B4H,0B8H,0BBH\n"
		+ "\tDB\t0B1H,0B9H,0B5H,0BAH,0B6H,0BCH,0BDH,0FFH\n" );
      appendPixUtilTo( buf, compiler );
      this.xpsetAppended = true;
    }
  }


  /*
   * Testen eines Pixels
   * Parameter:
   *   DE: X-Koordinate
   *   HL: Y-Koordinate
   * Rueckgabe:
   *   HL=0:  Pixel nicht gesetzt
   *   HL=1:  Pixel gesetzt
   *   HL=-1: Pixel existiert nicht
   */
  @Override
  public void appendXPTestTo( AsmCodeBuf buf, BasicCompiler compiler )
  {
    if( !this.xptestAppended ) {
      buf.append( "XPOINT:\n"
		+ "XPTEST:\tCALL\tX_PINFO\n"
		+ "\tJR\tC,X_PTEST_1\n"
		+ "\tLD\tA,B\n"
		+ "\tAND\tC\n"
		+ "\tLD\tHL,0000H\n"
		+ "\tRET\tZ\n"
		+ "\tINC\tHL\n"
		+ "\tRET\n"
		+ "X_PTEST_1:\n"
		+ "\tLD\tHL,0FFFFH\n"
		+ "\tRET\n" );
      appendPixUtilTo( buf, compiler );
      this.xptestAppended = true;
    }
  }


  @Override
  public int get100msLoopCount()
  {
    return 55;
  }


  @Override
  public String[] getBasicTargetNames()
  {
    return new String[] { BASIC_TARGET_NAME };
  }


  @Override
  public int getCompatibilityLevel( EmuSys emuSys )
  {
    int rv = 0;
    if( emuSys != null ) {
      if( emuSys instanceof Z1013 ) {
        rv = 2;
      }
    }
    return rv;
  }


  @Override
  public int getDefaultBegAddr()
  {
    return 0x0100;
  }


  @Override
  public int getGideIOBaseAddr()
  {
    return 0x80;
  }


  @Override
  public String getStartCmd( EmuSys emuSys, String appName, int begAddr )
  {
    String rv = null;
    if( (emuSys != null) && (begAddr >= 0) ) {
      if( emuSys instanceof Z1013 ) {
        rv = String.format( "J %04X", begAddr );
      }
    }
    return rv;
  }


  @Override
  public int[] getVdipBaseIOAddresses()
  {
    return new int[] { 0xDC, 0xFC };
  }


  @Override
  public void reset()
  {
    super.reset();
    this.pixUtilAppended    = false;
    this.xGetCrsPosAppended = false;
    this.xpsetAppended      = false;
    this.xptestAppended     = false;
  }


  @Override
  public boolean supportsGraphics()
  {
    return true;
  }


  @Override
  public boolean supportsXCURSOR()
  {
    return true;
  }


  @Override
  public boolean supportsXJOY()
  {
    return true;
  }


  @Override
  public boolean supportsXLOCATE()
  {
    return true;
  }


  @Override
  public boolean supportsXLPTCH()
  {
    return true;
  }


  @Override
  public boolean supportsXTIME()
  {
    return true;
  }


  @Override
  public String toString()
  {
    return "Z1013";
  }


	/* --- Hilfsfunktionen --- */

  protected void appendCheckGraphicScreenTo(
					AsmCodeBuf    buf,
					BasicCompiler compiler )
  {
    // leer
  }


  protected void appendPixUtilTo( AsmCodeBuf buf, BasicCompiler compiler )
  {
    if( !this.pixUtilAppended ) {
      /*
       * Pruefen der Parameter und
       * ermitteln von Informationen zu einem Pixel
       *
       * Parameter:
       *   DE: X-Koordinate (0...63)
       *   HL: Y-Koordinate (0...63)
       *
       * Rueckgabe:
       *   CY=1: Pixel ausserhalb des gueltigen Bereichs
       *   B:    Aktuelles Bitmuster (gesetzte Pixel) in der Speicherzelle,
       *         Bitanordnung innerhalb einer Zeichenposition:
       *           +---+
       *           |2 3|
       *           |0 1|
       *           +---+
       *   C:    Bitmuster mit einem gesetzten Bit,
       *         dass das Pixel in der Speicherzelle beschreibt
       *   HL:   Speicherzelle, in der sich das Pixel befindet
       */
      buf.append( "X_PINFO:\n" );
      appendCheckGraphicScreenTo( buf, compiler );
      buf.append( "X_PINFO_1:\n"
		+ "\tLD\tA,D\n"
		+ "\tOR\tH\n"
		+ "\tJR\tNZ,X_PINFO_4\n"
		+ "\tLD\tA,3FH\n"
		+ "\tCP\tE\n"
		+ "\tRET\tC\n"
		+ "\tCP\tL\n"
		+ "\tRET\tC\n"
		+ "\tLD\tA,01H\n"
		+ "\tSRL\tL\n"
		+ "\tJR\tNC,X_PINFO_2\n"
		+ "\tSLA\tA\n"
		+ "\tSLA\tA\n"
		+ "X_PINFO_2:\n"
		+ "\tSRL\tE\n"
		+ "\tJR\tNC,X_PINFO_3\n"
		+ "\tSLA\tA\n"
		+ "X_PINFO_3:\n"
		+ "\tADD\tHL,HL\n"
		+ "\tADD\tHL,HL\n"
		+ "\tADD\tHL,HL\n"
		+ "\tADD\tHL,HL\n"
		+ "\tADD\tHL,HL\n"
		+ "\tEX\tDE,HL\n"
		+ "\tLD\tBC,0EFE0H\n"
		+ "\tADD\tHL,BC\n"
		+ "\tLD\tC,A\n"
		+ "\tXOR\tA\n"
		+ "\tSBC\tHL,DE\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tLD\tB,0FH\n"
		+ "\tCP\t0EH\n"
		+ "\tCCF\n"
		+ "\tRET\tNC\n"
		+ "\tCP\t0FFH\n"	// 0FFh: 4 gesetzte Pixel
		+ "\tRET\tZ\n"
		+ "\tLD\tB,0\n"
		+ "\tSUB\t0B0H\n"	// < 0B0h: 4 nicht gesetzte Pixel
		+ "\tCCF\n"
		+ "\tRET\tNC\n"
		+ "\tCP\t0EH\n"		// >= 0BEh: 4 nicht gesetzte Pixel
		+ "\tRET\tNC\n"
		+ "\tPUSH\tHL\n"
		+ "\tLD\tHL,X_PINFO_TAB\n"
		+ "\tLD\tD,0\n"
		+ "\tLD\tE,A\n"
		+ "\tADD\tHL,DE\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tPOP\tHL\n"
		+ "\tLD\tB,A\n"
		+ "\tOR\tA\n"
		+ "\tRET\n"
		+ "X_PINFO_4:\n"
		+ "\tSCF\n"
		+ "\tRET\n"
      // Umcodierungstabelle von (Zeichen - 0B0H) zu Bitmuster
		+ "X_PINFO_TAB:\n"
		+ "\tDB\t04H,08H,02H,01H,05H,0AH,0CH,03H\n"
		+ "\tDB\t06H,09H,0BH,07H,0DH,0EH\n" );
      this.pixUtilAppended = true;
    }
  }
}
