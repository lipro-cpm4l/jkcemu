/*
 * (c) 2015 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog zum Aendern der Dateiattribute
 */

package jkcemu.disk;

import java.awt.*;
import java.lang.*;
import java.util.EventObject;
import javax.swing.*;
import jkcemu.base.BasicDlg;


public class ChangeFileAttrsDlg extends BasicDlg
{
  private Boolean      readOnly;
  private Boolean      sysFile;
  private Boolean      archive;
  private JRadioButton btnReadOnlyUnchanged;
  private JRadioButton btnReadOnlyYes;
  private JRadioButton btnReadOnlyNo;
  private JRadioButton btnSysFileUnchanged;
  private JRadioButton btnSysFileYes;
  private JRadioButton btnSysFileNo;
  private JRadioButton btnArchiveUnchanged;
  private JRadioButton btnArchiveYes;
  private JRadioButton btnArchiveNo;
  private JButton      btnOK;
  private JButton      btnCancel;


  public ChangeFileAttrsDlg( Window owner )
  {
    super( owner, "Dateiattribute \u00E4ndern" );
    this.readOnly = null;
    this.sysFile  = null;
    this.archive  = null;


    // Fensterinhalt
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.EAST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );

    // Schreibgeschuetzt
    add( new JLabel( "Schreibgesch\u00FCtzt:" ), gbc );

    ButtonGroup readOnlyGrp = new ButtonGroup();

    this.btnReadOnlyUnchanged = new JRadioButton( "Nicht \u00E4ndern", true );
    readOnlyGrp.add( this.btnReadOnlyUnchanged );
    gbc.anchor = GridBagConstraints.WEST;
    gbc.gridx++;
    add( this.btnReadOnlyUnchanged, gbc );

    this.btnReadOnlyYes = new JRadioButton( "Ja", false );
    readOnlyGrp.add( this.btnReadOnlyYes );
    gbc.gridx++;
    add( this.btnReadOnlyYes, gbc );

    this.btnReadOnlyNo = new JRadioButton( "Nein", false );
    readOnlyGrp.add( this.btnReadOnlyNo );
    gbc.gridx++;
    add( this.btnReadOnlyNo, gbc );


    // Systemdatei
    gbc.anchor     = GridBagConstraints.EAST;
    gbc.insets.top = 0;
    gbc.gridx      = 0;
    gbc.gridy++;
    add( new JLabel( "Systemdatei:" ), gbc );

    ButtonGroup sysFileGrp = new ButtonGroup();

    this.btnSysFileUnchanged = new JRadioButton( "Nicht \u00E4ndern", true );
    sysFileGrp.add( this.btnSysFileUnchanged );
    gbc.anchor = GridBagConstraints.WEST;
    gbc.gridx++;
    add( this.btnSysFileUnchanged, gbc );

    this.btnSysFileYes = new JRadioButton( "Ja", false );
    sysFileGrp.add( this.btnSysFileYes );
    gbc.gridx++;
    add( this.btnSysFileYes, gbc );

    this.btnSysFileNo = new JRadioButton( "Nein", false );
    sysFileGrp.add( this.btnSysFileNo );
    gbc.gridx++;
    add( this.btnSysFileNo, gbc );


    // Archiviert
    gbc.anchor = GridBagConstraints.EAST;
    gbc.gridx  = 0;
    gbc.gridy++;
    add( new JLabel( "Archiv:" ), gbc );

    ButtonGroup archiveGrp = new ButtonGroup();

    this.btnArchiveUnchanged = new JRadioButton( "Nicht \u00E4ndern", true );
    archiveGrp.add( this.btnArchiveUnchanged );
    gbc.anchor = GridBagConstraints.WEST;
    gbc.gridx++;
    add( this.btnArchiveUnchanged, gbc );

    this.btnArchiveYes = new JRadioButton( "Ja", false );
    archiveGrp.add( this.btnArchiveYes );
    gbc.gridx++;
    add( this.btnArchiveYes, gbc );

    this.btnArchiveNo = new JRadioButton( "Nein", false );
    archiveGrp.add( this.btnArchiveNo );
    gbc.gridx++;
    add( this.btnArchiveNo, gbc );


    // Knopfe
    JPanel panelBtn   = new JPanel( new GridLayout( 1, 2, 5, 5 ) );
    gbc.anchor        = GridBagConstraints.CENTER;
    gbc.insets.top    = 20;
    gbc.insets.bottom = 10;
    gbc.gridwidth     = GridBagConstraints.REMAINDER;
    gbc.gridx         = 0;
    gbc.gridy++;
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


  public Boolean getArchiveValue()
  {
    return this.archive;
  }


  public Boolean getReadOnlyValue()
  {
    return this.readOnly;
  }


  public Boolean getSystemFileValue()
  {
    return this.sysFile;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv = false;
    if( e != null ) {
      Object src = e.getSource();
      if( src == this.btnOK ) {
	if( this.btnReadOnlyYes.isSelected() ) {
	  this.readOnly = Boolean.TRUE;
	} else if( this.btnReadOnlyNo.isSelected() ) {
	  this.readOnly = Boolean.FALSE;
	}
	if( this.btnSysFileYes.isSelected() ) {
	  this.sysFile = Boolean.TRUE;
	} else if( this.btnSysFileNo.isSelected() ) {
	  this.sysFile = Boolean.FALSE;
	}
	if( this.btnArchiveYes.isSelected() ) {
	  this.archive = Boolean.TRUE;
	} else if( this.btnArchiveNo.isSelected() ) {
	  this.archive = Boolean.FALSE;
	}
	doClose();
        rv = true;
      }
      else if( src == this.btnCancel ) {
	doClose();
        rv = true;
      }
    }
    return rv;
  }
}
