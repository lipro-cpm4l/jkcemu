/*
 * (c) 2008-2010 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog zur Anzeige der Eigenschaften einer Datei- bzw. eines Verzeichnises
 */

package jkcemu.filebrowser;

import java.awt.*;
import java.io.File;
import java.lang.*;
import java.text.*;
import java.util.EventObject;
import javax.swing.*;
import jkcemu.base.*;


public class FilePropDlg extends BasicDlg
{
  private JButton btnOK;


  public FilePropDlg(
		Frame parent,
		File  file )
  {
    super( parent, "Eigenschaften" );
    boolean isDir  = false;
    boolean isFile = false;
    try {
      isDir  = file.isDirectory();
      isFile = file.isFile();
    }
    catch( Exception ex ) {}


    // Layout
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );


    // Fensterinhalt
    String text = file.getName();
    if( text != null ) {
      if( text.length() > 0 ) {
	if( isDir ) {
	  add( new JLabel( "Verzeichnisname:" ), gbc );
	}
	else if( isFile ) {
	  add( new JLabel( "Dateiname:" ), gbc );
	} else {
	  add( new JLabel( "Name:" ), gbc );
	}
	text = file.getName();
	if( text != null ) {
	  gbc.gridx++;
	  add( new JLabel( text ), gbc );
	}
      }
    }
    gbc.insets.top = 0;

    if( isFile ) {
      try {
	long n = file.length();
	if( n >= 0 ) {
	  gbc.gridx = 0;
	  gbc.gridy++;
	  add( new JLabel( "Gr\u00F6\u00DFe:" ), gbc );
	  gbc.gridx++;
	  add(
	    new JLabel( NumberFormat.getNumberInstance().format( n )
								+ " Bytes" ),
	    gbc );
	}
      }
      catch( Exception ex ) {}
    }

    try {
      long t = file.lastModified();
      if( t > 0L ) {
	gbc.gridx = 0;
	gbc.gridy++;
	add( new JLabel( "Zuletzt ge\u00E4ndert:" ), gbc );
	gbc.gridx++;
	add(
	    new JLabel( DateFormat.getDateTimeInstance(
				DateFormat.MEDIUM,
				DateFormat.MEDIUM ).format(
					new java.util.Date( t ) ) ),
	    gbc );
      }
    }
    catch( Exception ex ) {}

    gbc.gridx = 0;
    gbc.gridy++;
    add( new JLabel( "Lesezugriff:" ), gbc );
    try {
      gbc.gridx++;
      add( new JLabel( file.canRead() ? "ja" : "nein" ), gbc );
    }
    catch( Exception ex ) {}

    gbc.gridx = 0;
    gbc.gridy++;
    add( new JLabel( "Schreibzugriff:" ), gbc );
    try {
      gbc.gridx++;
      add( new JLabel( file.canWrite() ? "ja" : "nein" ), gbc );
    }
    catch( Exception ex ) {}

    gbc.insets.bottom = 5;
    gbc.gridx         = 0;
    gbc.gridy++;
    add( new JLabel( "Versteckt:" ), gbc );
    try {
      gbc.gridx++;
      add( new JLabel( file.isHidden() ? "ja" : "nein" ), gbc );
    }
    catch( Exception ex ) {}


    // Knopf
    this.btnOK = new JButton( "OK" );
    this.btnOK.addActionListener( this );

    gbc.anchor     = GridBagConstraints.CENTER;
    gbc.insets.top = 5;
    gbc.gridwidth  = GridBagConstraints.REMAINDER;
    gbc.gridx      = 0;
    gbc.gridy++;
    add( this.btnOK, gbc );


    // Fenstergroesse und -position
    pack();
    setParentCentered();
    setResizable( false );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv = false;
    if( e != null ) {
      Object src = e.getSource();
      if( src != null ) {
        if( src == this.btnOK ) {
	  rv = true;
	  doClose();
	}
      }
    }
    return rv;
  }
}

