package mlcs;

import mlcs.util.Env;
import mlcs.util.Stopwatch;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static mlcs.util.FileSearcher.*;

public class KPMLCS {

  public static void main(String[] args) throws Exception {
    if (args.length == 0) {
      System.out.println("Usage:KPMLCS /path/to/your/data/file algo=[ep|ap|quick_ap] [other=value]");
      System.out.println("      KPMLCS /path/to/your/data/file algo=ep");
      System.out.println("      KPMLCS /path/to/your/data/file algo=ap [precision=0.2] [maxReserved=500]");
      System.out.println("      KPMLCS /path/to/your/data/file algo=quick_ap [estimateCount=length] [maxRetry=0] [increment=length/2]");
      return;
    }
    Env.print();
    String algorithm = "quick_ap";
    Map<String, String> arguments = new HashMap<>();
    if (args.length >= 2) {
      for (int i = 1; i < args.length; i++) {
        String arg = args[i];
        int eqIndx = arg.indexOf('=');
        if (arg.startsWith("algo")) {
          algorithm = arg.substring(eqIndx + 1).trim();
        } else if (eqIndx > 0) {
          String key = arg.substring(0, eqIndx).trim();
          String value = arg.substring(eqIndx + 1).trim();
          arguments.put(key, value);
        }
      }
    }

    List<File> files = find(args[0]);
    for (File sourceFile : files) {
      System.out.println("processing file " + sourceFile + " using algorithm:" + algorithm);
      if (algorithm.equals("ep")) {
        ep(sourceFile, arguments);
      } else if (algorithm.equals("ap")) {
        ap(sourceFile, arguments);
      } else if (algorithm.equals("quick_ap")) {
        quickAp(sourceFile, arguments);
      } else {
        System.out.println("Unsupported algorithm " + algorithm + ",using ep|ap|quick_ap instead.");
      }
    }
  }

  /**
   * Exact Precision algorithm.
   *
   * @param sourceFile
   * @throws IOException
   */
  public static void ep(File sourceFile, Map<String, String> arguments) throws IOException {
    long startAt = System.currentTimeMillis();
    Mlcs mlcs = Mlcs.build(Mlcs.loadData(sourceFile));
    LocationStore store = buildStore(mlcs);
    int maxLevel = estimateLength(mlcs, arguments);
    System.out.println("obtain max length " + maxLevel);
    Limit limit = new Limit(mlcs.maxLength, maxLevel);
    EPCrawler crawler = new EPCrawler(mlcs, store, limit);
    Graph graph = crawler.search();
    String resultFile = getOutFile(sourceFile, "ep_" + getFileShortName(sourceFile) + ".txt");
    statResult(graph, store, resultFile, startAt);
  }

  /**
   * Approximate Precision algorithm.
   *
   * @param sourceFile
   * @param arguments
   * @throws IOException
   */
  public static void ap(File sourceFile, Map<String, String> arguments) throws IOException {
    long startAt = System.currentTimeMillis();
    Mlcs mlcs = Mlcs.build(Mlcs.loadData(sourceFile));
    float precision = Float.parseFloat(arguments.getOrDefault("precision", "0.2"));
    int maxReserved = Integer.parseInt(arguments.getOrDefault("maxReserved", String.valueOf(mlcs.maxLength)));
    int estimateCount = Integer.parseInt(arguments.getOrDefault("estimateCount", String.valueOf(mlcs.maxLength)));
    if (maxReserved < estimateCount) {
      throw new RuntimeException("Parameter maxReserved cannot less than estimateCount");
    }
    if (Float.compare(1, precision) <= 0) {
      System.out.println("The precision that approximate algorithm accepted should less than 1");
      System.exit(1);
    }
    LocationStore store = buildStore(mlcs);
    int maxLevel = estimateLength(mlcs, arguments);
    System.out.println("obtain max length " + maxLevel);
    Limit limit = new Limit(mlcs.maxLength, maxLevel);
    APCrawler apCrawler = new APCrawler(mlcs, store, limit, precision, maxReserved);
    Graph graph = apCrawler.search();
    String resultFile = getOutFile(sourceFile, "ap_" + getFileShortName(sourceFile) + "_" + precision + ".txt");
    statResult(graph, store, resultFile, startAt);
  }

  /**
   * Quick Approximate Precision length algorithm.
   *
   * @param sourceFile
   * @param arguments
   * @throws IOException
   */
  public static void quickAp(File sourceFile, Map<String, String> arguments) throws IOException {
    long startAt = System.currentTimeMillis();
    Mlcs mlcs = Mlcs.build(Mlcs.loadData(sourceFile));
    int mlcsLength = estimateLength(mlcs, arguments);
    long endAt = System.currentTimeMillis();
    System.out.println("file:" + sourceFile + " n=" + mlcs.maxLength + " length=" + mlcsLength + " using:" + Stopwatch.format(endAt - startAt));
  }

  private static short estimateLength(Mlcs mlcs, Map<String, String> arguments) {
    int defaultEstimateCount = mlcs.maxLength;
    int estimateCount = Integer.parseInt(arguments.getOrDefault("estimateCount", String.valueOf(defaultEstimateCount)));
    int maxRetry = Integer.parseInt(arguments.getOrDefault("maxRetry", "0"));
    int increment = Integer.parseInt(arguments.getOrDefault("increment", String.valueOf(defaultEstimateCount)));
    return QuickAP.estimateLength(mlcs, estimateCount, maxRetry, increment);
  }

  private static void statResult(Graph graph, LocationStore store, String resultFile, long startAt) {
    Result result = graph.stat(store.totalSize, store.maxSize, startAt);
    System.out.println(result.buildResultString());
    result.dumpTo(resultFile);
    System.out.println("find " + result.mlcsCount + " mlcs(length " + result.maxLevel + ")");
    if (graph.maxLevel < 300) result.visualize();
  }

  private static LocationStore buildStore(Mlcs mlcs) {
    long m = Runtime.getRuntime().maxMemory() / 1024 / 1024; // Mb
    //Location(array(seqs_size*2)8 + hashcode4+reserved1),
    int nodePerMB = (int) (1000000 / (2 * mlcs.seqs.size() + 4 + 8 + 1) * 0.75 * 0.2);
    return new LocationStore(mlcs, m * nodePerMB);
  }


}
