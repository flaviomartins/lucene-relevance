package org.novasearch.lucene.search.similarities;

import org.apache.lucene.search.CollectionStatistics;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.TermStatistics;

/**
 * Expert: TF-IDF with logarithmic tf weighting. You might want to consider using
 * {@link BM25Similarity} instead, which is generally considered superior to
 * TF-IDF.
 */
public class LncLtcSimilarity extends TFIDFSimilarity {

  /** Sole constructor: parameter-free */
  public LncLtcSimilarity() {
  }

  /** Implemented as
   *  <code>1/sqrt(1 + log(length))</code>.
   *
   *  @lucene.experimental */
  @Override
  public float lengthNorm(int numTerms) {
    return (float) (1.0 / (Math.sqrt(1 + Math.log(numTerms))));
  }

  /** Implemented as
   *  <code>1/sqrt(1 + log(length))</code>.
   *
   *  @lucene.experimental */
  @Override
  public float queryNorm(float numTerms) {
    return (float) (1.0 / Math.sqrt(1 + Math.log(numTerms)));
  }

  /** Implemented as <code>sqrt(1 + log(freq))</code>. */
  @Override
  public float qtf(float freq) {
    return 1 + (float)Math.log(freq);
  }

  /** Implemented as <code>sqrt(1 + log(freq))</code>. */
  @Override
  public float tf(float freq) {
    return (float)Math.sqrt(1 + Math.log(freq));
  }

  @Override
  public Explanation idfExplain(CollectionStatistics collectionStats, TermStatistics termStats) {
    final long df = termStats.docFreq();
    final long docCount = collectionStats.docCount();
    final float idf = idf(df, docCount);
    return Explanation.match(idf, "idf, computed as log((docCount+1)/(docFreq+1)) + 1 from:",
        Explanation.match(df, "docFreq, number of documents containing term"),
        Explanation.match(docCount, "docCount, total number of documents with field"));
  }

  /** Implemented as <code>log((docCount+1)/(docFreq+1)) + 1</code>. */
  @Override
  public float idf(long docFreq, long docCount) {
    return (float)(Math.log((docCount+1)/(double)(docFreq+1)) + 1.0);
  }

  @Override
  public String toString() {
    return "LtcSimilarity";
  }
}