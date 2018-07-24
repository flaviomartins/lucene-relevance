package org.novasearch.lucene.search.similarities;

import org.apache.lucene.search.similarities.ClassicSimilarity;

/**
 * LtcSimilarity is based on TF-IDF with logarithmic tf weighting.
 */
public class LtcSimilarity extends ClassicSimilarity {

  /**
   * Sole constructor: parameter-free
   */
  public LtcSimilarity() {
  }

  /**
   * Implemented as <code>1 + log(freq)</code>.
   */
  @Override
  public float tf(float freq) {
    return 1 + (float) Math.log(freq);
  }

  /**
   * Implemented as <code>log((docCount+1)/(docFreq+1)) + 1</code>.
   */
  @Override
  public float idf(long docFreq, long docCount) {
    return (float) (Math.log((docCount + 1) / (double) (docFreq + 1)) + 1.0);
  }

  @Override
  public String toString() {
    return "LtcSimilarity";
  }
}