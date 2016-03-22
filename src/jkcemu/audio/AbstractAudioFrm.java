/*
 * (c) 2008-2015 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Basisklasse fuer Fenster mit Audio-Funktionen (Pegel-Anzeige)
 */

package jkcemu.audio;

import java.awt.EventQueue;
import java.awt.event.*;
import java.lang.*;
import javax.sound.sampled.*;
import javax.swing.*;
import jkcemu.Main;
import jkcemu.base.*;


public class AbstractAudioFrm extends BasicFrm
{
  protected static final int[] sampleRates = {
				96000, 48000, 44100, 32000,
				22050, 16000, 11025, 8000 };

  protected Mixer.Info[]      mixers;
  protected javax.swing.Timer volumeTimer;
  protected JLabel            labelMixer;
  protected JComboBox<String> comboMixer;
  protected JLabel            labelSampleRate;
  protected JComboBox<Object> comboSampleRate;
  protected JProgressBar      volumeBar;

  private static final int VOLUME_BAR_MAX = 1000;

  private int minVolumeLimit;
  private int maxVolumeLimit;
  private int minVolumeValue;
  private int maxVolumeValue;


  protected AbstractAudioFrm()
  {
    this.mixers = AudioSystem.getMixerInfo();
    Main.updIcon( this );

    // Mixer
    this.labelMixer = new JLabel( "Ger\u00E4t:" );
    this.comboMixer = new JComboBox<>();
    this.comboMixer.setEditable( false );
    this.comboMixer.addItem( "Standard" );
    if( this.mixers != null ) {
      for( int i = 0; i < this.mixers.length; i++ ) {
	String s = this.mixers[ i ].getName();
	if( s != null ) {
	  if( s.isEmpty() ) {
	    s = null;
	  }
	}
	this.comboMixer.addItem( s != null ? s : "unbekannt" );
      }
    }

    // Abtastrate
    this.labelSampleRate = new JLabel( "Abtastrate (Hz):" );
    this.comboSampleRate = new JComboBox<>();
    this.comboSampleRate.setEditable( false );
    this.comboSampleRate.addItem( "Standard" );
    for( int i = 0; i < this.sampleRates.length; i++ ) {
      this.comboSampleRate.addItem( new Integer( this.sampleRates[ i ] ) );
    }

    // Pegel-Anzeige
    this.minVolumeLimit = 0;
    this.maxVolumeLimit = 255;
    this.minVolumeValue = this.maxVolumeLimit;
    this.maxVolumeValue = this.minVolumeLimit;
    this.volumeBar      = new JProgressBar(
					SwingConstants.VERTICAL,
					0,
					VOLUME_BAR_MAX );
    this.volumeTimer = new javax.swing.Timer(
			100,
			new ActionListener()
			{
			  public void actionPerformed( ActionEvent e )
			  {
			    updVolumeBar();
			  }
			} );
  }


  protected synchronized void setVolumeLimits( int minLimit, int maxLimit )
  {
    if( minLimit < maxLimit ) {
      this.minVolumeLimit = minLimit;
      this.maxVolumeLimit = maxLimit;
      this.maxVolumeValue = maxLimit;
      this.minVolumeValue = minLimit;
      this.volumeBar.setValue( 0 );
    }
  }


  protected Mixer getSelectedMixer()
  {
    Mixer mixer = null;
    if( this.mixers != null ) {
      int idx = this.comboMixer.getSelectedIndex() - 1;
      if( (idx >= 0) && (idx < this.mixers.length) ) {
	try {
	  mixer = AudioSystem.getMixer( (Mixer.Info) this.mixers[ idx ] );
	}
	catch( IllegalArgumentException ex ) {}
      }
    }
    return mixer;
  }


  protected int getSelectedSampleRate()
  {
    int    rv = 0;
    Object o  = this.comboSampleRate.getSelectedItem();
    if( o != null ) {
      if( o instanceof Integer ) {
	rv = ((Integer) o).intValue();
      }
    }
    return rv;
  }


  protected void setMixerState( boolean state )
  {
    this.labelMixer.setEnabled( state );
    this.comboMixer.setEnabled( state );
  }


  protected void setSampleRateState( boolean state )
  {
    this.labelSampleRate.setEnabled( state );
    this.comboSampleRate.setEnabled( state );
  }


  protected void setVolumeBarState( boolean state )
  {
    if( state ) {
      this.volumeTimer.start();
    } else {
      this.volumeBar.setValue( 0 );
      this.volumeTimer.stop();
    }
    this.volumeBar.setEnabled( state );
  }


  protected void showError( String errorText )
  {
    if( errorText == null ) {
      if( getSelectedSampleRate() > 0 ) {
	errorText = "Es konnte kein Audiokanal mit den angegebenen"
				+ " Optionen ge\u00F6ffnet werden.";
      } else {
	errorText = "Es konnte kein Audiokanal ge\u00F6ffnet werden.";
      }
    }
    BasicDlg.showErrorDlg( this, errorText );
  }


  public synchronized void updVolume( int value )
  {
    if( value < this.minVolumeValue ) {
      this.minVolumeValue = value;
    }
    if( value > this.maxVolumeValue ) {
      this.maxVolumeValue = value;
    }
  }


	/* --- private Methoden --- */

  private void updVolumeBar()
  {
    int barValue = 0;
    int volume   = 0;
    synchronized( this ) {
      volume              = this.maxVolumeValue - this.minVolumeValue;
      this.minVolumeValue = this.maxVolumeLimit;
      this.maxVolumeValue = this.minVolumeLimit;
    }
    /*
     * Logarithmische Pegelanzeige:
     *   Der Pegel wird auf den Bereich 0 bis 100 normiert,
     *   aus dem Wert plus eins der Logarithmus gebildet
     *   und anschliessend auf den Bereich der Anzeige skaliert.
     */
    double v = (double) volume
		/ (double) (this.maxVolumeLimit - this.minVolumeLimit)
		* 100.0;
    if( v > 0.0 ) {
      barValue = (int) Math.round( Math.log( 1.0 + v )
					* (double) VOLUME_BAR_MAX
					/ 4.6 );	// log(100)
      if( barValue < 0 ) {
	barValue = 0;
      } else if( barValue > VOLUME_BAR_MAX ) {
	barValue = VOLUME_BAR_MAX;
      }
    }
    this.volumeBar.setValue( barValue );
  }
}
