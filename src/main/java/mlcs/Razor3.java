package mlcs;

import mlcs.util.Queues;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;

/**
 * Dominated nodes razor.
 * Using non-dominant filtering, mark dominated locations.
 */
public class Razor3 {

  Mlcs mlcs;
  short level;

  public Razor3(Mlcs mlcs, int level) {
    this.level = (short) level;
    this.mlcs = mlcs;
  }

  /**
   * Using multiple thread,dig in the IndexTree to find dominated locations.
   *
   * @param pool thread pool
   * @param locs a list of locations
   * @return [original removed counts,new dominated counts]
   */
  public int[] shave(ForkJoinPool pool, ArrayList<Location> locs) {
    long startTime = System.currentTimeMillis();
    //1.1.create a minimal index tree by key locations
    int disabled = 0;
    for (Location id : locs) {
      if (id.isDiscard()) disabled++;
    }
    Set<Location> keyLocs = IndexTree.findKeyLocs(locs);
    IndexTree keyTree = new IndexTree(mlcs.seqs.size());
    for (Location id : keyLocs) {
      id.setReserved(true);
      keyTree.put(id);
    }
    // 1.2. build the minimal index tree
    keyTree.build();

    // 1.3. filter all locations using the minimal tree
    int marked = 0;
    LinkedList<ForkJoinTask<Integer>> tasks2 = new LinkedList<>();
    List<int[]> segs = Queues.split(locs.size(), pool.getParallelism());
    for (int[] seg : segs) {
      tasks2.add(pool.submit(new Blade(keyTree, locs, seg[0], seg[1], false)));
    }
    int keyMarked = 0;
    for (ForkJoinTask<Integer> task : tasks2) {
      int newer = task.join();
      marked += newer;
      keyMarked += newer;
    }

    //2.1 create a index tree which contains all locations.
    IndexTree otherTree = new IndexTree(mlcs.seqs.size());
    for (Location id : locs) {
      if (!id.isDiscard() && !keyLocs.contains(id)) {
        id.status = 0;
        otherTree.put(id);
      }
    }
    //2.2 build the tree
    otherTree.build();
    //2.3 filter
    segs = Queues.split(locs.size(), pool.getParallelism());
    tasks2.clear();
    for (int[] seg : segs) {
      tasks2.add(pool.submit(new Blade(otherTree, locs, seg[0], seg[1], false)));
    }
    int otherMarked = 0;
    for (ForkJoinTask<Integer> task : tasks2) {
      int newer = task.join();
      otherMarked += newer;
      marked += newer;
    }
    System.out.println(this.level + " sort(" + (locs.size() - disabled) + "-" + marked + "{" + keyMarked + "," + otherMarked + "}" + ") using " + (System.currentTimeMillis() - startTime));
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
