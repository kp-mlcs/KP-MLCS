package mlcs;

import java.util.ArrayList;

public interface LocationFilterPolicy {
  void filter(ArrayList<Location> locs, int removedCount);

  class ReserveAll implements LocationFilterPolicy {
    public void filter(ArrayList<Location> locs, int removedCount) {
    }
  }

  class ReserveByPercent implements LocationFilterPolicy {
    private int maxReservedCount;
    private float percent;
    private Location.ScoreSorter sorter = null;

    public ReserveByPercent(Mlcs mlcs, float percent, int maxReservedCount) {
      this.percent = percent;
      this.maxReservedCount = maxReservedCount;
      this.sorter = new Location.ScoreSorter(mlcs);
    }


    public int calcReservedCount(int size) {
      return Math.max(maxReservedCount, (int) percent * size);
    }

    public void filter(ArrayList<Location> locs, int removedCount) {
      int rmCnt = removedCount;
      if (rmCnt == 0) {
        for (Location loc : locs) {
          if (loc.isDiscard()) rmCnt++;
        }
      }
      int reservedCount = calcReservedCount(locs.size() - rmCnt);
      if ((locs.size() - rmCnt) > reservedCount) {
        locs.sort(sorter);
        int i = 0;
        for (Location l : locs) {
          if (i < reservedCount) {
            if (!l.isDiscard()) i += 1;
          } else {
            l.setReserved(false);
          }
        }
      }
    }
  }
}