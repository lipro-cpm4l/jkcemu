/*
 * (c) 2012-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * KC85-spezifische Code-Erzeugung des BASIC-Compilers
 */

package jkcemu.programming.basic.target;

import java.lang.*;
import java.util.Set;
import jkcemu.base.EmuSys;
import jkcemu.emusys.KC85;
import jkcemu.programming.basic.*;


public class KC85Target extends AbstractTarget
{
  public static final String BASIC_TARGET_NAME = "TARGET_KC85";

  protected boolean needsFullWindow;
  protected boolean pixUtilAppended;
  protected boolean xpsetAppended;

  private boolean usesXCOLOR;
  private boolean usesXINK;
  private boolean usesXPAPER;
  private boolean usesJoystick;
  private boolean usesM052;
  private boolean xpresAppended;


  public KC85Target()
  {
    setNamedValue( "GRAPHICSCREEN", 0 );
    setNamedValue( "BLACK", 0 );
    setNamedValue( "BLINKING", 0x10 );
    setNamedValue( "BLUE", 0x01 );
    setNamedValue( "CYAN", 0x05 );
    setNamedValue( "GREEN", 0x04 );
    setNamedValue( "MAGENTA", 0x03 );
    setNamedValue( "RED", 0x02 );
    setNamedValue( "WHITE", 0x07 );
    setNamedValue( "YELLOW", 0x06 );
    setNamedValue( "JOYST_LEFT", 0x04 );
    setNamedValue( "JOYST_RIGHT", 0x08 );
    setNamedValue( "JOYST_DOWN", 0x02 );
    setNamedValue( "JOYST_UP", 0x01 );
    setNamedValue( "JOYST_BUTTON1", 0x20 );
    setNamedValue( "JOYST_BUTTON2", 0x10 );
  }


  @Override
  public void appendBssTo( AsmCodeBuf buf )
  {
    super.appendBssTo( buf );
    if( this.usesM052 ) {
      buf.append( "X_M_M052SLOT:\tDS\t1\n"
		+ "X_M_M052STAT:\tDS\t1\n"
		+ "X_M_M052USED:\tDS\t1\n" );
    }
  }


  @Override
  public void appendEnableVdipTo( AsmCodeBuf buf )
  {
    buf.append( "\tCALL\tX_M052\n" );
    this.usesM052 = true;
  }


  @Override
  public void appendEtcPreXOutTo( AsmCodeBuf buf )
  {
    if( this.usesXINK ) {
      buf.append( "XINK:\tLD\tA,01H\n" );
      if( this.usesXCOLOR || this.usesXPAPER ) {
	buf.append( "\tJR\tXCOLO1\n" );
      } else {
	buf.append( "\tLD\t(0B781H),A\n"
		+ "\tCALL\t0F003H\n"
		+ "\tDB\t0FH\n"
		+ "\tRET\n" );
      }
    }
    if( this.usesXPAPER ) {
      buf.append( "XPAPER:\tLD\tE,L\n"
		+ "\tLD\tA,(0B7A3H)\n"
		+ "\tSRL\tA\n"
		+ "\tSRL\tA\n"
		+ "\tSRL\tA\n"
		+ "\tLD\tL,A\n" );
      this.usesXCOLOR = true;
      // unmittelbar weiter mit XCOLOR!
    }
    if( this.usesXCOLOR ) {
      buf.append( "XCOLOR:\tLD\tA,02H\n"
		+ "XCOLO1:\tLD\t(0B781H),A\n"
		+ "\tCALL\t0F003H\n"
		+ "\tDB\t0FH\n"
		+ "\tRET\n" );
    }
    if( this.usesM052 ) {
      /*
       * Aktivieren des Moduls M052
       * Rueckgabewert:
       *   CY=0: M052 aktiv
       *   CY=1: M052 nicht gefunden
       */
      buf.append( "X_M052:\tLD\tA,(X_M_M052USED)\n"
		+ "\tOR\tA\n"
		+ "\tRET\tNZ\n"
		+ "\tLD\tBC,0880H\n"
		+ "X_M052_1:\n"
		+ "\tIN\tA,(C)\n"
		+ "\tCP\t0FDH\n"
		+ "\tJR\tZ,X_M052_2\n"
		+ "\tINC\tB\n"
		+ "\tJR\tNZ,X_M052_1\n"
		+ "\tSCF\n"
		+ "\tRET\n"			// M052 nicht gefunden, CY=1
		+ "X_M052_2:\n"
		+ "\tLD\tA,B\n"
		+ "\tLD\t(X_M_M052SLOT),A\n"
		+ "\tLD\tH,0B8H\n"
		+ "\tLD\tL,B\n"
		+ "\tLD\tA,(HL)\n"
		+ "\tLD\t(X_M_M052STAT),A\n"
		+ "\tOR\t04H\n"
		+ "\tLD\tD,A\n"
		+ "\tLD\tA,02H\n"
		+ "\tCALL\t0F003H\n"
		+ "\tDB\t26H\n"
		+ "\tLD\tA,0FFH\n"
		+ "\tLD\t(X_M_M052USED),A\n"
		+ "\tOR\tA\n"			// CY=0
		+ "\tRET\n" );
    }
  }


  @Override
  public void appendPreExitTo( AsmCodeBuf buf )
  {
    if( this.usesM052 ) {
      buf.append( "\tLD\tA,(X_M_M052USED)\n"
		+ "\tOR\tA\n"
		+ "\tJR\tZ,X_EXIT1\n"
		+ "\tLD\tA,(X_M_M052STAT)\n"
		+ "\tLD\tD,A\n"
		+ "\tLD\tA,(X_M_M052SLOT)\n"
		+ "\tLD\tL,A\n"
		+ "\tLD\tA,02H\n"
		+ "\tCALL\t0F003H\n"
		+ "\tDB\t26H\n"
		+ "X_EXIT1:\n" );
    }
  }


  @Override
  public void appendExitTo( AsmCodeBuf buf )
  {
    buf.append( "\tRET\n" );
  }


  @Override
  public void appendHCharTo( AsmCodeBuf buf )
  {
    buf.append( "\tLD\tHL,0020H\n" );
  }


  @Override
  public void appendHPixelTo( AsmCodeBuf buf )
  {
    buf.append( "\tLD\tHL,0100H\n" );
  }


  @Override
  public void appendInitTo( AsmCodeBuf buf )
  {
    super.appendInitTo( buf );
    if( this.usesJoystick ) {
      /*
       * Joystick-PIO im Modul M008/M021 programmieren,
       * und zwar so, wie es auch der Treiber im CAOS 4.5 tut
       */
      buf.append( "\tLD\tA,0CFH\n"
		+ "\tOUT\t(92H),A\n"
		+ "\tLD\tA,7FH\n"
		+ "\tOUT\t(92H),A\n" );
    }
    if( this.usesM052 ) {
      buf.append( "\tXOR\tA\n"
		+ "\tLD\t(X_M_M052SLOT),A\n"
		+ "\tLD\t(X_M_M052STAT),A\n"
		+ "\tLD\t(X_M_M052USED),A\n" );
    }
    if( this.needsFullWindow ) {
      // Fenstergroesse pruefen und ggf. auf Maximalewerte setzen
      buf.append( "\tLD\tHL,(0B79CH)\n"		// Fensterposition
		+ "\tLD\tA,H\n"
		+ "\tOR\tL\n"
		+ "\tJR\tNZ,X_IWN1\n"
		+ "\tLD\tHL,(0B79EH)\n"		// Fenstergroesse
		+ "\tLD\tA,H\n"
		+ "\tCP\t20H\n"
		+ "\tJR\tC,X_IWN1\n"
		+ "\tLD\tA,L\n"
		+ "\tCP\t28H\n"
		+ "\tJR\tNC,X_IWN2\n"
		+ "X_IWN1:\tLD\tHL,0000H\n"
		+ "\tLD\t(0B79CH),HL\n"		// Fensteranfang
		+ "\tLD\tHL,2028H\n"
		+ "\tLD\t(0B79EH),HL\n"		// Fenstergroesse
		+ "\tLD\tA,(0B79BH)\n"		// Fensternummer
		+ "\tLD\tL,A\n"			// Position im
		+ "\tLD\tH,00H\n"		// Fenstervektorspeicher
		+ "\tLD\tC,L\n"			// berechnen
		+ "\tLD\tB,H\n"
		+ "\tADD\tHL,HL\n"
		+ "\tADD\tHL,HL\n"
		+ "\tADD\tHL,BC\n"
		+ "\tADD\tHL,HL\n"
		+ "\tLD\tDE,0B99CH\n"
		+ "\tADD\tHL,DE\n"
		+ "\tLD\t(HL),00H\n"		// Fensteranfang
		+ "\tINC\tHL\n"
		+ "\tLD\t(HL),00H\n"
		+ "\tINC\tHL\n"
		+ "\tLD\t(HL),28H\n"		// Fenstergroesse
		+ "\tINC\tHL\n"
		+ "\tLD\t(HL),20H\n"		// Fenstergroesse
		+ "\tINC\tHL\n"
		+ "\tLD\tA,0CH\n"		// Fenster loeschen
		+ "\tCALL\t0F003H\n"
		+ "\tDB\t00H\n"
		+ "X_IWN2:\n" );
    }
  }


  @Override
  public void appendInputTo(
			AsmCodeBuf buf,
			boolean    xckbrk,
			boolean    xinkey,
			boolean    xinch,
			boolean    canBreakOnInput )
  {
    if( xckbrk ) {
      buf.append( "XCKBRK:\tCALL\t0F003H\n"
		+ "\tDB\t2AH\n"
		+ "\tRET\tNC\n"
		+ "\tJR\tXBREAK\n" );
    }
    if( xinkey ) {
      buf.append( "XINKEY:\tCALL\t0F003H\n"
		+ "\tDB\t0EH\n" );
      if( canBreakOnInput ) {
	buf.append( "\tJR\tC,XINKE1\n"
		+ "\tXOR\tA\n"
		+ "\tRET\n"
		+ "XINKE1:\tCP\t03H\n"
		+ "\tJR\tZ,XBREAK\n"
		+ "\tRET\n" );
      } else {
	buf.append( "\tRET\tC\n"
		+ "\tXOR\tA\n"
		+ "\tRET\n" );
      }
    }
    if( xinch ) {
      buf.append( "XINCH:\tCALL\t0F003H\n"
		+ "\tDB\t16H\n" );
      if( canBreakOnInput ) {
	buf.append( "\tCP\t03H\n"
		+ "\tJR\tZ,XBREAK\n" );
      }
      buf.append( "\tRET\n" );
    }
  }


  @Override
  public void appendPrologTo(
			AsmCodeBuf    buf,
			BasicCompiler compiler,
			String        appName )
  {
    boolean done = false;
    if( appName != null ) {
      if( !appName.isEmpty() ) {
	buf.append( "\tDB\t7FH,7FH\n" );
	buf.appendStringLiteral( appName, "01H" );
	done = true;
      }
    }
    if( !done ) {
      compiler.putWarning(
		"Programm kann auf dem Zielsystem nicht aufgerufen werden,"
				+ " da der Programmname leer ist." );
    }
    buf.append( "\tENT\n" );
  }


  @Override
  public void appendWCharTo( AsmCodeBuf buf )
  {
    buf.append( "\tLD\tHL,0028H\n" );
  }


  @Override
  public void appendWPixelTo( AsmCodeBuf buf )
  {
    buf.append( "\tLD\tHL,0140H\n" );
  }


  @Override
  public void appendXClsTo( AsmCodeBuf buf )
  {
    buf.append( "XCLS:\tLD\tA,0CH\n"
		+ "\tCALL\t0F003H\n"
		+ "\tDB\t00H\n"
		+ "\tRET\n" );
    this.needsFullWindow = true;
  }


  /*
   * Gleichzeitiges Setzen der Vorder- und Hintergrundfarbe
   * Parameter:
   *   HL: Vordergrundfarbe
   *   DE: Hintergrundfarbe
   */
  @Override
  public void appendXColorTo( AsmCodeBuf buf )
  {
    this.usesXCOLOR = true;
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
    buf.append( "XHLINE:\tBIT\t7,B\n"
		+ "\tRET\tNZ\n"
		+ "\tPUSH\tBC\n"
		+ "\tPUSH\tDE\n"
		+ "\tPUSH\tHL\n"
		+ "\tCALL\tX_PST\n"
		+ "\tEXX\n"
		+ "\tPOP\tHL\n"
		+ "\tPOP\tDE\n"
		+ "\tPOP\tBC\n"
		+ "\tRET\tC\n"
		+ "\tLD\tH,00H\n"
		+ "XHLINE1:\n"
		+ "\tOR\tH\n"
		+ "\tLD\tH,A\n"
		+ "\tSRL\tA\n"
		+ "\tJR\tC,XHLINE2\n"
		+ "\tINC\tDE\n"
		+ "\tDEC\tBC\n"
		+ "\tBIT\t7,B\n"
		+ "\tJR\tZ,XHLINE1\n"
		+ "\tLD\tA,H\n"
		+ "\tEXX\n"
		+ "\tJR\tXPSET_A\n"
		+ "XHLINE2:\n"
		+ "\tLD\tA,H\n"
		+ "\tEXX\n"
		+ "\tCALL\tXPSET_A\n"
		+ "\tEXX\n"
		+ "\tINC\tDE\n"
		+ "\tDEC\tBC\n"
		+ "\tLD\tH,00H\n"
		+ "\tJR\tXHLINE\n" );
    appendXPSetTo( buf, compiler );
  }


  @Override
  public void appendXInkTo( AsmCodeBuf buf )
  {
    this.usesXINK = true;
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
    buf.append( "XJOY:\tLD\tA,H\n"
		+ "\tOR\tL\n"
		+ "\tJR\tNZ,XJOY1\n"
		+ "\tIN\tA,(90H)\n"
		+ "\tCPL\n"
		+ "\tAND\t3FH\n"
		+ "\tLD\tL,A\n"
		+ "\tLD\tH,00H\n"
		+ "\tRET\n"
		+ "XJOY1:\tLD\tHL,0000H\n"
		+ "\tRET\n" );
    this.usesJoystick = true;
  }


  @Override
  public void appendXLocateTo( AsmCodeBuf buf )
  {
    buf.append( "XLOCATE:\n"
		+ "\tLD\tH,E\n"
		+ "\tLD\t(0B7A0H),HL\n"
		+ "\tRET\n" );
    this.needsFullWindow = true;
  }


  @Override
  public void appendXLPtchTo( AsmCodeBuf buf )
  {
    buf.append( "XLPTCH:\tCALL\t0F003H\n"
		+ "\tDB\t02H\n"
		+ "\tRET\n" );
  }


  @Override
  public void appendXOutchTo( AsmCodeBuf buf )
  {
    if( !this.xoutchAppended ) {
      buf.append( "XOUTCH:\tCALL\t0F003H\n"
		+ "\tDB\t24H\n"
		+ "\tRET\n" );
      this.xoutchAppended = true;
    }
  }


  /*
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
  @Override
  public void appendXPaintTo( AsmCodeBuf buf, BasicCompiler compiler )
  {
    buf.append( "XPAINT_LEFT:\n"
		+ "\tLD\tDE,(PAINT_M_X)\n"
		+ "\tLD\tA,D\n"
		+ "\tOR\tE\n"
		+ "\tJR\tZ,XPAINT_LEFT5\n"
		+ "\tDEC\tDE\n"
		+ "XPAINT_LEFT1:\n"
		+ "\tLD\tHL,(PAINT_M_Y)\n"
		+ "\tPUSH\tDE\n"
		+ "\tCALL\tX_PST\n"
		+ "\tPUSH\tHL\n"
		+ "\tEXX\n"
		+ "\tPOP\tHL\n"
		+ "\tPOP\tDE\n"
		+ "\tJR\tC,XPAINT_LEFT4\n"
		+ "\tLD\tC,A\n"
		+ "\tLD\tB,(HL)\n"
		+ "\tLD\tA,B\n"
		+ "XPAINT_LEFT2:\n"
		+ "\tAND\tC\n"
		+ "\tJR\tNZ,XPAINT_LEFT3\n"
		+ "\tLD\tA,B\n"
		+ "\tOR\tC\n"
		+ "\tLD\tB,A\n"
		+ "\tDEC\tDE\n"
		+ "\tSLA\tC\n"
		+ "\tJR\tNC,XPAINT_LEFT2\n"
		+ "\tLD\t(HL),B\n"
		+ "\tEXX\n"
		+ "\tCALL\tXPSET_WR_COLOR\n"
		+ "\tEXX\n"
		+ "\tJR\tXPAINT_LEFT1\n"
		+ "XPAINT_LEFT3:\n"
                + "\tLD\t(HL),B\n"
		+ "\tEXX\n"
		+ "\tCALL\tXPSET_WR_COLOR\n"
		+ "\tEXX\n"
		+ "XPAINT_LEFT4:\n"
		+ "\tINC\tDE\n"
		+ "XPAINT_LEFT5:\n"
		+ "\tLD\t(PAINT_M_X1),DE\n"
		+ "\tRET\n"
		+ "XPAINT_RIGHT:\n"
		+ "\tLD\tDE,(PAINT_M_X)\n"
		+ "\tLD\tHL,(PAINT_M_Y)\n"
		+ "\tPUSH\tDE\n"
		+ "\tCALL\tX_PST\n"
		+ "\tPUSH\tHL\n"
		+ "\tEXX\n"
		+ "\tPOP\tHL\n"
		+ "\tPOP\tDE\n"
		+ "\tRET\tC\n"
		+ "\tLD\tC,A\n"
		+ "\tLD\tB,(HL)\n"
		+ "\tAND\tB\n"
		+ "\tSCF\n"
		+ "\tRET\tNZ\n"
		+ "\tJR\tXPAINT_RIGHT2\n"
		+ "XPAINT_RIGHT1:\n"
		+ "\tAND\tC\n"
		+ "\tJR\tNZ,XPAINT_RIGHT3\n"
		+ "XPAINT_RIGHT2:\n"
		+ "\tLD\tA,B\n"
		+ "\tOR\tC\n"
		+ "\tLD\tB,A\n"
		+ "\tINC\tDE\n"
		+ "\tSRL\tC\n"
		+ "\tJR\tNC,XPAINT_RIGHT1\n"
		+ "\tLD\t(HL),B\n"
		+ "\tEXX\n"
		+ "\tCALL\tXPSET_WR_COLOR\n"
		+ "\tEXX\n"
		+ "\tLD\tHL,(PAINT_M_Y)\n"
		+ "\tPUSH\tDE\n"
		+ "\tCALL\tX_PST\n"
		+ "\tPUSH\tHL\n"
		+ "\tEXX\n"
		+ "\tPOP\tHL\n"
		+ "\tPOP\tDE\n"
		+ "\tJR\tC,XPAINT_RIGHT4\n"
		+ "\tLD\tC,A\n"
		+ "\tLD\tB,(HL)\n"
		+ "\tAND\tB\n"
		+ "\tJR\tNZ,XPAINT_RIGHT4\n"
		+ "\tJR\tXPAINT_RIGHT2\n"
		+ "XPAINT_RIGHT3:\n"
		+ "\tLD\t(HL),B\n"
		+ "\tEXX\n"
		+ "\tCALL\tXPSET_WR_COLOR\n"
		+ "\tEXX\n"
		+ "XPAINT_RIGHT4:\n"
		+ "\tDEC\tDE\n"
		+ "\tLD\t(PAINT_M_X2),DE\n"
		+ "\tOR\tA\n"			// CY=0
		+ "\tRET\n" );
    appendXPSetTo( buf, compiler );
  }


  @Override
  public void appendXPaperTo( AsmCodeBuf buf )
  {
    this.usesXPAPER = true;
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
  @Override
  public void appendXPointTo( AsmCodeBuf buf, BasicCompiler compiler )
  {
    buf.append( "XPOINT:\tCALL\tX_PST\n"
		+ "\tJR\tC,XPOINT5\n"
		+ "\tLD\tB,A\n"
		+ "\tLD\tA,D\n"
		+ "\tOR\tE\n"
		+ "\tJR\tNZ,XPOINT1\n"
    // Farbbyte lesen bei KC85/4..5
		+ "\tDB\t0DDH,0CBH,01H,0CFH\t;SET 1,(IX+01H),A\n"
		+ "\tOUT\t(84H),A\n"
		+ "\tLD\tC,(HL)\n"
		+ "\tDB\t0DDH,0CBH,01H,8FH\t;RES 1,(IX+01H),A\n"
		+ "\tOUT\t(84H),A\n"
		+ "\tJR\tXPOINT2\n"
    // Farbbyte lesen bei KC85/2..3
		+ "XPOINT1:\n"
		+ "\tLD\tA,(DE)\n"
		+ "\tLD\tC,A\n"
    // Pixel auswerten
		+ "XPOINT2:\n"
		+ "\tLD\tA,B\n"
		+ "\tAND\t(HL)\n"
		+ "\tLD\tA,C\n"
		+ "\tJR\tZ,XPOINT3\n"
		+ "\tSRL\tA\n"
		+ "\tSRL\tA\n"
		+ "\tSRL\tA\n"
		+ "\tAND\t1FH\n"
		+ "\tJR\tXPOINT4\n"
		+ "XPOINT3:\n"
		+ "\tAND\t07H\n"
		+ "XPOINT4:\n"
		+ "\tLD\tL,A\n"
		+ "\tLD\tH,00H\n"
		+ "\tRET\n"
		+ "XPOINT5:\n"
		+ "\tLD\tHL,0FFFFH\n"
		+ "\tRET\n" );
    appendPixUtilTo( buf );
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
    buf.append( "XPRES:\tCALL\tX_PST\n"
		+ "\tRET\tC\n"
		+ "\tCPL\n"
		+ "\tAND\t(HL)\n"
		+ "\tJR\tXPSET_WR_A\n" );
    appendXPSetTo( buf, compiler );
    appendPixUtilTo( buf );
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
      buf.append( "XPSET:\tCALL\tX_PST\n"
		+ "\tRET\tC\n"
		+ "XPSET_A:\n" );
      if( this.usesX_M_PEN ) {
	buf.append( "\tLD\tB,A\n"
		+ "\tLD\tA,(X_M_PEN)\n"
		+ "\tDEC\tA\n"
		+ "\tJR\tZ,XPSET2\n"		// Stift 1 (Normal)
		+ "\tDEC\tA\n"
		+ "\tJR\tZ,XPSET1\n"		// Stift 2 (Loeschen)
		+ "\tDEC\tA\n"
		+ "\tRET\tNZ\n"
		+ "\tLD\tA,(HL)\n"		// Stift 3 (XOR-Mode)
		+ "\tXOR\tB\n"
		+ "\tJR\tXPSET_WR_A\n"
		+ "XPSET1:\tLD\tA,B\n"		// Pixel loeschen
		+ "\tCPL\n"
		+ "\tAND\t(HL)\n"
		+ "\tJR\tXPSET_WR_A\n"
		+ "XPSET2:\tLD\tA,B\n" );	// Pixel setzen
      }
      buf.append( "XPSET_OR_A:\n"
		+ "\tOR\t(HL)\n"
		+ "XPSET_WR_A:\n"
		+ "\tLD\t(HL),A\n"
      // Farbbyte schreiben
		+ "XPSET_WR_COLOR:\n"
		+ "\tLD\tA,D\n"
		+ "\tOR\tE\n"
		+ "\tJR\tNZ,XPSET3\n"
      /*
       * bei KC85/4..5 Farbebene einschalten, Farbe setzen
       * und wieder Pixelebene einschalten
       */
		+ "\tDB\t0DDH,0CBH,01H,0CFH\t;SET 1,(IX+01H),A\n"
		+ "\tOUT\t(84H),A\n"
		+ "\tLD\tA,(0B7A3H)\n"
		+ "\tLD\t(HL),A\n"
		+ "\tDB\t0DDH,0CBH,01H,8FH\t;RES 1,(IX+01H),A\n"
		+ "\tOUT\t(84H),A\n"
		+ "\tRET\n"
      /*
       * KC85/2..3: Zur Sicherheit pruefen,
       * ob DE eine Adresse im Farbspeicher enthaelt
       */
		+ "XPSET3:\tLD\tA,D\n"
		+ "\tCP\t0A8H\n"
		+ "\tRET\tC\n"
		+ "\tCP\t0B2H\n"
		+ "\tRET\tNC\n"
		+ "\tLD\tA,(0B7A3H)\n"
		+ "\tLD\t(DE),A\n"
		+ "\tRET\n" );
      appendPixUtilTo( buf );
      this.needsFullWindow = true;
      this.xpsetAppended   = true;
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
   *   HL=-1: Pixel exisistiert nicht
   */
  @Override
  public void appendXPTestTo( AsmCodeBuf buf, BasicCompiler compiler )
  {
    buf.append( "XPTEST:\tCALL\tX_PST\n"
		+ "\tJR\tC,X_PTEST1\n"
		+ "\tAND\t(HL)\n"
		+ "\tLD\tHL,0000H\n"
		+ "\tRET\tZ\n"
		+ "\tINC\tHL\n"
		+ "\tRET\n"
		+ "X_PTEST1:\n"
		+ "\tLD\tHL,0FFFFH\n"
		+ "\tRET\n" );
    appendPixUtilTo( buf );
  }


  @Override
  public int get100msLoopCount()
  {
    return 50;
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
      if( emuSys instanceof KC85 ) {
	rv = 3;
      }
    }
    return rv;
  }


  @Override
  public int getDefaultBegAddr()
  {
    return 0x0300;
  }


  @Override
  public int getMaxAppNameLen()
  {
    return 38;
  }


  @Override
  public String getStartCmd( EmuSys emuSys, String appName, int begAdAdr )
  {
    String rv = null;
    if( emuSys != null ) {
      if( emuSys instanceof KC85 ) {
	rv = appName;
      }
    }
    return appName;
  }


  @Override
  public int[] getVdipBaseIOAddresses()
  {
    return new int[] { 0x2C };
  }


  @Override
  public boolean needsEnableVdip()
  {
    return true;
  }


  @Override
  public void reset()
  {
    super.reset();
    this.needsFullWindow = false;
    this.usesJoystick    = false;
    this.usesM052        = false;
    this.usesXCOLOR      = false;
    this.usesXINK        = false;
    this.usesXPAPER      = false;
    this.pixUtilAppended = false;
    this.xpsetAppended   = false;
  }


  @Override
  public boolean supportsColors()
  {
    return true;
  }


  @Override
  public boolean supportsGraphics()
  {
    return true;
  }


  @Override
  public boolean supportsXCLS()
  {
    return true;
  }


  @Override
  public boolean supportsXHLINE()
  {
    return true;
  }


  @Override
  public boolean supportsXJOY()
  {
    return true;
  }


  @Override
  public boolean supportsXLOCAT()
  {
    return true;
  }


  @Override
  public boolean supportsXLPTCH()
  {
    return true;
  }


  @Override
  public boolean supportsXPAINT_LEFT_RIGHT()
  {
    return true;
  }


  @Override
  public String toString()
  {
    return "KC85/2..5, HC900";
  }


	/* --- Hilfsfunktionen --- */

  protected void appendPixUtilTo( AsmCodeBuf buf )
  {
    if( !this.pixUtilAppended ) {
      buf.append(
	/*
	 * Pruefen der Parameter, ermitteln von Informationen zu einem Pixel
	 * und bei KC85/4..5 einschalten der Pixelebene
	 *
	 * Parameter:
	 *   DE: X-Koordinate (0...319)
	 *   HL: Y-Koordinate (0...255)
	 * Rueckgabe:
	 *   CY=1: Pixel ausserhalb des gueltigen Bereichs
	 *   A:    Bitmuster mit einem gesetzten Bit,
	 *         dass das Pixel in der Speicherzelle beschreibt
	 *   C:    84h (nur bei DE=0)
	 *   DE:   Adresse im Farbspeicher (KC85/2..3) oder 0 (KC85/4..5)
	 *   HL:   Adresse im Pixelspeicher
	 */
		"X_PST:\tLD\tA,H\n"
		+ "\tOR\tA\n"
		+ "\tSCF\n"
		+ "\tRET\tNZ\n"
		+ "\tLD\tA,D\n"
		+ "\tOR\tA\n"
		+ "\tJR\tZ,X_PST1\n"
		+ "\tCP\t02H\n"
		+ "\tCCF\n"
		+ "\tRET\tC\n"
		+ "\tLD\tA,3FH\n"
		+ "\tCP\tE\n"
		+ "\tRET\tC\n"
		+ "X_PST1:\tLD\tA,L\n"
		+ "\tCPL\n"
		+ "\tLD\tH,A\n"
		+ "\tLD\tA,E\n"
		+ "\tAND\t07H\n"
		+ "\tLD\tB,A\n"
		+ "\tLD\tC,80H\n"
		+ "\tJR\tZ,X_PST3\n"
		+ "X_PST2:\tSRL\tC\n"
		+ "\tDJNZ\tX_PST2\n"
		+ "X_PST3:\tPUSH\tBC\n"
		+ "\tSRL\tD\n"
		+ "\tRR\tE\n"
		+ "\tSRL\tE\n"
		+ "\tSRL\tE\n"
		+ "\tLD\tL,E\n"
		+ "\tLD\tDE,0000H\n"
		+ "\tCALL\t0F003H\n"
		+ "\tDB\t34H\n"
		+ "\tPOP\tBC\n"
      // bei KC85/4..5 Pixelebene einschalten
		+ "\tLD\tA,D\n"
		+ "\tOR\tE\n"			// CY=0
		+ "\tLD\tA,C\n"
		+ "\tRET\tNZ\n"			// KC85/2..3
		+ "\tDB\t0DDH,0CBH,01H,88H\t;RES 1,(IX+01H),B\n"
		+ "\tLD\tC,84H\n"
		+ "\tOUT\t(C),B\n"
		+ "\tRET\n" );
      this.pixUtilAppended = true;
    }
  }
}
