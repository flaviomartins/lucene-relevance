package org.novasearch.lucene.search.similarities;

import org.apache.lucene.search.similarities.ClassicSimilarity;

/**
 * LtcSimilarity is based on Lucene's default scoring implementation,
 * but uses logarithmic tf weighting.
 * @lucene.experimental
 */
public class LtcSimilarity extends ClassicSimilarity {
  
  @Override
  public float tf(float freq) {
    return 1 + (float) Math.log(freq);
  }
  
  @Override
  public String toString() {
    return "LtcSimilarity";
  }
  
}
