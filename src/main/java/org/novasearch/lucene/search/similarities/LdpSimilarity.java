/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.novasearch.lucene.search.similarities;

import java.util.ArrayList;
import java.util.List;
import org.apache.lucene.search.CollectionStatistics;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.TermStatistics;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.util.SmallFloat;

/**
 * Ldp Similarity.
 */
public class LdpSimilarity extends Similarity {
  private final float b;
  private final float d;

  /**
   * Ldp with the supplied parameter values.
   *
   * @param b Controls to what degree document length normalizes tf values.
   * @param d Controls lower-bound of term frequency normalization in Ldp.
   * @throws IllegalArgumentException if {@code b} is not within the range
   * {@code [0..1]}, or if {@code d} is not within the range {@code [0..1]}
   */
  public LdpSimilarity(float b, float d, boolean discountOverlaps) {
    super(discountOverlaps);
    if (Float.isNaN(b) || b < 0 || b > 1) {
      throw new IllegalArgumentException("illegal b value: " + b + ", must be between 0 and 1");
    }
    if (Float.isNaN(d) || d < 0 || d > 1) {
      throw new IllegalArgumentException("illegal d value: " + d + ", must be between 0 and 1");
    }
    this.b = b;
    this.d = d;
  }

  /** Primary constructor. */
  public LdpSimilarity(boolean discountOverlaps) {
    this(0.75f, 0.5f, discountOverlaps);
  }

  /** Default constructor: parameter-free */
  public LdpSimilarity() {
    this(true);
  }

  /** Implemented as <code>log(1 + (docCount - docFreq + 0.5)/(docFreq + 0.5))</code>. */
  protected float idf(long docFreq, long docCount) {
    return (float) Math.log(1 + (docCount - docFreq + 0.5D) / (docFreq + 0.5D));
  }

  /** The default implementation computes the average as <code>sumTotalTermFreq / docCount</code> */
  protected float avgFieldLength(CollectionStatistics collectionStats) {
    return (float) (collectionStats.sumTotalTermFreq() / (double) collectionStats.docCount());
  }

  /** Cache of decoded bytes. */
  private static final float[] LENGTH_TABLE = new float[256];

  static {
    for (int i = 0; i < 256; i++) {
      LENGTH_TABLE[i] = SmallFloat.byte4ToInt((byte) i);
    }
  }

  /**
   * Computes a score factor for a simple term and returns an explanation for that score factor.
   *
   * <p>The default implementation uses:
   *
   * <pre class="prettyprint">
   * idf(docFreq, docCount);
   * </pre>
   *
   * Note that {@link CollectionStatistics#docCount()} is used instead of {@link
   * org.apache.lucene.index.IndexReader#numDocs() IndexReader#numDocs()} because also {@link
   * TermStatistics#docFreq()} is used, and when the latter is inaccurate, so is {@link
   * CollectionStatistics#docCount()}, and in the same direction. In addition, {@link
   * CollectionStatistics#docCount()} does not skew when fields are sparse.
   *
   * @param collectionStats collection-level statistics
   * @param termStats term-level statistics for the term
   * @return an Explain object that includes both an idf score factor and an explanation for the
   *     term.
   */
  public Explanation idfExplain(CollectionStatistics collectionStats, TermStatistics termStats) {
    final long df = termStats.docFreq();
    final long docCount = collectionStats.docCount();
    final float idf = idf(df, docCount);
    return Explanation.match(
        idf,
        "idf, computed as log(1 + (N - n + 0.5) / (n + 0.5)) from:",
        Explanation.match(df, "n, number of documents containing term"),
        Explanation.match(docCount, "N, total number of documents with field"));
  }

  /**
   * Computes a score factor for a phrase.
   *
   * <p>The default implementation sums the idf factor for each term in the phrase.
   *
   * @param collectionStats collection-level statistics
   * @param termStats term-level statistics for the terms in the phrase
   * @return an Explain object that includes both an idf score factor for the phrase and an
   *     explanation for each term.
   */
  public Explanation idfExplain(CollectionStatistics collectionStats, TermStatistics[] termStats) {
    double idf = 0d; // sum into a double before casting into a float
    List<Explanation> details = new ArrayList<>();
    for (final TermStatistics stat : termStats) {
      Explanation idfExplain = idfExplain(collectionStats, stat);
      details.add(idfExplain);
      idf += idfExplain.getValue().floatValue();
    }
    return Explanation.match((float) idf, "idf, sum of:", details);
  }

  @Override
  public final SimScorer scorer(
      float boost, CollectionStatistics collectionStats, TermStatistics... termStats) {
    Explanation idf =
        termStats.length == 1
            ? idfExplain(collectionStats, termStats[0])
            : idfExplain(collectionStats, termStats);
    float avgdl = avgFieldLength(collectionStats);

    float[] cache = new float[256];
    for (int i = 0; i < cache.length; i++) {
      cache[i] = ((1 - b) + b * LENGTH_TABLE[i] / avgdl);
    }
    return new LdpScorer(boost, b, d, idf, avgdl, cache);
  }

  /** Collection statistics for the BM25 model. */
  private static class LdpScorer extends SimScorer {
    /** query boost */
    private final float boost;
    /** b value for length normalization impact */
    private final float b;

    /** d parameter */
    private final float d;
    /** BM25's idf */
    private final Explanation idf;

    /** The average document length. */
    private final float avgdl;

    /** precomputed norm[256] with ((1 - b) + b * dl / avgdl) */
    private final float[] cache;

    /** weight (idf * boost) */
    private final float weight;

    LdpScorer(float boost, float b, float d, Explanation idf, float avgdl, float[] cache) {
      this.boost = boost;
      this.idf = idf;
      this.avgdl = avgdl;
      this.b = b;
      this.d = d;
      this.cache = cache;
      this.weight = boost * idf.getValue().floatValue();
    }

    @Override
    public float score(float freq, long encodedNorm) {
      double norm = cache[((byte) encodedNorm) & 0xFF];
      return weight * (float) (1 + Math.log(1 + Math.log(freq / norm + d)));
      // return weight * (float) (1 + Math.log10(freq / norm + d/10)); // fast approximation?
    }

    @Override
    public Explanation explain(Explanation freq, long encodedNorm) {
      List<Explanation> subs = new ArrayList<>(explainConstantFactors());
      Explanation tfExpl = explainTF(freq, encodedNorm);
      subs.add(tfExpl);
      return Explanation.match(weight * tfExpl.getValue().floatValue(),
          "score(freq="+freq.getValue()+"), product of:", subs);
    }
    
    private Explanation explainTF(Explanation freq, long norm) {
      List<Explanation> subs = new ArrayList<>();
      subs.add(freq);
      float doclen = LENGTH_TABLE[((byte) norm) & 0xff];
      subs.add(Explanation.match(b, "b, length normalization parameter"));
      if ((norm & 0xFF) > 39) {
        subs.add(Explanation.match(doclen, "dl, length of field (approximate)"));
      } else {
        subs.add(Explanation.match(doclen, "dl, length of field"));
      }
      subs.add(Explanation.match(avgdl, "avgdl, average length of field"));
      subs.add(Explanation.match(d, "parameter d"));
      float normValue = ((1 - b) + b * doclen / avgdl);
      return Explanation.match(
          (float) (1 + Math.log(1 + Math.log((freq.getValue().floatValue() / (double) normValue) + d))),
          "tf, computed as 1 + log(1 + log(freq / (1 - b + b * dl / avgdl) + d)) from:", subs);
    }

    private List<Explanation> explainConstantFactors() {
      List<Explanation> subs = new ArrayList<>();
      // query boost
      if (boost != 1.0f) {
        subs.add(Explanation.match(boost, "boost"));
      }
      // idf
      subs.add(idf);
      return subs;
    }
  }

  @Override
  public String toString() {
    return "LdpSimilarity(b=" + b + ",d=" + d + ")";
  }

  /**
   * Returns the <code>b</code> parameter
   *
   * @see #LdpSimilarity(float, float, boolean)
   */
  public final float getB() {
    return b;
  }

  /** 
   * Returns the <code>d</code> parameter
   *
   * @see #LdpSimilarity(float, float, boolean)
   */
  public final float getD() {
    return d;
  }
}
