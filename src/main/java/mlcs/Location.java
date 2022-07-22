/*
 * Beangle, Agile Development Scaffold and Toolkits.
 *
 * Copyright © 2005, The Beangle Software.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package mlcs;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;

/**
 * The location of a single character in all strings
 */
public class Location implements Serializable {
  private static final long serialVersionUID = -1561971028727693347L;

  public short[] index;// position information
  private int hashCode; // hash code
  byte status = (byte) 0; // 1 reserved, 0 unknown, -1 discard

  public boolean isUnknown() {
    return status == 0;
  }

  public boolean isDiscard() {
    return status == -1;
  }

  public void setReserved(boolean reserved) {
    status = reserved ? (byte) 1 : (byte) -1;
  }
//
//  public boolean reserved = true; // Shall we reserve it?

  /**
   * FIXME
   *
   * @param mlcs
   * @param limit
   * @param level
   * @return
   */
  public boolean canReach(Mlcs mlcs, Limit limit, short level) {
    int possible = mlcs.tailUpbound(index);
    return possible + level >= limit.mlcsLength;
  }

  //FIXME
  public short minIndex() {
    short m = Short.MAX_VALUE;
    for (short a : index) {
      if (a < m) m = a;
    }
    return m;
  }

  // FIXME
  public short maxIndex() {
    short m = 0;
    for (short a : index) {
      if (a > m) m = a;
    }
    return m;
  }

  public int sum(short level) {
    int sum = 0;
    for (short i : index) {
      short a = (short) (i - level);
      sum += a;
    }
    return sum;
  }

  public int score() {
    int sum = 0;
    short max = 0;
    for (short a : index) {
      if (a > max) max = a;
      sum += a;
    }
    return sum + max;
  }

  /**
   * less or equals <=
   * FIXME
   *
   * @param o
   * @return
   */
  public boolean priorOver(Location o) {
    for (int i = 0; i < index.length; i++) {
      if (index[i] > o.index[i]) {
        return false;
      }
    }
    return true;
  }

  /**
   * less <
   *
   * @param o
   * @return
   */
  public boolean morePriorOver(Location o) {
    for (int i = 0; i < index.length; i++) {
      if (index[i] >= o.index[i]) {
        return false;
      }
    }
    return true;
  }

  @Override
  public String toString() {
    String str = Arrays.toString(index);
    str = str.replaceAll(" ", "");
    return "(" + str.substring(1, str.length() - 1) + ")";
  }

  public Location(short[] index) {
    this.index = index;
    this.hashCode = buildHashCode();
  }

  private int buildHashCode() {
    int rs = 1;
    int i = 0;
    while (i < index.length) {
      rs = 31 * rs + index[i];
      i += 1;
    }
    return rs;
  }

  @Override
  public int hashCode() {
    return hashCode;
  }

  @Override
  public boolean equals(Object o) {
    Location bl = (Location) o;
    if (bl == this) return true;
    short[] b = bl.index;
    short[] a = index;
    int i = 0;
    boolean equals = true;
    while (i < a.length && equals) {
      if (a[i] != b[i]) {
        equals = false;
      }
      i += 1;
    }
    return equals;
  }

  public static class ScoreSorter implements Comparator<Location> {
    Mlcs mlcs;

    public ScoreSorter(Mlcs mlcs) {
      this.mlcs = mlcs;
    }

    public int compare(Location o1, Location o2) {
      if (!o1.isDiscard() && !o2.isDiscard()) {
        int tailCmp = mlcs.tailUpbound(o1.index) - mlcs.tailUpbound(o2.index);
        return (tailCmp == 0) ? o1.score() - o2.score() : -tailCmp;
      } else if (!o1.isDiscard() && o2.isDiscard()) {
        return -1;
      } else if (o1.isDiscard() && !o2.isDiscard()) {
        return 1;
      } else {
        return 0;
      }
    }
  }

}
