/**
 * Copyright 2015, deepsense.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.deepsense.deeplang.doperations.spark.wrappers.estimators

import io.deepsense.deeplang.DOperation.Id
import io.deepsense.deeplang.doperables.spark.wrappers.estimators.VectorIndexerEstimator
import io.deepsense.deeplang.doperations.EstimatorAsOperation

class VectorIndexer extends EstimatorAsOperation[VectorIndexerEstimator] {

  override val id: Id = "d62abcbf-1540-4d58-8396-a92b017f2ef0"
  override val name: String = "Vector Indexer"
  override val description: String = "Indexes categorical feature columns in a dataset of Vector"
}