package mlcs;

import java.io.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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

  /**
   * Restore the graph layer by layer from back to forward using persisted files.
   *
   * @param maxLevel
   * @return
   */
  public Graph restore(short maxLevel) {
    Graph graph = new Graph(mlcs);
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
      if (size > maxSize) {
        maxSize = size;
      }
      if (size >= capacity) {
        System.out.println("Start persisting mode at size:" + size);
        memoryMode = false;
        size = 0;
        for (short l = level; l > 0; l--) {
          serialize(l, levels[l - 1]);
          levels[l - 1] = null;
        }
      }
    } else {
      maxSize += nodes.size();
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
