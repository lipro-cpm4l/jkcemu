/*
 * (c) 2012-2013 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Puffer zum Speichern des erzeugten Assembler-Textes
 */

package jkcemu.programming.basic;


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
	      this.buf.append( (char) ',' );
	    } else {
	      this.buf.append( "\tDB\t" );
	    }
	    appendHex2Internal( ch );
	    nNum++;
	    if( nNum >= 8 ) {
	      this.buf.append( (char) '\n' );
	      nNum = 0;
	    }
	  } else {
	    if( nNum > 0 ) {
	      this.buf.append( (char) '\n' );
	      nNum = 0;
	    }
	    if( nCh == 0 ) {
	      this.buf.append( "\tDB\t\'" );
	    }
	    this.buf.append( ch );
	    nCh++;
	    if( nCh >= 16 ) {
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
      this.buf.append( (char) '\n' );
    }
  }


  public void appendStringLiteral( CharSequence text )
  {
    appendStringLiteral( text, "00H" );
  }


  public void append_LD_A_n( int value )
  {
    if( this.enabled ) {
      if( value == 0 ) {
	this.buf.append( "\tXOR\tA\n" );
      } else {
	this.buf.append( "\tLD\tA," );
	appendHex2Internal( value );
	buf.append( (char) '\n');
      }
    }
  }


  public void append_LD_BC_nn( int value )
  {
    if( this.enabled ) {
      this.buf.append( "\tLD\tBC," );
      appendHex4Internal( value );
      buf.append( (char) '\n');
    }
  }


  public void append_LD_DE_nn( int value )
  {
    if( this.enabled ) {
      this.buf.append( "\tLD\tDE," );
      appendHex4Internal( value );
      buf.append( (char) '\n');
    }
  }


  public void append_LD_DE_xx( String xx )
  {
    if( this.enabled && (xx != null) ) {
      this.buf.append( "\tLD\tDE," );
      this.buf.append( xx );
      this.buf.append( (char) '\n');
    }
  }


  public void append_LD_DE_IndirectIY( int iyOffs )
  {
    if( this.enabled ) {
      if( iyOffs < 0 ) {
	iyOffs = -iyOffs;
	this.buf.append( "\tLD\tD,(IY-" );
	this.buf.append( iyOffs - 1 );
	this.buf.append( ")\n"
			+ "\tLD\tE,(IY-" );
	this.buf.append( iyOffs );
	this.buf.append( ")\n" );
      } else {
	this.buf.append( "\tLD\tD,(IY+" );
	this.buf.append( iyOffs + 1 );
	this.buf.append( ")\n"
			+ "\tLD\tE,(IY+" );
	this.buf.append( iyOffs );
	this.buf.append( ")\n" );
      }
    }
  }


  public void append_LD_HL_nn( int value )
  {
    if( this.enabled ) {
      this.buf.append( "\tLD\tHL," );
      appendHex4Internal( value );
      buf.append( (char) '\n');
    }
  }


  public void append_LD_HL_xx( String xx )
  {
    if( this.enabled && (xx != null) ) {
      buf.append( "\tLD\tHL," );
      buf.append( xx );
      buf.append( (char) '\n');
    }
  }


  public void append_LD_HL_IndirectIY( int iyOffs )
  {
    if( this.enabled ) {
      if( iyOffs < 0 ) {
	iyOffs = -iyOffs;
	this.buf.append( "\tLD\tH,(IY-" );
	this.buf.append( iyOffs - 1 );
	this.buf.append( ")\n"
			+ "\tLD\tL,(IY-" );
	this.buf.append( iyOffs );
	this.buf.append( ")\n" );
      } else {
	this.buf.append( "\tLD\tH,(IY+" );
	this.buf.append( iyOffs + 1 );
	this.buf.append( ")\n"
			+ "\tLD\tL,(IY+" );
	this.buf.append( iyOffs );
	this.buf.append( ")\n" );
      }
    }
  }


  public void append_LD_IndirectIY_HL( int iyOffs )
  {
    if( this.enabled ) {
      if( iyOffs < 0 ) {
	iyOffs = -iyOffs;
	this.buf.append( "\tLD\t(IY-" );
	this.buf.append( iyOffs - 1 );
	this.buf.append( "),H\n"
			+ "\tLD\t(IY-" );
	this.buf.append( iyOffs );
	this.buf.append( "),L\n" );
      } else {
	this.buf.append( "\tLD\t(IY+" );
	this.buf.append( iyOffs + 1 );
	this.buf.append( "),H\n"
			+ "\tLD\t(IY+" );
	this.buf.append( iyOffs );
	this.buf.append( "),L\n" );
      }
    }
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


  public void delete( int begPos, int endPos )
  {
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


  public void insert( int pos, CharSequence text)
  {
    if( (text != null) && this.enabled )
      this.buf.insert( pos, text );
  }


  public int length()
  {
    return this.buf.length();
  }


  public void newLine()
  {
    if( this.enabled )
      this.buf.append( (char) '\n' );
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


  public String substring( int pos )
  {
    return this.buf.substring( pos );
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
      this.buf.append( (char) '0' );
    }
    this.buf.append( String.format( "%02XH", value ) );
  }


  private void appendHex4Internal( int value )
  {
    value &= 0xFFFF;
    if( value >= 0xA000 ) {
      this.buf.append( (char) '0' );
    }
    this.buf.append( String.format( "%04XH", value ) );
  }
}
