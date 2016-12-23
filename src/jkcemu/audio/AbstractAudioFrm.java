/*
 * (c) 2008-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Basisklasse fuer Fenster mit Audiofunktionen
 */

package jkcemu.audio;

import java.lang.*;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;
import javax.swing.JComboBox;
import jkcemu.Main;
import jkcemu.base.BaseFrm;


public class AbstractAudioFrm extends BaseFrm
{
  protected static final int[] frameRates = {
				96000, 48000, 44100, 45454, 32000,
				22050, 16000, 11025, 8000 };

  private Mixer.Info[] mixers;


  protected AbstractAudioFrm()
  {
    this.mixers = AudioSystem.getMixerInfo();
    Main.updIcon( this );
  }


  protected JComboBox<Object> createFrameRateComboBox()
  {
    JComboBox<Object> comboBox = new JComboBox<>();
    comboBox.setEditable( false );
    comboBox.addItem( "Standard" );
    for( int i = 0; i < this.frameRates.length; i++ ) {
      comboBox.addItem( this.frameRates[ i ] );
    }
    return comboBox;
  }


  protected JComboBox<String> createMixerComboBox()
  {
    JComboBox<String> comboBox = new JComboBox<>();
    comboBox.setEditable( false );
    comboBox.addItem( "Standard" );
    if( this.mixers != null ) {
      for( int i = 0; i < this.mixers.length; i++ ) {
	String s = this.mixers[ i ].getName();
	if( s != null ) {
	  if( s.isEmpty() ) {
	    s = null;
	  }
	}
	comboBox.addItem( s != null ? s : "unbekannt" );
      }
    }
    return comboBox;
  }


  protected Mixer getSelectedMixer( JComboBox<String> comboBox )
  {
    Mixer mixer = null;
    if( this.mixers != null ) {
      int idx = comboBox.getSelectedIndex() - 1;
      if( (idx >= 0) && (idx < this.mixers.length) ) {
	try {
	  mixer = AudioSystem.getMixer( (Mixer.Info) this.mixers[ idx ] );
	}
	catch( IllegalArgumentException ex ) {}
      }
    }
    return mixer;
  }


  protected int getSelectedFrameRate( JComboBox<Object> comboBox )
  {
    int    rv = 0;
    Object o  = comboBox.getSelectedItem();
    if( o != null ) {
      if( o instanceof Integer ) {
	rv = ((Integer) o).intValue();
      }
    }
    return rv;
  }
}
