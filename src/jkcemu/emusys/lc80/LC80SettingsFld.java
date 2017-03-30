/*
 * (c) 2012-2017 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Komponente fuer die Einstellungen fuer LC80
 */

package jkcemu.emusys.lc80;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.lang.*;
import java.util.EventObject;
import java.util.Properties;
import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import jkcemu.base.AbstractSettingsFld;
import jkcemu.base.AutoInputSettingsFld;
import jkcemu.base.AutoLoadSettingsFld;
import jkcemu.base.EmuThread;
import jkcemu.base.EmuUtil;
import jkcemu.base.ROMFileSettingsFld;
import jkcemu.base.SettingsFrm;
import jkcemu.base.UserInputException;
import jkcemu.emusys.LC80;


public class LC80SettingsFld extends AbstractSettingsFld
{
  private JTabbedPane          tabbedPane;
  private JPanel               tabModel;
  private JPanel               tabRom;
  private AutoLoadSettingsFld  tabAutoLoad;
  private AutoInputSettingsFld tabAutoInput;
  private JRadioButton         btnLC80_U505;
  private JRadioButton         btnLC80_2716;
  private JRadioButton         btnLC80_2;
  private JRadioButton         btnLC80e;
  private JRadioButton         btnLC80ex;
  private ROMFileSettingsFld   fldAltOS;
  private ROMFileSettingsFld   fldAltA000;
  private ROMFileSettingsFld   fldAltC000;


  public LC80SettingsFld( SettingsFrm settingsFrm, String propPrefix )
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
    gbcModel.gridy++;
    this.tabModel.add( this.btnLC80e, gbcModel );

    this.btnLC80ex = new JRadioButton(
		"LC-80ex, 20 KByte ROM, 32 KByte RAM, TV-Terminal 1.2",
		false );
    this.btnLC80ex.addActionListener( this );
    grpModel.add( this.btnLC80ex );
    gbcModel.insets.bottom = 5;
    gbcModel.gridy++;
    this.tabModel.add( this.btnLC80ex, gbcModel );


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
		propPrefix + LC80.PROP_OS_PREFIX,
		"Alternatives Monitorprogramm (0000h-1FFFh):" );
    gbcRom.gridy++;
    this.tabRom.add( this.fldAltOS, gbcRom );

    this.fldAltA000 = new ROMFileSettingsFld(
		settingsFrm,
		propPrefix + LC80.PROP_ROM_A000_PREFIX,
		"Alternativer ROM-Inhalt A000h-BFFFh (nur LC80ex):" );
    gbcRom.gridy++;
    this.tabRom.add( this.fldAltA000, gbcRom );

    this.fldAltC000 = new ROMFileSettingsFld(
		settingsFrm,
		propPrefix + LC80.PROP_ROM_C000_PREFIX,
		"Alternativer ROM-Inhalt C000h-FFFFh (nur LC80e/LC80ex):" );
    gbcRom.gridy++;
    this.tabRom.add( this.fldAltC000, gbcRom );


    // Tab AutoLoad
    this.tabAutoLoad = new AutoLoadSettingsFld(
		settingsFrm,
		propPrefix,
		LC80.DEFAULT_PROMPT_AFTER_RESET_MILLIS_MAX,
		true );
    this.tabbedPane.addTab( "AutoLoad", this.tabAutoLoad );


    // Tab AutoInput
    this.tabAutoInput = new AutoInputSettingsFld(
		settingsFrm,
		propPrefix,
		LC80.DEFAULT_SWAP_KEY_CHAR_CASE,
		LC80.FUNCTION_KEY_COUNT,
		LC80.DEFAULT_PROMPT_AFTER_RESET_MILLIS_MAX );
    this.tabbedPane.addTab( "AutoInput", this.tabAutoInput );


    updFldAltC000Enabled();
  }


  public String getModelSysName()
  {
    String rv = LC80.SYSNAME_LC80_2716;
    if( this.btnLC80_U505.isSelected() ) {
      rv = LC80.SYSNAME_LC80_U505;
    } else if( this.btnLC80_2.isSelected() ) {
      rv = LC80.SYSNAME_LC80_2;
    } else if( this.btnLC80e.isSelected() ) {
      rv = LC80.SYSNAME_LC80_E;
    } else if( this.btnLC80ex.isSelected() ) {
      rv = LC80.SYSNAME_LC80_EX;
    }
    return rv;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void applyInput(
			Properties props,
			boolean    selected ) throws UserInputException
  {
    Component tab = null;
    try {

      // Tab ROM
      tab = this.tabRom;
      this.fldAltOS.applyInput( props, selected );
      this.fldAltA000.applyInput( props, selected );
      this.fldAltC000.applyInput( props, selected );

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
    if( (src == this.btnLC80_U505)
	|| (src == this.btnLC80_2716)
	|| (src == this.btnLC80_2)
	|| (src == this.btnLC80e)
	|| (src == this.btnLC80ex) )
    {
      rv = true;
      updFldAltA000Enabled();
      updFldAltC000Enabled();
      fireDataChanged();
    }
    if( !rv ) {
      rv = this.tabAutoLoad.doAction( e );
    }
    if( !rv ) {
      rv = this.tabAutoInput.doAction( e );
    }
    if( !rv && (src instanceof AbstractButton) ) {
      fireDataChanged();
      rv = true;
    }
    return rv;
  }


  @Override
  public void lookAndFeelChanged()
  {
    this.fldAltOS.lookAndFeelChanged();
    this.fldAltA000.lookAndFeelChanged();
    this.fldAltC000.lookAndFeelChanged();
    this.tabAutoLoad.lookAndFeelChanged();
    this.tabAutoInput.lookAndFeelChanged();
  }


  @Override
  public void updFields( Properties props )
  {
    switch( EmuUtil.getProperty( props, EmuThread.PROP_SYSNAME ) ) {
      case LC80.SYSNAME_LC80_U505:
	this.btnLC80_U505.setSelected( true );
	break;
      case LC80.SYSNAME_LC80_2:
	this.btnLC80_2.setSelected( true );
	break;
      case LC80.SYSNAME_LC80_E:
	this.btnLC80e.setSelected( true );
	break;
      case LC80.SYSNAME_LC80_EX:
	this.btnLC80ex.setSelected( true );
	break;
      default:
	this.btnLC80_2716.setSelected( true );
    }
    updFldAltC000Enabled();
    this.fldAltOS.updFields( props );
    this.fldAltA000.updFields( props );
    this.fldAltC000.updFields( props );
    this.tabAutoLoad.updFields( props );
    this.tabAutoInput.updFields( props );
  }


	/* --- private Methoden --- */

  private void updFldAltA000Enabled()
  {
    this.fldAltA000.setEnabled( this.btnLC80ex.isSelected() );
  }


  private void updFldAltC000Enabled()
  {
    this.fldAltC000.setEnabled(
		this.btnLC80e.isSelected() || this.btnLC80ex.isSelected() );
  }
}
