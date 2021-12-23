/*
 * (c) 2016-2020 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog mit einem Fortschrittbalken und einem Abbrechen-Button
 */

package jkcemu.base;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.EventObject;
import javax.swing.JButton;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;


public class CancelableProgressDlg extends BaseDlg
{
  public interface Progressable
  {
    public int getProgressMax();
    public int getProgressValue();
  };


  private Progressable      progressable;
  private volatile boolean  cancelled;
  private boolean           notified;
  private javax.swing.Timer timer;
  private JProgressBar      progressBar;
  private JButton           btnCancel;



  public CancelableProgressDlg(
			Window       owner,
			String       title,
			Progressable progressable )
  {
    super( owner, title );
    this.progressable =  progressable;
    this.cancelled    = false;
    this.notified     = false;
    this.timer        = new javax.swing.Timer( 200, this );


    // Fensterinhalt
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.CENTER,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 5, 5 ),
					0, 0 );

    this.progressBar = GUIFactory.createProgressBar(
				SwingConstants.HORIZONTAL,
				0,
				progressable.getProgressMax() );
    this.progressBar.setPreferredSize( new Dimension( 200, 14 ) );
    add( this.progressBar, gbc );


    // Abbrechen-Button
    this.btnCancel = GUIFactory.createButtonCancel();
    gbc.insets.top = 10;
    gbc.gridy++;
    add( this.btnCancel, gbc );


    // Fenstergroesse und -position
    pack();
    setParentCentered();
    setResizable( false );
  }


  public void fireProgressFinished()
  {
    EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    progressFinished();
		  }
		} );
  }


  public boolean wasCancelled()
  {
    return this.cancelled;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void addNotify()
  {
    super.addNotify();
    if( !this.notified ) {
      this.notified = true;
      this.btnCancel.addActionListener( this );
    }
  }


  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv  = false;
    Object  src = e.getSource();
    if( src == this.timer ) {
      rv = true;
      updProgressBar();
    } else if( src == this.btnCancel ) {
      rv = true;
      doClose();
    }
    return rv;
  }


  @Override
  public boolean doClose()
  {
    boolean rv = super.doClose();
    if( rv ) {
      this.cancelled = true;
    }
    return rv;
  }


  @Override
  public void removeNotify()
  {
    super.removeNotify();
    if( this.notified ) {
      this.notified = false;
      this.btnCancel.removeActionListener( this );
    }
  }


  @Override
  public void setVisible( boolean state )
  {
    if( state ) {
      this.timer.start();
    }
    super.setVisible( state );
  }


	/* --- private Methoden --- */

  private void updProgressBar()
  {
    int value = this.progressable.getProgressValue();
    if( value < 0 ) {
      value = 0;
    } else if( value > this.progressBar.getMaximum() ) {
      value = this.progressBar.getMaximum();
    }
    this.progressBar.setValue( value );
  }


  private void progressFinished()
  {
    this.timer.stop();
    boolean cancelled = this.cancelled;
    doClose();					// setzt cancelled
    this.cancelled = cancelled;
  }
}
