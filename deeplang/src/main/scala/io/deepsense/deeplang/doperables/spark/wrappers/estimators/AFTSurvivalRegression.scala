/**
 * Copyright 2016, deepsense.io
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

package io.deepsense.deeplang.doperables.spark.wrappers.estimators

import scala.language.reflectiveCalls

import org.apache.spark.ml
import org.apache.spark.ml.regression.{AFTSurvivalRegression => SparkAFTSurvivalRegression, AFTSurvivalRegressionModel => SparkAFTSurvivalRegressionModel}

import io.deepsense.deeplang.doperables.SparkEstimatorWrapper
import io.deepsense.deeplang.doperables.spark.wrappers.models.AFTSurvivalRegressionModel
import io.deepsense.deeplang.doperables.spark.wrappers.params.AFTSurvivalRegressionParams
import io.deepsense.deeplang.doperables.spark.wrappers.params.common.{HasFitIntercept, HasLabelColumnParam, HasMaxIterationsParam, HasTolerance}
import io.deepsense.deeplang.params.Param
import io.deepsense.deeplang.params.selections.NameSingleColumnSelection
import io.deepsense.deeplang.params.wrappers.spark.SingleColumnSelectorParamWrapper

class AFTSurvivalRegression
  extends SparkEstimatorWrapper[
    SparkAFTSurvivalRegressionModel,
    SparkAFTSurvivalRegression,
    AFTSurvivalRegressionModel]
  with AFTSurvivalRegressionParams
  with HasLabelColumnParam
  with HasMaxIterationsParam
  with HasTolerance
  with HasFitIntercept {

  val censorColumn =
    new SingleColumnSelectorParamWrapper[
      ml.param.Params { val censorCol: ml.param.Param[String] }](
      name = "censor column",
      description = """Param for censor column name.
                      |The value of this column could be 0 or 1.
                      |If the value is 1, it means the event has occurred i.e. uncensored;
                      |otherwise censored.""".stripMargin,
      sparkParamGetter = _.censorCol,
      portIndex = 0)
  setDefault(censorColumn, NameSingleColumnSelection("censor"))

  override val params: Array[Param[_]] = declareParams(
    fitIntercept,
    maxIterations,
    tolerance,
    labelColumn,
    censorColumn,
    featuresColumn,
    predictionColumn,
    quantileProbabilities,
    optionalQuantilesColumn)
}
