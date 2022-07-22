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
 * The MLCS problem solution graph.
 * It's a layered storage structure. The nodes[i] represents the set of key points at layer i.
 */
public class Graph {
  public final HashMap<Location, Node>[] nodes;
  public final Mlcs mlcs;
  public int maxLevel = 0;
  public Node end;

  public Graph(Mlcs mlcs) {
    this.mlcs = mlcs;
    this.maxLevel = mlcs.maxLength;
    nodes = new HashMap[maxLevel + 2];
  }

  /**
   * How many point in the layer which contains the most points.
   *
   * @return
   */
  public int height() {
    int max = 0;
    for (HashMap<Location, Node> ns : nodes) {
      if (ns != null && ns.size() > max) {
        max = ns.size();
      }
    }
    return max;
  }

  public int size() {
    int count = 0;
    for (HashMap<Location, Node> ns : nodes) {
      if (ns != null) {
        count += ns.size();
      }
    }
    return count;
  }

  /**
   * Fetch the node corresponding the given id and level.
   *
   * @param id
   * @param level
   * @return
   */
  private Node get(Location id, short level) {
    return nodes[level].get(id);
  }

  /**
   * Register a layer
   *
   * @param level
   * @param ns
   */
  public void addLevel(short level, HashMap<Location, Node> ns) {
    nodes[level] = ns;
  }

  /**
   * Register a single node
   *
   * @param level
   * @param id
   * @param n
   */
  public void add(short level, Location id, Node n) {
    HashMap<Location, Node> ns = nodes[level];
    if (null == ns) {
      ns = new HashMap<>();
      nodes[level] = ns;
    }
    ns.put(id, n);
  }

  /**
   * Query layer nodes at the given level
   *
   * @param l
   * @return
   */
  public HashMap<Location, Node> getLevel(int l) {
    return nodes[l];
  }

  /**
   * Calculate all MLCS paths
   *
   * @return
   */
  public List<List<Location>> paths() {
    return paths(-1);
  }

  /**
   * Calculate limit MLCS paths
   *
   * @return the path list
   * @limit -1 represent unlimit
   */
  public List<List<Location>> paths(int limit) {
    List<List<Location>> results = new ArrayList<>();
    Stack<Node> stack = new Stack<>();
    Stack<Location> path = new Stack<>();
    Collection<Node> lasts = getPredecessors(end, end.level).values();
    for (Node n : lasts) stack.push(n);

    while ((limit < 0 || results.size() < limit) && !stack.isEmpty()) {
      Node current = stack.peek();
      if (!path.isEmpty() && path.peek().equals(current.id)) {
        path.pop();
        stack.pop();
      } else {
        path.push(current.id);
        if (current.level == 1) {
          List<Location> onePath = new ArrayList<>(path);
          Collections.reverse(onePath);//堆栈的dump是从底部开始的。所以要取反
          results.add(onePath);
          stack.pop();
          path.pop();
        } else {
          Map<Location, Node> ps = getPredecessors(current, current.level);
          for (Node pres : ps.values()) stack.push(pres);
        }
      }
    }
    return results;
  }

  /**
   * Stat the MLCS count
   *
   * @param totalCreateCount
   * @param highestCapacity
   * @param startAt
   * @return
   */
  public Result stat(long totalCreateCount, long highestCapacity, long startAt) {
    Location startLocation = mlcs.start;
    Location endLocation = mlcs.end;
    Node end = nodes[maxLevel + 1].get(endLocation);
    this.end = end;
    link();

    Set<Location> keyLocs = new HashSet<Location>();
    long matchedCount = 0; // Number of matched results
    LinkedList<Location> queue = new LinkedList<>();

    // The number of alternative paths from the virtual endpoint to the node, with an initial endpoint of 1
    Map<Location, Long> routeCounts = new HashMap<Location, Long>();

    routeCounts.put(endLocation, (long) 1);

    Location queueEnd = endLocation;
    queue.addLast(endLocation);

    short currentLevel = end.level;
    while (!queue.isEmpty()) {
      Location loc = queue.removeFirst();
      Node node = get(loc, currentLevel);
      for (Map.Entry<Location, Node> p : getPredecessors(node, currentLevel).entrySet()) {
        Location ploc = p.getKey();
        if (keyLocs.contains(ploc)) {
          if (routeCounts.get(ploc) < 0 || routeCounts.get(loc) < 0) {
            routeCounts.put(ploc, (long) -1);
          } else {
            long newCount = routeCounts.get(ploc) + routeCounts.get(loc);
            routeCounts.put(ploc, (newCount > Integer.MAX_VALUE) ? (long) -1 : newCount);
          }
        } else {
          keyLocs.add(ploc);
          routeCounts.put(ploc, routeCounts.get(loc));
          queue.addLast(ploc);
        }
      }
      if (loc == queueEnd) {
        if (!queue.isEmpty()) queueEnd = queue.getLast();
        currentLevel -= 1;
      }
    }
    keyLocs.remove(startLocation);
    keyLocs.remove(endLocation);
    matchedCount = routeCounts.get(startLocation);

    return new Result(this, matchedCount, keyLocs.size(), maxLevel, totalCreateCount, highestCapacity,
      startAt, System.currentTimeMillis());
  }

  /**
   * Link the graph from back to forward.
   */
  public void link() {
    int l = maxLevel;
    while (l >= 0) {
      HashMap<Location, Node> nexts = getLevel(l + 1);
      Iterator<Node> iter = getLevel(l).values().iterator();
      boolean isLastLayer = l == maxLevel;
      while (iter.hasNext()) {
        Node n = iter.next();
        List<Location> nextLocs = mlcs.nextLocations(n.id);
        for (Location loc : nextLocs) {
          Node next = nexts.get(loc);
          if (next != null) next.link(n.id);
        }
        if ((nextLocs.isEmpty() && isLastLayer)) {
          end.link(n.id);
        }
      }
      l -= 1;
    }
  }

  /**
   * Query the predecessors for given node.
   *
   * @param n
   * @param level
   * @return
   */
  public Map<Location, Node> getPredecessors(Node n, short level) {
    Map<Location, Node> pres = new HashMap<>();
    if (n.pres == null) {
      return Collections.emptyMap();
    }
    for (Location id : n.pres) {
      Node p = get(id, (short) (level - 1));
      if (null != p) pres.put(id, p);
    }
    return pres;
  }

  public boolean largeThan(Graph graph) {
    if (this.maxLevel > graph.maxLevel) return true;
    else if (this.maxLevel < graph.maxLevel) return false;
    else return this.size() > graph.size();
  }
}
