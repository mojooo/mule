/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.cxf.wsdl;

import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.transport.cxf.ClientWrapper;
import org.mule.transport.cxf.CxfMessageDispatcher;
import org.mule.util.StringUtils;

import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.dynamic.DynamicClientFactory;

/**
 * TODO document
 */
public class CxfWsdlMessageDispatcher extends CxfMessageDispatcher
{
    private final static Object CLIENT_CREATION_LOCK = new Object();
    
    public CxfWsdlMessageDispatcher(OutboundEndpoint endpoint)
    {
        super(endpoint);
    }

    // @Override
    protected void doConnect() throws Exception
    {
        try
        {
            wrapper = new WsdlClientWrapper(connector.getCxfBus(), endpoint);
        }
        catch (Exception ex)
        {
            disconnect();
            throw ex;
        }
    }

    protected Client createClient(Bus bus, String wsdlUrl, String serviceName, String portName) throws Exception
    {
        synchronized (CLIENT_CREATION_LOCK)
        {
            DynamicClientFactory cf = DynamicClientFactory.newInstance(bus);
            return cf.createClient(wsdlUrl, 
               (serviceName == null ? null : QName.valueOf(serviceName)), 
               (portName == null ? null : QName.valueOf(portName)));
        }
    }

    class WsdlClientWrapper extends ClientWrapper
    {
        public WsdlClientWrapper(Bus bus, ImmutableEndpoint endpoint) throws Exception
        {
            super(endpoint);
            
            String wsdlUrl = endpoint.getEndpointURI().getAddress();
            String serviceName = null;
            String portName = null;

            // If the property specified an alternative WSDL url, use it
            if (endpoint.getProperty("wsdlLocation") != null && StringUtils.isNotBlank(endpoint.getProperty("wsdlLocation").toString()))
            {
                wsdlUrl = (String) endpoint.getProperty("wsdlLocation");
            }
            
            // If the property specified an alternative service, use it
            if (endpoint.getProperty("service") != null && StringUtils.isNotBlank(endpoint.getProperty("service").toString()))
            {
                serviceName = (String) endpoint.getProperty("service");
            }
            
            // If the property specified an alternative port, use it
            if (endpoint.getProperty("port") != null && StringUtils.isNotBlank(endpoint.getProperty("port").toString()))
            {
                portName = (String) endpoint.getProperty("port");
            }
            
            try
            {
                this.client = createClient(bus, wsdlUrl, serviceName, portName);

                addMuleInterceptors();
            }
            catch (Exception ex)
            {
                disconnect();
                throw ex;
            }
        }
    };
}
