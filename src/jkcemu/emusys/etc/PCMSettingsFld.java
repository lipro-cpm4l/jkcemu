/*
 * (c) 2011 Jens Mueller
 * (c) 2014-2015 Stephan Linz
 *
 * Kleincomputer-Emulator
 *
 * Komponente fuer die PC/M-Einstellungen
 */

package jkcemu.emusys.etc;

import java.awt.*;
import java.io.File;
import java.lang.*;
import java.util.*;
import javax.swing.*;
import jkcemu.base.*;


public class PCMSettingsFld extends AbstractSettingsFld
{
  private JCheckBox          btnAutoLoadBDOS;
  private JRadioButton       btnRF64x16;
  private JRadioButton       btnFDC64x16;
  private JRadioButton       btnFDC80x24;
  private RAMFileSettingsFld fldAltBDOS;
  private ROMFileSettingsFld fldAltROM;
  private ROMFileSettingsFld fldAltFont;


  public PCMSettingsFld( SettingsFrm settingsFrm, String propPrefix )
  {
    super( settingsFrm, propPrefix );

    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					GridBagConstraints.REMAINDER, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );

    ButtonGroup grpSys = new ButtonGroup();

    this.btnRF64x16 = new JRadioButton(
				"RAM-Floppy-System, 64x16 Zeichen",
				true );
    grpSys.add( this.btnRF64x16 );
    this.btnRF64x16.addActionListener( this );
    add( this.btnRF64x16, gbc );

    this.btnAutoLoadBDOS = new JCheckBox(
				"Bei RESET automatisch BDOS laden",
				true );
    this.btnAutoLoadBDOS.addActionListener( this );
    gbc.insets.top  = 0;
    gbc.insets.left = 50;
    gbc.gridy++;
    add( this.btnAutoLoadBDOS, gbc );

    this.btnFDC64x16 = new JRadioButton(
				"Floppy-Disk-System, 64x16 Zeichen",
				false );
    grpSys.add( this.btnFDC64x16 );
    this.btnFDC64x16.addActionListener( this );
    gbc.insets.left = 5;
    gbc.gridy++;
    add( this.btnFDC64x16, gbc );

    this.btnFDC80x24 = new JRadioButton(
				"Floppy-Disk-System, 80x24 Zeichen",
				false );
    grpSys.add( this.btnFDC80x24 );
    this.btnFDC80x24.addActionListener( this );
    gbc.insets.bottom = 5;
    gbc.gridy++;
    add( this.btnFDC80x24, gbc );

    gbc.fill       = GridBagConstraints.HORIZONTAL;
    gbc.weightx    = 1.0;
    gbc.insets.top = 5;
    gbc.gridy++;
    add( new JSeparator(), gbc );

    this.fldAltBDOS = new RAMFileSettingsFld(
		settingsFrm,
		propPrefix + "bdos.",
		"Alternative RAM-Datei f\u00FCr BDOS:" );
    gbc.gridy++;
    add( this.fldAltBDOS, gbc );

    this.fldAltROM = new ROMFileSettingsFld(
		settingsFrm,
		propPrefix + "rom.",
		"Alternativer ROM-Inhalt (Grundbetriebssystem):" );
    gbc.gridy++;
    add( this.fldAltROM, gbc );

    this.fldAltFont = new ROMFileSettingsFld(
				settingsFrm,
				propPrefix + "font.",
				"Alternativer Zeichensatz:" );
    gbc.gridy++;
    add( this.fldAltFont, gbc );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void applyInput(
			Properties props,
			boolean    selected ) throws UserInputException
  {
    boolean fdc64x16 = this.btnFDC64x16.isSelected();
    boolean fdc80x24 = this.btnFDC80x24.isSelected();
    EmuUtil.setProperty(
		props,
		this.propPrefix + "floppydisk.enabled",
		fdc64x16 || fdc80x24 );
    EmuUtil.setProperty(
		props,
		this.propPrefix + "graphic",
		fdc80x24 ? "80x24" : "64x16" );
    EmuUtil.setProperty(
		props,
		this.propPrefix + "bdos.autoload",
		this.btnAutoLoadBDOS.isSelected() );
    this.fldAltBDOS.applyInput( props, selected );
    this.fldAltROM.applyInput( props, selected );
    this.fldAltFont.applyInput( props, selected );
  }


  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv  = false;
    Object  src = e.getSource();
    if( src != null ) {
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
    return rv;
  }


  @Override
  public void updFields( Properties props )
  {
    if( EmuUtil.getBooleanProperty(
			props,
			this.propPrefix + "floppydisk.enabled",
			false ) )
    {
      if( EmuUtil.getProperty(
			props,
			this.propPrefix + "graphic" ).equals( "80x24" ) )
      {
	this.btnFDC80x24.setSelected( true );
      } else {
	this.btnFDC64x16.setSelected( true );
      }
    } else {
      this.btnRF64x16.setSelected( true );
    }
    this.btnAutoLoadBDOS.setSelected(
			EmuUtil.getBooleanProperty(
				props,
				this.propPrefix + "bdos.autoload",
				true ) );
    this.fldAltBDOS.updFields( props );
    this.fldAltROM.updFields( props );
    this.fldAltFont.updFields( props );
    updAutoLoadBDOSFieldEnabled();
  }


	/* --- private Methoden --- */

  private void updAutoLoadBDOSFieldEnabled()
  {
    this.btnAutoLoadBDOS.setEnabled( this.btnRF64x16.isSelected() );
    this.fldAltBDOS.setEnabled( this.btnRF64x16.isSelected() );
  }
}

