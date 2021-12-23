/*
 * (c) 2012-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Informationen ueber den Speicherbereich einer einfachen Variablen
 * bzw. ueber ein Element einer Feldvariablen
 */

package jkcemu.programming.basic;

import jkcemu.programming.PrgException;


public class SimpleVarInfo
{
  private BasicCompiler.DataType dataType;
  private String                 addrExpr;
  private Integer                iyOffs;
  private Integer                myHashCode;


  public SimpleVarInfo(
		BasicCompiler.DataType dataType,
		String                 addrExpr,
		Integer                iyOffs )
  {
    this.dataType   = dataType;
    this.addrExpr   = addrExpr;
    this.iyOffs     = iyOffs;
    this.myHashCode = null;
  }


  public void ensureAddrInHL( AsmCodeBuf buf )
  {
    if( this.addrExpr != null ) {
      buf.append( "\tLD\tHL," );
      buf.append( this.addrExpr );
      buf.append( '\n' );
    } else if( this.iyOffs != null ) {
      buf.append( "\tPUSH\tIY\n"
		+ "\tPOP\tHL\n" );
      int iyOffs = this.iyOffs.intValue() & 0xFFFF;
      if( iyOffs == 0xFFFE ) {
	buf.append( "\tDEC\tHL\n"
		+ "\tDEC\tHL\n" );
      } else if( iyOffs == 2 ) {
	buf.append( "\tINC\tHL\n"
		+ "\tINC\tHL\n" );
      } else if( iyOffs != 0 ) {
	buf.append_LD_DE_nn( iyOffs );
	buf.append( "\tADD\tHL,DE\n" );
      }
    }
  }


  public void ensureStaticAddrInDE(
			AsmCodeBuf buf,
			boolean    saveHL ) throws PrgException
  {
    if( this.addrExpr != null ) {
      buf.append( "\tLD\tDE," );
      buf.append( this.addrExpr );
      buf.append( '\n' );
    } else if( this.iyOffs != null ) {
      if( saveHL ) {
	buf.append( "\tPUSH\tHL\n" );
      }
      buf.append( "\tPUSH\tIY\n"
		+ "\tPOP\tHL\n" );
      int iyOffs = this.iyOffs.intValue() & 0xFFFF;
      if( iyOffs != 0 ) {
	buf.append_LD_DE_nn( iyOffs );
	buf.append( "\tADD\tHL,DE\n"
		+ "\tEX\tDE,HL\n" );
      }
      if( saveHL ) {
	buf.append( "\tPOP\tHL\n" );
      }
    } else {
      throwNonStaticVarNotAllowd();
    }
  }


  public BasicCompiler.DataType getDataType()
  {
    return this.dataType;
  }


  public String getStaticAddrExpr()
  {
    return this.addrExpr;
  }


  public boolean hasStaticAddr()
  {
    return (this.addrExpr != null) || (this.iyOffs != null);
  }


  public void writeCode_LD_DE_VarValue(
			BasicCompiler compiler ) throws PrgException
  {
    if( this.dataType.equals( BasicCompiler.DataType.INT2 )
	|| this.dataType.equals( BasicCompiler.DataType.STRING ) )
    {
      AsmCodeBuf buf = compiler.getCodeBuf();
      if( this.addrExpr != null ) {
	buf.append( "\tLD\tDE,(" );
	buf.append( this.addrExpr );
	buf.append( ")\n" );
      } else if( this.iyOffs != null ) {
	buf.append_LD_DE_IndirectIY( this.iyOffs.intValue() );
      } else {
	buf.append( "\tLD\tE,(HL)\n"
		+ "\tINC\tHL\n"
		+ "\tLD\tD,(HL)\n" );
      }
    } else {
      BasicUtil.throwDataTypeMismatch();
    }
  }


  public void writeCode_LD_Reg_Var( BasicCompiler compiler )
  {
    AsmCodeBuf buf = compiler.getCodeBuf();
    if( this.addrExpr != null ) {
      if( this.dataType.equals( BasicCompiler.DataType.DEC6 ) ) {
	buf.append_LD_HL_xx( this.addrExpr );
	buf.append( "\tCALL\tD6_LD_ACCU_MEM\n" );
	compiler.addLibItem( BasicLibrary.LibItem.D6_LD_ACCU_MEM );
      } else {
	buf.append( "\tLD\tHL,(" );
	buf.append( this.addrExpr );
	buf.append( ")\n" );
	if( this.dataType.equals( BasicCompiler.DataType.INT4 ) ) {
	  buf.append( "\tLD\tDE,(" );
	  buf.append( this.addrExpr );
	  buf.append( "+2)\n" );
	}
      }
    } else if( this.iyOffs != null ) {
      if( this.dataType.equals( BasicCompiler.DataType.DEC6 ) ) {
	buf.append_LD_HL_IYOffsAddr( this.iyOffs.intValue() );
	buf.append( "\tCALL\tD6_LD_ACCU_MEM\n" );
	compiler.addLibItem( BasicLibrary.LibItem.D6_LD_ACCU_MEM );
      } else if( this.dataType.equals( BasicCompiler.DataType.INT4 ) ) {
	buf.append_LD_DEHL_IndirectIY( this.iyOffs.intValue() );
      } else {
	buf.append_LD_HL_IndirectIY( this.iyOffs.intValue() );
      }
    } else {
      if( this.dataType.equals( BasicCompiler.DataType.DEC6 ) ) {
	buf.append( "\tCALL\tD6_LD_ACCU_MEM\n" );
	compiler.addLibItem( BasicLibrary.LibItem.D6_LD_ACCU_MEM );
      } else if( this.dataType.equals( BasicCompiler.DataType.INT4 ) ) {
	buf.append( "\tCALL\tLD_DEHL_MEM\n" );
	compiler.addLibItem( BasicLibrary.LibItem.LD_DEHL_MEM );
      } else {
	buf.append( "\tCALL\tLD_HL_MEM\n" );
	compiler.addLibItem( BasicLibrary.LibItem.LD_HL_MEM );
      }
    }
  }


  public void writeCode_LD_Var_Reg( BasicCompiler compiler )
						throws PrgException
  {
    AsmCodeBuf buf = compiler.getCodeBuf();
    if( this.addrExpr != null ) {
      if( this.dataType.equals( BasicCompiler.DataType.DEC6 ) ) {
	buf.append_LD_HL_xx( this.addrExpr );
	buf.append( "\tCALL\tD6_LD_MEM_ACCU\n" );
	compiler.addLibItem( BasicLibrary.LibItem.D6_LD_MEM_ACCU );
      } else {
	buf.append( "\tLD\t(" );
	buf.append( this.addrExpr );
	buf.append( "),HL\n" );
	if( this.dataType.equals( BasicCompiler.DataType.INT4 ) ) {
	  buf.append( "\tLD\t(" );
	  buf.append( this.addrExpr );
	  buf.append( "+2),DE\n" );
	}
      }
    } else if( this.iyOffs != null ) {
      if( this.dataType.equals( BasicCompiler.DataType.DEC6 ) ) {
	buf.append_LD_HL_IYOffsAddr( this.iyOffs.intValue() );
	buf.append( "\tCALL\tD6_LD_MEM_ACCU\n" );
	compiler.addLibItem( BasicLibrary.LibItem.D6_LD_MEM_ACCU );
      } else if( this.dataType.equals( BasicCompiler.DataType.INT4 ) ) {
	buf.append_LD_IndirectIY_DEHL( this.iyOffs.intValue() );
      } else {
	buf.append_LD_IndirectIY_HL( this.iyOffs.intValue() );
      }
    } else {
      throwNonStaticVarNotAllowd();
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
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


  @Override
  public int hashCode()
  {
    if( this.myHashCode == null ) {
      StringBuilder buf = new StringBuilder( 128 );
      buf.append( getClass().getName() );
      buf.append( ':' );
      buf.append( this.dataType );
      buf.append( ':' );
      if( this.addrExpr != null ) {
	buf.append( this.addrExpr );
      }
      buf.append( ':' );
      if( this.dataType != null ) {
	buf.append( this.dataType );
      }
      buf.append( ':' );
      if( this.iyOffs != null ) {
	buf.append( this.iyOffs );
      }
      this.myHashCode = Integer.valueOf(
				buf.toString().hashCode() ^ 0x276D4913 );
    }
    return this.myHashCode.intValue();
  }


	/* --- private Methoden --- */

  private static void throwNonStaticVarNotAllowd() throws PrgException
  {
    throw new PrgException( "Feldvariable mit variablen Indexangaben"
					+ " an der Stelle nicht erlaubt" );
  }
}
