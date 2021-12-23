/*
 * (c) 2008-2021 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Anzeige der Hilfetexte
 */

package jkcemu.base;

import java.awt.EventQueue;
import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EmptyStackException;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeSet;
import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JToolBar;
import javax.swing.JViewport;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.Document;
import jkcemu.Main;


public class HelpFrm extends HTMLViewFrm implements HyperlinkListener
{
  public static final String PAGE_HOME    = "/help/home.htm";
  public static final String PAGE_INDEX   = "/help/index.htm";
  public static final String PAGE_LICENSE = "/help/license.htm";

  private static final String TEXT_FIND_IN_HELP = "Hilfe durchsuchen";

  private static final Map<String,String> page2Text  = new HashMap<>();
  private static final Map<String,String> page2Title = new HashMap<>();
  private static final Map<String,String> page2URL   = new HashMap<>();


  private static class PageStackEntry
  {
    private String htmlText;
    private URL    url;
    private String findOnNextPage;
    private Double viewPos;	// relative Angabe von 0.0 bis 1.0

    private PageStackEntry(
			String htmlText,
			URL    url,
			String findOnNextPage )
    {
      this.htmlText       = htmlText;
      this.url            = url;
      this.findOnNextPage = findOnNextPage;
      this.viewPos        = null;
    }
  };


  private static HelpFrm instance = null;

  private String                lastFindInHelpText;
  private String                textToFind;
  private Double                posToScroll;
  private javax.swing.Timer     jumpToTimer;
  private URL                   urlHome;
  private Stack<PageStackEntry> pageStack;
  private JMenuItem             mnuCopyPage;
  private JMenuItem             mnuCopyPageWithLinks;
  private JMenuItem             mnuNavBack;
  private JMenuItem             mnuNavHome;
  private JButton               btnBack;
  private JButton               btnHome;
  private JButton               btnPrint;


  public static void openFindInHelp()
  {
    EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    openFindInHelpInternal();
		  }
		} );
  }


  /*
   * Das Oeffnen bzw. Anzeigen des Fensters erfolgt erst im naechsten
   * Event-Verarbeitungszyklus, damit es keine Probleme gibt,
   * wenn die Methode aus einem modalen Dialog heraus aufgerufen werden.
   */
  public static void openPage( final String page )
  {
    fireOpenInternal( null, page, null );
  }


	/* --- HyperlinkListener --- */

  @Override
  public void hyperlinkUpdate( HyperlinkEvent e )
  {
    if( e.getEventType() == HyperlinkEvent.EventType.ACTIVATED ) {
      String findText = null;
      if( !this.pageStack.isEmpty() ) {
	try {
	  findText = this.pageStack.peek().findOnNextPage;
	}
	catch( EmptyStackException ex ) {}
      }
      setPage( true, null, e.getURL(), null, null, findText );
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doAction( EventObject e )
  {
    boolean rv = false;
    if( e != null ) {
      Object src = e.getSource();
      if( src == this.btnPrint ) {
	rv = true;
	doPrint();
      }
      else if( src == this.mnuCopyPage ) {
	doCopyPage( true );
      }
      else if( src == this.mnuCopyPageWithLinks ) {
	doCopyPage( false );
      }
      else if( (src == this.mnuNavBack) || (src == this.btnBack) ) {
	rv = true;
	doBack();
      }
      else if( (src == this.mnuNavHome) || (src == this.btnHome) ) {
	rv = true;
	setPage( true, null, null, null, null, null );
      }
      else if( src == this.jumpToTimer ) {
	rv = true;
	this.jumpToTimer.stop();
	doScrollTo( this.posToScroll );
	this.posToScroll = null;
	doFind( this.textToFind, false );
	this.textToFind = null;
      }
    }
    if( !rv ) {
      super.doAction( e );
    }
    return rv;
  }


  @Override
  protected void doFind2()
  {
    setPage(
	true,
	null,
	HelpFrm.class.getResource( PAGE_HOME ),
	null,
	null,
	null );

    String findText = showFindDlg(
				TEXT_FIND_IN_HELP,
				this.lastFindInHelpText );
    if( findText != null ) {
      int findLen = findText.length();
      if( findLen > 0 ) {
	this.lastFindInHelpText = findText;

	// beim ersten mal Hilfeseiten laden
	if( page2Text.isEmpty() ) {
	  try {
	    SortedSet<String> remainPages    = new TreeSet<>();
	    SortedSet<String> processedPages = new TreeSet<>();

	    /*
	     * Start- und Index-Seite laden,
	     * um die Links zu den weiteren Seiten zu sammeln
	     */
	    loadPageIntoFindCache(
				HelpFrm.PAGE_HOME,
				remainPages,
				processedPages );
	    loadPageIntoFindCache(
				HelpFrm.PAGE_INDEX,
				remainPages,
				processedPages );

	    /*
	     * Start- und Index-Seite selbst sollen nicht
	     * als Ergebnisse erscheinen
	     */
	    remainPages.remove( HelpFrm.PAGE_HOME );
	    remainPages.remove( HelpFrm.PAGE_INDEX );
	    page2Text.clear();
	    page2Title.clear();
	    page2URL.clear();

	    // restliche Seiten laden
	    while( !remainPages.isEmpty() ) {
	      loadPageIntoFindCache(
				remainPages.last(),
				remainPages,
				processedPages );
	    }
	  }
	  catch( Exception ex ) {}
	}

	// Suchtext aufbereiten
	StringBuilder findBuf = new StringBuilder( findLen + 16 );
	EmuUtil.appendHTML( findBuf, findText );
	toLowerCase( findBuf );
	findText = replacesMultiSpaces( findBuf ).toString();

	// eigentlich Suche
	final Map<String,Integer> page2Hits = new HashMap<>();
	for( String page : page2Text.keySet() ) {
	  findInCachedPage( page, findText, page2Hits );
	}

	// Ergebnis anzeigen
	StringBuilder resultBuf = new StringBuilder( 0x1000 );
	resultBuf.append( "<html>\n"
			+ "<body>\n"
			+ "<h1>Hilfe durchsuchen</h1>\n" );
	if( page2Hits.isEmpty() ) {
	  resultBuf.append( "Zum Suchtext <em>" );
	  EmuUtil.appendHTML( resultBuf, this.lastFindInHelpText );
	  resultBuf.append( "</em> wurden keine Treffer gefunden.\n" );
	} else {
	  java.util.List<String> pages = new ArrayList<>();
	  pages.addAll( page2Hits.keySet() );
	  resultBuf.append( "Zum Suchtext <em>" );
	  EmuUtil.appendHTML( resultBuf, this.lastFindInHelpText );
	  resultBuf.append( "</em>" );
	  if( pages.size() == 1 ) {
	    resultBuf.append( " wurde folgender Treffer gefunden:\n" );
	  } else {
	    resultBuf.append( " wurden folgende Treffer gefunden,\n"
			+ "absteigend sortiert nach Anzahl der Treffer:\n" );
	  }
	  resultBuf.append( "<ul>\n" );

	  // Ergebnisse nach Anzahl Treffer sortieren
	  try {
	    Collections.sort(
		pages,
		new Comparator<String>()
		{
		  @Override
		  public int compare( String page1, String page2 )
		  {
		    int rv = -1;
		    /*
		     * zuerst nach Anzahl Treffer absteigend sortieren
		     */
		    Integer hits1 = page2Hits.get( page1 );
		    Integer hits2 = page2Hits.get( page2 );
		    if( (hits1 != null) && (hits2 != null) ) {
		      rv = hits2.intValue() - hits1.intValue();
		      if( rv == 0 ) {
			/*
			 * wenn Anzahl Treffer gleich,
			 * dann nach Titel sortieren
			 */
			String t1 = page2Title.get( page1 );
			String t2 = page2Title.get( page2 );
			if( (t1 != null) && (t2 != null) ) {
			  rv = t1.compareTo( t2 );
			}
		      }
		    }
		    return rv;
		  }

		  @Override
		  public boolean equals( Object o )
		  {
		    return (o == this);
		  }
		} );
	  }
	  catch( ClassCastException ex ) {}

	  // Ergebnisse ausgeben
	  boolean multiHits = false;
	  for( String page : pages ) {
	    String  url   = page2URL.get( page );
	    String  title = page2Title.get( page );
	    Integer hits  = page2Hits.get( page );
	    if( (url != null) && (title != null) && (hits != null) ) {
	      resultBuf.append( "<li><a href=\"" );
	      resultBuf.append( url );
	      resultBuf.append( "\">" );
	      resultBuf.append( title );
	      resultBuf.append( "</a>&nbsp;&nbsp;" );
	      EmuUtil.appendHTML( resultBuf, hits.toString() );
	      resultBuf.append( "&nbsp;Treffer</li>\n" );
	      if( hits.intValue() > 1 ) {
		multiHits = true;
	      }
	    }
	  }
	  resultBuf.append( "</ul>\n"
			+ "<br/>\n"
			+ "Wenn Sie auf " );
	  if( pages.size() == 1 ) {
	    resultBuf.append( "diesen Link" );
	  } else {
	    resultBuf.append( "einen dieser Links" );
	  }
	  resultBuf.append( " klicken,\n"
			+ "wird auf der folgenden Seite automatisch" );
	  if( multiHits ) {
	    resultBuf.append( " zum ersten Treffer gesprungen.\n"
			+ "Mit dem Men&uuml;eintrag <em>Weitersuchen</em>"
			+ " oder der Taste&nbsp;<em>F3</em>"
			+ " gelangen Sie zum n&auml;chsten Treffer.\n" );
	  } else {
	    resultBuf.append( " zu dem Treffer gesprungen.\n" );
	  }
	}
	resultBuf.append( "</body\n"
			+ "</html>\n" );
	fireOpenInternal(
			resultBuf.toString(),
			null,
			this.lastFindInHelpText );
      }
    }
  }


	/* --- Konstruktor --- */

  private HelpFrm()
  {
    setTitle( "JKCEMU Hilfe" );
    this.lastFindInHelpText = null;
    this.posToScroll        = null;
    this.textToFind         = null;
    this.jumpToTimer        = new javax.swing.Timer( 500, this );
    this.pageStack          = new Stack<>();
    this.urlHome            = getClass().getResource( "/help/home.htm" );


    // Menu
    JMenu mnuFile = createMenuFile();

    this.mnuCopyPage = createMenuItem(
			"Hilfeseite ohne Hypertext-Links kopieren" );
    mnuFile.add( this.mnuCopyPage );

    this.mnuCopyPageWithLinks = createMenuItem(
			"Hilfeseite mit Hypertext-Links kopieren" );
    mnuFile.add( this.mnuCopyPageWithLinks );
    mnuFile.addSeparator();

    JMenu mnuNav = GUIFactory.createMenu( "Navigation" );
    mnuNav.setMnemonic( KeyEvent.VK_N );

    this.mnuNavBack = createMenuItemWithStandardAccelerator(
						"Zur\u00FCck",
						KeyEvent.VK_B );
    mnuNav.add( this.mnuNavBack );

    this.mnuNavHome = createMenuItemWithStandardAccelerator(
						"Startseite",
						KeyEvent.VK_H );
    mnuNav.add( this.mnuNavHome );

    createMenuBar(
		mnuFile,
		"Seite durchsuchen...",
		"Hilfe durchsuchen...",
		mnuNav,
		null,
		null );


    // Fensterinhalt
    setLayout( new BorderLayout() );


    // Werkzeugleiste
    JToolBar toolBar = GUIFactory.createToolBar();
    toolBar.setFloatable( false );
    toolBar.setBorderPainted( false );
    toolBar.setOrientation( JToolBar.HORIZONTAL );
    toolBar.setRollover( true );

    this.btnBack = GUIFactory.createRelImageResourceButton(
					this,
					"nav/back.png",
					this.mnuNavBack.getText() );
    this.btnBack.addActionListener( this );
    toolBar.add( this.btnBack );

    this.btnHome = GUIFactory.createRelImageResourceButton(
					this,
					"nav/home.png",
					this.mnuNavHome.getText() );
    this.btnHome.addActionListener( this );
    toolBar.add( this.btnHome );
    toolBar.addSeparator();

    this.btnPrint = GUIFactory.createRelImageResourceButton(
					this,
					"file/print.png",
					EmuUtil.TEXT_OPEN_PRINT );
    this.btnPrint.addActionListener( this );
    toolBar.add( this.btnPrint );

    add( toolBar, java.awt.BorderLayout.NORTH );


    // Anzeigebereich
    createEditorPane( BorderLayout.CENTER );
    this.editorPane.addCaretListener( this );
    this.editorPane.addHyperlinkListener( this );


    // Fenstergroesse
    setResizable( true );
    if( !applySettings( Main.getProperties() ) ) {
      setBoundsToDefaults();
    }
  }


	/* --- private Methoden --- */

  private void doBack()
  {
    if( this.pageStack.size() > 1 ) {
      try {
	this.pageStack.pop();	// aktuelle Seite vom Stack entfernen
	PageStackEntry entry = this.pageStack.pop();
	setPage(
		false,
		entry.htmlText,
		entry.url,
		entry.viewPos,
		entry.findOnNextPage,
		null );
      }
      catch( EmptyStackException ex ) {}
    }
  }


  private void doCopyPage( boolean removeLinks )
  {
    try {
      URL url = this.urlHome;
      if( !this.pageStack.isEmpty() ) {
	url = pageStack.peek().url;
      }
      String htmlText = this.editorPane.getText();
      if( htmlText != null ) {
	if( removeLinks ) {
	  StringBuilder buf  = new StringBuilder( htmlText );
	  int           idx1 = 0;
	  for(;;) {
	    idx1 = buf.indexOf( "<a href=", idx1 );
	    if( idx1 < 0 ) {
	      break;
	    }
	    int idx2 = buf.indexOf( ">", idx1 );
	    if( idx2 > idx1 ) {
	      int idx3 = buf.indexOf( "</a>", idx2 + 1 );
	      if( idx3 > idx2 ) {
		buf = buf.delete( idx3, idx3 + 4 );
		buf = buf.delete( idx1, idx2 + 1 );
	      } else {
		idx1 = idx2 + 1;
	      }
	    } else {
	      idx1++;
	    }
	  }
	  htmlText = buf.toString();
	}
	TransferableHTML t = new TransferableHTML( htmlText );
	EmuUtil.getClipboard( this ).setContents( t, t );
      }
    }
    catch( Exception ex ) {
      BaseDlg.showErrorDlg(
		this,
		"Die Seite konnte nicht nicht die Zwischenablage"
			+ " kopiert werden." );
    }
  }


  private void doScrollTo( Double viewPos )
  {
    if( viewPos != null ) {
      double d = viewPos.doubleValue();
      int    h = this.editorPane.getHeight();
      if( (d > 0.0) && (d <= 1.0) && (h > 0) ) {
	JViewport vp = this.scrollPane.getViewport();
	if( vp != null ) {
	  vp.setViewPosition(
		new Point( 0, (int) Math.round( d * (double) h ) ) );
	}
      }
    }
  }


  private static void findInCachedPage(
				String              page,
				String              findText,
				Map<String,Integer> page2Hits )
  {
    String baseText = page2Text.get( page );
    if( baseText != null ) {
      int hits    = 0;
      int findLen = findText.length();
      int baseLen = baseText.length();
      int pos     = 0;
      while( pos < baseLen ) {
	int foundAt = baseText.indexOf( findText, pos );
	if( foundAt < 0 ) {
	  break;
	}
	hits++;
	pos = foundAt + findLen;
      }
      if( hits > 0 ) {
	page2Hits.put( page, hits );
      }
    }
  }


  private static void fireOpenInternal(
				final String htmlText,
				final String page,
				final String findText )
  {
    EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    openInternal( htmlText, page, findText );
		  }
		} );
  }


  private void loadPageIntoFindCache(
			String      page,
			Set<String> remainPages,
			Set<String> processedPages )
  {
    remainPages.remove( page );
    if( !processedPages.contains( page ) ) {
      processedPages.add( page );
      InputStream in = null;
      try {
	URL url = getClass().getResource( page );
	if( url != null ) {
	  in = url.openStream();

	  String        h1          = null;
	  String        title       = null;
	  StringBuilder elemBuf     = new StringBuilder( 128 );
	  StringBuilder textBuf     = new StringBuilder( 0x2000 );
	  boolean       withinElem  = false;
	  boolean       withinQuote = false;
	  int           h1BegPos    = -1;
	  int           titleBegPos = -1;

	  /*
	   * da die Hilfe-Dateien reine ASCII-Dateien sind,
	   * koennen sie ohne Byte-Character-Mapping gelesen werden.
	   */
	  in    = new BufferedInputStream( in );
	  int b = in.read();
	  while( b >= 0 ) {
	    if( b == '<' ) {
	      elemBuf.setLength( 0 );
	      withinElem  = true;
	      withinQuote = false;
	    } else if( b == '>' ) {
	      int len = elemBuf.length();
	      if( len > 0 ) {
		String elem = elemBuf.toString().trim().toLowerCase();
		if( elem.startsWith( "a href=\"" ) ) {
		  int pos1 = elemBuf.indexOf( "\"" );
		  if( pos1 >= 0 ) {
		    pos1++;
		    if( pos1 < len ) {
		      int pos2 = elemBuf.indexOf( "\"", pos1 );
		      if( pos2 > (pos1 + 1) ) {
			String href = resolve(
					elemBuf.substring( pos1, pos2 ),
					page );
			if( (href != null)
			    && !processedPages.contains( href ) )
			{
			  remainPages.add( href );
			}
		      }
		    }
		  }
		} else if( elem.equals( "title" ) ) {
		  titleBegPos = textBuf.length();
		} else if( elem.equals( "/title" ) ) {
		  if( (titleBegPos >= 0)
		      && (titleBegPos < textBuf.length()) )
		  {
		    title = textBuf.substring( titleBegPos );
		  }
		  titleBegPos = -1;
		} else if( elem.equals( "h1" ) ) {
		  h1BegPos = textBuf.length();
		} else if( elem.equals( "/h1" ) ) {
		  if( (h1BegPos >= 0) && (h1BegPos < textBuf.length()) ) {
		    h1 = textBuf.substring( h1BegPos );
		  }
		  h1BegPos = -1;
		} else if( !elem.equals( "h2" ) && !elem.equals( "/h2" )
			   && !elem.equals( "h3" ) && !elem.equals( "/h3" )
			   && !elem.equals( "h4" ) && !elem.equals( "/h4" )
			   && !elem.equals( "b" ) && !elem.equals( "/b" )
			   && !elem.equals( "i" ) && !elem.equals( "/i" )
			   && !elem.equals( "u" ) && !elem.equals( "/u" ) )
		{
		  textBuf.append( '\u0020' );
		}
	      }
	      elemBuf.setLength( 0 );
	      withinElem  = false;
	      withinQuote = false;
	    } else {
	      char ch = (char) b;
	      if( withinElem ) {
		if( b == '\"' ) {
		  withinQuote = !withinQuote;
		} else {
		  if( !withinQuote ) {
		    ch = Character.toLowerCase( ch );
		  }
		}
		elemBuf.append( ch );
	      } else {
		textBuf.append( ch );
	      }
	    }
	    b = in.read();
	  }
	  if( textBuf.length() > 0 ) {
	    toLowerCase( textBuf );
	    textBuf = replaces( textBuf, "&nbsp", "\u0020" );
	    textBuf = replacesMultiSpaces( textBuf );
	    page2Text.put( page, textBuf.toString() );
	    page2URL.put( page, url.toString() );
	    if( title != null ) {
	      page2Title.put( page, title );
	    } else if( h1 != null ) {
	      page2Title.put( page, h1 );
	    }
	  }
	}
      }
      catch( IOException ex ) {}
      finally {
	EmuUtil.closeSilently( in );
      }
    }
  }


  private static void openFindInHelpInternal()
  {
    String page = null;
    if( instance == null ) {
      instance = new HelpFrm();
    }
    EmuUtil.showFrame( instance );
    instance.doFind2();
  }


  private static void openInternal(
				String htmlText,
				String page,
				String findText )
  {
    if( instance == null ) {
      instance = new HelpFrm();
    }
    EmuUtil.showFrame( instance );
    instance.setPage(
		true,
		htmlText,
		page != null ? HelpFrm.class.getResource( page ) : null,
		null,
		findText,
		null );
  }


  private static StringBuilder replaces(
					StringBuilder buf,
					String        pattern,
					String        replacement )
  {
    try {
      int pLen   = pattern.length();
      int bufLen = buf.length();
      int pos    = 0;
      while( pos < bufLen ) {
	int foundAt = buf.indexOf( pattern, pos );
	if( foundAt < 0 ) {
	  break;
	}
	buf = buf.replace( foundAt, foundAt + pLen, replacement );
	pos = foundAt + 1;
      }
    }
    catch( StringIndexOutOfBoundsException ex ) {
      // sollte nie vorkommen
    }
    return buf;
  }


  private static StringBuilder replacesMultiSpaces( StringBuilder buf )
  {
    return replaces( buf, "\u0020\u0020", "\u0020" );
  }


  private static String resolve( String href, String baseRes )
  {
    int pos = href.indexOf( '#' );
    if( pos >= 0 ) {
      href = href.substring( 0, pos );
    }
    if( !href.startsWith( "/" ) ) {
      if( href.startsWith( "./" ) ) {
	href = href.substring( 2 );
      }
      pos = baseRes.lastIndexOf( '/' );
      if( pos >= 0 ) {
	baseRes = baseRes.substring( 0, pos + 1 );
	while( href.startsWith( "../" ) ) {
	  int baseLen = baseRes.length();
	  if( baseLen < 2 ) {
	    href = null;
	    break;
	  }
	  pos = baseRes.lastIndexOf( '/', baseLen - 2 );
	  if( pos < 0 ) {
	    href = null;
	    break;
	  }
	  baseRes = baseRes.substring( 0, pos + 1 );
	  href    = href.substring( 3 );
	}
	href = baseRes + href;
      }
    }
    return href.isEmpty() ? null : href;
  }


  private static void toLowerCase( StringBuilder buf )
  {
    int len = buf.length();
    for( int i = 0; i < len; i++ ) {
      buf.setCharAt( i, Character.toLowerCase( buf.charAt( i ) ) );
    }
  }


  private void setPage(
		boolean saveCurViewPos,
		String  htmlText,
		URL     url,
		Double  viewPos,
		String  findTextToSave,
		String  findTextToUse )
  {
    if( (htmlText == null) && (url == null) ) {
      this.pageStack.clear();
      url = this.urlHome;
    }
    if( (htmlText != null) || (url != null) ) {

      /*
       * Seite nur anzeigen, wenn sie sich von der vorhergehenden
       * unterscheidet
       */
      boolean        alreadyVisible = false;
      PageStackEntry topEntry       = null;
      if( !this.pageStack.isEmpty() ) {
	topEntry = this.pageStack.peek();
	if( (htmlText != null) && (topEntry.htmlText != null) ) {
	  if( topEntry.htmlText.equals( htmlText ) ) {
	    alreadyVisible = true;
	  }
	} else if( (url != null) && (topEntry.url != null) ) {
	  if( topEntry.url.equals( url ) ) {
	    alreadyVisible = true;
	  }
	}
      }
      if( !alreadyVisible ) {
	try {

	  // aktuelle Position ermitteln und merken
	  if( saveCurViewPos && (topEntry != null) ) {
	    topEntry.viewPos = null;
	    int h = this.editorPane.getHeight();
	    if( h > 0 ) {
	      JViewport vp = this.scrollPane.getViewport();
	      if( vp != null ) {
		Point pt = vp.getViewPosition();
		if( pt != null ) {
		  double d = (double) pt.y / (double) h;
		  if( (d > 0.0) && (d <= 1.0) ) {
		    topEntry.viewPos = d;
		  }
		}
	      }
	    }
	  }

	  // neue Seite anzeigen
	  if( htmlText != null ) {
	    setHTMLText( htmlText );
	  } else if( url != null ) {
	    Document doc = this.editorPane.getDocument();
	    if( doc != null ) {
	      doc.putProperty( Document.StreamDescriptionProperty, null );
	    }
	    this.editorPane.setPage( url );
	  }

	  /*
	   * wenn Seite angezeigt werden konnte,
	   * dann neuen Stack-Eintrag erzeugen
	   */
	  this.pageStack.push(
		new PageStackEntry( htmlText, url, findTextToSave ) );

	  // auf Position bzw. Text suchen scrollen
	  if( (viewPos != null) || (findTextToUse != null) ) {
	    this.posToScroll = viewPos;
	    this.textToFind  = findTextToUse;
	    this.jumpToTimer.restart();
	  }

	  // Aktionsknoepfe aktualisieren
	  boolean stateBack = (this.pageStack.size() > 1);
	  this.mnuNavBack.setEnabled( stateBack );
	  this.btnBack.setEnabled( stateBack );

	  boolean stateHome = stateBack;
	  if( url != null ) {
	    stateHome |= !url.equals( this.urlHome );
	  } else {
	    stateHome = true;
	  }
	  this.mnuNavHome.setEnabled( stateHome );
	  this.btnHome.setEnabled( stateHome );

	  setContentActionFieldsEnabled( true );
	}
	catch( Exception ex ) {
	  BaseDlg.showErrorDlg(
		this,
		"Die Hilfeseite kann nicht angezeigt werden.\n\n"
			+ ex.getMessage() );
	}
      }
    }
  }
}
