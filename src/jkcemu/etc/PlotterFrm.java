/*
 * (c) 2011 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Fenster fuer den Plotter
 */

package jkcemu.etc;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.lang.*;
import java.util.*;
import javax.swing.*;
import jkcemu.Main;
import jkcemu.base.*;
import jkcemu.image.AbstractImageFrm;
import jkcemu.print.PrintUtil;


public class PlotterFrm extends AbstractImageFrm
{
  private Plotter              plotter;
  private javax.swing.Timer    refreshTimer;
  private volatile boolean     dirty;
  private JMenuItem            mnuNewPage;
  private JMenuItem            mnuPrint;
  private JMenuItem            mnuSaveAs;
  private JMenuItem            mnuCopy;
  private JRadioButtonMenuItem mnuPenThk1;
  private JRadioButtonMenuItem mnuPenThk2;
  private JRadioButtonMenuItem mnuPenThk3;
  private JRadioButtonMenuItem mnuPenThk4;
  private JRadioButtonMenuItem mnuPenThk5;
  private JCheckBoxMenuItem    mnuConfirmNewPage;
  private JButton              btnNewPage;
  private JButton              btnPrint;
  private JButton              btnSaveAs;
  private JButton              btnRotateLeft;
  private JButton              btnRotateRight;


  public PlotterFrm( ScreenFrm screenFrm, Plotter plotter )
  {
    super( screenFrm );
    this.plotter = plotter;
    this.dirty   = false;
    this.file    = null;
    setTitle( "JKCEMU Plotter" );
    Main.updIcon( this );


    // Menu
    JMenuBar mnuBar = new JMenuBar();
    setJMenuBar( mnuBar );


    // Menu Datei
    JMenu mnuFile = new JMenu( "Datei" );
    mnuFile.setMnemonic( KeyEvent.VK_D );
    mnuBar.add( mnuFile );

    this.mnuNewPage = createJMenuItem( "Neue Seite" );
    mnuFile.add( this.mnuNewPage );

    this.mnuPrint = createJMenuItem( "Drucken..." );
    mnuFile.add( this.mnuPrint );
    mnuFile.addSeparator();

    this.mnuSaveAs = createJMenuItem( "Speichern unter..." );
    mnuFile.add( this.mnuSaveAs );
    mnuFile.addSeparator();

    mnuFile.add( createJMenuItem( "Schlie\u00DFen", "close" ) );


    // Menu Bearbeiten
    JMenu mnuEdit = new JMenu( "Bearbeiten" );
    mnuEdit.setMnemonic( KeyEvent.VK_B );
    mnuBar.add( mnuEdit );

    this.mnuCopy = createJMenuItem( "Kopieren" );
    mnuEdit.add( this.mnuCopy );


    // Menu Einstellungen
    JMenu mnuSettings = new JMenu( "Einstellungen" );
    mnuEdit.setMnemonic( KeyEvent.VK_E );
    mnuBar.add( mnuSettings );

    JMenu mnuPenThk = new JMenu( "Stiftbreite" );
    mnuSettings.add( mnuPenThk );

    ButtonGroup grpPenThk = new ButtonGroup();

    this.mnuPenThk1 = new JRadioButtonMenuItem( "1 Pixel" );
    this.mnuPenThk1.addActionListener( this );
    grpPenThk.add( this.mnuPenThk1 );
    mnuPenThk.add( this.mnuPenThk1 );

    this.mnuPenThk2 = new JRadioButtonMenuItem( "2 Pixel" );
    this.mnuPenThk2.addActionListener( this );
    grpPenThk.add( this.mnuPenThk2 );
    mnuPenThk.add( this.mnuPenThk2 );

    this.mnuPenThk3 = new JRadioButtonMenuItem( "3 Pixel" );
    this.mnuPenThk3.addActionListener( this );
    grpPenThk.add( this.mnuPenThk3 );
    mnuPenThk.add( this.mnuPenThk3 );

    this.mnuPenThk4 = new JRadioButtonMenuItem( "4 Pixel" );
    this.mnuPenThk4.addActionListener( this );
    grpPenThk.add( this.mnuPenThk4 );
    mnuPenThk.add( this.mnuPenThk4 );

    this.mnuPenThk5 = new JRadioButtonMenuItem( "5 Pixel" );
    this.mnuPenThk5.addActionListener( this );
    grpPenThk.add( this.mnuPenThk5 );
    mnuPenThk.add( this.mnuPenThk5 );

    mnuSettings.add( createJMenuItem( "Stiftfarbe...", "color.pen" ) );
    mnuSettings.add( createJMenuItem( "Papierfarbe...", "color.paper" ) );
    mnuSettings.addSeparator();
    this.mnuConfirmNewPage = new JCheckBoxMenuItem(
			"Best\u00E4tigung f\u00FCr neue Seite",
			true );
    this.mnuConfirmNewPage.addActionListener( this );
    mnuSettings.add( this.mnuConfirmNewPage );


    // Menu Hilfe
    JMenu mnuHelp = new JMenu( "?" );
    mnuBar.add( mnuHelp );
    mnuHelp.add( createJMenuItem( "Hilfe...", "help" ) );


    // Werkzeugleiste
    JPanel panelToolBar = new JPanel(
		new FlowLayout( FlowLayout.LEFT, 0, 0 ) );
    add( panelToolBar, BorderLayout.NORTH );

    JToolBar toolBar = new JToolBar();
    toolBar.setFloatable( false );
    toolBar.setBorderPainted( false );
    toolBar.setOrientation( JToolBar.HORIZONTAL );
    toolBar.setRollover( true );
    panelToolBar.add( toolBar );

    this.btnNewPage = createImageButton(
			"/images/file/new.png", "Neue Seite" );
    toolBar.add( this.btnNewPage );
    toolBar.addSeparator();

    this.btnSaveAs = createImageButton(
			"/images/file/save_as.png", "Speichern unter" );
    toolBar.add( this.btnSaveAs );

    this.btnPrint = createImageButton( "/images/file/print.png", "Drucken" );
    toolBar.add( this.btnPrint );
    toolBar.addSeparator();

    this.btnRotateLeft = createImageButton(
				"/images/edit/rotate_left.png",
				"Nach links drehen" );
    toolBar.add( this.btnRotateLeft );

    this.btnRotateRight = createImageButton(
				"/images/edit/rotate_right.png",
				"Nach rechts drehen" );
    toolBar.add( this.btnRotateRight );
    toolBar.addSeparator();

    toolBar.add( createScaleComboBox( 25, 25, 33, 50, 75, 100 ) );
    doScale();


    // Fenstergroesse
    if( !applySettings( Main.getProperties(), true ) ) {
      pack();
      setScreenCentered();
    }
    setResizable( true );


    // sonstiges
    if( this.plotter != null ) {
      this.plotter.setPlotterFrm( this );
    } else {
      setPenThickness( Plotter.DEFAULT_PEN_THICKNESS );
    }
    updActionBtns();
    this.refreshTimer = new javax.swing.Timer(
				100,
				new ActionListener()
				{
				  public void actionPerformed( ActionEvent e )
				  {
				    repaintImage();
				  }
				} );
  }


  public void setDirty()
  {
    this.dirty = true;
  }


  public void setImage( Image image )
  {
    this.image = image;
    this.imgFld.setImage( image );
  }


  public void setPenThickness( int value )
  {
    switch( value ) {
      case 2:
	this.mnuPenThk2.setSelected( true );
	break;
      case 3:
	this.mnuPenThk3.setSelected( true );
	break;
      case 4:
	this.mnuPenThk4.setSelected( true );
	break;
      case 5:
	this.mnuPenThk5.setSelected( true );
	break;
      default:
	this.mnuPenThk1.setSelected( true );
    }
  }


  public void setPlotter( Plotter plotter )
  {
    if( this.plotter != null ) {
      this.plotter.setPlotterFrm( null );
    }
    this.plotter = plotter;
    if( this.plotter != null ) {
      this.plotter.setPlotterFrm( this );
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public boolean applySettings( Properties props, boolean resizable )
  {
    this.mnuConfirmNewPage.setSelected(
		EmuUtil.parseBooleanProperty(
			props,
			getSettingsPrefix() + ".confirm_new_page",
			true ) );
    return super.applySettings( props, resizable );
  }


  @Override
  public void componentHidden( ComponentEvent e )
  {
    if( e.getComponent() == this ) {
      this.refreshTimer.stop();
    }
  }


  @Override
  public void componentShown( ComponentEvent e )
  {
    if( e.getComponent() == this ) {
      this.refreshTimer.start();
    }
  }


  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv = false;
    if( e != null ) {
      Object src = e.getSource();
      if( src != null ) {
	if( src == this.btnRotateLeft ) {
	  rv = true;
	  doRotateLeft();
	}
	else if( src == this.btnRotateRight ) {
	  rv = true;
	  doRotateRight();
	}
	if( (src == this.mnuNewPage) || (src == this.btnNewPage) ) {
	  rv = true;
	  doNewPage();
	}
	else if( (src == this.mnuPrint) || (src == this.btnPrint) ) {
	  rv = true;
	  if( this.image != null ) {
	    PrintUtil.doPrint( this, this.imgFld, "Plotter" );
	  }
	}
	else if( (src == this.mnuSaveAs) || (src == this.btnSaveAs) ) {
	  rv = true;
	  doSaveAs();
	}
	else if( src == this.mnuCopy ) {
	  rv = true;
	  doImgCopy();
	}
	else if( (src == this.mnuPenThk1)
		 || (src == this.mnuPenThk2)
		 || (src == this.mnuPenThk3)
		 || (src == this.mnuPenThk4)
		 || (src == this.mnuPenThk5) )
	{
	  rv = true;
	  if( this.plotter != null ) {
	    int thk = 1;
	    if( this.mnuPenThk2.isSelected() ) {
	      thk = 2;
	    }
	    else if( this.mnuPenThk3.isSelected() ) {
	      thk = 3;
	    }
	    else if( this.mnuPenThk4.isSelected() ) {
	      thk = 4;
	    }
	    else if( this.mnuPenThk5.isSelected() ) {
	      thk = 5;
	    }
	    this.plotter.setPenThickness( thk );
	    Main.setProperty(
			"jkcemu.plotter.pen.thickness",
			Integer.toString( thk ) );
	  }
	}
	else if( src == this.mnuConfirmNewPage ) {
	  rv = true;
	  Main.setProperty(
		getSettingsPrefix() + ".confirm_new_page",
		Boolean.toString( this.mnuConfirmNewPage.isSelected() ) );
	}
      }
      if( !rv && (e instanceof ActionEvent) ) {
	String cmd = ((ActionEvent) e).getActionCommand();
	if( cmd != null ) {
	  if( cmd.equals( "close" ) ) {
	    rv = true;
	    doClose();
	  }
	  else if( cmd.equals( "color.pen" ) ) {
	    rv = true;
	    doColorPen();
          }
	  else if( cmd.equals( "color.paper" ) ) {
	    rv = true;
	    doColorPaper();
          }
	  else if( cmd.equals( "help" ) ) {
	    rv = true;
	    this.screenFrm.showHelp( "/help/plotter.htm" );
          }
	}
      }
    }
    if( !rv ) {
      rv = super.doAction( e );
    }
    return rv;
  }


	/* --- private Methoden --- */

  private void doColorPaper()
  {
    Plotter plotter = this.plotter;
    if( plotter != null ) {
      Color color = JColorChooser.showDialog(
				this,
				"Auswahl Papierfarbe",
				this.plotter.getPaperColor() );
      if( color != null ) {
	try {
	  plotter.setPaperColor( color );
	}
	catch( UserInputException ex ) {
	  BasicDlg.showInfoDlg( this, ex.getMessage() );
	}
	Main.setProperty(
		"jkcemu.plotter.paper.color",
		String.format( "%08X", color.getRGB() ) );
      }
    }
  }


  private void doColorPen()
  {
    Plotter plotter = this.plotter;
    if( plotter != null ) {
      Color color = JColorChooser.showDialog(
				this,
				"Auswahl Stiftfarbe",
				this.plotter.getPenColor() );
      if( color != null ) {
	try {
	  plotter.setPenColor( color );
	}
	catch( UserInputException ex ) {
	  BasicDlg.showInfoDlg( this, ex.getMessage() );
	}
	Main.setProperty(
		"jkcemu.plotter.pen.color",
		String.format( "%08X", color.getRGB() ) );
      }
    }
  }


  private void doNewPage()
  {
    Plotter plotter = this.plotter;
    if( plotter != null ) {
      if( this.mnuConfirmNewPage.isSelected() ) {
	if( BasicDlg.showYesNoDlg( this, "Neue Seite?" ) ) {
	  plotter.newPage();
	}
      } else {
	plotter.newPage();
      }
    }
  }


  private boolean isClean()
  {
    Plotter plotter = this.plotter;
    return plotter != null ? plotter.isClean() : true;
  }


  private void repaintImage()
  {
    this.dirty = false;
    updActionBtns();
    repaint();
  }


  private void updActionBtns()
  {
    boolean state = false;
    Plotter plotter = this.plotter;
    if( plotter != null ) {
      state = !plotter.isClean();
    }
    this.mnuSaveAs.setEnabled( state );
    this.mnuPrint.setEnabled( state );
    this.mnuCopy.setEnabled( state );
    this.btnSaveAs.setEnabled( state );
    this.btnPrint.setEnabled( state );
    this.btnRotateLeft.setEnabled( state );
    this.btnRotateRight.setEnabled( state );
  }
}

