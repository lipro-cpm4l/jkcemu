/*
 * (c) 2013-2016 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Komponente zur Anzeige der Lautstaerke
 */

package jkcemu.audio;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.*;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;


public class VolumeBar extends JPanel
{
  private static final int VOLUME_BAR_MAX = 1000;

  private javax.swing.Timer timer;
  private JProgressBar      progressBar;
  private int               minLimit;
  private int               maxLimit;
  private int               minValue;
  private int               maxValue;


  public VolumeBar( int orientation )
  {
    this.minLimit  = 0;
    this.maxLimit  = 255;
    this.minValue  = this.maxLimit;
    this.maxValue  = this.minLimit;
    this.timer     = new javax.swing.Timer(
			100,
			new ActionListener()
			{
			  public void actionPerformed( ActionEvent e )
			  {
			    updVolumeBar();
			  }
			} );

    // Layout
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 1.0,
					GridBagConstraints.CENTER,
					GridBagConstraints.VERTICAL,
					new Insets( 5, 5, 5, 5 ),
					0, 0 );
    if( orientation == SwingConstants.HORIZONTAL ) {
      gbc.weightx = 1.0;
      gbc.weighty = 0.0;
      gbc.fill    = GridBagConstraints.HORIZONTAL;
    }
    this.progressBar = new JProgressBar( orientation, 0, VOLUME_BAR_MAX );
    add( this.progressBar, gbc );
  }


  public synchronized void setVolumeLimits( int minLimit, int maxLimit )
  {
    if( minLimit < maxLimit ) {
      this.minLimit = minLimit;
      this.maxLimit = maxLimit;
      this.maxValue = maxLimit;
      this.minValue = minLimit;
      this.progressBar.setValue( 0 );
    }
  }


  public void setVolumeBarState( boolean state )
  {
    if( state ) {
      this.timer.start();
    } else {
      this.progressBar.setValue( 0 );
      this.timer.stop();
    }
    this.progressBar.setEnabled( state );
  }


  public synchronized void updVolume( int value )
  {
    if( value < this.minValue ) {
      this.minValue = value;
    }
    if( value > this.maxValue ) {
      this.maxValue = value;
    }
  }


	/* --- private Methoden --- */

  private void updVolumeBar()
  {
    int barValue = 0;
    int volume   = 0;
    synchronized( this ) {
      volume        = this.maxValue - this.minValue;
      this.minValue = this.maxLimit;
      this.maxValue = this.minLimit;
    }
    /*
     * Logarithmische Pegelanzeige:
     *   Der Pegel wird auf den Bereich 0 bis 100 normiert,
     *   aus dem Wert plus eins der Logarithmus gebildet
     *   und anschliessend auf den Bereich der Anzeige skaliert.
     */
    double v = (double) volume
		/ (double) (this.maxLimit - this.minLimit)
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
    this.progressBar.setValue( barValue );
  }
}
