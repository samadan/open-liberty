package com.ibm.ws.jpa.container.v32.cdi.internal;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cdi.internal.interfaces.CDIRuntime;
import com.ibm.ws.cdi.internal.interfaces.ContextBeginnerEnder;
import com.ibm.ws.jpa.JPAAccessor;
import com.ibm.ws.jpa.JPAComponent;
import com.ibm.ws.kernel.service.util.ServiceCaller;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.persistence.EntityManager;
import jakarta.persistence.spi.PersistenceUnitInfo;

public class JPACDIExtension implements Extension {

    private static final TraceComponent tc = Tr.register(JPACDIExtension.class);

    private final JPAComponent jpaComponent = JPAAccessor.getJPAComponent();

    private J2EEName getCurrentAppJ2EEName() {
        Optional<ContextBeginnerEnder> cbe = ServiceCaller.runOnce(JPACDIExtension.class, CDIRuntime.class, CDIRuntime::cloneActiveContextBeginnerEnder);
        Optional<J2EEName> maybeJ2EEName = cbe.flatMap(ContextBeginnerEnder::getJ2EEName);

        if (!maybeJ2EEName.isPresent()) {
            //This should be impossible, in theory the ContextBeginnerEnder API allows it since its a builder pattern and doesn't check
            //we have a component metadata, but its only used by CDIRuntimeImpl and that always gets a CMD
            throw new IllegalStateException("There was no active CDI Context, no Persistence Units will be available for injection");
        }

        J2EEName originalJ2EEName = maybeJ2EEName.get();
        return new AppOnlyJ2EEName(originalJ2EEName.getApplication());
    }

    public void afterBeanDiscovery(@Observes AfterBeanDiscovery abd, BeanManager bm) {

        J2EEName j2EEName = getCurrentAppJ2EEName();
        List<PersistenceUnitInfo> persistenceUnits = jpaComponent.getPersistenceUnits(j2EEName);

        for (PersistenceUnitInfo pui : persistenceUnits) {
            try {
                createBeanForPersistenceUnit(abd, pui);
            } catch (ClassNotFoundException e) {
                //TODO we'll probably want a better message and NLS translation
                Tr.warning(tc, "Could not create bean for PersistenceUnit {0}, it will not be injectable", pui.getPersistenceUnitName());
            }
        }
    }

    private void createBeanForPersistenceUnit(AfterBeanDiscovery abd, PersistenceUnitInfo pui) throws ClassNotFoundException {
        Set<Annotation> qualfiiers = getQualifiers(pui);
        Class<? extends Annotation> scope = getScope(pui);

        abd.addBean().types(EntityManager.class).addQualifiers(qualfiiers).scope(scope);
    }

    private Class<? extends Annotation> getScope(PersistenceUnitInfo pui) throws ClassNotFoundException {
        String scopeName = pui.getScopeAnnotationName();

        Class<? extends Annotation> scope;
        if (scopeName == null) {
            scope = jakarta.transaction.TransactionScoped.class;
        } else {
            scope = Class.forName(scopeName).asSubclass(Annotation.class);
        }

        return scope;
    }

    private Set<Annotation> getQualifiers(PersistenceUnitInfo pui) throws ClassNotFoundException {
        List<String> qualifierNames = pui.getQualifierAnnotationNames();
        Set<Annotation> qualfiiers = new HashSet<Annotation>();
        for (String qualifierName : qualifierNames) {
            final Class<? extends Annotation> qualifierClass = Class.forName(qualifierName).asSubclass(Annotation.class);
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
