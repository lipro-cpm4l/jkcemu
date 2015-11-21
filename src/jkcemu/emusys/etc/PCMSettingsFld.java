/*
 * (c) 2011-2021 Jens Mueller
 * (c) 2014-2022 Stephan Linz
 *
 * Kleincomputer-Emulator
 *
 * Komponente fuer die PC/M-Einstellungen
 */

package jkcemu.emusys.etc;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.EventObject;
import java.util.Properties;
import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import jkcemu.base.AutoInputCharSet;
import jkcemu.base.EmuUtil;
import jkcemu.base.GUIFactory;
import jkcemu.base.UserInputException;
import jkcemu.emusys.PCM;
import jkcemu.file.RAMFileSettingsFld;
import jkcemu.file.ROMFileSettingsFld;
import jkcemu.settings.AbstractSettingsFld;
import jkcemu.settings.AutoInputSettingsFld;
import jkcemu.settings.AutoLoadSettingsFld;
import jkcemu.settings.SettingsFrm;


public class PCMSettingsFld extends AbstractSettingsFld
{
  private JTabbedPane          tabbedPane;
  private JPanel               tabModel;
  private AutoLoadSettingsFld  tabAutoLoad;
  private AutoInputSettingsFld tabAutoInput;
  private JCheckBox            cbAutoLoadBDOS;
  private JRadioButton         rbRF64x16;
  private JRadioButton         rbFDC64x16;
  private JRadioButton         rbFDC80x24;
  private JPanel               tabExt;
  private JCheckBox            cbK1520Sound;
  private JCheckBox            cbVDIP;
  private RAMFileSettingsFld   fldAltBDOS;
  private ROMFileSettingsFld   fldAltROM;
  private ROMFileSettingsFld   fldAltFont;


  public PCMSettingsFld( SettingsFrm settingsFrm, String propPrefix )
  {
    super( settingsFrm, propPrefix );

    setLayout( new BorderLayout() );

    this.tabbedPane = GUIFactory.createTabbedPane();
    add( this.tabbedPane, BorderLayout.CENTER );


    // Tab Modell
    this.tabModel = GUIFactory.createPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "Modell", this.tabModel );

    GridBagConstraints gbcModel = new GridBagConstraints(
					0, 0,
					GridBagConstraints.REMAINDER, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );

    ButtonGroup grpSys = new ButtonGroup();

    this.rbRF64x16 = GUIFactory.createRadioButton(
				"RAM-Floppy-System, 64x16 Zeichen",
				true );
    grpSys.add( this.rbRF64x16 );
    this.tabModel.add( this.rbRF64x16, gbcModel );

    this.cbAutoLoadBDOS = GUIFactory.createCheckBox(
				"Bei RESET automatisch BDOS laden",
				true );
    gbcModel.insets.top  = 0;
    gbcModel.insets.left = 50;
    gbcModel.gridy++;
    this.tabModel.add( this.cbAutoLoadBDOS, gbcModel );

    this.rbFDC64x16 = GUIFactory.createRadioButton(
				"Floppy-Disk-System, 64x16 Zeichen" );
    grpSys.add( this.rbFDC64x16 );
    gbcModel.insets.left = 5;
    gbcModel.gridy++;
    this.tabModel.add( this.rbFDC64x16, gbcModel );

    this.rbFDC80x24 = GUIFactory.createRadioButton(
				"Floppy-Disk-System, 80x24 Zeichen" );
    grpSys.add( this.rbFDC80x24 );
    gbcModel.insets.bottom = 5;
    gbcModel.gridy++;
    this.tabModel.add( this.rbFDC80x24, gbcModel );

    gbcModel.fill          = GridBagConstraints.HORIZONTAL;
    gbcModel.weightx       = 1.0;
    gbcModel.insets.top    = 10;
    gbcModel.insets.bottom = 10;
    gbcModel.gridy++;
    this.tabModel.add( GUIFactory.createSeparator(), gbcModel );

    this.fldAltBDOS = new RAMFileSettingsFld(
		settingsFrm,
		propPrefix + PCM.PROP_BDOS_PREFIX,
		"Alternative RAM-Datei f\u00FCr BDOS:" );
    gbcModel.insets.top    = 5;
    gbcModel.insets.bottom = 5;
    gbcModel.gridy++;
    this.tabModel.add( this.fldAltBDOS, gbcModel );

    this.fldAltROM = new ROMFileSettingsFld(
		settingsFrm,
		propPrefix + PCM.PROP_ROM_PREFIX,
		"Alternativer ROM-Inhalt (Grundbetriebssystem):" );
    gbcModel.insets.top    = 5;
    gbcModel.insets.bottom = 5;
    gbcModel.gridy++;
    this.tabModel.add( this.fldAltROM, gbcModel );

    this.fldAltFont = new ROMFileSettingsFld(
				settingsFrm,
				propPrefix + PCM.PROP_FONT_PREFIX,
				"Alternativer Zeichensatz:" );
    gbcModel.gridy++;
    this.tabModel.add( this.fldAltFont, gbcModel );


    // Tab Erweiterungen
    this.tabExt = GUIFactory.createPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "Erweiterungen", this.tabExt );

    GridBagConstraints gbcExt = new GridBagConstraints(
					0, 0,
					GridBagConstraints.REMAINDER, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );

    this.cbK1520Sound = GUIFactory.createCheckBox( "K1520-Sound-Karte" );
    this.tabExt.add( this.cbK1520Sound, gbcExt );

    this.cbVDIP = GUIFactory.createCheckBox(
				"USB-Anschluss (Vinculum VDIP Modul)" );
    gbcExt.insets.top    = 0;
    gbcExt.insets.bottom = 5;
    gbcExt.gridy++;
    this.tabExt.add( this.cbVDIP, gbcExt );


    // Tab AutoLoad
    this.tabAutoLoad = new AutoLoadSettingsFld(
				settingsFrm,
				propPrefix,
				PCM.DEFAULT_PROMPT_AFTER_RESET_MILLIS_MAX,
				true );
    this.tabbedPane.addTab( "AutoLoad", this.tabAutoLoad );


    // Tab AutoInput
    this.tabAutoInput = new AutoInputSettingsFld(
				settingsFrm,
				propPrefix,
				AutoInputCharSet.getCPMCharSet(),
				PCM.DEFAULT_SWAP_KEY_CHAR_CASE,
				PCM.DEFAULT_PROMPT_AFTER_RESET_MILLIS_MAX );
    this.tabbedPane.addTab( "AutoInput", this.tabAutoInput );


    // Listener
    this.rbRF64x16.addActionListener( this );
    this.rbFDC64x16.addActionListener( this );
    this.rbFDC80x24.addActionListener( this );
    this.cbAutoLoadBDOS.addActionListener( this );
    this.cbK1520Sound.addActionListener( this );
    this.cbVDIP.addActionListener( this );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void applyInput(
			Properties props,
			boolean    selected ) throws UserInputException
  {
    // Tab Modell
    boolean fdc64x16 = this.rbFDC64x16.isSelected();
    boolean fdc80x24 = this.rbFDC80x24.isSelected();
    EmuUtil.setProperty(
		props,
		this.propPrefix + PCM.PROP_FDC_ENABLED,
		fdc64x16 || fdc80x24 );
    EmuUtil.setProperty(
		props,
		this.propPrefix + PCM.PROP_GRAPHIC,
		fdc80x24 ?
			PCM.VALUE_GRAPHIC_80X24
			: PCM.VALUE_GRAPHIC_64X32 );
    EmuUtil.setProperty(
		props,
		this.propPrefix + PCM.PROP_BDOS_PREFIX + PCM.PROP_AUTOLOAD,
		this.cbAutoLoadBDOS.isSelected() );
    this.fldAltBDOS.applyInput( props, selected );
    this.fldAltROM.applyInput( props, selected );
    this.fldAltFont.applyInput( props, selected );

    // Tab Erweiterungen
    EmuUtil.setProperty(
		props,
		this.propPrefix + PCM.PROP_K1520SOUND_ENABLED,
		this.cbK1520Sound.isSelected() );
    EmuUtil.setProperty(
		props,
		this.propPrefix + PCM.PROP_VDIP_ENABLED,
		this.cbVDIP.isSelected() );

    // Tab AutoLoad
    this.tabAutoLoad.applyInput( props, selected );

    // Tab AutoInput
    this.tabAutoInput.applyInput( props, selected );
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
      if( !rv ) {
	if( src instanceof JRadioButton ) {
	  updAutoLoadBDOSFieldEnabled();
	  fireDataChanged();
	  rv = true;
	}
	else if( src instanceof AbstractButton ) {
	  fireDataChanged();
	  rv = true;
	}
      }
    }
    return rv;
  }


  @Override
  public void updFields( Properties props )
  {
    if( EmuUtil.getBooleanProperty(
		props,
		this.propPrefix + PCM.PROP_FDC_ENABLED,
		false ) )
    {
      if( EmuUtil.getProperty(
		props,
		this.propPrefix + PCM.PROP_GRAPHIC ).equals(
					PCM.VALUE_GRAPHIC_80X24 ) )
      {
	this.rbFDC80x24.setSelected( true );
      } else {
	this.rbFDC64x16.setSelected( true );
      }
    } else {
      this.rbRF64x16.setSelected( true );
    }
    this.cbAutoLoadBDOS.setSelected(
		EmuUtil.getBooleanProperty(
				props,
				this.propPrefix + PCM.PROP_BDOS_PREFIX
						+ PCM.PROP_AUTOLOAD,
				true ) );
    this.fldAltBDOS.updFields( props );
    this.fldAltROM.updFields( props );
    this.fldAltFont.updFields( props );
    updAutoLoadBDOSFieldEnabled();

    this.cbK1520Sound.setSelected(
		EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + PCM.PROP_K1520SOUND_ENABLED,
			false ) );
    this.cbVDIP.setSelected(
		EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + PCM.PROP_VDIP_ENABLED,
			false ) );

    this.tabAutoLoad.updFields( props );
    this.tabAutoInput.updFields( props );
  }


	/* --- private Methoden --- */

  private void updAutoLoadBDOSFieldEnabled()
  {
    this.cbAutoLoadBDOS.setEnabled( this.rbRF64x16.isSelected() );
    this.fldAltBDOS.setEnabled( this.rbRF64x16.isSelected() );
  }
}
