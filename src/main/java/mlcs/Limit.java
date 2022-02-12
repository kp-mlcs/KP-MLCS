package mlcs;

/**
 * Mlcs problem limit
 */
public class Limit {
  public final int maxLength; // the max length of origin sequences
  public int mlcsLength; // the max length of solution.

  public Limit(int maxLength, int mlcsLength) {
    this.maxLength = maxLength;
    this.mlcsLength = mlcsLength;
  }

  @Override
  public String toString() {
    return "(" + maxLength + "," + mlcsLength + ')';
  }
}
