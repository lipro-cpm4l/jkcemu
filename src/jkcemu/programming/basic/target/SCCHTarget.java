/*
 * (c) 2012-2013 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * SCCH-spezifische Code-Erzeugung des BASIC-Compilers
 *
 * Unter SCCH versteht man einen AC1 oder LLC2 mit einem
 * Monitorprogramm des Studio Computer Clubs Halle (SCCH)
 * bzw. einer kompatiblen Weiterentwicklung davon.
 */

package jkcemu.programming.basic.target;

import java.lang.*;
import jkcemu.base.EmuSys;
import jkcemu.emusys.*;
import jkcemu.programming.basic.*;


public class SCCHTarget extends AbstractTarget
{
  public SCCHTarget()
  {
    // leer
  }


  @Override
  public void appendExit( AsmCodeBuf buf )
  {
    buf.append( "\tJP\t07FDH\n" );
  }


  @Override
  public void appendHChar( AsmCodeBuf buf )
  {
    buf.append( "\tLD\tHL,0020H\n" );
  }


  @Override
  public void appendInput(
			AsmCodeBuf buf,
			boolean    xckbrk,
			boolean    xinkey,
			boolean    xinch,
			boolean    canBreakOnInput )
  {
    if( xckbrk ) {
      buf.append( "XCKBRK:\n" );
    }
    if( xckbrk || xinkey) {
      buf.append( "XINKEY:\tCALL\t07FAH\n"
		+ "\tAND\t7FH\n" );
      if( canBreakOnInput ) {
	buf.append( "\tCP\t03H\n"
		+ "\tJR\tZ,XBREAK\n" );
      }
      buf.append( "\tRET\n" );
    }
    if( xinch ) {
      if( canBreakOnInput ) {
	buf.append( "XINCH:\tCALL\t00008H\n"
		+ "\tCP\t03H\n"
		+ "\tJR\tZ,XBREAK\n"
		+ "\tRET\n" );
      } else {
	buf.append( "XINCH:\tJP\t0008H\n" );
      }
    }
  }


  @Override
  public void appendProlog(
			BasicCompiler compiler,
			AsmCodeBuf    buf,
			String        appName )
  {
    if( appName != null ) {
      int len = appName.length();
      if( len > 0 ) {
	boolean done = false;
	if( len == 1 ) {
	  char ch = appName.charAt( 0 );
	  if( ((ch >= '0') && (ch <= '9'))
	      || ((ch >= 'A') && (ch <= 'Z'))
	      || ((ch >= 'a') && (ch <= 'z')) )
	  {
	    buf.append( "\tJR\tXSTART\n"
			+ "\tDB\t00H,09H,\'" );
	    buf.append( ch );
	    buf.append( "\',0DH\n"
			+ "XSTART:" );
	    done = true;
	  }
	}
	if( !done && !appName.equals( BasicOptions.DEFAULT_APP_NAME ) ) {
	  compiler.putWarning( "Warnung: Applikationsname ignoriert"
		+ " (nur ein Buchstabe oder eine Ziffer erlaubt)\n"
		+ "Aufruf des Programms auf dem Zielsystem nur \u00FCber"
		+ " die Startadresse m\u00F6glich" );
	}
      }
    }
  }


  @Override
  public void appendWChar( AsmCodeBuf buf )
  {
    buf.append( "\tLD\tHL,0040H\n" );
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
   *         Bit 4: Aktionsknopf
   */
  public void appendXJOY( AsmCodeBuf buf )
  {
    buf.append( "XJOY:\tLD\tA,H\n"
		+ "\tOR\tL\n"
		+ "\tJR\tNZ,XJOY1\n"
		+ "\tCALL\t0EB4H\n"
		+ "\tLD\tB,A\n"
		+ "\tSRL\tA\n"
		+ "\tSRL\tA\n"
		+ "\tAND\t03H\n"	// links und rechts maskieren
		+ "\tLD\tC,A\n"
		+ "\tLD\tA,B\n"
		+ "\tSLA\tA\n"
		+ "\tAND\t04H\n"	// runter maskieren
		+ "\tOR\tC\n"
		+ "\tLD\tC,A\n"
		+ "\tLD\tA,B\n"
		+ "\tSLA\tA\n"
		+ "\tSLA\tA\n"
		+ "\tSLA\tA\n"
		+ "\tAND\t08H\n"	// hoch maskieren
		+ "\tOR\tC\n"
		+ "\tLD\tC,A\n"
		+ "\tLD\tA,B\n"
		+ "\tAND\t10H\n"	// Aktionsknopf maskieren
		+ "\tOR\tC\n"
		+ "\tLD\tL,A\n"
		+ "\tLD\tH,00H\n"
		+ "\tRET\n"
		+ "XJOY1:\tLD\tHL,0000H\n"
		+ "\tRET\n" );
  }


  @Override
  public void appendXLOCATE( AsmCodeBuf buf )
  {
    buf.append( "XLOCATE:\n"
		+ "\tLD\tA,D\n"
		+ "\tOR\tH\n"
		+ "\tRET\tNZ\n"
		+ "\tLD\tA,E\n"
		+ "\tCP\t64H\n"
		+ "\tRET\tNC\n"
		+ "\tLD\tA,L\n"
		+ "\tCP\t64H\n"
		+ "\tRET\tNC\n"
		+ "\tPUSH\tHL\n"
		+ "\tPUSH\tDE\n"
		+ "\tLD\tA,0EH\n"
		+ "\tRST\t10H\n"
		+ "\tPOP\tDE\n"
		+ "\tLD\tA,E\n"
		+ "\tCALL\tX_LOC1\n"
		+ "\tPOP\tHL\n"
		+ "\tLD\tA,L\n"
    // Ausgabe des in A stehenden Wertes als zweistellige Dezimalzahl
		+ "X_LOC1:\tLD\tB,0FFH\n"
		+ "X_LOC2:\tINC\tB\n"
		+ "\tSUB\t0AH\n"
		+ "\tJR\tNC,X_LOC2\n"
		+ "\tADD\tA,0AH\n"
		+ "\tPUSH\tAF\n"
		+ "\tLD\tA,B\n"
		+ "\tADD\tA,30H\n"
		+ "\tRST\t10H\n"
		+ "\tPOP\tAF\n"
		+ "\tADD\tA,30H\n"
		+ "\tRST\t10H\n"
		+ "\tRET\n" );
  }


  @Override
  public void appendXLPTCH( AsmCodeBuf buf )
  {
    buf.append( "XLPTCH:\tLD\tB,A\n"
		+ "\tLD\tA,(1821H)\n"
		+ "\tPUSH\tAF\n"
		+ "\tAND\t0FH\n"
		+ "\tOR\t20H\n"
		+ "\tLD\t(1821H),A\n"
		+ "\tLD\tA,B\n"
		+ "\tRST\t10H\n"
		+ "\tPOP\tAF\n"
		+ "\tLD\t(1821H),A\n"
		+ "\tRET\n" );
  }


  @Override
  public void appendXOUTCH( AsmCodeBuf buf )
  {
    if( !this.xoutchAppended ) {
      buf.append( "XOUTCH:\tCP\t0AH\n"
			+ "\tRET\tZ\n"
			+ "\tJP\t0010H\n" );
      this.xoutchAppended = true;
    }
  }


  /*
   * Ausgabe eines Zeilenumbruchs
   */
  @Override
  public void appendXOUTNL( AsmCodeBuf buf )
  {
    buf.append( "XOUTNL:\tLD\tA,0DH\n"
			+ "\tJP\t0010H\n" );
  }


  /*
   * Target-ID-String
   */
  @Override
  public void appendXTARID( AsmCodeBuf buf )
  {
    buf.append( "XTARID:\tDB\t\'SCCH\'\n"
		+ "\tDB\t00H\n" );
  }


  @Override
  public boolean createsCodeFor( EmuSys emuSys )
  {
    boolean rv = false;
    if( emuSys != null ) {
      if( emuSys instanceof AC1 ) {
	rv = (((AC1) emuSys).emulates2010Mode()
	      || ((AC1) emuSys).emulatesSCCHMode());
      } else if( emuSys instanceof LLC2 ) {
	rv = true;
      }
    }
    return rv;
  }


  @Override
  public int get100msLoopCount()
  {
    return 58;
  }


  @Override
  public int getDefaultBegAddr()
  {
    return 0x2000;
  }


  @Override
  public int getKCNetBaseIOAddr()
  {
    return 0xC0;
  }


  @Override
  public int[] getVdipBaseIOAddresses()
  {
    return new int[] { 0xDC, 0xFC };
  }


  @Override
  public boolean supportsAppName()
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
  public String toString()
  {
    return "SCCH (AC1-2010, AC1-SCCH, LLC2)";
  }
}
