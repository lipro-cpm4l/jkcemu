/*
 * (c) 2008-2013 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog zum Setzen des Aenderungszeitpunktes
 */

package jkcemu.filebrowser;

import java.awt.*;
import java.io.File;
import java.lang.*;
import java.util.*;
import java.text.*;
import javax.swing.*;
import jkcemu.base.*;


public class LastModifiedDlg extends BasicDlg
{
  private FileBrowserFrm       fileBrowserFrm;
  private java.util.List<File> files;
  private int                  numChanged;
  private int                  numUnchanged;
  private DateFormat           dateFmt;
  private JRadioButton         btnCurrentTime;
  private JRadioButton         btnTimeInput;
  private JCheckBox            btnRecursive;
  private JTextField           fldTime;
  private JButton              btnOK;
  private JButton              btnCancel;


  public LastModifiedDlg(
		FileBrowserFrm       fileBrowserFrm,
		java.util.List<File> files )
  {
    super( fileBrowserFrm, "\u00C4nderungszeitpunkt" );
    this.fileBrowserFrm = fileBrowserFrm;
    this.files          = files;
    this.numChanged     = 0;
    this.numUnchanged   = 0;
    this.dateFmt        = DateFormat.getDateTimeInstance(
					DateFormat.MEDIUM,
					DateFormat.MEDIUM );
    this.dateFmt.setLenient( false );


    // Layout
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					2, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );


    // Fensterinhalt
    add( new JLabel( "\u00C4nderungszeitpunkt setzen auf:" ), gbc );

    ButtonGroup grpTime = new ButtonGroup();

    this.btnCurrentTime = new JRadioButton( "aktuellen Zeitpunkt", true );
    this.btnCurrentTime.addActionListener( this );
    grpTime.add( this.btnCurrentTime );
    gbc.insets.left = 50;
    gbc.gridy++;
    add( this.btnCurrentTime, gbc );

    this.btnTimeInput = new JRadioButton( "Datum/Uhrzeit:", false );
    this.btnTimeInput.addActionListener( this );
    grpTime.add( this.btnTimeInput );
    gbc.insets.top = 0;
    gbc.gridwidth  = 1;
    gbc.gridy++;
    add( this.btnTimeInput, gbc );

    this.fldTime = new JTextField();
    this.fldTime.setEditable( false );
    this.fldTime.addActionListener( this );
    gbc.insets.left = 5;
    gbc.fill        = GridBagConstraints.HORIZONTAL;
    gbc.weightx     = 1.0;
    gbc.gridx++;
    add( this.fldTime, gbc );

    this.btnRecursive = null;
    for( File file : this.files ) {
      if( file.isDirectory() ) {
	this.btnRecursive = new JCheckBox(
				"In Verzeichnisse hinein wechseln",
				false );
	gbc.insets.left = 50;
	gbc.fill        = GridBagConstraints.NONE;
	gbc.weightx     = 0.0;
	gbc.gridwidth   = 2;
	gbc.gridx       = 0;
	gbc.gridy++;
	add( this.btnRecursive, gbc );
	break;
      }
    }


    // Knoepfe
    JPanel panelBtn = new JPanel( new GridLayout( 1, 2, 5, 5 ) );

    this.btnOK = new JButton( "OK" );
    this.btnOK.addActionListener( this );
    panelBtn.add( this.btnOK );

    this.btnCancel = new JButton( "Abbrechen" );
    this.btnCancel.addActionListener( this );
    panelBtn.add( this.btnCancel );

    gbc.anchor        = GridBagConstraints.CENTER;
    gbc.fill          = GridBagConstraints.NONE;
    gbc.insets.top    = 10;
    gbc.insets.bottom = 10;
    gbc.weightx       = 0.0;
    gbc.gridwidth     = 2;
    gbc.gridx         = 0;
    gbc.gridy++;
    add( panelBtn, gbc );


    // Vorbelegung
    java.util.Date date = null;
    if( !files.isEmpty() ) {
      long t = files.get( 0 ).lastModified();
      if( t > 0L ) {
	date = new java.util.Date( t );
      }
    }
    if( date == null ) {
      date = new java.util.Date();
    }
    this.fldTime.setText( this.dateFmt.format( date ) );


    // Fenstergroesse und -position
    pack();
    setParentCentered();
    setResizable( true );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv = false;
    if( e != null ) {
      Object src = e.getSource();
      if( src != null ) {
	if( (src == this.btnCurrentTime) || (src == this.btnTimeInput) ) {
	  rv = true;
	  this.fldTime.setEditable( this.btnTimeInput.isSelected() );
	}
	else if( src == this.btnOK ) {
	  rv = true;
	  doApply();
	}
        else if( src == this.btnCancel ) {
	  rv = true;
	  doClose();
	}
      }
    }
    return rv;
  }


	/* --- private Methoden --- */

  private void doApply()
  {
    try {
      long time = -1L;
      if( this.btnCurrentTime.isSelected() ) {
	time = System.currentTimeMillis();
      }
      else if( this.btnTimeInput.isSelected() ) {
	String text = this.fldTime.getText();
	if( text != null ) {
	  java.util.Date date = this.dateFmt.parse( text );
	  if( date != null ) {
	    time = date.getTime();
	  }
	}
      }
      if( time <= 0 ) {
	showErrorDlg( this, "Sie m\u00FCssen eine Zeit festlegen." );
      } else {
	this.numChanged   = 0;
	this.numUnchanged = 0;
	try {
	  boolean recursive = false;
	  if( this.btnRecursive != null ) {
	    recursive = this.btnRecursive.isSelected();
	  }
	  for( File file : this.files ) {
	    setLastModified( file, time, recursive );
	  }
	}
	catch( Exception ex ) {}

	StringBuilder buf = new StringBuilder();
	buf.append( "\u00C4nderungszeitpunkt von\n" );
	buf.append( this.numChanged );
	if( this.numChanged == 1 ) {
	  buf.append( " Datei/Verzeichnis" );
	} else {
	  buf.append( " Dateien/Verzeichnissen" );
	}
	buf.append( " ge\u00E4ndert" );
	if( this.numUnchanged > 0 ) {
	  buf.append( "\n" );
	  buf.append( this.numUnchanged );
	  if( this.numUnchanged == 1 ) {
	    buf.append( " Datei/Verzeichnis konnte" );
	  } else {
	    buf.append( " Dateien/Verzeichnisse konnten" );
	  }
	  buf.append( " nicht ge\u00E4ndert werden." );
	}
	showInfoDlg( this, buf.toString() );
	doClose();
      }
    }
    catch( ParseException ex ) {
      showErrorDlg( this, "Datum/Uhrzeit: ung\u00FCltige Eingabe" );
    }
  }


  private void setLastModified( File file, long time, boolean recursive )
  {
    try {
      if( file.setLastModified( time ) ) {
	this.numChanged++;
      } else {
	this.numUnchanged++;
      }
    }
    catch( Exception ex ) {
      this.numUnchanged++;
    }
    if( recursive && file.isDirectory() ) {
      File[] files = file.listFiles();
      if( files != null ) {
	for( int i = 0; i < files.length; i++ ) {
	  String name = files[ i ].getName();
	  if( name != null ) {
	    if( !name.equals( "." ) && !name.equals( ".." ) ) {
	      setLastModified( files[ i ], time, recursive );
	    }
	  }
	}
      }
    }
  }
}

