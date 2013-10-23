/*
 * Copyright (C) 2013 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.opendatakit.aggregate.odktables;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.opendatakit.aggregate.odktables.exception.TableAlreadyExistsException;
import org.opendatakit.aggregate.odktables.rest.entity.Column;
import org.opendatakit.aggregate.odktables.rest.entity.Scope;
import org.opendatakit.aggregate.odktables.rest.entity.TableEntry;
import org.opendatakit.aggregate.odktables.rest.entity.TableRole;
import org.opendatakit.common.persistence.exception.ODKDatastoreException;
import org.opendatakit.common.persistence.exception.ODKEntityNotFoundException;
import org.opendatakit.common.persistence.exception.ODKEntityPersistException;
import org.opendatakit.common.persistence.exception.ODKTaskLockException;
import org.opendatakit.common.web.CallingContext;
import org.opendatakit.common.web.TestContextFactory;

import com.google.common.collect.Lists;

// TODO: tests here have been updated and if they fail are likely due to config
// errors as much as real errors.
public class TableManagerTest {

  private CallingContext cc;
  private TableManager tm;
  private String tableId;
  private String tableName;
  private String tableId2;
  private List<Column> columns;
  private String tableProperties;

  @Before
  public void setUp() throws Exception {
    this.cc = TestContextFactory.getCallingContext();
    this.tm = new TableManager(cc);
    this.tableId = T.tableId;
    this.tableName = T.tableName;
    this.tableId2 = T.tableId + "2";
    this.columns = T.columns;
    this.tableProperties = T.tableMetadata;
  }

  @After
  public void tearDown() throws Exception {
    try {
      tm.deleteTable(tableId);
    } catch (ODKEntityNotFoundException e) {
      // ignore
    }
    try {
      tm.deleteTable(tableId2);
    } catch (ODKEntityNotFoundException e) {
      // ignore
    }
  }

  @Test
  public void testGetTablesEmpty() throws ODKDatastoreException {
    List<TableEntry> entries = tm.getTables();
    assertTrue(entries.isEmpty());
  }

  @Test
  public void testCreateTable() throws ODKDatastoreException, TableAlreadyExistsException {
    TableEntry entry = tm.createTable(tableId, T.tableKey, T.dbTableName,
        T.tableType, T.tableIdAccessControls, T.columns,
        T.kvsEntries);
    assertEquals(tableId, entry.getTableId());
    assertNotNull(entry.getPropertiesEtag());
    // data eTag is null when table is first created
    assertTrue(null == entry.getDataEtag());
  }

  @Test(expected = TableAlreadyExistsException.class)
  public void testCreateTableAlreadyExists() throws ODKDatastoreException,
      TableAlreadyExistsException {
    tm.createTable(tableId, T.tableKey, T.dbTableName,
        T.tableType, T.tableIdAccessControls, T.columns,
        T.kvsEntries);
    tm.createTable(tableId, T.tableKey, T.dbTableName,
        T.tableType, T.tableIdAccessControls, T.columns,
        T.kvsEntries);
  }

//  @Test(expected = IllegalArgumentException.class)
//  public void testCreateTableNullTableId() throws ODKEntityPersistException, ODKDatastoreException,
//      TableAlreadyExistsException {
//    tm.createTable(null, tableName, columns, tableProperties);
//  }
//
//  @Test(expected = IllegalArgumentException.class)
//  public void testCreateTableNullTableName() throws ODKEntityPersistException,
//      ODKDatastoreException, TableAlreadyExistsException {
//    tm.createTable(tableId, null, columns, tableProperties);
//  }
//
//  @Test(expected = IllegalArgumentException.class)
//  public void testCreateTableNullColumns() throws ODKEntityPersistException, ODKDatastoreException,
//      TableAlreadyExistsException {
//    tm.createTable(tableId, tableName, null, tableProperties);
//  }

  @Test
  public void testGetTable() throws ODKDatastoreException, TableAlreadyExistsException {
    TableEntry expected = tm.createTable(tableId, T.tableKey, T.dbTableName,
        T.tableType, T.tableIdAccessControls, T.columns,
        T.kvsEntries);
    TableEntry actual = tm.getTableNullSafe(tableId);
    assertEquals(expected, actual);
  }

  @Test(expected = ODKEntityNotFoundException.class)
  public void testGetTableDoesNotExist() throws ODKEntityNotFoundException, ODKDatastoreException {
    tm.getTableNullSafe(tableId);
  }

  @Test(expected = NullPointerException.class)
  public void testGetTableNullTableId() throws ODKEntityNotFoundException, ODKDatastoreException {
    tm.getTableNullSafe(null);
  }

  @Test
  public void testGetTables() throws ODKEntityPersistException, ODKDatastoreException,
      ODKTaskLockException, TableAlreadyExistsException {
    List<TableEntry> expected = new ArrayList<TableEntry>();

    TableEntry one = tm.createTable(tableId2, T.tableKey, T.dbTableName,
        T.tableType, T.tableIdAccessControls, T.columns,
        T.kvsEntries);
    TableEntry two = tm.createTable(tableId, T.tableKey, T.dbTableName,
        T.tableType, T.tableIdAccessControls, T.columns,
        T.kvsEntries);

    expected.add(one);
    expected.add(two);

    List<TableEntry> actual = tm.getTables();
    assertEquals(2, actual.size());

    Util.assertCollectionSameElements(expected, actual);
  }

  // TODO: reactivate when we have scopes working...
  @Ignore
  public void testGetTablesByScopes() throws ODKEntityNotFoundException, ODKDatastoreException,
      TableAlreadyExistsException {
    List<TableEntry> expected = new ArrayList<TableEntry>();

    TableEntry one = tm.createTable(tableId2, T.tableKey, T.dbTableName,
        T.tableType, T.tableIdAccessControls, T.columns,
        T.kvsEntries);
    tm.createTable(tableId, T.tableKey, T.dbTableName,
        T.tableType, T.tableIdAccessControls, T.columns,
        T.kvsEntries);

    TableAclManager am = new TableAclManager(one.getTableId(), cc);
    Scope scope = new Scope(Scope.Type.DEFAULT, null);
    am.setAcl(scope, TableRole.READER);

    expected.add(one);

    List<TableEntry> actual = tm.getTables(Lists.newArrayList(scope));

    Util.assertCollectionSameElements(expected, actual);
  }

  @Test(expected = ODKEntityNotFoundException.class)
  public void testDeleteTable() throws ODKDatastoreException, ODKTaskLockException,
      TableAlreadyExistsException {
    tm.createTable(tableId, T.tableKey, T.dbTableName,
        T.tableType, T.tableIdAccessControls, T.columns,
        T.kvsEntries);    tm.deleteTable(tableId);
    tm.getTableNullSafe(tableId);
  }

  @Test(expected = ODKEntityNotFoundException.class)
  public void testDeleteTableDoesNotExist() throws ODKDatastoreException, ODKTaskLockException {
    tm.deleteTable(tableId);
  }

  @Test(expected = NullPointerException.class)
  public void testDeleteTableNullTableId() throws ODKDatastoreException, ODKTaskLockException {
    tm.deleteTable(null);
  }

}
