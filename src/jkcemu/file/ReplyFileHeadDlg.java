/*
 * (c) 2011-2020 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog zur Eingabe von Dateikopfdaten
 */

package jkcemu.file;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.EventObject;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import jkcemu.base.BaseDlg;
import jkcemu.base.GUIFactory;
import jkcemu.base.HexDocument;
import jkcemu.base.LimitedDocument;
import jkcemu.base.UserInputException;


public class ReplyFileHeadDlg extends BaseDlg implements DocumentListener
{
  public enum Option {
		BEGIN_ADDRESS,
		END_ADDRESS,
		START_ADDRESS,
		FILE_NAME_6,
		FILE_NAME_8,
		FILE_NAME_16,
		SCCH_FILE_TYPE };

  private boolean           approved;
  private int               approvedBegAddr;
  private int               approvedEndAddr;
  private int               approvedStartAddr;
  private char              approvedScchFileType;
  private String            approvedFileName;
  private HexDocument       docBegAddr;
  private HexDocument       docEndAddr;
  private HexDocument       docStartAddr;
  private LimitedDocument   docFileName;
  private JComboBox<String> comboScchFileType;
  private JTextField        fldFileName;
  private JButton           btnApprove;
  private JButton           btnCancel;


  public ReplyFileHeadDlg(
		Window    parent,
		String    fileName,
		String    approveBtnText,
		String    title,
		Option... options )
  {
    super( parent, title );
    this.approved             = false;
    this.approvedBegAddr      = 0;
    this.approvedEndAddr      = 0;
    this.approvedStartAddr    = 0;
    this.approvedScchFileType = 0;
    this.approvedFileName     = null;
    this.docBegAddr           = null;
    this.docStartAddr         = null;
    this.fldFileName          = null;


    // Layout
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 5, 5 ),
					0, 0 );


    // Eingabefelder
    if( options != null ) {
      for( int i = 0; i < options.length; i++ ) {
	if( i == 0 ) {
	  gbc.insets.bottom = 0;
	} else if( i == (options.length - 1) ) {
	  gbc.insets.bottom = 5;
	}
	int       len        = 0;
	String    presetText = null;
	String    labelText  = null;
	Component component  = null;
	switch( options[ i ] ) {
	  case BEGIN_ADDRESS:
	    labelText       = "Anfangsadresse:";
	    this.docBegAddr = new HexDocument( 4, labelText );
	    component       = GUIFactory.createTextField(
						this.docBegAddr,
						0 );
	    break;
	  case END_ADDRESS:
	    labelText       = "Endadresse (optional):";
	    this.docEndAddr = new HexDocument( 4, labelText );
	    component       = GUIFactory.createTextField(
						this.docEndAddr,
						0 );
	    break;
	  case START_ADDRESS:
	    labelText         = "Startadresse (optional):";
	    this.docStartAddr = new HexDocument( 4, labelText );
	    component         = GUIFactory.createTextField(
						this.docStartAddr,
						0 );
	    break;
	  case FILE_NAME_6:
	  case FILE_NAME_8:
	  case FILE_NAME_16:
	    if( fileName != null ) {
	      int dot = fileName.indexOf( '.' );
	      if( dot > 1 ) {
		presetText = fileName.substring( 0, dot );
	      } else {
		presetText = fileName;
	      }
	      presetText = presetText.toUpperCase();
	    }
	    labelText = "Dateiname:";
	    if( options[ i ] == Option.FILE_NAME_6 ) {
	      len = 6;
	    } else if( options[ i ] == Option.FILE_NAME_8 ) {
	      len = 8;
	    } else {
	      len = 16;
	    }
	    this.docFileName = new LimitedDocument( len );
	    this.docFileName.setAsciiOnly( true );
	    this.fldFileName = GUIFactory.createTextField(
						this.docFileName,
						presetText,
						len );
	    component = this.fldFileName;
	    break;
	  case SCCH_FILE_TYPE:
	    labelText              = "Dateityp:";
	    this.comboScchFileType = GUIFactory.createComboBox(
						new String[] {
							"P Programm",
							"D Daten",
							"B BASIC-Programm",
							"F BASIC-Feld" } );
	    this.comboScchFileType.setEditable( false );
	    component = this.comboScchFileType;
	    break;
	}
	if( (labelText != null) && (component != null) ) {
	  gbc.fill    = GridBagConstraints.NONE;
	  gbc.weightx = 0.0;
	  add( GUIFactory.createLabel( labelText ), gbc );
	  gbc.fill    = GridBagConstraints.HORIZONTAL;
	  gbc.weightx = 1.0;
	  gbc.gridx++;
	  add( component, gbc );
	  gbc.gridx = 0;
	  gbc.gridy++;
	}
      }
    }


    // Knoepfe
    JPanel panelBtn   = GUIFactory.createPanel(
					new GridLayout( 1, 2, 5, 5 ) );
    gbc.gridwidth     = GridBagConstraints.REMAINDER;
    gbc.anchor        = GridBagConstraints.CENTER;
    gbc.fill          = GridBagConstraints.NONE;
    gbc.weightx       = 0.0;
    gbc.insets.bottom = 5;
    add( panelBtn, gbc );

    this.btnApprove = GUIFactory.createButton( approveBtnText );
    panelBtn.add( this.btnApprove );

    this.btnCancel = GUIFactory.createButtonCancel();
    panelBtn.add( this.btnCancel );


    // Fenstergroesse und -position
    pack();
    setParentCentered();
    setResizable( false );


    // Vorbelegung
    if( (fileName != null)
	&& ((this.docBegAddr != null) || (this.docStartAddr != null)) )
    {
      int[] addrs = FileUtil.extractAddressesFromFileName( fileName );
      if( addrs != null ) {
	if( (addrs.length > 0) && (this.docBegAddr != null) ) {
	  this.docBegAddr.setValue( addrs[ 0 ], 4 );
	}
	if( (addrs.length > 1) && (this.docEndAddr != null) ) {
	  this.docEndAddr.setValue( addrs[ 1 ], 4 );
	}
	if( (addrs.length > 2) && (this.docStartAddr != null) ) {
	  this.docStartAddr.setValue( addrs[ 2 ], 4 );
	}
      }
    }


    // Aktivierung/Deaktivierung des Aktionsknopfes
    checkApprovedBtnEnabled();


    // Listener
    if( this.docBegAddr != null ) {
      this.docBegAddr.addDocumentListener( this );
    }
    if( this.docEndAddr != null ) {
      this.docEndAddr.addDocumentListener( this );
    }
    if( this.docFileName != null ) {
      this.docFileName.addDocumentListener( this );
    }
    this.btnApprove.addActionListener( this );
    this.btnCancel.addActionListener( this );
  }


  public int getApprovedBeginAddress()
  {
    return this.approvedBegAddr;
  }


  public int getApprovedEndAddress()
  {
    return this.approvedEndAddr;
  }


  public String getApprovedFileName()
  {
    return this.approvedFileName;
  }


  public char getApprovedSCCHFileType()
  {
    return this.approvedScchFileType;
  }


  public int getApprovedStartAddress()
  {
    return this.approvedStartAddr;
  }


  public boolean wasApproved()
  {
    return this.approved;
  }


	/* --- DocumentListener --- */

  @Override
  public void changedUpdate( DocumentEvent e )
  {
    docChanged( e );
  }


  @Override
  public void insertUpdate( DocumentEvent e )
  {
    docChanged( e );
  }


  @Override
  public void removeUpdate( DocumentEvent e )
  {
    docChanged( e );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv = false;
    if( e != null ) {
      Object src = e.getSource();
      if( src != null ) {
        if( src == this.btnApprove ) {
	  rv = true;
	  try {
	    if( this.docBegAddr != null ) {
	      this.approvedBegAddr = this.docBegAddr.intValue();
	    }
	    if( this.docEndAddr != null ) {
	      Integer addr = this.docEndAddr.getInteger();
	      if( addr != null ) {
		this.approvedEndAddr = addr.intValue();
	      } else {
		this.approvedEndAddr = -1;
	      }
	    }
	    if( this.docStartAddr != null ) {
	      Integer addr = this.docStartAddr.getInteger();
	      if( addr != null ) {
		this.approvedStartAddr = addr.intValue();
	      } else {
		this.approvedStartAddr = -1;
	      }
	    }
	    if( this.fldFileName != null ) {
	      this.approvedFileName = this.fldFileName.getText();
	      if( this.approvedFileName != null ) {
		if( this.approvedFileName.isEmpty() ) {
		  this.approvedFileName = null;
		}
	      }
	      if( this.approvedFileName == null ) {
		throw new UserInputException( "Dateiname nicht angegeben" );
	      }
	    }
	    if( this.comboScchFileType != null ) {
	      char   ch = 'P';
	      Object o  = this.comboScchFileType.getSelectedItem();
	      if( o != null ) {
		String s = o.toString();
		if( s != null ) {
		  if( !s.isEmpty() ) {
		    ch = s.charAt( 0 );
		  }
		}
	      }
	      this.approvedScchFileType = ch;
	    }
	    this.approved = true;
	    doClose();
	  }
	  catch( Exception ex ) {
	    showErrorDlg( this, ex );
	  }
	}
        else if( src == this.btnCancel ) {
	  this.approved = false;
	  rv            = true;
	  doClose();
	}
      }
    }
    return rv;
  }


	/* --- private Methoden --- */

  private void checkApprovedBtnEnabled()
  {
    boolean state = true;
    try {
      if( this.docBegAddr != null ) {
	this.docBegAddr.intValue();		// Wert pruefen
      }
      if( this.docFileName != null ) {
	if( this.docFileName.getLength() <= 0 ) {
	  state = false;
	}
      }
    }
    catch( NumberFormatException ex ) {
      state = false;
    }
    this.btnApprove.setEnabled( state );
  }


  private void docChanged( DocumentEvent e )
  {
    Document doc = e.getDocument();
    if( (doc != null)
	&& (doc == this.docBegAddr) || (doc == this.docFileName) )
    {
      checkApprovedBtnEnabled();
    }
  }
}
