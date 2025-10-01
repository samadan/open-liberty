/*******************************************************************************
 * Copyright (c) 2003, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.jfapchannel.richclient.framework.impl;

import static com.ibm.ws.messaging.lifecycle.SingletonsReady.requireService;

import com.ibm.websphere.channelfw.CFEndPoint;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.jfapchannel.framework.FrameworkException;
import com.ibm.ws.sib.jfapchannel.framework.NetworkConnectionFactory;
import com.ibm.ws.sib.jfapchannel.framework.NetworkTransportFactory;
import com.ibm.ws.sib.jfapchannel.impl.CommsClientServiceFacade;
import com.ibm.ws.sib.jfapchannel.impl.CommsOutboundChain;
import com.ibm.ws.sib.jfapchannel.netty.NettyNetworkConnectionFactory;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.channelfw.ChannelFramework;
import com.ibm.wsspi.channelfw.VirtualConnectionFactory;
import com.ibm.wsspi.channelfw.exception.InvalidChainNameException;

import io.openliberty.netty.internal.NettyFramework;

/**
 * An implementation of com.ibm.ws.sib.jfapchannel.framework.NetworkTransportFactory. It is the
 * top level class that provides access to the NetworkConnectionFactory classes from which
 * connections can be created.
 * 
 * @see com.ibm.ws.sib.jfapchannel.framework.NetworkTransportFactory
 * 
 * @author Gareth Matthews
 */
public class RichClientTransportFactory implements NetworkTransportFactory
{
    /** Trace */
    private static final TraceComponent tc = SibTr.register(RichClientTransportFactory.class,
                                                            JFapChannelConstants.MSG_GROUP,
                                                            JFapChannelConstants.MSG_BUNDLE);

    /** Class name for FFDC's */
    private static final String CLASS_NAME = RichClientTransportFactory.class.getName();

    /** Log class info on load */
    static
    {
        if (tc.isDebugEnabled())
            SibTr.debug(tc,
                        "@(#) SIB/ws/code/sib.jfapchannel.client.rich.impl/src/com/ibm/ws/sib/jfapchannel/framework/impl/RichClientTransportFactory.java, SIB.comms, WASX.SIB, uu1215.01 1.1");
    }


    /**
     * Channel Framework constructor.
     * 
     * @param channelFramework
     */
    public RichClientTransportFactory()
    {
        if (tc.isEntryEnabled())
            SibTr.entry(this, tc, "<init>");
        if (tc.isEntryEnabled())
            SibTr.exit(tc, "<init>");
    }
    

    /**
     * @see com.ibm.ws.sib.jfapchannel.framework.NetworkTransportFactory#getOutboundNetworkConnectionFactoryByName(java.lang.String)
     */
    @Override
    public NetworkConnectionFactory getOutboundNetworkConnectionFactoryByName(String chainName) throws FrameworkException
    {
        if (tc.isEntryEnabled())
            SibTr.entry(this, tc, "getOutboundNetworkConnectionFactoryByName", chainName);

        NetworkConnectionFactory connFactory = null;

        try
        {
            // Get the virtual connection factory from the channel framework using the chain name
        	CommsOutboundChain chain = CommsOutboundChain.getChainDetails(chainName);
        	
        	if(chain != null) {
        		if(chain.useNetty()) {
        			// If Netty, create new Netty channel factory
        			// TODO: Consult with team. Getting error difference cause channelfw fails for testSSLFeatureUpdate
        			// in com.ibm.ws.messaging.open_comms_fat because SSL chain failed to init properly due to no SSL Options
        			// Check what to do here appropriately see https://github.com/OpenLiberty/open-liberty/issues/24823
        			boolean usingSSL = chain.isSecureChain();
        			if(usingSSL && chain.getSslOptions() == null)
        				throw new InvalidChainNameException("Chain configuration not found in framework, " + chainName);
                    connFactory = new NettyNetworkConnectionFactory(chainName, chain.getTcpOptions(), usingSSL ? chain.getSslOptions() : null, usingSSL ? chain.getNettyTlsProvider() : null);
        		}else {
        			VirtualConnectionFactory vcFactory = requireService(CommsClientServiceFacade.class).getChannelFramework().getOutboundVCFactory(chainName);
        			connFactory = new CFWNetworkConnectionFactory(vcFactory);
        		}
        	}

        } catch (com.ibm.wsspi.channelfw.exception.ChannelException e) {

            FFDCFilter.processException(e, CLASS_NAME + ".getOutboundNetworkConnectionFactoryByName",
                                        JFapChannelConstants.RICHCLIENTTRANSPORTFACT_GETCONNFACBYNAME_01,
                                        new Object[] { this, chainName });

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "Failure to obtain OVC for Outbound chain" + chainName, e);

            throw new FrameworkException(e);
        } catch (com.ibm.wsspi.channelfw.exception.ChainException e) {

            FFDCFilter.processException(e, CLASS_NAME + ".getOutboundNetworkConnectionFactoryByName",
                                        JFapChannelConstants.RICHCLIENTTRANSPORTFACT_GETCONNFACBYNAME_01,
                                        new Object[] { this, chainName });
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "Failure to obtain OVC for Outbound chain" + chainName, e);

            throw new FrameworkException(e);
        }

        if (tc.isEntryEnabled())
            SibTr.exit(this, tc, "getOutboundNetworkConnectionFactoryByName", connFactory);
        return connFactory;
    }

    /**
     * NO OP -- Traditional WebSphere behaivor not ported to Liberty.  See OLGH22692
     */
    @Override
    public NetworkConnectionFactory getOutboundNetworkConnectionFactoryFromEndPoint(Object endPoint)
    {
        if (tc.isDebugEnabled())
            SibTr.warning(tc, "Within RichClientTransportFactory.java#getOutboundNetworkConnectionFactoryFromEndPoint -- not supported");
            
        return null;
    }

}
