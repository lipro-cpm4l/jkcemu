/*
 * (c) 2010 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Objektvergleicher anhand des per toString() zurueckgelieferten Textes
 */

package jkcemu.base;

import java.lang.*;
import java.util.Comparator;


public class ObjectByStringComparator implements Comparator<Object>
{
  private static ObjectByStringComparator instance           = null;
  private static ObjectByStringComparator ignoreCaseInstance = null;

  private boolean ignoreCase;


  public static ObjectByStringComparator getInstance()
  {
    if( instance == null ) {
      instance = new ObjectByStringComparator( false );
    }
    return instance;
  }


  public static ObjectByStringComparator getIgnoreCaseInstance()
  {
    if( ignoreCaseInstance == null ) {
      ignoreCaseInstance = new ObjectByStringComparator( true );
    }
    return ignoreCaseInstance;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public int compare( Object o1, Object o2 )
  {
    String s1 = null;
    String s2 = null;
    if( o1 != null ) {
      s1 = o1.toString();
    }
    if( o2 != null ) {
      s2 = o2.toString();
    }
    if( s1 == null ) {
      s1 = "";
    }
    if( s2 == null ) {
      s2 = "";
    }
    return this.ignoreCase ?
		s1.compareToIgnoreCase( s2 )
		: s1.compareTo( s2 );
  }


  @Override
  public boolean equals( Object o )
  {
    return o == this;
  }


	/* --- private Konstruktoren --- */

  private ObjectByStringComparator( boolean ignoreCase )
  {
    this.ignoreCase = ignoreCase;
  }
}
