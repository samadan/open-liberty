package com.ibm.ws.jpa.container.v32.cdi.internal;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jpa.JPAAccessor;
import com.ibm.ws.jpa.JPAComponent;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.persistence.Cache;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnitUtil;
import jakarta.persistence.SchemaManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.metamodel.Metamodel;
import jakarta.persistence.spi.PersistenceUnitInfo;

public class JPACDIExtension implements Extension {

    private static final TraceComponent tc = Tr.register(JPACDIExtension.class);

    private final JPAComponent jpaComponent = JPAAccessor.getJPAComponent();

    public void afterBeanDiscovery(@Observes AfterBeanDiscovery abd, BeanManager bm) {
        J2EEName j2EEName = getCurrentAppJ2EEName();
        List<PersistenceUnitInfo> persistenceUnits = jpaComponent.getPersistenceUnits(j2EEName);

        for (PersistenceUnitInfo pui : persistenceUnits) {
            try {
                createBeanForPersistenceUnit(abd, pui, j2EEName);
            } catch (ClassNotFoundException e) {
                //TODO we'll probably want a better message and NLS translation
                Tr.warning(tc, "Could not create bean for PersistenceUnit {0}, it will not be injectable", pui.getPersistenceUnitName());
            }
        }
    }

    private void createBeanForPersistenceUnit(AfterBeanDiscovery abd, final PersistenceUnitInfo pui, final J2EEName j2eeName) throws ClassNotFoundException {
        Set<Annotation> qualfiiers = getQualifiers(pui);
        Class<? extends Annotation> scope = getScope(pui);

        abd.addBean().types(EntityManager.class).addQualifiers(qualfiiers).scope(scope).produceWith((instance) -> jpaComponent.getEntityManager(j2eeName, pui));
        abd.addBean().types(EntityManagerFactory.class).addQualifiers(qualfiiers).scope(scope).produceWith((instance) -> jpaComponent.getEntityManagerFactory(j2eeName, pui));
        abd.addBean().types(PersistenceUnitUtil.class).addQualifiers(qualfiiers).scope(scope).produceWith((instance) -> jpaComponent.getEntityManagerFactory(j2eeName,
                                                                                                                                                             pui).getPersistenceUnitUtil());
        abd.addBean().types(CriteriaBuilder.class).addQualifiers(qualfiiers).scope(scope).produceWith((instance) -> jpaComponent.getEntityManagerFactory(j2eeName,
                                                                                                                                                         pui).getCriteriaBuilder());
        abd.addBean().types(Cache.class).addQualifiers(qualfiiers).scope(scope).produceWith((instance) -> jpaComponent.getEntityManagerFactory(j2eeName,
                                                                                                                                               pui).getCache());
        abd.addBean().types(Metamodel.class).addQualifiers(qualfiiers).scope(scope).produceWith((instance) -> jpaComponent.getEntityManagerFactory(j2eeName,
                                                                                                                                                   pui).getMetamodel());
        abd.addBean().types(SchemaManager.class).addQualifiers(qualfiiers).scope(scope).produceWith((instance) -> jpaComponent.getEntityManagerFactory(j2eeName,
                                                                                                                                                       pui).getSchemaManager());

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            String qualifiersString = qualfiiers.stream().map(Annotation::annotationType).map(Class::getName).collect(Collectors.joining(", "));
            Tr.debug(tc, "Creating beans for a persistence unit (and related) with scope {0} and qualifiers {1}", scope.getName(), qualifiersString);
        }

    }

    private J2EEName getCurrentAppJ2EEName() {

        ComponentMetaData cmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
        if (cmd != null) {
            J2EEName j2eeName = cmd.getJ2EEName();
            return new AppOnlyJ2EEName(j2eeName.getApplication());
        }
        return null;
    }

    private Class<? extends Annotation> getScope(PersistenceUnitInfo pui) throws ClassNotFoundException {
        String scopeName = pui.getScopeAnnotationName();

        Class<? extends Annotation> scope;
        if (scopeName == null || scopeName.equals("")) {
            scope = jakarta.transaction.TransactionScoped.class;
        } else {
            //The TCCL will be set by CDIRuntimeImpl to the app classloader.
            scope = Class.forName(scopeName, false, Thread.currentThread().getContextClassLoader()).asSubclass(Annotation.class);
        }

        return scope;
    }

    private Set<Annotation> getQualifiers(PersistenceUnitInfo pui) throws ClassNotFoundException {
        List<String> qualifierNames = pui.getQualifierAnnotationNames();
        Set<Annotation> qualfiiers = new HashSet<Annotation>();
        for (String qualifierName : qualifierNames) {

            final Class<? extends Annotation> qualifierClass = Class.forName(qualifierName, false, Thread.currentThread().getContextClassLoader()).asSubclass(Annotation.class);
            @SuppressWarnings("rawtypes")
            Annotation qualifierAnnotationLiteral = new AnnotationLiteral() {
                @Override
                public Class<? extends Annotation> annotationType() {
                    return qualifierClass;
                }
            };
            qualfiiers.add(qualifierAnnotationLiteral);
        }
        return qualfiiers;
    }

    //TODO ask JPA if they object to adding appOnly to jpaComponent.getPersistenceUnits (or overloading the method)
    private static class AppOnlyJ2EEName implements J2EEName {

        private final String appName;

        public AppOnlyJ2EEName(String appName) {
            this.appName = appName;
        }

        @Override
        public String getApplication() {
            return appName;
        }

        @Override
        public byte[] getBytes() {
            return null;
        }

        @Override
        public String getComponent() {
            return null;
        }

        @Override
        public String getModule() {
            return null;
        }

        //For pretty trace output
        @Override
        public String toString() {
            return "AppOnlyJ2EEName[" + appName + "]";
        }
    }
}
