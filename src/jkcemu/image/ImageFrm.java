/*
 * (c) 2008-2012 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Anzeige von Bildern
 */

package jkcemu.image;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import java.lang.*;
import java.util.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import jkcemu.Main;
import jkcemu.base.*;
import jkcemu.print.PrintUtil;


public class ImageFrm extends AbstractImageFrm implements
						DropTargetListener,
						FlavorListener,
						MouseMotionListener
{
  private static final String DEFAULT_STATUS_TEXT = "Bereit";

  private static ImageFrm instance = null;

  private JButton   btnOpen;
  private JButton   btnSaveAs;
  private JButton   btnPrint;
  private JButton   btnRotateLeft;
  private JButton   btnRotateRight;
  private JMenuItem mnuOpen;
  private JMenuItem mnuSaveAs;
  private JMenuItem mnuPrint;
  private JMenuItem mnuClose;
  private JMenuItem mnuImgCopy;
  private JMenuItem mnuImgPaste;
  private JMenuItem mnuRoundCorners;
  private JMenuItem mnuBgSystem;
  private JMenuItem mnuBgBlack;
  private JMenuItem mnuBgWhite;
  private JMenuItem mnuAutoResize;
  private JMenuItem mnuHelpContent;
  private JLabel    labelStatus;
  private int       lastRoundTopPixels;
  private int       lastRoundBottomPixels;


  public static void open()
  {
    if( instance != null ) {
      if( instance.getExtendedState() == Frame.ICONIFIED ) {
	instance.setExtendedState( Frame.NORMAL );
      }
    } else {
      instance = new ImageFrm();
    }
    instance.toFront();
    instance.setVisible( true );
  }


  public static void open( Image image, String title )
  {
    open();
    if( image != null ) {
      instance.showImageInternal( image, title );
    }
  }


  public static void open( File file )
  {
    open();
    if( file != null ) {
      instance.showImageFile( file );
    }
  }


	/* --- DropTargetListener --- */

  @Override
  public void dragEnter( DropTargetDragEvent e )
  {
    if( !EmuUtil.isFileDrop( e ) )
      e.rejectDrag();
  }


  @Override
  public void dragExit( DropTargetEvent e )
  {
    // empty
  }


  @Override
  public void dragOver( DropTargetDragEvent e )
  {
    // empty
  }


  @Override
  public void drop( DropTargetDropEvent e )
  {
    File file = EmuUtil.fileDrop( this, e );
    if( file != null ) {
      showImageFile( file );
    }
  }


  @Override
  public void dropActionChanged( DropTargetDragEvent e )
  {
    if( !EmuUtil.isFileDrop( e ) )
      e.rejectDrag();
  }


	/* --- FlavorListener --- */

  @Override
  public void flavorsChanged( FlavorEvent e )
  {
    if( e.getSource() == this.clipboard )
      updPasteBtn();
  }


  @Override
  public void mouseDragged( MouseEvent e )
  {
    mouseMoved( e );
  }


  @Override
  public void mouseMoved( MouseEvent e )
  {
    if( e.getComponent() == this.imgFld ) {
      String text = DEFAULT_STATUS_TEXT;
      if( this.image != null ) {
	int    w = this.image.getWidth( this );
	int    h = this.image.getHeight( this );
	if( (w > 0) && (h > 0) ) {
	  double f = this.imgFld.getCurScale();
	  int    x = (int) Math.round( (double) e.getX() / f );
	  int    y = (int) Math.round( (double) e.getY() / f );
	  if( (x >= 0) && (x < w) && (y >= 0) && (y < h) ) {
	    if( this.image instanceof BufferedImage ) {
	      if( ImgUtil.hasAlpha( (BufferedImage) this.image ) ) {
		int rgba = ((BufferedImage) this.image).getRGB( x, y );
		text = String.format(
				"X,Y=%d,%d  RGB=%d,%d,%d  Alpha=%d",
				x,
				y,
				(rgba >> 16) & 0xFF,
				(rgba >> 8) & 0xFF,
				rgba & 0xFF,
				(rgba >> 24) & 0xFF );
	      } else {
		int rgb = ((BufferedImage) this.image).getRGB( x, y );
		text = String.format(
				"X,Y=%d,%d  RGB=%d,%d,%d",
				x,
				y,
				(rgb >> 16) & 0xFF,
				(rgb >> 8) & 0xFF,
				rgb & 0xFF );
	      }
	    } else {
	      text = String.format( "X=%d Y=%d", x, y );
	    }
	  }
	}
      }
      this.labelStatus.setText( text );
      e.consume();
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public boolean applySettings( Properties props, boolean resizable )
  {
    boolean rv = false;
    if( props != null ) {
      rv = super.applySettings( props, resizable );

      String prefix = getSettingsPrefix();

      this.mnuAutoResize.setSelected(
		EmuUtil.parseBoolean(
			props.getProperty( prefix + ".auto_resize" ),
			false ) );

      AbstractButton btn   = this.mnuBgSystem;
      Color          color = SystemColor.window;
      String         text  = props.getProperty( prefix + ".background" );
      if( text != null ) {
	if( text.trim().toLowerCase().equals( "black" ) ) {
	  color = Color.black;
	  btn   = this.mnuBgBlack;
	}
	else if( text.trim().toLowerCase().equals( "white" ) ) {
	  color = Color.white;
	  btn   = this.mnuBgWhite;
	}
      }
      btn.setSelected( true );
      this.imgFld.setBackground( color );
      this.imgFld.repaint();
    }
    return rv;
  }


  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv = false;
    if( e != null ) {
      Object src = e.getSource();
      if( src == this.btnRotateLeft ) {
	rv = true;
	doRotateLeft();
      }
      else if( src == this.btnRotateRight ) {
	rv = true;
	doRotateRight();
      }
      else if( (src == this.btnOpen) || (src == this.mnuOpen) ) {
	rv = true;
	doOpen();
      }
      else if( src == this.mnuClose ) {
	rv = true;
	doClose();
      }
      else if( (src == this.btnSaveAs) || (src == this.mnuSaveAs) ) {
	rv = true;
	doSaveAs();
      }
      else if( (src == this.btnPrint) || (src == this.mnuPrint) ) {
	rv = true;
	if( this.image != null )
	  PrintUtil.doPrint( this, this.imgFld, "Bildbetrachter" );
      }
      else if( src == this.mnuImgCopy ) {
	rv = true;
	doImgCopy();
      }
      else if( src == this.mnuImgPaste ) {
	rv = true;
	doImgPaste();
      }
      else if( src == this.mnuRoundCorners ) {
	rv = true;
	doRoundCorners();
      }
      else if( src == this.mnuBgSystem ) {
	rv = true;
	this.imgFld.setBackground( SystemColor.window );
	this.imgFld.repaint();
      }
      else if( src == this.mnuBgBlack ) {
	rv = true;
	this.imgFld.setBackground( Color.black );
	this.imgFld.repaint();
      }
      else if( src == this.mnuBgWhite ) {
	rv = true;
	this.imgFld.setBackground( Color.white );
	this.imgFld.repaint();
      }
      else if( src == this.mnuHelpContent ) {
        rv = true;
        HelpFrm.open( "/help/tools/imageviewer.htm" );
      }
    }
    if( !rv ) {
      rv = super.doAction( e );
    }
    return rv;
  }


  @Override
  public boolean doClose()
  {
    boolean rv = super.doClose();
    if( rv ) {
      Main.checkQuit( this );
    } else {
      // // damit beim erneuten Oeffnen das Fenster leer ist
      showImageInternal( null, null );
      this.labelStatus.setText( DEFAULT_STATUS_TEXT );
    }
    return rv;
  }


  @Override
  public void mouseExited( MouseEvent e )
  {
    if( e.getComponent() == this.imgFld )
      this.labelStatus.setText( DEFAULT_STATUS_TEXT );
  }


  @Override
  public void putSettingsTo( Properties props )
  {
    if( props != null ) {
      super.putSettingsTo( props );

      String prefix = getSettingsPrefix();

      props.setProperty(
		prefix + ".auto_resize",
		String.valueOf( this.mnuAutoResize.isSelected() ) );

      String colorText = "system";
      if( this.mnuBgBlack.isSelected() ) {
	colorText = "black";
      }
      else if( this.mnuBgWhite.isSelected() ) {
	colorText = "white";
      }
      props.setProperty( prefix + ".background", colorText );
    }
  }


  @Override
  protected void updWindowSize()
  {
    if( this.mnuAutoResize.isSelected() )
      pack();
  }


	/* --- Aktionen --- */

  private void doOpen()
  {
    File file = EmuUtil.showFileOpenDlg(
			this,
			"Bilddatei \u00F6ffnen",
			Main.getLastPathFile( "image" ),
			ImgUtil.createFileFilter(
					ImageIO.getReaderFileSuffixes() ) );
    if( file != null ) {
      if( showImageFile( file ) ) {
	Main.setLastFile( file, "image" );
      }
    }
  }


  private void doImgPaste()
  {
    if( this.clipboard != null ) {
      try {
	if( this.clipboard.isDataFlavorAvailable( DataFlavor.imageFlavor ) ) {
	  Object o = this.clipboard.getData( DataFlavor.imageFlavor );
	  if( o != null ) {
	    if( o instanceof Image ) {
	      showImageInternal( (Image) o, "Eingef\u00FCgtes Bild" );
	      this.file = null;
	    }
	  }
	}
      }
      catch( Exception ex ) {}
    }
  }


  private void doRoundCorners()
  {
    if( this.image != null ) {
      RoundCornersDlg dlg = new RoundCornersDlg(
					this,
					this.lastRoundTopPixels,
					this.lastRoundBottomPixels );
      dlg.setVisible( true );
      int nTopPixels    = dlg.getNumTopPixels();
      int nBottomPixels = dlg.getNumBottomPixels();
      if( (nTopPixels > 0) || (nBottomPixels > 0) ) {
	BufferedImage image = ImgUtil.roundCorners(
						this,
						this.image,
						nTopPixels,
						nBottomPixels );
	if( image != null ) {
	  this.image = image;
	  this.scrollPane.invalidate();
	  this.imgFld.setImage( image );
	  this.scrollPane.validate();
	  this.scrollPane.repaint();
	}
	this.lastRoundTopPixels    = nTopPixels;
	this.lastRoundBottomPixels = nBottomPixels;
      }
    }
  }


	/* --- Konstruktor --- */

  private ImageFrm()
  {
    this.lastRoundTopPixels    = 0;
    this.lastRoundBottomPixels = 0;
    setTitleInternal( null );
    Main.updIcon( this );


    // Menu
    JMenu mnuFile = new JMenu( "Datei" );
    mnuFile.setMnemonic( KeyEvent.VK_D );

    // Menu Datei
    this.mnuOpen = createJMenuItem( "\u00D6ffnen..." );
    mnuFile.add( this.mnuOpen );

    this.mnuSaveAs = createJMenuItem( "Speichern unter..." );
    this.mnuSaveAs.setEnabled( false );
    mnuFile.add( this.mnuSaveAs );
    mnuFile.addSeparator();

    this.mnuPrint = createJMenuItem( "Drucken..." );
    this.mnuPrint.setEnabled( false );
    mnuFile.add( this.mnuPrint );
    mnuFile.addSeparator();

    this.mnuClose = createJMenuItem( "Schlie\u00DFen" );
    mnuFile.add( this.mnuClose );


    // Menu Bearbeiten
    JMenu mnuEdit = new JMenu( "Bearbeiten" );
    mnuEdit.setMnemonic( KeyEvent.VK_B );

    this.mnuImgCopy = createJMenuItem( "Bild kopieren" );
    this.mnuImgCopy.setEnabled( false );
    mnuEdit.add( this.mnuImgCopy );

    this.mnuImgPaste = createJMenuItem( "Bild einf\u00FCgen" );
    mnuEdit.add( this.mnuImgPaste );
    mnuEdit.addSeparator();

    this.mnuRoundCorners = createJMenuItem( "Ecken abrunden..." );
    this.mnuRoundCorners.setEnabled( false );
    mnuEdit.add( this.mnuRoundCorners );


    // Menu Einstellungen
    JMenu mnuSettings = new JMenu( "Einstellungen" );
    mnuSettings.setMnemonic( KeyEvent.VK_E );

    this.mnuAutoResize = new JCheckBoxMenuItem(
				"Fenstergr\u00F6\u00DFe anpassen",
				false );
    mnuSettings.add( this.mnuAutoResize );
    mnuSettings.addSeparator();

    JMenu mnuBgColor = new JMenu( "Hintergrundfarbe" );
    mnuSettings.add( mnuBgColor );

    ButtonGroup grpBgColor = new ButtonGroup();

    this.mnuBgSystem = new JRadioButtonMenuItem( "System", true );
    this.mnuBgSystem.addActionListener( this );
    grpBgColor.add( this.mnuBgSystem );
    mnuBgColor.add( this.mnuBgSystem );
    mnuBgColor.addSeparator();

    this.mnuBgBlack = new JRadioButtonMenuItem( "schwarz", false );
    this.mnuBgBlack.addActionListener( this );
    grpBgColor.add( this.mnuBgBlack );
    mnuBgColor.add( this.mnuBgBlack );

    this.mnuBgWhite = new JRadioButtonMenuItem( "wei\u00DF", false );
    this.mnuBgWhite.addActionListener( this );
    grpBgColor.add( this.mnuBgWhite );
    mnuBgColor.add( this.mnuBgWhite );


    // Menu Hilfe
    JMenu mnuHelp = new JMenu( "?" );

    this.mnuHelpContent = createJMenuItem( "Hilfe..." );
    mnuHelp.add( this.mnuHelpContent );


    // Menu zusammenbauen
    JMenuBar mnuBar = new JMenuBar();
    mnuBar.add( mnuFile );
    mnuBar.add( mnuEdit );
    mnuBar.add( mnuSettings );
    mnuBar.add( mnuHelp );
    setJMenuBar( mnuBar );


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

    this.btnOpen = createImageButton( "/images/file/open.png", "\u00D6ffnen" );
    toolBar.add( this.btnOpen );

    this.btnSaveAs = createImageButton(
			"/images/file/save_as.png", "Speichern unter" );
    this.btnSaveAs.setEnabled( false );
    toolBar.add( this.btnSaveAs );

    this.btnPrint = createImageButton( "/images/file/print.png", "Drucken" );
    this.btnPrint.setEnabled( false );
    toolBar.add( this.btnPrint );
    toolBar.addSeparator();

    this.btnRotateLeft = createImageButton(
				"/images/edit/rotate_left.png",
				"Nach links drehen" );
    this.btnRotateLeft.setEnabled( false );
    toolBar.add( this.btnRotateLeft );

    this.btnRotateRight = createImageButton(
				"/images/edit/rotate_right.png",
				"Nach rechts drehen" );
    this.btnRotateRight.setEnabled( false );
    toolBar.add( this.btnRotateRight );
    toolBar.addSeparator();

    toolBar.add( createScaleComboBox(
			100, 25, 33, 50, 75, 100, 200, 300, 400 ) );


    // Statuszeile
    this.labelStatus = new JLabel( DEFAULT_STATUS_TEXT );
    JPanel panelStatus = new JPanel(
		new FlowLayout( FlowLayout.LEFT, 5, 5 ) );
    panelStatus.add( this.labelStatus );
    add( panelStatus, BorderLayout.SOUTH );


    // Drop-Ziele
    (new DropTarget( this.imgFld, this )).setActive( true );
    (new DropTarget( this.scrollPane, this )).setActive( true );


    // Fenstergroesse
    if( !applySettings( Main.getProperties(), true ) ) {
      pack();
      setScreenCentered();
    }
    setResizable( true );


    // sonstiges
    this.imgFld.addMouseListener( this );
    this.imgFld.addMouseMotionListener( this );
    if( this.clipboard != null ) {
      this.clipboard.addFlavorListener( this );
    }
  }


	/* --- private Methoden --- */

  private void setTitleInternal( String title )
  {
    StringBuilder buf = new StringBuilder( 64 );
    buf.append( "JKCEMU Bildbetrachter" );
    if( title != null ) {
      if( !title.isEmpty() ) {
	buf.append( ": " );
	buf.append( title );
      }
      if( this.image != null) {
	buf.append( " (" );
	buf.append( this.image.getWidth( this ) );
	buf.append( (char) 'x' );
	buf.append( this.image.getHeight( this ) );
	buf.append( (char) ')' );
      }
    }
    setTitle( buf.toString() );
  }


  private boolean showImageFile( File file )
  {
    boolean rv = false;
    if( file != null ) {
      Image       image  = null;
      String      errMsg = null;
      InputStream in     = null;
      try {
	in    = new FileInputStream( file );
	image = ImageIO.read( in );
	if( image != null ) {
	  showImageInternal( image, file.getPath() );
	  this.file = file;
	  rv = true;
	} else {
	  errMsg = "Dateiformat nicht unterst\u00FCtzt";
	}
      }
      catch( Exception ex ) {
	errMsg = ex.getMessage();
      }
      catch( OutOfMemoryError ex ) {
	errMsg = "Es steht nicht gen\u00FCgend Speicher zur Verf\u00FCgung.";
      }
      finally {
	if( in != null ) {
	  try {
	    in.close();
	  }
	  catch( IOException ex ) {}
	}
      }
      if( image == null ) {
	StringBuilder buf = new StringBuilder( 64 );
	buf.append( "Bilddatei kann nicht geladen werden" );
	if( errMsg != null ) {
	  buf.append( ":\n" );
	  buf.append( errMsg );
	} else {
	  buf.append( (char) '.' );
	}
	BasicDlg.showErrorDlg( this, buf.toString() );
      }
    }
    return rv;
  }


  private void showImageInternal( Image image, String title )
  {
    boolean state     = false;
    boolean autoScale = true;
    double  scale     = 1.0;
    if( image != null ) {

      /*
       * Bild moeglichst in ein BufferedImage umwandeln,
       * Sollte das nicht moeglich sein, dann eben als Image behalten.
       * In dem Fall ist dann die Anzeige der Farbwerte
       * in der Statuszeile nicht moeglich.
       */
      if( !(image instanceof BufferedImage) ) {
	BufferedImage bImg = ImgUtil.createBufferedImage( this, image );
	if( bImg != null ) {
	  if( bImg != image ) {
	    image.flush();
	  }
	  image = bImg;
	}
      }

      /*
       * Wenn das Fenster noch unsichtbar oder
       * die Option "Fenstergroesse anpassen" eingeschaltet ist,
       * soll das Bild je nach Groesse entweder unskaliert oder
       * auf 80% der Bildschirmgroesse (abzueglich Fensterraender
       * und Menuleiste) skaliert angezeigt werden.
       *
       * Ist die Option "Fenstergroesse anpassen" ausgeschaltet,
       * soll sich das Bild nur im aktuellen Fensterbereich ausbreiten.
       */
      int maxW = 0;
      int maxH = 0;
      if( isVisible() && !this.mnuAutoResize.isSelected() ) {
	JViewport vp = this.scrollPane.getViewport();
	if( vp != null ) {
	  int vpW = vp.getWidth();
	  int vpH = vp.getHeight();
	  if( (vpW > 0) && (vpH > 0) ) {
	    maxW      = vpW;
	    maxH      = vpH;
	    autoScale = false;
	  }
        }
      }
      if( (maxW < 1) || (maxH < 1) ) {
	Toolkit tk = getToolkit();
	if( tk != null ) {
	  Dimension screenSize = tk.getScreenSize();
	  if( screenSize != null ) {
	    maxW  = (screenSize.width * 8) / 10;
	    maxH = (screenSize.height * 8) / 10;
	  }
	}
      }
      if( (maxW < 1) || (maxH < 1) ) {
	maxW = 640;
	maxH = 480;
      }

      // Bildgroesse ermitteln
      int wImg = 0;
      int hImg = 0;
      if( image instanceof BufferedImage ) {
	wImg = ((BufferedImage) image).getWidth();
	hImg = ((BufferedImage) image).getHeight();
      } else {
	ImgUtil.ensureImageLoaded( this, image );
	wImg = image.getWidth( this );
	hImg = image.getHeight( this );
      }

      // Skalierung ermitteln
      if( (wImg > 0) && (hImg > 0)
	  && ((wImg > maxW) || (hImg > maxH)) )
      {
	scale = Math.min(
			(double) maxW / (double) wImg,
			(double) maxH / (double) hImg );
      }

      // Bild vorhanden
      state = true;
    }

    // Bild anzeigen
    Image oldImage = this.image;
    this.image     = image;
    this.scrollPane.invalidate();
    this.imgFld.setImage( image );
    this.imgFld.setRotation( ImgFld.Rotation.NONE );
    this.imgFld.setScale( scale );
    this.scrollPane.validate();
    this.scrollPane.repaint();
    setTitleInternal( title );
    updDisplayScale();
    if( oldImage != null ) {
      oldImage.flush();
    }
    this.btnSaveAs.setEnabled( state );
    this.btnPrint.setEnabled( state );
    this.btnRotateLeft.setEnabled( state );
    this.btnRotateRight.setEnabled( state );
    this.mnuSaveAs.setEnabled( state );
    this.mnuPrint.setEnabled( state );
    this.mnuImgCopy.setEnabled( state );
    this.mnuRoundCorners.setEnabled( state );
    this.file = null;
    if( autoScale ) {
      pack();
    }
  }


  private void updPasteBtn()
  {
    boolean state = false;
    if( this.clipboard != null ) {
      try {
	state = this.clipboard.isDataFlavorAvailable( DataFlavor.imageFlavor );
      }
      catch( IllegalStateException ex ) {}
    }
    this.mnuImgPaste.setEnabled( state );
  }
}

