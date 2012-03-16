/*
 * (c) 2011-2012 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog zur Eingabe von Dateikopfdaten
 */

package jkcemu.base;

import java.awt.*;
import java.io.File;
import java.lang.*;
import java.util.EventObject;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import jkcemu.base.*;


public class ReplyFileHeadDlg extends BasicDlg implements DocumentListener
{
  public enum Option {
		BEGIN_ADDRESS,
		END_ADDRESS,
		START_ADDRESS,
		FILE_NAME_6,
		FILE_NAME_8,
		FILE_NAME_16,
		SCCH_FILE_TYPE };

  private boolean     approved;
  private int         approvedBegAddr;
  private int         approvedEndAddr;
  private int         approvedStartAddr;
  private char        approvedScchFileType;
  private String      approvedFileName;
  private HexDocument docBegAddr;
  private HexDocument docEndAddr;
  private HexDocument docStartAddr;
  private Document    docFileName;
  private JComboBox   comboScchFileType;
  private JTextField  fldFileName;
  private JButton     btnApprove;
  private JButton     btnCancel;


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
	    component       = new JTextField();
	    this.docBegAddr = new HexDocument(
				(JTextField) component, 4, labelText );
	    break;
	  case END_ADDRESS:
	    labelText         = "Endadresse (optional):";
	    component       = new JTextField();
	    this.docEndAddr = new HexDocument(
				(JTextField) component, 4, labelText );
	    break;
	  case START_ADDRESS:
	    labelText         = "Startadresse (optional):";
	    component         = new JTextField();
	    this.docStartAddr = new HexDocument(
				(JTextField) component, 4, labelText );
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
	    this.docFileName = new LimitedDocument( len, false );
	    this.fldFileName = new JTextField(
					this.docFileName,
					presetText,
					len );
	    component = this.fldFileName;
	    break;
	  case SCCH_FILE_TYPE:
	    labelText              = "Dateityp:";
	    this.comboScchFileType = new JComboBox( new Object[] {
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
	  add( new JLabel( labelText ), gbc );
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
    JPanel panelBtn   = new JPanel( new GridLayout( 1, 2, 5, 5 ) );
    gbc.gridwidth     = GridBagConstraints.REMAINDER;
    gbc.anchor        = GridBagConstraints.CENTER;
    gbc.fill          = GridBagConstraints.NONE;
    gbc.weightx       = 0.0;
    gbc.insets.bottom = 5;
    add( panelBtn, gbc );

    this.btnApprove = new JButton( approveBtnText );
    this.btnApprove.addActionListener( this );
    panelBtn.add( this.btnApprove );

    this.btnCancel = new JButton( "Abbrechen" );
    this.btnCancel.addActionListener( this );
    panelBtn.add( this.btnCancel );


    // Fenstergroesse und -position
    pack();
    setParentCentered();
    setResizable( false );


    // Vorbelegung
    if( (fileName != null)
	&& ((this.docBegAddr != null) || (this.docStartAddr != null)) )
    {
      int[] addrs = EmuUtil.extractAddressesFromFileName( fileName );
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
    if( this.docBegAddr != null ) {
      this.docBegAddr.addDocumentListener( this );
    }
    if( this.docEndAddr != null ) {
      this.docEndAddr.addDocumentListener( this );
    }
    if( this.docFileName != null ) {
      this.docFileName.addDocumentListener( this );
    }
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

