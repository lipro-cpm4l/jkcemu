/*
 * (c) 2010-2016 Jens Mueller
 * (c) 2014-2017 Stephan Linz
 *
 * Kleincomputer-Emulator
 *
 * Komponente fuer die Auswahl einer Datei
 * Die Komponente enthaelt ein Label, ein Textfeld mit dem Dateinamen
 * sowie zwei Knoepfen zum Auswaehlen und entfernen der Datei
 */

package jkcemu.base;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.lang.*;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.Properties;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import jkcemu.Main;


public class RAMFileSettingsFld extends AbstractSettingsFld
{
  private java.util.List<ChangeListener> changeListeners;
  private JLabel                         label;
  private FileNameFld                    fileNameFld;
  private JButton                        btnSelect;
  private JButton                        btnRemove;


  public RAMFileSettingsFld(
			SettingsFrm settingsFrm,
                        String      propPrefix,
			String      labelText )
  {
    super( settingsFrm, propPrefix );
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					GridBagConstraints.REMAINDER, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 0, 0, 0, 0 ),
					0, 0 );

    this.label = new JLabel( labelText );
    add( this.label, gbc );

    this.fileNameFld = new FileNameFld();
    gbc.fill         = GridBagConstraints.HORIZONTAL;
    gbc.weightx      = 1.0;
    gbc.gridwidth    = 1;
    gbc.gridy++;
    add( this.fileNameFld, gbc );

    this.btnSelect = createImageButton(
				"/images/file/open.png",
				"RAM-Datei ausw\u00E4hlen" );
    gbc.fill        = GridBagConstraints.NONE;
    gbc.weightx     = 0.0;
    gbc.insets.left = 5;
    gbc.gridx++;
    add( this.btnSelect, gbc );

    this.btnRemove = createImageButton(
				"/images/file/delete.png",
				"RAM-Datei entfernen" );
    this.btnRemove.setEnabled( false );
    gbc.gridx++;
    add( this.btnRemove, gbc );

    enableFileDrop( this.fileNameFld );
  }


  public synchronized void addChangeListener( ChangeListener listener )
  {
    if( this.changeListeners == null ) {
      this.changeListeners = new ArrayList<>();
    }
    this.changeListeners.add( listener );
  }


  public File getFile()
  {
    return this.fileNameFld.getFile();
  }


  public synchronized void removeChangeListener( ChangeListener listener )
  {
    if( this.changeListeners != null )
      this.changeListeners.remove( listener );
  }


  public void setFile( File file )
  {
    if( this.fileNameFld.setFile( file ) ) {
      this.btnRemove.setEnabled( (file != null) && this.label.isEnabled() );
      if( file != null ) {
	Main.setLastFile( file, Main.FILE_GROUP_RAM );
      }
      fireDataChanged();
    }
  }


  public void setFileName( String fileName )
  {
    File file = null;
    if( fileName != null ) {
      if( !fileName.isEmpty() ) {
	file = new File( fileName );
      }
    }
    setFile( file );
  }


  public void setLabelText( String text )
  {
    this.label.setText( text );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void applyInput(
                Properties props,
                boolean    selected ) throws UserInputException
  {
    File file = this.fileNameFld.getFile();
    EmuUtil.setProperty(
		props,
		this.propPrefix + EmuSys.PROP_FILE,
                file != null ? file.getPath() : null );
  }


  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv  = false;
    Object  src = e.getSource();
    if( src == this.btnSelect ) {
      File file = selectFile(
			"RAM-Datei ausw\u00E4hlen",
			Main.FILE_GROUP_RAM,
			this.fileNameFld.getFile(),
			EmuUtil.getRAMFileFilter() );
      if( file != null ) {
	setFile( file );
      }
      rv = true;
    }
    else if( src == this.btnRemove ) {
      setFile( null );
      this.btnRemove.setEnabled( false );
      rv = true;
    }
    return rv;
  }


  @Override
  protected boolean fileDropped( Component c, File file )
  {
    boolean rv = false;
    if( file != null ) {
      setFile( file );
      rv = true;
    }
    return rv;
  }


  @Override
  protected void fireDataChanged()
  {
    super.fireDataChanged();
    synchronized( this ) {
      if( this.changeListeners != null ) {
	for( ChangeListener listener : this.changeListeners ) {
	  listener.stateChanged( new ChangeEvent( this ) );
	}
      }
    }
  }


  @Override
  public void setEnabled( boolean state )
  {
    super.setEnabled( state );
    this.label.setEnabled( state );
    this.fileNameFld.setEnabled( state );
    this.btnSelect.setEnabled( state );
    this.btnRemove.setEnabled(
			state && (this.fileNameFld.getFile() != null) );
  }


  @Override
  public void updFields( Properties props )
  {
    this.fileNameFld.setFileName(
	EmuUtil.getProperty( props, this.propPrefix + EmuSys.PROP_FILE ) );
    this.btnRemove.setEnabled(
	this.label.isEnabled() && (this.fileNameFld.getFile() != null) );
  }
}
