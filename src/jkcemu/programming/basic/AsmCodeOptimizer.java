/*
 * (c) 2014-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Optimierung des vom BASIC-Compiler erzeugten Programmcodes
 *
 * Achtung! Dieser Optimierer ist speziell auf den
 * vom JKCEMU-BASIC-Compiler erzeugten Assemblercode abgestimmt
 * und kann deshalb nicht allgemein zum Optimieren
 * von beliebigen Z80-Assemblercode eingesetzt werden.
 */

package jkcemu.programming.basic;

import java.lang.*;
import java.util.*;
import jkcemu.programming.*;
import jkcemu.programming.assembler.AsmLine;


public class AsmCodeOptimizer
{
  private static final String CODE_CALL_O_GE   = "\tCALL\tO_GE\n";
  private static final String CODE_CALL_O_LT   = "\tCALL\tO_LT\n";
  private static final String CODE_CALL_SCREEN = "\tCALL\tSCREEN\n";
  private static final String CODE_SCREEN_0    = "\tLD\tHL,0000H\n"
                                                + "\tCALL\tSCREEN\n";


  // Optimierungen fuer das eigentliche Programms ohne Bibliothek.
  public static void optimize1( BasicCompiler compiler )
  {
    AsmCodeBuf buf = compiler.getCodeBuf();
    if( buf != null ) {
      buf.setEnabled( true );

      /*
       * Entfernen der SCREEN-Anweisungen,
       * wenn diese immer "SCREEN 0" sind
       */
      int nScreenX = buf.getNumOccurences( CODE_CALL_SCREEN );
      int nScreen0 = buf.getNumOccurences( CODE_SCREEN_0 );
      if( (nScreenX > 0) && (nScreenX == nScreen0) ) {
	buf.removeAllOccurences( CODE_SCREEN_0 );
	compiler.getLibItems().remove( BasicLibrary.LibItem.SCREEN );
      }
      java.util.List<String> lines = buf.getLinesAsList( 0 );
      removeUnusedLineLabels( lines, compiler );
      buf.setLines( lines );

      // Optimieren von "IF X<Y THEN"
      if( buf.replace(
		CODE_CALL_O_LT
			+ "\tLD\tA,H\n"
			+ "\tOR\tL\n"
			+ "\tJP\tZ,",
		"\tCALL\tCPHLDE\n"
			+ "\tJP\tNC," ) )
      {
	if( !buf.contains( CODE_CALL_O_LT ) ) {
	  compiler.getLibItems().remove( BasicLibrary.LibItem.O_LT );
	  compiler.getLibItems().add( BasicLibrary.LibItem.CPHLDE );
	}
      }

      // Optimieren von "IF X>=Y THEN"
      if( buf.replace(
		CODE_CALL_O_GE
			+ "\tLD\tA,H\n"
			+ "\tOR\tL\n"
			+ "\tJP\tZ,",
		"\tCALL\tCPHLDE\n"
			+ "\tJP\tC," ) )
      {
	if( !buf.contains( CODE_CALL_O_GE ) ) {
	  compiler.getLibItems().remove( BasicLibrary.LibItem.O_GE );
	  compiler.getLibItems().add( BasicLibrary.LibItem.CPHLDE );
	}
      }
    }
  }


  // Optimierungen fuer den gesamten Programmcode inkl. Bibliothek.
  public static void optimize2( BasicCompiler compiler )
  {
    AsmCodeBuf buf = compiler.getCodeBuf();
    if( buf != null ) {
      buf.setEnabled( true );
      java.util.List<String> lines = buf.getLinesAsList( 0 );
      removeUselessJumpsAndUnreachableCode( lines );
      removeUselessLoadReg( lines );
      buf.setLines( lines );
    }
  }


	/* --- private Methoden --- */

  private static void removeUnusedLineLabels(
				java.util.List<String> lines,
				BasicCompiler          compiler )
  {
    Collection<String> allLineLabels  = compiler.getAllLineLabels();
    Collection<String> usedLineLabels = compiler.getUsedLineLabels();
    if( (allLineLabels != null) && (usedLineLabels != null) ) {
      int idx = 0;
      while( idx < lines.size() ) {
	String line      = lines.get( idx++ );
	int    lineLen   = line.length();
	int    prefixLen = BasicCompiler.LINE_LABEL_PREFIX.length();
	if( (lineLen > (prefixLen + 2) )
	    && line.startsWith( BasicCompiler.LINE_LABEL_PREFIX )
	    && line.endsWith( ":\n" ) )
	{
	  String label = line.substring( prefixLen, lineLen - 2 );
	  if( allLineLabels.contains( label )
	      && !usedLineLabels.contains( label ) )
	  {
	    --idx;
	    lines.remove( idx );
	  }
	}
      }
    }
  }


  private static void removeUselessJumpsAndUnreachableCode(
					java.util.List<String> lines )
  {
    boolean enabled = false;
    int     idx     = 0;
    while( idx < lines.size() ) {
      try {
	AsmLine asmLine = AsmLine.scanLine( null, lines.get( idx++ ), true );
	if( asmLine != null ) {
	  String label = asmLine.getLabel();
	  if( !enabled && (label != null) ) {
	    if( label.equals( BasicCompiler.START_LABEL ) ) {
	      enabled = true;
	    }
	  }
	  if( enabled ) {
	    String instr = asmLine.getInstruction();
	    if( instr != null ) {
	      boolean uncondJump = false;
	      String  dstLabel   = null;
	      if( instr.equals( "JP" ) || instr.equals( "JR" ) ) {
		if( asmLine.hasMoreArgs() ) {
		  uncondJump = true;
		  dstLabel   = asmLine.nextArg().toString();
		  if( asmLine.hasMoreArgs() ) {
		    uncondJump = false;
		    dstLabel   = asmLine.nextArg().toString();
		  }
		}
	      } else if( instr.equals( "RET" ) ) {
		if( !asmLine.hasMoreArgs() ) {
		  uncondJump = true;
		}
	      }
	      if( uncondJump || (dstLabel != null) ) {

		/*
		 * naechste Programmzeile mit einer Instruktion suchen
		 *
		 * Wenn es sich um einen unbedingten Sprung handelt,
		 * dann alle unmittelbar nachfolgenden Programmzeilen
		 * entfernen, die keine Marke haben,
		 * da dieser Programmcode nicht erreichbar ist.
		 */
		String nextLabel = null;
		int    idx2      = idx;
		while( (nextLabel == null) & (idx2 < lines.size()) ) {
		  String  instr2   = null;
		  AsmLine asmLine2 = AsmLine.scanLine(
						null,
						lines.get( idx2++ ),
						true );
		  if( asmLine2 != null ) {
		    instr2    = asmLine2.getInstruction();
		    nextLabel = asmLine2.getLabel();
		    if( (nextLabel == null)
			&& uncondJump
			&& (instr2 != null) )
		    {
		      --idx2;
		      lines.remove( idx2 );
		      instr2 = null;
		    }
		  }
		  if( (nextLabel != null) || (instr2 != null) ) {
		    break;
		  }
		}

		/*
		 * Wenn die naechste Programmzeile das Ziel des Sprungs
		 * ist, kann der Sprungbefehl entfernt werden.
		 */
		if( (dstLabel != null) && (nextLabel != null) ) {
		  if( dstLabel.equals( nextLabel ) ) {
		    if( label != null ) {
		      lines.set( idx - 1, label + ":\n" );
		    } else {
		      --idx;
		      lines.remove( idx );
		    }
		  }
		}
	      }
	    }
	  }
	}
      }
      catch( PrgException ex ) {}
    }
  }


  private static void removeUselessLoadReg( java.util.List<String> lines )
  {
    /*
     * In "aValues " und "hlValues" werden alle Ausdruecke gehalten,
     * die aktuell den gleichen Wert haben wie das A- bzw. HL-Register.
     * Die Ausdruecke koennen ein Direktwert sein oder
     * eine indirekte Adressierung einer Speicherzelle.
     */
    Set<String> aValues  = new TreeSet<>();
    Set<String> hlValues = new TreeSet<>();
    int         idx      = 0;
    while( idx < lines.size() ) {
      String line = lines.get( idx++ );
      if( !line.isEmpty()
	  && !line.startsWith( "\n" )
	  && !line.startsWith( ";" )
	  && !line.startsWith( "\t" ) )
      {
	// Marke -> moeglicher Einsprungpunkt -> Registerwerte ungueltig
	aValues.clear();
	hlValues.clear();

	// Marke uebergehen
	int tabPos = line.indexOf( '\t' );
	if( (tabPos > 0) && (line.indexOf( ';' ) < 0) ) {
	  line = line.substring( tabPos );
	}
      }
      if( !line.isEmpty()
	  && !line.startsWith( "\n" )
	  && !line.startsWith( ";" ) )
      {
	if( line.startsWith( "\tLD\t" ) ) {
	  int len = line.length();
	  if( line.startsWith( "\tLD\tA," ) ) {
	    if( (len > 7) && line.endsWith( "\n" )
		&& !line.startsWith( "\tLD\tA,(HL)" )
		&& !line.startsWith( "\tLD\tA,(IX)" )
		&& !line.startsWith( "\tLD\tA,(IX+" )
		&& !line.startsWith( "\tLD\tA,(IX-" )
		&& !line.startsWith( "\tLD\tA,(IY)" )
		&& !line.startsWith( "\tLD\tA,(IY+" )
		&& !line.startsWith( "\tLD\tA,(IY-" ) )
	    {
	      String value = line.substring( 6, len - 1 );
	      if( aValues.contains( value ) ) {
		// gleicher Wert -> Zuweisung entfernen
		--idx;
		lines.remove( idx );
	      } else {
		// neuer Wert -> merken
		aValues.clear();
		aValues.add( value );
	      }
	    } else {
	      aValues.clear();
	    }
	  }
	  else if( line.startsWith( "\tLD\tHL," ) ) {
	    if( (len > 8) && line.endsWith( "\n" ) ) {
	      String value = line.substring( 7, len - 1 );
	      if( hlValues.contains( value ) ) {
		// gleicher Wert -> Zuweisung entfernen
		--idx;
		lines.remove( idx );
	      } else {
		// neuer Wert -> merken
		hlValues.clear();
		hlValues.add( value );
	      }
	    }
	  }
	  else if( line.startsWith( "\tLD\tH," )
		   || line.startsWith( "\tLD\tL," ) )
	  {
	    hlValues.clear();
	  }
	  else if( line.startsWith( "\tLD\t(" )
		   && line.endsWith( "),A\n" )
		   && (len > 8) )
	  {
	    if( !line.startsWith( "\tLD\t(HL)" )
	        && !line.startsWith( "\tLD\t(IX)" )
	        && !line.startsWith( "\tLD\t(IX+" )
	        && !line.startsWith( "\tLD\t(IX-" )
	        && !line.startsWith( "\tLD\t(IY)" )
	        && !line.startsWith( "\tLD\t(IY+" )
	        && !line.startsWith( "\tLD\t(IY-" ) )
	    {
	      aValues.add( line.substring( 4, len - 4 ) );
	    }
	  }
	  else if( line.startsWith( "\tLD\t(" )
		   && line.endsWith( "),HL\n" )
		   && (len > 9) )
	  {
	    hlValues.add( line.substring( 4, len - 4 ) );
	  }
	} else if( line.startsWith( "\tXOR\tA\n" ) ) {
	  String value = "00H";
	  if( aValues.contains( value ) ) {
	    // gleicher Wert -> Zuweisung entfernen
	    --idx;
	    lines.remove( idx );
	  } else {
	    // neuer Wert -> merken
	    aValues.clear();
	    aValues.add( value );
	  }
	} else {
	  /*
	   * Alle anderen Befehle veraendern moeglicherweise
	   * den Inhalt der Register A und HL.
	   */
	  aValues.clear();
	  hlValues.clear();
	}
      }
    }
  }
}
