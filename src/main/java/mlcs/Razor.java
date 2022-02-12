package mlcs;

import mlcs.util.Queues;

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

  public Razor(short level) {
    this.level = level;
  }

  /**
   * Using multiple thread,dig in the IndexTree to find dominated locations.
   * @param pool thread pool
   * @param locs a list of locations
   * @return [original removed counts,new dominated counts]
   */
  public int[] shave(ForkJoinPool pool, ArrayList<Location> locs) {
    long startTime = System.currentTimeMillis();
    IndexTree all = new IndexTree();
    int disabled = 0;
    for (Location id : locs) {
      if (id.reserved) all.put(id);
      else disabled++;
    }
    all.build();

    LinkedList<ForkJoinTask<Integer>> tasks2 = new java.util.LinkedList<>();
    List<int[]> segs = Queues.split(locs.size(), pool.getParallelism());
    for (int[] seg : segs) {
      tasks2.add(pool.submit(new Blade(all, locs, seg[0], seg[1])));
    }
    int marked = 0;
    for (ForkJoinTask<Integer> task : tasks2) {
      marked += task.join();
    }

    System.out.println(this.level + " sort(" + (locs.size()-disabled) + "-" + marked + ") using " + (System.currentTimeMillis() - startTime));
    return new int[]{disabled, marked};
  }

  /**
   * A task which checks dominance relationship of nodes in range [from,to).
   */
  static class Blade extends RecursiveTask<Integer> {
    ArrayList<Location> locations;
    IndexTree matrix;
    int from, to;

    public Blade(IndexTree matrix, ArrayList<Location> locations, int from, int to) {
      super();
      this.matrix = matrix;
      this.locations = locations;
      this.from = from;
      this.to = to;
    }

    public Integer compute() {
      int count = 0;
      for (int i = from; i < to; i++) {
        Location src = locations.get(i);
        if (src.reserved && matrix.dominated(src)) {
          src.reserved = false;
          count += 1;
        }
      }
      return count;
    }
  }
}
