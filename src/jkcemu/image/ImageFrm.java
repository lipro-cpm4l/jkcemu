/*
 * (c) 2008-2011 Jens Mueller
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


public class ImageFrm extends BasicFrm implements
						ComponentListener,
						DropTargetListener,
						FlavorListener
{
  private ScreenFrm   screenFrm;
  private Clipboard   clipboard;
  private File        file;
  private Image       image;
  private JButton     btnOpen;
  private JButton     btnSaveAs;
  private JButton     btnPrint;
  private JButton     btnRotateLeft;
  private JButton     btnRotateRight;
  private JComboBox   comboScale;
  private JMenuItem   mnuOpen;
  private JMenuItem   mnuSaveAs;
  private JMenuItem   mnuPrint;
  private JMenuItem   mnuClose;
  private JMenuItem   mnuImgCopy;
  private JMenuItem   mnuImgPaste;
  private JMenuItem   mnuRoundCorners;
  private JMenuItem   mnuBgSystem;
  private JMenuItem   mnuBgBlack;
  private JMenuItem   mnuBgWhite;
  private JMenuItem   mnuAutoResize;
  private JMenuItem   mnuHelpContent;
  private ImgFld      imgFld;
  private JScrollPane scrollPane;
  private int         lastRoundTopPixels;
  private int         lastRoundBottomPixels;
  private int         maxUnscaledWidth;
  private int         maxUnscaledHeight;
  private boolean     scaleEnabled;


  public ImageFrm( ScreenFrm screenFrm )
  {
    this.screenFrm             = screenFrm;
    this.clipboard             = null;
    this.file                  = null;
    this.image                 = null;
    this.lastRoundTopPixels    = 0;
    this.lastRoundBottomPixels = 0;
    this.maxUnscaledWidth      = 0;
    this.maxUnscaledHeight     = 0;
    this.scaleEnabled          = true;
    setTitleInternal( null );
    Main.updIcon( this );

    Toolkit tk = getToolkit();
    if( tk != null ) {
      this.clipboard = tk.getSystemClipboard();
      if( this.clipboard != null ) {
	this.clipboard.addFlavorListener( this );
      }
    }


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
				true );
    this.mnuAutoResize.addActionListener( this );
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


    // Fensterinhalt
    setLayout( new BorderLayout( 0, 0 ) );


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

    this.comboScale = new JComboBox();
    this.comboScale.setEditable( true );
    this.comboScale.addItem( "25 %" );
    this.comboScale.addItem( "33 %" );
    this.comboScale.addItem( "50 %" );
    this.comboScale.addItem( "75 %" );
    this.comboScale.addItem( "100 %" );
    this.comboScale.addItem( "150 %" );
    this.comboScale.addItem( "200 %" );
    this.comboScale.addItem( "300 %" );
    this.comboScale.addItem( "400 %" );
    this.comboScale.setSelectedItem( "100 %" );
    this.comboScale.setToolTipText( "Skalierungsfaktor" );
    this.comboScale.addActionListener( this );
    toolBar.add( this.comboScale );


    /*
     * Bildanzeige
     * Anfangsgroesse im Verhaeltnis 4:3, aber so,
     * dass ein Schnappschuss der Bildschirmausgabe hineinpasst.
     */
    this.imgFld     = new ImgFld( 342, 256 );
    this.scrollPane = new JScrollPane(
				this.imgFld,
				JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS );
    add( this.scrollPane, BorderLayout.CENTER );


    // Drop-Ziele
    (new DropTarget( this.imgFld, this )).setActive( true );
    (new DropTarget( this.scrollPane, this )).setActive( true );


    // sonstiges
    if( !applySettings( Main.getProperties(), true ) ) {
      pack();
      setScreenCentered();
    }
    setResizable( true );
    addComponentListener( this );
  }


  public void showImage( Image image, String title )
  {
    if( image != null ) {

      /*
       * Wenn die Option "Fenstergroesse anpassen" gesetzt ist,
       * soll das Bild bei Bedarf auf den ganzen Bildschirm abzueglich
       * der Fensterraender und der Menuleiste ausgebreitet werden.
       * Das Bild wird deshalb erst skaliert, wenn es mehr als 90%
       * des Bildschirms einnehmen wuerde.
       *
       * Ist die Option "Fenstergroesse anpassen" ausgeschaltet,
       * soll sich das Bild nur im aktuellen Fensterbereich ausbreiten.
       */
      if( (this.maxUnscaledWidth < 1) || (this.maxUnscaledHeight < 1) ) {
	Toolkit tk = getToolkit();
	if( tk != null ) {
	  Dimension screenSize = tk.getScreenSize();
	  if( screenSize != null ) {
	    this.maxUnscaledWidth  = (screenSize.width * 9) / 10;
	    this.maxUnscaledHeight = (screenSize.height * 9) / 10;
	  }
	}
	if( (this.maxUnscaledWidth < 1) || (this.maxUnscaledHeight < 1) ) {
	  this.maxUnscaledWidth  = 640;
	  this.maxUnscaledHeight = 480;
	}
      }
      int maxUnscaledWidth  = this.maxUnscaledWidth;
      int maxUnscaledHeight = this.maxUnscaledHeight;
      if( !this.mnuAutoResize.isSelected() ) {
	JViewport vp = this.scrollPane.getViewport();
	if( vp != null ) {
	  int wVP = vp.getWidth();
	  int hVP = vp.getHeight();
	  if( (wVP > 0) && (hVP > 0) ) {
	    maxUnscaledWidth  = wVP;
	    maxUnscaledHeight = hVP;
	  }
	}
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
      double scale = 1.0;
      if( (wImg > 0) && (hImg > 0)
	  && ((wImg > maxUnscaledWidth) || (hImg > maxUnscaledHeight)) )
      {
	scale = Math.min(
			(double) maxUnscaledWidth / (double) wImg,
			(double) maxUnscaledHeight / (double) hImg );
      }

      // Bild anzeigen
      this.image = image;
      this.scrollPane.invalidate();
      this.imgFld.setImage( image );
      this.imgFld.setRotation( ImgFld.Rotation.NONE );
      this.imgFld.setScale( scale );
      this.scrollPane.validate();
      this.scrollPane.repaint();
      setTitleInternal( title );
      updDisplayScale();
      updWindowSize();

      this.btnSaveAs.setEnabled( true );
      this.btnPrint.setEnabled( true );
      this.btnRotateLeft.setEnabled( true );
      this.btnRotateRight.setEnabled( true );
      this.mnuSaveAs.setEnabled( true );
      this.mnuPrint.setEnabled( true );
      this.mnuImgCopy.setEnabled( true );
      this.mnuRoundCorners.setEnabled( true );
      this.file = null;
    }
  }


  public boolean showImageFile( File file )
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
	  showImage( image, file.getPath() );
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



	/* --- ComponentListener --- */

  @Override
  public void componentHidden( ComponentEvent e )
  {
    // leer
  }


  @Override
  public void componentMoved( ComponentEvent e )
  {
    // leer
  }


  @Override
  public void componentResized( ComponentEvent e )
  {
    JViewport vp = this.scrollPane.getViewport();
    if( vp != null )
      this.imgFld.setViewportSize( vp.getExtentSize() );
  }


  @Override
  public void componentShown( ComponentEvent e )
  {
    // leer
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
    if( file != null )
      showImageFile( file );
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


	/* --- ueberschriebene Methoden --- */

  @Override
  public boolean applySettings( Properties props, boolean resizable )
  {
    boolean rv = false;
    if( props != null ) {
      rv = super.applySettings( props, resizable );

      String prefix = getSettingsPrefix();

      boolean autoResize = EmuUtil.parseBoolean(
			  props.getProperty( prefix + ".auto_resize" ),
			  true );
      if( autoResize != this.mnuAutoResize.isSelected() ) {
	this.mnuAutoResize.setSelected( autoResize );
	if( autoResize ) {
	  updWindowSize();
	}
      }

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
      if( src == this.comboScale ) {
	rv = true;
	doScale();
      }
      else if( src == this.btnRotateLeft ) {
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
	doSave();
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
      else if( src == this.mnuAutoResize ) {
	rv = true;
	updWindowSize();
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
        this.screenFrm.showHelp( "/help/tools/imageviewer.htm" );
      }
    }
    return rv;
  }


  @Override
  public boolean doClose()
  {
    boolean rv = super.doClose();
    if( !rv ) {
      this.screenFrm.childFrameClosed( this );
    }
    return rv;
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
  public void windowClosed( WindowEvent e )
  {
    if( e.getWindow() == this )
      this.screenFrm.childFrameClosed( this );
  }


	/* --- private Methoden --- */

  private void doOpen()
  {
    File file = EmuUtil.showFileOpenDlg(
			this,
			"Bilddatei \u00F6ffnen",
			Main.getLastPathFile( "image" ),
			ImgUtil.createFileFilter(
					ImageIO.getReaderFileSuffixes() ) );
    if( file != null ) {
      if( showImageFile( file ) )
	Main.setLastFile( file, "image" );
    }
  }


  private void doSave()
  {
    if( this.image != null ) {
      BufferedImage   imgToSave = null;
      ImgFld.Rotation rotation  = this.imgFld.getRotation();
      if( rotation == ImgFld.Rotation.NONE ) {
	imgToSave = getBufferedImage();
      } else {
	int w       = this.image.getWidth( this );
	int h       = this.image.getHeight( this );
	int imgType = BufferedImage.TYPE_CUSTOM;
	if( this.image instanceof BufferedImage ) {
	  imgType = ((BufferedImage) this.image).getType();
	  if( imgType == BufferedImage.TYPE_CUSTOM ) {
	    ColorModel cm = ((BufferedImage) this.image).getColorModel();
	    if( cm != null ) {
	      if( !cm.hasAlpha() ) {
		imgType = BufferedImage.TYPE_INT_RGB;
	      }
	    }
	  }
	}
	if( imgType == BufferedImage.TYPE_CUSTOM ) {
	  imgType = BufferedImage.TYPE_INT_ARGB;
	}
	if( (w > 0) && (h > 0) ) {
	  int option = JOptionPane.showConfirmDialog(
				this,
				"Soll das Bild gedreht gespeichert werden,\n"
					+ "so wie Sie es gerade sehen?",
				"Bild gedreht",
				JOptionPane.YES_NO_CANCEL_OPTION,
				JOptionPane.QUESTION_MESSAGE );
	  if( option == JOptionPane.YES_OPTION ) {
	    if( (rotation == ImgFld.Rotation.LEFT)
		|| (rotation == ImgFld.Rotation.RIGHT) )
	    {
	      imgToSave = new BufferedImage( h, w, imgType );
	    } else {
	      imgToSave = new BufferedImage( w, h, imgType );
	    }
	    Graphics g = imgToSave.createGraphics();
	    this.imgFld.drawImage( g, 0, 0, w, h );
	    g.dispose();
	  }
	  else if( option == JOptionPane.NO_OPTION ) {
	    imgToSave = getBufferedImage();
	  }
	} else {
	  imgToSave = getBufferedImage();
	}
      }
      if( imgToSave != null ) {
	File file = ImgUtil.saveImage( this, imgToSave, this.file );
	if( file != null ) {
	  this.file = file;
	}
      }
    }
  }


  private void doImgCopy()
  {
    if( (this.clipboard != null) && (this.image != null) ) {
      try {
	ImgSelection ims = new ImgSelection( this.image );
	this.clipboard.setContents( ims, ims );
      }
      catch( IllegalStateException ex ) {}
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
	      showImage( (Image) o, "Eingef\u00FCgtes Bild" );
	      this.file = null;
	    }
	  }
	}
      }
      catch( Exception ex ) {}
    }
  }


  private void doRotateLeft()
  {
    this.scrollPane.invalidate();
    switch( this.imgFld.getRotation() ) {
      case NONE:
	this.imgFld.setRotation( ImgFld.Rotation.LEFT );
	break;

      case LEFT:
	this.imgFld.setRotation( ImgFld.Rotation.DOWN );
	break;

      case RIGHT:
	this.imgFld.setRotation( ImgFld.Rotation.NONE );
	break;

      case DOWN:
	this.imgFld.setRotation( ImgFld.Rotation.RIGHT );
	break;
    }
    this.scrollPane.validate();
    this.scrollPane.repaint();
    updWindowSize();
  }


  private void doRotateRight()
  {
    this.scrollPane.invalidate();
    switch( this.imgFld.getRotation() ) {
      case NONE:
	this.imgFld.setRotation( ImgFld.Rotation.RIGHT );
	break;

      case LEFT:
	this.imgFld.setRotation( ImgFld.Rotation.NONE );
	break;

      case RIGHT:
	this.imgFld.setRotation( ImgFld.Rotation.DOWN );
	break;

      case DOWN:
	this.imgFld.setRotation( ImgFld.Rotation.LEFT );
	break;
    }
    this.scrollPane.validate();
    this.scrollPane.repaint();
    updWindowSize();
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


  private void doScale()
  {
    if( this.scaleEnabled ) {
      Object o = this.comboScale.getSelectedItem();
      if( o != null ) {
	String s = o.toString();
	if( s != null ) {
	  int pos = s.indexOf( '%' );
	  if( pos >= 0 ) {
	    s = s.substring( 0, pos );
	  }
	  try {
	    int value = Integer.parseInt( s.trim() );
	    if( value >= 0 ) {
	      this.scrollPane.invalidate();
	      this.imgFld.setScale( (double) value / 100.0 );
	      this.scrollPane.validate();
	      this.scrollPane.repaint();
	      updDisplayScale();
	      updWindowSize();
	    }
	  }
	  catch( NumberFormatException ex ) {}
	}
      }
    }
  }


	/* --- private Methoden --- */

  private BufferedImage getBufferedImage()
  {
    BufferedImage rv = null;
    if( this.image != null ) {
      if( this.image instanceof BufferedImage ) {
	rv = (BufferedImage) this.image;
      } else {
	int w = this.image.getWidth( this );
        int h = this.image.getHeight( this );
        if( (w > 0) && (h > 0) ) {
          rv = new BufferedImage( w, h, BufferedImage.TYPE_INT_ARGB );
          Graphics g = rv.createGraphics();
          g.drawImage( this.image, 0, 0, this );
          g.dispose();
	  this.image.flush();
	  this.image = rv;
        }
      }
    }
    return rv;
  }


  private void setTitleInternal( String title )
  {
    StringBuilder buf = new StringBuilder( 64 );
    buf.append( "JKCEMU Bildbetrachter" );
    if( title != null ) {
      if( title.length() > 0 ) {
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


  private void updDisplayScale()
  {
    this.scaleEnabled = false;
    this.comboScale.setSelectedItem(
		String.valueOf( Math.round( this.imgFld.getScale() * 100.0 ) )
								+ " %" );
    this.scaleEnabled = true;
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


  private void updWindowSize()
  {
    if( this.mnuAutoResize.isSelected() )
      pack();
  }
}

