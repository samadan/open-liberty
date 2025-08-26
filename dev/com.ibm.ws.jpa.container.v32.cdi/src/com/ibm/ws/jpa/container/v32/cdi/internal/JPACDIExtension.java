package com.ibm.ws.jpa.container.v32.cdi.internal;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cdi.CDIService;
import com.ibm.ws.cdi.internal.interfaces.CDIRuntime;
import com.ibm.ws.jpa.JPAAccessor;
import com.ibm.ws.jpa.JPAComponent;
import com.ibm.ws.kernel.service.util.ServiceCaller;

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

    @SuppressWarnings("rawtypes")
    private static final ServiceCaller<CDIService> CDI_SERVICE;
    private static final CDIRuntime CDI_RUNTIME;

    //TODO clean this up a bit, ok a lot, discuss if we should add something to the interface
    //Think about getting the J2EEName from the ComponentMetaDataAccessor
    static {
        CDI_SERVICE = new ServiceCaller<CDIService>(JPACDIExtension.class, CDIService.class);
        CDI_RUNTIME = (CDIRuntime) CDI_SERVICE.current().get();
    }

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
    }

    //TODO while this should always work since CDIRuntime should always have a CBE with a j2EEName for
    //extensions, the API doesn't guarantee it so throw up some defences.
    private J2EEName getCurrentAppJ2EEName() {
        J2EEName j2eeName = CDI_RUNTIME.cloneActiveContextBeginnerEnder().getJ2EEName().get();
        return new AppOnlyJ2EEName(j2eeName.getApplication());
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

    //TODO its probably better. A LOT better. To add a method to the jpaComponent that takes a J2EEName but ignores the non-app parts than to do this.
    //Ask JPA if they can add one.
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
    }
}
