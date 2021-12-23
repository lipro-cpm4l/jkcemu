/*
 * (c) 2016-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Dialog zum Anzeigen von Detailinformationen eines Bildes
 */

package jkcemu.image;

import java.awt.EventQueue;
import java.awt.Insets;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.Transparency;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.io.File;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.EventObject;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import jkcemu.Main;
import jkcemu.base.BaseDlg;
import jkcemu.base.EmuUtil;
import jkcemu.base.GUIFactory;


public class ImagePropsDlg extends BaseDlg implements Runnable
{
  private static final String TEXT_IS_DETERMINING = "wird ermittelt...";

  private static Rectangle lastBounds      = null;
  private static Rectangle lastOwnerBounds = null;

  private File          file;
  private BufferedImage image;
  private int           wImg;
  private int           hImg;
  private int           nIndexedColors;
  private int           nTransparentPixels;
  private int           nTranslucentPixels;
  private boolean       gray;
  private boolean       monochrome;
  private Set<Integer>  totalColors;
  private Set<Integer>  visibleColors;
  private JEditorPane   generalPane;
  private JButton       btnClose;


  public static void showDlg( Window owner, ImageEntry entry )
  {
    (new ImagePropsDlg( owner, entry )).setVisible( true );
  }


	/* --- Runnable --- */

  @Override
  public void run()
  {
    try {
      this.nTransparentPixels = 0;
      this.nTranslucentPixels = 0;
      this.gray               = true;
      this.monochrome         = true;
      this.totalColors.clear();
      this.visibleColors.clear();
      for( int y = 0; y < this.hImg; y++ ) {
	for( int x = 0; x < this.wImg; x++ ) {
	  int v = image.getRGB( x, y );
	  int b = v & 0xFF;
	  int g = (v >> 8) & 0xFF;
	  int r = (v >> 16) & 0xFF;
	  int a = (v >> 24) & 0xFF;
	  if( a == 0 ) {
	    this.nTransparentPixels++;
	  } else if( a < 0xFF ) {
	    this.nTranslucentPixels++;
	  }
	  if( (r != g) || (r != b) ) {
	    this.gray       = false;
	    this.monochrome = false;
	  }
	  if( ((r > 0) && (r < 0xFF))
		|| ((g > 0) && (g < 0xFF))
		|| ((b > 0) && (b < 0xFF)) )
	  {
	    this.monochrome = false;
	  }
	  totalColors.add( v );
	  if( a > 0 ) {
	    visibleColors.add( v );
	  }
	}
      }
    }
    finally {
      EventQueue.invokeLater(
			new Runnable()
			{
			  @Override
			  public void run()
			  {
			    colorsChecked();
			  }
			} );
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv  = false;
    Object  src = e.getSource();
    if( src == this.btnClose ) {
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
      this.btnClose.removeActionListener( this );
      lastBounds = getBounds();
    }
    return rv;
  }


	/* --- Konstruktor --- */

  private ImagePropsDlg( Window owner, ImageEntry entry )
  {
    super( owner, "Bildeigenschaften" );
    this.file               = entry.getFile();
    this.image              = entry.getImage();
    this.wImg               = this.image.getWidth( this );
    this.hImg               = this.image.getHeight( this );
    this.nIndexedColors     = 0;
    this.nTransparentPixels = 0;
    this.nTranslucentPixels = 0;
    this.gray               = false;
    this.monochrome         = false;
    this.totalColors        = new TreeSet<>();
    this.visibleColors      = new TreeSet<>();

    IndexColorModel icm = ImageUtil.getIndexColorModel( image );
    if( icm != null ) {
      this.nIndexedColors = icm.getMapSize();
    }


    // Fensterinhalt
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					1, 1,
					1.0, 1.0,
					GridBagConstraints.CENTER,
					GridBagConstraints.BOTH,
					new Insets( 5, 5, 5, 5 ),
					0, 0 );

    JTabbedPane tabbedPane = GUIFactory.createTabbedPane();
    add( tabbedPane, gbc );


    // Allgemeine Daten
    this.generalPane = GUIFactory.createEditorPane();
    this.generalPane.setMargin( new Insets( 5, 5, 5, 5 ) );
    this.generalPane.setEditable( false );
    fillGeneralPane( -1, -1 );
    tabbedPane.addTab(
		"Allgemein",
		GUIFactory.createScrollPane( this.generalPane ) );


    // Zusatzinformationen
    JEditorPane detailsPane = GUIFactory.createEditorPane();
    detailsPane.setMargin( new Insets( 5, 5, 5, 5 ) );
    detailsPane.setEditable( false );
    tabbedPane.addTab(
		"Zusatzinformationen",
		GUIFactory.createScrollPane( detailsPane ) );


    // Schaltflaeche
    this.btnClose  = GUIFactory.createButtonClose();
    gbc.fill       = GridBagConstraints.NONE;
    gbc.insets.top = 0;
    gbc.weightx    = 0.0;
    gbc.weighty    = 0.0;
    gbc.gridy++;
    add( this.btnClose, gbc );


    // Fenstergroesse und -position
    boolean   boundsDone  = false;
    Rectangle ownerBounds = null;
    if( owner != null ) {
      ownerBounds = owner.getBounds();
    }
    if( (lastBounds != null)
	&& (lastOwnerBounds != null)
	&& (ownerBounds != null) )
    {
      if( ownerBounds.equals( lastOwnerBounds ) ) {
	setBounds( lastBounds );
	boundsDone = true;
      }
    }
    lastOwnerBounds = ownerBounds;
    if( !boundsDone ) {
      pack();
      setParentCentered();
    }
    setResizable( true );


    /*
     * Details erst nach der pack()-Methode fuellen,
     * damit der Dialog nicht zu gross wird
     */
    StringBuilder buf = new StringBuilder( 0x1000 );
    buf.append( "<html>\n" );
    int           posTableBeg = buf.length();
    int           posFirstRow = posTableBeg;
    ExifData      exifData    = entry.getExifData();
    if( exifData != null ) {
      buf.append( "<table border=\"0\">\n" );
      posFirstRow = buf.length();

      // Gruppe Bild
      String docName        = exifData.getDocumentName();
      String imgDesc        = exifData.getImageDesc();
      String author         = exifData.getAuthor();
      String copyright      = exifData.getCopyright();
      String comment        = exifData.getComment();
      String imgID          = exifData.getImageID();
      String resolutionText = exifData.getResolutionText();
      String colorSpace     = exifData.getColorSpace();
      String bitsPerPixel   = exifData.getCompressedBitsPerPixelText();
      String software       = exifData.getSoftware();
      if( (docName != null)
	  || (imgDesc != null)
	  || (author != null)
	  || (copyright != null)
	  || (comment != null)
	  || (imgID != null)
	  || (resolutionText != null)
	  || (colorSpace != null)
	  || (bitsPerPixel != null)
	  || (software != null) )
      {
	buf.append( "<tr><th align=\"left\" colspan=\"2\">Bild</th</tr>\n" );
	if( docName != null ) {
	  buf.append( "<tr><td valign=\"top\">Name:</td>"
			+ "<td valign=\"top\">" );
	  EmuUtil.appendHTML( buf, docName );
	  buf.append( "</td></tr>\n" );
	}
	if( imgDesc != null ) {
	  buf.append( "<tr><td valign=\"top\">Beschreibung:</td>"
			+ "<td valign=\"top\">" );
	  EmuUtil.appendHTML( buf, imgDesc );
	  buf.append( "</td></tr>\n" );
	}
	if( author != null ) {
	  buf.append( "<tr><td valign=\"top\">Autor:</td>"
			+ "<td valign=\"top\">" );
	  EmuUtil.appendHTML( buf, author );
	  buf.append( "</td></tr>\n" );
	}
	if( copyright != null ) {
	  buf.append( "<tr><td valign=\"top\">Copyright:</td>"
			+ "<td valign=\"top\">" );
	  EmuUtil.appendHTML( buf, copyright );
	  buf.append( "</td></tr>\n" );
	}
	if( comment != null ) {
	  buf.append( "<tr><td valign=\"top\">Kommentar:</td>"
			+ "<td valign=\"top\">" );
	  EmuUtil.appendHTML( buf, comment );
	  buf.append( "</td></tr>\n" );
	}
	if( imgID != null ) {
	  buf.append( "<tr><td valign=\"top\">Bild-ID:</td>"
			+ "<td valign=\"top\">" );
	  EmuUtil.appendHTML( buf, imgID );
	  buf.append( "</td></tr>\n" );
	}
	if( resolutionText!= null ) {
	  buf.append( "<tr><td valign=\"top\">Aufl&ouml;sung:</td>"
			+ "<td valign=\"top\">" );
	  EmuUtil.appendHTML( buf, resolutionText );
	  buf.append( "</td></tr>\n" );
	}
	if( colorSpace != null ) {
	  buf.append( "<tr><td valign=\"top\">Farbdarstellung:</td>"
			+ "<td valign=\"top\">" );
	  EmuUtil.appendHTML( buf, colorSpace );
	  buf.append( "</td></tr>\n" );
	}
	if( bitsPerPixel != null ) {
	  buf.append( "<tr><td valign=\"top\">Komprimierte Bits/Pixel:</td>"
			+ "<td valign=\"top\">" );
	  EmuUtil.appendHTML( buf, bitsPerPixel );
	  buf.append( "</td></tr>\n" );
	}
	if( software != null ) {
	  buf.append( "<tr><td valign=\"top\">Software:</td>"
			+ "<td valign=\"top\">" );
	  EmuUtil.appendHTML( buf, software );
	  buf.append( "</td></tr>\n" );
	}
      }

      // Gruppe Kamera
      String cameraVendor = exifData.getCameraVendor();
      String cameraModel  = exifData.getCameraModel();
      if( (cameraVendor != null) || (cameraModel != null) ) {
	if( buf.length() > posFirstRow ) {
	  buf.append( "<tr><td colspan=\"2\">&nbsp;</td</tr>\n" );
	}
	buf.append( "<tr><th align=\"left\" colspan=\"2\">"
			+ "Kamera</th</tr>\n" );
	if( cameraVendor != null ) {
	  buf.append( "<tr><td valign=\"top\">Hersteller:</td>"
			+ "<td valign=\"top\">" );
	  EmuUtil.appendHTML( buf, cameraVendor );
	  buf.append( "</td></tr>\n" );
	}
	if( cameraModel != null ) {
	  buf.append( "<tr><td valign=\"top\">Modell:</td>"
			+ "<td valign=\"top\">" );
	  EmuUtil.appendHTML( buf, cameraModel );
	  buf.append( "</td></tr>\n" );
	}
      }

      // Gruppe Aufnahme
      java.util.Date date     = exifData.getDate();
      String sceneCaptureType = exifData.getSceneCaptureTypeText();
      String fNumber          = exifData.getFNumberText();
      String exposureProgram  = exifData.getExposureProgramm();
      String exposureMode     = exifData.getExposureModeText();
      String meteringMode     = exifData.getMeteringMode();
      String exposureTime     = exifData.getExposureTimeText();
      String flashMode        = exifData.getFlashMode();
      String isoText          = exifData.getISOText();
      String focalLength      = exifData.getFocalLengthText();
      String focalLength35mm  = exifData.getFocalLength35mmText();
      String maxApertureValue = exifData.getMaxApertureValue();
      String subjectDistRange = exifData.getSubjectDistanceRangeText();
      String subjectDistance  = exifData.getSubjectDistanceText();
      if( (date != null)
	  || (sceneCaptureType != null)
	  || (fNumber != null)
	  || (exposureProgram != null)
	  || (exposureMode != null)
	  || (meteringMode != null)
	  || (exposureTime != null)
	  || (flashMode != null)
	  || (isoText != null)
	  || (focalLength != null)
	  || (focalLength35mm != null)
	  || (maxApertureValue != null)
	  || (subjectDistRange != null)
	  || (subjectDistance != null) )
      {
	if( buf.length() > posFirstRow ) {
	  buf.append( "<tr><td colspan=\"2\">&nbsp;</td</tr>\n" );
	}
	buf.append( "<tr><th align=\"left\" colspan=\"2\">"
			+ "Aufnahme</th</tr>\n" );
	if( date != null ) {
	  buf.append( "<tr><td valign=\"top\">Datum:</td>"
			+ "<td valign=\"top\">" );
	  EmuUtil.appendHTML(
			buf,
			DateFormat.getDateTimeInstance(
				DateFormat.MEDIUM,
				DateFormat.MEDIUM ).format( date ) );
	  buf.append( "</td></tr>\n" );
	}
	if( sceneCaptureType != null ) {
	  buf.append( "<tr><td valign=\"top\">Bilderfassungsart:</td>"
			+ "<td valign=\"top\">" );
	  EmuUtil.appendHTML( buf, sceneCaptureType );
	  buf.append( "</td></tr>\n" );
	}
	if( fNumber != null ) {
	  buf.append( "<tr><td valign=\"top\">Blendenzahl:</td>"
			+ "<td valign=\"top\">" );
	  EmuUtil.appendHTML( buf, fNumber );
	  buf.append( "</td></tr>\n" );
	}
	if( exposureProgram != null ) {
	  buf.append( "<tr><td valign=\"top\">Belichtungsverfahren:</td>"
			+ "<td valign=\"top\">" );
	  EmuUtil.appendHTML( buf, exposureProgram );
	  buf.append( "</td></tr>\n" );
	}
	if( exposureMode != null ) {
	  buf.append( "<tr><td valign=\"top\">Belichtungsart:</td>"
			+ "<td valign=\"top\">" );
	  EmuUtil.appendHTML( buf, exposureMode );
	  buf.append( "</td></tr>\n" );
	}
	if( meteringMode != null ) {
	  buf.append( "<tr><td valign=\"top\">Belichtungsmessung:</td>"
			+ "<td valign=\"top\">" );
	  EmuUtil.appendHTML( buf, meteringMode );
	  buf.append( "</td></tr>\n" );
	}
	if( exposureTime != null ) {
	  buf.append( "<tr><td valign=\"top\">Belichtungszeit:</td>"
			+ "<td valign=\"top\">" );
	  EmuUtil.appendHTML( buf, exposureTime );
	  buf.append( "</td></tr>\n" );
	}
	if( flashMode != null ) {
	  buf.append( "<tr><td valign=\"top\">Blitzlicht:</td>"
			+ "<td valign=\"top\">" );
	  EmuUtil.appendHTML( buf, flashMode );
	  buf.append( "</td></tr>\n" );
	}
	if( isoText != null ) {
	  buf.append( "<tr><td valign=\"top\">Lichtempfindlichkeit:</td>"
			+ "<td valign=\"top\">" );
	  EmuUtil.appendHTML( buf, isoText );
	  buf.append( "</td></tr>\n" );
	}
	if( focalLength != null ) {
	  buf.append( "<tr><td valign=\"top\">Brenweite:</td>"
			+ "<td valign=\"top\">" );
	  EmuUtil.appendHTML( buf, focalLength );
	  buf.append( "</td></tr>\n" );
	}
	if( focalLength35mm != null ) {
	  buf.append( "<tr><td valign=\"top\">35mm-Brenweite:</td>"
			+ "<td valign=\"top\">" );
	  EmuUtil.appendHTML( buf, focalLength35mm );
	  buf.append( "</td></tr>\n" );
	}
	if( maxApertureValue != null ) {
	 buf.append( "<tr><td valign=\"top\">Maximale Blende:</td>"
			+ "<td valign=\"top\">" );
	  EmuUtil.appendHTML( buf, maxApertureValue );
	  buf.append( "</td></tr>\n" );
	}
	if( subjectDistRange != null ) {
	  buf.append( "<tr><td valign=\"top\">Entfernungsmodus:</td>"
			+ "<td valign=\"top\">" );
	  EmuUtil.appendHTML( buf, subjectDistRange );
	  buf.append( "</td></tr>\n" );
	}
	if( subjectDistance != null ) {
	  buf.append( "<tr><td valign=\"top\">Abstand zum Motiv:</td>"
			+ "<td valign=\"top\">" );
	  EmuUtil.appendHTML( buf, subjectDistance );
	  buf.append( "</td></tr>\n" );
	}
      }

      // Gruppe GPS
      String gpsPosDegreeText  = exifData.getGPSPosDegreeText();
      String gpsPosNumericText = exifData.getGPSPosNumericText();
      if( gpsPosNumericText != null ) {
	if( buf.length() > posFirstRow ) {
	  buf.append( "<tr><td colspan=\"2\">&nbsp;</td</tr>\n" );
	}
	buf.append( "<tr><th align=\"left\" colspan=\"2\">GPS</th</tr>\n"
			+ "<tr><td valign=\"top\">Position:</td>"
			+ "<td valign=\"top\">" );
	if( gpsPosDegreeText != null ) {
	  EmuUtil.appendHTML( buf, gpsPosDegreeText );
	  buf.append( "<br/>" );
	}
	EmuUtil.appendHTML( buf, gpsPosNumericText );
	buf.append( "</td></tr>\n"
		+ "<tr><td valign=\"top\">H&ouml;he:</td>"
	      + "<td valign=\"top\">" );
	EmuUtil.appendHTML( buf, exifData.getGPSAltitudeText() );
	buf.append( "</td></tr>\n" );
      }

      // Gruppe Sonstiges
      String digitalZoom  = exifData.getDigitalZoomText();
      String contrast     = exifData.getContrast();
      String saturation   = exifData.getSaturation();
      String sharpness    = exifData.getSharpness();
      String whiteBalance = exifData.getWhiteBalanceText();
      String exifVersion  = exifData.getExifVersion();
      if( (digitalZoom != null)
	  && (contrast != null)
	  && (saturation != null)
	  && (sharpness != null)
	  && (whiteBalance != null)
	  && (exifVersion != null) )
      {
	if( buf.length() > posFirstRow ) {
	  buf.append( "<tr><td colspan=\"2\">&nbsp;</td</tr>\n" );
	}
	buf.append( "<tr><th align=\"left\" colspan=\"2\">"
			+ "Sonstiges</th</tr>\n" );
	if( digitalZoom != null ) {
	  buf.append( "<tr><td valign=\"top\">Digitalzoom:</td>"
			+ "<td valign=\"top\">" );
	  EmuUtil.appendHTML( buf, digitalZoom );
	  buf.append( "</td></tr>\n" );
	}
	if( contrast != null ) {
	  buf.append( "<tr><td valign=\"top\">Kontrast:</td>"
			+ "<td valign=\"top\">" );
	  EmuUtil.appendHTML( buf, contrast );
	  buf.append( "</td></tr>\n" );
	}
	if( saturation != null ) {
	  buf.append( "<tr><td valign=\"top\">S&auml;ttigung:</td>"
			+ "<td valign=\"top\">" );
	  EmuUtil.appendHTML( buf, saturation );
	  buf.append( "</td></tr>\n" );
	}
	if( sharpness != null ) {
	  buf.append( "<tr><td valign=\"top\">Sch&auml;rfe:</td>"
			+ "<td valign=\"top\">" );
	  EmuUtil.appendHTML( buf, sharpness );
	  buf.append( "</td></tr>\n" );
	}
	if( whiteBalance != null ) {
	  buf.append( "<tr><td valign=\"top\">Wei&szlig;abgleich:</td>"
			+ "<td valign=\"top\">" );
	  EmuUtil.appendHTML( buf, whiteBalance );
	  buf.append( "</td></tr>\n" );
	}
	if( exifVersion != null ) {
	  buf.append( "<tr><td valign=\"top\">EXIF-Version:</td>"
			+ "<td valign=\"top\">" );
	  EmuUtil.appendHTML( buf, exifVersion );
	  buf.append( "</td></tr>\n" );
	}
      }
    }
    if( buf.length() > posFirstRow ) {
      buf.append( "</table>\n" );
    } else {
      buf.setLength( posTableBeg );
      buf.append(
	"Keine Zusatzinformationen bzw. EXIF-Daten verf&uuml;gbar" );
    }
    buf.append( "</html>\n" );

    detailsPane.setContentType( "text/html" );
    detailsPane.setText( buf.toString() );
    try {
      detailsPane.setCaretPosition( 0 );
    }
    catch( IllegalArgumentException ex ) {}


    // Listener
    this.btnClose.addActionListener( this );


    // Farben ermitteln
    (new Thread(
		Main.getThreadGroup(),
		this,
		"JKCEMU imageviewer color counter" )).start();
  }


	/* --- private Methoden --- */

  private void colorsChecked()
  {
    fillGeneralPane( this.totalColors.size(), this.visibleColors.size() );
  }


  private void fillGeneralPane( int nTotalColors, int nVisibleColors )
  {
    StringBuilder buf = new StringBuilder( 0x400 );
    buf.append( "<html>\n"
		+ "<table border=\"0\">\n"
		+ "<tr><td valign=\"top\">Datei:</td>"
		+ "<td valign=\"top\">" );
    if( this.file != null ) {
      EmuUtil.appendHTML( buf, this.file.getPath() );
    }
    buf.append( "</td></tr>\n"
		+ "<tr><td valign=\"top\">Dateigr&ouml;&szlig;e:</td>"
		+ "<td valign=\"top\">" );
    if( this.file != null ) {
      long fileSize = this.file.length();
      if( fileSize > 0) {
	EmuUtil.appendHTML(
		buf, EmuUtil.formatSize( fileSize, false, true ) );
      }
    }
    buf.append( "</td></tr>\n"
		+ "<tr><td valign=\"top\">Bildgr&ouml;&szlig;e:</td>"
		+ "<td valign=\"top\">" );
    EmuUtil.appendHTML( buf, EmuUtil.formatInt( this.wImg ) );
    buf.append( " x " );
    EmuUtil.appendHTML( buf, EmuUtil.formatInt( this.hImg ) );
    buf.append( " = " );
    EmuUtil.appendHTML( buf, EmuUtil.formatInt( this.wImg * this.hImg ) );
    buf.append( " Pixel</td></tr>\n"
		+ "<tr><td valign=\"top\">Anzahl Farben:</td>"
		+ "<td valign=\"top\">" );
    EmuUtil.appendHTML(
		buf,
		nTotalColors > 0 ?
			EmuUtil.formatInt( nTotalColors )
			: TEXT_IS_DETERMINING );
    buf.append( "</td></tr>\n"
		+ "<tr><td valign=\"top\">Farbpalette:</td>"
		+ "<td valign=\"top\">" );
    if( this.nIndexedColors > 0 ) {
      EmuUtil.appendHTML( buf, EmuUtil.formatInt( this.nIndexedColors ) );
      buf.append( " indexierte Farben" );
    } else {
      switch( this.image.getType() ) {
	case BufferedImage.TYPE_USHORT_555_RGB:
	  EmuUtil.appendHTML( buf, EmuUtil.formatInt( 32 * 32 * 32 ) );
	  buf.append( " Farben" );
	  break;
	case BufferedImage.TYPE_USHORT_565_RGB:
	  EmuUtil.appendHTML( buf, EmuUtil.formatInt( 32 * 64 * 32 ) );
	  buf.append( " Farben" );
	  break;
	case BufferedImage.TYPE_USHORT_GRAY:
	  buf.append( "256 Graustufen" );
	  break;
	default:
	  buf.append( "16,7 Mio Farben" );
      }
      buf.append( " (nicht indexiert)" );
    }
    buf.append( "</td></tr>\n"
		+ "<tr><td valign=\"top\">Transparenz:</td>"
		+ "<td valign=\"top\">" );
    if( this.image.getTransparency() == Transparency.OPAQUE ) {
      buf.append( "keine" );
    } else {
      if( (this.nTransparentPixels >= 0)
	  && (this.nTranslucentPixels >= 0) )
      {
	if( (this.nTransparentPixels > 0)
	    || (this.nTranslucentPixels > 0) )
	{
	  float total = (float) (this.wImg * this.hImg);
	  EmuUtil.appendHTML(
		buf,
		EmuUtil.formatInt( this.nTransparentPixels ) );
	  buf.append( " Pixel (" );
	  EmuUtil.appendHTML(
		buf,
		toPercent( (float) this.nTransparentPixels / total ) );
	  buf.append( "%)" );
	  if( this.nTranslucentPixels > 0 ) {
	    buf.append( " voll- und " );
	    EmuUtil.appendHTML(
		buf,
		EmuUtil.formatInt( this.nTranslucentPixels ) );
	    buf.append( " Pixel (" );
	    EmuUtil.appendHTML(
		buf,
		toPercent( (float) this.nTranslucentPixels / total ) );
	    buf.append( "%) teiltransparent" );
	  } else {
	    buf.append( " volltransparent" );
	  }
	} else {
	  buf.append( TEXT_IS_DETERMINING );
	}
      }
    }
    buf.append( "</td></tr>\n"
		+ "</table>\n"
		+ "</html>\n" );

    this.generalPane.setContentType( "text/html" );
    this.generalPane.setText( buf.toString() );
    try {
      this.generalPane.setCaretPosition( 0 );
    }
    catch( IllegalArgumentException ex ) {}
  }


  private static String toPercent( float value )
  {
    String pattern = "##0.0";
    if( Math.round( value * 100F ) == 0 ) {
      pattern += "##";
    }
    NumberFormat numFmt = NumberFormat.getNumberInstance();
    if( numFmt instanceof DecimalFormat ) {
      ((DecimalFormat) numFmt).applyPattern( pattern );
    }
    return numFmt.format( value * 100F );
  }
}
