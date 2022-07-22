package mlcs;

import java.util.List;

public class Links {
  public final Location start;
  public final List<Location> nexts;

  public Links(Location start, List<Location> nexts) {
    this.start = start;
    this.nexts = nexts;
  }
}
