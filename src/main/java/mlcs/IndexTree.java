package mlcs;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * IndexTree is a hierarchical index tree.Its shape is similar to the B Tree.
 * It cannot be used to store nodes, just to search for dominant relationships.
 *
 * Use it in three steps: add all nodes, build index, searching for the specified node.
 */
public class IndexTree {
  IndexVector root = new IndexVector(true);

  public void build() {
    root.build();
  }

  /**
   * Exist dominant location which dominated the given point.
   * @param id
   * @return
   */
  public boolean dominated(Location id) {
    return dominated(root, id.index, 0);
  }

  /**
   * Search dominant location from given dimension
   * @param vector search vector
   * @param index position information
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
   * Add a location to the tree
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
    private IndexVector findFirstLess(short end) {
      return (minKey < end) ? orderedElems[0] : null;
    }

    private IndexVector findLess(int beginIdx, short end) {
      if (beginIdx < orderedKeys.length && orderedKeys[beginIdx] < end) {
        return orderedElems[beginIdx];
      } else {
        return null;
      }
    }

    private boolean containLess(short end) {
      return end > minKey;
    }

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
}
