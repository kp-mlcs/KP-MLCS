package mlcs;

import java.math.BigDecimal;
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

  /**
   * Fetch the node corresponding the given id and level.
   * @param id
   * @param level
   * @return
   */
  private Node get(Location id, short level) {
    return nodes[level].get(id);
  }

  /**
   * Register a layer
   * @param level
   * @param ns
   */
  public void addLevel(short level, HashMap<Location, Node> ns) {
    nodes[level] = ns;
  }

  /**
   * Register a single node
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
   * @param l
   * @return
   */
  public HashMap<Location, Node> getLevel(int l) {
    return nodes[l];
  }

  /**
   * Calculate all MLCS paths
   * @return
   */
  public List<List<Location>> paths() {
    return paths(-1);
  }

  /**
   * Calculate limit MLCS paths
   * @limit  -1 represent unlimit
   * @return the path list
   */
  public List<List<Location>> paths(int limit) {
    List<List<Location>> results = new ArrayList<>();
    Stack<Node> stack = new Stack<>();
    Collection<Node> lasts = getPredecessors(end, end.level).values();
    for (Node n : lasts) stack.push(n);
    Stack<Location> path = new Stack<>();

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
   * @param maxLevel
   * @param totalCreateCount
   * @param highestCapacity
   * @param startAt
   * @return
   */
  public Result stat(int maxLevel, long totalCreateCount, long highestCapacity, long startAt) {
    Location startLocation = mlcs.start;
    Location endLocation = mlcs.end;
    this.maxLevel = maxLevel;
    Node end = nodes[maxLevel + 1].get(endLocation);
    this.end = end;
    link();

    Set<Location> keyLocs = new HashSet<Location>();
    BigDecimal matchedCount = new BigDecimal(0); // Number of matched results
    LinkedList<Location> queue = new LinkedList<>();

    // The number of alternative paths from the virtual endpoint to the node, with an initial endpoint of 1
    Map<Location, BigDecimal> routeCounts = new HashMap<Location, BigDecimal>();

    routeCounts.put(endLocation, new BigDecimal(1));

    Location queueEnd = endLocation;
    queue.addLast(endLocation);

    short currentLevel = end.level;
    while (!queue.isEmpty()) {
      Location loc = queue.removeFirst();
      Node node = get(loc, currentLevel);
      for (Map.Entry<Location, Node> p : getPredecessors(node, currentLevel).entrySet()) {
        Location ploc = p.getKey();
        if (keyLocs.contains(ploc)) {
          routeCounts.put(ploc, routeCounts.get(ploc).add(routeCounts.get(loc)));
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
}
