/*
 * (c) 2011-2015 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Komponente fuer die KC-compact-Einstellungen
 */

package jkcemu.emusys.kccompact;

import java.awt.*;
import java.lang.*;
import java.util.*;
import javax.swing.*;
import jkcemu.base.*;


public class KCcompactSettingsFld extends AbstractSettingsFld
{
  private JTabbedPane        tabbedPane;
  private JCheckBox          btnFDC;
  private JCheckBox          btnFixedScreenSize;
  private JRadioButton       btnSoundMono;
  private JRadioButton       btnSoundStereo;
  private JPanel             tabFDC;
  private JPanel             tabSound;
  private JPanel             tabEtc;
  private ROMFileSettingsFld fldAltFDC;
  private ROMFileSettingsFld fldAltOS;
  private ROMFileSettingsFld fldAltBasic;


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
		this.propPrefix + "fdc.rom.",
		"Alternativer ROM in der Floppy-Disk-Station:" );
    gbcFDC.fill        = GridBagConstraints.HORIZONTAL;
    gbcFDC.weightx     = 1.0;
    gbcFDC.insets.left = 50;
    gbcFDC.gridy++;
    this.tabFDC.add( this.fldAltFDC, gbcFDC );


    // Tab Sound
    this.tabSound = new JPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "Sound", this.tabSound );

    GridBagConstraints gbcSound = new GridBagConstraints(
					0, 0,
					GridBagConstraints.REMAINDER, 1,
					1.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.HORIZONTAL,
					new Insets( 5, 5, 5, 5 ),
					0, 0 );

    this.tabSound.add(
		new JLabel( "Tonausgabe des Sound-Generators:" ),
		gbcSound );

    ButtonGroup grpSoundChannels = new ButtonGroup();

    this.btnSoundMono = new JRadioButton( "Mono (Kan\u00E4le: A+B+C)", true );
    this.btnSoundMono.addActionListener( this );
    grpSoundChannels.add( this.btnSoundMono );
    gbcSound.insets.left   = 50;
    gbcSound.insets.top    = 0;
    gbcSound.insets.bottom = 0;
    gbcSound.gridy++;
    this.tabSound.add( this.btnSoundMono, gbcSound );

    this.btnSoundStereo = new JRadioButton(
                "Stereo (Kan\u00E4le: Links=A+B/2, Rechts=C+B/2)" );
    this.btnSoundStereo.addActionListener( this );
    grpSoundChannels.add( this.btnSoundStereo );
    gbcSound.insets.bottom = 5;
    gbcSound.gridy++;
    this.tabSound.add( this.btnSoundStereo, gbcSound );


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
				propPrefix + "os.",
				"Alternativer Betriebssystem-ROM:" );
    gbcEtc.insets.top    = 5;
    gbcEtc.insets.bottom = 5;
    gbcEtc.gridy++;
    this.tabEtc.add( this.fldAltOS, gbcEtc );

    this.fldAltBasic = new ROMFileSettingsFld(
				settingsFrm,
				propPrefix + "basic.",
				"Alternativer BASIC-ROM:" );
    gbcEtc.gridy++;
    this.tabEtc.add( this.fldAltBasic, gbcEtc );

    updFieldsEnabled();
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void applyInput(
		Properties props,
		boolean    selected ) throws UserInputException
  {
    EmuUtil.setProperty(
		props,
		this.propPrefix + "floppydisk.enabled",
		this.btnFDC.isSelected() );
    EmuUtil.setProperty(
		props,
		this.propPrefix + "fixed_screen_size",
		this.btnFixedScreenSize.isSelected() );
    EmuUtil.setProperty(
		props,
		this.propPrefix + "sound.stereo",
		this.btnSoundStereo.isSelected() );
    this.fldAltFDC.applyInput( props, selected );
    this.fldAltOS.applyInput( props, selected );
    this.fldAltBasic.applyInput( props, selected );
  }


  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv  = false;
    Object  src = e.getSource();
    if( src != null ) {
      if( src instanceof AbstractButton ) {
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
  }


  @Override
  public void updFields( Properties props )
  {
    this.btnFDC.setSelected(
		EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + "floppydisk.enabled",
			false ) );
    this.btnFixedScreenSize.setSelected(
		EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + "fixed_screen_size",
			false ) );
    if( EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + "sound.stereo",
			false ) )
    {
      this.btnSoundStereo.setSelected( true );
    } else {
      this.btnSoundMono.setSelected( true );
    }
    this.fldAltFDC.updFields( props );
    this.fldAltOS.updFields( props );
    this.fldAltBasic.updFields( props );
    updFieldsEnabled();
  }


	/* --- private Methoden --- */

  private void updFieldsEnabled()
  {
    this.fldAltFDC.setEnabled( this.btnFDC.isSelected() );
  }
}
