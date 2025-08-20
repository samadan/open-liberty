package com.ibm.ws.jpa.container.v32.cdi.internal;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jpa.JPAAccessor;
import com.ibm.ws.jpa.JPAComponent;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.Extension;

public class JPACDIExtension implements Extension {

    private static final TraceComponent tc = Tr.register(JPACDIExtension.class);

    private final JPAComponent jpaComponent = JPAAccessor.getJPAComponent();

    public void afterBeanDiscovery(@Observes AfterBeanDiscovery abd, BeanManager bm) {

    }

}
