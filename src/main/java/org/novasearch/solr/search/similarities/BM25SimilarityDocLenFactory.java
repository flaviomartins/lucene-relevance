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
package org.novasearch.solr.search.similarities;

import org.apache.lucene.search.similarities.Similarity;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.schema.SimilarityFactory;
import org.novasearch.lucene.search.similarities.BM25Similarity;
import org.novasearch.lucene.search.similarities.BM25Similarity.BM25Model;

/**
 * Factory for {@link BM25Similarity}
 * <p>
 * Parameters:
 * <ul>
 *   <li>k1 (float): Controls non-linear term frequency normalization (saturation).
 *                   The default is <code>1.2</code>
 *   <li>b (float): Controls to what degree document length normalizes tf values.
 *                  The default is <code>0.75</code>
 *   <li>d (float): Controls document length normalization of tf values in BM25L.
 *                  Controls lower-bound of term frequency normalization in BM25PLUS.
 *                  The default is <code>0.5</code> for BM25L, <code>1.0</code> for BM25PLUS
 *   <li>model (string): BM25 model.
  *        <ul>
 *           <li>CLASSIC: Classic BM25 model
 *           <li>BM25L: Does not overly-penalize very long documents.
 *         </ul>
 * </ul>
 * <p>
 * Optional settings:
 * <ul>
 *   <li>discountOverlaps (bool): Sets
 *       {@link BM25Similarity#setDiscountOverlaps(boolean)}</li>
 * </ul>
 */
public class BM25SimilarityDocLenFactory extends SimilarityFactory {
  private boolean discountOverlaps;
  private float k1;
  private float b;
  private float d;
  private BM25Model model;

  @Override
  public void init(SolrParams params) {
    super.init(params);
    discountOverlaps = params.getBool("discountOverlaps", true);
    k1 = params.getFloat("k1", 1.2f);
    b = params.getFloat("b", 0.75f);
    d = params.getFloat("d", 0);
    String m = params.get("model");
    model = BM25Model.valueOf(m);
  }

  @Override
  public Similarity getSimilarity() {
    BM25Similarity sim;
    if (model != null) {
      if (d == 0) {
        d = (model == BM25Model.L) ? 0.5f : 1.0f;
      }
      sim = new BM25Similarity(k1, b, d, model);
    } else {
      sim = new BM25Similarity(k1, b);
    }
    sim.setDiscountOverlaps(discountOverlaps);
    return sim;
  }
}
