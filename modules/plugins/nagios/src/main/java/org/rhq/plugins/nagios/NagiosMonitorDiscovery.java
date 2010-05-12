package org.rhq.plugins.nagios;
/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

import java.util.HashSet;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ManualAddFacet;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.plugins.nagios.network.NetworkConnection;
import org.rhq.plugins.nagios.reply.LqlReply;
import org.rhq.plugins.nagios.request.LqlResourceTypeRequest;


/**
 * Discovery class
 *
 *@author Alexander Kiefer
 */
public class NagiosMonitorDiscovery implements ResourceDiscoveryComponent, ManualAddFacet
{
    private final Log log = LogFactory.getLog(this.getClass());
  
    /**
    * Support manually adding this resource type via Platform's inventory tab
    * @param configuration
    * @param resourceDiscoveryContext
    * @return
    * @throws org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException
     */
   public DiscoveredResourceDetails discoverResource(Configuration configuration, ResourceDiscoveryContext resourceDiscoveryContext) throws InvalidPluginConfigurationException {

	   String nagiosHost = configuration.getSimpleValue("nagiosHost",NagiosMonitorComponent.DEFAULT_NAGIOSIP);
	   String nagiosPort = configuration.getSimpleValue("nagiosPort",NagiosMonitorComponent.DEFAULT_NAGIOSPORT);

        DiscoveredResourceDetails detail = new DiscoveredResourceDetails
        (
        		resourceDiscoveryContext.getResourceType(),
        		"nagios@"+nagiosHost+":"+nagiosPort,
        		"Nagios@"+nagiosHost+":"+nagiosPort,
        		null,
        		"Nagios server @ " + nagiosHost + ":" + nagiosPort,
        		configuration,
        		null
        );

      return detail;
   }

    /**
     * Don't run the auto-discovery of this "nagios" server type,
     * as we probably won't have one on each platform. Rather have the admin
     * explicitly add it to one platform.
     */
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext discoveryContext) throws Exception {
    	Set<DiscoveredResourceDetails> discoveredResources = new HashSet<DiscoveredResourceDetails>();
    	    	
       	//Method requests available nagios services an returns the names of them
       	LqlReply resourceTypeReply = getResourceTypeInformation(NagiosMonitorComponent.DEFAULT_NAGIOSIP, Integer.parseInt(NagiosMonitorComponent.DEFAULT_NAGIOSPORT));
    		
       	//for each available service
		for(int i = 0; i < resourceTypeReply.getLqlReply().size(); i++)
		{
			//create new DiscoveredResourceDetails instance
			DiscoveredResourceDetails detail = new DiscoveredResourceDetails 
			(
				//new ResourceType instance per service
				new ResourceType(resourceTypeReply.getLqlReply().get(i),"NagiosMonitor", ResourceCategory.SERVICE, null), 
				"nagiosKey@" + "Nr:" + i + ":" + resourceTypeReply.getLqlReply().get(i),
				"Nagios@" + "Nr:" + i + ":" + resourceTypeReply.getLqlReply().get(i),
	            null,
	            "NagiosService: " + resourceTypeReply.getLqlReply().get(i),
	            null,
	            null
			);
			
			//add DiscoveredResourceDetails instance to Set
			discoveredResources.add(detail);
		}
   	
       return discoveredResources;
   }

    /**
     * Don't run the auto-discovery of this "nagios" server type,
     * as we probably won't have one on each platform. Rather have the admin
     * explicitly add it to one platform.
     */
   
    private LqlReply getResourceTypeInformation(String nagiosIp, int nagiosPort)
    {
    	LqlResourceTypeRequest resourceTypeRequest = new LqlResourceTypeRequest();
		LqlReply resourceTypeReply = new LqlReply(resourceTypeRequest.getRequestType());
		
		NetworkConnection connection = new NetworkConnection(nagiosIp, nagiosPort);
		resourceTypeReply = connection.sendAndReceive(resourceTypeRequest);
    	
		return resourceTypeReply;
    }

}