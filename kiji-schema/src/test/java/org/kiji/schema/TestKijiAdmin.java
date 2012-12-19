/**
 * (c) Copyright 2012 WibiData, Inc.
 *
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
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

package org.kiji.schema;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.kiji.schema.avro.TableLayoutDesc;
import org.kiji.schema.layout.KijiTableLayout;
import org.kiji.schema.layout.KijiTableLayouts;

public class TestKijiAdmin extends KijiClientTest {
  private static final Logger LOG = LoggerFactory.getLogger(TestKijiAdmin.class);

  private TableLayoutDesc mLayoutDesc;
  private TableLayoutDesc mLayoutDescUpdate;

  private KijiTableLayout getLayout(String table) throws IOException {
    return getKiji().getMetaTable().getTableLayout(table);
  }

  @Before
  public void setup() throws IOException {
    mLayoutDesc = KijiTableLayouts.getLayout(KijiTableLayouts.SIMPLE);
    mLayoutDescUpdate =
        KijiTableLayouts.getLayout(KijiTableLayouts.SIMPLE_UPDATE_NEW_LOCALITY_GROUP);
  }

  @Test
  public void testCreateTable() throws Exception {
    final KijiTableLayout tableLayout = new KijiTableLayout(mLayoutDesc, null);

    getKijiAdmin().createTable("table", tableLayout, false);
    assertEquals(tableLayout.getName().toString(), getLayout("table").getName().toString());
  }

  @Test
  public void testSetTableLayoutAdd() throws Exception {
    getKijiAdmin().createTable("table", new KijiTableLayout(mLayoutDesc, null), false);

    mLayoutDescUpdate.setReferenceLayout(getLayout("table").getDesc().getLayoutId());

    final KijiTableLayout tableLayout = getKijiAdmin().setTableLayout("table", mLayoutDescUpdate);
    assertEquals(tableLayout.getFamilies().size(), getLayout("table").getFamilies().size());
  }

  @Test
  public void testSetTableLayoutModify() throws Exception {
    getKijiAdmin().createTable("table", new KijiTableLayout(mLayoutDesc, null), false);

    final TableLayoutDesc newTableLayoutDesc = TableLayoutDesc.newBuilder(mLayoutDesc).build();
    assertEquals(3, (int) newTableLayoutDesc.getLocalityGroups().get(0).getMaxVersions());
    newTableLayoutDesc.getLocalityGroups().get(0).setMaxVersions(1);
    newTableLayoutDesc.setReferenceLayout(getLayout("table").getDesc().getLayoutId());

    final KijiTableLayout newTableLayout =
        getKijiAdmin().setTableLayout("table", newTableLayoutDesc);
    assertEquals(
        newTableLayout.getLocalityGroupMap().get("default").getDesc().getMaxVersions(),
        getLayout("table").getLocalityGroupMap().get("default").getDesc().getMaxVersions());
  }

  @Test(expected=IllegalArgumentException.class)
  public void testSetTableLayoutEmpty() throws Exception {
    final TableLayoutDesc tableLayoutDesc = new TableLayoutDesc();

    getKijiAdmin().setTableLayout("", tableLayoutDesc);
  }

  @Test(expected=RuntimeException.class)
  public void testDeleteTable() throws Exception {
    getKijiAdmin().createTable("table", new KijiTableLayout(mLayoutDesc, null), false);
    assertNotNull(getLayout("table"));

    getKijiAdmin().deleteTable("table");

    // Make sure it was deleted from the meta table, too.
    // The following line should throw a KijiTableNotFoundException.
    getLayout("table");
  }

  @Test(expected=KijiTableNotFoundException.class)
  public void testSetTableLayoutOnATableThatDoesNotExist() throws Exception {
    final TableLayoutDesc tableLayoutDesc = KijiTableLayouts.getLayout(KijiTableLayouts.SIMPLE);
    getKijiAdmin().setTableLayout("table", tableLayoutDesc);
  }
}