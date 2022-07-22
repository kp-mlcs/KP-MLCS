package mlcs;

import java.util.*;

public class IndexGraph {

  DimensionNodes[] dimensionNodes;

  public IndexGraph(int dimensions) {
    dimensionNodes = new DimensionNodes[dimensions];
    for (int i = 0; i < dimensionNodes.length; i++) {
      dimensionNodes[i] = new DimensionNodes();
    }
  }

  public void build() {
    for (DimensionNodes dn : dimensionNodes) {
      dn.build();
    }
  }

  public void reserveHead(List<Location> locs) {
    for (int j = 0; j < dimensionNodes.length - 1; j++) {
      IndexGraph.DimensionNodes dn = dimensionNodes[j];
      for (IndexGraph.Edge edge : dn.nodes[0].orderedEdges) {
        for (Integer i : edge.locationIds) {
          locs.get(i).setReserved(true);
        }
      }
    }
    if (dimensionNodes.length >= 3) {
      short lastMinIndex = dimensionNodes[dimensionNodes.length - 1].nodes[0].index;
      IndexGraph.DimensionNodes beforeLast = dimensionNodes[dimensionNodes.length - 2];
      for (IndexGraph.IndexNode n : beforeLast.nodes) {
        if (n.orderedEdges[0].to.index == lastMinIndex) {
          for (Integer i : n.orderedEdges[0].locationIds) {
            locs.get(i).setReserved(true);
          }
        }
      }
    }
  }

  public boolean dominated(Location id) {
    IndexNode[] dimension0 = dimensionNodes[0].nodes;
    short d0Index = id.index[0];
    for (IndexNode node : dimension0) {
      if (node.index < d0Index) {
        boolean is_dominated = dominated(node, id.index, 1, null);
        if (is_dominated) return true;
      } else {
        return false;
      }
    }
    return false;
  }

  private boolean dominated(IndexNode node, short[] index, int dimentionIdx, Set<Integer> locationIds) {
    if (dimentionIdx == index.length - 1) {
      return node.containLess(index[dimentionIdx], locationIds);
    } else {
      MatchedEdge elem = node.findLess(0, index[dimentionIdx], locationIds);
      if (elem == null) return false;
      while (elem != null) {
        boolean subResult = dominated(elem.edge.to, index, dimentionIdx + 1, elem.locationIds);
        if (subResult) return true;
        elem = node.findLess(elem.searchIndex + 1, index[dimentionIdx], locationIds);
      }
      return false;
    }
  }

  /**
   * Add a location to the graph
   *
   * @param id
   */
  public void put(int locationId, Location id) {
    short[] index = id.index;
    int len = index.length;
    IndexNode prev = dimensionNodes[0].getOrCreate(index[0]);
    for (int i = 1; i < len; i++) {
      IndexNode next = dimensionNodes[i].getOrCreate(index[i]);
      prev.addEdge(next, locationId);
      prev = next;
    }
  }

  public static class DimensionNodes {
    IndexNode[] nodes;
    Map<Short, IndexNode> nodeMaps = new HashMap<>();

    public IndexNode getOrCreate(short index) {
      IndexNode n = nodeMaps.get(index);
      if (null == n) {
        n = new IndexNode(index);
        nodeMaps.put(index, n);
      }
      return n;
    }

    public void build() {
      nodes = new IndexNode[nodeMaps.size()];
      List<Short> keys = new ArrayList<>(nodeMaps.keySet());
      Collections.sort(keys);
      for (int i = 0; i < nodes.length; i++) {
        nodes[i] = nodeMaps.get(keys.get(i));
        nodes[i].build();
      }
      nodeMaps = null;
    }
  }

  public static class Edge {
    final IndexNode to;
    final Set<Integer> locationIds;

    public Edge(IndexNode to, Set<Integer> locationIds) {
      this.to = to;
      this.locationIds = locationIds;
    }
  }

  public static class MatchedEdge {
    public final int searchIndex;
    public final Edge edge;
    public final Set<Integer> locationIds;

    public MatchedEdge(int searchIndex, Edge edge, Set<Integer> locationIds) {
      this.searchIndex = searchIndex;
      this.edge = edge;
      this.locationIds = locationIds;
    }
  }

  public static class IndexNode implements Comparable<IndexNode> {
    final short index;
    Edge[] orderedEdges;
    Map<IndexNode, Set<Integer>> edgeDatas = new HashMap<>();

    public IndexNode(short index) {
      this.index = index;
    }

    public void build() {
      List<IndexNode> nexts = new ArrayList<>(edgeDatas.keySet());
      Collections.sort(nexts);
      orderedEdges = new Edge[nexts.size()];
      for (int i = 0; i < orderedEdges.length; i++) {
        IndexNode to = nexts.get(i);
        orderedEdges[i] = new Edge(to, edgeDatas.get(to));
      }
      edgeDatas = null;
    }

    public void addEdge(IndexNode to, int locationId) {
      Set<Integer> locationIds = edgeDatas.get(to);
      if (null == locationIds) {
        locationIds = new HashSet<>();
        edgeDatas.put(to, locationIds);
      }
      locationIds.add(locationId);
    }

    @Override
    public int hashCode() {
      return index;
    }

    @Override
    public String toString() {
      StringBuilder a = new StringBuilder();
      for (Edge edge : orderedEdges) {
        a.append(edge.to.index).append(",");
      }
      if (a.length() > 0) {
        return index + " -> (" + a.deleteCharAt(a.length() - 1).toString() + ")";
      } else {
        return String.valueOf(index);
      }
    }

    @Override
    public int compareTo(IndexNode o) {
      return this.index - o.index;
    }

    private MatchedEdge findLess(int beginIdx, short locationIndex, Set<Integer> locationIds) {
      for (int i = beginIdx; i < orderedEdges.length; i++) {
        if (orderedEdges[i].to.index < locationIndex) {
          Set<Integer> matchedLocationIds = IndexGraph.intersection(orderedEdges[i].locationIds, locationIds);
          if (!matchedLocationIds.isEmpty())
            return new MatchedEdge(i, orderedEdges[i], matchedLocationIds);
        } else {
          break;
        }
      }
      return null;
    }

    public boolean containLess(short index, Set<Integer> locationIds) {
      for (Edge edge : orderedEdges) {
        if (edge.to.index < index) {
          if (hasIntersection(edge.locationIds, locationIds)) return true;
        } else {
          return false;
        }
      }
      return false;
    }
  }

  public static <T> boolean hasIntersection(Set<T> first, Set<T> second) {
    if (null == second) return true;
    if (first.size() < second.size()) {
      for (T obj : first)
        if (second.contains(obj)) return true;
    } else {
      for (T obj : second)
        if (first.contains(obj)) return true;
    }
    return false;
  }

//  public static <T> Set<T> intersection(Set<T> first, Set<T> second) {
//    long start = System.currentTimeMillis();
//    Set<T> rs = intersection2(first, second);
//    System.out.println("insection on first(" + first.size() + ") and sencond (" + ((null == second) ? "all" : second.size()) + ") using " + (System.currentTimeMillis() - start));
//    return rs;
//  }

  public static <T> Set<T> intersection(Set<T> first, Set<T> second) {
    if (null == second) return first;
    Set<T> elts = new HashSet<T>();
    if (first.size() < second.size()) {
      for (T obj : first)
        if (second.contains(obj)) elts.add(obj);
    } else {
      for (T obj : second)
        if (first.contains(obj)) elts.add(obj);
    }
    return elts;
  }

  public static void main(String[] args) {
    IndexGraph graph = new IndexGraph(3);
    List<Location> locations = new ArrayList<>();
    List<short[]> points = List.of(new short[]{20, 19, 14}, new short[]{16, 18, 18},
      new short[]{16, 16, 17}, new short[]{17, 15, 15}, new short[]{19, 18, 17},
      new short[]{20, 19, 17}, new short[]{20, 18, 17});
    for (int i = 0; i < points.size(); i++) {
      locations.add(new Location(points.get(i)));
    }
    for (int i = 0; i < locations.size(); i++) {
      graph.put(i, locations.get(i));
    }
    graph.build();
    graph.reserveHead(locations);
    for (int i = 0; i < locations.size(); i++) {
      Location loc = locations.get(i);
      if (loc.isUnknown()) {
        if (graph.dominated(loc)) {
          loc.setReserved(false);
        } else {
          loc.setReserved(true);
        }
      }
    }
    System.out.println(graph);
  }
}
