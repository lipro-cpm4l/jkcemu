/*
 * (c) 2011-2012 Jens Mueller
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
  private JCheckBox          btnFDC;
  private JCheckBox          btnFixedScreenSize;
  private ROMFileSettingsFld fldAltRomFDC;


  public KCcompactSettingsFld( SettingsFrm settingsFrm, String propPrefix )
  {
    super( settingsFrm, propPrefix );

    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					GridBagConstraints.REMAINDER, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 5, 5 ),
					0, 0 );

    this.btnFDC = new JCheckBox( "Floppy-Disk-Station emulieren", false );
    this.btnFDC.addActionListener( this );
    add( this.btnFDC, gbc );

    this.fldAltRomFDC = new ROMFileSettingsFld(
		settingsFrm,
		"jkcemu.kccompact.fdc.rom.",
		"Alternativer ROM in der Floppy-Disk-Station:" );
    gbc.fill    = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;
    gbc.gridy++;
    add( this.fldAltRomFDC, gbc );

    gbc.gridy++;
    add( new JSeparator(), gbc );

    this.btnFixedScreenSize = new JCheckBox(
		"Gleiche Fenstergr\u00F6\u00DFe in allen Bildschirmmodi" );
    this.btnFixedScreenSize.addActionListener( this );
    gbc.fill    = GridBagConstraints.NONE;
    gbc.weightx = 0.0;
    gbc.gridy++;
    add( this.btnFixedScreenSize, gbc );

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
    this.fldAltRomFDC.applyInput( props, selected );
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
    this.fldAltRomFDC.updFields( props );
    updFieldsEnabled();
  }


	/* --- private Methoden --- */

  private void updFieldsEnabled()
  {
    this.fldAltRomFDC.setEnabled( this.btnFDC.isSelected() );
  }
}
