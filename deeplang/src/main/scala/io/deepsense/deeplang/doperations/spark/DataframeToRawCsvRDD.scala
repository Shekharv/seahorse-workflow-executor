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

package io.deepsense.deeplang.doperations.spark

import org.apache.commons.csv.QuoteMode
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.DataFrame

import io.deepsense.deeplang.ExecutionContext

/*
  This is copypasta from com.databricks.spark.csv.CsvSchemaRDD
  Spark's code has perfect method converting Dataframe -> raw csv RDD[String]
  But in last lines of that method it's hardcoded against writing as text file -
  for our case we need RDD.
  Another difference is using map intead of mapPartitionsWithIndex -> there is
  custom header handling code in here.
 */
object DataframeToRawCsvRDD {

  val defaultCsvFormat = com.databricks.spark.csv.defaultCsvFormat

  def apply(dataFrame: DataFrame, parameters: Map[String, String] = Map())
           (implicit ctx: ExecutionContext): RDD[String] = {
    val delimiter = parameters.getOrElse("delimiter", ",")
    val delimiterChar = if (delimiter.length == 1) {
      delimiter.charAt(0)
    } else {
      throw new Exception("Delimiter cannot be more than one character.")
    }

    val escape = parameters.getOrElse("escape", null)
    val escapeChar: Character = if (escape == null) {
      null
    } else if (escape.length == 1) {
      escape.charAt(0)
    } else {
      throw new Exception("Escape character cannot be more than one character.")
    }

    val quote = parameters.getOrElse("quote", "\"")
    val quoteChar: Character = if (quote == null) {
      null
    } else if (quote.length == 1) {
      quote.charAt(0)
    } else {
      throw new Exception("Quotation cannot be more than one character.")
    }

    val quoteModeString = parameters.getOrElse("quoteMode", "MINIMAL")
    val quoteMode: QuoteMode = if (quoteModeString == null) {
      null
    } else {
      QuoteMode.valueOf(quoteModeString.toUpperCase)
    }

    val nullValue = parameters.getOrElse("nullValue", "null")

    val csvFormat = defaultCsvFormat
      .withDelimiter(delimiterChar)
      .withQuote(quoteChar)
      .withEscape(escapeChar)
      .withQuoteMode(quoteMode)
      .withSkipHeaderRecord(false)
      .withNullString(nullValue)

    val generateHeader = parameters.getOrElse("header", "false").toBoolean
    val headerRdd = if (generateHeader) {
      ctx.sparkContext.parallelize(Seq(
        csvFormat.format(dataFrame.columns.map(_.asInstanceOf[AnyRef]): _*)
      ))
    } else {
      ctx.sparkContext.emptyRDD[String]
    }

    val rowsRdd = dataFrame.rdd.map(row => {
      csvFormat.format(row.toSeq.map(_.asInstanceOf[AnyRef]): _*)
    })

    headerRdd union rowsRdd
  }

}
