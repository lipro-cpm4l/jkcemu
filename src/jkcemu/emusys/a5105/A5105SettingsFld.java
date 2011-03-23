/*
 * (c) 2010-2011 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Komponente fuer die A5105-Einstellungen
 */

package jkcemu.emusys.a5105;

import java.awt.*;
import java.lang.*;
import java.util.*;
import javax.swing.*;
import jkcemu.base.*;


public class A5105SettingsFld extends AbstractSettingsFld
{
  private JTabbedPane            tabbedPane;
  private JPanel                 tabEtc;
  private RAMFloppiesSettingsFld tabRF;
  private JCheckBox              btnFloppyDisk;
  private JCheckBox              btnPasteFast;


  public A5105SettingsFld( SettingsFrm settingsFrm, String propPrefix )
  {
    super( settingsFrm, propPrefix );

    setLayout( new BorderLayout() );
    this.tabbedPane = new JTabbedPane( JTabbedPane.TOP );
    add( this.tabbedPane, BorderLayout.CENTER );


    // Bereich RAM-Floppies
    this.tabRF = new RAMFloppiesSettingsFld(
			settingsFrm,
			propPrefix,
			"RAM-Floppy an IO-Adressen 20h/21h",
			RAMFloppy.RFType.ADW,
			"RAM-Floppy an IO-Adressen 24h/25h",
			RAMFloppy.RFType.ADW );
    this.tabbedPane.addTab( "RAM-Floppies", this.tabRF );


    // Bereich Sonstiges
    this.tabEtc = new JPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "Sonstiges", this.tabEtc );

    GridBagConstraints gbcEtc = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );

    this.btnFloppyDisk = new JCheckBox(
		"Floppy-Disk-Station emulieren",
		true );
    this.btnFloppyDisk.addActionListener( this );
    this.tabEtc.add( this.btnFloppyDisk, gbcEtc );

    this.btnPasteFast = new JCheckBox(
		"Einf\u00FCgen von Text durch Abfangen des Systemaufrufs",
		true );
    this.btnPasteFast.addActionListener( this );
    gbcEtc.insets.top    = 0;
    gbcEtc.insets.bottom = 5;
    gbcEtc.gridy++;
    this.tabEtc.add( this.btnPasteFast, gbcEtc );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void applyInput(
		Properties props,
		boolean    selected ) throws UserInputException
  {
    Component tab = null;
    try {

      // RAM-Floppies
      tab = this.tabRF;
      this.tabRF.applyInput( props, selected );

      // Sonstiges
      props.setProperty(
		this.propPrefix + "floppydisk.enabled",
		Boolean.toString( this.btnFloppyDisk.isSelected() ) );
      props.setProperty(
		this.propPrefix + "paste.fast",
		Boolean.toString( this.btnPasteFast.isSelected() ) );
    }
    catch( UserInputException ex ) {
      if( tab != null ) {
	this.tabbedPane.setSelectedComponent( tab );
      }
      throw ex;
    }
  }


  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv  = false;
    Object  src = e.getSource();
    if( src != null ) {
      if( src instanceof AbstractButton ) {
	rv = true;
	fireDataChanged();
      }
    }
    return rv;
  }


  @Override
  public void updFields( Properties props )
  {
    this.tabRF.updFields( props );
    this.btnFloppyDisk.setSelected(
	EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + "floppydisk.enabled",
			true ) );
    this.btnPasteFast.setSelected(
	EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + "paste.fast",
			true ) );
  }
}
