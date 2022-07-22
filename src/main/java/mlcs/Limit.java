/*
 * Beangle, Agile Development Scaffold and Toolkits.
 *
 * Copyright © 2005, The Beangle Software.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
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
