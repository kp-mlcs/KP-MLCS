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

import java.util.*;

/**
 * IndexTree is a hierarchical index tree.Its shape is similar to the B Tree.
 * It cannot be used to store nodes, just to search for dominant relationships.
 * <p>
 * Use it in three steps: add all nodes, build index, searching for the specified node.
 */
public class IndexTree {
  IndexVector root = new IndexVector(true);

  public IndexTree(int dimensions) {
  }

  public void build() {
    if (root.children.isEmpty()) {
      root.minKey = Short.MAX_VALUE;
    } else {
      root.build();
    }
  }

  public static Set<Location> findKeyLocs(List<Location> locs) {
    int dimensions = locs.get(0).index.length;
    Head[] heads = new Head[dimensions];
    for (int i = 0; i < dimensions; i++) {
      heads[i] = new Head();
    }
    for (Location id : locs) {
      if (id.isDiscard()) continue;
      short[] index = id.index;
      int len = index.length;
      for (int i = 0; i < len; i++) {
        Head head = heads[i];
        if (index[i] < head.index) {
          head.update(index[i], id);
        } else if (index[i] == head.index) {
          head.add(id);
        }
      }
    }
    Set<Location> reserves = new HashSet<>();
    for (Head head : heads) {
      reserves.addAll(head.locations);
    }
    return reserves;
  }

  /**
   * Exist dominant location which dominated the given point.
   *
   * @param id
   * @return
   */
  public boolean dominated(Location id) {
    return dominated(root, id.index, 0);
  }

  /**
   * if exists dominant location from given dimension
   *
   * @param vector       search vector
   * @param index        position information
   * @param dimentionIdx for which dimension
   * @return true if exist at least one dominant
   */
  private boolean dominated(IndexVector vector, short[] index, int dimentionIdx) {
    if (dimentionIdx == index.length - 1) {
      return vector.containLess(index[dimentionIdx]);
    } else {
      IndexVector elem = vector.findFirstLess(index[dimentionIdx]);
      if (elem == null) return false;
      int searchIdx = 0;
      while (elem != null) {
        boolean subResult = dominated(elem, index, dimentionIdx + 1);
        if (subResult) return true;
        searchIdx += 1;
        elem = vector.findLess(searchIdx, index[dimentionIdx]);
      }
      return false;
    }
  }

  /**
   * 展平递归的算法，但是效果不是很好
   */
  public boolean existPrior2(Location id) {
    Slot[] slots = new Slot[id.index.length];
    for (int i = 0; i < id.index.length; i++) {
      slots[i] = new Slot(null, 0);
    }
    int d = 0;
    slots[d].vector = root;
    short[] index = id.index;
    int lastIdx = index.length - 1;
    while (d > -1) {
      Slot slot = slots[d];
      if (d == lastIdx) {
        if (slot.vector.containLess(index[d])) return true;
        d--;
      } else {
        IndexVector next = slot.vector.findLess(slot.idx++, index[d]);
        if (next == null) {
          d--;
        } else {
          ++d;
          slots[d].vector = next;
          slots[d].idx = 0;
        }
      }
    }
    return false;
  }

  private static class Slot {
    IndexVector vector;
    int idx;

    public Slot(IndexVector vector, int idx) {
      this.vector = vector;
      this.idx = idx;
    }
  }

  /**
   * Add a location to the tree
   *
   * @param id
   */
  public void put(Location id) {
    short[] index = id.index;
    IndexVector cur = root;
    int len = index.length;
    int last = index.length - 1;
    int initLast = index.length - 2;
    for (int i = 0; i < len; i++) {
      if (i < last) {
        cur = cur.getOrCreate(index[i], i < initLast);
      } else {
        cur.setMinimal(index[i]);
      }
    }
  }

  /**
   * A index vector, contains children groups.
   */
  private static class IndexVector {
    Map<Short, IndexVector> children;
    short[] orderedKeys;
    IndexVector[] orderedElems;
    short minKey = -1;

    IndexVector(boolean createMap) {
      if (createMap) children = new HashMap<>();
    }

    public IndexVector getOrCreate(short idx, boolean initMap) {
      IndexVector vector = children.get(idx);
      if (null == vector) {
        vector = new IndexVector(initMap);
        children.put(idx, vector);
        return vector;
      } else {
        return vector;
      }
    }

    public IndexVector get(short idx) {
      return children.get(idx);
    }

    public void setMinimal(short idx) {
      if (minKey < 0 || idx < minKey) {
        minKey = idx;
      }
    }

    /**
     * find element less than end from beginIdx
     *
     * @param end exclusive
     * @return
     */
    private IndexVector findFirstLess(short key) {
      return (minKey < key) ? orderedElems[0] : null;
    }

    private boolean containLess(short key) {
      return key > minKey;
    }

    private IndexVector findLess(int beginIdx, short key) {
      if (beginIdx < orderedKeys.length && orderedKeys[beginIdx] < key) {
        return orderedElems[beginIdx];
      } else {
        return null;
      }
    }

    /**
     * Build inner index,Convert hashmap to ordered keys and elements
     */
    public void build() {
      if (orderedKeys == null && children != null && !children.isEmpty()) {
        orderedKeys = new short[children.size()];
        int i = 0;
        Iterator<Short> iter = children.keySet().iterator();
        while (iter.hasNext()) {
          orderedKeys[i] = iter.next();
          i += 1;
        }
        Arrays.sort(orderedKeys);
        orderedElems = new IndexVector[orderedKeys.length];
        for (int j = 0; j < orderedKeys.length; j++) {
          IndexVector elem = children.get(orderedKeys[j]);
          orderedElems[j] = elem;
          elem.build();
        }
        minKey = orderedKeys[0];
        children = null;
      }
    }
  }

  private static class Head {
    short index = Short.MAX_VALUE;
    Set<Location> locations = new HashSet<>();

    public void update(short newIndex, Location location) {
      this.index = newIndex;
      locations = new HashSet<>();
      locations.add(location);
    }

    public void add(Location location) {
      locations.add(location);
    }
  }
}
