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
import org.novasearch.lucene.search.similarities.LdpSimilarity;

/**
 * Factory for {@link LdpSimilarity}
 * <p>
 * Parameters:
 * <ul>
 *   <li>b (float): Controls to what degree document length normalizes tf values.
 *                  The default is <code>0.75</code>
 *   <li>d (float): Controls lower-bound of term frequency normalization in Ldp.
 *                  The default is <code>0.5</code>
 * </ul>
 * <p>
 */
public class LdpSimilarityFactory extends SimilarityFactory {
  private LdpSimilarity similarity;

  @Override
  public void init(SolrParams params) {
    super.init(params);
    boolean discountOverlaps = params.getBool("discountOverlaps", true);
    float b = params.getFloat("b", 0.75f);
    float d = params.getFloat("d", 0.5f);
    similarity = new LdpSimilarity(b, d, discountOverlaps);
  }

  @Override
  public Similarity getSimilarity() {
    return similarity;
  }
}
