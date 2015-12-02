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

package io.deepsense.workflowexecutor.partialexecution

import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar

import io.deepsense.commons.StandardSpec
import io.deepsense.commons.exception.FailureDescription
import io.deepsense.commons.models.Entity
import io.deepsense.deeplang.DOperable
import io.deepsense.deeplang.doperables.dataframe.DataFrame
import io.deepsense.deeplang.inference.InferContext
import io.deepsense.graph._
import io.deepsense.graph.nodestate.{NodeStatus, Queued}
import io.deepsense.models.workflows.{EntitiesMap, NodeState, NodeStateWithResults}
import io.deepsense.reportlib.model.ReportContent

class ExecutionSpec
  extends StandardSpec
  with MockitoSugar
  with GraphTestSupport {

  val directedGraph = DirectedGraph(nodeSet, edgeSet)
  val statefulGraph = StatefulGraph(
    directedGraph,
    directedGraph.nodes.map(_.id -> NodeStateWithResults.draft).toMap,
    None)
  val allNodesIds = directedGraph.nodes.map(_.id)

  "Execution" should {
    "have all nodes Draft" when {
      "empty" in {
        val execution = Execution.empty
        execution.states.values.toSet should have size 0
      }
      "created with selection" in {
        val execution = Execution(statefulGraph, Set(idA, idB))
        execution.states should have size execution.graph.size
        execution.selectedNodes.foreach {
          n => execution.states(n) shouldBe NodeStateWithResults.draft
        }
      }
    }
    "infer knowledge only on the selected part" in {
      val graph = mock[StatefulGraph]
      when(graph.directedGraph).thenReturn(DirectedGraph())
      val subgraph = mock[StatefulGraph]
      when(graph.subgraph(any())).thenReturn(subgraph)
      when(subgraph.enqueueDraft).thenReturn(subgraph)

      val nodes = Set[Node.Id]()
      val execution = IdleExecution(graph, nodes)

      val inferenceResult = mock[StatefulGraph]
      when(subgraph.inferAndApplyKnowledge(any())).thenReturn(inferenceResult)
      when(graph.updateStates(any())).thenReturn(inferenceResult)

      val inferContext = mock[InferContext]
      val inferred = execution.inferAndApplyKnowledge(inferContext)
      verify(subgraph).inferAndApplyKnowledge(inferContext)

      inferred shouldBe IdleExecution(inferenceResult, nodes)
    }
    "mark nodes as Draft when a predecessor changed " +
      "even if the nodes were excluded from execution" in {
        val statefulGraph = StatefulGraph(
          directedGraph,
          Map(
            idA -> nodeCompletedState,
            idB -> nodeState(nodeFailed),
            idC -> nodeCompletedState,
            idD -> nodeCompletedState,
            idE -> nodeState(nodestate.Aborted)
          ),
          None
        )

        val execution = IdleExecution(
          statefulGraph,
          statefulGraph.nodes.map(_.id))

        val updated =
          execution.updateStructure(statefulGraph.directedGraph, Set(idC))

        updated.states(idA) shouldBe execution.states(idA)
        updated.states(idB) shouldBe NodeStateWithResults.draft
        updated.states(idC) shouldBe NodeStateWithResults.draft
        updated.states(idD) shouldBe NodeStateWithResults.draft
        updated.states(idE) shouldBe NodeStateWithResults.draft
    }
    "enqueue all nodes" when {
      "all nodes where specified" in {
        val allSelected = Execution(statefulGraph, allNodesIds)

        val draftGraph = StatefulGraph(
          directedGraph,
          directedGraph.nodes.map(n => n.id -> NodeStateWithResults.draft).toMap,
          None
        )

        val queuedGraph = StatefulGraph(
          directedGraph,
          directedGraph.nodes.map(n => n.id -> nodeState(Queued)).toMap,
          None
        )

        val enqueued = allSelected.enqueue

        enqueued shouldBe
          RunningExecution(
            draftGraph,
            queuedGraph,
            allNodesIds.toSet)

        enqueued.states.forall { case (_, state) => state.isQueued } shouldBe true
      }
    }
    "mark all selected nodes as Draft" in {
      val statefulGraph = StatefulGraph(
        DirectedGraph(nodeSet, edgeSet),
        Map(
          idA -> nodeCompletedState,
          idB -> nodeCompletedState,
          idC -> nodeCompletedState,
          idD -> nodeCompletedState,
          idE -> nodeCompletedState
        ),
        None
      )

      val execution = IdleExecution(
        statefulGraph,
        statefulGraph.nodes.map(_.id))

      val updated =
        execution.updateStructure(statefulGraph.directedGraph, Set(idC, idE))

      updated.states(idA) shouldBe execution.states(idA)
      updated.states(idB) shouldBe execution.states(idB)
      updated.states(idC) shouldBe NodeStateWithResults.draft
      updated.states(idD) shouldBe NodeStateWithResults.draft
      updated.states(idE) shouldBe NodeStateWithResults.draft
    }
    "not execute operations that are already completed (if they are not selected)" +
      "finish execution if the selected subgraph finished" in {
      val stateWC = nodeCompletedIdState(idC)
      val stateWD = nodeCompletedIdState(idD)
      val stateWE = nodeCompletedIdState(idE)
      val statefulGraph = StatefulGraph(
        DirectedGraph(nodeSet, edgeSet),
        Map(
          idA -> nodeCompletedIdState(idA),
          idB -> nodeCompletedIdState(idB),
          idC -> stateWC,
          idD -> stateWD,
          idE -> stateWE
        ),
        None
      )

      val execution = IdleExecution(
        statefulGraph,
        statefulGraph.nodes.map(_.id))

      val enqueued = execution
          .updateStructure(statefulGraph.directedGraph, Set(idC, idE))
          .enqueue

      enqueued.states(idA) shouldBe execution.states(idA)
      enqueued.states(idB) shouldBe execution.states(idB)
      enqueued.states(idC) shouldBe stateWC.draft.enqueue
      enqueued.states(idD) shouldBe stateWD.draft
      enqueued.states(idE) shouldBe stateWE.draft.enqueue

      enqueued.readyNodes.map(rn => rn.node) should contain theSameElementsAs List(nodeC, nodeE)
      val cStarted = enqueued.nodeStarted(idC)
      cStarted.readyNodes.map(rn => rn.node) should contain theSameElementsAs List(nodeE)
      val eStarted = cStarted.nodeStarted(idE)
      eStarted.readyNodes.map(rn => rn.node) shouldBe 'empty

      val idCResults = results(idC)
      val idEResults = results(idE)
      def reports(ids: Seq[Entity.Id]): Map[Entity.Id, ReportContent] =
        ids.map(_ -> ReportContent("{}")).toMap
      def dOperables(ids: Seq[Entity.Id]): Map[Entity.Id, DOperable] =
        ids.map(_ -> mock[DOperable]).toMap
      val finished = eStarted
        .nodeFinished(idC, idCResults, reports(idCResults), dOperables(idCResults))
        .nodeFinished(idE, idEResults, reports(idEResults), dOperables(idEResults))

      finished shouldBe an[IdleExecution]
    }
    "expose inference errors" in {
      val failedGraph = mock[StatefulGraph]
      val failureDescription = Some(mock[FailureDescription])
      when(failedGraph.executionFailure).thenReturn(failureDescription)

      val graph = mock[StatefulGraph]
      val directedGraph = mock[DirectedGraph]
      when(directedGraph.nodes).thenReturn(Set[Node]())
      when(graph.directedGraph).thenReturn(directedGraph)
      when(graph.subgraph(any())).thenReturn(graph)
      when(graph.inferAndApplyKnowledge(any())).thenReturn(failedGraph)
      when(graph.updateStates(any())).thenReturn(failedGraph)
      when(graph.executionFailure).thenReturn(None)

      val execution = IdleExecution(graph, Set())
      execution.inferAndApplyKnowledge(mock[InferContext]).error shouldBe failureDescription
    }
    "reset successors state when predecessor is replaced" in {
      val changedC = Node(Node.Id.randomId, op1To1) // Notice: different Id
      checkSuccessorsStatesAfterANodeChange(changedC)
    }
    "reset successors state when predecessor's parameters are modified" in pendingUntilFixed {
      val changedC = Node(idC, op1To1) // Notice: the same id; different parameters!
      checkSuccessorsStatesAfterANodeChange(changedC)
    }
    "be idle" when {
      "stated with an empty structure and enqueued" in {
        val statefulGraph = StatefulGraph(
          DirectedGraph(nodeSet, edgeSet),
          Map(
            idA -> nodeCompletedIdState(idA),
            idB -> nodeCompletedIdState(idB),
            idC -> nodeCompletedIdState(idC),
            idD -> nodeCompletedIdState(idD),
            idE -> nodeCompletedIdState(idE)
          ),
          None
        )

        val execution = IdleExecution(
          statefulGraph,
          statefulGraph.nodes.map(_.id))

        val emptyStructure = execution.updateStructure(DirectedGraph(), Set())
        val enqueued = emptyStructure.inferAndApplyKnowledge(mock[InferContext])
          .enqueue

        enqueued shouldBe an[IdleExecution]
      }
      "was empty and enqueud" in {
        Execution.empty.enqueue shouldBe an[IdleExecution]
      }
    }
  }

  def checkSuccessorsStatesAfterANodeChange(changedC: Node): Unit = {
    val graph = DirectedGraph(nodeSet, edgeSet)

    val edgeBtoC = Edge(nodeB, 0, changedC, 0)
    val edgeCtoD = Edge(changedC, 0, nodeD, 0)
    val updatedGraph = DirectedGraph(
      Set(nodeA, nodeB, changedC, nodeD, nodeE),
      Set(edge1, edgeBtoC, edgeCtoD, edge4, edge5)
    )

    val stateWD = nodeCompletedIdState(idD)
    val stateWE = nodeCompletedIdState(idE)
    val statefulGraph = StatefulGraph(
      graph,
      Map(
        idA -> nodeCompletedIdState(idA),
        idB -> nodeCompletedIdState(idB),
        idC -> nodeCompletedIdState(idC),
        idD -> stateWD,
        idE -> stateWE
      ),
      None)

    val execution = IdleExecution(
      statefulGraph,
      statefulGraph.nodes.map(_.id))

    val updatedExecution = execution.updateStructure(updatedGraph, Set(idE))
    updatedExecution.states(idA) shouldBe statefulGraph.states(idA)
    updatedExecution.states(idB) shouldBe statefulGraph.states(idB)
    updatedExecution.states(changedC.id) shouldBe NodeStateWithResults.draft
    updatedExecution.states(idD) shouldBe stateWD.draft
    updatedExecution.states(idE) shouldBe stateWE.draft

    val queuedExecution = updatedExecution.enqueue
    queuedExecution.states(idA) shouldBe statefulGraph.states(idA)
    queuedExecution.states(idB) shouldBe statefulGraph.states(idB)
    queuedExecution.states(changedC.id) shouldBe NodeStateWithResults.draft
    queuedExecution.states(idD) shouldBe stateWD.draft
    queuedExecution.states(idE) shouldBe stateWE.draft.enqueue
  }

  private def nodeCompletedState: NodeStateWithResults = {
    nodeState(nodeCompleted)
  }

  private def nodeCompletedIdState(entityId: Entity.Id): NodeStateWithResults = {
    val dOperables: Map[Entity.Id, DataFrame] = Map(entityId -> mock[DataFrame])
    val reports: Map[Entity.Id, ReportContent] = Map(entityId -> ReportContent("whatever"))
    NodeStateWithResults(
      NodeState(
        nodeCompleted.copy(results = Seq(entityId)),
        Some(EntitiesMap(dOperables, reports))),
      dOperables)
  }

  private def nodeState(status: NodeStatus): NodeStateWithResults = {
    NodeStateWithResults(NodeState(status, Some(EntitiesMap())), Map())
  }
}
