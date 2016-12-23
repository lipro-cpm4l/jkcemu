/*
 * (c) 2011-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Komponente fuer die KC-compact-Einstellungen
 */

package jkcemu.emusys.kccompact;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.lang.*;
import java.util.EventObject;
import java.util.Properties;
import javax.swing.AbstractButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import jkcemu.base.AbstractSettingsFld;
import jkcemu.base.AutoInputSettingsFld;
import jkcemu.base.EmuUtil;
import jkcemu.base.ROMFileSettingsFld;
import jkcemu.base.SettingsFrm;
import jkcemu.base.UserInputException;
import jkcemu.emusys.KCcompact;


public class KCcompactSettingsFld extends AbstractSettingsFld
{
  private JTabbedPane          tabbedPane;
  private JCheckBox            btnFDC;
  private JCheckBox            btnFixedScreenSize;
  private JPanel               tabFDC;
  private JPanel               tabEtc;
  private ROMFileSettingsFld   fldAltFDC;
  private ROMFileSettingsFld   fldAltOS;
  private ROMFileSettingsFld   fldAltBasic;
  private AutoInputSettingsFld tabAutoInput;


  public KCcompactSettingsFld( SettingsFrm settingsFrm, String propPrefix )
  {
    super( settingsFrm, propPrefix );
    setLayout( new BorderLayout() );

    this.tabbedPane = new JTabbedPane( JTabbedPane.TOP );
    add( this.tabbedPane, BorderLayout.CENTER );


    // Tab Floppy-Disk-Station
    this.tabFDC = new JPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "Floppy-Disk-Station", this.tabFDC );

    GridBagConstraints gbcFDC = new GridBagConstraints(
					0, 0,
					GridBagConstraints.REMAINDER, 1,
					1.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.HORIZONTAL,
					new Insets( 5, 5, 5, 5 ),
					0, 0 );

    this.btnFDC = new JCheckBox( "Floppy-Disk-Station emulieren", false );
    this.btnFDC.addActionListener( this );
    this.tabFDC.add( this.btnFDC, gbcFDC );

    this.fldAltFDC = new ROMFileSettingsFld(
		settingsFrm,
		this.propPrefix + KCcompact.PROP_FDC_ROM_PREFIX,
		"Alternativer ROM in der Floppy-Disk-Station:" );
    gbcFDC.fill        = GridBagConstraints.HORIZONTAL;
    gbcFDC.weightx     = 1.0;
    gbcFDC.insets.left = 50;
    gbcFDC.gridy++;
    this.tabFDC.add( this.fldAltFDC, gbcFDC );


    // Tab Sonstiges
    this.tabEtc = new JPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "Sonstiges", this.tabEtc );

    GridBagConstraints gbcEtc = new GridBagConstraints(
					0, 0,
					GridBagConstraints.REMAINDER, 1,
					1.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.HORIZONTAL,
					new Insets( 5, 5, 5, 5 ),
					0, 0 );

    this.btnFixedScreenSize = new JCheckBox(
		"Gleiche Fenstergr\u00F6\u00DFe in allen Bildschirmmodi" );
    this.btnFixedScreenSize.addActionListener( this );
    this.tabEtc.add( this.btnFixedScreenSize, gbcEtc );

    gbcEtc.fill          = GridBagConstraints.HORIZONTAL;
    gbcEtc.weightx       = 1.0;
    gbcEtc.insets.top    = 10;
    gbcEtc.insets.bottom = 10;
    gbcEtc.gridy++;
    this.tabEtc.add( new JSeparator(), gbcEtc );

    this.fldAltOS = new ROMFileSettingsFld(
				settingsFrm,
				propPrefix + KCcompact.PROP_OS_PREFIX,
				"Alternativer Betriebssystem-ROM:" );
    gbcEtc.insets.top    = 5;
    gbcEtc.insets.bottom = 5;
    gbcEtc.gridy++;
    this.tabEtc.add( this.fldAltOS, gbcEtc );

    this.fldAltBasic = new ROMFileSettingsFld(
				settingsFrm,
				propPrefix + KCcompact.PROP_BASIC_PREFIX,
				"Alternativer BASIC-ROM:" );
    gbcEtc.gridy++;
    this.tabEtc.add( this.fldAltBasic, gbcEtc );


    // Tab AutoInput
    this.tabAutoInput = new AutoInputSettingsFld(
		settingsFrm,
		propPrefix,
		KCcompact.DEFAULT_SWAP_KEY_CHAR_CASE,
		KCcompact.DEFAULT_PROMPT_AFTER_RESET_MILLIS_MAX );
    this.tabbedPane.addTab( "AutoInput", this.tabAutoInput );


    updFieldsEnabled();
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void applyInput(
		Properties props,
		boolean    selected ) throws UserInputException
  {
    Component tab = null;
    try {

      // Tab Floppy-Disk-Station
      tab = this.tabFDC;
      EmuUtil.setProperty(
		props,
		this.propPrefix + KCcompact.PROP_FDC_ENABLED,
		this.btnFDC.isSelected() );

      // Tab Sonstiges
      tab = this.tabEtc;
      EmuUtil.setProperty(
		props,
		this.propPrefix + KCcompact.PROP_FIXED_SCREEN_SIZE,
		this.btnFixedScreenSize.isSelected() );
      this.fldAltFDC.applyInput( props, selected );
      this.fldAltOS.applyInput( props, selected );
      this.fldAltBasic.applyInput( props, selected );

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
      rv = this.tabAutoInput.doAction( e );
      if( !rv && (src instanceof AbstractButton) ) {
	updFieldsEnabled();
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
    this.fldAltFDC.lookAndFeelChanged();
    this.fldAltBasic.lookAndFeelChanged();
    this.tabAutoInput.lookAndFeelChanged();
  }


  @Override
  public void updFields( Properties props )
  {
    this.btnFDC.setSelected(
		EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + KCcompact.PROP_FDC_ENABLED,
			false ) );
    this.btnFixedScreenSize.setSelected(
		EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + KCcompact.PROP_FIXED_SCREEN_SIZE,
			false ) );
    this.fldAltFDC.updFields( props );
    this.fldAltOS.updFields( props );
    this.fldAltBasic.updFields( props );
    this.tabAutoInput.updFields( props );
    updFieldsEnabled();
  }


	/* --- private Methoden --- */

  private void updFieldsEnabled()
  {
    this.fldAltFDC.setEnabled( this.btnFDC.isSelected() );
  }
}
