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

package com.stratio.meta.core.structures;

import java.util.ArrayList;
import java.util.List;

import com.stratio.meta.core.utils.ParserUtils;

/**
 * Class that models a relationship in a {@code WHERE} clause that includes a reference to the
 * {@code TOKEN} Cassandra function.
 */
public class RelationToken extends Relation {

  private boolean rightSideTokenType = false;

  public RelationToken(List<String> identifiers) {
    this.terms = new ArrayList<>();
    this.type = TYPE_TOKEN;
    this.identifiers = new ArrayList<>();
  }

  public RelationToken(List<String> identifiers, String operator) {
    this(identifiers);
    this.operator = operator;
  }

  public RelationToken(List<String> identifiers, String operator, Term term) {
    this(identifiers, operator);
    this.terms.add(term);
  }

  public RelationToken(List<String> identifiers, String operator, List<Term<?>> terms) {
    this(identifiers, operator);
    this.terms = terms;
    this.rightSideTokenType = true;
  }

  public boolean isRightSideTokenType() {
    return rightSideTokenType;
  }

  public void setRightSideTokenType(boolean rightSideTokenType) {
    this.rightSideTokenType = rightSideTokenType;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("TOKEN(");
    sb.append(ParserUtils.stringList(identifiers, ", ")).append(")");
    sb.append(" ").append(operator).append(" ");
    if (rightSideTokenType) {
      sb.append("TOKEN(").append(ParserUtils.stringList(terms, ", ")).append(")");
    } else {
      sb.append(ParserUtils.stringList(terms, ", "));
    }
    return sb.toString();
  }

}
