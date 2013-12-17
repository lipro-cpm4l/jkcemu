/*
 * (c) 2008-2013 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog zum Setzen des Aenderungszeitpunktes
 */

package jkcemu.filebrowser;

import java.awt.*;
import java.io.*;
import java.lang.*;
import java.util.*;
import java.util.zip.*;
import java.text.*;
import javax.swing.*;
import jkcemu.base.*;


public class LastModifiedDlg extends BasicDlg
{
  private FileBrowserFrm       fileBrowserFrm;
  private java.util.List<File> files;
  private int                  numChanged;
  private int                  numUnchanged;
  private boolean              suppressArchiveErrDlg;
  private DateFormat           dateFmt;
  private JRadioButton         btnCurrentTime;
  private JRadioButton         btnTimeInput;
  private JCheckBox            btnRecursive;
  private JCheckBox            btnWithinArchiveFiles;
  private JTextField           fldTime;
  private JButton              btnOK;
  private JButton              btnCancel;


  public LastModifiedDlg(
		FileBrowserFrm       fileBrowserFrm,
		java.util.List<File> files )
  {
    super( fileBrowserFrm, "\u00C4nderungszeitpunkt" );
    this.fileBrowserFrm        = fileBrowserFrm;
    this.files                 = files;
    this.numChanged            = 0;
    this.numUnchanged          = 0;
    this.suppressArchiveErrDlg = false;
    this.dateFmt               = DateFormat.getDateTimeInstance(
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

    this.btnRecursive = new JCheckBox(
			"In Verzeichnisse hinein wechseln",
			false );
    gbc.insets.top  = 10;
    gbc.insets.left = 5;
    gbc.fill        = GridBagConstraints.NONE;
    gbc.weightx     = 0.0;
    gbc.gridwidth   = 2;
    gbc.gridx       = 0;
    gbc.gridy++;
    add( this.btnRecursive, gbc );

    this.btnWithinArchiveFiles = new JCheckBox(
			"In JAR- und ZIP-Dateien hinein wechseln",
			false );
    gbc.insets.top = 0;
    gbc.gridy++;
    add( this.btnWithinArchiveFiles, gbc );

    boolean hasDirs     = false;
    boolean hasArchives = false;
    for( File file : this.files ) {
      if( file.isDirectory() ) {
	hasDirs = true;
      }
      if( file.isFile() ) {
	String fName = file.getName();
	if( fName != null ) {
	  fName = fName.toLowerCase();
	  if( fName.endsWith( ".jar" )  || fName.endsWith( ".zip" ) ) {
	    hasArchives = true;
	  }
	}
      }
      if( hasDirs && hasArchives ) {
	break;
      }
    }
    this.btnRecursive.setEnabled( hasDirs );
    this.btnWithinArchiveFiles.setEnabled( hasArchives );


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
	this.numChanged            = 0;
	this.numUnchanged          = 0;
	this.suppressArchiveErrDlg = false;
	try {
	  boolean recursive    = this.btnRecursive.isSelected();
	  boolean archiveFiles = this.btnWithinArchiveFiles.isSelected();
	  for( File file : this.files ) {
	    setLastModified( file, time, recursive, archiveFiles );
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


  private void setLastModified(
			File file,
			long time,
			boolean recursive,
			boolean archiveFiles )
  {
    if( archiveFiles ) {
      String fName = file.getName();
      if( fName != null ) {
	fName = fName.toLowerCase();
	if( fName.endsWith( ".jar" ) || fName.endsWith( ".zip" ) ) {
	  setLastModifiedInZIP( file, time );
	}
      }
    }
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
	      setLastModified( files[ i ], time, recursive, archiveFiles );
	    }
	  }
	}
      }
    }
  }


  private void setLastModifiedInZIP( File file, long time )
  {
    InputStream     in      = null;
    ZipInputStream  zipIn   = null;
    ZipOutputStream zipOut  = null;
    File            fileOut = null;
    try {
      if( !file.canWrite() ) {
	throw new IOException( "Datei ist schreibgesch\u00FCtzt." );
      }
      in    = new FileInputStream( file );
      zipIn = new ZipInputStream( in );

      // ZIP-Datei konnte geoeffnet werden -> Ausgabedatei anlegen
      File dirFile = file.getParentFile();
      if( dirFile == null ) {
	dirFile = new File( "." );
      }
      fileOut = File.createTempFile( "jkcemu_", ".zip", dirFile );
      zipOut  = new ZipOutputStream( new FileOutputStream( fileOut ) );

      // Datei kopieren und dabei die Zeitstempel anpassen
      ZipEntry entry = zipIn.getNextEntry();
      while( entry != null ) {
	entry.setTime( time );
	zipOut.putNextEntry( entry );
	int b = zipIn.read();
	while( b >= 0 ) {
	  zipOut.write( b );
	  b = zipIn.read();
	}
	zipOut.closeEntry();
	zipIn.closeEntry();
	entry = zipIn.getNextEntry();
      }
      zipOut.finish();
      zipOut.close();
      zipOut = null;

      zipIn.close();
      zipIn = null;

      // alte Datei loeschen und neue umbenennen
      file.delete();
      fileOut.renameTo( file );
    }
    catch( Exception ex ) {
      EmuUtil.doClose( in );
      EmuUtil.doClose( zipOut );
      if( !this.suppressArchiveErrDlg ) {
	StringBuilder buf = new StringBuilder( 256 );
	buf.append( "Die Zeitstempel der in der Archivdatei\n" );
	buf.append( file.getPath() );
	buf.append( "\nenthaltenen Dateien"
		+ " konnten nicht ge\u00E4ndert werden." );
	String exMsg = ex.getMessage();
	if( exMsg != null ) {
	  if( !exMsg.isEmpty() ) {
	    buf.append( "\n\n" );
	    buf.append( exMsg );
	  }
	}
	if( this.files.size() > 1 ) {
	  JCheckBox cb = new JCheckBox(
		"Diese Meldung bei weiteren Dateien nicht mehr anzeigen" );
	  buf.append( "\n\n" );
	  JOptionPane.showMessageDialog(
		this,
		new Object[] { buf, cb },
		"Fehler",
		JOptionPane.ERROR_MESSAGE );
	  this.suppressArchiveErrDlg = cb.isSelected();
	} else {
	  JOptionPane.showMessageDialog(
		this,
		buf,
		"Fehler",
		JOptionPane.ERROR_MESSAGE );
	}
      }
    }
  }
}

