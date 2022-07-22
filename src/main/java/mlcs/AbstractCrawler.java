package mlcs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.RecursiveTask;

public abstract class AbstractCrawler {
  public Mlcs mlcs;
  public final Node start; // start node
  public final Node end = null;
  short currentLevel = 0;
  Limit limit;
  LocationStore locStore;

  public AbstractCrawler(Mlcs mlcs, LocationStore locStore, Limit limit) {
    this.mlcs = mlcs;
    start = new Node(mlcs.start);
    this.locStore = locStore;
    this.limit = limit;
  }

  /**
   * Find and mark immediate successors in the same layer.
   */
  static class ImmediateCleaner extends RecursiveTask<Integer> {
    ArrayList<Location> locations;
    HashMap<Location, Location> locs;
    Mlcs mlcs;
    int from, to;

    public ImmediateCleaner(Mlcs mlcs, ArrayList<Location> locations, HashMap<Location, Location> locs, int from, int to) {
      this.locations = locations;
      this.locs = locs;
      this.mlcs = mlcs;
      this.from = from;
      this.to = to;
    }

    public Integer compute() {
      int endPredecessorsCnt = 0;
      Mlcs mlcs = this.mlcs;
      for (int i = from; i < to; i++) {
        Location loc = locations.get(i);
        List<Location> nexts = mlcs.nextLocations(loc);
        for (Location next : nexts) {
          Location n = locs.get(next);
          if (null != n) n.setReserved(false);
        }
        if (nexts.isEmpty()) {
          loc.setReserved(false);
          endPredecessorsCnt++;
        }
      }
      return endPredecessorsCnt;
    }
  }


  /**
   * A single search task,find all successors in range locations[from,to]
   */
  static class SearchCrawler extends RecursiveTask<List<Links>> {
    AbstractCrawler crawler;
    ArrayList<Location> locations;
    int from, to;

    public SearchCrawler(AbstractCrawler crawler, ArrayList<Location> locations, int from, int to) {
      super();
      this.crawler = crawler;
      this.locations = locations;
      this.from = from;
      this.to = to;
    }

    public List<Links> compute() {
      List<Links> results = new ArrayList<>();
      short level = crawler.currentLevel;
      Limit limit = crawler.limit;
      for (int i = from; i < to; i++) {
        Location n = locations.get(i);
        if (!n.isDiscard()) {
          List<Location> ns = crawler.mlcs.nextReachableLocations(n, limit.mlcsLength, level);
          if (!ns.isEmpty()) results.add(new Links(n, ns));
        }
      }
      return results;
    }
  }

}
