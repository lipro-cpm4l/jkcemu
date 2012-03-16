/*
 * (c) 2012 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Komponente fuer die Einstellungen fuer LC80
 */

package jkcemu.emusys.lc80;

import java.awt.*;
import java.lang.*;
import java.util.*;
import javax.swing.*;
import jkcemu.base.*;


public class LC80SettingsFld extends AbstractSettingsFld
{
  private JTabbedPane        tabbedPane;
  private JPanel             tabModel;
  private JPanel             tabRom;
  private JRadioButton       btnLC80_U505;
  private JRadioButton       btnLC80_2716;
  private JRadioButton       btnLC80_2;
  private JRadioButton       btnLC80e;
  private ROMFileSettingsFld fldAltOS;
  private ROMFileSettingsFld fldAltC000;


  public LC80SettingsFld( SettingsFrm settingsFrm, String propPrefix )
  {
    super( settingsFrm, propPrefix );

    setLayout( new BorderLayout() );
    this.tabbedPane = new JTabbedPane( JTabbedPane.TOP );
    add( this.tabbedPane, BorderLayout.CENTER );


    // Bereich Modell
    this.tabModel = new JPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "Modell", this.tabModel );

    GridBagConstraints gbcModel = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.NORTHWEST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 0, 5 ),
						0, 0 );

    ButtonGroup grpModel = new ButtonGroup();

    this.btnLC80_U505 = new JRadioButton(
		"LC-80, 2 KByte ROM (2xU505), 1 KByte RAM",
		false );
    this.btnLC80_U505.addActionListener( this );
    grpModel.add( this.btnLC80_U505 );
    this.tabModel.add( this.btnLC80_U505, gbcModel );

    this.btnLC80_2716 = new JRadioButton(
		"LC-80, 2 KByte ROM (2716), 4 KByte RAM",
		true );
    this.btnLC80_2716.addActionListener( this );
    grpModel.add( this.btnLC80_2716 );
    gbcModel.insets.top = 0;
    gbcModel.gridy++;
    this.tabModel.add( this.btnLC80_2716, gbcModel );

    this.btnLC80_2 = new JRadioButton(
		"LC-80.2, 4 KByte ROM mit Buschendorf-Monitor, 4 KByte RAM",
		false );
    this.btnLC80_2.addActionListener( this );
    grpModel.add( this.btnLC80_2 );
    gbcModel.gridy++;
    this.tabModel.add( this.btnLC80_2, gbcModel );

    this.btnLC80e = new JRadioButton(
		"LC-80e, 12 KByte ROM mit Schachprogramm SC-80, 4 KByte RAM",
		false );
    this.btnLC80e.addActionListener( this );
    grpModel.add( this.btnLC80e );
    gbcModel.insets.bottom = 5;
    gbcModel.gridy++;
    this.tabModel.add( this.btnLC80e, gbcModel );


    // Bereich ROM
    this.tabRom = new JPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "ROM", this.tabRom );

    GridBagConstraints gbcRom = new GridBagConstraints(
						0, 0,
						1, 1,
						1.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.HORIZONTAL,
						new Insets( 5, 5, 5, 5 ),
						0, 0 );

    this.fldAltOS = new ROMFileSettingsFld(
		settingsFrm,
		propPrefix + "os.",
		"Alternatives Monitorprogramm (0000h-1FFFh):" );
    gbcRom.gridy++;
    this.tabRom.add( this.fldAltOS, gbcRom );

    this.fldAltC000 = new ROMFileSettingsFld(
		settingsFrm,
		propPrefix + "rom_c000.",
		"Alternatives Schachprogramm (C000h-FFFFh, nur LC80e):" );
    gbcRom.gridy++;
    this.tabRom.add( this.fldAltC000, gbcRom );

    updFldAltC000Enabled();
  }


  public String getModelText()
  {
    String rv = "LC80_2716";
    if( this.btnLC80_U505.isSelected() ) {
      rv = "LC80_U505";
    } else if( this.btnLC80_2.isSelected() ) {
      rv = "LC80.2";
    } else if( this.btnLC80e.isSelected() ) {
      rv = "LC80e";
    }
    return rv;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void applyInput(
			Properties props,
			boolean    selected ) throws UserInputException
  {
    Component tab = this.tabRom;
    try {
      this.fldAltOS.applyInput( props, selected );
      this.fldAltC000.applyInput( props, selected );
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
      if( (src == this.btnLC80_U505)
	  || (src == this.btnLC80_2716)
	  || (src == this.btnLC80_2)
	  || (src == this.btnLC80e) )
      {
	rv = true;
	updFldAltC000Enabled();
	fireDataChanged();
      }
    }
    return rv;
  }


  @Override
  public void updFields( Properties props )
  {
    String sysName = EmuUtil.getProperty( props, "jkcemu.system" );
    if( sysName.startsWith( "LC80_U505" ) ) {
      this.btnLC80_U505.setSelected( true );
    } else if( sysName.startsWith( "LC80.2" ) ) {
      this.btnLC80_2.setSelected( true );
    } else if( sysName.startsWith( "LC80e" ) ) {
      this.btnLC80e.setSelected( true );
    } else {
      this.btnLC80_2716.setSelected( true );
    }
    updFldAltC000Enabled();
    this.fldAltOS.updFields( props );
    this.fldAltC000.updFields( props );
  }


	/* --- private Methoden --- */

  private void updFldAltC000Enabled()
  {
    this.fldAltC000.setEnabled( this.btnLC80e.isSelected() );
  }
}
