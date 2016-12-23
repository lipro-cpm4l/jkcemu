/*
 * (c) 2012-2016 Jens Mueller
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
import jkcemu.emusys.AC1;
import jkcemu.emusys.LLC2;
import jkcemu.programming.basic.AbstractTarget;
import jkcemu.programming.basic.AsmCodeBuf;
import jkcemu.programming.basic.BasicCompiler;
import jkcemu.programming.basic.BasicOptions;


public class SCCHTarget extends AbstractTarget
{
  public static final String BASIC_TARGET_NAME = "TARGET_SCCH";


  public SCCHTarget()
  {
    setNamedValue( "JOYST_LEFT", 0x04 );
    setNamedValue( "JOYST_RIGHT", 0x08 );
    setNamedValue( "JOYST_DOWN", 0x02 );
    setNamedValue( "JOYST_UP", 0x01 );
    setNamedValue( "JOYST_BUTTON1", 0x10 );
  }


  @Override
  public void appendExitTo( AsmCodeBuf buf )
  {
    buf.append( "\tJP\t07FDH\n" );
  }


  @Override
  public void appendHCharTo( AsmCodeBuf buf )
  {
    buf.append( "\tLD\tHL,0020H\n" );
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
  public void appendPrologTo(
			AsmCodeBuf    buf,
			BasicCompiler compiler,
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
	    buf.append( "\tJR\t" );
	    buf.append( BasicCompiler.START_LABEL );
	    buf.append( "\n"
			+ "\tDB\t00H,09H,\'" );
	    buf.append( ch );
	    buf.append( "\',0DH\n" );
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
  public void appendWCharTo( AsmCodeBuf buf )
  {
    buf.append( "\tLD\tHL,0040H\n" );
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
		+ "\tCALL\t0EB4H\n"
		+ "\tLD\tL,A\n"
		+ "\tLD\tH,00H\n"
		+ "\tRET\n"
		+ "XJOY1:\tLD\tHL,0000H\n"
		+ "\tRET\n" );
  }


  @Override
  public void appendXLocateTo( AsmCodeBuf buf )
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
  public void appendXLPtchTo( AsmCodeBuf buf )
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
  public void appendXOutchTo( AsmCodeBuf buf )
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
  public void appendXOutnlTo( AsmCodeBuf buf )
  {
    buf.append( "XOUTNL:\tLD\tA,0DH\n"
			+ "\tJP\t0010H\n" );
  }


  @Override
  public int get100msLoopCount()
  {
    return 58;
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
      if( emuSys instanceof AC1 ) {
	rv = 2;
	if( ((AC1) emuSys).emulates2010Mode()
	    || ((AC1) emuSys).emulatesSCCHMode() )
	{
	  rv = 3;
	}
      } else if( emuSys instanceof LLC2 ) {
	rv = 3;
      }
    }
    return rv;
  }


  @Override
  public int getDefaultBegAddr()
  {
    return 0x2000;
  }


  @Override
  public int getMaxAppNameLen()
  {
    return 1;
  }


  @Override
  public String getStartCmd( EmuSys emuSys, String appName, int begAddr )
  {
    String rv = null;
    if( (emuSys != null) && (begAddr >= 0) ) {
      if( (emuSys instanceof AC1) || (emuSys instanceof LLC2) ) {
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
