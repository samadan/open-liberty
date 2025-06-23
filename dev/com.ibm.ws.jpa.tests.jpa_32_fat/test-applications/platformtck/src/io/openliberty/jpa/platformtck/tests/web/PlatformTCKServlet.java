/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.jpa.platformtck.tests.web;

import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import componenttest.app.FATServlet;
import io.openliberty.jpa.platformtck.tests.models.TestEntity;
import jakarta.annotation.Resource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.annotation.WebServlet;
import jakarta.transaction.UserTransaction;
import jakarta.inject.Inject;
import org.junit.Ignore;

/**
 * Main test servlet for testing the jpa cdi integration changes in jpa 3.2.
 *
 * This servlet contains tests that verify the proper behavior of EntityManager injection
 * and usage in various CDI contexts. It tests:
 * 
 * 1. Basic EntityManager injection via CDI
 * 2. EntityManager instances with different scopes (shorter and longer than TransactionScoped)
 * 3. Multiple EntityManager instances from the same persistence unit
 * 4. EntityManager usage in complex bean hierarchies
 * Specification link https://jakarta.ee/specifications/webprofile/11/jakarta-webprofile-spec-11.0#obtaining-an-entity-manager-using-cdi
 */
@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/PlatformTCK32")
public class PlatformTCKServlet extends FATServlet {
    @Inject
    private EntityManager defaultEM; // Default TransactionScoped
    
    @Inject
    @ShortScoped
    @Dependent
    private EntityManager shortScopedEM; // Dependent scope (shorter than TransactionScoped)

    @Inject
    @LongScoped
    @ApplicationScoped
    private EntityManager longScopedEM; // ApplicationScoped (longer than TransactionScoped)
    
    @Inject
    private Instance<MultipleEntityManagerTest> multipleEMTestInstance;
    
    @Inject
    private Instance<NestedEntityManagerTest> nestedEMTestInstance;

    @Resource
    private UserTransaction tx;

    @Test
    public void alwaysPasses() {
        assertTrue(true);
    }

    @Test
    public void testEntityManagerInjection() throws Exception {
        // Start a transaction since the default EntityManager is TransactionScoped
        tx.begin();
        try {
            assertNotNull(defaultEM);
            System.out.println("EntityManager injected via @Inject");
            assertTrue(defaultEM.isOpen());
            System.out.println("EntityManager @Inject test passed!");
        } finally {
            tx.commit();
        }
    }

    @Test
    public void testEntityManagerWithShorterScope() throws Exception {
        // Start a transaction to ensure we have an active context
        tx.begin();
        try {
            assertNotNull(shortScopedEM);
            assertTrue(shortScopedEM.isOpen());
            System.out.println("Short-scoped EntityManager injected successfully");
            
            shortScopedEM.clear();
            System.out.println("Short-scoped EntityManager used within transaction");
            EntityManager em1 = shortScopedEM;
            
            // verify that the EntityManager is still usable within the transaction
            assertTrue(em1.isOpen());
            System.out.println("Short-scoped EntityManager is open within transaction");
        
            tx.commit();
            System.out.println("Transaction committed");
            
            tx.begin();
            assertTrue(shortScopedEM.isOpen());
            System.out.println("Short-scoped EntityManager is still open after transaction");
            System.out.println("Short-scoped EntityManager test passed!");
            tx.commit();
        } catch (Exception e) {
            if (tx.getStatus() != jakarta.transaction.Status.STATUS_NO_TRANSACTION) {
                tx.rollback();
            }
            throw e;
        }
    }

    @Test
    public void testEntityManagerWithLongerScope() throws Exception {
        tx.begin();
        try {
            assertNotNull(longScopedEM);
            assertTrue(longScopedEM.isOpen());
            System.out.println("Long-scoped EntityManager injected successfully");
            
            longScopedEM.clear();
            System.out.println("Long-scoped EntityManager used within transaction");
            tx.commit();
            System.out.println("Transaction committed");
            
            tx.begin();
            // The EntityManager should still be open after the transaction
            // because it's ApplicationScoped (longer than TransactionScoped)
            assertTrue(longScopedEM.isOpen());
            System.out.println("Long-scoped EntityManager is still open after transaction");
            
            longScopedEM.clear();
            tx.commit();
            System.out.println("Long-scoped EntityManager used in a second transaction");
        } catch (Exception e) {
            if (tx.getStatus() != jakarta.transaction.Status.STATUS_NO_TRANSACTION) {
                tx.rollback();
            }
            throw e;
        }
    }

    @Test
    public void testMultipleEntityManagersFromSamePU() throws Exception {
        // Test injecting two EntityManagers from same Persistence Unit (one with qualifier, one without)
        assertNotNull("Default EntityManager should not be null", defaultEM);
        assertNotNull("ShortScoped EntityManager should not be null", shortScopedEM);
        
        // Verify both are open
        tx.begin();
        try {
            assertTrue("Default EntityManager should be open", defaultEM.isOpen());
            assertTrue("ShortScoped EntityManager should be open", shortScopedEM.isOpen());
            
            // Verify they are different instances
            assertFalse("Default and ShortScoped EntityManagers should be different instances",
                       defaultEM == shortScopedEM);
            
            // Use default EntityManager to persist an entity
            TestEntity entity1 = new TestEntity("DefaultEntityManager");
            defaultEM.persist(entity1);
            System.out.println("Persisted entity1 with ID: " + entity1.getId());
            
            // Use qualified EntityManager to persist another entity
            TestEntity entity2 = new TestEntity("ShortScopedEntityManager");
            shortScopedEM.persist(entity2);
            System.out.println("Persisted entity2 with ID: " + entity2.getId());            
            // Commit and start a new transaction for verification
            tx.commit();
            System.out.println("Committed transaction after persisting entities");
            tx.begin();
            
            // Clear persistence contexts to ensure fresh data is loaded
            defaultEM.clear();
            shortScopedEM.clear();
            
            System.out.println("Cleared both EntityManagers");
            
            // Verify both entities were persisted and can be found by either EntityManager
            TestEntity found1 = defaultEM.find(TestEntity.class, entity1.getId());
            TestEntity found2 = shortScopedEM.find(TestEntity.class, entity2.getId());
            
            System.out.println("Found entity1 with defaultEM: " + (found1 != null));
            System.out.println("Found entity2 with shortScopedEM: " + (found2 != null));
            
            assertNotNull("Entity should be found with default EntityManager", found1);
            assertNotNull("Entity should be found with ShortScoped EntityManager", found2);
            
            System.out.println("Entity1 name: " + found1.getName());
            System.out.println("Entity2 name: " + found2.getName());
            
            assertEquals("DefaultEntityManager", found1.getName());
            assertEquals("ShortScopedEntityManager", found2.getName());
            
            // Cross-check: verify that each EntityManager can find entities created by the other
            TestEntity crossCheck1 = shortScopedEM.find(TestEntity.class, entity1.getId());
            TestEntity crossCheck2 = defaultEM.find(TestEntity.class, entity2.getId());
            
            assertNotNull("ShortScoped EntityManager should find entity created by default EntityManager", crossCheck1);
            assertNotNull("Default EntityManager should find entity created by ShortScoped EntityManager", crossCheck2);
            tx.commit();

            tx.begin();
            defaultEM.clear();
            shortScopedEM.clear();
            
            // Find entities again for removal
            TestEntity toRemove1 = defaultEM.find(TestEntity.class, entity1.getId());
            TestEntity toRemove2 = shortScopedEM.find(TestEntity.class, entity2.getId());
            
            if (toRemove1 != null) {
                defaultEM.remove(toRemove1);
                System.out.println("Removed entity1");
            } else {
                System.out.println("Entity1 not found for removal");
            }
            
            if (toRemove2 != null) {
                shortScopedEM.remove(toRemove2);
                System.out.println("Removed entity2");
            } else {
                System.out.println("Entity2 not found for removal");
            }
            tx.commit();
            System.out.println("Multiple EntityManagers from same PU test passed!");
        } catch (Exception e) {
            System.out.println("Test failed with exception: " + e.getMessage());
            e.printStackTrace(System.out);
            if (tx.getStatus() != jakarta.transaction.Status.STATUS_NO_TRANSACTION) {
                tx.rollback();
            }
            throw e;
        }
    }


    @Test
    public void testNestedEntityManagersInMultipleBeans() throws Exception {
        tx.begin();
        try {
            NestedEntityManagerTest nestedEMTest = nestedEMTestInstance.get();
            assertNotNull("NestedEntityManagerTest instance should not be null", nestedEMTest);
            
            nestedEMTest.createEntities();
            System.out.println("Created entities using parent and child beans");
            tx.commit();
            
            tx.begin();
            if (defaultEM != null) defaultEM.clear();
            if (shortScopedEM != null) shortScopedEM.clear();
            if (longScopedEM != null) longScopedEM.clear();
            
            // Verify entities can be found by all beans
            boolean verificationResult = nestedEMTest.verifyEntities();
            System.out.println("Verification result: " + verificationResult);
            assertTrue("All beans should be able to find entities created by other beans", verificationResult);
            System.out.println("Verified all beans can find entities created by others");
            tx.commit();

            tx.begin();
            if (defaultEM != null) defaultEM.clear();
            if (shortScopedEM != null) shortScopedEM.clear();
            if (longScopedEM != null) longScopedEM.clear();
            
            // Update entities using different beans than the ones that created them
            nestedEMTest.updateEntities();
            System.out.println("Updated entities using different beans than the ones that created them");
            
            if (defaultEM != null) defaultEM.flush();
            if (shortScopedEM != null) shortScopedEM.flush();
            if (longScopedEM != null) longScopedEM.flush();
            tx.commit();

            tx.begin();
            if (defaultEM != null) defaultEM.clear();
            if (shortScopedEM != null) shortScopedEM.clear();
            if (longScopedEM != null) longScopedEM.clear();
            
            // Verify updates are visible to all beans
            boolean updateVerificationResult = nestedEMTest.verifyUpdates();
            System.out.println("Update verification result: " + updateVerificationResult);
            assertTrue("All beans should see updates made by other beans", updateVerificationResult);
            System.out.println("Verified all beans can see updates made by others");
            tx.commit();

            tx.begin();
            if (defaultEM != null) defaultEM.clear();
            if (shortScopedEM != null) shortScopedEM.clear();
            if (longScopedEM != null) longScopedEM.clear();
            // Clean up
            nestedEMTest.cleanupEntities();            
            tx.commit();
            System.out.println("Nested EntityManagers in multiple beans test passed!");
        } catch (Exception e) {
            System.out.println("Test failed with exception: " + e.getMessage());
            e.printStackTrace(System.out);
            if (tx.getStatus() != jakarta.transaction.Status.STATUS_NO_TRANSACTION) {
                tx.rollback();
            }
            throw e;
        }
    }
}
