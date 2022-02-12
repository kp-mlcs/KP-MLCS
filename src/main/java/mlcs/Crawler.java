package mlcs;

import mlcs.util.Queues;

import java.io.File;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;

/**
 * MLCS problem traversal.
 * It traverses the MLCS sequences,and build a compact MLCS-DAG layer by layer.
 */
public class Crawler {
  private Limit limit;
  public Mlcs mlcs;
  public final Node start; // startnode
  public final Node end = null;
  short minLevel = 0;
  LocationStore locStore;

  public Crawler(Mlcs mlcs) {
    super();
    this.mlcs = mlcs;
    start = new Node(mlcs.start);
    long m = Runtime.getRuntime().maxMemory() / 1024 / 1024; // Mb
    //Location(array(seqs_size*2)8 + hashcode4+reserved1),
    int nodePerMB = (int) (1000000 / (2 * mlcs.seqs.size() + 4 + 8 + 1) * 0.75 * 0.2);
    locStore = new LocationStore(mlcs, m * nodePerMB);
  }

  public static void main(String[] args) throws Exception {
    if (args.length == 0) {
      System.out.println("Usage:Crawler /path/to/your/data/file");
      return;
    }
    for (String arg : args) {
      List<String> fileNames = findFiles("./file", arg);
      for (String file : fileNames) {
        System.gc();
        searchFile(file);
      }
    }
  }

  /**
   * find all files prefixed with name under the specified path
   */
  private static List<String> findFiles(String path, String name) {
    List<String> fileNamesList = new ArrayList<String>();
    File file = new File(path);
    File[] files = file.listFiles();
    for (File f : files) {
      if (f.isFile() && f.getName().startsWith(name)) {
        fileNamesList.add(f.getName());
      }
    }
    return fileNamesList;
  }

  /**
   * Search for given file
   *
   * @param fileName
   * @throws Exception
   */
  private static void searchFile(String fileName) throws Exception {
    long startAt = System.currentTimeMillis();
    String[] datas = Mlcs.loadData("./file/" + fileName);
    Mlcs mlcs = Mlcs.build(datas);
    Crawler crawler = new Crawler(mlcs);
    crawler.limit = new Limit(mlcs.maxLength, Pioneer.search(mlcs));
    int mlcsLength = crawler.limit.mlcsLength;
    System.out.println(new Date().toString() + " : obtain max length " + mlcsLength);
    Graph graph = crawler.search();
    LocationStore store = crawler.locStore;
    Result result = graph.stat(crawler.minLevel, store.totalSize, store.maxSize, startAt);
    if (null != result) {
      result.dumpTo("./out/result_" + fileName);
      System.out.println("Find " + result.mlcsCount.toString() + " mlcs(length " + result.maxLevel + ")");
      System.out.println("The result is dumped into file:" + new File("./out/result_" + fileName).getAbsolutePath());
      if (graph.maxLevel < 300) result.visualize();
    }
  }

  /**
   * A single search task,find all successors in range locations[from,to]
   */
  static class SearchCrawler extends RecursiveTask<List<Links>> {
    Crawler crawler;
    ArrayList<Location> locations;
    int from, to;

    public SearchCrawler(Crawler crawler, ArrayList<Location> locations, int from, int to) {
      super();
      this.crawler = crawler;
      this.locations = locations;
      this.from = from;
      this.to = to;
    }

    public List<Links> compute() {
      List<Links> results = new ArrayList<>();
      short level = crawler.minLevel;
      Limit limit = crawler.limit;
      for (int i = from; i < to; i++) {
        Location n = locations.get(i);
        if (n.reserved) {
          List<Location> ns = crawler.mlcs.nextReachableLocations(n, limit, level);
          if (!ns.isEmpty()) results.add(new Links(n, ns));
        }
      }
      return results;
    }
  }


  /**
   * Find and mark immediate successors in the same layer.
   */
  static class ImmediateCleaner extends RecursiveTask<Integer> {
    ArrayList<Location> locations;
    HashMap<Location, Location> locs;
    Crawler crawler;
    int from, to;

    public ImmediateCleaner(Crawler crawler, ArrayList<Location> locations, HashMap<Location, Location> locs, int from, int to) {
      this.locations = locations;
      this.locs = locs;
      this.crawler = crawler;
      this.from = from;
      this.to = to;
    }

    public Integer compute() {
      Mlcs mlcs = crawler.mlcs;
      int endPredecessorsCnt = 0;
      for (int i = from; i < to; i++) {
        Location loc = locations.get(i);
        List<Location> nexts = mlcs.nextLocations(loc);
        for (Location next : nexts) {
          Location n = locs.get(next);
          if (null != n) n.reserved = false;
        }
        if (nexts.isEmpty()) {
          loc.reserved = false;
          endPredecessorsCnt++;
        }
      }
      return endPredecessorsCnt;
    }
  }

  /**
   * Concurrent search the MLCS graph.
   */
  private Graph search() {
    this.minLevel = 0;
    ArrayList<Location> fronts = new ArrayList<>();
    fronts.add(start.id);

    ForkJoinPool pool = this.mlcs.newPool();
    while (!fronts.isEmpty()) {
      this.minLevel = (short) (this.minLevel + 1);
      if (this.minLevel > limit.mlcsLength) limit.mlcsLength = this.minLevel;
      // all nodes in this round
      HashMap<Location, Location> nodes = new HashMap<>();

      // step 1. search successors locations using multiple threaded
      // Each successor met the conditions for the maximum length assessment
      List<int[]> segs = Queues.split(fronts.size(), pool.getParallelism());
      LinkedList<ForkJoinTask<List<Links>>> tasks = new java.util.LinkedList<>();
      for (int[] seg : segs) {
        tasks.add(pool.submit(new SearchCrawler(this, fronts, seg[0], seg[1])));
      }
      for (ForkJoinTask<List<Links>> task : tasks) {
        List<Links> pairs = task.join();
        for (Links p : pairs) {
          for (Location l : p.nexts) {
            // don't drop the if,It avoids that key and value are not the same object.
            if (!nodes.containsKey(l)) nodes.put(l, l);
          }
        }
      }

      // step 2. traverse the new location using multiple threaded
      // find and mark 1)immediate successor,2) predecessors of end
      fronts = new ArrayList<>();
      for (Location loc : nodes.keySet()) {
        fronts.add(loc);
      }
      segs = Queues.split(fronts.size(), pool.getParallelism());
      LinkedList<ForkJoinTask<Integer>> cleanTasks = new java.util.LinkedList<>();
      for (int[] seg : segs) {
        cleanTasks.add(pool.submit(new ImmediateCleaner(this, fronts, nodes, seg[0], seg[1])));
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
          l.reserved = true;
          last.put(l, l);
        }
        locStore.add(this.minLevel, last);
        break;
      }

      // step 3. Multi-thread dominant filtering was used to exclude an indirect successors.
      int immediateRemoveCnt = 0;
      int nonImmediateRemoveCnt = 0;
      if (fronts.size() > 1) {
        Razor razor = new Razor(this.minLevel);
        int[] rs = razor.shave(pool, fronts);
        immediateRemoveCnt = rs[0];
        nonImmediateRemoveCnt = rs[1];
      } else {
        for (Location loc : nodes.keySet()) {
          if (!loc.reserved) immediateRemoveCnt++;
        }
      }

      // step 4. Register the remaining points to the store
      int removeCnt = immediateRemoveCnt + nonImmediateRemoveCnt;
      int allNodeCnt = nodes.size();
      nodes.entrySet().removeIf(entry -> !entry.getKey().reserved);
      locStore.add(this.minLevel, nodes);
      System.out.println(this.minLevel + " remove nodes(" + allNodeCnt + " - " + removeCnt + "(" + immediateRemoveCnt + "+" + nonImmediateRemoveCnt + "))");

      // if location store is at memory mode,using recursive checks, remove points with no successors.
      if (locStore.isMemoryMode()) report(filter(locStore, (short) (this.minLevel - 1)));
      System.gc();
    }
    //restore the graph from back to forward.
    return locStore.restore(this.minLevel);
  }

  private void report(int a) {
    if (a > 0) System.out.println(this.minLevel + " remove backward " + a + " nodes");
  }

  /**
   * Using recursive checks, remove points with no successors.
   *
   * @param level
   * @return
   */
  private int filter(LocationStore store, short level) {
    if (level <= 0) return 0;
    List<Location> removed = new ArrayList<>();
    HashMap<Location, Location> nextLevelNodes = store.get((short) (level + 1));
    Iterator<Location> iterator = store.iterator(level);
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
    locStore.remove(level, removed);

    if (!removed.isEmpty() && level > 1) {
      return removed.size() + filter(store, (short) (level - 1));
    } else {
      return removed.size();
    }
  }

  private static class Links {
    public final Location start;
    public final List<Location> nexts;

    public Links(Location start, List<Location> nexts) {
      this.start = start;
      this.nexts = nexts;
    }
  }

}
