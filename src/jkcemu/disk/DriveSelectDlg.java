/*
 * (c) 2009-2010 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Auswahl eines Laufwerks
 */

package jkcemu.disk;

import java.awt.*;
import java.awt.event.WindowEvent;
import java.io.File;
import java.lang.*;
import java.util.EventObject;
import javax.swing.*;
import jkcemu.Main;
import jkcemu.base.BasicDlg;


public class DriveSelectDlg extends BasicDlg
{
  private String    driveFileName;
  private JComboBox comboDrive;
  private JCheckBox btnReadOnly;
  private JButton   btnOK;
  private JButton   btnCancel;


  public static String selectDriveFileName( Window owner )
  {
    DriveSelectDlg dlg = new DriveSelectDlg( owner, false );
    dlg.setVisible( true );
    return dlg.getSelectedDriveFileName();
  }


  public DriveSelectDlg( Window owner, boolean askReadOnly )
  {
    super( owner, "Auswahl Laufwerk" );
    this.driveFileName = null;

    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.EAST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 5, 5 ),
					0, 0 );

    this.comboDrive = new JComboBox();
    if( File.separatorChar == '/' ) {
      add( new JLabel( "Ger\u00E4tedatei:" ), gbc );
      this.comboDrive.setEditable( true );
      this.comboDrive.addItem( "/dev/fd0" );
      this.comboDrive.addItem( "/dev/fd1" );
      this.comboDrive.addItem( "/dev/sdb" );
      this.comboDrive.addItem( "/dev/sdc" );
    } else {
      add( new JLabel( "Laufwerk:" ), gbc );
      this.comboDrive.setEditable( false );
      for( char ch = 'A'; ch <= 'Z'; ch++ ) {
	this.comboDrive.addItem( String.format( "%c:", ch ) );
      }
    }
    String lastDriveFileName = Main.getLastDriveFileName();
    if( lastDriveFileName != null ) {
      if( (lastDriveFileName.length() > 4)
	  && lastDriveFileName.startsWith( "\\\\.\\" ) )
      {
	lastDriveFileName = lastDriveFileName.substring( 4 );
      }
      this.comboDrive.setSelectedItem( lastDriveFileName );
    }
    this.comboDrive.addKeyListener( this );
    gbc.anchor = GridBagConstraints.WEST;
    gbc.gridx++;
    add( this.comboDrive, gbc );

    gbc.anchor    = GridBagConstraints.CENTER;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.gridx     = 0;
    gbc.gridy++;

    if( askReadOnly ) {
      this.btnReadOnly = new JCheckBox( "Nur lesen", true );
      add( this.btnReadOnly, gbc );
      gbc.gridy++;
    } else {
      this.btnReadOnly = null;
    }

    JPanel panelBtn = new JPanel( new GridLayout( 1, 2, 5, 5 ) );
    add( panelBtn, gbc );

    this.btnOK = new JButton( "OK" );
    this.btnOK.addActionListener( this );
    this.btnOK.addKeyListener( this );
    panelBtn.add( this.btnOK );

    this.btnCancel = new JButton( "Abbrechen" );
    this.btnCancel.addActionListener( this );
    this.btnCancel.addKeyListener( this );
    panelBtn.add( this.btnCancel );

    pack();
    setParentCentered();
  }


  public String getSelectedDriveFileName()
  {
    return this.driveFileName;
  }


  public boolean isReadOnlySelected()
  {
    return this.btnReadOnly != null ? this.btnReadOnly.isSelected() : false;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv = false;
    if( e != null ) {
      Object src = e.getSource();
      if( (src == this.btnOK) || (src == this.comboDrive) ) {
	Object obj = this.comboDrive.getSelectedItem();
	if( obj != null ) {
	  String s = obj.toString();
	  if( s != null ) {
	    int len = s.length();
	    if( len > 0 ) {
	      if( File.separatorChar == '/' ) {
		this.driveFileName = s;
	      } else {
		this.driveFileName = "\\\\.\\" + s;
	      }
	      doClose();
	    }
	  }
	}
        rv = true;
      }
      else if( src == this.btnCancel ) {
	doClose();
        rv = true;
      }
    }
    return rv;
  }


  /*
   * Focus auf OK-Knopf setzen,
   * damit man einfach Enter druecken kann.
   */
  @Override
  public void windowOpened( WindowEvent e )
  {
    if( (e.getWindow() == this) && (this.btnOK != null) )
      this.btnOK.requestFocus();
  }
}

