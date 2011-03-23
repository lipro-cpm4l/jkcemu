/*
 * (c) 2008-2011 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Komponente fuer eine Hex-Character-Anzeige
 */

package jkcemu.tools.hexedit;

import java.awt.*;
import java.lang.*;
import javax.swing.*;
import jkcemu.base.EmuUtil;


public class HexCharFld extends JComponent
{
  public static final int BYTES_PER_ROW = 16;
  public static final int MARGIN        = 5;

  private static final int SEP_W  = 20;
  private static final int PAD_Y  = 1;

  private static final JComponent fontPrototypeFld = new JTextArea();

  private AbstractHexCharFrm dataSrc;
  private int                totalRows;
  private int                topRow;
  private int                caretPos;
  private int                markPos;
  private int                rowHeight;
  private int                wChar;
  private int                wHex;
  private int                xHex;
  private int                xAscii;
  private int                xOffset;
  private int                yOffset;


  public HexCharFld( AbstractHexCharFrm dataSrc )
  {
    this.dataSrc   = dataSrc;
    this.totalRows = 0;
    this.topRow    = 0;
    this.caretPos  = -1;
    this.markPos   = -1;
    this.rowHeight = 0;
    this.wChar     = 0;
    this.xHex      = 0;
    this.xAscii    = 0;
    this.xOffset   = 0;

    Font font = fontPrototypeFld.getFont();
    if( font == null ) {
      font = new Font( "Monospaced", Font.PLAIN, 12 );
    }
    setFont( font );
  }


  public String createAddrFmtString()
  {
    int maxAddr = this.dataSrc.getAddrOffset()
				+ this.dataSrc.getDataLength()
				- 1;
    if( maxAddr < 0 ) {
      maxAddr = 0;
    }
    int addrDigits = 0;
    while( maxAddr > 0 ) {
      addrDigits++;
      maxAddr >>= 4;
    }
    return String.format( "%%0%dX", addrDigits > 4 ? addrDigits : 4 );
  }


  public void copySelectedText()
  {
    int dataLen  = this.dataSrc.getDataLength();
    int caretPos = getCaretPosition();
    int markPos  = getMarkPosition();
    int m1       = -1;
    int m2       = -1;
    if( (caretPos >= 0) && (markPos >= 0) ) {
      m1 = Math.min( caretPos, markPos );
      m2 = Math.max( caretPos, markPos );
    } else {
      m1 = caretPos;
      m2 = caretPos;
    }
    if( m2 >= dataLen ) {
      m2 = dataLen - 1;
    }
    if( m1 >= 0 ) {
      int rows   = m1 / BYTES_PER_ROW;
      int pos    = rows * BYTES_PER_ROW;  // auf Anfang der Zeile setzen
      rows       = (m2 + BYTES_PER_ROW - 1) / BYTES_PER_ROW;
      int endPos = rows * BYTES_PER_ROW;

      StringBuilder buf = new StringBuilder(
		((m2 - m1) / BYTES_PER_ROW) * ((BYTES_PER_ROW * 4) + 12) );

      int    addrOffs = this.dataSrc.getAddrOffset();
      String addrFmt  = createAddrFmtString();
      while( pos < m2 ) {
	buf.append( String.format( addrFmt, addrOffs + pos ) );
	buf.append( "\u0020\u0020" );
	for( int i = 0; i < BYTES_PER_ROW; i++ ) {
	  int idx = pos + i;
	  if( idx < dataLen ) {
	    buf.append(
		String.format(
			" %02X",
			(int) this.dataSrc.getDataByte( idx ) & 0xFF ) );
	  } else {
	    buf.append( "\u0020\u0020\u0020" );
	  }
	}
	buf.append( "\u0020\u0020\u0020" );
	for( int i = 0; i < BYTES_PER_ROW; i++ ) {
	  int idx = pos + i;
	  if( idx >= dataLen ) {
	    break;
	  }
	  char ch = (char) this.dataSrc.getDataByte( pos + i );
	  if( (ch < 0x20) || (ch > 0x7F) ) {
	    ch = '.';
	  }
	  buf.append( (char) ch );
	}
	buf.append( (char) '\n' );
	pos += BYTES_PER_ROW;
      }
      if( buf.length() > 0 ) {
	EmuUtil.copyToClipboard( this, buf.toString() );
      }
    }
  }


  public int getCaretPosition()
  {
    return this.caretPos;
  }


  public int getCharWidth()
  {
    if( this.wChar == 0 ) {
      calcPositions();
    }
    return this.wChar;
  }


  public int getContentHeight()
  {
    return (this.totalRows + 2) * this.rowHeight + (2 * MARGIN);
  }


  public int getContentWidth()
  {
    if( this.wChar == 0 ) {
      calcPositions();
    }
    return this.xAscii + (BYTES_PER_ROW * this.wChar) + MARGIN;
  }


  public int getDataIndexAt( int x, int y )
  {
    int rv = -1;
    if( (this.wChar > 0) && (this.wHex > 0) && (y > MARGIN)
	&& (this.rowHeight > 0) )
    {
      int row  = this.topRow + ((y - MARGIN) / this.rowHeight);
      int xAbs = this.xOffset + x;
      if( (xAbs >= this.xHex)
	  && (xAbs < this.xHex + (BYTES_PER_ROW * this.wHex)) )
      {
	int m = (xAbs - this.xHex) % this.wHex;
	if( m < (2 * this.wChar) ) {
	  rv = (row * BYTES_PER_ROW) + ((xAbs - this.xHex) / this.wHex);
	}
      }
      else if( (xAbs >= this.xAscii)
	       && (xAbs < this.xAscii + (BYTES_PER_ROW * this.wChar)) )
      {
	rv = (row * BYTES_PER_ROW) + ((xAbs - this.xAscii) / this.wChar);
      }
    }
    if( rv > this.dataSrc.getDataLength() ) {
      rv = -1;
    }
    return rv;
  }


  public int getMarkPosition()
  {
    return this.markPos;
  }


  public int getRowHeight()
  {
    return this.rowHeight;
  }


  public int getYOffset()
  {
    return this.yOffset;
  }


  public void refresh()
  {
    this.totalRows = (this.dataSrc.getDataLength() + BYTES_PER_ROW - 1)
							/ BYTES_PER_ROW;
    this.topRow    = 0;
    this.caretPos  = -1;
    this.markPos   = -1;
    invalidate();
    repaint();
  }


  public void setCaretPosition( int pos, boolean moveOp )
  {
    this.caretPos = pos;
    if( !moveOp ) {
      this.markPos = pos;
    }
    repaint();
  }


  public void setXOffset( int xOffset )
  {
    this.xOffset = (xOffset > 0 ? xOffset : 0);
    repaint();
  }


  public void setYOffset( int yOffset )
  {
    this.yOffset = (yOffset > 0 ? yOffset : 0);
    repaint();
  }


  public void setSelection( int begPos, int endPos )
  {
    this.caretPos = endPos;
    this.markPos  = begPos;
    repaint();
  }


  public void updateUI()
  {
    super.updateUI();
    fontPrototypeFld.updateUI();
    Font font = fontPrototypeFld.getFont();
    if( font != null ) {
      setFont( font );
    }
  }


	/* --- ueverschriebene Methoden --- */

  @Override
  public Dimension getPreferredSize()
  {
    return new Dimension(
		getContentWidth(),
		(12 * this.rowHeight) + (2 * MARGIN) );
  }


  @Override
  public boolean isFocusable()
  {
    return true;
  }


  @Override
  public void paintComponent( Graphics g )
  {
    int w = getWidth();
    int h = getHeight();
    if( (w > 0) && (h > 0) && (this.rowHeight > 0) ) {
      if( this.wChar == 0 ) {
	calcPositions();
      }
      g.setColor( SystemColor.text );
      g.fillRect( 0, 0, w, h );
      if( this.xOffset > 0 ) {
	g.translate( -this.xOffset, 0 );
      }
      g.setFont( getFont() );
      g.setPaintMode();

      int hFont  = getFont().getSize();
      int y      = MARGIN + hFont;

      this.topRow = (this.yOffset - MARGIN) / this.rowHeight;
      if( this.topRow < 0 ) {
	this.topRow = 0;
      }
      int pos = this.topRow * BYTES_PER_ROW;
      int m1  = -1;
      int m2  = -1;
      if( (this.caretPos >= 0) && (this.markPos >= 0) ) {
	m1 = Math.min( this.caretPos, this.markPos );
	m2 = Math.max( this.caretPos, this.markPos );
      } else {
	m1 = this.caretPos;
	m2 = this.caretPos;
      }
      int    dataLen  = this.dataSrc.getDataLength();
      int    addrOffs = this.dataSrc.getAddrOffset();
      String addrFmt  = createAddrFmtString();
      while( (pos < dataLen) && (y < h + hFont) ) {
	g.setColor( SystemColor.textText );
	g.drawString( String.format( addrFmt, addrOffs + pos ), MARGIN, y );
	int x = this.xHex;
	for( int i = 0; i < BYTES_PER_ROW; i++ ) {
	  int idx = pos + i;
	  if( idx < dataLen ) {
	    if( (idx >= m1) && (idx <= m2) ) {
	      int nMark = 2;
	      if( (i < BYTES_PER_ROW - 1) && (idx < m2) ) {
		nMark = 3;
	      }
	      g.setColor( SystemColor.textHighlight );
	      g.fillRect(
			x,
			y - hFont + 2,
			nMark * this.wChar,
			this.rowHeight );
	      g.setColor( SystemColor.textHighlightText );
	    } else {
	      g.setColor( SystemColor.textText );
	    }
	    g.drawString(
		String.format( "%02X", this.dataSrc.getDataByte( idx ) ),
		x,
		y );
	    x += this.wHex;
	  } else {
	    break;
	  }
	}
	x = this.xAscii;
	for( int i = 0; i < BYTES_PER_ROW; i++ ) {
	  int idx = pos + i;
	  if( idx >= dataLen ) {
	    break;
	  }
	  char ch = (char) this.dataSrc.getDataByte( pos + i );
	  if( (ch < 0x20) || (ch > 0x7F) ) {
	    ch = '.';
	  }
	  if( (idx >= m1) && (idx <= m2) ) {
	    g.setColor( SystemColor.textHighlight );
	    g.fillRect( x, y - hFont + 2, this.wChar, this.rowHeight );
	    g.setColor( SystemColor.textHighlightText );
	  } else {
	    g.setColor( SystemColor.textText );
	  }
	  g.drawString( Character.toString( ch ), x, y );
	  x += this.wChar;
	}
	y   += this.rowHeight;
	pos += BYTES_PER_ROW;
      }
      if( this.xOffset > 0 ) {
	g.translate( this.xOffset, 0 );
      }
    }
  }


  @Override
  public void setFont( Font font )
  {
    super.setFont( font );
    this.rowHeight = getFont().getSize() + PAD_Y;
  }


	/* --- private Methoden --- */

  private void calcPositions()
  {
    FontMetrics fm = getFontMetrics( getFont() );
    if( fm != null ) {
      this.wChar  = fm.stringWidth( "0" );
      this.wHex   = 3 * wChar;
      this.xHex   = MARGIN + (6 * this.wChar) + SEP_W;
      this.xAscii = this.xHex + (BYTES_PER_ROW * this.wHex) + SEP_W;
    }
  }
}

