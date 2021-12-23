/*
 * (c) 2014-2021 Jens Mueller
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

import java.util.Collection;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;
import jkcemu.programming.PrgException;
import jkcemu.programming.assembler.AsmLine;


public class AsmCodeOptimizer
{
  private static final String CODE_CALL_I2_GE_I2_I2 = "\tCALL\tI2_GE_I2_I2\n";
  private static final String CODE_CALL_I2_GE_I4_I4 = "\tCALL\tI2_GE_I4_I4\n";
  private static final String CODE_CALL_I2_LT_I2_I2 = "\tCALL\tI2_LT_I2_I2\n";
  private static final String CODE_CALL_I2_LT_I4_I4 = "\tCALL\tI2_LT_I4_I4\n";
  private static final String CODE_CALL_SCREEN      = "\tCALL\tSCREEN\n";
  private static final String CODE_SCREEN_0         = "\tLD\tHL,0000H\n"
							+ "\tCALL\tSCREEN\n";


  // Optimierungen fuer das eigentliche Programms ohne Bibliothek.
  public static void optimize1( BasicCompiler compiler )
  {
    AsmCodeBuf buf = compiler.getCodeBuf();
    if( buf != null ) {
      buf.setEnabled( true );

      /*
       * unbenutzte Marken entfernen,
       * da diese bei der Optimierung stoeren koennen
       */
      java.util.List<String> lines = buf.getLinesAsList( 0 );
      removeUnusedLineLabels( lines, compiler );
      buf.setLines( lines );

      /*
       * Entfernen der SCREEN-Anweisungen,
       * wenn diese immer "SCREEN 0" sind
       */
      int nScreenX = buf.getNumOccurences( CODE_CALL_SCREEN );
      int nScreen0 = buf.getNumOccurences( CODE_SCREEN_0 );
      if( (nScreenX > 0) && (nScreenX == nScreen0) ) {
	buf.removeAllOccurences( CODE_SCREEN_0 );
	compiler.removeLibItemComplete( BasicLibrary.LibItem.SCREEN );
      }

      // "IF X<Y THEN" optimieren
      if( buf.replace(
		CODE_CALL_I2_LT_I2_I2
			+ "\tLD\tA,H\n"
			+ "\tOR\tL\n"
			+ "\tJP\tZ,",
		"\tCALL\tCMP_HL_DE\n"
			+ "\tJP\tNC," ) )
      {
	compiler.addLibItem( BasicLibrary.LibItem.CMP_HL_DE );
	if( !buf.contains( CODE_CALL_I2_LT_I2_I2 ) ) {
	  compiler.removeLibItemComplete( BasicLibrary.LibItem.I2_LT_I2_I2 );
	}
      }
      if( buf.replace(
		CODE_CALL_I2_LT_I4_I4
			+ "\tLD\tA,H\n"
			+ "\tOR\tL\n"
			+ "\tJP\tZ,",
		"\tCALL\tCMP_DE2HL2_DEHL\n"
			+ "\tJP\tNC," ) )
      {
	compiler.addLibItem( BasicLibrary.LibItem.CMP_DE2HL2_DEHL );
	if( !buf.contains( CODE_CALL_I2_LT_I4_I4 ) ) {
	  compiler.removeLibItemComplete( BasicLibrary.LibItem.I2_LT_I4_I4 );
	}
      }

      // "IF X>=Y THEN" optimieren
      if( buf.replace(
		CODE_CALL_I2_GE_I2_I2
			+ "\tLD\tA,H\n"
			+ "\tOR\tL\n"
			+ "\tJP\tZ,",
		"\tCALL\tCMP_HL_DE\n"
			+ "\tJP\tC," ) )
      {
	compiler.addLibItem( BasicLibrary.LibItem.CMP_HL_DE );
	if( !buf.contains( CODE_CALL_I2_GE_I2_I2 ) ) {
	  compiler.removeLibItemComplete( BasicLibrary.LibItem.I2_GE_I2_I2 );
	}
      }
      if( buf.replace(
		CODE_CALL_I2_GE_I4_I4
			+ "\tLD\tA,H\n"
			+ "\tOR\tL\n"
			+ "\tJP\tZ,",
		"\tCALL\tCMP_DE2HL2_DEHL\n"
			+ "\tJP\tC," ) )
      {
	compiler.addLibItem( BasicLibrary.LibItem.CMP_DE2HL2_DEHL );
	if( !buf.contains( CODE_CALL_I2_GE_I4_I4 ) ) {
	  compiler.removeLibItemComplete( BasicLibrary.LibItem.I2_GE_I4_I4 );
	}
      }

      // "X+1" optimieren
      if( buf.replace(
		"\tLD\tDE,0001H\n"
			+ "\tCALL\tI2_ADD_I2_I2\n",
		"\tCALL\tI2_INC_I2\n" ) )
      {
	compiler.addLibItem( BasicLibrary.LibItem.I2_INC_I2 );
	if( !buf.contains( "\tI2_ADD_I2_I2\n" )
	    && !buf.contains( ",I2_ADD_I2_I2\n" ) )
	{
	  compiler.removeLibItemComplete( BasicLibrary.LibItem.I2_ADD_I2_I2 );
	}
      }

      // "X-1" optimieren
      if( buf.replace(
		"\tLD\tDE,0001H\n"
			+ "\tCALL\tI2_SUB_I2_I2\n",
		"\tCALL\tI2_DEC_I2\n" ) )
      {
	compiler.addLibItem( BasicLibrary.LibItem.I2_DEC_I2 );
	if( !buf.contains( "\tI2_SUB_I2_I2\n" )
	    && !buf.contains( ",I2_SUB_I2_I2\n" ) )
	{
	  compiler.removeLibItemComplete( BasicLibrary.LibItem.I2_SUB_I2_I2 );
	}
      }
    }

    /*
     * Ersetzen:
     *   CALL D6_LD_ACCU_MEM
     *   CALL D6_PUSH_ACCU
     * durch
     *   CALL D6_PUSH_MEM
     */
    if( buf.replace(
		"\tCALL\tD6_LD_ACCU_MEM\n"
			+ "\tCALL\tD6_PUSH_ACCU\n",
		"\tCALL\tD6_PUSH_MEM\n" ) )
    {
      compiler.addLibItem( BasicLibrary.LibItem.D6_PUSH_MEM );
      if( !buf.contains( "D6_LD_ACCU_MEM" ) ) {
	compiler.removeLibItemComplete(
			BasicLibrary.LibItem.D6_LD_ACCU_MEM );
      }
      if( !buf.contains( "D6_PUSH_ACCU" ) ) {
	compiler.removeLibItemComplete(
			BasicLibrary.LibItem.D6_PUSH_ACCU );
      }
    }

    /*
     * Ersetzen:
     *   PUSH HL
     *   LD HL,...
     *   POP DE
     * durch:
     *   EX DE,HL
     *   LD HL,...
     */
    final String INSTR1        = "\tPUSH\tHL\n";
    final String INSTR2_BEG    = "\tLD\tHL,";
    final String INSTR3        = "\tPOP\tDE\n";
    final String PATTERN_BEG   = INSTR1 + INSTR2_BEG;
    final int    patternBegLen = PATTERN_BEG.length();
    final int    instr3Len     = INSTR3.length();
    int          pos           = 0;
    while( pos < buf.length() ) {
      int foundAt = buf.indexOf( PATTERN_BEG, pos );
      if( foundAt < 0 ) {
	break;
      }
      int eol = buf.indexOf( "\n", foundAt + PATTERN_BEG.length() );
      if( buf.containsAt( eol + 1, INSTR3 ) ) {
	int    endPos = eol + 1 + instr3Len;
	String text   = "\tEX\tDE,HL\n"
			+ "\tLD\tHL," + buf.substring(
						foundAt + patternBegLen,
						eol + 1 );
	buf.replace( foundAt, eol + 1 + instr3Len, text );
	pos = foundAt + text.length();
      } else {
	pos += patternBegLen;
      }
    }
  }


  // Optimierungen fuer den gesamten Programmcode inkl. Bibliothek.
  public static void optimize2( BasicCompiler compiler )
  {
    AsmCodeBuf buf = compiler.getCodeBuf();
    if( buf != null ) {
      buf.setEnabled( true );

      // Leeres CHECK_STACK entfernen
      if( buf.contains( BasicLibrary.SUB_CHECK_STACK_BEG
				+ BasicLibrary.SUB_CHECK_STACK_END ) )
      {
	buf.removeAllOccurences( BasicLibrary.CALL_CHECK_STACK_N );
	buf.removeAllOccurences( BasicLibrary.CALL_CHECK_STACK );
	buf.removeAllOccurences( BasicLibrary.SUB_CHECK_STACK_BEG
				+ BasicLibrary.SUB_CHECK_STACK_END );
      }

      // Leeres RESET_ERROR entfernen
      if( buf.contains( BasicLibrary.SUB_RESET_ERROR_BEG
				+ BasicLibrary.SUB_RESET_ERROR_END ) )
      {
	buf.removeAllOccurences( BasicLibrary.CALL_RESET_ERROR );
	buf.removeAllOccurences( BasicLibrary.SUB_RESET_ERROR_BEG
				+ BasicLibrary.SUB_RESET_ERROR_END );
      }

      // ueberfluessige Befehle entfernen
      java.util.List<String> lines = buf.getLinesAsList( 0 );
      removeUselessJumpsAndUnreachableCode( lines );
      removeUselessLoadReg( lines );
      buf.setLines( lines );
    }
  }


	/* --- private Methoden --- */

  private static boolean isFixValue( String value )
  {
    boolean rv = false;
    if( value != null ) {
      rv = true;
      if( value.equals( "B" )
	  || value.equals( "C" )
	  || value.equals( "D" )
	  || value.equals( "E" )
	  || value.equals( "H" )
	  || value.equals( "L" )
	  || value.equals( "I" )
	  || value.equals( "R" )
	  || value.startsWith( "(BC)" )
	  || value.startsWith( "(DE)" )
	  || value.startsWith( "(HL)" )
	  || value.startsWith( "(IX)" )
	  || value.startsWith( "(IX+" )
	  || value.startsWith( "(IX-" )
	  || value.startsWith( "(IY)" )
	  || value.startsWith( "(IY+" )
	  || value.startsWith( "(IY-" )
	  || (value.indexOf( '\u0020' ) >= 0)
	  || (value.indexOf( '\t' ) >= 0) )
      {
	rv = false;
      }
    }
    return rv;
  }


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
		 * und alle bis dahin definierten Marken merken
		 */
		java.util.List<String> nextLabels = null;
		String                 nextInstr  = null;
		int                    idx2       = idx;
		while( (nextInstr == null) & (idx2 < lines.size()) ) {
		  AsmLine asmLine2 = AsmLine.scanLine(
						null,
						lines.get( idx2++ ),
						true );
		  if( asmLine2 != null ) {
		    nextInstr        = asmLine2.getInstruction();
		    String nextLabel = asmLine2.getLabel();
		    if( nextLabel != null ) {
		      if( nextLabels == null ) {
			nextLabels = new ArrayList<>();
		      }
		      nextLabels.add( nextLabel );
		    }
		    /*
		     * Ist der vorherige Sprung unbedingt,
		     * dann alle Programmzeilen bis zur
		     * naechsten Marke loeschen
		     */
		    if( uncondJump && (nextLabels == null) ) {
		      --idx2;
		      lines.remove( idx2 );
		      nextInstr = null;
		    }
		  }
		}

		/*
		 * Wenn die naechste Programmzeile das Ziel des Sprungs
		 * ist, kann der Sprungbefehl entfernt werden.
		 */
		if( (dstLabel != null) && (nextLabels != null) ) {
		  if( nextLabels.contains( dstLabel ) ) {
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
     * In "aValues " "deValues" und "hlValues" werden alle Ausdruecke
     * gehalten, die aktuell den gleichen Wert haben wie das A-,
     * DE- bzw. HL-Register.
     * Die Ausdruecke koennen ein Direktwert sein oder
     * eine indirekte Adressierung einer Speicherzelle.
     */
    Set<String> aValues  = new TreeSet<>();
    Set<String> deValues = new TreeSet<>();
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
	deValues.clear();
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
	    if( (len > 7) && line.endsWith( "\n" ) ) {
	      String value = line.substring( 6, len - 1 );
	      if( isFixValue( value ) ) {
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
		// Wert von A nicht konstant
		aValues.clear();
	      }
	    }
	  }
	  else if( line.startsWith( "\tLD\tDE," ) ) {
	    if( (len > 8) && line.endsWith( "\n" ) ) {
	      String value = line.substring( 7, len - 1 );
	      if( deValues.contains( value ) ) {
		// gleicher Wert -> Zuweisung entfernen
		--idx;
		lines.remove( idx );
	      } else {
		// neuer Wert -> merken
		deValues.clear();
		deValues.add( value );
	      }
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
	  else if( line.startsWith( "\tLD\tD," )
		   || line.startsWith( "\tLD\tE," ) )
	  {
	    deValues.clear();
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
	    aValues.clear();
	    String value = line.substring( 4, len - 4 );
	    if( isFixValue( value ) ) {
	      aValues.add( value );
	    }
	  }
	  else if( line.startsWith( "\tLD\t(" )
		   && line.endsWith( "),DE\n" )
		   && (len > 9) )
	  {
	    deValues.clear();
	    deValues.add( line.substring( 4, len - 4 ) );
	  }
	  else if( line.startsWith( "\tLD\t(" )
		   && line.endsWith( "),HL\n" )
		   && (len > 9) )
	  {
	    hlValues.clear();
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
	  deValues.clear();
	  hlValues.clear();
	}
      }
    }
  }
}
