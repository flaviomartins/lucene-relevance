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
 * BM25 Similarity. Introduced in Stephen E. Robertson, Steve Walker, Susan Jones, Micheline
 * Hancock-Beaulieu, and Mike Gatford. Okapi at TREC-3. In Proceedings of the Third <b>T</b>ext
 * <b>RE</b>trieval <b>C</b>onference (TREC 1994). Gaithersburg, USA, November 1994.
 * <p/>
 * BM25L version. Introduced in
 * Yuanhua Lv, ChengXiang Zhai. "When Documents Are Very Long, BM25 Fails!".
 * In Proceedings of The 34th International ACM SIGIR conference on research
 * and development in Information Retrieval (SIGIR'11).
 * <p/>
 * BM25+ version. Introduced in
 * Yuanhua Lv, ChengXiang Zhai. "Lower-Bounding Term Frequency Normalization".
 * In Proceedings of the 20th ACM International Conference on Information and
 * Knowledge Management  (CIKM'11).
 */
public class BM25Similarity extends Similarity {
  public enum BM25Model {
    CLASSIC, L, PLUS
  }

  private final float k1;
  private final float b;
  private final float d;
  private final BM25Model model;

  /**
   * BM25 with the supplied parameter values.
   *
   * @param k1 Controls non-linear term frequency normalization (saturation).
   * @param b Controls to what degree document length normalizes tf values.
   * @param d Controls document length normalization of tf values in BM25L.
   * @param discountOverlaps True if overlap tokens (tokens with a position of increment of zero)
   *     are discounted from the document's length.
   * @throws IllegalArgumentException if {@code k1} is infinite or negative, or if {@code b} is not
   *     within the range {@code [0..1]}
   */
  public BM25Similarity(float k1, float b, float d, BM25Model model, boolean discountOverlaps) {
    super(discountOverlaps);
    if (Float.isFinite(k1) == false || k1 < 0) {
      throw new IllegalArgumentException("illegal k1 value: " + k1 + ", must be a non-negative finite value");
    }
    if (Float.isNaN(b) || b < 0 || b > 1) {
      throw new IllegalArgumentException("illegal b value: " + b + ", must be between 0 and 1");
    }
    if (Float.isNaN(d) || d < 0 || d > 1.5) {
      throw new IllegalArgumentException("illegal d value: " + d + ", must be between 0 and 1.5");
    }
    this.k1 = k1;
    this.b  = b;
    this.d  = d;
    this.model = model;
  }

  /**
   * BM25 with the supplied parameter values.
   *
   * @param k1 Controls non-linear term frequency normalization (saturation).
   * @param b Controls to what degree document length normalizes tf values.
   * @throws IllegalArgumentException if {@code k1} is infinite or negative, or if {@code b} is not
   *     within the range {@code [0..1]}
   */
  public BM25Similarity(float k1, float b, boolean discountOverlaps) {
    super(discountOverlaps);
    if (Float.isFinite(k1) == false || k1 < 0) {
      throw new IllegalArgumentException("illegal k1 value: " + k1 + ", must be a non-negative finite value");
    }
    if (Float.isNaN(b) || b < 0 || b > 1) {
      throw new IllegalArgumentException("illegal b value: " + b + ", must be between 0 and 1");
    }
    this.k1 = k1;
    this.b  = b;
    this.d  = 0;
    this.model = BM25Model.CLASSIC;
  }

  /**
   * BM25 with these default values:
   *
   * <ul>
   *   <li>{@code k1 = 1.2}</li>
   *   <li>{@code b = 0.75}</li>
   *   <li>{@code d = 0.5} for BM25L, {@code d = 1.0} for BM25PLUS</li>
   * </ul>
   *
   * and the supplied parameter value:
   *
   * @param discountOverlaps True if overlap tokens (tokens with a position of increment of zero)
   *     are discounted from the document's length.
   */
  public BM25Similarity(BM25Model model, boolean discountOverlaps) {
    super(discountOverlaps);
    this.k1 = 1.2f;
    this.b  = 0.75f;
    if (model == BM25Model.L) {
      this.d = 0.5f;
    } else if (model == BM25Model.PLUS) {
      this.d = 1.0f;
    } else {
      this.d  = 0;
    }
    this.model = model;
  }

  /** Primary constructor. */
  public BM25Similarity(boolean discountOverlaps) {
      this(BM25Model.CLASSIC, true);
  }

  /** Default constructor: parameter-free */
  public BM25Similarity() {
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
      cache[i] = k1 * ((1 - b) + b * LENGTH_TABLE[i] / avgdl);
    }
    return new BM25Scorer(boost, k1, b, d, model, idf, avgdl, cache);
  }

  /** Collection statistics for the BM25 model. */
  private static class BM25Scorer extends SimScorer {
    /** query boost */
    private final float boost;

    /** k1 value for scale factor */
    private final float k1;

    /** b value for length normalization impact */
    private final float b;

    /** d parameter */
    private final float d;

    /** BM25's model */
    private final BM25Model model;

    /** BM25's idf */
    private final Explanation idf;

    /** The average document length. */
    private final float avgdl;

    /** precomputed norm[256] with k1 * ((1 - b) + b * dl / avgdl) */
    private final float[] cache;

    /** weight (idf * boost) */
    private final float weight;

    /** precomputed d / k1. */
    private final float multInvK1_d;

    BM25Scorer(float boost, float k1, float b, float d, BM25Model model, Explanation idf, float avgdl, float[] cache) {
      this.boost = boost;
      this.idf = idf;
      this.avgdl = avgdl;
      this.k1 = k1;
      this.b = b;
      this.d = d;
      this.model = model;
      this.cache = cache;
      this.weight = (k1 + 1) * boost * idf.getValue().floatValue();
      this.multInvK1_d = d / k1;
    }

    @Override
    public float score(float freq, long encodedNorm) {
      double norm = cache[((byte) encodedNorm) & 0xFF];
      double multInvK1_d_norm = multInvK1_d * norm;
      if (model == BM25Model.PLUS) {
        return weight * (float) (freq / (freq + norm)) + (d * idf.getValue().floatValue());
      } else {
        return weight * (float) ((freq + multInvK1_d_norm) / (freq + norm + multInvK1_d_norm));
      }
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
      subs.add(Explanation.match(k1, "k1, term saturation parameter"));
      float doclen = LENGTH_TABLE[((byte) norm) & 0xff];
      subs.add(Explanation.match(b, "b, length normalization parameter"));
      if ((norm & 0xFF) > 39) {
        subs.add(Explanation.match(doclen, "dl, length of field (approximate)"));
      } else {
        subs.add(Explanation.match(doclen, "dl, length of field"));
      }
      subs.add(Explanation.match(avgdl, "avgdl, average length of field"));
      if (model == BM25Model.L) {
        subs.add(Explanation.match(d, "parameter d"));
        float normValue = d + freq.getValue().floatValue() / (1 - b + b * doclen / avgdl);
        return Explanation.match(
            (float) (freq.getValue().floatValue() / (freq.getValue().floatValue() + (double) normValue)),
            "tf, computed as freq / (freq + k1 * (1 - b + b * dl / avgdl)) from:", subs);
      } else if (model == BM25Model.PLUS) {
        subs.add(Explanation.match(d, "parameter d"));
        float normValue = freq.getValue().floatValue() / (1 - b + b * doclen / avgdl);
        return Explanation.match(
            (float) (freq.getValue().floatValue() / (freq.getValue().floatValue() + (double) normValue)),
            "tf, computed as freq / (freq + k1 * (1 - b + b * dl / avgdl)) from:", subs);
      } else {
        float normValue = k1 * ((1 - b) + b * doclen / avgdl);
        return Explanation.match(
            (float) (freq.getValue().floatValue() / (freq.getValue().floatValue() + (double) normValue)),
            "tf, computed as freq / (freq + k1 * (1 - b + b * dl / avgdl)) from:", subs);
      }
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
    if (model == BM25Model.CLASSIC) {
      return "BM25(k1=" + k1 + ",b=" + b + ")";
    } else {
      return "BM25" + model.toString() + "(k1=" + k1 + ",b=" + b + ",d=" + d + ")";
    }
  }

  /**
   * Returns the <code>k1</code> parameter
   *
   * @see #BM25Similarity(float, float, boolean)
   */
  public final float getK1() {
    return k1;
  }

  /**
   * Returns the <code>b</code> parameter
   *
   * @see #BM25Similarity(float, float, boolean)
   */
  public final float getB() {
    return b;
  }

  /**
   * Returns the <code>d</code> parameter
   *
   * @see #BM25Similarity(float, float, float, BM25Model, boolean)
   */
  public final float getD() {
    return d;
  }
}
