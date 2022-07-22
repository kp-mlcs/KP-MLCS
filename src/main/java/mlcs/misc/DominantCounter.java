package mlcs.misc;

import mlcs.Location;
import mlcs.Mlcs;
import mlcs.Node;
import mlcs.util.FileSearcher;
import mlcs.util.Queues;
import mlcs.util.Stopwatch;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;

public class DominantCounter {
  public Mlcs mlcs;
  public final Node start; // startnode
  Matrix matrix;

  public DominantCounter(Mlcs mlcs) {
    super();
    this.mlcs = mlcs;
    start = new Node(mlcs.start);
    matrix = new Matrix();
  }

  public static void main(String[] args) throws Exception {
    if (args.length == 0) {
      System.out.println("Usage:DominantCounter /path/to/your/data/file");
      return;
    }
    for (String arg : args) {
      List<File> files = FileSearcher.find(arg);
      for (File file : files) {
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


  private static void searchFile(File source) throws Exception {
    long startAt = System.currentTimeMillis();
    Mlcs mlcs = Mlcs.build(Mlcs.loadData(source));
    DominantCounter counter = new DominantCounter(mlcs);
    counter.search();
    BigDecimal matchedCounts = mlcs.matchedCounts();
    System.out.println("calculate matched points is " + matchedCounts);
    double percents = counter.matrix.size * 1.0 / matchedCounts.longValue() * 100;
    System.out.println("find " + counter.matrix.size + "(" + percents + "% of matched points) dominants using " + Stopwatch.format(System.currentTimeMillis() - startAt));
  }

  private void search() {
    ArrayList<Location> fronts = new ArrayList<>();
    fronts.add(start.id);

    ForkJoinPool pool = this.mlcs.newPool();
    int i = 0;
    while (!fronts.isEmpty()) {
      //开始多线程遍历,登记新点
      List<int[]> segs = Queues.split(fronts.size(), pool.getParallelism());
      LinkedList<ForkJoinTask<List<Links>>> tasks = new java.util.LinkedList<>();
      for (int[] seg : segs) {
        tasks.add(pool.submit(new SearchCrawler(mlcs, fronts, seg[0], seg[1])));
      }
      fronts = new ArrayList<>();
      for (ForkJoinTask<List<Links>> task : tasks) {
        List<Links> pairs = task.join();
        for (Links p : pairs) {
          for (Location l : p.nexts) {
            if (matrix.put(l)) fronts.add(l);
          }
        }
      }
      System.out.println("round " + (++i) + " fond " + fronts.size() + " nodes");
    }
  }

  static class SearchCrawler extends RecursiveTask<List<Links>> {
    Mlcs mlcs;
    ArrayList<Location> locations;
    int from, to;

    public SearchCrawler(Mlcs mlcs, ArrayList<Location> locations, int from, int to) {
      super();
      this.mlcs = mlcs;
      this.locations = locations;
      this.from = from;
      this.to = to;
    }

    public List<Links> compute() {
      List<Links> results = new ArrayList<>();
      for (int i = from; i < to; i++) {
        Location n = locations.get(i);
        List<Location> ns = mlcs.nextLocations(n);
        if (!ns.isEmpty()) results.add(new Links(n, ns));
      }
      return results;
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
