/*
 * (c) 2010-2011 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Componente fuer eine RAM-Floppy
 */

package jkcemu.base;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.*;
import javax.swing.*;
import jkcemu.Main;


public class RAMFloppyFld extends JComponent implements ActionListener
{
  private Frame        owner;
  private RAMFloppy    ramFloppy;
  private JLabel       labelUsedSize;
  private JButton      btnLoad;
  private JButton      btnSave;
  private JButton      btnClear;
  private JComponent   ledFld;
  private boolean      ledState;
  private volatile int accessCounter;


  public RAMFloppyFld( Frame owner, RAMFloppy ramFloppy )
  {
    this.owner         = owner;
    this.ramFloppy     = ramFloppy;
    this.ledState      = false;
    this.accessCounter = 0;

    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 5, 5 ),
						0, 0 );

    add( new JLabel( "Gr\u00F6\u00DFe:" ), gbc );
    gbc.gridy++;
    add( new JLabel( "Davon beschrieben:" ), gbc );

    gbc.gridwidth = 2;
    gbc.gridy     = 0;
    gbc.gridx++;
    add(
	new JLabel( EmuUtil.formatSize(
				this.ramFloppy.getSize(),
				false,
				false ) ),
	gbc );

    this.labelUsedSize = new JLabel();
    gbc.gridwidth = 1;
    gbc.gridy++;
    add( this.labelUsedSize, gbc );

    Dimension ledSize = new Dimension( 30, 15 );
    this.ledFld = new JPanel()
	{
	  public void paintComponent( Graphics g )
	  {
	    paintLED( g, getWidth(), getHeight() );
	  }
	};
    this.ledFld.setBorder( BorderFactory.createLoweredBevelBorder() );
    this.ledFld.setOpaque( true );
    this.ledFld.setPreferredSize( ledSize );
    this.ledFld.setMinimumSize( ledSize );
    this.ledFld.setMaximumSize( ledSize );
    gbc.anchor = GridBagConstraints.EAST;
    gbc.gridx++;
    add( this.ledFld, gbc );

    JPanel panelBtn = new JPanel( new GridLayout( 1, 3, 5, 5 ) );
    gbc.anchor      = GridBagConstraints.CENTER;
    gbc.gridwidth   = GridBagConstraints.REMAINDER;
    gbc.gridx = 0;
    gbc.gridy++;
    add( panelBtn, gbc );

    this.btnLoad = new JButton( "Laden..." );
    this.btnLoad.addActionListener( this );
    panelBtn.add( this.btnLoad );

    this.btnSave = new JButton( "Speichern..." );
    this.btnSave.addActionListener( this );
    panelBtn.add( this.btnSave );

    this.btnClear = new JButton( "L\u00F6schen" );
    this.btnClear.addActionListener( this );
    panelBtn.add( this.btnClear );

    if( this.ramFloppy != null ) {
      this.ramFloppy.setRAMFloppyFld( this );
    } else {
      this.btnClear.setEnabled( false );
      this.btnSave.setEnabled( false );
    }
    updFields();
  }


  public void checkLEDState()
  {
    boolean state = false;
    if( this.accessCounter > 0 ) {
      --this.accessCounter;
      state = true;
    }
    if( state != this.ledState ) {
      this.ledState = state;
      this.ledFld.repaint();
    }
  }


  public void fireRAMFloppyAccess()
  {
    this.accessCounter = 5;
  }


  public void fireRAMFloppyChanged()
  {
    EventQueue.invokeLater(
		new Runnable()
		{
		  public void run()
		  {
		    updFields();
		  }
		} );
  }


	/* --- ActionListener --- */

  @Override
  public void actionPerformed( ActionEvent e )
  {
    Object src = e.getSource();
    if( src != null ) {
      if( src == this.btnClear ) {
	doClear();
      }
      else if( src == this.btnLoad ) {
	doLoad();
      }
      else if( src == this.btnSave ) {
	doSave();
      }
    }
  }


	/* --- Aktionen --- */

  private void doClear()
  {
    if( this.ramFloppy != null ) {
      if( BasicDlg.showYesNoDlg(
		this,
		"M\u00F6chten Sie die RAM-Floppy l\u00F6schen?\n"
			+ "Dabei gehen alle in ihr gespeicherten Daten"
			+ " verloren!" ) )
      {
	this.ramFloppy.clear();
	updFields();
      }
    }
  }


  private void doLoad()
  {
    if( this.ramFloppy != null ) {
      File file = this.ramFloppy.getFile();
      file      = EmuUtil.showFileOpenDlg(
			this.owner,
			"RAM-Floppy laden",
			file != null ?
				file
				: Main.getLastPathFile( "ramfloppy" ) );
      if( file != null ) {
	try {
	  this.ramFloppy.load( file );
	  Main.setLastFile( file, "ramfloppy" );
	}
	catch( IOException ex ) {
	  BasicDlg.showErrorDlg(
		this,
		"Die RAM-Floppy kann nicht geladen werden.\n\n"
						+ ex.getMessage() );
	}
      }
    }
  }


  private void doSave()
  {
    if( this.ramFloppy != null ) {
      File file = this.ramFloppy.getFile();
      file      = EmuUtil.showFileSaveDlg(
			this.owner,
			"RAM-Floppy speichern",
			file != null ?
				file
				: Main.getLastPathFile( "ramfloppy" ) );
      if( file != null ) {
	try {
	  this.ramFloppy.save( file );
	  Main.setLastFile( file, "ramfloppy" );
	}
	catch( IOException ex ) {
	  BasicDlg.showErrorDlg(
		this,
		"RAM-Floppy kann nicht gespeichert werden.\n\n"
						+ ex.getMessage() );
	}
      }
    }
  }


	/* --- private Methoden --- */

  private void paintLED( Graphics g, int w, int h )
  {
    if( (w > 0) && (h > 0) ) {
      g.setColor( this.ledState ? Color.red : Color.gray );
      g.fillRect( 0, 0, w, h );
    }
  }


  private void updFields()
  {
    if( this.ramFloppy != null ) {
      int usedSize = this.ramFloppy.getUsedSize();
      this.labelUsedSize.setText(
		EmuUtil.formatSize( usedSize, false, false ) );
      this.btnClear.setEnabled( usedSize > 0 );
      this.btnSave.setEnabled( this.ramFloppy.hasDataChanged() );
    }
  }
}
