/*
 * (c) 2011 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Komponente fuer die Einstellungen einer RAM-Floppy
 */

package jkcemu.base;

import java.awt.*;
import java.awt.dnd.*;
import java.io.File;
import java.lang.*;
import java.util.*;
import javax.swing.*;
import jkcemu.Main;
import jkcemu.base.*;


public class RAMFloppySettingsFld extends AbstractSettingsFld
{
  private JCheckBox   btnRF;
  private JLabel      labelRF;
  private JLabel      labelFile;
  private JComboBox   comboSize;
  private FileNameFld fileNameFld;
  private JButton     btnSelect;
  private JButton     btnRemove;


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

    this.btnRF     = null;
    this.labelRF   = null;
    this.comboSize = null;
    if( rfType == RAMFloppy.RFType.MP_3_1988 ) {
      this.btnRF    = new JCheckBox( labelText );
      gbc.gridwidth = GridBagConstraints.REMAINDER;
      add( this.btnRF, gbc );
    } else if( rfType == RAMFloppy.RFType.ADW ) {
      this.labelRF = new JLabel( labelText + ":" );
      add( this.labelRF, gbc );

      this.comboSize = new JComboBox();
      this.comboSize.setEditable( false );
      this.comboSize.addItem( "Nicht emulieren" );
      this.comboSize.addItem( "128 KByte" );
      this.comboSize.addItem( "512 KByte" );
      this.comboSize.addItem( "2 MByte" );
      gbc.gridwidth = GridBagConstraints.REMAINDER;
      gbc.gridx++;
      add( this.comboSize, gbc );
    }

    this.labelFile  = new JLabel( "Automatisch laden (optional):" );
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

    this.btnSelect = createImageButton(
				"/images/file/open.png",
				"Abbilddatei ausw\u00E4hlen" );
    gbc.fill        = GridBagConstraints.NONE;
    gbc.weightx     = 0.0;
    gbc.insets.left = 0;
    gbc.gridwidth   = 1;
    gbc.gridx += 2;
    add( this.btnSelect, gbc );

    this.btnRemove = createImageButton(
				"/images/file/delete.png",
				"Abbilddatei entfernen" );
    gbc.gridx++;
    add( this.btnRemove, gbc );

    if( this.btnRF != null ) {
      this.btnRF.addActionListener( this );
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
      props.remove( this.propPrefix + "enabled" );
      props.remove( this.propPrefix + "kbyte" );
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
	EmuUtil.setProperty( props, this.propPrefix + "kbyte", kb );
      } else if( this.btnRF != null ) {
	EmuUtil.setProperty(
			props,
			this.propPrefix + "enabled",
			Boolean.toString( this.btnRF.isSelected() ) );
      }

      File file = this.fileNameFld.getFile();
      props.setProperty(
		this.propPrefix + "file",
		file != null ? file.getPath() : "" );
    }
  }


  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv  = false;
    Object  src = e.getSource();
    if( src != null ) {
      if( (src == this.btnRF) || (src == this.comboSize) ) {
	updFieldsEnabled();
	fireDataChanged();
	rv = true;
      }
      else if( src == this.btnSelect ) {
	File file = selectFile(
			"RAM-Floppy-Abbilddatei ausw\u00E4hlen",
			"ramfloppy",
			this.fileNameFld.getFile(),
			EmuUtil.getBinaryFileFilter() );
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
  protected boolean fileDropped( Component c, File file )
  {
    boolean rv = false;
    if( file != null ) {
      this.fileNameFld.setFile( file );
      this.btnRemove.setEnabled( this.labelFile.isEnabled() );
      Main.setLastFile( file, "ramfloppy" );
      fireDataChanged();
      rv = true;
    }
    return rv;
  }


  @Override
  public void setEnabled( boolean state )
  {
    super.setEnabled( state );
    if( this.btnRF != null ) {
      this.btnRF.setEnabled( state );
    } else if( this.labelRF != null ) {
      this.labelRF.setEnabled( state );
    }
    updFieldsEnabled();
  }


  @Override
  public void updFields( Properties props )
  {
    if( this.btnRF != null ) {
      this.btnRF.setSelected(
		EmuUtil.getBooleanProperty(
				props,
				this.propPrefix + "enabled",
				false ) );
    } else if( this.comboSize != null ) {
      int    idx = 0;
      String s   = EmuUtil.getProperty( props, this.propPrefix + "kbyte" );
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
		EmuUtil.getProperty( props, this.propPrefix + "file" ) );
    updFieldsEnabled();
  }


	/* --- private Methoden --- */

  private void updFieldsEnabled()
  {
    boolean state = false;
    if( this.btnRF != null ) {
      state = this.btnRF.isSelected();
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

