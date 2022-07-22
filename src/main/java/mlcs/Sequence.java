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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Sequence is char sequence.
 * For convenience,it provides charAt,buildSuccessors methods
 */
public class Sequence {

  char[] chars;

  public Sequence(char[] chars) {
    super();
    this.chars = chars;
  }

  /**
   * Calculate each character counts
   * @return
   */
  public Map<Character, Integer> charCounts() {
    Map<Character, Integer> stats = new HashMap<>();
    int i = 1;
    while (i < chars.length - 1) {
      Integer c = stats.get(chars[i]);
      if (null == c) {
        stats.put(chars[i], 1);
      } else {
        stats.put(chars[i], c.intValue() + 1);
      }
      i++;
    }
    return stats;
  }

  /**
   * Find the distinct characters
   * @return
   */
  public Set<Character> charsets() {
    Set<Character> cs = new java.util.HashSet<Character>();
    int i = 1;
    while (i < chars.length - 1) {
      cs.add(chars[i]);
      i += 1;
    }
    return cs;
  }

  /**
   * Query char in given position.
   * @param index
   * @return
   */
  public char charAt(int index) {
    return chars[index];
  }

  public int length() {
    return chars.length - 1;
  }

  /**
   * Build a sequence from string.
   * It replaces white spaces,a dummy dot is added at both ends.
   * @param str
   * @return
   */
  public static Sequence build(String str) {
    if (str.length() >= Short.MAX_VALUE - 2) {
      throw new RuntimeException("Cannot process string longer than " + (Short.MAX_VALUE - 2));
    }
    String s = str.replaceAll(" ", "").trim();
    char[] c = new char[s.length() + 2];
    c[0] = '.';
    c[s.length() + 1] = '.';
    s.getChars(0, s.length(), c, 1);
    return new Sequence(c);
  }

  /**
   * Find the successors of the current point with respect to a character
   */
  public short[] buildSuccessors(char c) {
    int length = chars.length;
    short[] successors = new short[length];
    int i = 0;
    int j = i + 1;
    while (i < length) {
      if (j == i) j += 1;
      while (j < length && chars[j] != c) {
        j += 1;
      }
      if (j >= length - 1) successors[i] = -1;
      else successors[i] = (short) j;
      i += 1;
    }
    return successors;
  }

  /**
   * Find the predecessors of the current point with respect to a character
   */
  public short[] buildPredecessors(char c) {
    int length = chars.length;
    short[] predecessors = new short[length];
    int i = length - 1;
    int j = i - 1;
    while (i > 0) {
      if (j == i) j -= 1;
      while (j >= 0 && chars[j] != c) {
        j -= 1;
      }
      if (j < 0) predecessors[i] = 0;
      else predecessors[i] = (short) j;
      i -= 1;
    }
    predecessors[0] = -1;
    return predecessors;
  }

  /**
   * build a distance array for given character.
   * @param c
   * @return
   */
  public short[] buildDistance(char c) {
    int length = chars.length;
    short[] distances = new short[length];
    short cnt = 0;
    for (short i = (short) (length - 1); i >= 0; i--) {
      distances[i] = cnt;
      if (chars[i] == c) cnt += 1;
    }
    return distances;
  }
}
