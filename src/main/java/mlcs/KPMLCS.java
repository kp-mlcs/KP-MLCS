package mlcs;

import mlcs.util.Stopwatch;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static mlcs.util.FileSearcher.*;

public class KPMLCS {

  public static void main(String[] args) throws Exception {
    if (args.length == 0) {
      System.out.println("Usage:KPMLCS /path/to/your/data/file algo=[ep|ap|quick_ap] [parallelism=32] [other=value]");
      System.out.println("      KPMLCS /path/to/your/data/file algo=ep");
      System.out.println("      KPMLCS /path/to/your/data/file algo=ap [precision=0.2] [maxReserved=500]");
      System.out.println("      KPMLCS /path/to/your/data/file algo=quick_ap [estimateCount=length] [maxRetry=0] [increment=length/2]");
      return;
    }
    var setting = Setting.parse(Arrays.copyOfRange(args, 1, args.length));
    List<File> files = find(args[0]);

    for (File sourceFile : files) {
      var data = Mlcs.loadData(sourceFile);
      Mlcs mlcs = Mlcs.build(data);
      setting.clearObservers();
      String resultFile = getOutFile(sourceFile, setting.algo + "_" + getFileShortName(sourceFile) + ".txt");
      setting.addObserver(new Observer.Debuger());
      setting.addObserver(new Observer.Dumper(resultFile));
      setting.addObserver(new Observer.Visualizer());
      setting.notify("processing file " + sourceFile + " using algorithm:" + setting.algo);
      setting.addObserver(new Observer.Debuger());
      mine(mlcs, setting);
    }
  }

  public static void mine(Mlcs mlcs, Setting setting) {
    switch (setting.algo) {
      case "ep":
        ep(mlcs, setting);
        break;
      case "ap":
        ap(mlcs, setting);
        break;
      case "quick_ap":
        quickAp(mlcs, setting);
        break;
      default:
        throw new RuntimeException("unknown algorithm:" + setting.algo);
    }
  }

  /**
   * Exact Precision algorithm.
   *
   * @param mlcs
   * @throws IOException
   */
  public static void ep(Mlcs mlcs, Setting setting) {
    long startAt = System.currentTimeMillis();
    LocationStore store = buildStore(mlcs);
    int maxLevel = estimateLength(mlcs, setting);
    setting.notify("obtain max length " + maxLevel);
    Limit limit = new Limit(mlcs.maxLength, maxLevel);
    EPCrawler crawler = new EPCrawler(mlcs, setting, store, limit);
    Graph graph = crawler.search();
    statResult(setting, graph, store, startAt);
  }

  /**
   * Approximate Precision algorithm.
   *
   * @param mlcs
   * @param setting
   * @throws IOException
   */
  public static void ap(Mlcs mlcs, Setting setting) {
    long startAt = System.currentTimeMillis();
    float precision = Float.parseFloat(setting.args.getOrDefault("precision", "0.2"));
    int maxReserved = Integer.parseInt(setting.args.getOrDefault("maxReserved", String.valueOf(mlcs.maxLength)));
    int estimateCount = Integer.parseInt(setting.args.getOrDefault("estimateCount", String.valueOf(mlcs.maxLength)));
    if (maxReserved < estimateCount) {
      var msg = "Parameter maxReserved cannot less than estimateCount";
      setting.notify(msg);
      throw new RuntimeException(msg);
    }
    if (Float.compare(1, precision) <= 0) {
      var msg = "The precision that approximate algorithm accepted should less than 1";
      setting.notify(msg);
      System.exit(1);
    }
    LocationStore store = buildStore(mlcs);
    int maxLevel = estimateLength(mlcs, setting);
    setting.notify("obtain max length " + maxLevel);
    Limit limit = new Limit(mlcs.maxLength, maxLevel);
    APCrawler apCrawler = new APCrawler(mlcs, setting, store, limit, precision, maxReserved);
    Graph graph = apCrawler.search();
    statResult(setting, graph, store, startAt);
  }

  /**
   * Quick Approximate Precision length algorithm.
   *
   * @param mlcs
   * @param setting
   * @throws IOException
   */
  public static void quickAp(Mlcs mlcs, Setting setting) {
    long startAt = System.currentTimeMillis();
    int mlcsLength = estimateLength(mlcs, setting);
    long endAt = System.currentTimeMillis();
    setting.notify(" n=" + mlcs.maxLength + " length=" + mlcsLength + " using:" + Stopwatch.format(endAt - startAt));
  }

  private static short estimateLength(Mlcs mlcs, Setting setting) {
    int defaultEstimateCount = mlcs.maxLength;
    int estimateCount = Integer.parseInt(setting.args.getOrDefault("estimateCount", String.valueOf(defaultEstimateCount)));
    int maxRetry = Integer.parseInt(setting.args.getOrDefault("maxRetry", "0"));
    int increment = Integer.parseInt(setting.args.getOrDefault("increment", String.valueOf(defaultEstimateCount)));
    return QuickAP.estimateLength(mlcs, setting, estimateCount, maxRetry, increment);
  }

  private static void statResult(Setting setting, Graph graph, LocationStore store, long startAt) {
    Result result = graph.stat(setting, store.totalSize, store.maxSize, startAt);
    setting.notify("find " + result.mlcsCount + " mlcs(length " + result.maxLevel + ")");
    setting.finish(result);
  }

  private static LocationStore buildStore(Mlcs mlcs) {
    long m = Runtime.getRuntime().maxMemory() / 1024 / 1024; // Mb
    //Location(array(seqs_size*2)8 + hashcode4+reserved1),
    int nodePerMB = (int) (1000000 / (2 * mlcs.seqs.size() + 4 + 8 + 1) * 0.75 * 0.2);
    return new LocationStore(mlcs, m * nodePerMB);
  }

}
