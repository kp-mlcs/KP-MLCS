package mlcs;

import mlcs.util.Queues;
import mlcs.util.Stopwatch;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;

/**
 * Dominated nodes razor.
 * Using non-dominant filtering, mark dominated locations.
 */
public class Razor {
  short level;
  Mlcs mlcs;
  boolean weakDominated = false;

  public Razor(Mlcs mlcs, short level) {
    this.level = level;
    this.mlcs = mlcs;
  }

  public Razor(Mlcs mlcs, short level, boolean weakDominated) {
    this.weakDominated = weakDominated;
    this.level = level;
    this.mlcs = mlcs;
  }

  /**
   * Using multiple thread,dive in the IndexTree to find dominated locations.
   * @param pool thread pool
   * @param locs a list of locations
   * @return [original removed counts,new dominated counts]
   */
  public int[] shave(ForkJoinPool pool, ArrayList<Location> locs) {
    long startTime = System.currentTimeMillis();
    //1. create index tree
    IndexTree indexTree = new IndexTree(mlcs.seqs.size());
    int disabled = 0;
    for (Location id : locs) {
      if (!id.isDiscard()) indexTree.put(id);
      else disabled++;
    }
    //2. build index tree
    indexTree.build();

    //3. filter locations by dominate relation
    LinkedList<ForkJoinTask<Integer>> tasks2 = new java.util.LinkedList<>();
    List<int[]> segs = Queues.split(locs.size(), pool.getParallelism());
    for (int[] seg : segs) {
      tasks2.add(pool.submit(new Blade(indexTree, locs, seg[0], seg[1], weakDominated)));
    }
    //collect results
    int marked = 0;
    for (ForkJoinTask<Integer> task : tasks2) {
      marked += task.join();
    }
    System.out.println(this.level + " sort(" + (locs.size() - disabled) + "-" + marked + ") using " + Stopwatch.format(System.currentTimeMillis() - startTime));
    return new int[]{disabled, marked};
  }

  /**
   * A task which checks dominance relationship of nodes in range [from,to).
   */
  static class Blade extends RecursiveTask<Integer> {
    ArrayList<Location> locations;
    IndexTree matrix;
    int from, to;
    boolean weakDominated;

    public Blade(IndexTree matrix, ArrayList<Location> locations, int from, int to, boolean weakDominated) {
      super();
      this.matrix = matrix;
      this.locations = locations;
      this.from = from;
      this.to = to;
      this.weakDominated = weakDominated;
    }

    public Integer compute() {
      int count = 0;
      for (int i = from; i < to; i++) {
        Location src = locations.get(i);
        if (src.isUnknown()) {
          if (matrix.dominated(src)) {
            src.setReserved(false);
            count += 1;
          } else {
            src.setReserved(true);
          }
        }
      }
      return count;
    }
  }
}
