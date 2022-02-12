package mlcs;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * A approximation algorithm
 * It obtains the approximate length of the MLCS as soon as possible by retaining some points.
 * The strategy for filtering nodes is to rank by the respective sum values and then take a certain number.
 */
public class Pioneer {
  /**
   * Try to find a approximate length of the given MLCS
   */
  public static short search(Mlcs mlcs) {
    List<Location> routes = List.of(mlcs.start);
    short level = 0;
    int maxLocs = 5000;
    Location.ScoreSorter sorter = new Location.ScoreSorter();
    while (!routes.isEmpty()) {
      HashSet<Location> nexts = new HashSet<>();
      for (Location a : routes) {
        nexts.addAll(mlcs.nextDirectLocations(a));
      }
      if (nexts.isEmpty()) break;
      //building a IndexTree to mark dominated nodes.
      level += 1;
      IndexTree matrix = new IndexTree();
      for (Location n : nexts) {
        matrix.put(n);
      }
      matrix.build();
      ArrayList<Location> locs = new ArrayList<>();
      for (Location l : nexts) {
        if (!matrix.dominated(l)) {
          locs.add(l);
        }
      }

      // sorting and filtering
      if (locs.size() <= maxLocs) {
        routes = locs;
      } else {
        locs.sort(sorter);
        routes = locs.subList(0, maxLocs);
      }
    }
    return level;
  }
}
