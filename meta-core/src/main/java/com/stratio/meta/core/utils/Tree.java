/*
 * Stratio Meta
 *
 * Copyright (c) 2014, Stratio, All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */

package com.stratio.meta.core.utils;

import com.datastax.driver.core.Session;
import com.stratio.deep.context.DeepSparkContext;
import com.stratio.meta.common.actor.ActorResultListener;
import com.stratio.meta.common.result.QueryResult;
import com.stratio.meta.common.result.Result;
import com.stratio.meta.core.engine.EngineConfig;
import com.stratio.meta.core.executor.CassandraExecutor;
import com.stratio.meta.core.executor.CommandExecutor;
import com.stratio.meta.core.executor.DeepExecutor;
import com.stratio.meta.core.executor.StreamExecutor;
import com.stratio.streaming.api.IStratioStreamingAPI;

import org.apache.log4j.Logger;
import org.apache.spark.SparkEnv;

import java.util.ArrayList;
import java.util.List;

/**
 * Class that implements a Tree data structure.
 */
public class Tree {


  /**
   * Class logger.
   */
  private static final Logger LOG = Logger.getLogger(Tree.class.getName());

  /**
   * Parent of the root of the current tree.
   */
  private Tree parent = null;

  /**
   * Data stored in the root node.
   */
  private MetaStep node = null;

  /**
   * List of children.
   */
  private List<Tree> children = null;

  /**
   * Variable that determines whether any child of the tree, or the node itself contains a
   * streaming query.
   */
  private boolean involvesStreaming = false;

  /**
   * Class constructor.
   */
  public Tree() {
    children = new ArrayList<>();
  }

  /**
   * Class constructor.
   * @param node The root node.
   */
  public Tree(MetaStep node) {
    this();
    this.node = node;
  }

  /**
   * Set the parent of the tree.
   * @param parent The parent.
   */
  public void setParent(Tree parent) {
    this.parent = parent;
  }

  /**
   * Set the root node of this tree.
   * @param node The root node.
   */
  public void setNode(MetaStep node) {
    this.node = node;
  }

  /**
   * Add a child to the tree.
   * @param child The child.
   * @return The added child.
   */
  public Tree addChild(Tree child){
    child.setParent(this);
    children.add(child);
    return child;
  }

  /**
   * Check whether the current tree has parent or not.
   * @return Whether the tree has a null parent.
   */
  public boolean isRoot() {
    return parent == null;
  }

  /**
   * Get the string representation of this tree starting from the top.
   * @return A String representation.
   */
  public String toStringDownTop(){
    StringBuilder sb = new StringBuilder();
    int deep = 0;
    for(Tree child: children){
      sb.append(child.printDownTop(deep+1)).append(System.lineSeparator());
    }
    if(node != null){
      sb.append(node.toString());
    }

    return sb.toString();
  }

  /**
   * Get the string representation of the node adding the current node and their children.
   * @param depth The current depth.
   * @return The String representation.
   */
  private String printDownTop(int depth){
    StringBuilder sb = new StringBuilder();
    sb.append(node.toString());
    for(Tree child: children){
      sb.append(child.printDownTop(depth+1)).append(System.getProperty("line.separator"));
    }
    for(int i=0; i<depth; i++){
      sb.append("\t");
    }

    return sb.toString();
  }

  /**
   * Execute the current node of the tree.
   * @param session The Cassandra session.
   * @param deepSparkContext The Deep context.
   * @param engineConfig The engine configuration.
   * @param resultsFromChildren The results from the children.
   * @return A {@link com.stratio.meta.common.result.Result}.
   */
  public Result executeMyself(String queryId,
                              Session session,
                              IStratioStreamingAPI stratioStreamingAPI,
                              DeepSparkContext deepSparkContext,
                              EngineConfig engineConfig,
                              List<Result> resultsFromChildren,
                              ActorResultListener callbackActor){
    Result result = null;
    if(node == null){
      return QueryResult.createSuccessQueryResult();
    }
    MetaStep myStep = node;
    MetaPath myPath = myStep.getPath();
    if(myPath == MetaPath.COMMAND){
      result = CommandExecutor.execute(queryId, myStep.getStmt(), session, stratioStreamingAPI);
    } else if(myPath == MetaPath.CASSANDRA){
      result = CassandraExecutor.execute(myStep, session);
    } else if(myPath == MetaPath.DEEP){
      result = DeepExecutor.execute(myStep.getStmt(), resultsFromChildren, isRoot(), session, deepSparkContext, engineConfig);
    } else if(myPath == MetaPath.STREAMING){
      result = StreamExecutor.execute(queryId, myStep.getStmt(), stratioStreamingAPI, deepSparkContext, engineConfig, callbackActor, isRoot());
    } else if(myPath == MetaPath.UNSUPPORTED){
      result = Result.createUnsupportedOperationErrorResult("Query not supported.");
    } else {
      result = Result.createUnsupportedOperationErrorResult("Query not supported yet.");
    }
    return result;
  }

  /**
   * Execute the elements of the tree starting from the bottom up.
   * @param session The Cassandra session.
   * @param deepSparkContext The Deep context.
   * @param engineConfig The engine configuration.
   * @return A {@link com.stratio.meta.common.result.Result}.
   */
  public Result executeTreeDownTop(
      String queryId,
      Session session, IStratioStreamingAPI stratioStreamingAPI,
      DeepSparkContext deepSparkContext, EngineConfig engineConfig,
      ActorResultListener callbackActor){
    // Get results from my children
    List<Result> resultsFromChildren = new ArrayList<>();
    for(Tree child: children){
      resultsFromChildren.add(child.executeTreeDownTop(queryId, session, stratioStreamingAPI, deepSparkContext, engineConfig, callbackActor));
    }
    // Execute myself and return final result
    return executeMyself(queryId, session, stratioStreamingAPI, deepSparkContext, engineConfig, resultsFromChildren, callbackActor);
  }

  public Result executeTreeDownTop(
      String queryId,
      Session session, IStratioStreamingAPI stratioStreamingAPI,
      DeepSparkContext deepSparkContext, EngineConfig engineConfig,
      ActorResultListener callbackActor, Result result){
    // Get results from my children
    List<Result> resultsFromChildren = new ArrayList<>();
    resultsFromChildren.add(result);
    for(Tree child: children){
      resultsFromChildren.add(child.executeTreeDownTop(queryId, session, stratioStreamingAPI, deepSparkContext, engineConfig, callbackActor));
    }
    // Execute myself and return final result
    return executeMyself(queryId, session, stratioStreamingAPI, deepSparkContext, engineConfig, resultsFromChildren, callbackActor);
  }

  /**
   * Determine if the tree has not node.
   * @return Whether the tree does not contain a node.
   */
  public boolean isEmpty(){
    return node == null;
  }

  /**
   * Get the node assigned to this vertex of the tree.
   * @return A {@link com.stratio.meta.core.utils.MetaStep}.
   */
  public MetaStep getNode(){
    return node;
  }

  /**
   * Get the list of childrens.
   * @return The list.
   */
  public  List<Tree> getChildren(){
    return children;
  }

  /**
   * Set whether any child of the tree, or the node itself involves Streaming.
   * @param involvesStreaming The boolean value.
   */
  public void setInvolvesStreaming(boolean involvesStreaming) {
    this.involvesStreaming = involvesStreaming;
  }

  /**
   * Whether any child of the tree, or the node itself involves Streaming.
   * @return The boolean value.
   */
  public boolean involvesStreaming() {
    return involvesStreaming;
  }

  public Result executeTreeTopDown(String queryId, List<Result> resultsFromParents, Session session,
                                   DeepSparkContext deepSparkContext, EngineConfig engineConfig) {

    if(children.size() == 0){
      //No more children, execute final node.
      return executeMyself(queryId, session, null, deepSparkContext,
                           engineConfig, resultsFromParents, null);
    }else{
      resultsFromParents.add(executeMyself(queryId, session, null, deepSparkContext,
                                           engineConfig, resultsFromParents, null));
      return children.get(0).executeTreeTopDown(queryId, resultsFromParents, session, deepSparkContext, engineConfig);
    }

  }

}
