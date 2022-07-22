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

import java.io.*;
import java.util.*;

/**
 * LocationStore is a layered storage structure.
 * It contains up to <code>maxLength</code> hash tables, and each layer corresponds to one hash table.
 * Combining maximum capacity and serialization mechanisms, it provides two working modes, memory and persistence modes.
 */
class LocationStore {
  final Mlcs mlcs;
  final String[] fileNames;
  final HashMap<Location, Location>[] levels;
  final Serializer serializer;
  long size = 0;//in memory node count
  long capacity;//in memory capacity
  long maxSize = 0;//max size in memory
  long totalSize = 0;//total create node count in both modes.
  boolean memoryMode = true;

  public LocationStore(Mlcs mlcs, long capacity) {
    int maxLevel = mlcs.maxLength;
    this.serializer = new Serializer(mlcs);
    this.mlcs = mlcs;
    this.fileNames = new String[maxLevel];
    for (int i = 0; i < maxLevel; i++) {
      this.fileNames[i] = "out/data_" + maxLevel + "_" + mlcs.seqs.size() + "_" + (i + 1);
    }
    int ml = mlcs.maxLength;
    this.levels = new HashMap[ml];
    this.capacity = capacity;
  }

  public void clear() {
    for (int i = 0; i < levels.length; i++) {
      levels[i] = null;
    }
  }

  /**
   * Restore the graph layer by layer from back to forward using persisted files.
   *
   * @param maxLevel
   * @return
   */
  public Graph restore(short maxLevel) {
    Graph graph = new Graph(mlcs);
    graph.maxLevel = maxLevel;
    graph.add((short) 0, mlcs.start, new Node(mlcs.start));
    graph.add((short) (maxLevel + 1), mlcs.end, new Node(mlcs.end));
    short l = maxLevel;
    HashMap<Location, Node> nexts = new HashMap<>();
    HashMap<Location, Node> currents = new HashMap<>();

    while (l > 0) {
      Iterator<Location> iter = iterator(l);
      boolean isLastLayer = l == maxLevel;
      while (iter.hasNext()) {
        Location id = iter.next();
        boolean hasSuccessors = false;
        List<Location> nextLocs = mlcs.nextLocations(id);
        for (Location loc : nextLocs) {
          Node next = nexts.get(loc);
          if (next != null) {
            hasSuccessors = true;
            next.link(id);
          }
        }
        if (hasSuccessors || (nextLocs.isEmpty() && isLastLayer)) {
          currents.put(id, new Node(id, l));
        }
      }
      graph.addLevel(l, currents);
      if (isLastLayer) {
        Node end = new Node(mlcs.end);
        end.level = (short) (maxLevel + 1);
        for (Map.Entry<Location, Node> entry : currents.entrySet()) {
          end.link(entry.getKey());
        }
        graph.add(end.level, end.id, end);
      }
      nexts = currents;
      currents = new HashMap<>();
      l -= 1;
    }
    //add start to level 1 nodes
    for (Map.Entry<Location, Node> entry : nexts.entrySet()) {
      entry.getValue().link(mlcs.start);
    }
    afterRestore();
    return graph;
  }

  /**
   * Remove files after restoration.
   */
  public void afterRestore() {
    for (String fileName : fileNames) {
      File f = new File(fileName);
      if (f.exists()) f.delete();
    }
  }

  public boolean isMemoryMode() {
    return memoryMode;
  }

  /**
   * Add a layer to store.
   *
   * @param level the level of the layer
   * @param nodes not empty nodes collections
   */
  public void add(short level, HashMap<Location, Location> nodes) {
    totalSize += nodes.size();
    if (memoryMode) {
      levels[level - 1] = nodes;
      size += nodes.size();
      if (size > maxSize) maxSize = size;

      if (size >= capacity) {
        System.out.println("Start persisting mode at size:" + size);
        memoryMode = false;
        size = 0;
        for (short l = level; l > 0; l--) {
          serialize(l, levels[l - 1]);
          levels[l - 1] = null;
        }
      }
      if (memoryMode) {
        // if location store is at memory mode,using recursive checks, remove points without successors.
        int removeCount=backwardFilter((short) (level - 1));
        //report(level - 1,removeCount);
      }
    } else {
      if (nodes.size() > maxSize) maxSize = nodes.size();
      serialize(level, nodes);
    }
  }

  /**
   * Serialize given nodes to files[level-1]
   *
   * @param level
   * @param levelNodes
   */
  private void serialize(short level, HashMap<Location, Location> levelNodes) {
    try {
      FileOutputStream os = new FileOutputStream(this.fileNames[level - 1]);
      for (Location loc : levelNodes.keySet()) {
        os.write(serializer.toBytes(loc));
      }
      os.flush();
      os.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Iterate the layered location
   *
   * @param level
   * @return
   */
  public Iterator<Location> iterator(short level) {
    HashMap<Location, Location> levelNodes = levels[level - 1];
    if (null == levelNodes) {
      try {
        FileInputStream is = new FileInputStream(fileNames[level - 1]);
        return new LocationIterator(is, new byte[serializer.bytes], serializer);
      } catch (Exception e) {
        e.printStackTrace();
        return null;
      }
    } else {
      return levelNodes.values().iterator();
    }
  }

  /**
   * Batch delete the nodes at a given level
   *
   * @param level
   * @param removed
   */
  public void remove(short level, List<Location> removed) {
    HashMap<Location, Location> levelNodes = levels[level - 1];
    if (null == levelNodes) {
      throw new RuntimeException("Operation is only supported in memory model.");
    } else {
      size -= removed.size();
      for (Location r : removed) {
        levelNodes.remove(r);
      }
    }
  }

  /**
   * Fetch the nodes at a given level
   *
   * @param level
   * @return
   */
  public HashMap<Location, Location> get(short level) {
    HashMap<Location, Location> levelNodes = levels[level - 1];
    if (null == levelNodes) {
      throw new RuntimeException("Operation is only supported in memory model.");
    } else {
      return levelNodes;
    }
  }

  private void report(int level, int removeCount) {
    if (removeCount > 0) System.out.println(level + " backward remove " + removeCount + " nodes");
  }

  /**
   * 过滤level层的点。通过观察后继是否存在
   *
   * @param startLevel
   * @return
   */
  public int backwardFilter(short startLevel) {
    if (startLevel <= 0) return 0;
    short level = startLevel;
    int totalRemoved = 0;
    while (level > 0) {
      List<Location> removed = new ArrayList<>();
      HashMap<Location, Location> nextLevelNodes = get((short) (level + 1));
      Iterator<Location> iterator = iterator(level);
      while (iterator.hasNext()) {
        Location id = iterator.next();
        List<Location> successors = mlcs.nextLocations(id);
        boolean hasNoneSuccessors = true;
        for (Location loc : successors) {
          if (nextLevelNodes.containsKey(loc)) {
            hasNoneSuccessors = false;
            break;
          }
        }
        if (hasNoneSuccessors) removed.add(id);
      }
      remove(level, removed);
      totalRemoved += removed.size();
      if (removed.isEmpty()) break;
    }
    return totalRemoved;
  }

  /**
   * Wrapper a file into a location iterator.
   */
  static class LocationIterator implements Iterator<Location> {
    final InputStream is;
    final Serializer serializer;
    byte[] bytes;
    int readBytes;

    public LocationIterator(InputStream is, byte[] bytes, Serializer serializer) {
      this.bytes = bytes;
      this.is = is;
      this.serializer = serializer;
    }

    @Override
    public boolean hasNext() {
      try {
        readBytes = is.read(bytes);
        if (readBytes < 0) is.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
      return readBytes > 0;
    }

    @Override
    public Location next() {
      return serializer.fromBytes(bytes);
    }

    @Override
    public void remove() {
    }
  }
}
