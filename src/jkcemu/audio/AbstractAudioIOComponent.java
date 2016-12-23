/*
 * (c) 2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Komponente fuer die Emulation des Lautsprechers
 * bzw. Sound-Generatorausgangs
 */

package jkcemu.audio;

import java.io.IOException;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.*;
import javax.sound.sampled.Mixer;
import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import jkcemu.base.BaseDlg;
import jkcemu.base.EmuThread;


public abstract class AbstractAudioIOComponent
			extends JPanel
			implements ActionListener, AudioIOObserver
{
  protected AudioFrm          audioFrm;
  protected EmuThread         emuThread;
  protected JLabel            labelFrameRate;
  protected JLabel            labelMixer;
  protected JLabel            labelFormat;
  protected JComboBox<Object> comboFrameRate;
  protected JComboBox<String> comboMixer;
  protected JTextField        fldFormat;
  protected VolumeBar         volumeBar;
  protected JButton           btnEnable;
  protected JButton           btnDisable;


  public AbstractAudioIOComponent( AudioFrm audioFrm, EmuThread emuThread )
  {
    this.audioFrm  = audioFrm;
    this.emuThread = emuThread;
  }


  protected boolean doAction( ActionEvent e )
  {
    boolean rv  = true;
    Object  src = e.getSource();
    if( src == this.btnEnable ) {
      doEnable();
    } else if( src == this.btnDisable ) {
      doDisable();
    } else if( src instanceof AbstractButton ) {
      updFieldsEnabled();
    } else {
      rv = false;
    }
    return rv;
  }


  protected abstract void doEnable();
  protected abstract void doDisable();


  protected int getSelectedFrameRate()
  {
    return this.audioFrm.getSelectedFrameRate( this.comboFrameRate );
  }


  protected Mixer getSelectedMixer()
  {
    return this.audioFrm.getSelectedMixer( this.comboMixer );
  }


  protected abstract void updFieldsEnabled();


	/* --- ActionListener --- */

  @Override
  public void actionPerformed( ActionEvent e )
  {
    doAction( e );
  }


	/* --- AudioIOObserver --- */

  @Override
  public void fireFinished( final AudioIO audioIO, final String errMsg )
  {
    EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    doDisable();
		    if( errMsg != null ) {
		      showErrorDlg( errMsg );
		    }
		  }
		} );
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
