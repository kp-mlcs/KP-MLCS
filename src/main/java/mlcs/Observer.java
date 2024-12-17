package mlcs;

public interface Observer {
  default void notify(String msg) {
  }

  default void onFinish(Result result) {
  }

  /**
   * Debug Observer
   */
  public static class Debuger implements Observer {
    @Override
    public void notify(String msg) {
      System.out.println(msg);
    }

  }

  /**
   * Dump result to file
   */
  public static class Dumper implements Observer {
    public final String outputFile;

    public Dumper(String outputFile) {
      this.outputFile = outputFile;
    }

    @Override
    public void onFinish(Result result) {
      System.out.println("dump output to " + outputFile);
      result.dumpTo(outputFile);
    }
  }

  public static class Visualizer implements Observer {
    @Override
    public void onFinish(Result result) {
      if (result.graph.maxLevel < 300) result.visualize();
    }
  }
}

