/*
 * (c) 2008-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Basisklasse fuer Fenster mit einer Hex-Character-Anzeige
 */

package jkcemu.tools.hexedit;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.security.NoSuchAlgorithmException;
import java.util.EventObject;
import java.util.Properties;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import jkcemu.Main;
import jkcemu.base.BaseDlg;
import jkcemu.base.BaseFrm;
import jkcemu.base.ByteDataSource;
import jkcemu.base.EmuUtil;
import jkcemu.base.GUIFactory;
import jkcemu.base.HexCharFld;
import jkcemu.base.ReplyBytesDlg;
import jkcemu.etc.CksCalculator;
import jkcemu.text.TextUtil;


public abstract class AbstractHexCharFrm
				extends BaseFrm
				implements
					ByteDataSource,
					CaretListener,
					Printable
{
  protected static final String PROP_DIRECT_EDIT = "direct_edit.enabled";

  protected HexCharFld                hexCharFld;
  protected ReplyBytesDlg.InputFormat lastInputFmt;
  protected boolean                   lastBigEndian;

  private String            lastCksAlgorithm;
  private String            lastFindText;
  private String            readOnlyErrorMsg;
  private int               findPos;
  private byte[]            findBytes;
  private JTextField        fldCaretDec;
  private JTextField        fldCaretHex;
  private JTextField        fldValue8;
  private JTextField        fldValue16;
  private JTextField        fldValue32;
  private JLabel            labelValue8;
  private JLabel            labelValue16;
  private JLabel            labelValue32;
  private JCheckBox         cbValueSigned;
  private JCheckBox         cbLittleEndian;
  private JCheckBoxMenuItem mnuDirectEdit;


  public AbstractHexCharFrm()
  {
    this.lastBigEndian    = false;
    this.lastInputFmt     = null;
    this.lastCksAlgorithm = null;
    this.lastFindText     = null;
    this.readOnlyErrorMsg = null;
    this.findPos          = 0;
    this.findBytes        = null;
    this.mnuDirectEdit    = null;
  }


  protected void addDirectEditMenuItemTo( JMenu menu )
  {
    this.mnuDirectEdit = GUIFactory.createCheckBoxMenuItem(
		"Bytes durch direkte Eingabe \u00FCberschreiben",
		Main.getBooleanProperty(
			getSettingsPrefix() + PROP_DIRECT_EDIT,
			false ) );
    this.mnuDirectEdit.addActionListener( this );
    menu.add( this.mnuDirectEdit );

    // ReadOnly-Fehlermeldung erzeugen
    StringBuilder buf      = new StringBuilder();
    String        menuText = menu.getText();
    buf.append( "Sie m\u00FCssen zuerst den Men\u00FCeintrag" );
    if( menuText != null ) {
      buf.append( "\n\'" );
      buf.append( menuText );
      buf.append( "\' \u2192" );
    }
    buf.append( " \'" );
    buf.append( this.mnuDirectEdit.getText() );
    buf.append( "\'\n"
		+ "aktivieren, bevor Sie die Bytes direkt"
		+ " in der Anzeige \u00E4ndern k\u00F6nnen." );
    this.readOnlyErrorMsg = buf.toString();
  }


  protected Component createCaretPosFld( String title )
  {
    JPanel panel = GUIFactory.createPanel( new GridBagLayout() );
    panel.setBorder( GUIFactory.createTitledBorder( title ) );

    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 5, 5 ),
					0, 0 );

    panel.add( GUIFactory.createLabel( "Hexadezimal:" ), gbc );

    this.fldCaretHex = GUIFactory.createTextField();
    this.fldCaretHex.addActionListener( this );
    gbc.fill    = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 0.5;
    gbc.gridx++;
    panel.add( this.fldCaretHex, gbc );

    gbc.fill    = GridBagConstraints.NONE;
    gbc.weightx = 0.0;
    gbc.gridx++;
    panel.add( GUIFactory.createLabel( "Dezimal:" ), gbc );

    this.fldCaretDec = GUIFactory.createTextField();
    this.fldCaretDec.addActionListener( this );
    gbc.fill    = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 0.5;
    gbc.gridx++;
    panel.add( this.fldCaretDec, gbc );

    return panel;
  }


  protected Component createHexCharFld()
  {
    JPanel panel = GUIFactory.createPanel( new BorderLayout() );

    this.hexCharFld = new HexCharFld( this );
    GUIFactory.initFont( this.hexCharFld );
    this.hexCharFld.setBorder( BorderFactory.createEtchedBorder() );
    this.hexCharFld.addCaretListener( this );
    panel.add(
	GUIFactory.createScrollPane( this.hexCharFld ),
	BorderLayout.CENTER );

    return panel;
  }


  protected Component createValueFld()
  {
    JPanel panel = GUIFactory.createPanel( new GridBagLayout() );
    panel.setBorder( GUIFactory.createTitledBorder(
			"Dezimalwerte der Bytes ab Cursor-Position" ) );

    GridBagConstraints gbcValue = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 2, 5 ),
					0, 0 );

    this.labelValue8 = GUIFactory.createLabel( "8 Bit:" );
    this.labelValue8.setEnabled( false );
    panel.add( this.labelValue8, gbcValue );

    this.fldValue8 = GUIFactory.createTextField();
    this.fldValue8.setEditable( false );
    gbcValue.fill    = GridBagConstraints.HORIZONTAL;
    gbcValue.weightx = 0.33;
    gbcValue.gridx++;
    panel.add( this.fldValue8, gbcValue );

    this.labelValue16 = GUIFactory.createLabel( "16 Bit:" );
    this.labelValue16.setEnabled( false );
    gbcValue.fill    = GridBagConstraints.NONE;
    gbcValue.weightx = 0.0;
    gbcValue.gridx++;
    panel.add( this.labelValue16, gbcValue );

    this.fldValue16 = GUIFactory.createTextField();
    this.fldValue16.setEditable( false );
    gbcValue.fill    = GridBagConstraints.HORIZONTAL;
    gbcValue.weightx = 0.33;
    gbcValue.gridx++;
    panel.add( this.fldValue16, gbcValue );

    this.labelValue32 = GUIFactory.createLabel( "32 Bit:" );
    this.labelValue32.setEnabled( false );
    gbcValue.fill    = GridBagConstraints.NONE;
    gbcValue.weightx = 0.0;
    gbcValue.gridx++;
    panel.add( this.labelValue32, gbcValue );

    this.fldValue32 = GUIFactory.createTextField();
    this.fldValue32.setEditable( false );
    gbcValue.fill    = GridBagConstraints.HORIZONTAL;
    gbcValue.weightx = 0.33;
    gbcValue.gridx++;
    panel.add( this.fldValue32, gbcValue );

    JPanel panelOpt = GUIFactory.createPanel(
			new FlowLayout( FlowLayout.LEFT, 5, 0 ) );
    gbcValue.weightx   = 1.0;
    gbcValue.gridwidth = GridBagConstraints.REMAINDER;
    gbcValue.gridx     = 0;
    gbcValue.gridy++;
    panel.add( panelOpt, gbcValue );

    this.cbValueSigned = GUIFactory.createCheckBox(
						"Vorzeichenbehaftet",
						true );
    this.cbValueSigned.addActionListener( this );
    this.cbValueSigned.setEnabled( false );
    panelOpt.add( this.cbValueSigned, gbcValue );

    this.cbLittleEndian = GUIFactory.createCheckBox(
						"Little Endian",
						true );
    this.cbLittleEndian.addActionListener( this );
    this.cbLittleEndian.setEnabled( false );
    panelOpt.add( this.cbLittleEndian, gbcValue );

    return panel;
  }


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
      int addr = caretPos + getAddrOffset();
      this.fldCaretDec.setText( Integer.toString( addr ) );
      this.fldCaretHex.setText( Integer.toHexString( addr ).toUpperCase() );
    } else {
      this.fldCaretDec.setText( "" );
      this.fldCaretHex.setText( "" );
    }
    updValueFields();
  }


  protected void updView()
  {
    this.hexCharFld.refresh();
    setContentActionsEnabled( getDataLength() > 0 );
    updValueFields();
  }


	/* --- ByteDataSource --- */

  @Override
  public int getAddrOffset()
  {
    return 0;
  }


  @Override
  abstract public int getDataByte( int idx );


  @Override
  abstract public int getDataLength();


  @Override
  public abstract boolean getDataReadOnly();


  @Override
  public boolean setDataByte( int addr, int value )
  {
    return false;
  }


	/* --- CaretListener --- */

  @Override
  public void caretUpdate( CaretEvent e )
  {
    updCaretPosFields();
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
      Font font = null;
      if( this.hexCharFld != null ) {
	font = this.hexCharFld.getFont();
      }
      if( font != null ) {
	font = font.deriveFont( (float) fontSize );
      } else {
	font = new Font( Font.MONOSPACED, Font.PLAIN, fontSize );
      }
      g.setFont( font );

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
		String.format( "%02X", getDataByte( idx ) & 0xFF ) );
	  } else {
	    buf.append( "\u0020\u0020" );
	  }
	}
	buf.append( "\u0020\u0020\u0020" );
	for( int i = 0; i < HexCharFld.BYTES_PER_ROW; i++ ) {
	  int idx = pos + i;
	  if( idx < dataLen ) {
	    int ch = getDataByte( idx ) & 0xFF;
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
  public void addNotify()
  {
    updDirectEditDependFields();
    super.addNotify();
  }


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
      else if( (src == this.cbValueSigned)
	       || (src == this.cbLittleEndian) )
      {
	rv = true;
	updValueFields();
      } else if( src == this.mnuDirectEdit ) {
	rv = true;
	updDirectEditDependFields();
      }
    }
    return rv;
  }


  @Override
  public void putSettingsTo( Properties props )
  {
    super.putSettingsTo( props );
    if( this.mnuDirectEdit != null ) {
      EmuUtil.setProperty(
		props,
		getSettingsPrefix() + PROP_DIRECT_EDIT,
		this.mnuDirectEdit.isSelected() );
    }
  }


	/* --- Aktionen --- */

  protected void doChecksum()
  {
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
	    BaseDlg.showInfoDlg(
		this,
		String.format(
			"%s des ausgew\u00E4hlten Bereichs: %s",
			cc.getAlgorithm(),
			value ) );
	  }
	}
	catch( NoSuchAlgorithmException ex ) {
	  BaseDlg.showErrorDlg(
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
	      if( getDataByte( idx ) !=
			((int) this.findBytes[ k ] & 0xFF) )
	      {
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
	    EventQueue.invokeLater(
			new Runnable()
			{
			  @Override
			  public void run()
			  {
			    doFindNext();
			  }
			} );
	  } else {
	    BaseDlg.showInfoDlg( this, "Byte-Folge nicht gefunden" );
	  }
	}
      }
    }
  }


  protected void doSelectAll()
  {
    int len = getDataLength();
    if( len > 0 ) {
      setSelection( 0, len - 1 );
      setContentActionsEnabled( true );
    }
  }


	/* --- private Methoden --- */

  private Long getLong( int pos, int len, boolean littleEndian )
  {
    Long rv = null;
    if( (pos >= 0) && (pos + len <= getDataLength()) ) {
      long value = 0L;
      if( littleEndian ) {
	for( int i = pos + len - 1; i >= pos; --i ) {
	  value = (value << 8) | (getDataByte( i ) & 0xFF);
	}
      } else {
	for( int i = 0; i < len; i++ ) {
	  value = (value << 8) | (getDataByte( pos + i ) & 0xFF);
	}
      }
      rv = value;
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
      BaseDlg.showErrorDlg( this, "Ung\u00FCltige Eingabe" );
    }
  }


  private void updDirectEditDependFields()
  {
    if( (this.hexCharFld != null) && (this.mnuDirectEdit != null) ) {
      this.hexCharFld.setEditable(
			this.mnuDirectEdit.isSelected(),
			this.readOnlyErrorMsg );
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

      boolean valueSigned  = this.cbValueSigned.isSelected();
      boolean littleEndian = this.cbLittleEndian.isSelected();

      if( valueSigned ) {
	text8 = Integer.toString( (int) (byte) getDataByte( caretPos ) );
      } else {
	text8 = Integer.toString( getDataByte( caretPos ) & 0xFF );
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

    this.cbValueSigned.setEnabled( state8 );
    this.cbLittleEndian.setEnabled( state16 );

    setSelectedByteActionsEnabled( state8 );
  }
}
