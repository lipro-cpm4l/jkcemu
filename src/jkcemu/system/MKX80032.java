/*
 * (c) 2009 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Emulation des MKX80032
 */

package jkcemu.system;

import java.awt.*;
import java.awt.event.*;
import java.lang.*;
import java.util.*;
import jkcemu.base.*;
import z80emu.*;


public class MKX80032 extends EmuSys
{
  private int               bootStep;
  private Font              font;
  private javax.swing.Timer timer;


  public MKX80032( EmuThread emuThread, Properties props )
  {
    super( emuThread, props );
    reset( EmuThread.ResetLevel.POWER_ON, props );
    this.font  = new Font( "Monospaced", Font.BOLD, 12 );
    this.timer = new javax.swing.Timer(
			3000,
			new ActionListener()
			{
			  public void actionPerformed( ActionEvent e )
			  {
			    incBootStep();
			  }
			} );
    this.timer.start();
  }


  public static int getDefaultSpeedKHz()
  {
    return 16000;
  }


	/* --- ueberschriebene Methoden --- */

  public void die()
  {
    this.timer.stop();
  }


  public int getBorderColorIndex()
  {
    return this.bootStep < 34 ? BLACK : 2;
  }


  public Color getColor( int colorIdx )
  {
    Color color = Color.black;
    switch( colorIdx ) {
      case 1:
        color = this.colorWhite;
        break;

      case 2:
        color = Color.lightGray;
        break;

      case 3:
        color = Color.blue;
        break;

      case 4:
        color = Color.red;
        break;
    }
    return color;
  }


  public int getColorCount()
  {
    return 4;
  }


  public int getMemByte( int addr )
  {
    return 0;
  }


  public int getScreenHeight()
  {
    return 384;		// 12*32
  }


  public int getScreenWidth()
  {
    return 640;
  }


  public String getTitle()
  {
    return "MKX-80032";
  }


  public boolean paintScreen( Graphics g, int x, int y, int screenScale )
  {
    if( this.bootStep < 34 ) {
      g.setColor( this.colorWhite );
      g.setFont( this.font );
      y += 12;
      g.drawString( "MKX-80032 BIOS", x, y );
      if( this.bootStep > 0 ) {
	y += 24;
	g.drawString( " 8 MByte RAM", x, y );
	y += 12;
	g.drawString( "12 MByte optischer RAM", x, y );
	y += 12;
	g.drawString( "20 MByte MFM-Festplatte", x, y );
      }
      if( this.bootStep > 1 ) {
	y += 24;
	g.drawString( "Boote von MFM-Festplatte:", x, y );
      }
      if( this.bootStep > 2 ) {
	y += 24;
	g.drawString( "MILTANOS Version 2.3, 16. November 1988", x, y );
      }
      if( this.bootStep > 3 ) {
	y += 24;
	g.drawString( "Lade modularen Multi-Prozessor-Kernel:", x, y );
	y += 12;
	g.drawString( "CPU: 4x U80032 / 16 MHz gefunden", x, y );
      }
      if( this.bootStep > 4 ) {
	y += 24;
	g.drawString( "Initialisiere Zufallsgenerator...", x, y );
	if( this.bootStep > 5 ) {
	  g.drawString(
		"Initialisiere Zufallsgenerator..........................OK",
		x,
		y );
	}
      }
      if( this.bootStep > 6 ) {
	y += 12;
	g.drawString( "Initialisiere optische Festplatte...", x, y );
	if( this.bootStep > 7 ) {
	  g.drawString(
		"Initialisiere optische Festplatte.......................OK",
		x,
		y );
	}
      }
      if( this.bootStep > 8 ) {
	y += 12;
	g.drawString( "Initialisiere Ethernet...", x, y );
	if( this.bootStep > 9 ) {
	  g.drawString(
		"Initialisiere Ethernet..................................OK",
		x, y );
	}
      }
      if( this.bootStep > 10 ) {
	y += 12;
	g.drawString( "Initialisiere optisches Laser-Netzwerk...", x, y );
	if( this.bootStep > 11 ) {
	  g.drawString(
		"Initialisiere optisches Laser-Netzwerk..................nicht gefunden",
		x, y );
	}
      }
      if( this.bootStep > 12 ) {
	y += 12;
	g.drawString(
		"Initialisiere seriellen Hochgeschwindigkeitsbus HSSB2...",
		x,
		y );
	if( this.bootStep > 13 ) {
	  g.drawString(
		"Initialisiere seriellen Hochgeschwindigkeitsbus HSSB2...OK",
		x, y );
	}
      }
      if( this.bootStep > 14 ) {
	y += 12;
	g.drawString(
		"Aktiviere Transputer-Schnittstelle...",
		x,
		y );
	if( this.bootStep > 15 ) {
	  g.drawString(
		"Aktiviere Transputer-Schnittstelle......................OK",
		x, y );
	}
      }
      if( this.bootStep > 16 ) {
	y += 12;
	g.drawString(
		"Aktiviere TLMK-Verschl\u00FCsselung...",
		x,
		y );
	if( this.bootStep > 17 ) {
	  g.drawString(
		"Aktiviere TLMK-Verschl\u00FCsselung..........................OK",
		x, y );
	}
      }
      if( this.bootStep > 18 ) {
	y += 12;
	g.drawString(
		"Aktiviere Fuzzy Logic Modul...",
		x,
		y );
	if( this.bootStep > 19 ) {
	  g.drawString(
		"Aktiviere Fuzzy Logic Modul.............................nicht gefunden",
		x, y );
	}
      }
      if( this.bootStep > 20 ) {
	y += 12;
	g.drawString(
		"Aktiviere Mehrbenutzermodus...",
		x,
		y );
	if( this.bootStep > 21 ) {
	  g.drawString(
		"Aktiviere Mehrbenutzermodus.............................OK",
		x, y );
	}
      }
      if( this.bootStep > 22 ) {
	y += 12;
	g.drawString(
		"Aktiviere Grafik-Virtualisierung...",
		x,
		y );
	if( this.bootStep > 23 ) {
	  g.drawString(
		"Aktiviere Grafik-Virtualisierung........................OK",
		x, y );
	}
      }
      if( this.bootStep > 24 ) {
	y += 12;
	g.drawString(
		"Aktiviere Netzwerk Transportebene..",
		x,
		y );
	if( this.bootStep > 25 ) {
	  g.drawString(
		"Aktiviere Netzwerk Transportebene.......................OK",
		x, y );
	}
      }
      if( this.bootStep > 26 ) {
	y += 12;
	g.drawString(
		"Aktiviere Netzwerk Applikationsebene..",
		x,
		y );
	if( this.bootStep > 27 ) {
	  g.drawString(
		"Aktiviere Netzwerk Applikationsebene....................OK",
		x, y );
	}
      }
      if( this.bootStep > 28 ) {
	y += 12;
	g.drawString(
		"Aktiviere Netzwerk Einbrucherkennung..",
		x,
		y );
	if( this.bootStep > 29 ) {
	  g.drawString(
		"Aktiviere Netzwerk Einbrucherkennung....................OK",
		x, y );
	}
      }
      if( this.bootStep > 30 ) {
	y += 12;
	g.drawString(
		"Starte grafische Oberfl\u00E4che...",
		x,
		y );
      }
    } else if( this.bootStep < 38 ) {
      g.setColor( Color.blue );
      g.setFont( new Font( "SansSerif", Font.BOLD | Font.ITALIC, 18 ) );
      FontMetrics fm = g.getFontMetrics();
      if( fm != null ) {
	String s = "Starte 3D-Oberfl\u00E4che...";
	g.drawString( s, x + ((640 - fm.stringWidth( s )) / 2), y + 192 );
	s = "Bitte warten!";
	g.drawString( s, x + ((640 - fm.stringWidth( s )) / 2), y + 208 );
      }
    } else {
      g.setColor( Color.red );
      g.setFont( new Font( "SansSerif", Font.BOLD, 100 ) );
      g.drawString( "APRIL!", x, y + 110 );
      g.drawString( "APRIL!", x + 100, y + 220 );
      g.setFont( new Font( "SansSerif", Font.BOLD, 18 ) );
      g.drawString(
		"Den MKX-80032-Computer hat es nie gegeben!",
		x + 100,
		y + 340 );
      g.setFont( this.font );
      g.drawString( "1. April 2009", x + 500, y + 380 );
    }
    return true;
  }


  /*
   * Ein RESET ist erforderlich, wenn sich das emulierte System aendert.
   */
  public boolean requiresReset( Properties props )
  {
    return !EmuUtil.getProperty(
			props,
			"jkcemu.system" ).startsWith( "MKX80032" );
  }


  public void reset( EmuThread.ResetLevel resetLevel, Properties props )
  {
    this.bootStep = 0;
  }


  public boolean setMemByte( int addr, int value )
  {
    return false;
  }


	/* --- private Methoden --- */

  private void incBootStep()
  {
    if( this.bootStep < 40 ) {
      this.bootStep++;
      this.screenFrm.setScreenDirty( true );
    }
  }
}

