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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import io.openliberty.jpa.platformtck.tests.models.TestEntity;

/**
 * This class demonstrates injecting and using multiple EntityManager instances with different scopes
 * within a single CDI bean.
 *
 * This test is important for validating that the JPA provider correctly handles
 * multiple EntityManager instances from the same persistence unit but with different
 * CDI scopes, ensuring they all operate on the same underlying persistence context.
 */
@RequestScoped
public class MultipleEntityManagerTest {
    
    // Inject EntityManager with different qualifiers
    @Inject
    private EntityManager defaultEM;
    
    @Inject
    @ShortScoped
    @Dependent
    private EntityManager shortScopedEM;
    
    @Inject
    @LongScoped
    @ApplicationScoped
    private EntityManager longScopedEM;
    
    // Fields to store entity IDs for verification
    private Long defaultEntityId;
    private Long shortScopedEntityId;
    private Long longScopedEntityId;
    
    /**
     * Creates entities using different EntityManager instances
     */
    @Transactional
    public void createEntities() {
        // Create and persist entities using different EntityManager instances
        TestEntity defaultEntity = new TestEntity("Created by defaultEM");
        defaultEM.persist(defaultEntity);
        defaultEntityId = defaultEntity.getId();
        
        TestEntity shortScopedEntity = new TestEntity("Created by shortScopedEM");
        shortScopedEM.persist(shortScopedEntity);
        shortScopedEntityId = shortScopedEntity.getId();
        
        TestEntity longScopedEntity = new TestEntity("Created by longScopedEM");
        longScopedEM.persist(longScopedEntity);
        longScopedEntityId = longScopedEntity.getId();
    }
    
    /**
     * Verifies that entities created by one EntityManager can be found by others
     * @return true if all verifications pass
     */
    @Transactional
    public boolean verifyEntities() {
        try {
            // Clear all EntityManagers first to ensure fresh data
            clearAll();
            
            // Verify each EntityManager can find its own entity
            TestEntity defaultFound = defaultEM.find(TestEntity.class, defaultEntityId);
            TestEntity shortFound = shortScopedEM.find(TestEntity.class, shortScopedEntityId);
            TestEntity longFound = longScopedEM.find(TestEntity.class, longScopedEntityId);
            
            // Log entity IDs and found status for debugging
            System.out.println("Default entity ID: " + defaultEntityId + ", found: " + (defaultFound != null));
            System.out.println("ShortScoped entity ID: " + shortScopedEntityId + ", found: " + (shortFound != null));
            System.out.println("LongScoped entity ID: " + longScopedEntityId + ", found: " + (longFound != null));
            
            if (defaultFound == null || shortFound == null || longFound == null) {
                System.out.println("One or more entities not found by their creating EntityManager");
                return false;
            }
            
            // Clear again before cross-verification
            clearAll();
            
            // Cross-verification: each EntityManager should be able to find entities created by others
            TestEntity defaultFoundByShort = shortScopedEM.find(TestEntity.class, defaultEntityId);
            TestEntity defaultFoundByLong = longScopedEM.find(TestEntity.class, defaultEntityId);
            
            TestEntity shortFoundByDefault = defaultEM.find(TestEntity.class, shortScopedEntityId);
            TestEntity shortFoundByLong = longScopedEM.find(TestEntity.class, shortScopedEntityId);
            
            TestEntity longFoundByDefault = defaultEM.find(TestEntity.class, longScopedEntityId);
            TestEntity longFoundByShort = shortScopedEM.find(TestEntity.class, longScopedEntityId);
            
            // Log cross-verification results for debugging
            System.out.println("Default entity found by ShortScoped EM: " + (defaultFoundByShort != null));
            System.out.println("Default entity found by LongScoped EM: " + (defaultFoundByLong != null));
            System.out.println("ShortScoped entity found by Default EM: " + (shortFoundByDefault != null));
            System.out.println("ShortScoped entity found by LongScoped EM: " + (shortFoundByLong != null));
            System.out.println("LongScoped entity found by Default EM: " + (longFoundByDefault != null));
            System.out.println("LongScoped entity found by ShortScoped EM: " + (longFoundByShort != null));
            
            boolean result = defaultFoundByShort != null && defaultFoundByLong != null &&
                             shortFoundByDefault != null && shortFoundByLong != null &&
                             longFoundByDefault != null && longFoundByShort != null;
            
            System.out.println("Overall verification result: " + result);
            return result;
        } catch (Exception e) {
            System.out.println("Exception during verifyEntities: " + e.getMessage());
            e.printStackTrace(System.out);
            return false;
        }
    }
    
    /**
     * Updates entities using different EntityManager instances than the ones that created them
     */
    @Transactional
    public void updateEntities() {
        try {
            //Clear all EntityManagers first to ensure fresh data
            clearAll();
            
            // Update entities using different EntityManager instances than the ones that created them
            TestEntity defaultEntity = shortScopedEM.find(TestEntity.class, defaultEntityId);
            if (defaultEntity != null) {
                defaultEntity.setName("Updated by shortScopedEM");
                System.out.println("Default entity updated by shortScopedEM");
            } else {
                System.out.println("ERROR: Default entity not found by shortScopedEM");
            }
            
            TestEntity shortEntity = longScopedEM.find(TestEntity.class, shortScopedEntityId);
            if (shortEntity != null) {
                shortEntity.setName("Updated by longScopedEM");
                System.out.println("ShortScoped entity updated by longScopedEM");
            } else {
                System.out.println("ERROR: ShortScoped entity not found by longScopedEM");
            }
            
            TestEntity longEntity = defaultEM.find(TestEntity.class, longScopedEntityId);
            if (longEntity != null) {
                longEntity.setName("Updated by defaultEM");
                System.out.println("LongScoped entity updated by defaultEM");
            } else {
                System.out.println("ERROR: LongScoped entity not found by defaultEM");
            }
            
        } catch (Exception e) {
            System.out.println("Exception during updateEntities: " + e.getMessage());
            e.printStackTrace(System.out);
            throw e;
        }
    }
    
    /**
     * Verifies that updates made by one EntityManager are visible to others
     * @return true if all verifications pass
     */
    @Transactional
    public boolean verifyUpdates() {
        try {
            // Clear all EntityManagers first to ensure fresh data
            clearAll();
            
            // Find entities using their respective EntityManagers
            TestEntity defaultEntity = defaultEM.find(TestEntity.class, defaultEntityId);
            TestEntity shortEntity = shortScopedEM.find(TestEntity.class, shortScopedEntityId);
            TestEntity longEntity = longScopedEM.find(TestEntity.class, longScopedEntityId);
            
            // Log actual values for debugging
            System.out.println("Default entity name: " + (defaultEntity != null ? defaultEntity.getName() : "null"));
            System.out.println("ShortScoped entity name: " + (shortEntity != null ? shortEntity.getName() : "null"));
            System.out.println("LongScoped entity name: " + (longEntity != null ? longEntity.getName() : "null"));
            
            if (defaultEntity == null || shortEntity == null || longEntity == null) {
                System.out.println("One or more entities not found during update verification");
                return false;
            }
            
            boolean defaultUpdated = "Updated by shortScopedEM".equals(defaultEntity.getName());
            boolean shortUpdated = "Updated by longScopedEM".equals(shortEntity.getName());
            boolean longUpdated = "Updated by defaultEM".equals(longEntity.getName());
            
            System.out.println("Default entity correctly updated: " + defaultUpdated);
            System.out.println("ShortScoped entity correctly updated: " + shortUpdated);
            System.out.println("LongScoped entity correctly updated: " + longUpdated);
            
            // Try cross-checking with other EntityManagers if any verification failed
            if (!defaultUpdated || !shortUpdated || !longUpdated) {
                System.out.println("Some updates not verified, trying cross-check with other EntityManagers");
                
                // Clear again before cross-verification
                clearAll();
                
                // Cross-check with different EntityManagers
                TestEntity defaultByLong = longScopedEM.find(TestEntity.class, defaultEntityId);
                TestEntity shortByDefault = defaultEM.find(TestEntity.class, shortScopedEntityId);
                TestEntity longByShort = shortScopedEM.find(TestEntity.class, longScopedEntityId);
                
                System.out.println("Cross-check - Default entity name via longScopedEM: " +
                                  (defaultByLong != null ? defaultByLong.getName() : "null"));
                System.out.println("Cross-check - Short entity name via defaultEM: " +
                                  (shortByDefault != null ? shortByDefault.getName() : "null"));
                System.out.println("Cross-check - Long entity name via shortScopedEM: " +
                                  (longByShort != null ? longByShort.getName() : "null"));
                
                // Update verification with cross-check results
                if (!defaultUpdated && defaultByLong != null) {
                    defaultUpdated = "Updated by shortScopedEM".equals(defaultByLong.getName());
                }
                if (!shortUpdated && shortByDefault != null) {
                    shortUpdated = "Updated by longScopedEM".equals(shortByDefault.getName());
                }
                if (!longUpdated && longByShort != null) {
                    longUpdated = "Updated by defaultEM".equals(longByShort.getName());
                }
                
                System.out.println("After cross-check - Default entity correctly updated: " + defaultUpdated);
                System.out.println("After cross-check - ShortScoped entity correctly updated: " + shortUpdated);
                System.out.println("After cross-check - LongScoped entity correctly updated: " + longUpdated);
            }
            
            return defaultUpdated && shortUpdated && longUpdated;
        } catch (Exception e) {
            System.out.println("Exception during verifyUpdates: " + e.getMessage());
            e.printStackTrace(System.out);
            return false;
        }
    }
    
    /**
     * Cleans up by removing all created entities
     */
    @Transactional
    public void cleanupEntities() {
        defaultEM.remove(defaultEM.find(TestEntity.class, defaultEntityId));
        shortScopedEM.remove(shortScopedEM.find(TestEntity.class, shortScopedEntityId));
        longScopedEM.remove(longScopedEM.find(TestEntity.class, longScopedEntityId));
    }
    
    public EntityManager getDefaultEM() {
        return defaultEM;
    }
    
    public EntityManager getShortScopedEM() {
        return shortScopedEM;
    }
    
    public EntityManager getLongScopedEM() {
        return longScopedEM;
    }
    

    
    /**
     * Clears all EntityManager instances to ensure fresh data is loaded from the database
     */
    @Transactional
    public void clearAll() {
        defaultEM.clear();
        shortScopedEM.clear();
        longScopedEM.clear();
    }
}

