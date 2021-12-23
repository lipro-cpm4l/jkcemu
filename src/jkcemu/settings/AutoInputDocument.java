/*
 * (c) 2015-2020 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dokument fuer den Eingabetext eines AutoInput-Eintrag
 *
 * Es gibt 3 verschiedene Arten von Texten, die die Klasse verwaltet:
 *   1. der im Dokument, d.h. in der Superklasse PlainDocument,
 *      gespeicherte Text (docText)
 *   2. der in JTextField angezeigte Text, d.h.,
 *      der durch die Methoden "getText" gelieferete Text (viewText)
 *   3. der Text, der im Profil gespeichert und an das emulierte System
 *      weitergegeben wird (rawText),
 *      Ueber die Methode "insertString" wird rawText eingegeben.
 *
 * ASCII-Zeichen sind in allen 3 Textarten gleich.
 * Setuerzeichen werden in eine lesbare Form umgewandelt bzw. ersetzt.
 * Dabei gibt es zwei Arten von Ersetzungen:
 *   1. 1:1-Ersetzung (Cursor-Tasten, Enter),
 *      Die Ersatzzeichen sind im docText und viewTetx gleich.
 *   2. 1:n-Ersetzung (CTRL-Steuercodes, andere Steuertasten)
 *      Hier unterscheidet sich der Text in allen drei Textarten.
 *      Beispiel: CTRL-C
 *        rawText:  "\u0003"
 *        viewText: "<^C>"
 *        docText:  "\uE03C\uE15E\uE243\uE33E"
 *                  In den unteren 8 Bit sind die sichtbaren ASCII-Zeichen
 *                  (viewText) enthalten,
 *                  In den oberen 8 Bit sind die Bytes E0h, E1h, E2h, ...
 *                  enthalten, wobei davon die oberen 4 Bit (Eh)
 *                  als Kennung fuer diese Form der Ersetzung stehen
 *                  und die unteren 4 Bit die Postion innerhalb
 *                  der Ersatzzeichenkette enthalten.
 *                  Mit dieser Position ist es moeglich, den Anfang und
 *                  das Ende eines auf dieser Art und Weise ersetzten
 *                  Steuerzeichens zu erkennen. Das wird verwendet,
 *                  um das Einfuegen von Zeichen innerhalb einer
 *                  Ersatzzeichenkette oder das Loeschen nur eines Teils
 *                  einer Ersatzzeichenkette zu unterbinden.
 *                  In so einem Fall werden die Einfuegeposition bzw.
 *                  die Loeschpositionen entsprechend angepasst.
 */

package jkcemu.settings;

import java.io.CharArrayWriter;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import javax.swing.text.PlainDocument;
import javax.swing.text.Segment;
import jkcemu.base.AutoInputCharSet;
import jkcemu.base.EmuUtil;


public class AutoInputDocument extends PlainDocument
{
  /*
   * Die Klasse wird abgeleitet,
   * um direkten Zugriff auf das intere char-Array zu erhalten.
   * Dadurch kann dieses Array direkt in einem Segment-Objekt referenziert
   * und so das doppelte Duplizieren der Zeichen beim Auslesen
   * (char-Array -> String) und anschliessendem Umwandeln
   * (String -> charArray) vermieden werden.
   * Allerdings darf ein Objekt der Klasse nach dem Aufruf von "getBuf()"
   * nicht mehr veraendert werden.
   */
  private static class MyCharArrayWriter extends CharArrayWriter
  {
    private MyCharArrayWriter()
    {
      super( 32 );
    }

    private char[] getBuf()
    {
      return this.buf;
    }
  };


  private static final int MAGIC_MASK  = 0xF000;
  private static final int MAGIC_VALUE = 0xE000;
  private static final int IDX_MASK    = 0x0F00;

  private AutoInputCharSet charSet;
  private boolean          swapCase;
  private Segment          docText;


  public AutoInputDocument( AutoInputCharSet charSet, boolean swapCase )
  {
    this.charSet  = charSet;
    this.swapCase = swapCase;
    this.docText  = new Segment();
  }


  public void copy( JTextComponent textComp, boolean remove )
  {
    try {
      int begPos = textComp.getSelectionStart();
      int endPos = textComp.getSelectionEnd();
      if( (begPos >= 0) && (begPos < endPos) ) {
	begPos = toBegOfSpecialChar( begPos );
	endPos = toEndOfSpecialChar( endPos );
	if( (begPos >= 0) && (begPos < endPos) ) {
	  updDocText();
	  int len = this.docText.length();
	  if( len > 0 ) {
	    StringBuilder buf     = new StringBuilder( len );
	    int           viewPos = 0;
	    char          ch      = this.docText.first();
	    while( ch != Segment.DONE ) {
	      if( (viewPos >= begPos) && (viewPos < endPos) ) {
		if( (ch & MAGIC_MASK) == MAGIC_VALUE ) {
		  if( ((((int) ch) >> 8) & 0x000F) == 0 ) {
		    buf.append( (char) (ch & 0x00FF) );
		  }
		} else {
		  buf.append( this.charSet.getCharByView( ch ) );
		}
	      }
	      viewPos++;
	      ch = this.docText.next();
	    }
	    if( EmuUtil.copyToClipboard( textComp, buf.toString() ) ) {
	      if( remove ) {
		super.remove( begPos, endPos - begPos );
	      }
	    }
	  }
	}
      }
    }
    catch( BadLocationException ex ) {}
  }


  public String getRawText()
  {
    String rv = "";
    try {
      updDocText();
      int len = this.docText.length();
      if( len > 0 ) {
	StringBuilder buf = new StringBuilder( len );
	char          ch  = this.docText.first();
	while( ch != Segment.DONE ) {
	  if( (ch & MAGIC_MASK) == MAGIC_VALUE ) {
	    if( ((((int) ch) >> 8) & 0x000F) == 0 ) {
	      buf.append( (char) (ch & 0x00FF) );
	    }
	  } else {
	    buf.append( this.charSet.getCharByView( ch ) );
	  }
	  ch = this.docText.next();
	}
	rv = buf.toString();
      }
    }
    catch( BadLocationException ex ) {}
    return rv;
  }


  public boolean getSwapCase()
  {
    return this.swapCase;
  }


  public void setSwapCase( boolean state )
  {
    this.swapCase = state;
  }


  public int insertRawText(
			int    offs,
			String text ) throws BadLocationException
  {
    int crsPos = offs;
    if( text != null ) {
      StringBuilder buf = new StringBuilder();
      int           len = text.length();
      for( int i = 0; i <len; i++ ) {
	char ch = text.charAt( i );
	if( ((int) ch & MAGIC_MASK) != MAGIC_VALUE ) {
	  String view = this.charSet.getViewByChar( ch );
	  if( (view == null)
	      && (ch < '\u001B')
	      && this.charSet.containsCtrlCodes() )
	  {
	    view = toCtrlView( ch );
	  }
	  if( view != null ) {
	    int vLen = view.length();
	    if( vLen == 1 ) {
	      buf.append( view.charAt( 0 ) );
	    } else if( vLen > 1 ) {
	      for( int k = 0; k < vLen; k++ ) {
		buf.append( (char) (MAGIC_VALUE
					| ((k << 8) & IDX_MASK)
					| ch ) );
	      }
	    }
	  } else {
	    if( this.charSet.contains( ch ) ) {
	      buf.append( toModelCase( ch ) );
	    }
	  }
	}
      }
      len = buf.length();
      if( len > 0 ) {
	offs   = toBegOfSpecialChar( offs );
	crsPos = offs + len;
	super.insertString( offs, buf.toString(), null );
      }
    }
    return crsPos;
  }


  public void paste( JTextComponent textComp )
  {
    try {
      int pos = textComp.getCaretPosition();
      if( pos >= 0 ) {
	String text = EmuUtil.getClipboardText( textComp );
	if( text != null ) {
	  int len = text.length();
	  if( len > 0 ) {
	    if( this.swapCase ) {
	      StringBuilder buf = new StringBuilder( len );
	      for( int i = 0; i < len; i++ ) {
		buf.append( toModelCase( text.charAt( i ) ) );
	      }
	      text = buf.toString();
	    }
	    insertRawText( pos, text );
	  }
	}
      }
    }
    catch( BadLocationException ex ) {}
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public String getText( int offs, int len ) throws BadLocationException
  {
    String rv = "";
    try {
      rv = getViewChars().toString().substring( offs, offs + len );
    }
    catch( IndexOutOfBoundsException ex ) {
      throw new BadLocationException( "getText", offs + len );
    }
    return rv;
  }


  @Override
  public void getText(
		int     offs,
		int     len,
		Segment seg ) throws BadLocationException
  {
    if( offs < 0 ) {
      throw new BadLocationException( "offs < 0", offs );
    }
    MyCharArrayWriter buf = getViewChars();
    if( (offs + len) > buf.size() ) {
      throw new BadLocationException( "offs + len < text_len", offs + len );
    }
    seg.array  = buf.getBuf();
    seg.count  = len;
    seg.offset = offs;
  }


  @Override
  public void insertString(
			int          offs,
			String       text,
			AttributeSet a ) throws BadLocationException
  {
    insertRawText( offs, text );
  }


  @Override
  public void remove( int offs, int len ) throws BadLocationException
  {
    int endPos = toEndOfSpecialChar( offs + len );
    offs       = toBegOfSpecialChar( offs );
    super.remove( offs, endPos - offs );
  }


	/* --- private Methoden --- */

  private char docCharAt( int idx ) throws BadLocationException
  {
    updDocText();
    if( (idx < 0) || (idx >= this.docText.length()) ) {
      throw new BadLocationException( "docCharAt", idx );
    }
    return this.docText.charAt( idx );
  }


  public MyCharArrayWriter getViewChars() throws BadLocationException
  {
    updDocText();
    MyCharArrayWriter buf = new MyCharArrayWriter();
    char              ch  = this.docText.first();
    while( ch != Segment.DONE ) {
      if( (ch & MAGIC_MASK) == MAGIC_VALUE ) {
	int    idx   = (((int) ch) >> 8) & 0x000F;
	char   chRaw = (char) (ch & 0xFF);
	String view  = this.charSet.getViewByChar( chRaw );
	if( (view == null) && (chRaw >= '\u0001') && (chRaw < '\u001B') ) {
	  view = toCtrlView( chRaw );
	}
	ch = '?';
	if( view != null ) {
	  if( idx < view.length() ) {
	    ch = view.charAt( idx );
	  }
	}
      } else {
	String view = this.charSet.getViewByChar( ch );
	if( view != null ) {
	  if( !view.isEmpty() ) {
	    ch = view.charAt( 0 );
	  }
	}
      }
      buf.write( ch );
      ch = this.docText.next();
    }
    return buf;
  }


  private int toBegOfSpecialChar( int offs ) throws BadLocationException
  {
    if( (offs >= 0) && (offs < getLength()) ) {
      int c = (int) docCharAt( offs );
      if( (c & MAGIC_MASK) == MAGIC_VALUE ) {
	int idx = (c >> 8) & 0x000F;
	if( idx > 0 ) {
	  offs -= idx;
	}
      }
    }
    return offs > 0 ? offs : 0;
  }


  private int toEndOfSpecialChar( int offs ) throws BadLocationException
  {
    if( offs < 0 ) {
      offs = 0;
    }
    int len = getLength();
    while( offs < len ) {
      int c = (int) docCharAt( offs );
      if( (c & MAGIC_MASK) != MAGIC_VALUE ) {
	break;
      }
      if( ((c >> 8) & 0x000F) == 0 ) {
	break;
      }
      offs++;
    }
    return offs;
  }


  private static String toCtrlView( char ch )
  {
    return String.format( "<^%c>", ch + '\u0040' );
  }


  private char toModelCase( char ch )
  {
    if( this.swapCase ) {
      if( (ch >= 'A') && (ch <= 'Z') ) {
	ch = Character.toLowerCase( ch );
      } else if( (ch >= 'a') && (ch <= 'z') ) {
	ch = Character.toUpperCase( ch );
      }
    }
    return ch;
  }


  private void updDocText() throws BadLocationException
  {
    super.getText( 0, getLength(), this.docText );
  }
}
