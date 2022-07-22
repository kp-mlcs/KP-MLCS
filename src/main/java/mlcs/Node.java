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
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

/**
 * A Node represent a location in graph.
 * It contains a location, level information and precursors.
 */
public class Node implements Serializable {
  private static final long serialVersionUID = -426003142019185878L;
  /**
   * location of node
   */
  public final Location id;
  /**
   * level of node
   */
  public short level;

  public Set<Location> pres;

  /**
   * link precursor to this nodes
   * @param pre
   */
  public void link(Location pre) {
    if (null == pres) {
      pres = new HashSet<>();
    }
    pres.add(pre);
  }

  public Node(Location id) {
    this.id = id;
  }

  public Node(Location id, short level) {
    this.id = id;
    this.level = level;
  }

  @Override
  public String toString() {
    return id + " l:" + level + " d:" + ((null == pres) ? 0 : pres.size());
  }

  public boolean isPresEmpty() {
    return null == pres || pres.isEmpty();
  }

  /**
   * add predecessor
   * FIXME
   */
  public synchronized boolean addPredecessor(EPCrawler crawler, Node from, int newLevel) {
    if (null == pres) return false;
    boolean removed = pres.remove(from.id);
    if (removed) {
      if (this.level < newLevel) {
        this.level = (short) newLevel;
      }
      if (pres.isEmpty()) {
        pres = null;
        return true;
      } else {
        return false;
      }
    } else {
      return false;
    }
  }

  @Override
  public boolean equals(Object o) {
    return id.equals(((Node) o).id);
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }

  /**
   * optimal path selector
   */
  public static class SumSorter implements Comparator<Node> {

    short level;

    public SumSorter(short level) {
      this.level = level;
    }

    public int compare(Node o1, Node o2) {
      return o1.id.sum(level) - o2.id.sum(level);
    }
  }
}
