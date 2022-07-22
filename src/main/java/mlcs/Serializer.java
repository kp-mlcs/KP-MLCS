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

import java.util.Arrays;

/**
 * Serializer
 * It provides a node-to-byte serialization and deserialization mechanism.
 */
public class Serializer {
  private final int bits;
  public final int bytes;
  private final int dimension;
  private final int mask;

  public Serializer(Mlcs mlcs) {
    this(mlcs.maxLength, mlcs.seqs.size());
  }

  public Serializer(int maxLength, int dimension) {
    this.dimension = dimension;
    this.bits = maxBits(maxLength);
    this.bytes = maxBytes(dimension, this.bits);
    char[] v = new char[bits];
    Arrays.fill(v, '1');
    this.mask = Integer.parseInt(new String(v), 2);
  }

  /**
   * Calculate max bytes which contains dimension*bit bits;
   * @param dimension
   * @param bits
   * @return
   */
  private int maxBytes(int dimension, int bits) {
    int l = dimension * bits;
    return (l % 8 > 0) ? l / 8 + 1 : l / 8;
  }

  /**
   * Calculate the max bits needed by maxLength
   * for example, expression 1-7 need 3 bits;8-15 need 4 bits;
   * @param maxLength
   * @return
   */
  private int maxBits(int maxLength) {
    int i = 1;
    int base = 2;
    while (base - 1 < maxLength) {
      base = base * 2;
      i += 1;
    }
    return i;
  }

  /**
   * Convert a short integer to byte array.
   * @param a
   * @param offset left shift offset
   * @return
   */
  private byte[] toBytes(short a, int offset) {
    int num = (bits + offset) % 8 > 0 ? (bits + offset) / 8 + 1 : (bits + offset) / 8;
    int tmp = (((int) a) << offset);
    byte[] bits = new byte[num];
    for (short i = 0; i < num; i++) {
      bits[i] = (byte) (tmp >> (8 * i));
    }
    return bits;
  }

  /**
   * Convert a location to byte array
   * @param loc
   * @return
   */
  public byte[] toBytes(Location loc) {
    byte[] bin = new byte[bytes];
    int bitIdx = 0;
    for (short idx : loc.index) {
      int offset = bitIdx % 8;
      int startBytes = bitIdx / 8;
      byte[] data = toBytes(idx, offset);
      for (byte d : data) {
        bin[startBytes] |= d;
        startBytes += 1;
      }
      bitIdx += bits;
    }
    return bin;
  }

  /**
   * deserialize bytes to Location
   * @param data
   * @return
   */
  public Location fromBytes(byte[] data) {
    short[] idx = new short[dimension];
    int total = dimension * bits;
    int bitIdx = 0;
    int k = 0;
    while (bitIdx < total) {
      int offset = bitIdx % 8;
      int startBytes = bitIdx / 8;
      int endBytes = (bitIdx + bits - 1) / 8;
      int tmp = 0;
      for (int i = startBytes; i <= endBytes; i++) {
        tmp |= ((data[i] & 0xFF) << (8 * (i - startBytes)));
      }
      tmp >>= offset;
      tmp &= mask;
      idx[k++] = (short) tmp;
      bitIdx += bits;
    }
    return new Location(idx);
  }
}
