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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;

/**
 * MLCS problem model
 */
public class Mlcs {
  final Set<Character> charset; // the character that All strings of this problem consist of
  public final List<Sequence> seqs; // Primitive character sequence

  public final Location start; //startNode
  public final Location end; // endNode

  short[][][] successorTable; // char->seq->table
  short[][][] predecessorTable; // char->seq->table
  short[][][] distanceTable;// char->seq->table
  // maximum current subscript set
  public final int maxLength;
  int maxThread = 0;

  /**
   * building successor tables
   */
  public Mlcs(Set<Character> charset, List<Sequence> seqs) {
    this.charset = charset;
    this.seqs = seqs;
    start = Mlcs.buildStart(seqs.size());
    end = Mlcs.buildEnd(seqs);
    successorTable = new short[charset.size()][][];
    predecessorTable = new short[charset.size()][][];
    distanceTable = new short[charset.size()][][];
    List<Character> charList = new ArrayList<Character>(charset);

    for (int i = 0; i < charList.size(); i++) {
      successorTable[i] = new short[seqs.size()][];
      predecessorTable[i] = new short[seqs.size()][];
      distanceTable[i] = new short[seqs.size()][];
      for (int j = 0; j < seqs.size(); j++) {
        Sequence seq = seqs.get(j);
        successorTable[i][j] = seq.buildSuccessors(charList.get(i).charValue());
        predecessorTable[i][j] = seq.buildPredecessors(charList.get(i).charValue());
        distanceTable[i][j] = seq.buildDistance(charList.get(i).charValue());
      }
    }

    // The default is set to the maximum number of layers, and then set when searching in segments.
    int minOfSeq = Integer.MAX_VALUE;
    for (Sequence s : seqs) {
      if (s.length() - 1 < minOfSeq) minOfSeq = s.length() - 1;
    }
    maxLength = minOfSeq;
  }

  /**
   * Calculate a tail upbound for given location
   *
   * @param index
   * @return
   */
  public int tailUpbound(short[] index) {
    int bound = 0;
    for (int i = 0; i < distanceTable.length; i++) {
      short minOfChar = Short.MAX_VALUE;
      for (int j = 0; j < seqs.size(); j++) {
        short d = distanceTable[i][j][index[j]];
        if (d < minOfChar) minOfChar = d;
      }
      bound += minOfChar;
    }
    return bound;
  }

  public char charAt(Location location) {
    return seqs.get(0).charAt(location.index[0]);
  }

  public ForkJoinPool newPool() {
    return (maxThread > 0) ? new ForkJoinPool(maxThread) : new ForkJoinPool();
  }

  /**
   * All successors after the current node
   */
  public List<Location> nextLocations(Location current) {
    List<Location> nexts = new ArrayList<Location>(charset.size());
    for (int i = 0; i < successorTable.length; i++) {
      int snum = seqs.size();
      short[] tmp = new short[snum];
      for (int j = 0; j < seqs.size(); j++) {
        short successor = successorTable[i][j][current.index[j]];
        if (successor < 0 || successor > maxLength) break;
        else tmp[j] = successor;
      }
      if (tmp[tmp.length - 1] > 0) nexts.add(new Location(tmp));
    }
    return nexts;
  }

  /**
   * All reachable successors after the current node.
   * The reachable successor is possible to reach the end point on the longest path.
   */
  public List<Location> nextReachableLocations(Location current, int mlcsLength, short level) {
    List<Location> nexts = new ArrayList<Location>(charset.size());
    for (int i = 0; i < successorTable.length; i++) {
      int snum = seqs.size();
      short[] tmp = new short[snum];
      for (int j = 0; j < seqs.size(); j++) {
        short successor = successorTable[i][j][current.index[j]];
        if (successor < 0 || successor > maxLength) break;
        else tmp[j] = successor;
      }
      if (tmp[tmp.length - 1] > 0) {
        int possible = this.tailUpbound(tmp);
        if (possible + level >= mlcsLength) {
          nexts.add(new Location(tmp));
        }
      }
    }
    return nexts;
  }

  public List<Location> nextDirectLocations(Location current) {
    List<Location> nexts = nextLocations(current);
    List<Location> result = new ArrayList<>();
    for (Location l : nexts) {
      List<Location> ps = preLocations(l);
      boolean isDirect = true;
      for (Location p : ps) {
        if (current.morePriorOver(p)) {
          isDirect = false;
          break;
        }
      }
      if (isDirect) result.add(l);
    }
    return result;
  }

  public List<Location> preLocations(Location current) {
    List<Location> pres = new ArrayList<Location>(charset.size());
    for (int i = 0; i < predecessorTable.length; i++) {
      int snum = seqs.size();
      short[] tmp = new short[snum];
      for (int j = 0; j < seqs.size(); j++) {
        short successor = predecessorTable[i][j][current.index[j]];
        if (successor <= 0) break;
        else tmp[j] = successor;
      }
      if (tmp[tmp.length - 1] > 0) {
        pres.add(new Location(tmp));
      }
    }
    return (pres.isEmpty()) ? List.of(start) : pres;
  }

  /**
   * build start node
   */
  public static Location buildStart(int length) {
    return new Location(new short[length]);
  }

  /**
   * build end node
   */
  public static Location buildEnd(List<Sequence> seqs) {
    short[] index = new short[seqs.size()];
    int i = 0;
    for (Sequence seq : seqs) {
      index[i] = (short) seq.length();
      i += 1;
    }
    return new Location(index);
  }

  /**
   * Read the file to get all strings and character sets
   */
  public static String[] loadData(File file) throws IOException {
    List<String> datas = new ArrayList<>();
    BufferedReader in = new BufferedReader(new FileReader(file));
    String str = in.readLine();
    while (str != null) {
      if (str.length() > 0) {
        datas.add(str);
      }
      str = in.readLine();
    }
    in.close();
    return datas.toArray(new String[datas.size()]);
  }

  /**
   * Build the mlcs model using given strings
   *
   * @param strs
   * @return
   */
  public static Mlcs build(String[] strs) {
    List<Sequence> seqs = new ArrayList<Sequence>();
    Set<Character> charsets = new java.util.HashSet<Character>();
    for (String str : strs) {
      Sequence s = Sequence.build(str);
      seqs.add(s);
      charsets.addAll(s.charsets());
    }
    return new Mlcs(charsets, seqs);
  }

  /**
   * Calculate the matched counts for this mlcs problem.
   *
   * @return
   */
  public BigDecimal matchedCounts() {
    Map<Character, Integer>[] stats = new Map[seqs.size()];
    for (int i = 0; i < seqs.size(); i++) {
      stats[i] = seqs.get(i).charCounts();
    }
    BigDecimal rs = new BigDecimal(0);
    for (Character c : charset) {
      BigDecimal crs = new BigDecimal(stats[0].get(c));
      for (int i = 1; i < seqs.size(); i++) {
        crs = crs.multiply(new BigDecimal(stats[i].get(c)));
      }
      rs = rs.add(crs);
    }
    return rs;
  }
}
