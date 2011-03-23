/*
 * (c) 2011 Jens Mueller
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
					new Insets( 5, 5, 5, 5 ),
					0, 0 );

    this.btnAutoLoadBDOS = new JCheckBox(
				"Bei RESET automatisch BDOS laden",
				true );
    this.btnAutoLoadBDOS.addActionListener( this );
    add( this.btnAutoLoadBDOS, gbc );

    gbc.fill       = GridBagConstraints.HORIZONTAL;
    gbc.weightx    = 1.0;
    gbc.insets.top = 5;
    gbc.gridy++;
    add( new JSeparator(), gbc );

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
    EmuUtil.setProperty(
		props,
		this.propPrefix + "auto_load_bdos",
		this.btnAutoLoadBDOS.isSelected() );
    this.fldAltROM.applyInput( props, selected );
    this.fldAltFont.applyInput( props, selected );
  }


  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv  = false;
    Object  src = e.getSource();
    if( src != null ) {
      if( src instanceof AbstractButton ) {
	fireDataChanged();
	rv = true;
      }
    }
    return rv;
  }


  @Override
  public void updFields( Properties props )
  {
    this.btnAutoLoadBDOS.setSelected(
			EmuUtil.getBooleanProperty(
				props,
				this.propPrefix + "auto_load_bdos",
				true ) );
    this.fldAltROM.updFields( props );
    this.fldAltFont.updFields( props );
  }
}

