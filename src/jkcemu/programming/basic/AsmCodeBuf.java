/*
 * (c) 2012-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Puffer zum Speichern des erzeugten Assembler-Textes
 */

package jkcemu.programming.basic;

import java.util.ArrayList;


public class AsmCodeBuf
{
  private StringBuilder buf;
  private boolean       enabled;


  public AsmCodeBuf( int initalCapacity )
  {
    this.buf      = new StringBuilder( initalCapacity );
    this.enabled = true;
  }


  public void append( char ch )
  {
    if( this.enabled )
      this.buf.append( ch );
  }


  public void append( CharSequence text )
  {
    if( this.enabled )
      this.buf.append( text );
  }


  public void append( long value )
  {
    if( this.enabled )
      this.buf.append( value );
  }


  public void append( AsmCodeBuf textBuf )
  {
    if( this.enabled )
      this.buf.append( textBuf.buf );
  }


  public void appendHex2( int value )
  {
    if( this.enabled )
      appendHex2Internal( value );
  }


  public void appendHex4( int value )
  {
    if( this.enabled )
      appendHex4Internal( value );
  }


  public void appendStringLiteral( CharSequence text, String termByteStr )
  {
    if( this.enabled ) {
      int nCh  = 0;
      int nNum = 0;
      if( text != null ) {
	int len  = text.length();
	for( int i = 0; i < len; i++ ) {
	  char ch = text.charAt( i );
	  if( (ch < '\u0020') || (ch > '\u007E')
	      || (ch == '\'') || (ch == '\"') )
	  {
	    if( nCh > 0 ) {
	      this.buf.append( "\'\n" );
	      nCh = 0;
	    }
	    if( nNum > 0 ) {
	      this.buf.append( ',' );
	    } else {
	      this.buf.append( "\tDB\t" );
	    }
	    appendHex2Internal( ch );
	    nNum++;
	    if( nNum >= 8 ) {
	      newLineInternal();
	      nNum = 0;
	    }
	  } else {
	    if( nNum > 0 ) {
	      newLineInternal();
	      nNum = 0;
	    }
	    if( nCh == 0 ) {
	      this.buf.append( "\tDB\t\'" );
	    }
	    this.buf.append( ch );
	    nCh++;
	    if( nCh >= 40 ) {
	      this.buf.append( "\'\n" );
	      nCh = 0;
	    }
	  }
	}
      }
      if( nCh > 0 ) {
	this.buf.append( "\'\n" );
      }
      if( nNum > 0 ) {
	this.buf.append( "," );
      } else {
	this.buf.append( "\tDB\t" );
      }
      this.buf.append( termByteStr );
      newLineInternal();
    }
  }


  public void appendStringLiteral( CharSequence text )
  {
    appendStringLiteral( text, "00H" );
  }


  public void append_CALL_RESET_ERROR( BasicCompiler compiler )
  {
    if( this.enabled ) {
      this.buf.append( BasicLibrary.CALL_RESET_ERROR );
      compiler.addLibItem( BasicLibrary.LibItem.RESET_ERROR );
    }
  }


  public void append_LD_A_n( int value )
  {
    if( this.enabled ) {
      value &= 0xFF;
      if( value == 0 ) {
	this.buf.append( "\tXOR\tA\n" );
      } else {
	this.buf.append( "\tLD\tA," );
	appendHex2Internal( value );
	newLineInternal();
      }
    }
  }


  public void append_LD_B_n( int value )
  {
    if( this.enabled ) {
      this.buf.append( "\tLD\tB," );
      appendHex2Internal( value );
      newLineInternal();
    }
  }


  public void append_LD_BC_nn( int value )
  {
    if( this.enabled ) {
      this.buf.append( "\tLD\tBC," );
      appendHex4Internal( value );
      newLineInternal();
    }
  }


  public void append_LD_BC_xx( String xx )
  {
    if( this.enabled && (xx != null) ) {
      this.buf.append( "\tLD\tBC," );
      this.buf.append( xx );
      newLineInternal();
    }
  }


  public void append_LD_DE_nn( int value )
  {
    if( this.enabled ) {
      this.buf.append( "\tLD\tDE," );
      appendHex4Internal( value );
      newLineInternal();
    }
  }


  public void append_LD_DE_xx( String xx )
  {
    if( this.enabled && (xx != null) ) {
      this.buf.append( "\tLD\tDE," );
      this.buf.append( xx );
      newLineInternal();
    }
  }


  public void append_LD_DE_IndirectIY( int iyOffs )
  {
    if( this.enabled ) {
      append_LD_R_IndirectIY( 'E', iyOffs );
      append_LD_R_IndirectIY( 'D', iyOffs + 1 );
    }
  }


  public void append_LD_DEHL_IndirectIY( int iyOffs )
  {
    if( this.enabled ) {
      append_LD_R_IndirectIY( 'L', iyOffs );
      append_LD_R_IndirectIY( 'H', iyOffs + 1 );
      append_LD_R_IndirectIY( 'E', iyOffs + 2 );
      append_LD_R_IndirectIY( 'D', iyOffs + 3 );
    }
  }


  public void append_LD_D6Accu_IndirectIY(
					int iyOffs,
					BasicCompiler compiler )
  {
    if( this.enabled ) {
      buf.append( "\tCALL\tD6_LD_ACCU_IYOFFS\n"
		+ "\tDB\t" );
      appendHex2( iyOffs & 0xFF );
      newLineInternal();
      compiler.addLibItem( BasicLibrary.LibItem.D6_LD_ACCU_IYOFFS );
    }
  }


  public void append_LD_DEHL_nnnn( long value )
  {
    if( (value & 0x0000FF00) == 0 ) {
      append_LD_HL_nn( (int) value );
      append( "\tLD\tD,H\n"
		+ "\tLD\tE,H\n" );
    } else {
      append_LD_DE_nn( (int) (value >> 16) );
      append_LD_HL_nn( (int) value );
    }
  }


  public void append_LD_HL_nn( int value )
  {
    if( this.enabled ) {
      this.buf.append( "\tLD\tHL," );
      appendHex4Internal( value );
      newLineInternal();
    }
  }


  public void append_LD_HL_xx( String xx )
  {
    if( this.enabled && (xx != null) ) {
      this.buf.append( "\tLD\tHL," );
      this.buf.append( xx );
      newLineInternal();
    }
  }


  public void append_LD_HL_IndirectIY( int iyOffs )
  {
    if( this.enabled ) {
      append_LD_R_IndirectIY( 'L', iyOffs );
      append_LD_R_IndirectIY( 'H', iyOffs + 1 );
    }
  }


  public void append_LD_HL_IYOffsAddr( int iyOffs )
  {
    if( this.enabled ) {
      this.buf.append( "\tPUSH\tIY\n"
			+ "\tPOP\tHL\n"
			+ "\tLD\tDE," );
      appendHex4Internal( iyOffs );
      this.buf.append( "\n"
			+ "\tADD\tHL,DE\n" );
    }
  }


  public void append_LD_IndirectIY_HL( int iyOffs )
  {
    if( this.enabled ) {
      append_LD_IndirectIY_R( iyOffs, 'L' );
      append_LD_IndirectIY_R( iyOffs + 1, 'H' );
    }
  }


  public void append_LD_IndirectIY_DEHL( int iyOffs )
  {
    if( this.enabled ) {
      append_LD_IndirectIY_R( iyOffs, 'L' );
      append_LD_IndirectIY_R( iyOffs + 1, 'H' );
      append_LD_IndirectIY_R( iyOffs + 2, 'E' );
      append_LD_IndirectIY_R( iyOffs + 3, 'D' );
    }
  }


  public void append_POP_D6Accu( BasicCompiler compiler )
  {
    if( this.enabled ) {
      this.buf.append( "\tCALL\tD6_POP_ACCU\n" );
      compiler.addLibItem( BasicLibrary.LibItem.D6_POP_ACCU );
    }
  }


  public void append_POP_D6Op1( BasicCompiler compiler )
  {
    if( this.enabled ) {
      this.buf.append( "\tCALL\tD6_POP_OP1\n" );
      compiler.addLibItem( BasicLibrary.LibItem.D6_POP_OP1 );
    }
  }


  public void append_POP_DE2HL2( BasicCompiler compiler )
  {
    if( this.enabled ) {
      this.buf.append( "\tCALL\tI4_POP_DE2HL2\n" );
      compiler.addLibItem( BasicLibrary.LibItem.I4_POP_DE2HL2 );
    }
  }


  public void append_PUSH_D6Accu( BasicCompiler compiler )
  {
    if( this.enabled ) {
      this.buf.append( "\tCALL\tD6_PUSH_ACCU\n" );
      compiler.addLibItem( BasicLibrary.LibItem.D6_PUSH_ACCU );
    }
  }


  public void append_PUSH_DEHL()
  {
    if( this.enabled ) {
      this.buf.append( "\tPUSH\tDE\n"
		+ "\tPUSH\tHL\n" );
    }
  }


  public boolean contains( String text )
  {
    return (this.buf.indexOf( text ) >= 0);
  }


  public boolean containsAt( int pos, String text )
  {
    return this.buf.substring( pos, pos + text.length() ).equals( text );
  }


  public String cut( int pos )
  {
    String rv = "";
    if( this.enabled ) {
      rv = this.buf.substring( pos );
      this.buf.setLength( pos );
    }
    return rv;
  }


  public boolean cutIfEndsWith( String pattern )
  {
    boolean rv = false;
    if( this.enabled ) {
      int pos = this.buf.length() - pattern.length();
      if( pos >= 0 ) {
	if( this.buf.substring( pos ).equals( pattern ) ) {
	  this.buf.setLength( pos );
	  rv = true;
	}
      }
    }
    return rv;
  }


  public String cutLastLine()
  {
    return cut( getLastLinePos() );
  }


  public void delete( int begPos, int endPos )
  {
    if( this.enabled )
      this.buf.delete( begPos, endPos );
  }


  public int getLastLinePos()
  {
    int rv  = 0;
    int pos = this.buf.length() - 1;
    if( pos > 0 ) {
      if( this.buf.charAt( pos ) == '\n' ) {
	while( pos > 0 ) {
	  --pos;
	  if( this.buf.charAt( pos ) == '\n' ) {
	    rv = pos + 1;
	    break;
	  }
	}
      }
    }
    return rv;
  }


  public java.util.List<String> getLinesAsList( int begPos )
  {
    java.util.List<String> lines = new ArrayList<>();
    synchronized( this.buf ) {
      int len = this.buf.length();
      if( begPos < 0 ) {
	begPos = 0;
      }
      while( begPos < len ) {
	int eol = this.buf.indexOf( "\n", begPos );
	if( eol < begPos ) {
	  lines.add( this.buf.substring( begPos ) );
	  break;
	}
	eol++;
	lines.add( this.buf.substring( begPos, eol ) );
	begPos = eol;
      }
    }
    return lines;
  }


  public int getNumOccurences( String text )
  {
    int n = 0;
    if( text != null ) {
      int len    = this.buf.length();
      int curPos = 0;
      while( curPos < len ) {
	int foundPos = this.buf.indexOf( text, curPos );
	if( foundPos < 0 ) {
	  break;
	}
	n++;
	curPos = foundPos + 1;
      }
    }
    return n;
  }


  public int indexOf( String pattern, int begPos )
  {
    return this.buf.indexOf( pattern, begPos );
  }


  public void insert( int pos, CharSequence text)
  {
    if( this.enabled && (text != null) )
      this.buf.insert( pos, text );
  }


  public void insert_PUSH_D6Accu( int pos, BasicCompiler compiler )
  {
    if( this.enabled ) {
      this.buf.insert( pos, "\tCALL\tD6_PUSH_ACCU\n" );
      compiler.addLibItem( BasicLibrary.LibItem.D6_PUSH_ACCU );
    }
  }


  public boolean isEnabled()
  {
    return this.enabled;
  }


  public int length()
  {
    return this.buf.length();
  }


  public void newLine()
  {
    if( this.enabled )
      newLineInternal();
  }


  public void removeAllOccurences( String text )
  {
    if( this.enabled && (text != null) ) {
      int textLen = text.length();
      if( textLen > 0 ) {
	int codeLen = this.buf.length();
	int curPos  = 0;
	while( curPos < codeLen ) {
	  int foundPos = this.buf.indexOf( text, curPos );
	  if( foundPos < 0 ) {
	    break;
	  }
	  this.buf.delete( foundPos, foundPos + textLen );
	  curPos = foundPos;
	}
      }
    }
  }


  public boolean replace( String oldText, String newText )
  {
    boolean rv = false;
    if( this.enabled && contains( oldText ) ) {
      String s = this.buf.toString().replace( oldText, newText );
      this.buf.setLength( 0 );
      this.buf.append( s );
      rv = true;
    }
    return rv;
  }


  public boolean replaceEnd( String oldText, String newText )
  {
    boolean rv = false;
    if( this.enabled ) {
      int oldLen = oldText.length();
      int pos    = this.buf.length() - oldLen;
      if( pos >= 0 ) {
	if( this.buf.substring( pos ).equals( oldText ) ) {
	  this.buf.setLength( pos );
	  this.buf.append( newText );
	  rv = true;
	}
      }
    }
    return rv;
  }


  public void replace( int startPos, int endPos, String text )
  {
    if( this.enabled )
      this.buf.replace( startPos, endPos, text );
  }


  public void setEnabled( boolean state )
  {
    this.enabled = state;
  }


  public void setLength( int len )
  {
    if( this.enabled )
      this.buf.setLength( len );
  }


  public void setLines( java.util.List<String> lines )
  {
    if( this.enabled ) {
      this.buf.setLength( 0 );
      if( lines != null ) {
	for( String line : lines ) {
	  this.buf.append( line );
	}
      }
    }
  }


  public String substring( int pos )
  {
    return this.buf.substring( pos );
  }


  public String substring( int begPos, int endPos )
  {
    return this.buf.substring( begPos, endPos );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public String toString()
  {
    return this.buf.toString();
  }


	/* --- private Methoden --- */

  private void appendHex2Internal( int value )
  {
    value &= 0xFF;
    if( value >= 0xA0 ) {
      this.buf.append( '0' );
    }
    this.buf.append( String.format( "%02XH", value ) );
  }


  private void appendHex4Internal( int value )
  {
    value &= 0xFFFF;
    if( value >= 0xA000 ) {
      this.buf.append( '0' );
    }
    this.buf.append( String.format( "%04XH", value ) );
  }


  private void append_LD_IndirectIY_R( int iyOffs, char r )
  {
    iyOffs &= 0xFF;

    char op = '+';
    if( iyOffs >= 0x80 ) {
      op = '-';
      iyOffs = (-iyOffs) & 0xFF;
    }
    this.buf.append( String.format(
			"\tLD\t(IY%c%02XH),%c\n",
			op,
			iyOffs,
			r ) );
  }


  private void append_LD_R_IndirectIY( char r, int iyOffs )
  {
    iyOffs &= 0xFF;

    char op = '+';
    if( iyOffs >= 0x80 ) {
      op = '-';
      iyOffs = (-iyOffs) & 0xFF;
    }
    this.buf.append( String.format(
			"\tLD\t%c,(IY%c%02XH)\n",
			r,
			op,
			iyOffs ) );
  }


  private void newLineInternal()
  {
    this.buf.append( '\n' );
  }
}
