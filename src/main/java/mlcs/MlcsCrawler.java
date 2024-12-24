package mlcs;

import mlcs.util.Queues;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;

/**
 * MLCS problem DAG crawler.
 * It traverses the MLCS sequences,and build a compact MLCS-DAG layer by layer.
 */
public class MlcsCrawler {
  public final Mlcs mlcs;
  public final Setting setting;
  public final Node start; // start node
  public final Node end = null;
  short currentLevel = 0;
  Limit limit;
  LocationStore locStore;
  LocationFilterPolicy filterPolicy;
  ArrayList<Location> fronts;
  MineStatus status;
  private long startAt = System.currentTimeMillis();

  public MlcsCrawler(Mlcs mlcs, Setting setting, LocationStore locStore, Limit limit, LocationFilterPolicy filterPolicy) {
    this.mlcs = mlcs;
    this.setting = setting;
    this.start = new Node(mlcs.start);
    this.locStore = locStore;
    this.limit = limit;
    this.filterPolicy = filterPolicy;
    this.currentLevel = 0;
    this.fronts = new ArrayList<>();
    this.fronts.add(start.id);
    this.status = MineStatus.Initial;
  }

  public boolean search() {
    status = MineStatus.Mining;
    ForkJoinPool pool = this.setting.newPool();
    while (!fronts.isEmpty() && status == MineStatus.Mining) {
      this.currentLevel = (short) (this.currentLevel + 1);
      if (this.currentLevel > limit.mlcsLength) limit.mlcsLength = this.currentLevel;
      // all nodes in this round
      HashMap<Location, Location> nodes = new HashMap<>();

      // step 1. search successors locations using multiple threaded
      // Each successor met the conditions for the maximum length assessment
      List<int[]> segs = Queues.split(fronts.size(), pool.getParallelism());
      LinkedList<ForkJoinTask<List<Links>>> tasks = new LinkedList<>();
      for (int[] seg : segs) {
        tasks.add(pool.submit(new SearchCrawler(this, fronts, seg[0], seg[1])));
      }
      for (ForkJoinTask<List<Links>> task : tasks) {
        List<Links> pairs = task.join();
        for (Links p : pairs) {
          for (Location l : p.nexts) {
            // don't drop the <if>,It avoids that key and value are not the same object.
            if (!nodes.containsKey(l)) nodes.put(l, l);
          }
        }
      }

      // step 2. traverse the new location using multiple threaded
      // find and mark 1)immediate successor,2) predecessors of end
      fronts = new ArrayList<>(nodes.keySet());
      segs = Queues.split(fronts.size(), pool.getParallelism());
      LinkedList<ForkJoinTask<Integer>> cleanTasks = new LinkedList<>();
      for (int[] seg : segs) {
        cleanTasks.add(pool.submit(new ImmediateCleaner(this.mlcs, fronts, nodes, seg[0], seg[1])));
      }
      int endPredecessorsCnt = 0;
      for (ForkJoinTask<Integer> task : cleanTasks) {
        endPredecessorsCnt += task.join();
      }

      //Is the last layer
      boolean finished = nodes.size() == endPredecessorsCnt;
      if (finished) {
        HashMap<Location, Location> last = new HashMap<>();
        for (Location l : nodes.keySet()) {
          l.setReserved(true);
          last.put(l, l);
        }
        locStore.add(this.currentLevel, last);
        break;
      }

      // step 3. Multi-thread dominant filtering was used to exclude an indirect successors.
      int immediateRemoveCnt = 0;
      int nonImmediateRemoveCnt = 0;
      if (fronts.size() > 1) {
//      Razor razor = new Razor(this.mlcs, this.currentLevel);
        Razor3 razor = new Razor3(this.mlcs, this.setting, this.currentLevel);
        int[] rs = razor.shave(pool, fronts);
        // int[] rs = FirstOrderSorter.shave(this.minLevel,pool,fronts);
        immediateRemoveCnt = rs[0];
        nonImmediateRemoveCnt = rs[1];
      } else {
        for (Location loc : nodes.keySet()) {
          if (loc.isDiscard()) immediateRemoveCnt++;
        }
      }

      // step 4. Register the remaining points to the store
      int removedCount = immediateRemoveCnt + nonImmediateRemoveCnt;
      filterPolicy.filter(fronts, removedCount);
      nodes.entrySet().removeIf(entry -> entry.getKey().isDiscard());
      locStore.add(this.currentLevel, nodes);
      //System.out.println(this.minLevel + " remove nodes(" + allNodeCnt + " - " + removeCnt + "(" + immediateRemoveCnt + "+" + nonImmediateRemoveCnt + "))");
      setting.notify("mining level " + currentLevel + " " + (currentLevel * 100.0 / limit.mlcsLength) + "% ");
    }
    pool.shutdown();
    boolean f = finished();
    if (f) status = MineStatus.Finished;
    return f;
  }

  public boolean finished() {
    return fronts.isEmpty();
  }

  /**
   * Concurrent search the MLCS graph.
   */
  public Result stat() {
    //restore the graph from back to forward.
    Graph graph = locStore.restore(this.currentLevel);
    setting.notify("mining complete.");
    Result result = graph.stat(setting, this.locStore.totalSize, this.locStore.maxSize, startAt);
    setting.notify("find " + result.mlcsCount + " mlcs(length " + result.maxLevel + ")");
    setting.finish(result);
    return result;
  }

  public void stop() {
    if (status == MineStatus.Mining) {
      status = MineStatus.Stopped;
    }
  }

  public void continueWith(LocationFilterPolicy filterPolicy) {
    if (null != filterPolicy) {
      this.filterPolicy = filterPolicy;
      filterPolicy.filter(this.fronts, 0);
    }
    search();
  }

  public LocationFilterPolicy getFilterPolicy() {
    return filterPolicy;
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
    MlcsCrawler crawler;
    ArrayList<Location> locations;
    int from, to;

    public SearchCrawler(MlcsCrawler crawler, ArrayList<Location> locations, int from, int to) {
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
