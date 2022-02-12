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
  public boolean reserved = true; // Shall we reserve it?

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
    public int compare(Location o1, Location o2) {
      return o1.score() - o2.score();
    }
  }

}
