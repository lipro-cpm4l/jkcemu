/*
 * (c) 2008-2012 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog fuer Eingabe von Bytes
 */

package jkcemu.tools.hexedit;

import java.awt.*;
import java.awt.event.*;
import java.io.ByteArrayOutputStream;
import java.lang.*;
import java.text.ParseException;
import java.util.EventObject;
import javax.swing.*;
import jkcemu.base.*;


public class ReplyBytesDlg extends BasicDlg
{
  public enum InputFormat { HEX8, DEC8, DEC16, DEC32, STRING };

  private byte[]       approvedBytes;
  private String       approvedText;
  private InputFormat  approvedInputFmt;
  private boolean      approvedBigEndian;
  private JRadioButton btnHex8;
  private JRadioButton btnDec8;
  private JRadioButton btnDec16;
  private JRadioButton btnDec32;
  private JRadioButton btnString;
  private JRadioButton btnLittleEndian;
  private JRadioButton btnBigEndian;
  private JLabel       labelByteOrder;
  private JTextField   fldInput;
  private JButton      btnPaste;
  private JButton      btnOK;
  private JButton      btnCancel;


  public ReplyBytesDlg(
		Window      owner,
		String      title,
		InputFormat inputFmt,
		boolean     bigEndian,
		String      text )
  {
    super( owner, title != null ? title : "Eingabe" );
    this.approvedBytes     = null;
    this.approvedText      = null;
    this.approvedInputFmt  = null;
    this.approvedBigEndian = false;


    // Fensterinhalt
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 0, 5 ),
						0, 0 );


    // Eingabebereich
    add( new JLabel( "Bytes eingeben als:" ), gbc );

    this.labelByteOrder = new JLabel( "Byte-Anordnung:" );
    gbc.gridx++;
    add( this.labelByteOrder, gbc );

    ButtonGroup grpType  = new ButtonGroup();
    ButtonGroup grpOrder = new ButtonGroup();

    this.btnHex8 = new JRadioButton( "8-Bit hexadezimale Zahlen", true );
    this.btnHex8.setMnemonic( KeyEvent.VK_H );
    this.btnHex8.addActionListener( this );
    grpType.add( this.btnHex8 );
    gbc.insets.top = 0;
    gbc.gridx      = 0;
    gbc.gridy++;
    add( this.btnHex8, gbc );

    this.btnLittleEndian = new JRadioButton( "Little Endian", !bigEndian );
    this.btnLittleEndian.setMnemonic( KeyEvent.VK_L );
    this.btnLittleEndian.addActionListener( this );
    grpOrder.add( this.btnLittleEndian );
    gbc.gridx++;
    add( this.btnLittleEndian, gbc );
  
    this.btnDec8 = new JRadioButton( "8-Bit Dezimalzahlen", false );
    this.btnDec8.setMnemonic( KeyEvent.VK_8 );
    this.btnDec8.addActionListener( this );
    grpType.add( this.btnDec8 );
    gbc.gridx = 0;
    gbc.gridy++;
    add( this.btnDec8, gbc );

    this.btnBigEndian = new JRadioButton( "Big Endian", bigEndian );
    this.btnBigEndian.setMnemonic( KeyEvent.VK_B );
    this.btnBigEndian.addActionListener( this );
    grpOrder.add( this.btnBigEndian );
    gbc.gridx++;
    add( this.btnBigEndian, gbc );
  
    this.btnDec16 = new JRadioButton( "16-Bit Dezimalzahlen", false );
    this.btnDec16.setMnemonic( KeyEvent.VK_6 );
    this.btnDec16.addActionListener( this );
    grpType.add( this.btnDec16 );
    gbc.gridx = 0;
    gbc.gridy++;
    add( this.btnDec16, gbc );

    this.btnDec32 = new JRadioButton( "32-Bit Dezimalzahlen", false );
    this.btnDec32.setMnemonic( KeyEvent.VK_3 );
    this.btnDec32.addActionListener( this );
    grpType.add( this.btnDec32 );
    gbc.gridy++;
    add( this.btnDec32, gbc );

    this.btnString = new JRadioButton( "ASCII-Zeichenkette", false );
    this.btnString.setMnemonic( KeyEvent.VK_A );
    this.btnString.addActionListener( this );
    grpType.add( this.btnString );
    gbc.insets.bottom = 5;
    gbc.gridy++;
    add( this.btnString, gbc );

    gbc.insets.top    = 5;
    gbc.insets.bottom = 0;
    gbc.gridwidth     = 2;
    gbc.gridy++;
    add( new JLabel( "Eingabe:" ), gbc );

    if( inputFmt != null ) {
      switch( inputFmt ) {
	case HEX8:
	  this.btnHex8.setSelected( true );
	  break;

	case DEC8:
	  this.btnDec8.setSelected( true );
	  break;

	case DEC16:
	  this.btnDec16.setSelected( true );
	  break;

	case DEC32:
	  this.btnHex8.setSelected( true );
	  break;

	case STRING:
	  this.btnString.setSelected( true );
	  break;
      }
    }

    JPanel panelInput = new JPanel( new GridBagLayout() );
    gbc.fill          = GridBagConstraints.HORIZONTAL;
    gbc.weightx       = 1.0;
    gbc.insets.top    = 0;
    gbc.insets.bottom = 5;
    gbc.gridy++;
    add( panelInput, gbc );

    GridBagConstraints gbcInput = new GridBagConstraints(
						0, 0,
						1, 1,
						1.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.HORIZONTAL,
						new Insets( 0, 0, 0, 0 ),
						0, 0 );

    this.fldInput = new JTextField();
    if( text != null ) {
      this.fldInput.setText( text );
    }
    this.fldInput.addActionListener( this );
    panelInput.add( this.fldInput, gbcInput );

    this.btnPaste = createImageButton(
                                "/images/edit/paste.png",
                                "Einf\u00FCgen" );
    gbcInput.fill        = GridBagConstraints.NONE;
    gbcInput.weightx     = 0.0;
    gbcInput.insets.left = 5;
    gbcInput.gridx++;
    panelInput.add( this.btnPaste, gbcInput );


    // Knoepfe
    JPanel panelBtn = new JPanel( new GridLayout( 1, 2, 5, 5 ) );
    gbc.anchor      = GridBagConstraints.CENTER;
    gbc.fill        = GridBagConstraints.NONE;
    gbc.weightx     = 0.0;
    gbc.insets.top  = 5;
    gbc.gridy++;
    add( panelBtn, gbc );

    this.btnOK = new JButton( "OK" );
    this.btnOK.addActionListener( this );
    panelBtn.add( this.btnOK );

    this.btnCancel = new JButton( "Abbrechen" );
    this.btnCancel.addActionListener( this );
    panelBtn.add( this.btnCancel );


    // Fenstergroesse und -position
    pack();
    setParentCentered();
    setResizable( true );


    // sonstiges
    updByteOrderFields();
  }


  public boolean getApprovedBigEndian()
  {
    return this.approvedBigEndian;
  }


  public byte[] getApprovedBytes()
  {
    return this.approvedBytes;
  }


  public InputFormat getApprovedInputFormat()
  {
    return this.approvedInputFmt;
  }


  public String getApprovedText()
  {
    return this.approvedText;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv = false;
    if( e != null ) {
      Object src = e.getSource();
      if( (src == this.btnHex8)
	  || (src == this.btnDec8)
	  || (src == this.btnDec16)
	  || (src == this.btnDec32)
	  || (src == this.btnString) )
      {
	rv = true;
	updByteOrderFields();
	this.fldInput.requestFocus();
      }
      else if( (src == this.btnLittleEndian) || (src == this.btnBigEndian) ) {
	rv = true;
	this.fldInput.requestFocus();
      }
      else if( (src == this.fldInput) || (src == this.btnOK) ) {
	rv = true;
	doApprove();
      }
      else if( src == this.btnCancel ) {
	rv = true;
	doClose();
      }
      else if( src == this.btnPaste ) {
	rv = true;
	this.fldInput.paste();
      }
    }
    return rv;
  }


  @Override
  public void windowOpened( WindowEvent e )
  {
    this.fldInput.requestFocus();
  }


	/* --- private Methoden --- */

  private void doApprove()
  {
    byte[] rv = null;
    try {
      InputFormat inputFmt  = null;
      boolean     bigEndian = this.btnBigEndian.isSelected();
      String      text      = this.fldInput.getText();
      if( text != null ) {
	int len = text.length();
	if( len > 0 ) {
	  if( this.btnString.isSelected() ) {
	    inputFmt = InputFormat.STRING;
	    rv       = new byte[ len ];
	    for( int i = 0; i < len; i++ ) {
	      char ch = text.charAt( i );
	      if( (ch < 0x20) || (ch > 0x7E) ) {
		throw new ParseException(
			String.format(
				"Das Zeichen \'%c\' ist kein ASCII-Zeichen.",
				ch ),
			i );
	      }
	      rv[ i ] = (byte) ch;
	    }
	  } else {
	    inputFmt         = InputFormat.HEX8;
	    int bytesPerItem = 1;
	    int radix        = 16;
	    if( this.btnDec8.isSelected() ) {
	      inputFmt = InputFormat.DEC8;
	      radix    = 10;
	    }
	    else if( this.btnDec16.isSelected() ) {
	      inputFmt     = InputFormat.DEC16;
	      bytesPerItem = 2;
	      radix        = 10;
	    }
	    else if( this.btnDec32.isSelected() ) {
	      inputFmt     = InputFormat.DEC32;
	      bytesPerItem = 4;
	      radix        = 10;
	    }
	    String[] items = text.toUpperCase().split( "[\\s,:;]" );
	    if( items != null ) {
	      if( items.length > 0 ) {
		ByteArrayOutputStream buf = new ByteArrayOutputStream(
						items.length * bytesPerItem );
		for( int i = 0; i < items.length; i++ ) {
		  String itemText = items[ i ];
		  if( itemText != null ) {
		    if( itemText.length() > 0 ) {
		      try {
			int value = Integer.parseInt( items[ i ], radix );
			int pos   = i * bytesPerItem;
			if( this.btnBigEndian.isSelected() ) {
			  for( int k = bytesPerItem - 1; k >= 0; --k ) {
			    if( k > 0 ) {
			      buf.write( (value >> (k * 8)) & 0xFF );
			    } else {
			      buf.write( value & 0xFF );
			    }
			  }
			} else {
			  for( int k = 0; k < bytesPerItem; k++ ) {
			    buf.write( value & 0xFF );
			    value >>= 8;
			  }
			}
		      }
		      catch( NumberFormatException ex ) {
			throw new ParseException(
				String.format(
					"%s: ung\u00FCltiges Format",
					items[ i ] ),
				i );
		      }
		    }
		  }
		}
		rv = buf.toByteArray();
	      }
	    }
	  }
	}
      }
      if( rv != null ) {
	this.approvedBytes     = rv;
	this.approvedText      = text;
	this.approvedInputFmt  = inputFmt;
	this.approvedBigEndian = false;
	doClose();
      }
    }
    catch( Exception ex ) {
      showErrorDlg( this, ex.getMessage() );
    }
  }


  private void updByteOrderFields()
  {
    boolean state = (this.btnDec16.isSelected() || this.btnDec32.isSelected());
    this.labelByteOrder.setEnabled( state );
    this.btnLittleEndian.setEnabled( state );
    this.btnBigEndian.setEnabled( state );
  }
}

