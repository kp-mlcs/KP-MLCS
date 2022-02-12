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
