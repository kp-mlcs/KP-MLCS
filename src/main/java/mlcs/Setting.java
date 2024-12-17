package mlcs;

import java.util.*;
import java.util.concurrent.ForkJoinPool;

/**
 * 设置
 */
public class Setting {

  final String algo;

  final int parallelism;

  final Map<String, String> args;

  String outputFile;

  final List<Observer> observers = new ArrayList<>();

  public Setting(String algo, int parallelism, Map<String, String> args) {
    this.algo = algo;
    this.parallelism = parallelism;
    this.args = args;
  }

  public static Setting parse(Map<String, String> arguments) {
    var algo = arguments.getOrDefault("algo", "quick_ap");
    var cores = Runtime.getRuntime().availableProcessors();
    var parallelism = Integer.parseInt(arguments.getOrDefault("parallelism", "0"));
    int p = (parallelism <= 0) ? cores : parallelism;
    return new Setting(algo, p, arguments);
  }

  public static Setting parse(String[] args) {
    String algorithm = "quick_ap";
    Map<String, String> arguments = new HashMap<>();
    for (int i = 0; i < args.length; i++) {
      String arg = args[i];
      int eqIndx = arg.indexOf('=');
      if (eqIndx == -1)
        eqIndx = arg.indexOf(':');
      if (arg.startsWith("algo")) {
        algorithm = arg.substring(eqIndx + 1).trim();
        arguments.put("algo", algorithm);
      } else if (eqIndx > 0) {
        String key = arg.substring(0, eqIndx).trim();
        String value = arg.substring(eqIndx + 1).trim();
        arguments.put(key, value);
      }
    }
    var algos = Set.of("ep", "ap", "quick_ap");
    if (!algos.contains(algorithm)) {
      throw new RuntimeException("Unsupported algorithm " + algorithm + ",using ep|ap|quick_ap instead.");
    }
    return parse(arguments);
  }

  public ForkJoinPool newPool() {
    return (parallelism > 0) ? new ForkJoinPool(parallelism) : new ForkJoinPool();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (Map.Entry<String, String> entry : args.entrySet()) {
      sb.append(" " + entry.getKey() + "=" + entry.getValue() + "\n");
    }
    return "  algo=" + algo + "\n  parallelism=" + parallelism + "\n  " + sb;
  }

  public void notify(String msg) {
    for (Observer o : observers) {
      o.notify(msg);
    }
  }

  public void finish(Result result) {
    for (Observer o : observers) {
      o.onFinish(result);
    }
  }

  public void clearObservers() {
    this.observers.clear();
  }

  public void addObserver(Observer observer) {
    this.observers.add(observer);
  }

  public void removeObserver(Observer observer) {
    this.observers.remove(observer);
  }

  public static final class Env {

    final int cores;
    /**
     * how many MB
     */
    final long memory;

    final String os;

    final String jre;

    public Env(int cores, long memory, String os, String jre) {
      this.cores = cores;
      this.memory = memory;
      this.os = os;
      this.jre = jre;
    }

    public static Env get() {
      var cores = Runtime.getRuntime().availableProcessors();
      var os = System.getProperty("os.name") + " " + System.getProperty("os.version") + " " + System.getProperty("os.arch");
      var memory = Runtime.getRuntime().totalMemory() / 1024 / 1024;
      var jre = System.getProperty("java.vm.version") + "(" + System.getProperty("java.vendor") + ")";
      return new Env(cores, memory, os, jre);
    }

    public static Env parse(String[] args) {
      Map<String, String> arguments = new HashMap<>();
      for (int i = 0; i < args.length; i++) {
        String arg = args[i];
        int eqIndx = arg.indexOf(':');
        if (eqIndx > 0) {
          String key = arg.substring(0, eqIndx).trim();
          String value = arg.substring(eqIndx + 1).trim();
          arguments.put(key, value);
        }
      }
      var cores = Integer.parseInt(arguments.getOrDefault("cores", "0"));
      var os = arguments.getOrDefault("os", "unkown");
      var jre = arguments.getOrDefault("jre", "unkown");
      var memory = Long.parseLong(arguments.getOrDefault("memory", "unkown"));
      return new Env(cores, memory, os, jre);
    }

    @Override
    public String toString() {
      return "  cores:" + cores + "\n  memory:" + memory + "\n  os:" + os + "\n  jre:" + jre;
    }
  }

}
