/*
 * (c) 2008-2013 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Basisklasse fuer Fenster mit einer Hex-Character-Anzeige
 */

package jkcemu.tools.hexedit;

import java.awt.*;
import java.awt.event.*;
import java.awt.print.*;
import java.lang.*;
import java.security.NoSuchAlgorithmException;
import java.util.EventObject;
import javax.swing.*;
import jkcemu.Main;
import jkcemu.base.*;
import jkcemu.etc.CksCalculator;


public abstract class AbstractHexCharFrm
				extends BasicFrm
				implements
					AdjustmentListener,
					ComponentListener,
					MouseMotionListener,
					MouseWheelListener,
					Printable
{
  protected HexCharFld                hexCharFld;
  protected ReplyBytesDlg.InputFormat lastInputFmt;
  protected boolean                   lastBigEndian;

  private String     lastCksAlgorithm;
  private String     lastFindText;
  private int        findPos;
  private byte[]     findBytes;
  private boolean    asciiSelected;
  private JScrollBar hScrollBar;
  private JScrollBar vScrollBar;
  private JTextField fldCaretDec;
  private JTextField fldCaretHex;
  private JTextField fldValue8;
  private JTextField fldValue16;
  private JTextField fldValue32;
  private JLabel     labelValue8;
  private JLabel     labelValue16;
  private JLabel     labelValue32;
  private JCheckBox  btnValueSigned;
  private JCheckBox  btnLittleEndian;


  public AbstractHexCharFrm()
  {
    this.lastBigEndian    = false;
    this.lastInputFmt     = null;
    this.lastCksAlgorithm = null;
    this.lastFindText     = null;
    this.findPos          = 0;
    this.findBytes        = null;
    this.asciiSelected    = false;
    Main.updIcon( this );
  }


  protected Component createCaretPosFld( String title )
  {
    JPanel panel = new JPanel( new GridBagLayout() );
    panel.setBorder( BorderFactory.createTitledBorder( title ) );

    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 5, 5 ),
					0, 0 );

    panel.add( new JLabel( "Hexadezimal:" ), gbc );

    this.fldCaretHex = new JTextField();
    this.fldCaretHex.addActionListener( this );
    gbc.fill    = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 0.5;
    gbc.gridx++;
    panel.add( this.fldCaretHex, gbc );

    gbc.fill    = GridBagConstraints.NONE;
    gbc.weightx = 0.0;
    gbc.gridx++;
    panel.add( new JLabel( "Dezimal:" ), gbc );

    this.fldCaretDec = new JTextField();
    this.fldCaretDec.addActionListener( this );
    gbc.fill    = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 0.5;
    gbc.gridx++;
    panel.add( this.fldCaretDec, gbc );

    return panel;
  }


  protected Component createHexCharFld()
  {
    JPanel panel = new JPanel( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
						0, 0,
						1, 1,
						1.0, 1.0,
						GridBagConstraints.NORTHWEST,
						GridBagConstraints.BOTH,
						new Insets( 0, 0, 0, 0 ),
						0, 0 );

    this.hexCharFld = new HexCharFld( this );
    this.hexCharFld.setBorder( BorderFactory.createEtchedBorder() );
    this.hexCharFld.addKeyListener( this );
    panel.add( this.hexCharFld, gbc );

    this.vScrollBar = new JScrollBar( JScrollBar.VERTICAL );
    this.vScrollBar.setMinimum( 0 );
    this.vScrollBar.setUnitIncrement( this.hexCharFld.getRowHeight() );
    this.vScrollBar.addAdjustmentListener( this );
    this.vScrollBar.addComponentListener( this );
    gbc.anchor  = GridBagConstraints.WEST;
    gbc.fill    = GridBagConstraints.VERTICAL;
    gbc.weightx = 0.0;
    gbc.gridx++;
    panel.add( this.vScrollBar, gbc );

    this.hScrollBar = new JScrollBar( JScrollBar.HORIZONTAL );
    this.hScrollBar.setMinimum( 0 );
    this.hScrollBar.setUnitIncrement( this.hexCharFld.getCharWidth() );
    this.hScrollBar.addAdjustmentListener( this );
    this.hScrollBar.addComponentListener( this );
    gbc.anchor  = GridBagConstraints.NORTH;
    gbc.fill    = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;
    gbc.weighty = 0.0;
    gbc.gridx   = 0;
    gbc.gridy++;
    panel.add( this.hScrollBar, gbc );

    this.hexCharFld.addMouseListener( this );
    this.hexCharFld.addMouseMotionListener( this );
    this.hexCharFld.addMouseWheelListener( this );

    return panel;
  }


  protected Component createValueFld()
  {
    JPanel panel = new JPanel( new GridBagLayout() );
    panel.setBorder( BorderFactory.createTitledBorder(
			"Dezimalwerte der Bytes ab Cursor-Position" ) );

    GridBagConstraints gbcValue = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 2, 5 ),
					0, 0 );

    this.labelValue8 = new JLabel( "8 Bit:" );
    this.labelValue8.setEnabled( false );
    panel.add( this.labelValue8, gbcValue );

    this.fldValue8 = new JTextField();
    this.fldValue8.setEditable( false );
    gbcValue.fill    = GridBagConstraints.HORIZONTAL;
    gbcValue.weightx = 0.33;
    gbcValue.gridx++;
    panel.add( this.fldValue8, gbcValue );

    this.labelValue16 = new JLabel( "16 Bit:" );
    this.labelValue16.setEnabled( false );
    gbcValue.fill    = GridBagConstraints.NONE;
    gbcValue.weightx = 0.0;
    gbcValue.gridx++;
    panel.add( this.labelValue16, gbcValue );

    this.fldValue16 = new JTextField();
    this.fldValue16.setEditable( false );
    gbcValue.fill    = GridBagConstraints.HORIZONTAL;
    gbcValue.weightx = 0.33;
    gbcValue.gridx++;
    panel.add( this.fldValue16, gbcValue );

    this.labelValue32 = new JLabel( "32 Bit:" );
    this.labelValue32.setEnabled( false );
    gbcValue.fill    = GridBagConstraints.NONE;
    gbcValue.weightx = 0.0;
    gbcValue.gridx++;
    panel.add( this.labelValue32, gbcValue );

    this.fldValue32 = new JTextField();
    this.fldValue32.setEditable( false );
    gbcValue.fill    = GridBagConstraints.HORIZONTAL;
    gbcValue.weightx = 0.33;
    gbcValue.gridx++;
    panel.add( this.fldValue32, gbcValue );

    JPanel panelOpt = new JPanel(
			new FlowLayout( FlowLayout.LEFT, 5, 0 ) );
    gbcValue.weightx   = 1.0;
    gbcValue.gridwidth = GridBagConstraints.REMAINDER;
    gbcValue.gridx     = 0;
    gbcValue.gridy++;
    panel.add( panelOpt, gbcValue );

    this.btnValueSigned = new JCheckBox( "Vorzeichenbehaftet", true );
    this.btnValueSigned.addActionListener( this );
    this.btnValueSigned.setEnabled( false );
    panelOpt.add( this.btnValueSigned, gbcValue );

    this.btnLittleEndian = new JCheckBox( "Little Endian", true );
    this.btnLittleEndian.addActionListener( this );
    this.btnLittleEndian.setEnabled( false );
    panelOpt.add( this.btnLittleEndian, gbcValue );

    return panel;
  }


  public int getAddrOffset()
  {
    return 0;
  }


  abstract public byte getDataByte( int idx );
  abstract public int  getDataLength();


  protected String getFileNameToPrint()
  {
    return null;
  }


  protected void setCaretPosition( int pos, boolean moveOp )
  {
    this.hexCharFld.setCaretPosition( pos, moveOp );
    updCaretPosFields();
  }


  protected void setContentActionsEnabled( boolean state )
  {
    // leer
  }


  protected void setFindNextActionsEnabled( boolean state )
  {
    // leer
  }


  protected void setSelectedByteActionsEnabled( boolean state )
  {
    // leer
  }


  protected void setSelection( int begPos, int endPos )
  {
    this.hexCharFld.setSelection( begPos, endPos );
    updCaretPosFields();
  }


  protected void updCaretPosFields()
  {
    int caretPos = this.hexCharFld.getCaretPosition();
    if( (caretPos >= 0) && (caretPos < getDataLength()) ) {
      int row = (caretPos + HexCharFld.BYTES_PER_ROW - 1)
					/ HexCharFld.BYTES_PER_ROW;
      int hRow   = this.hexCharFld.getRowHeight();
      int hFld   = this.hexCharFld.getHeight();
      int yCaret = HexCharFld.MARGIN + (row * hRow);
      int yOffs  = this.hexCharFld.getYOffset();

      if( yCaret < yOffs + hRow ) {
	changeVScrollBar( -(yOffs + hRow - yCaret) );
      }
      else if( yCaret > yOffs + hFld - hRow ) {
	changeVScrollBar( yCaret - (yOffs + hFld - hRow ) );
      }
      int addr = caretPos + getAddrOffset();
      this.fldCaretDec.setText( Integer.toString( addr ) );
      this.fldCaretHex.setText( Integer.toHexString( addr ).toUpperCase() );
    } else {
      this.fldCaretDec.setText( "" );
      this.fldCaretHex.setText( "" );
    }
    updValueFields();
  }


  protected void updScrollBar( Component c )
  {
    if( c == this.hScrollBar ) {
      updScrollBar(
		this.hScrollBar,
		this.hScrollBar.getWidth(),
		this.hexCharFld.getContentWidth() );
    }
    else if( c == this.vScrollBar ) {
      updScrollBar(
		this.vScrollBar,
		this.vScrollBar.getHeight(),
		this.hexCharFld.getContentHeight() );
    }
  }


  protected void updView()
  {
    this.hexCharFld.refresh();
    updScrollBar( this.vScrollBar );
    updScrollBar( this.hScrollBar );
    setContentActionsEnabled( getDataLength() > 0 );
    updValueFields();
  }


	/* --- AdjustmentListener --- */

  @Override
  public void adjustmentValueChanged( AdjustmentEvent e )
  {
    if( e.getSource() == this.vScrollBar ) {
      int yOffset  = 0;
      int yOffsMax = this.hexCharFld.getContentHeight()
				- this.hexCharFld.getHeight();
      if( yOffsMax > 0 ) {
	int maxValue = this.vScrollBar.getMaximum()
				- this.vScrollBar.getVisibleAmount();
	if( maxValue > 0 ) {
	  yOffset = (int) Math.round( (double) this.vScrollBar.getValue()
						/ (double) maxValue
						* (double) yOffsMax );
	}
      }
      this.hexCharFld.setYOffset( yOffset );
    }
    else if( e.getSource() == this.hScrollBar ) {
      int xOffset  = 0;
      int xOffsMax = this.hexCharFld.getContentWidth()
				- this.hexCharFld.getWidth();
      if( xOffsMax > 0 ) {
	int maxValue = this.hScrollBar.getMaximum()
				- this.hScrollBar.getVisibleAmount();
	if( maxValue > 0 ) {
	  xOffset = (int) Math.round( (double) this.hScrollBar.getValue()
						/ (double) maxValue
						* (double) xOffsMax );
	}
      }
      this.hexCharFld.setXOffset( xOffset );
    }
  }


	/* --- ComponentListener --- */

  @Override
  public void componentHidden( ComponentEvent e )
  {
    // leer
  }


  @Override
  public void componentMoved( ComponentEvent e )
  {
    // leer
  }


  @Override
  public void componentResized( ComponentEvent e )
  {
    updScrollBar( e.getComponent() );
  }


  @Override
  public void componentShown( ComponentEvent e )
  {
    updScrollBar( e.getComponent() );
  }


	/* --- MouseMotionListener --- */

  @Override
  public void mouseDragged( MouseEvent e )
  {
    if( (getDataLength() > 0)
	&& (e.getComponent() == this.hexCharFld) )
    {
      int pos = this.hexCharFld.getDataIndexAt( e.getX(), e.getY() );
      if( pos >= 0 ) {
	setCaretPosition( pos, true );
      }
      e.consume();
    }
  }


  @Override
  public void mouseMoved( MouseEvent e )
  {
    // leer
  }


	/* --- MouseWheelListener --- */

  @Override
  public void mouseWheelMoved( MouseWheelEvent e )
  {
    if( e.getComponent() == this.hexCharFld ) {
      int diffValue = 0;
      switch( e.getScrollType() ) {
	case MouseWheelEvent.WHEEL_UNIT_SCROLL:
	  diffValue = this.vScrollBar.getUnitIncrement();
	  break;

	case MouseWheelEvent.WHEEL_BLOCK_SCROLL:
	  diffValue = this.vScrollBar.getBlockIncrement();
	  break;
      }
      if( diffValue > 0 ) {
	diffValue *= e.getWheelRotation();
      }
      changeVScrollBar( diffValue );
      e.consume();
    }
  }


	/* --- Printable --- */

  @Override
  public int print(
		Graphics   g,
		PageFormat pf,
		int        pageNum ) throws PrinterException
  {
    int rv       = NO_SUCH_PAGE;
    int fontSize = Main.getPrintFontSize();
    if( fontSize < 1 ) {
      fontSize = 12;
    }
    int    rowHeight = fontSize + 1;
    int    nRows     = ((int) pf.getImageableHeight() - 1) / rowHeight;
    String fileName  = null;
    if( Main.getPrintFileName() ) {
      fileName = getFileNameToPrint();
    }
    if( (fileName != null) || Main.getPrintPageNum() ) {
      nRows -= 2;
    }
    if( nRows < 1 ) {
      throw new PrinterException( "Druckbarer Bereich zu klein" );
    }
    int dataLen = getDataLength();
    int pos     = nRows * HexCharFld.BYTES_PER_ROW * pageNum;
    if( pos < dataLen ) {
      g.setFont( new Font( "Monospaced", Font.PLAIN, fontSize ) );

      String        addrFmt = this.hexCharFld.createAddrFmtString();
      StringBuilder buf     = new StringBuilder( addrFmt.length() + 6
					+ (4 * HexCharFld.BYTES_PER_ROW) );

      int x = (int) pf.getImageableX() + 1;
      int y = (int) pf.getImageableY() + rowHeight;

      int addrOffs = getAddrOffset();
      while( (nRows > 0) && (pos < dataLen) ) {
	buf.setLength( 0 );
	buf.append( String.format( addrFmt, addrOffs + pos ) );
	buf.append( "\u0020\u0020" );
	for( int i = 0; i < HexCharFld.BYTES_PER_ROW; i++ ) {
	  buf.append( "\u0020" );
	  int idx = pos + i;
	  if( idx < dataLen ) {
	    buf.append(
		String.format( "%02X", (int) getDataByte( idx ) & 0xFF ) );
	  } else {
	    buf.append( "\u0020\u0020" );
	  }
	}
	buf.append( "\u0020\u0020\u0020" );
	for( int i = 0; i < HexCharFld.BYTES_PER_ROW; i++ ) {
	  int idx = pos + i;
	  if( idx < dataLen ) {
	    int ch = (int) getDataByte( idx ) & 0xFF;
	    if( (ch < 0x20) || (ch > 0x7E) ) {
	      ch = '.';
	    }
	    buf.append( (char) ch );
	  } else {
	    break;
	  }
	}
	g.drawString( buf.toString(), x, y );
	y   += rowHeight;
	pos += HexCharFld.BYTES_PER_ROW;
	--nRows;
      }
      if( (fileName != null) || Main.getPrintPageNum() ) {
	y += rowHeight;
      }
      if( fileName != null ) {
	g.drawString( fileName, x, y );
	if( Main.getPrintPageNum() ) {
	  FontMetrics fm = g.getFontMetrics();
	  if( fm != null ) {
	    String s = Integer.toString( pageNum + 1 );
	    int    w = fm.stringWidth( s );
	    if( w > 0 ) {
	      g.drawString( s, x + ((int) pf.getImageableWidth() - 1) - w, y );
	    }
	  }
	}
      } else {
	if( Main.getPrintPageNum() ) {
	  FontMetrics fm = g.getFontMetrics();
	  if( fm != null ) {
	    String s = String.format( "- %d -", pageNum + 1 );
	    int    w = fm.stringWidth( s );
	    if( w > 0 ) {
	      g.drawString(
			s,
			x + (((int) pf.getImageableWidth() - 1) - w) / 2,
			y );
	    }
	  }
	}
      }
      rv = PAGE_EXISTS;
    }
    return rv;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv  = false;
    Object  src = e.getSource();
    if( src != null ) {
      if( src == this.fldCaretHex ) {
	rv = true;
	setCaretPosition( this.fldCaretHex, 16 );
      }
      else if( src == this.fldCaretDec ) {
	rv = true;
	setCaretPosition( this.fldCaretDec, 10 );
      }
      else if( (src == this.btnValueSigned)
	       || (src == this.btnLittleEndian) )
      {
	rv = true;
	updValueFields();
      }
    }
    return rv;
  }


  @Override
  public void keyPressed( KeyEvent e )
  {
    if( e.getSource() == this.hexCharFld ) {
      int hRow     = 0;
      int caretPos = this.hexCharFld.getCaretPosition();
      if( caretPos >= 0 ) {
	boolean state   = false;
	int     dataLen = getDataLength();
	switch( e.getKeyCode() ) {
	  case KeyEvent.VK_LEFT:
	    --caretPos;
	    state = true;
	    break;

	  case KeyEvent.VK_RIGHT:
	    caretPos++;
	    state = true;
	    break;

	  case KeyEvent.VK_UP:
	    caretPos -= HexCharFld.BYTES_PER_ROW;
	    state = true;
	    break;

	  case KeyEvent.VK_DOWN:
	    caretPos += HexCharFld.BYTES_PER_ROW;
	    state = true;
	    break;

	  case KeyEvent.VK_PAGE_UP:
	    hRow = this.hexCharFld.getRowHeight();
	    if( hRow > 0 ) {
	      caretPos -= ((this.hexCharFld.getHeight() / hRow)
						* HexCharFld.BYTES_PER_ROW);
	      while( caretPos < 0 ) {
		caretPos += HexCharFld.BYTES_PER_ROW;
	      }
	    }
	    state = true;
	    break;

	  case KeyEvent.VK_PAGE_DOWN:
	    hRow = this.hexCharFld.getRowHeight();
	    if( hRow > 0 ) {
	      caretPos += ((this.hexCharFld.getHeight() / hRow)
						* HexCharFld.BYTES_PER_ROW);
	      while( caretPos >= dataLen ) {
		caretPos += HexCharFld.BYTES_PER_ROW;
	      }
	    }
	    state = true;
	    break;

	  case KeyEvent.VK_BEGIN:
	    caretPos = 0;
	    state    = true;
	    break;

	  case KeyEvent.VK_END:
	    caretPos = dataLen - 1;
	    state    = true;
	    break;
	}
	if( state ) {
	  e.consume();
	  if( (caretPos >= 0) && (caretPos < dataLen) ) {
	    setCaretPosition( caretPos, e.isShiftDown() );
	  }
	}
      }
    }
  }


  @Override
  public void lookAndFeelChanged()
  {
    this.hexCharFld.repaint();
  }


  @Override
  public void mousePressed( MouseEvent e )
  {
    if( e.getComponent() == this.hexCharFld ) {
      if( getDataLength() > 0 ) {
	int pos = this.hexCharFld.getDataIndexAt( e.getX(), e.getY() );
	if( pos >= 0 ) {
	  setCaretPosition( pos, e.isShiftDown() );
	}
      }
      this.hexCharFld.requestFocus();
      e.consume();
    } else {
      super.mouseClicked( e );
    }
  }


	/* --- Aktionen --- */

  protected void doBytesCopy()
  {
    int dataLen  = getDataLength();
    int caretPos = this.hexCharFld.getCaretPosition();
    int markPos  = this.hexCharFld.getMarkPosition();
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
      StringBuilder buf = new StringBuilder( (m2 - m1 + 1) * 3 );
      boolean       sp  = false;
      while( m1 <= m2 ) {
	if( sp ) {
	  buf.append( '\u0020' );
	}
	byte b = getDataByte( m1++ );
	buf.append( EmuUtil.getHexChar( b >> 4 ) );
	buf.append( EmuUtil.getHexChar( b ) );
	sp = true;
      }
      if( buf.length() > 0 ) {
	EmuUtil.copyToClipboard( this, buf.toString() );
      }
    }
  }


  protected void doChecksum()
  {
    int dataLen  = getDataLength();
    int caretPos = this.hexCharFld.getCaretPosition();
    int markPos  = this.hexCharFld.getMarkPosition();
    int m1       = -1;
    int m2       = -1;
    if( (caretPos >= 0) && (markPos >= 0) ) {
      m1 = Math.min( caretPos, markPos );
      m2 = Math.max( caretPos, markPos );
    }
    if( (m1 >= 0) && (m1 <= m2) ) {
      String algorithm = ReplyCksAlgorithmDlg.askCksAlgorithm(
						this,
						this.lastCksAlgorithm );
      if( algorithm != null ) {
	try {
	  CksCalculator cc      = new CksCalculator( algorithm );
	  this.lastCksAlgorithm = algorithm;
	  while( m1 <= m2 ) {
	    cc.update( getDataByte( m1++ ) );
	  }
	  String value = cc.getValue();
	  if( value != null ) {
	    BasicDlg.showInfoDlg(
		this,
		String.format(
			"%s des ausgew\u00E4hlten Bereichs: %s",
			cc.getAlgorithm(),
			value ) );
	  }
	}
	catch( NoSuchAlgorithmException ex ) {
	  BasicDlg.showErrorDlg(
		this,
		algorithm + ": Unbekannter bzw. nicht"
			+ " unterst\u00FCtzter Algorithmus" );
	}
      }
    }
  }


  protected void doFind()
  {
    ReplyBytesDlg dlg = new ReplyBytesDlg(
					this,
					"Bytes suchen",
					this.lastInputFmt,
					this.lastBigEndian,
					this.lastFindText );
    dlg.setVisible( true );
    byte[] a = dlg.getApprovedBytes();
    if( a != null ) {
      if( a.length > 0 ) {
	this.lastInputFmt  = dlg.getApprovedInputFormat();
	this.lastBigEndian = dlg.getApprovedBigEndian();
	this.lastFindText  = dlg.getApprovedText();
	this.findBytes     = a;
	this.findPos       = 0;
	int caretPos       = this.hexCharFld.getCaretPosition();
	if( (caretPos >= 0) && (caretPos < getDataLength()) ) {
	  this.findPos = caretPos;
	}
	doFindNext();
	setFindNextActionsEnabled( true );
      }
    }
  }


  protected void doFindNext()
  {
    if( this.findBytes != null ) {
      if( this.findBytes.length > 0 ) {
	int foundAt = -1;
	if( this.findPos < 0 ) {
	  this.findPos = 0;
	}
	int dataLen = getDataLength();
	for( int i = this.findPos; i < dataLen; i++ ) {
	  boolean found = true;
	  for( int k = 0; k < this.findBytes.length; k++ ) {
	    int idx = i + k;
	    if( idx < dataLen ) {
	      if( getDataByte( idx ) != this.findBytes[ k ] ) {
		found = false;
		break;
	      }
	    } else {
	      found = false;
	      break;
	    }
	  }
	  if( found ) {
	    foundAt = i;
	    break;
	  }
	}
	if( foundAt >= 0 ) {
	  /*
	   * Es wird rueckwaerts selektiert, damit der Cursor
	   * auf der ersten, d.h. der gefundenen Position steht.
	   */
	  this.hexCharFld.setSelection(
				foundAt + this.findBytes.length - 1,
				foundAt );
	  this.findPos = foundAt + 1;
	  updCaretPosFields();
	} else {
	  if( this.findPos > 0 ) {
	    this.findPos = 0;
	    doFindNext();
	  } else {
	    BasicDlg.showInfoDlg( this, "Byte-Folge nicht gefunden" );
	  }
	}
      }
    }
  }


	/* --- private Methoden --- */

  private void changeVScrollBar( int diffValue )
  {
    if( diffValue != 0 ) {
      int oldValue = this.vScrollBar.getValue();
      int newValue = oldValue + diffValue;
      if( newValue < this.vScrollBar.getMinimum() ) {
	newValue = this.vScrollBar.getMinimum();
      }
      else if( newValue > this.vScrollBar.getMaximum() ) {
	newValue = this.vScrollBar.getMaximum();
      }
      if( newValue != oldValue ) {
	this.vScrollBar.setValue( newValue );
      }
    }
  }


  private Long getLong( int pos, int len, boolean littleEndian )
  {
    Long rv = null;
    if( (pos >= 0) && (pos + len <= getDataLength()) ) {
      long value = 0L;
      if( littleEndian ) {
	for( int i = pos + len - 1; i >= pos; --i ) {
	  value = (value << 8) | ((int) (getDataByte( i )) & 0xFF);
	}
      } else {
	for( int i = 0; i < len; i++ ) {
	  value = (value << 8) | ((int) (getDataByte( pos + i )) & 0xFF);
	}
      }
      rv = new Long( value );
    }
    return rv;
  }


  private void setCaretPosition( JTextField textFld, int radix )
  {
    boolean done = false;
    String  text = textFld.getText();
    if( text != null ) {
      try {
	int pos = Integer.parseInt( text, radix ) - getAddrOffset();
	if( pos < 0 ) {
	  pos = 0;
	}
	int dataLen = getDataLength();
	if( pos >= dataLen ) {
	  pos = dataLen - 1;
	}
	setCaretPosition( pos, false );
	done = true;
      }
      catch( NumberFormatException ex ) {}
    }
    if( !done ) {
      BasicDlg.showErrorDlg( this, "Ung\u00FCltige Eingabe" );
    }
  }


  private void updScrollBar(
			JScrollBar scrollBar,
			int        visibleSize,
			int        fullSize )
  {
    if( (visibleSize > 0) && (fullSize > 0) ) {
      if( visibleSize > fullSize ) {
	visibleSize = fullSize;
      }
      scrollBar.setMaximum( fullSize );
      scrollBar.setBlockIncrement( visibleSize );
      scrollBar.setVisibleAmount( visibleSize );
    }
  }


  private void updValueFields()
  {
    String  text8    = null;
    String  text16   = null;
    String  text32   = null;
    boolean state8   = false;
    boolean state16  = false;
    int     caretPos = this.hexCharFld.getCaretPosition();
    if( (caretPos >= 0) && (caretPos < getDataLength()) ) {
      state8 = true;

      boolean valueSigned  = this.btnValueSigned.isSelected();
      boolean littleEndian = this.btnLittleEndian.isSelected();

      if( valueSigned ) {
	text8 = Integer.toString( (int) (byte) getDataByte( caretPos ) );
      } else {
	text8 = Integer.toString( (int) getDataByte( caretPos ) & 0xFF );
      }

      Long value = getLong( caretPos, 2, littleEndian );
      if( value != null ) {
	state16 = true;
	if( valueSigned ) {
	  text16 = Integer.toString( (int) value.shortValue() );
	} else {
	  text16 = value.toString();
	}
      }

      value = getLong( caretPos, 4, littleEndian );
      if( value != null ) {
	if( valueSigned ) {
	  text32 = Integer.toString( value.intValue() );
	} else {
	  text32 = value.toString();
	}
      }
    }
    this.fldValue8.setText( text8 );
    this.fldValue16.setText( text16 );
    this.fldValue32.setText( text32 );

    this.labelValue8.setEnabled( text8 != null );
    this.labelValue16.setEnabled( text16 != null );
    this.labelValue32.setEnabled( text32 != null );

    this.btnValueSigned.setEnabled( state8 );
    this.btnLittleEndian.setEnabled( state16 );

    setSelectedByteActionsEnabled( state8 );
  }
}

