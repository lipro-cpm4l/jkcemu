/*
 * (c) 2016-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Komponente fuer die Emulation des Lautsprechers
 * bzw. Sound-Generatorausgangs
 */

package jkcemu.audio;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.Properties;
import javax.sound.sampled.Mixer;
import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import jkcemu.base.BaseDlg;
import jkcemu.base.EmuThread;
import jkcemu.base.EmuUtil;


public abstract class AbstractAudioIOFld
			extends JPanel
			implements ActionListener, AudioIOObserver
{
  protected static final String PROP_ENABLED    = "enabled";
  protected static final String PROP_FRAME_RATE = "frame_rate";
  protected static final String PROP_MIXER_NAME = "mixer.name";

  protected AudioFrm          audioFrm;
  protected EmuThread         emuThread;
  protected JLabel            labelFrameRate;
  protected JLabel            labelMixer;
  protected JLabel            labelFormat;
  protected JComboBox<Object> comboFrameRate;
  protected JComboBox<Object> comboMixer;
  protected JTextField        fldFormat;
  protected VolumeBar         volumeBar;
  protected JButton           btnEnable;
  protected JButton           btnDisable;


  public AbstractAudioIOFld( AudioFrm audioFrm, EmuThread emuThread )
  {
    this.audioFrm  = audioFrm;
    this.emuThread = emuThread;
  }


  protected void audioFinished( AudioIO audioIO, String errorMsg )
  {
    doDisable();
    if( errorMsg != null ) {
      showErrorDlg( errorMsg );
    }
  }


  protected boolean confirmMultipleLinesOpen()
  {
    return BaseDlg.showSuppressableConfirmDlg(
		this,
		"Mit \u00D6ffnen des Audiokanals ist sowohl eingangs-"
			+ " als auch ausgangseitig\n"
			+ "ein Audiokanal ge\u00F6ffnet,"
			+ " der durch den emulierten Mikroprozessor"
			+ " bedient wird.\n"
			+ "Falls Sie Daten \u00FCber das Audiosystem"
			+ " einlesen m\u00F6chten, sollte gleichzeitig\n"
			+ "kein ausgangsseitiger Audiokanal ge\u00F6ffnet"
			+ " sein, da dieser st\u00F6ren k\u00F6nnte." );
  }


  protected boolean doAction( ActionEvent e )
  {
    boolean rv  = true;
    Object  src = e.getSource();
    if( src == this.btnEnable ) {
      doEnable( true );
    } else if( src == this.btnDisable ) {
      doDisable();
    } else if( src instanceof AbstractButton ) {
      updFieldsEnabled();
    } else {
      rv = false;
    }
    return rv;
  }


  protected abstract void doDisable();
  protected abstract void doEnable( boolean interactive );


  protected void formatChanged( AudioIO audioIO, String formatText )
  {
    // leer
  }


  protected int getFrameRate( Properties props, String prefix )
  {
    return EmuUtil.getIntProperty( props, prefix + PROP_FRAME_RATE, 0 );
  }


  protected String getMixerName( Properties props, String prefix )
  {
    return props != null ?
		props.getProperty( prefix + PROP_MIXER_NAME )
		: null;
  }


  protected int getSelectedFrameRate()
  {
    return AudioUtil.getSelectedFrameRate( this.comboFrameRate );
  }


  protected Mixer.Info getSelectedMixerInfo() throws IOException
  {
    return AudioUtil.getSelectedMixerInfo( this.comboMixer );
  }


  protected abstract String getSettingsPrefix();


  public void putSettingsTo( Properties props )
  {
    if( props != null ) {
      String prefix = getSettingsPrefix();
      EmuUtil.setProperty(
		props,
		prefix + PROP_MIXER_NAME,
		this.comboMixer.getSelectedItem() );
      EmuUtil.setProperty(
		props,
		prefix + PROP_FRAME_RATE,
		getSelectedFrameRate() );
    }
  }


  protected void setSelectedFrameRate( int frameRate )
  {
    this.comboFrameRate.setSelectedItem( Integer.valueOf( frameRate ) );
  }


  protected void setSelectedMixerByName( String mixerName )
  {
    EmuUtil.setSelectedItem( this.comboMixer, mixerName );
  }


  public abstract void updFieldsEnabled();


	/* --- ActionListener --- */

  @Override
  public void actionPerformed( ActionEvent e )
  {
    doAction( e );
  }


	/* --- AudioIOObserver --- */

  @Override
  public void fireFinished( final AudioIO audioIO, final String errorMsg )
  {
    EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    audioFinished( audioIO, errorMsg );
		  }
		} );
  }


  @Override
  public void fireFormatChanged(
			final AudioIO audioIO,
			final String  formatText )
  {
    EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    formatChanged( audioIO, formatText );
		  }
		} );
  }


  @Override
  public void fireMonitorFailed(
			final AudioIn audioIn,
			final String  errorMsg )
  {
    EmuUtil.fireShowErrorDlg( this, errorMsg, null );
  }


  @Override
  public void fireProgressUpdate( AudioInFile audioInFile )
  {
    // leer
  }


  @Override
  public void fireRecordingStatusChanged( AudioOut audioOut )
  {
    EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    updFieldsEnabled();
		  }
		} );
  }


  @Override
  public void setVolumeLimits( int minLimit, int maxLimit )
  {
    if( this.volumeBar != null )
      this.volumeBar.setVolumeLimits( minLimit, maxLimit );
  }


  @Override
  public void updVolume( int value )
  {
    if( this.volumeBar != null )
      this.volumeBar.updVolume( value );
  }


	/* --- private Methoden --- */

  private void showErrorDlg( String errMsg )
  {
    if( errMsg != null )
      BaseDlg.showErrorDlg( this, errMsg );
  }
}
