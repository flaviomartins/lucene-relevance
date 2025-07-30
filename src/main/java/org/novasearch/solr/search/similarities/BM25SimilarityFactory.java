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
 *
 * <p>Parameters:
 *
 * <ul>
 *   <li>k1 (float): Controls non-linear term frequency normalization (saturation). The default is
 *       <code>1.2</code>
 *   <li>b (float): Controls to what degree document length normalizes tf values. The default is
 *       <code>0.75</code>
 *   <li>d (float): Controls document length normalization of tf values in BM25L.
 *                  Controls lower-bound of term frequency normalization in BM25PLUS.
 *                  The default is
 *       <code>0.5</code> for BM25L, <code>1.0</code> for BM25PLUS
 *   <li>model (string): BM25 model.
  *        <ul>
 *           <li>CLASSIC: Classic BM25 model
 *           <li>BM25L: Does not overly-penalize very long documents.
 *         </ul>
 *   <li>discountOverlaps (bool): True if overlap tokens (tokens with a position of increment of
 *       zero) are discounted from the document's length. The default is <code>true</code>
 * </ul>
 *
 * @lucene.experimental
 * @since 8.0.0
 */
public class BM25SimilarityFactory extends SimilarityFactory {
  private BM25Similarity similarity;

  @Override
  public void init(SolrParams params) {
    super.init(params);
    boolean discountOverlaps = params.getBool("discountOverlaps", true);
    float k1 = params.getFloat("k1", 1.2f);
    float b = params.getFloat("b", 0.75f);
    float d = params.getFloat("d", -1.0f);
    String m = params.get("model", "CLASSIC");
    BM25Model model = BM25Model.valueOf(m);
    if (d < 0) {
      if (model == BM25Model.L) {
        d = 0.5f;
      } else if (model == BM25Model.PLUS) {
        d = 1.0f;
      } else {
        d = 0;
      }
    }
    similarity = new BM25Similarity(k1, b, d, model, discountOverlaps);
  }

  @Override
  public Similarity getSimilarity() {
    return similarity;
  }
}
