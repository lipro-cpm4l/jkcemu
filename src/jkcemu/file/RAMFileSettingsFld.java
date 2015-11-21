/*
 * (c) 2010-2020 Jens Mueller
 * (c) 2014-2021 Stephan Linz
 *
 * Kleincomputer-Emulator
 *
 * Komponente fuer die Auswahl einer Datei
 * Die Komponente enthaelt ein Label, ein Textfeld mit dem Dateinamen
 * sowie zwei Knoepfen zum Auswaehlen und entfernen der Datei
 */

package jkcemu.file;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.Properties;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import jkcemu.Main;
import jkcemu.base.EmuSys;
import jkcemu.base.EmuUtil;
import jkcemu.base.GUIFactory;
import jkcemu.base.UserInputException;
import jkcemu.file.FileNameFld;
import jkcemu.file.FileUtil;
import jkcemu.settings.AbstractSettingsFld;
import jkcemu.settings.SettingsFrm;


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

    this.label = GUIFactory.createLabel( labelText );
    add( this.label, gbc );

    this.fileNameFld = new FileNameFld();
    gbc.fill         = GridBagConstraints.HORIZONTAL;
    gbc.weightx      = 1.0;
    gbc.gridwidth    = 1;
    gbc.gridy++;
    add( this.fileNameFld, gbc );

    this.btnSelect = GUIFactory.createRelImageResourceButton(
					this,
					"file/open.png",
					EmuUtil.TEXT_SELECT_RAM_FILE );
    this.btnSelect.addActionListener( this );
    gbc.fill        = GridBagConstraints.NONE;
    gbc.weightx     = 0.0;
    gbc.insets.left = 5;
    gbc.gridx++;
    add( this.btnSelect, gbc );

    this.btnRemove = GUIFactory.createRelImageResourceButton(
					this,
					"file/delete.png",
					EmuUtil.TEXT_REMOVE_RAM_FILE );
    this.btnRemove.setEnabled( false );
    this.btnRemove.addActionListener( this );
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
			EmuUtil.TEXT_SELECT_RAM_FILE,
			Main.FILE_GROUP_RAM,
			this.fileNameFld.getFile(),
			FileUtil.getRAMFileFilter() );
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
  protected void fileDropped( Component c, File file )
  {
    if( file != null )
      setFile( file );
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
