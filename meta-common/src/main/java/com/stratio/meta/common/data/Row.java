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

package com.stratio.meta.common.data;

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class Row implements Serializable {

    private static final long serialVersionUID = 2053597366590152927L;
    private Map<String, Cell> cells;

    /**
     * Row default constructor.
     */
    public Row() {
        cells = new LinkedHashMap<>();
    }

    /**
     * Row params constructor with a cell.
     *
     * @param key Key of the cell
     * @param cell A {@link com.stratio.meta.common.data.Cell}
     */
    public Row(String key, Cell cell) {
        this();
        addCell(key, cell);
    }

    /**
     * Get the size of row.
     *
     * @return the size requested
     */
    public int size(){
        return cells.size();
    }

    /**
     * Get the cells of current row.
     *
     * @return A map which contains the cells
     */
    public Map<String, Cell> getCells() {
        return cells;
    }

    /**
     * Get the collection of cells.
     * @return A collection of Cells.
     */
    public Collection<Cell> getCellList(){
        return cells.values();
    }

    /**
     * Set the cells of the row.
     *
     * @param cells A map of cells
     */
    public void setCells(Map<String, Cell> cells) {
        this.cells = cells;
    }

    /**
     * Add a Cell to the row.
     *
     * @param key Key
     * @param cell Cell
     */
    public void addCell(String key, Cell cell){
        cells.put(key, cell);
        cells.values();
    }

    /**
     * Get a cell by key.
     *
     * @param key Key of the cell
     * @return Cell requested.
     */
    public Cell getCell(String key){
        return cells.get(key);
    }

}
