---
layout: global
displayTitle: Remove Stop Words
title: Remove Stop Words
description: Remove Stop Words
usesMathJax: true
includeOperationsMenu: true
---
Filters out default English stop words from the input.
Null values from the input array are preserved.

This operation is ported from Spark ML.


For a comprehensive introduction, see
<a target="_blank" href="https://spark.apache.org/docs/1.6.1/ml-features.html#stopwordsremover">Spark documentation</a>.


For scala docs details, see
<a target="_blank" href="https://spark.apache.org/docs/1.6.1/api/scala/index.html#org.apache.spark.ml.feature.StopWordsRemover">org.apache.spark.ml.feature.StopWordsRemover documentation</a>.

**Since**: Seahorse 1.0.0

## Input


<table>
<thead>
<tr>
<th style="width:15%">Port</th>
<th style="width:15%">Type Qualifier</th>
<th style="width:70%">Description</th>
</tr>
</thead>
<tbody>
    <tr><td><code>0</code></td><td><code><a href="../classes/dataframe.html">DataFrame</a></code></td><td>The input <code>DataFrame</code>.</td></tr>
</tbody>
</table>


## Output


<table>
<thead>
<tr>
<th style="width:15%">Port</th>
<th style="width:15%">Type Qualifier</th>
<th style="width:70%">Description</th>
</tr>
</thead>
<tbody>
    <tr><td><code>0</code></td><td><code><a href="../classes/dataframe.html">DataFrame</a></code></td><td>The output <code>DataFrame</code>.</td></tr><tr><td><code>1</code></td><td><code><a href="../classes/transformer.html">Transformer</a></code></td><td>A <code>Transformer</code> that allows to apply the operation on other <code>DataFrames</code> using a <a href="transform.html">Transform</a>.</td></tr>
</tbody>
</table>


## Parameters


<table class="table">
<thead>
<tr>
<th style="width:15%">Name</th>
<th style="width:15%">Type</th>
<th style="width:70%">Description</th>
</tr>
</thead>
<tbody>

<tr>
<td><code>case sensitive</code></td>
<td><code><a href="../parameter_types.html#boolean">Boolean</a></code></td>
<td>Whether to do a case sensitive comparison over the stop words.</td>
</tr>

<tr>
<td><code>operate on</code></td>
<td><code><a href="../parameter_types.html#input-output-column-selector">InputOutputColumnSelector</a></code></td>
<td>The input and output columns for the operation.</td>
</tr>

</tbody>
</table>


{% markdown operations/examples/RemoveStopWords.md %}
