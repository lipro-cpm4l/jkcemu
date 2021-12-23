/*
 * (c) 2011-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Komponente fuer die Einstellungen einer RAM-Floppy
 */

package jkcemu.settings;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.dnd.DropTargetContext;
import java.awt.dnd.DropTargetDragEvent;
import java.io.File;
import java.util.EventObject;
import java.util.Properties;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import jkcemu.Main;
import jkcemu.base.EmuUtil;
import jkcemu.base.GUIFactory;
import jkcemu.base.RAMFloppy;
import jkcemu.base.UserInputException;
import jkcemu.file.FileNameFld;
import jkcemu.file.FileUtil;


public class RAMFloppySettingsFld extends AbstractSettingsFld
{
  private JCheckBox         cbRF;
  private JLabel            labelRF;
  private JLabel            labelFile;
  private JComboBox<String> comboSize;
  private FileNameFld       fileNameFld;
  private JButton           btnSelect;
  private JButton           btnRemove;


  public RAMFloppySettingsFld(
			SettingsFrm      settingsFrm,
			String           propPrefix,
			String           labelText,
			RAMFloppy.RFType rfType )
  {
    super( settingsFrm, propPrefix );
    this.propPrefix = propPrefix;

    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );

    this.cbRF      = null;
    this.labelRF   = null;
    this.comboSize = null;
    if( rfType == RAMFloppy.RFType.MP_3_1988 ) {
      this.cbRF     = GUIFactory.createCheckBox( labelText );
      gbc.gridwidth = GridBagConstraints.REMAINDER;
      add( this.cbRF, gbc );
    } else if( rfType == RAMFloppy.RFType.ADW ) {
      this.labelRF = GUIFactory.createLabel( labelText + ":" );
      add( this.labelRF, gbc );

      this.comboSize = GUIFactory.createComboBox();
      this.comboSize.setEditable( false );
      this.comboSize.addItem( "Nicht emulieren" );
      this.comboSize.addItem( "128 KByte" );
      this.comboSize.addItem( "512 KByte" );
      this.comboSize.addItem( "2 MByte" );
      gbc.gridwidth = GridBagConstraints.REMAINDER;
      gbc.gridx++;
      add( this.comboSize, gbc );
    }

    this.labelFile  = GUIFactory.createLabel(
				"Automatisch laden (optional):" );
    gbc.insets.left = 50;
    gbc.gridwidth   = GridBagConstraints.REMAINDER;
    gbc.gridx       = 0;
    gbc.gridy++;
    add( this.labelFile, gbc );

    this.fileNameFld  = new FileNameFld();
    gbc.fill          = GridBagConstraints.HORIZONTAL;
    gbc.weightx       = 1.0;
    gbc.insets.top    = 0;
    gbc.insets.bottom = 5;
    gbc.gridwidth     = 2;
    gbc.gridy++;
    add( this.fileNameFld, gbc );

    this.btnSelect = GUIFactory.createRelImageResourceButton(
					this,
					"file/open.png",
					"Abbilddatei ausw\u00E4hlen" );
    this.btnSelect.addActionListener( this );
    gbc.fill        = GridBagConstraints.NONE;
    gbc.weightx     = 0.0;
    gbc.insets.left = 0;
    gbc.gridwidth   = 1;
    gbc.gridx += 2;
    add( this.btnSelect, gbc );

    this.btnRemove = GUIFactory.createRelImageResourceButton(
					this,
					"file/delete.png",
					"Abbilddatei entfernen" );
    this.btnRemove.addActionListener( this );
    gbc.gridx++;
    add( this.btnRemove, gbc );

    if( this.cbRF != null ) {
      this.cbRF.addActionListener( this );
    }
    if( this.comboSize != null ) {
      this.comboSize.addActionListener( this );
    }
    enableFileDrop( this.fileNameFld );
    updFieldsEnabled();
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void applyInput(
		Properties props,
		boolean    selected ) throws UserInputException
  {
    if( props != null ) {
      props.remove( this.propPrefix + RAMFloppy.PROP_ENABLED );
      props.remove( this.propPrefix + RAMFloppy.PROP_KBYTE );
      if( this.comboSize != null ) {
	int kb = 0;
	switch( this.comboSize.getSelectedIndex() ) {
	  case 1:
	    kb = 128;
	    break;
	  case 2:
	    kb = 512;
	    break;
	  case 3:
	    kb = 2048;
	    break;
	}
	EmuUtil.setProperty(
			props,
			this.propPrefix + RAMFloppy.PROP_KBYTE,
			kb );
      } else if( this.cbRF != null ) {
	EmuUtil.setProperty(
			props,
			this.propPrefix + RAMFloppy.PROP_ENABLED,
			Boolean.toString( this.cbRF.isSelected() ) );
      }

      File file = this.fileNameFld.getFile();
      props.setProperty(
		this.propPrefix + RAMFloppy.PROP_FILE,
		file != null ? file.getPath() : "" );
    }
  }


  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv  = false;
    Object  src = e.getSource();
    if( src != null ) {
      if( (src == this.cbRF) || (src == this.comboSize) ) {
	updFieldsEnabled();
	fireDataChanged();
	rv = true;
      }
      else if( src == this.btnSelect ) {
	File file = selectFile(
			"RAM-Floppy-Abbilddatei ausw\u00E4hlen",
			Main.FILE_GROUP_RF,
			this.fileNameFld.getFile(),
			FileUtil.getBinaryFileFilter() );
	if( file != null ) {
	  this.fileNameFld.setFile( file );
	  this.btnRemove.setEnabled( this.labelFile.isEnabled() );
	  fireDataChanged();
	}
	rv = true;
      }
      else if( src == this.btnRemove ) {
        File oldFile = this.fileNameFld.getFile();
        if( oldFile != null ) {
          this.fileNameFld.setFileName( null );
          this.btnRemove.setEnabled( false );
          fireDataChanged();
        }
	rv = true;
      }
    }
    return rv;
  }


  @Override
  public void dragEnter( DropTargetDragEvent e )
  {
    boolean           rejected = false;
    DropTargetContext context  = e.getDropTargetContext();
    if( context != null ) {
      if( context.getComponent() == this.fileNameFld ) {
	if( !this.fileNameFld.isEnabled() ) {
	  e.rejectDrag();
	  rejected = true;
	}
      }
    }
    if( !rejected ) {
      super.dragEnter( e );
    }
  }


  @Override
  protected void fileDropped( Component c, File file )
  {
    if( file != null ) {
      this.fileNameFld.setFile( file );
      this.btnRemove.setEnabled( this.labelFile.isEnabled() );
      Main.setLastFile( file, Main.FILE_GROUP_RF );
      fireDataChanged();
    }
  }


  @Override
  public void setEnabled( boolean state )
  {
    super.setEnabled( state );
    if( this.cbRF != null ) {
      this.cbRF.setEnabled( state );
    } else if( this.labelRF != null ) {
      this.labelRF.setEnabled( state );
    }
    updFieldsEnabled();
  }


  @Override
  public void updFields( Properties props )
  {
    if( this.cbRF != null ) {
      this.cbRF.setSelected(
		EmuUtil.getBooleanProperty(
				props,
				this.propPrefix + RAMFloppy.PROP_ENABLED,
				false ) );
    } else if( this.comboSize != null ) {
      int    idx = 0;
      String s   = EmuUtil.getProperty(
				props,
				this.propPrefix + RAMFloppy.PROP_KBYTE );
      if( s != null ) {
	if( !s.isEmpty() ) {
	  try {
	    switch( Integer.valueOf( s ) ) {
	      case 128:
		idx = 1;
		break;
	      case 512:
		idx = 2;
		break;
	      case 2048:
		idx = 3;
		break;
	    }
	  }
	  catch( NumberFormatException ex ) {}
	}
      }
      if( (idx >= 0) && (idx < this.comboSize.getItemCount()) ) {
	try {
	  this.comboSize.setSelectedIndex( idx );
	}
	catch( IllegalArgumentException ex ) {}
      }
    }
    this.fileNameFld.setFileName(
		EmuUtil.getProperty(
			props,
			this.propPrefix + RAMFloppy.PROP_FILE ) );
    updFieldsEnabled();
  }


	/* --- private Methoden --- */

  private void updFieldsEnabled()
  {
    boolean state = false;
    if( this.cbRF != null ) {
      state = this.cbRF.isSelected();
    } else if( (this.labelRF != null) && (this.comboSize != null) ) {
      if( this.labelRF.isEnabled()
	  && (this.comboSize.getSelectedIndex() > 0) )
      {
	state = true;
      }
    }
    this.labelFile.setEnabled( state );
    this.fileNameFld.setEnabled( state );
    this.btnSelect.setEnabled( state );
    this.btnRemove.setEnabled(
		state && (this.fileNameFld.getFile() != null) );
  }
}
