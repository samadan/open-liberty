/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package io.openliberty.springboot.support.version30.test.jms.app;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jndi.JndiTemplate;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.Queue;


@EnableJms
@Configuration
public class JmsConfig {
    @Bean
    public ConnectionFactory connectionFactory() throws NamingException{
        return (ConnectionFactory) new JndiTemplate().lookup("jms/CF1");
    }
	
    @Bean
    public JmsListenerContainerFactory<?> myListenerContainerFactory(ConnectionFactory connectionFactory) throws Exception {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
	    factory.setConnectionFactory(connectionFactory);
        return factory;
    }
	 
    @Bean
    public JmsTemplate jmsTemplate(ConnectionFactory connectionFactory) {
        return new JmsTemplate(connectionFactory);
    }

    @Bean(name = "Q1")
    public Queue queue() throws NamingException {
        return (Queue) new JndiTemplate().lookup("jms/Q1");
    }
}
