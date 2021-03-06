package org.ff4j.test.store;

/*
 * #%L ff4j-store-jdbc %% Copyright (C) 2013 Ff4J %% Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License. #L%
 */

import org.ff4j.FF4j;
import org.ff4j.core.FeatureStore;
import org.ff4j.test.AbstractStoreTest;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:*applicationContext-jdbc-test.xml")
public class JdbcFeatureStoreSpring2Test extends AbstractStoreTest {

    @Autowired
    private FF4j ff4j;

    private static FeatureStore store = null;

    @Override
    protected FeatureStore initStore() {
        if (store == null) {
            System.out.println("OK");
            store = ff4j.getStore();
        }
        return store;
    }
}