package mlcs;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

public class QuickAP {

  public static short estimateLength(Mlcs mlcs, final int estimateCount, final int maxRetry, final int increment) {
    if (maxRetry <= 0) return estimateLength(mlcs, estimateCount, 0);

    int i = 0;
    short mlcsLength = 0;
    int retryCount = 0;
    while (retryCount < maxRetry) {
      short newLength = estimateLength(mlcs, estimateCount + i * increment, mlcsLength);
      i += 1;
      if (newLength > mlcsLength) {
        mlcsLength = newLength;
        retryCount = 0;
      } else {
        retryCount += 1;
      }
    }
    return mlcsLength;
  }

  /**
   * Try to find a approximate length of the given MLCS
   */
  public static short estimateLength(Mlcs mlcs, int estimateCount, int mlcsLength) {
    List<Location> routes = List.of(mlcs.start);
    Location.ScoreSorter sorter = new Location.ScoreSorter(mlcs);
    ForkJoinPool pool = mlcs.newPool();
    short level = 0;
    int maxLength = mlcs.maxLength;
    while (!routes.isEmpty()) {
      level += 1;
      HashSet<Location> nexts = new HashSet<>();
      if (mlcsLength <= 0) {
        for (Location a : routes) nexts.addAll(mlcs.nextLocations(a));
      } else {
        for (Location a : routes) nexts.addAll(mlcs.nextReachableLocations(a, mlcsLength, level));
      }
      if (nexts.isEmpty()) {
        level -= 1;
        break;
      }

      //building a IndexTree to mark dominated nodes.
      ArrayList<Location> fronts = new ArrayList<>(nexts);
//      Razor razor = new Razor(mlcs, level, true);
      Razor3 razor = new Razor3(mlcs, level);
      razor.shave(pool, fronts);
      ArrayList<Location> locs = new ArrayList<>();
      for (int i = 0, n = fronts.size(); i < n; i++) {
        Location loc = fronts.get(i);
        if (!loc.isDiscard()) locs.add(loc);
      }
      // sorting and filtering
      if (locs.size() <= estimateCount) {
        routes = locs;
      } else {
        locs.sort(sorter);
        routes = locs.subList(0, estimateCount);
      }
      //System.out.print("\restimate mlcs length..." + (level * 100.0 / maxLength) + "%");
    }
    System.out.println("\restimate mlcs length " + level + " 100% (reserve " + estimateCount + " points)");
    pool.shutdown();
    return level;
  }

}
