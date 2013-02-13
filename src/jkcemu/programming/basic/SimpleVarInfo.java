/*
 * (c) 2012 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Informationen ueber den Speicherbereich einer einfachen Variablen
 * bzw. ueber ein Element einer Feldvariablen
 */

package jkcemu.programming.basic;

import java.lang.*;
import jkcemu.programming.PrgException;


public class SimpleVarInfo
{
  private BasicCompiler.DataType dataType;
  private String                 addrExpr;
  private Integer                iyOffs;


  public SimpleVarInfo(
		BasicCompiler.DataType dataType,
		String                 addrExpr,
		Integer                iyOffs )
  {
    this.dataType = dataType;
    this.addrExpr = addrExpr;
    this.iyOffs   = iyOffs;
  }


  public void ensureAddrInHL( AsmCodeBuf buf )
  {
    if( this.addrExpr != null ) {
      buf.append( "\tLD\tHL," );
      buf.append( this.addrExpr );
      buf.append( (char) '\n' );
    } else if( this.iyOffs != null ) {
      int iyOffs = this.iyOffs.intValue() & 0xFFFF;
      buf.append( "\tPUSH\tIY\n"
		+ "\tPOP\tHL\n"
		+ "\tLD\tDE," );
      buf.appendHex4( iyOffs );
      buf.append( "\n"
		+ "\tADD\tHL,DE\n" );
    }
  }


  public void ensureStaticAddrInDE(
			AsmCodeBuf buf,
			boolean    saveHL ) throws PrgException
  {
    if( this.addrExpr != null ) {
      buf.append( "\tLD\tDE," );
      buf.append( this.addrExpr );
      buf.append( (char) '\n' );
    } else if( this.iyOffs != null ) {
      if( saveHL ) {
	buf.append( "\tPUSH\tHL\n" );
      }
      int iyOffs = this.iyOffs.intValue() & 0xFFFF;
      buf.append( "\tPUSH\tIY\n"
		+ "\tPOP\tHL\n"
		+ "\tLD\tDE," );
      buf.appendHex4( iyOffs );
      buf.append( "\n"
		+ "\tADD\tHL,DE\n"
		+ "\tEX\tDE,HL\n" );
      if( saveHL ) {
	buf.append( "\tPOP\tHL\n" );
      }
    } else {
      throwNonStaticVarNotAllowd();
    }
  }


  public void ensureValueInDE( AsmCodeBuf buf )
  {
    if( this.addrExpr != null ) {
      buf.append( "\tLD\tDE,(" );
      buf.append( this.addrExpr );
      buf.append( ")\n" );
    } else if( this.iyOffs != null ) {
      int iyOffs = this.iyOffs.intValue();
      if( iyOffs < 0 ) {
	iyOffs = -iyOffs & 0xFFFF;
	buf.append( "\tLD\tD,(IY-" );
	buf.append( iyOffs - 1 );
	buf.append( ")\n"
		+ "\tLD\tE,(IY-" );
	buf.append( iyOffs );
	buf.append( ")\n" );
      } else {
	buf.append( "\tLD\tD,(IY+" );
	buf.append( iyOffs + 1 );
	buf.append( ")\n"
		+ "\tLD\tE,(IY+" );
	buf.append( iyOffs );
	buf.append( ")\n" );
      }
    } else {
      buf.append( "\tLD\tE,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tD,(HL)\n" );
    }
  }


  public void ensureValueInHL( AsmCodeBuf buf )
  {
    if( this.addrExpr != null ) {
      buf.append( "\tLD\tHL,(" );
      buf.append( this.addrExpr );
      buf.append( ")\n" );
    } else if( this.iyOffs != null ) {
      int iyOffs = this.iyOffs.intValue();
      if( iyOffs < 0 ) {
	iyOffs = -iyOffs & 0xFFFF;
	buf.append( "\tLD\tH,(IY-" );
	buf.append( iyOffs - 1 );
	buf.append( ")\n"
		+ "\tLD\tL,(IY-" );
	buf.append( iyOffs );
	buf.append( ")\n" );
      } else {
	buf.append( "\tLD\tH,(IY+" );
	buf.append( iyOffs + 1 );
	buf.append( ")\n"
		+ "\tLD\tL,(IY+" );
	buf.append( iyOffs );
	buf.append( ")\n" );
      }
    } else {
      buf.append( "\tLD\tA,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tH,(HL)\n"
		+ "\tLD\tL,A\n" );
    }
  }


  public BasicCompiler.DataType getDataType()
  {
    return this.dataType;
  }


  public boolean hasStaticAddr()
  {
    return (this.addrExpr != null) || (this.iyOffs != null);
  }


  public void writeCode_LD_Var_HL( AsmCodeBuf buf ) throws PrgException
  {
    if( this.addrExpr != null ) {
      buf.append( "\tLD\t(" );
      buf.append( this.addrExpr );
      buf.append( "),HL\n" );
    } else if( this.iyOffs != null ) {
      int iyOffs = this.iyOffs.intValue();
      if( iyOffs < 0 ) {
	iyOffs = -iyOffs & 0xFFFF;
	buf.append( "\tLD\t(IY-" );
	buf.append( iyOffs - 1 );
	buf.append( "),H\n"
		+ "\tLD\t(IY-" );
	buf.append( iyOffs );
	buf.append( "),L\n" );
      } else {
	buf.append( "\tLD\t(IY+" );
	buf.append( iyOffs + 1 );
	buf.append( "),H\n"
		+ "\tLD\t(IY+" );
	buf.append( iyOffs );
	buf.append( "),L\n" );
      }
    } else {
      throwNonStaticVarNotAllowd();
    }
  }


	/* --- ueberschriebene Methoden --- */

  public boolean equals( Object o )
  {
    boolean rv = false;
    if( o != null ) {
      if( o instanceof SimpleVarInfo ) {
	SimpleVarInfo varInfo = (SimpleVarInfo) o;
	if( (varInfo.addrExpr != null) && (this.addrExpr != null) ) {
	  rv = varInfo.addrExpr.equals( this.addrExpr );
	} else if( (varInfo.iyOffs != null) && (this.iyOffs != null) ) {
	  rv = varInfo.iyOffs.equals( this.iyOffs );
	} else {
	  if( (varInfo.addrExpr == null) && (this.addrExpr == null)
	      && (varInfo.iyOffs == null) && (this.iyOffs == null) )
	  {
	    rv = true;
	  }
	}
      }
    }
    return rv;
  }


	/* --- private Methoden --- */

  private static void throwNonStaticVarNotAllowd() throws PrgException
  {
    throw new PrgException( "Feldvariable mit variablen Indexangaben"
					+ " an der Stelle nicht erlaubt" );
  }
}

