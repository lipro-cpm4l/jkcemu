/*
 * (c) 2009-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Auswahl eines Laufwerks
 */

package jkcemu.disk;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.util.EventObject;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import jkcemu.Main;
import jkcemu.base.BaseDlg;
import jkcemu.base.DeviceIO;
import jkcemu.base.GUIFactory;


public class DriveSelectDlg extends BaseDlg
{
  private DeviceIO.MediaType requestedType;
  private boolean            notified;
  private Object             selectedDrive;
  private String             selectedDriveFName;
  private JComboBox<Object>  comboDrive;
  private JCheckBox          cbReadOnly;
  private JButton            btnOK;
  private JButton            btnCancel;


  public DriveSelectDlg( Window owner, DeviceIO.MediaType requestedType )
  {
    super( owner, "Auswahl Laufwerk" );
    this.requestedType      = requestedType;
    this.notified           = false;
    this.selectedDrive      = null;
    this.selectedDriveFName = null;

    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.EAST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 5, 5 ),
					0, 0 );

    String  lastDriveFileName = Main.getLastDriveFileName();
    boolean driveStatus       = false;
    boolean floppyDisk        = false;
    switch( requestedType ) {
      case FLOPPYDISK_READ_ONLY:
      case FLOPPYDISK:
	floppyDisk = true;
	break;
    }
    int presetIdx   = -1;
    this.comboDrive = GUIFactory.createComboBox();
    if( Main.isUnixLikeOS() ) {
      add( GUIFactory.createLabel( "Ger\u00E4tedatei:" ), gbc );
      this.comboDrive.setEditable( true );
    } else {
      add( GUIFactory.createLabel( "Laufwerk:" ), gbc );
      this.comboDrive.setEditable( false );
    }
    for( DeviceIO.Drive drive : DeviceIO.getDrives( requestedType ) ) {
      // bei Disketten zu grosse Medien heraussortieren
      if( !floppyDisk || (drive.getDiskSize() <= (2880L * 1024L)) ) {
	if( lastDriveFileName != null ) {
	  if( drive.getFileName().equals( lastDriveFileName ) ) {
	    presetIdx = this.comboDrive.getItemCount();
	  }
	}
	this.comboDrive.addItem( drive );
	driveStatus = true;
      }
    }
    if( !driveStatus && !this.comboDrive.isEditable() ) {
      this.comboDrive.addItem(
		floppyDisk ?
			"Kein Diskettenlaufwerk gefunden"
			: "Kein Wechselmedium gefunden" );
    }
    if( presetIdx >= 0 ) {
      try {
	this.comboDrive.setSelectedIndex( presetIdx );
      }
      catch( IllegalArgumentException ex ) {}
    }
    gbc.anchor  = GridBagConstraints.WEST;
    gbc.fill    = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;
    gbc.gridx++;
    add( this.comboDrive, gbc );

    gbc.anchor    = GridBagConstraints.CENTER;
    gbc.fill      = GridBagConstraints.NONE;
    gbc.weightx   = 0.0;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.gridx     = 0;
    gbc.gridy++;

    if( (requestedType == DeviceIO.MediaType.ANY_DISK)
	|| (requestedType == DeviceIO.MediaType.FLOPPYDISK) )
    {
      this.cbReadOnly = GUIFactory.createCheckBox( "Nur lesen", true );
      add( this.cbReadOnly, gbc );
      gbc.gridy++;
    } else {
      this.cbReadOnly = null;
    }

    JPanel panelBtn = GUIFactory.createPanel( new GridLayout( 1, 2, 5, 5 ) );
    add( panelBtn, gbc );

    this.btnOK = GUIFactory.createButtonOK();
    this.btnOK.setEnabled( driveStatus || this.comboDrive.isEditable() );
    panelBtn.add( this.btnOK );

    this.btnCancel = GUIFactory.createButtonCancel();
    panelBtn.add( this.btnCancel );

    pack();
    setParentCentered();
  }


  public static Object selectDrive(
				Window             owner,
				DeviceIO.MediaType requestedType )
  {
    DriveSelectDlg dlg = new DriveSelectDlg( owner, requestedType );
    dlg.setVisible( true );
    return dlg.getSelectedDrive();
  }


  public static String selectDriveFileName(
				Window             owner,
				DeviceIO.MediaType requestedType )
  {
    DriveSelectDlg dlg = new DriveSelectDlg( owner, requestedType );
    dlg.setVisible( true );
    return dlg.getSelectedDriveFileName();
  }


  public Object getSelectedDrive()
  {
    return this.selectedDrive;
  }


  public String getSelectedDriveFileName()
  {
    return this.selectedDriveFName;
  }


  public boolean isReadOnlySelected()
  {
    return this.cbReadOnly != null ? this.cbReadOnly.isSelected() : false;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void addNotify()
  {
    super.addNotify();
    if( !this.notified ) {
      this.notified = true;
      this.comboDrive.addKeyListener( this );
      this.btnOK.addActionListener( this );
      this.btnCancel.addActionListener( this );
    }
  }


  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv = false;
    if( e != null ) {
      Object src = e.getSource();
      if( (src == this.btnOK) || (src == this.comboDrive) ) {
	doApply();
        rv = true;
      }
      else if( src == this.btnCancel ) {
	doClose();
        rv = true;
      }
    }
    return rv;
  }


  @Override
  public void removeNotify()
  {
    super.removeNotify();
    if( this.notified ) {
      this.notified = false;
      this.comboDrive.removeKeyListener( this );
      this.btnOK.removeActionListener( this );
      this.btnCancel.removeActionListener( this );
    }
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


	/* --- private Methoden --- */

  private void doApply()
  {
    String  driveFName  = null;
    boolean specialPriv = false;
    Object  drive       = this.comboDrive.getSelectedItem();
    if( drive != null ) {
      if( drive instanceof DeviceIO.Drive ) {
	driveFName  = ((DeviceIO.Drive) drive).getFileName();
	specialPriv = ((DeviceIO.Drive) drive).needsSpecialPrivileges();
      } else {
	String s = drive.toString();
	if( s != null ) {
	  if( !s.isEmpty() ) {
	    drive       = s;
	    driveFName  = s;
	    specialPriv = DeviceIO.needsSpecialPrivileges(
						driveFName,
						this.requestedType );
	  }
	}
      }
    }
    if( driveFName != null ) {
      boolean status = false;
      if( specialPriv ) {
	StringBuilder buf = new StringBuilder( 0x200 );
	buf.append( "Wahrscheinlich sind f\u00FCr den Zugriff auf"
		+ " das Laufwerk spezielle Rechte notwendig.\n"
		+ "Wenn " );
	buf.append( Main.APPNAME );
	buf.append( " unter einem Benutzer gestartet wurde,"
		+ " der diese Rechte\n"
		+ "(i.d.R. " );
	if( Main.isUnixLikeOS() ) {
	  buf.append( "root" );
	} else {
	  buf.append( "Administrator" );
	}
	buf.append( "-Rechte) nicht hat, wird der Vorgang zu einer"
		+ " Fehlermeldung f\u00FChren." );
	if( JOptionPane.showConfirmDialog(
		this,
		buf.toString(),
		"Achtung",
		JOptionPane.OK_CANCEL_OPTION,
		JOptionPane.WARNING_MESSAGE ) == JOptionPane.OK_OPTION )
	{
	  status = true;
	}
      } else {
	status = true;
      }
      if( status ) {
	this.selectedDriveFName = driveFName;
	this.selectedDrive      = drive;
	doClose();
      }
    }
  }
}
