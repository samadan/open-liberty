/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2025 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
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
package org.jboss.resteasy.client.jaxrs.internal.proxy;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.RxInvoker;
import jakarta.ws.rs.client.RxInvokerProvider;
import jakarta.ws.rs.client.SyncInvoker;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.container.DynamicFeature;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.client.jaxrs.ProxyConfig;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.client.jaxrs.internal.ClientConfiguration;
import org.jboss.resteasy.client.jaxrs.internal.ClientInvocation;
import org.jboss.resteasy.client.jaxrs.internal.ClientInvocationBuilder;
import org.jboss.resteasy.client.jaxrs.internal.ClientResponse;
import org.jboss.resteasy.client.jaxrs.internal.proxy.extractors.ClientContext;
import org.jboss.resteasy.client.jaxrs.internal.proxy.extractors.DefaultEntityExtractorFactory;
import org.jboss.resteasy.client.jaxrs.internal.proxy.extractors.EntityExtractor;
import org.jboss.resteasy.client.jaxrs.internal.proxy.processors.InvocationProcessor;
import org.jboss.resteasy.client.jaxrs.internal.proxy.processors.ProcessorFactory;
import org.jboss.resteasy.client.jaxrs.internal.proxy.processors.WebTargetProcessor;
import org.jboss.resteasy.util.FeatureContextDelegate;
import org.jboss.resteasy.util.MediaTypeHelper;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ClientInvoker implements MethodInvoker {
    protected String httpMethod;
    protected Method method;
    protected Class<?> declaring;
    protected MediaType[] accepts;
    protected Object[] processors;
    protected ResteasyWebTarget webTarget;
    protected boolean followRedirects;
    @SuppressWarnings("rawtypes")
    protected EntityExtractor extractor;
    protected DefaultEntityExtractorFactory entityExtractorFactory;
    protected ClientConfiguration invokerConfig;
    protected RxInvokerProvider<?> rxInvokerProvider;
    protected SyncInvoker syncInvoker;

    public ClientInvoker(final ResteasyWebTarget parent, final Class<?> declaring, final Method method,
            final ProxyConfig config) {
        // webTarget must be a clone so that it has a cloned ClientConfiguration so we can apply DynamicFeature
        if (method.isAnnotationPresent(Path.class)) {
            this.webTarget = parent.path(method);
        } else {
            this.webTarget = parent.clone();
        }
        this.declaring = declaring;
        this.method = method;
        invokerConfig = (ClientConfiguration) this.webTarget.getConfiguration();
        ResourceInfo info = new ResourceInfo() {
            @Override
            public Method getResourceMethod() {
                return ClientInvoker.this.method;
            }

            @Override
            public Class<?> getResourceClass() {
                return ClientInvoker.this.declaring;
            }
        };
        Set<DynamicFeature> dynamicFeatures = invokerConfig.getDynamicFeatures();
        if (null != dynamicFeatures) {
            for (DynamicFeature feature : dynamicFeatures) {
                feature.configure(info, new FeatureContextDelegate(invokerConfig));
            }
        }

        this.processors = ProcessorFactory.createProcessors(declaring, method, invokerConfig, config.getDefaultConsumes());
        accepts = MediaTypeHelper.getProduces(declaring, method, config.getDefaultProduces());
        entityExtractorFactory = new DefaultEntityExtractorFactory();
        this.extractor = entityExtractorFactory.createExtractor(method);
        rxInvokerProvider = invokerConfig.getRxInvokerProviderFromReactiveClass(method.getReturnType());
    }

    public MediaType[] getAccepts() {
        return accepts;
    }

    public Method getMethod() {
        return method;
    }

    public Class<?> getDeclaring() {
        return declaring;
    }

    public Object invoke(Object[] args) {
        return rxInvokerProvider != null ? invokeAsync(args) : invokeSync(args);
    }

    protected Object invokeAsync(final Object[] args) {
        ClientInvocation request = createRequest(args);
        WebTarget t = request.getActualTarget();
        ClientInvocationBuilder builder = (ClientInvocationBuilder) (t != null ? t : webTarget).request();
        builder.setClientInvocation(request);
        ExecutorService executor = webTarget.getResteasyClient().getScheduledExecutor();
        if (executor == null) {
            executor = request.asyncInvocationExecutor();
        }
        RxInvoker<?> rxInvoker = (RxInvoker<?>) rxInvokerProvider.getRxInvoker(builder, executor);
        Type type = method.getGenericReturnType();
        if (type instanceof ParameterizedType) {
            type = ((ParameterizedType) type).getActualTypeArguments()[0];
        }
        GenericType<?> gt = new GenericType(type);
        Object e = request.getEntity();
        Object o = null;
        if (e != null) {
            //Liberty change: Converted e to GenericEntity to fix the issue: 
            o = rxInvoker.method(getHttpMethod(),
                    Entity.entity(new GenericEntity<Object>(e, request.getEntityGenericType()), 
                    request.getHeaders().getMediaType(), request.getEntityAnnotations()), gt); 
        } else {
            o = rxInvoker.method(getHttpMethod(), gt);
        }
        return o;
    }

    protected Object invokeSync(Object[] args) {
        ClientInvocation request = createRequest(args);
        ClientResponse response = (ClientResponse) request.invoke();
        ClientContext context = new ClientContext(request, response, entityExtractorFactory);
        return extractor.extractEntity(context);
    }

    protected ClientInvocation createRequest(Object[] args) {
        WebTarget target = this.webTarget;
        for (int i = 0; i < processors.length; i++) {
            if (processors != null && processors[i] instanceof WebTargetProcessor) {
                WebTargetProcessor processor = (WebTargetProcessor) processors[i];
                target = processor.build(target, args[i]);

            }
        }
        ClientInvocationBuilder builder = (ClientInvocationBuilder) target.request();
        ClientInvocation clientInvocation = (ClientInvocation) builder.build(httpMethod);

        clientInvocation.setClientInvoker(this);
        if (target != this.webTarget) {
            clientInvocation.setActualTarget(target);
        }
        if (accepts != null) {
            clientInvocation.getHeaders().accept(accepts);
        }
        for (int i = 0; i < processors.length; i++) {
            if (processors != null && processors[i] instanceof InvocationProcessor) {
                InvocationProcessor processor = (InvocationProcessor) processors[i];
                processor.process(clientInvocation, args[i]);

            }
        }
        return clientInvocation;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public boolean isFollowRedirects() {
        return followRedirects;
    }

    public void setFollowRedirects(boolean followRedirects) {
        this.followRedirects = followRedirects;
    }

    public void followRedirects() {
        setFollowRedirects(true);
    }

    public SyncInvoker getSyncInvoker() {
        return syncInvoker;
    }

    public void setSyncInvoker(SyncInvoker syncInvoker) {
        this.syncInvoker = syncInvoker;
    }
}
