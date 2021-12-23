/*
 * (c) 2015-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog zum Aendern der Dateiattribute
 */

package jkcemu.disk;

import java.awt.Window;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.EventObject;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import jkcemu.base.BaseDlg;
import jkcemu.base.GUIFactory;


public class ChangeFileAttrsDlg extends BaseDlg
{
  private boolean      notified;
  private Boolean      readOnly;
  private Boolean      sysFile;
  private Boolean      archive;
  private JRadioButton rbReadOnlyUnchanged;
  private JRadioButton rbReadOnlyYes;
  private JRadioButton rbReadOnlyNo;
  private JRadioButton rbSysFileUnchanged;
  private JRadioButton rbSysFileYes;
  private JRadioButton rbSysFileNo;
  private JRadioButton rbArchiveUnchanged;
  private JRadioButton rbArchiveYes;
  private JRadioButton rbArchiveNo;
  private JButton      btnOK;
  private JButton      btnCancel;


  public ChangeFileAttrsDlg( Window owner )
  {
    super( owner, "Dateiattribute \u00E4ndern" );
    this.notified = false;
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
    add( GUIFactory.createLabel( "Schreibgesch\u00FCtzt:" ), gbc );

    ButtonGroup readOnlyGrp = new ButtonGroup();

    this.rbReadOnlyUnchanged = createRadioButtonUnchanged();
    readOnlyGrp.add( this.rbReadOnlyUnchanged );
    gbc.anchor = GridBagConstraints.WEST;
    gbc.gridx++;
    add( this.rbReadOnlyUnchanged, gbc );

    this.rbReadOnlyYes = createRadioButtonYes();
    readOnlyGrp.add( this.rbReadOnlyYes );
    gbc.gridx++;
    add( this.rbReadOnlyYes, gbc );

    this.rbReadOnlyNo = createRadioButtonNo();
    readOnlyGrp.add( this.rbReadOnlyNo );
    gbc.gridx++;
    add( this.rbReadOnlyNo, gbc );


    // Systemdatei
    gbc.anchor     = GridBagConstraints.EAST;
    gbc.insets.top = 0;
    gbc.gridx      = 0;
    gbc.gridy++;
    add( GUIFactory.createLabel( "Systemdatei:" ), gbc );

    ButtonGroup sysFileGrp = new ButtonGroup();

    this.rbSysFileUnchanged = createRadioButtonUnchanged();
    sysFileGrp.add( this.rbSysFileUnchanged );
    gbc.anchor = GridBagConstraints.WEST;
    gbc.gridx++;
    add( this.rbSysFileUnchanged, gbc );

    this.rbSysFileYes = createRadioButtonYes();
    sysFileGrp.add( this.rbSysFileYes );
    gbc.gridx++;
    add( this.rbSysFileYes, gbc );

    this.rbSysFileNo = createRadioButtonNo();
    sysFileGrp.add( this.rbSysFileNo );
    gbc.gridx++;
    add( this.rbSysFileNo, gbc );


    // Archiviert
    gbc.anchor = GridBagConstraints.EAST;
    gbc.gridx  = 0;
    gbc.gridy++;
    add( GUIFactory.createLabel( "Archiv:" ), gbc );

    ButtonGroup archiveGrp = new ButtonGroup();

    this.rbArchiveUnchanged = createRadioButtonUnchanged();
    archiveGrp.add( this.rbArchiveUnchanged );
    gbc.anchor = GridBagConstraints.WEST;
    gbc.gridx++;
    add( this.rbArchiveUnchanged, gbc );

    this.rbArchiveYes = createRadioButtonYes();
    archiveGrp.add( this.rbArchiveYes );
    gbc.gridx++;
    add( this.rbArchiveYes, gbc );

    this.rbArchiveNo = createRadioButtonNo();
    archiveGrp.add( this.rbArchiveNo );
    gbc.gridx++;
    add( this.rbArchiveNo, gbc );


    // Knopfe
    JPanel panelBtn   = GUIFactory.createPanel(
					new GridLayout( 1, 2, 5, 5 ) );
    gbc.anchor        = GridBagConstraints.CENTER;
    gbc.insets.top    = 20;
    gbc.insets.bottom = 10;
    gbc.gridwidth     = GridBagConstraints.REMAINDER;
    gbc.gridx         = 0;
    gbc.gridy++;
    add( panelBtn, gbc );

    this.btnOK = GUIFactory.createButtonOK();
    panelBtn.add( this.btnOK );

    this.btnCancel = GUIFactory.createButtonCancel();
    panelBtn.add( this.btnCancel );


    // Fenstergroesse und -position
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
  public void addNotify()
  {
    super.addNotify();
    if( !this.notified ) {
      this.notified = true;
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
      if( src == this.btnOK ) {
	if( this.rbReadOnlyYes.isSelected() ) {
	  this.readOnly = Boolean.TRUE;
	} else if( this.rbReadOnlyNo.isSelected() ) {
	  this.readOnly = Boolean.FALSE;
	}
	if( this.rbSysFileYes.isSelected() ) {
	  this.sysFile = Boolean.TRUE;
	} else if( this.rbSysFileNo.isSelected() ) {
	  this.sysFile = Boolean.FALSE;
	}
	if( this.rbArchiveYes.isSelected() ) {
	  this.archive = Boolean.TRUE;
	} else if( this.rbArchiveNo.isSelected() ) {
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


  @Override
  public void removeNotify()
  {
    super.removeNotify();
    if( this.notified ) {
      this.notified = false;
      this.btnOK.removeActionListener( this );
      this.btnCancel.removeActionListener( this );
    }
  }


	/* --- private Methoden --- */

  private static JRadioButton createRadioButtonNo()
  {
    return GUIFactory.createRadioButton( "Nein" );
  }


  private static JRadioButton createRadioButtonUnchanged()
  {
    return GUIFactory.createRadioButton( "Nicht \u00E4ndern", true );
  }


  private static JRadioButton createRadioButtonYes()
  {
    return GUIFactory.createRadioButton( "Ja" );
  }
}
