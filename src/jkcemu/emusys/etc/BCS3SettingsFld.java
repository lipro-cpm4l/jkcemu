/*
 * (c) 2015-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Komponente fuer die BCS3-Einstellungen
 */

package jkcemu.emusys.etc;

import java.awt.*;
import java.lang.*;
import java.util.*;
import javax.swing.*;
import jkcemu.base.*;
import jkcemu.emusys.BCS3;


public class BCS3SettingsFld extends AbstractSettingsFld
{
  private static final int DEFAULT_AUTO_ACTION_WAIT_MILLIS = 500;

  private JTabbedPane          tabbedPane;
  private JPanel               tabModel;
  private JPanel               tabRam;
  private JPanel               tabRom;
  private JPanel               tabEtc;
  private AutoLoadSettingsFld  tabAutoLoad;
  private AutoInputSettingsFld tabAutoInput;
  private JRadioButton         btnSE24_27;
  private JRadioButton         btnSE31_29;
  private JRadioButton         btnSE31_40;
  private JRadioButton         btnSP33_29;
  private JRadioButton         btnRam1k;
  private JRadioButton         btnRam17k;
  private JCheckBox            btnRemoveHSyncFromAudio;
  private ROMFileSettingsFld   fldAltOS;
  private ROMFileSettingsFld   fldAltFont;


  public BCS3SettingsFld( SettingsFrm settingsFrm, String propPrefix )
  {
    super( settingsFrm, propPrefix );

    setLayout( new BorderLayout() );

    this.tabbedPane = new JTabbedPane( JTabbedPane.TOP );
    add( this.tabbedPane, BorderLayout.CENTER );


    // Tab Modell
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

    ButtonGroup grpOS = new ButtonGroup();

    this.btnSE24_27 = new JRadioButton(
		"2 KByte BASIC-SE 2.4, 2,5 MHz, 27 Zeichen pro Zeile",
		true );
    this.btnSE24_27.addActionListener( this );
    grpOS.add( this.btnSE24_27 );
    this.tabModel.add( this.btnSE24_27, gbcModel );

    this.btnSE31_29 = new JRadioButton(
		"4 KByte BASIC-SE 3.1, 2,5 MHz, 29 Zeichen pro Zeile",
		false );
    this.btnSE31_29.addActionListener( this );
    grpOS.add( this.btnSE31_29 );
    gbcModel.insets.top = 0;
    gbcModel.gridy++;
    this.tabModel.add( this.btnSE31_29, gbcModel );

    this.btnSE31_40 = new JRadioButton(
		"4 KByte BASIC-SE 3.1, 3,5 MHz, 40 Zeichen pro Zeile",
		false );
    this.btnSE31_40.addActionListener( this );
    grpOS.add( this.btnSE31_40 );
    gbcModel.insets.top = 0;
    gbcModel.gridy++;
    this.tabModel.add( this.btnSE31_40, gbcModel );

    this.btnSP33_29 = new JRadioButton(
		"4 KByte S/P-BASIC V3.3, 2,5 MHz, 29 Zeichen pro Zeile",
		false );
    this.btnSP33_29.addActionListener( this );
    grpOS.add( this.btnSP33_29 );
    gbcModel.gridy++;
    this.tabModel.add( this.btnSP33_29, gbcModel );


    // Tab RAM
    this.tabRam = new JPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "RAM", this.tabRam );

    GridBagConstraints gbcRam = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.NORTHWEST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 0, 5 ),
						0, 0 );

    ButtonGroup grpRam = new ButtonGroup();

    this.btnRam1k = new JRadioButton( "1 KByte RAM", true );
    this.btnRam1k.addActionListener( this );
    grpRam.add( this.btnRam1k );
    gbcRam.insets.top = 10;
    gbcRam.gridy++;
    this.tabRam.add( this.btnRam1k, gbcRam );

    this.btnRam17k = new JRadioButton(
				"17 KByte RAM (16 KByte RAM-Erweiterung)",
				false );
    this.btnRam17k.addActionListener( this );
    grpRam.add( this.btnRam17k );
    gbcRam.insets.top    = 0;
    gbcRam.insets.bottom = 5;
    gbcRam.gridy++;
    this.tabRam.add( this.btnRam17k, gbcRam );


    // Tab ROM
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
		"Alternativer ROM-Inhalt (0000h-0FFFh):" );
    gbcRom.gridy++;
    this.tabRom.add( this.fldAltOS, gbcRom );

    this.fldAltFont = new ROMFileSettingsFld(
		settingsFrm,
		propPrefix + "font.",
		"Alternativer Zeichensatz:" );
    gbcRom.gridy++;
    this.tabRom.add( this.fldAltFont, gbcRom );


    // Tab Sonstiges
    this.tabEtc = new JPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "Sonstiges", this.tabEtc );

    GridBagConstraints gbcEtc = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.CENTER,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 5, 5 ),
						0, 0 );

    this.btnRemoveHSyncFromAudio = new JCheckBox(
			"HSync-Signale aus Audioausgabe entfernen" );
    this.btnRemoveHSyncFromAudio.addActionListener( this );
    this.tabEtc.add( this.btnRemoveHSyncFromAudio, gbcEtc );


    // Tab AutoLoad
    this.tabAutoLoad = new AutoLoadSettingsFld(
					settingsFrm,
					propPrefix,
					DEFAULT_AUTO_ACTION_WAIT_MILLIS,
					true );
    this.tabbedPane.addTab( "AutoLoad", this.tabAutoLoad );


    // Tab AutoInput
    this.tabAutoInput = new AutoInputSettingsFld(
					settingsFrm,
					propPrefix,
					BCS3.getDefaultSwapKeyCharCase(),
					DEFAULT_AUTO_ACTION_WAIT_MILLIS );
    this.tabbedPane.addTab( "AutoInput", this.tabAutoInput );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void applyInput(
			Properties props,
			boolean    selected ) throws UserInputException
  {
    Component tab = this.tabModel;
    try {

      // Tab Modell
      String osVersion    = "2.4";
      String charsPerLine = "27";
      if( this.btnSE31_29.isSelected() ) {
	osVersion    = "3.1";
	charsPerLine = "29";
      } else if( this.btnSE31_40.isSelected() ) {
	osVersion    = "3.1";
	charsPerLine = "40";
      } else if( this.btnSP33_29.isSelected() ) {
	osVersion    = "3.3";
	charsPerLine = "29";
      }
      EmuUtil.setProperty(
		props,
		this.propPrefix + "os.version",
		osVersion );
      EmuUtil.setProperty(
		props,
		this.propPrefix + "chars_per_line",
		charsPerLine );

      // Tab RAM
      tab = this.tabRam;
      EmuUtil.setProperty(
		props,
		this.propPrefix + "ram.kbyte",
		this.btnRam17k.isSelected() ? "17" : "1" );

      // Tab ROM
      tab = this.tabRom;
      this.fldAltOS.applyInput( props, selected );
      this.fldAltFont.applyInput( props, selected );

      // Tab Sonstiges
      tab = this.tabEtc;
      EmuUtil.setProperty(
		props,
		this.propPrefix + "remove_hsync_from_audio",
		this.btnRemoveHSyncFromAudio.isSelected() );

      // Tab AutoLoad
      tab = this.tabAutoLoad;
      this.tabAutoLoad.applyInput( props, selected );

      // Tab AutoInput
      tab = this.tabAutoInput;
      this.tabAutoInput.applyInput( props, selected );
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
      rv = this.tabAutoLoad.doAction( e );
      if( !rv ) {
	rv = this.tabAutoInput.doAction( e );
      }
      if( !rv && (src instanceof AbstractButton) ) {
	fireDataChanged();
	rv = true;
      }
    }
    return rv;
  }


  @Override
  public void lookAndFeelChanged()
  {
    this.fldAltOS.lookAndFeelChanged();
    this.fldAltFont.lookAndFeelChanged();
    this.tabAutoLoad.lookAndFeelChanged();
    this.tabAutoInput.lookAndFeelChanged();
  }


  @Override
  public void updFields( Properties props )
  {
    String osVersion = EmuUtil.getProperty(
				props,
				this.propPrefix + "os.version" );
    if( osVersion.equals( "3.1" ) ) {
      if( EmuUtil.getProperty(
		props,
		this.propPrefix + "chars_per_line" ).equals( "40" ) )
      {
	this.btnSE31_40.setSelected( true );
      } else {
	this.btnSE31_29.setSelected( true );
      }
    } else if( osVersion.equals( "3.3" ) ) {
      this.btnSP33_29.setSelected( true );
    } else {
      this.btnSE24_27.setSelected( true );
    }
    if( EmuUtil.getProperty(
		props,
		this.propPrefix + "ram.kbyte" ).equals( "17" ) )
    {
      this.btnRam17k.setSelected( true );
    } else {
      this.btnRam1k.setSelected( true );
    }
    this.fldAltOS.updFields( props );
    this.fldAltFont.updFields( props );
    this.btnRemoveHSyncFromAudio.setSelected(
		EmuUtil.getBooleanProperty(
				props,
				this.propPrefix + "remove_hsync_from_audio",
				true ) );
    this.tabAutoLoad.updFields( props );
    this.tabAutoInput.updFields( props );
  }
}
