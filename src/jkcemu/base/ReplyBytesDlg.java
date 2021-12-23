/*
 * (c) 2008-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog fuer Eingabe von Bytes
 */

package jkcemu.base;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.io.ByteArrayOutputStream;
import java.text.ParseException;
import java.util.EventObject;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;


public class ReplyBytesDlg extends BaseDlg
{
  public enum InputFormat { HEX8, DEC8, DEC16, DEC32, STRING };

  private byte[]       approvedBytes;
  private String       approvedText;
  private InputFormat  approvedInputFmt;
  private boolean      approvedBigEndian;
  private boolean      notified;
  private JRadioButton rbHex8;
  private JRadioButton rbDec8;
  private JRadioButton rbDec16;
  private JRadioButton rbDec32;
  private JRadioButton rbString;
  private JRadioButton rbLittleEndian;
  private JRadioButton rbBigEndian;
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
    this.notified          = false;


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
    add( GUIFactory.createLabel( "Bytes eingeben als:" ), gbc );

    this.labelByteOrder = GUIFactory.createLabel( "Byte-Anordnung:" );
    gbc.gridx++;
    add( this.labelByteOrder, gbc );

    ButtonGroup grpType  = new ButtonGroup();
    ButtonGroup grpOrder = new ButtonGroup();

    this.rbHex8 = GUIFactory.createRadioButton(
					"8-Bit hexadezimale Zahlen",
					true );
    this.rbHex8.setMnemonic( KeyEvent.VK_H );
    grpType.add( this.rbHex8 );
    gbc.insets.top = 0;
    gbc.gridx      = 0;
    gbc.gridy++;
    add( this.rbHex8, gbc );

    this.rbLittleEndian = GUIFactory.createRadioButton(
						"Little Endian",
						!bigEndian );
    this.rbLittleEndian.setMnemonic( KeyEvent.VK_L );
    grpOrder.add( this.rbLittleEndian );
    gbc.gridx++;
    add( this.rbLittleEndian, gbc );
  
    this.rbDec8 = GUIFactory.createRadioButton( "8-Bit Dezimalzahlen" );
    this.rbDec8.setMnemonic( KeyEvent.VK_8 );
    grpType.add( this.rbDec8 );
    gbc.gridx = 0;
    gbc.gridy++;
    add( this.rbDec8, gbc );

    this.rbBigEndian = GUIFactory.createRadioButton(
						"Big Endian",
						bigEndian );
    this.rbBigEndian.setMnemonic( KeyEvent.VK_B );
    grpOrder.add( this.rbBigEndian );
    gbc.gridx++;
    add( this.rbBigEndian, gbc );
  
    this.rbDec16 = GUIFactory.createRadioButton( "16-Bit Dezimalzahlen" );
    this.rbDec16.setMnemonic( KeyEvent.VK_6 );
    grpType.add( this.rbDec16 );
    gbc.gridx = 0;
    gbc.gridy++;
    add( this.rbDec16, gbc );

    this.rbDec32 = GUIFactory.createRadioButton( "32-Bit Dezimalzahlen" );
    this.rbDec32.setMnemonic( KeyEvent.VK_3 );
    grpType.add( this.rbDec32 );
    gbc.gridy++;
    add( this.rbDec32, gbc );

    this.rbString = GUIFactory.createRadioButton( "ASCII-Zeichenkette" );
    this.rbString.setMnemonic( KeyEvent.VK_A );
    grpType.add( this.rbString );
    gbc.insets.bottom = 5;
    gbc.gridy++;
    add( this.rbString, gbc );

    gbc.insets.top    = 5;
    gbc.insets.bottom = 0;
    gbc.gridwidth     = 2;
    gbc.gridy++;
    add( GUIFactory.createLabel( "Eingabe:" ), gbc );

    if( inputFmt != null ) {
      switch( inputFmt ) {
	case HEX8:
	  this.rbHex8.setSelected( true );
	  break;

	case DEC8:
	  this.rbDec8.setSelected( true );
	  break;

	case DEC16:
	  this.rbDec16.setSelected( true );
	  break;

	case DEC32:
	  this.rbHex8.setSelected( true );
	  break;

	case STRING:
	  this.rbString.setSelected( true );
	  break;
      }
    }

    JPanel panelInput = GUIFactory.createPanel( new GridBagLayout() );
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

    this.fldInput = GUIFactory.createTextField();
    if( text != null ) {
      this.fldInput.setText( text );
    }
    panelInput.add( this.fldInput, gbcInput );

    this.btnPaste = GUIFactory.createRelImageResourceButton(
					this,
					"edit/paste.png",
	                                EmuUtil.TEXT_PASTE );
    gbcInput.fill        = GridBagConstraints.NONE;
    gbcInput.weightx     = 0.0;
    gbcInput.insets.left = 5;
    gbcInput.gridx++;
    panelInput.add( this.btnPaste, gbcInput );


    // Knoepfe
    JPanel panelBtn = GUIFactory.createPanel( new GridLayout( 1, 2, 5, 5 ) );
    gbc.anchor      = GridBagConstraints.CENTER;
    gbc.fill        = GridBagConstraints.NONE;
    gbc.weightx     = 0.0;
    gbc.insets.top  = 5;
    gbc.gridy++;
    add( panelBtn, gbc );

    this.btnOK = GUIFactory.createButtonOK();
    panelBtn.add( this.btnOK );

    this.btnCancel = GUIFactory.createButtonCancel();
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
  public void addNotify()
  {
    super.addNotify();
    if( !this.notified ) {
      this.notified = true;
      this.rbHex8.addActionListener( this );
      this.rbLittleEndian.addActionListener( this );
      this.rbDec8.addActionListener( this );
      this.rbBigEndian.addActionListener( this );
      this.rbDec16.addActionListener( this );
      this.rbDec32.addActionListener( this );
      this.rbString.addActionListener( this );
      this.fldInput.addActionListener( this );
      this.btnPaste.addActionListener( this );
      this.btnOK.addActionListener( this );
      this.btnCancel.addActionListener( this );
    }
  }


  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv  = false;
    Object  src = e.getSource();
    if( (src == this.rbHex8)
	|| (src == this.rbDec8)
	|| (src == this.rbDec16)
	|| (src == this.rbDec32)
	|| (src == this.rbString) )
    {
      rv = true;
      updByteOrderFields();
      this.fldInput.requestFocus();
    }
    else if( (src == this.rbLittleEndian) || (src == this.rbBigEndian) ) {
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
    return rv;
  }


  @Override
  public boolean getPackOnUIUpdate()
  {
    return true;
  }


  @Override
  public void removeNotify()
  {
    super.removeNotify();
    if( this.notified ) {
      this.notified = false;
      this.rbHex8.removeActionListener( this );
      this.rbLittleEndian.removeActionListener( this );
      this.rbDec8.removeActionListener( this );
      this.rbBigEndian.removeActionListener( this );
      this.rbDec16.removeActionListener( this );
      this.rbDec32.removeActionListener( this );
      this.rbString.removeActionListener( this );
      this.fldInput.removeActionListener( this );
      this.btnPaste.removeActionListener( this );
      this.btnOK.removeActionListener( this );
      this.btnCancel.removeActionListener( this );
    }
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
      boolean     bigEndian = this.rbBigEndian.isSelected();
      String      text      = this.fldInput.getText();
      if( text != null ) {
	int len = text.length();
	if( len > 0 ) {
	  if( this.rbString.isSelected() ) {
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
	    if( this.rbDec8.isSelected() ) {
	      inputFmt = InputFormat.DEC8;
	      radix    = 10;
	    }
	    else if( this.rbDec16.isSelected() ) {
	      inputFmt     = InputFormat.DEC16;
	      bytesPerItem = 2;
	      radix        = 10;
	    }
	    else if( this.rbDec32.isSelected() ) {
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
			if( this.rbBigEndian.isSelected() ) {
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
    boolean state = (this.rbDec16.isSelected() || this.rbDec32.isSelected());
    this.labelByteOrder.setEnabled( state );
    this.rbLittleEndian.setEnabled( state );
    this.rbBigEndian.setEnabled( state );
  }
}
