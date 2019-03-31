/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.novasearch.elasticsearch.plugin;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.Version;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.IndexModule;
import org.elasticsearch.plugins.Plugin;
import org.novasearch.lucene.search.similarities.BM25Similarity;
import org.novasearch.lucene.search.similarities.LdpSimilarity;
import org.novasearch.lucene.search.similarities.LtcSimilarity;
import org.novasearch.lucene.search.similarities.RobertsonSimilarity;

public class RelevancePlugin extends Plugin {
  private static final Logger logger = LogManager.getLogger(RelevancePlugin.class);
  
  static final String DISCOUNT_OVERLAPS = "discount_overlaps";

  public String name() {
    return "elasticsearch-relevance-plugin";
  }

  public String description() {
    return "Elasticsearch relevance plugin.";
  }

  public void onIndexModule(IndexModule indexModule) {
    indexModule.addSimilarity("bm25_", (settings, version, scriptService) -> createBM25Similarity(settings, version));
    indexModule.addSimilarity("ltc", (settings, version, scriptService) -> createLtcSimilarity(settings, version));
    indexModule.addSimilarity("ldp", (settings, version, scriptService) -> createLdpSimilarity(settings, version));
    indexModule.addSimilarity("robertson", (settings, version, scriptService) -> createRobertsonSimilarity(settings, version));
  }

  public static BM25Similarity createBM25Similarity(Settings settings, Version indexCreatedVersion) {
    float k1 = settings.getAsFloat("k1", 1.2f);
    float b = settings.getAsFloat("b", 0.75f);
    float d = settings.getAsFloat("d", 0.5f);
    String modelName = settings.get("model");
    BM25Similarity.BM25Model model = modelName != null ? BM25Similarity.BM25Model.valueOf(modelName) : BM25Similarity.BM25Model.CLASSIC;
    boolean discountOverlaps = settings.getAsBoolean(DISCOUNT_OVERLAPS, true);

    BM25Similarity sim;
    if (model != null) {
      if (d == 0) {
        d = (model == BM25Similarity.BM25Model.L) ? 0.5f : 1.0f;
      }
      sim = new BM25Similarity(k1, b, d, model);
    } else {
      sim = new BM25Similarity(k1, b);
    }
    sim.setDiscountOverlaps(discountOverlaps);
    logger.info(sim.toString());
    return sim;
  }

  public static LtcSimilarity createLtcSimilarity(Settings settings, Version indexCreatedVersion) {
    boolean discountOverlaps = settings.getAsBoolean(DISCOUNT_OVERLAPS, true);

    LtcSimilarity similarity = new LtcSimilarity();
    similarity.setDiscountOverlaps(discountOverlaps);
    logger.info(similarity.toString());
    return similarity;
  }

  public static LdpSimilarity createLdpSimilarity(Settings settings, Version indexCreatedVersion) {
    float b = settings.getAsFloat("b", 0.75f);
    float d = settings.getAsFloat("d", 0.5f);
    boolean discountOverlaps = settings.getAsBoolean(DISCOUNT_OVERLAPS, true);

    LdpSimilarity sim = new LdpSimilarity(b, d);
    sim.setDiscountOverlaps(discountOverlaps);
    logger.info(sim.toString());
    return sim;
  }

  public static RobertsonSimilarity createRobertsonSimilarity(Settings settings, Version indexCreatedVersion) {
    float k1 = settings.getAsFloat("k1", 1.2f);
    float b = settings.getAsFloat("b", 0.75f);
    boolean discountOverlaps = settings.getAsBoolean(DISCOUNT_OVERLAPS, true);

    RobertsonSimilarity sim = new RobertsonSimilarity(k1, b);
    sim.setDiscountOverlaps(discountOverlaps);
    logger.info(sim.toString());
    return sim;
  }
}