/*
 * Stratio Meta
 * 
 * Copyright (c) 2014, Stratio, All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free Software Foundation; either version
 * 3.0 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along with this library.
 */

package com.stratio.meta.core.statements;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.datastax.driver.core.ColumnMetadata;
import com.datastax.driver.core.TableMetadata;
import com.stratio.meta.common.result.QueryResult;
import com.stratio.meta.common.result.Result;
import com.stratio.meta.core.engine.EngineConfig;
import com.stratio.meta.core.metadata.MetadataManager;
import com.stratio.meta.core.structures.Assignment;
import com.stratio.meta.core.structures.FloatTerm;
import com.stratio.meta.core.structures.IdentIntOrLiteral;
import com.stratio.meta.core.structures.IdentifierAssignment;
import com.stratio.meta.core.structures.IntTerm;
import com.stratio.meta.core.structures.IntegerTerm;
import com.stratio.meta.core.structures.Option;
import com.stratio.meta.core.structures.Relation;
import com.stratio.meta.core.structures.SelectorIdentifier;
import com.stratio.meta.core.structures.Term;
import com.stratio.meta.core.structures.ValueAssignment;
import com.stratio.meta.core.structures.ValueProperty;
import com.stratio.meta.core.utils.CoreUtils;
import com.stratio.meta.core.utils.MetaPath;
import com.stratio.meta.core.utils.MetaStep;
import com.stratio.meta.core.utils.ParserUtils;
import com.stratio.meta.core.utils.Tree;

/**
 * Class that models an {@code UPDATE} statement from the META language.
 */
public class UpdateTableStatement extends MetaStatement {

  /**
   * The name of the table.
   */
  private String tableName;

  /**
   * Whether options are included.
   */
  private boolean optsInc;

  /**
   * The list of options.
   */
  private List<Option> options;

  /**
   * The list of assignments.
   */
  private List<Assignment> assignments;

  /**
   * The list of relations.
   */
  private List<Relation> whereClauses;

  /**
   * Whether conditions are included.
   */
  private boolean condsInc;

  /**
   * Map of conditions.
   */
  private Map<String, Term<?>> conditions;

  /**
   * Class constructor.
   * 
   * @param tableName The name of the table.
   * @param optsInc Whether options are included.
   * @param options The list of options.
   * @param assignments The list of assignments.
   * @param whereClauses The list of relations.
   * @param condsInc Whether conditions are included.
   * @param conditions The map of conditions.
   */
  public UpdateTableStatement(String tableName, boolean optsInc, List<Option> options,
      List<Assignment> assignments, List<Relation> whereClauses, boolean condsInc,
      Map<String, Term<?>> conditions) {
    this.command = false;
    if (tableName.contains(".")) {
      String[] ksAndTableName = tableName.split("\\.");
      keyspace = ksAndTableName[0];
      this.tableName = ksAndTableName[1];
      keyspaceInc = true;
    } else {
      this.tableName = tableName;
    }

    this.optsInc = optsInc;

    // this.options = options;
    if (optsInc) {
      this.options = new ArrayList<>();
      for (Option opt : options) {
        opt.setNameProperty(opt.getNameProperty().toLowerCase());
        this.options.add(opt);
      }
    }

    this.assignments = assignments;
    this.whereClauses = whereClauses;
    this.condsInc = condsInc;
    this.conditions = conditions;
  }

  /**
   * Class constructor.
   * 
   * @param tableName The name of the table.
   * @param options The list of options.
   * @param assignments The list of assignments.
   * @param whereClauses The list of relations.
   * @param conditions The map of conditions.
   */
  public UpdateTableStatement(String tableName, List<Option> options, List<Assignment> assignments,
      List<Relation> whereClauses, Map<String, Term<?>> conditions) {
    this(tableName, true, options, assignments, whereClauses, true, conditions);
  }

  /**
   * Class constructor.
   * 
   * @param tableName The name of the table.
   * @param assignments The list of assignments.
   * @param whereClauses The list of relations.
   * @param conditions The map of conditions.
   */
  public UpdateTableStatement(String tableName, List<Assignment> assignments,
      List<Relation> whereClauses, Map<String, Term<?>> conditions) {
    this(tableName, false, null, assignments, whereClauses, true, conditions);
  }

  /**
   * Class constructor.
   * 
   * @param tableName The name of the table.
   * @param options The list of options.
   * @param assignments The list of assignments.
   * @param whereClauses The list of relations.
   */
  public UpdateTableStatement(String tableName, List<Option> options, List<Assignment> assignments,
      List<Relation> whereClauses) {
    this(tableName, true, options, assignments, whereClauses, false, null);
  }

  /**
   * Class constructor.
   * 
   * @param tableName The name of the table.
   * @param assignments The list of assignments.
   * @param whereClauses The list of relations.
   */
  public UpdateTableStatement(String tableName, List<Assignment> assignments,
      List<Relation> whereClauses) {
    this(tableName, false, null, assignments, whereClauses, false, null);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("UPDATE ");
    if (keyspaceInc) {
      sb.append(keyspace).append(".");
    }
    sb.append(tableName);
    if (optsInc) {
      sb.append(" ").append("USING ");
      sb.append(ParserUtils.stringList(options, " AND "));
    }
    sb.append(" ").append("SET ");
    sb.append(ParserUtils.stringList(assignments, ", "));
    sb.append(" ").append("WHERE ");
    sb.append(ParserUtils.stringList(whereClauses, " AND "));
    if (condsInc) {
      sb.append(" ").append("IF ");
      sb.append(ParserUtils.stringMap(conditions, " = ", " AND "));
    }
    return sb.toString();
  }

  @Override
  public String translateToCQL() {
    return this.toString();
  }

  /**
   * Validate the semantics of the current statement. This method checks the existing metadata to
   * determine that all referenced entities exists in the {@code targetKeyspace} and the types are
   * compatible with the assignations or comparisons.
   * 
   * @param metadata The {@link com.stratio.meta.core.metadata.MetadataManager} that provides the
   *        required information.
   * @return A {@link com.stratio.meta.common.result.Result} with the validation result.
   */
  @Override
  public Result validate(MetadataManager metadata, EngineConfig config) {
    Result result =
        validateKeyspaceAndTable(metadata, sessionKeyspace, keyspaceInc, keyspace, tableName);
    if (!result.hasError()) {
      TableMetadata tableMetadata = metadata.getTableMetadata(getEffectiveKeyspace(), tableName);

      if (optsInc) {
        result = validateOptions();
      }

      if (!result.hasError()) {
        result = validateAssignments(tableMetadata);
      }

      if (!result.hasError()) {
        result = validateWhereClauses(tableMetadata);
      }

      if ((!result.hasError()) && condsInc) {
        result = validateConds(tableMetadata);
      }
    }
    return result;
  }

  private Result validateConds(TableMetadata tableMetadata) {
    updateTermClassesInConditions(tableMetadata);
    Result result = QueryResult.createSuccessQueryResult();
    for (String key : conditions.keySet()) {
      ColumnMetadata cm = tableMetadata.getColumn(key);
      if (cm != null) {
        if (!(cm.getType().asJavaClass() == conditions.get(key).getTermClass())) {
          result =
              Result.createValidationErrorResult("Column " + key + " should be type "
                  + cm.getType().asJavaClass().getSimpleName());
        }
      } else {
        result =
            Result.createValidationErrorResult("Column " + key + " was not found in table "
                + tableName);
      }
    }
    return result;
  }

  private void updateTermClassesInConditions(TableMetadata tableMetadata) {
    for (String ident : conditions.keySet()) {
      ColumnMetadata cm = tableMetadata.getColumn(ident);
      Term<?> term = conditions.get(ident);
      if ((cm != null) && (term != null)) {
        if (term instanceof Term) {
          if (CoreUtils.castForLongType(cm, term)) {
            conditions.put(ident, new IntegerTerm((Term<Long>) term));
          } else if (CoreUtils.castForDoubleType(cm, term)) {
            conditions.put(ident, new FloatTerm((Term<Double>) term));
          }
        }
      }
    }
  }

  private Result validateOptions() {
    Result result = QueryResult.createSuccessQueryResult();
    for (Option opt : options) {
      if (!("ttl".equalsIgnoreCase(opt.getNameProperty()) || "timestamp".equalsIgnoreCase(opt
          .getNameProperty()))) {
        result =
            Result.createValidationErrorResult("TIMESTAMP and TTL are the only accepted options.");
      }
    }
    for (Option opt : options) {
      if (opt.getProperties().getType() != ValueProperty.TYPE_CONST) {
        result = Result.createValidationErrorResult("TIMESTAMP and TTL must have a constant value.");
      }
    }
    return result;
  }

  /**
   * Check that the specified columns exist on the target table
   * 
   * @param tableMetadata Table metadata associated with the target table.
   * @return A {@link com.stratio.meta.common.result.Result} with the validation result.
   */
  private Result validateAssignments(TableMetadata tableMetadata) {
    updateTermClassesInAssignments(tableMetadata);
    Result result = QueryResult.createSuccessQueryResult();
    for (int index = 0; index < assignments.size(); index++) {
      Assignment assignment = assignments.get(index);

      IdentifierAssignment assignmentId = assignment.getIdent();

      // Check if identifier exists
      ColumnMetadata cm = tableMetadata.getColumn(assignmentId.getIdentifier());
      if (cm == null) {
        result =
            Result.createValidationErrorResult("Column " + assignmentId.getIdentifier()
                + " not found in " + tableMetadata.getName() + ".");
        break;
      }

      // Check if column data type of the identifier is one of the supported types
      Class<?> idClazz = cm.getType().asJavaClass();
      if (!result.hasError()) {
        if (!CoreUtils.supportedTypes.contains(idClazz.getSimpleName().toLowerCase())) {
          result =
              Result.createValidationErrorResult("Column " + assignmentId.getIdentifier()
                  + " is of type " + cm.getType().asJavaClass().getSimpleName()
                  + ", which is not supported yet.");
        }
      }

      // Check if identifier is simple, otherwise it refers to a collection, which are not supported
      // yet
      if (!result.hasError()) {
        if (assignmentId.getType() == IdentifierAssignment.TYPE_COMPOUND) {
          result = Result.createValidationErrorResult("Collections are not supported yet.");
        }
      }

      if (!result.hasError()) {
        ValueAssignment valueAssignment = assignment.getValue();
        if (valueAssignment.getType() == ValueAssignment.TYPE_TERM) {
          Term<?> valueTerm = valueAssignment.getTerm();
          // Check data type between column of the identifier and term type of the statement
          Class<?> valueClazz = valueTerm.getTermClass();
          String valueClass = valueClazz.getSimpleName();
          if (!idClazz.getSimpleName().equalsIgnoreCase(valueClass)) {
            result =
                Result.createValidationErrorResult(cm.getName() + " and " + valueTerm.getTermValue()
                    + " are not compatible type.");
          }
        } else if (valueAssignment.getType() == ValueAssignment.TYPE_IDENT_MAP) {
          result = Result.createValidationErrorResult("Collections are not supported yet.");
        } else {
          IdentIntOrLiteral iiol = valueAssignment.getIiol();
          if (iiol instanceof IntTerm) {
            // Check if identifier is of int type
            if (!Arrays.asList("integer", "int").contains(idClazz.getSimpleName().toLowerCase())) {
              result =
                  Result.createValidationErrorResult("Column " + cm.getName()
                      + " should be integer type.");
            }
            if (!result.hasError()) {
              // Check if value identifier exists
              String valueId = iiol.getIdentifier();
              ColumnMetadata colValue = tableMetadata.getColumn(valueId);
              if (colValue == null) {
                result = Result.createValidationErrorResult("Column " + valueId + " not found.");
              }
              if (!result.hasError()) {
                // Check if value identifier is int type
                if (!Arrays.asList("integer", "int").contains(
                    colValue.getType().asJavaClass().getSimpleName().toLowerCase())) {
                  result =
                      Result.createValidationErrorResult("Column " + colValue.getName()
                          + " should be integer type.");
                }
              }
            }

          } else { // Set or List
            result = Result.createValidationErrorResult("Collections are not supported yet.");
          }
        }
      }
    }
    return result;
  }

  private void updateTermClassesInAssignments(TableMetadata tableMetadata) {
    for (Assignment assignment : assignments) {
      String ident = assignment.getIdent().getIdentifier();
      ColumnMetadata cm = tableMetadata.getColumn(ident);
      Term<?> term = assignment.getValue().getTerm();
      if ((cm != null) && (term != null)) {
        if (term instanceof Term) {
          if (CoreUtils.castForLongType(cm, term)) {
            assignment.getValue().setTerm(new IntegerTerm((Term<Long>) term));
          } else if (CoreUtils.castForDoubleType(cm, term)) {
            assignment.getValue().setTerm(new FloatTerm((Term<Double>) term));
          }
        }
      }
    }
  }

  private Result validateWhereClauses(TableMetadata tableMetadata) {
    Result result = QueryResult.createSuccessQueryResult();
    for (Relation rel : whereClauses) {
      Term<?> term = rel.getTerms().get(0);
      Class<?> valueClazz = term.getTermClass();
      for (SelectorIdentifier id : rel.getIdentifiers()) {
        boolean foundAndSameType = false;
        for (ColumnMetadata cm : tableMetadata.getColumns()) {
          if (cm.getName().equalsIgnoreCase(id.toString())
              && (cm.getType().asJavaClass() == valueClazz)) {
            foundAndSameType = true;
            break;
          }
        }
        if (!foundAndSameType) {
          result =
              Result.createValidationErrorResult("Column " + id + " not found in "
                  + tableMetadata.getName());
          break;
        }
      }
      if (result.hasError()) {
        break;
      }
    }
    return result;
  }

  @Override
  public Tree getPlan(MetadataManager metadataManager, String targetKeyspace) {
    Tree tree = new Tree();
    tree.setNode(new MetaStep(MetaPath.CASSANDRA, this));
    return tree;
  }

}
