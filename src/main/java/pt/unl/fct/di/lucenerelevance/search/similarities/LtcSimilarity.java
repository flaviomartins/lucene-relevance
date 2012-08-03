package pt.unl.fct.di.lucenerelevance.search.similarities;

import org.apache.lucene.search.similarities.DefaultSimilarity;

/**
 * LtcSimilarity is based on Lucene's default scoring implementation,
 * but uses logarithmic tf weighting.
 * @lucene.experimental
 */
public class LtcSimilarity extends DefaultSimilarity {
  
  @Override
  public float tf(float freq) {
    return 1 + (float) Math.log(freq);
  }
  
  @Override
  public String toString() {
    return "LtcSimilarity";
  }
  
}
