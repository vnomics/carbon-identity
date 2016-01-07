/*
 *  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.entitlement.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.entitlement.PAPStatusDataHandler;
import org.wso2.carbon.identity.entitlement.PDPConstants;
import org.wso2.carbon.identity.entitlement.internal.properties.PropertiesExtensionFinder;
import org.wso2.carbon.identity.entitlement.pap.EntitlementDataFinderModule;
import org.wso2.carbon.identity.entitlement.pip.PIPAttributeFinder;
import org.wso2.carbon.identity.entitlement.pip.PIPExtension;
import org.wso2.carbon.identity.entitlement.pip.PIPResourceFinder;
import org.wso2.carbon.identity.entitlement.policy.collection.PolicyCollection;
import org.wso2.carbon.identity.entitlement.policy.finder.PolicyFinderModule;
import org.wso2.carbon.identity.entitlement.policy.publisher.PolicyPublisherModule;
import org.wso2.carbon.identity.entitlement.policy.publisher.PostPublisherModule;
import org.wso2.carbon.identity.entitlement.policy.publisher.PublisherVerificationModule;
import org.wso2.carbon.identity.entitlement.policy.store.PolicyDataStore;
import org.wso2.carbon.identity.entitlement.policy.store.PolicyStoreManageModule;
import org.wso2.carbon.identity.entitlement.policy.version.PolicyVersionManager;

/**
 * Build Entitlement configuration from entitlement.properties. First this will
 * try to find the configuration file from [CARBON_HOME]\repository\conf -
 * failing to do so will load the file from this bundle it self.The default file
 * ships with the bundle only includes
 * org.wso2.carbon.identity.entitlement.pip.DefaultAttributeFinder as an
 * AttributeDesignator and default caching configurations.
 * <p/>
 * <p/>
 * PDP.OnDemangPolicyLoading.Enable=false
 * PDP.OnDemangPolicyLoading.MaxInMemoryPolicies=1000
 * PDP.DecisionCaching.Enable=true PDP.DecisionCaching.CachingInterval=30000
 * PDP.AttributeCaching.Enable=true PDP.DecisionCaching.CachingInterval=30000
 * PDP.ResourceCaching.Enable=true PDP.ResourceCaching.CachingInterval=30000
 * <p/>
 * PDP.Extensions.Extension.1=org.wso2.carbon.identity.entitlement.pdp.
 * DefaultExtension
 * <p/>
 * PIP.AttributeDesignators.Designator.1=org.wso2.carbon.identity.entitlement.
 * pip.DefaultAttributeFinder
 * PIP.ResourceFinders.Finder.1="org.wso2.carbon.identity.entitlement.pip.
 * DefaultResourceFinder
 * <p/>
 * PAP.MetaDataFinders.Finder.1=org.wso2.carbon.identity.entitlement.pap.
 * CarbonEntitlementDataFinder
 * PAP.PolicyPublishers.Publisher.1=org.wso2.carbon.identity.entitlement.policy.
 * publisher .CarbonBasicPolicyPublisherModule
 * <p/>
 * # Properties needed for each extension. #
 * org.wso2.carbon.identity.entitlement.pip.DefaultAttributeFinder.1=name,value
 * #
 * org.wso2.carbon.identity.entitlement.pip.DefaultAttributeFinder.2=name,value
 * # org.wso2.carbon.identity.entitlement.pip.DefaultResourceFinder.1=name.value
 * # org.wso2.carbon.identity.entitlement.pip.DefaultResourceFinder.2=name,value
 * #
 * org.wso2.carbon.identity.entitlement.pap.CarbonEntitlementDataFinder.1=name,
 * value #
 * org.wso2.carbon.identity.entitlement.pap.CarbonEntitlementDataFinder.2=name,
 * value
 */
public class EntitlementExtensionBuilder {

	public static final String PDP_SCHEMA_VALIDATION = "PDP.SchemaValidation.Enable";

	private static final String ENTITLEMENT_CONFIG = "entitlement.properties";

	private static final Log log = LogFactory.getLog(EntitlementExtensionBuilder.class);

	private BundleContext bundleContext;
	private ExtensionFinder extensionFinder = new PropertiesExtensionFinder();

	public void setBundleContext(BundleContext bundleContext) {
		this.bundleContext = bundleContext;
	}

	public void buildEntitlementConfig(EntitlementConfigHolder holder) throws Exception {
		log.info("buildEntitlementConfig");
		Properties properties;

		if ((properties = loadProperties()) != null) {
			populateEntitlementAttributes(properties, holder);
			extensionFinder.init(properties);
			findExtensions(properties, holder);
		}
	}

	/**
	 * @return properties
	 * @throws IOException
	 */
	private Properties loadProperties() throws IOException {
		log.info("loadProperties");
		Properties properties = new Properties();
		InputStream inStream = null;
		String warningMessage = null;

		File pipConfigXml = new File(IdentityUtil.getIdentityConfigDirPath(), ENTITLEMENT_CONFIG);

		try {
			if (pipConfigXml.exists()) {
				inStream = new FileInputStream(pipConfigXml);
			} else {
				URL url;
				if (bundleContext != null) {
					if ((url = bundleContext.getBundle().getResource(ENTITLEMENT_CONFIG)) != null) {
						inStream = url.openStream();
					} else {
						warningMessage = "Bundle context could not find resource " + ENTITLEMENT_CONFIG
								+ " or user does not have sufficient permission to access the resource.";
					}

				} else {

					if ((url = this.getClass().getClassLoader().getResource(ENTITLEMENT_CONFIG)) != null) {
						inStream = url.openStream();
					} else {
						warningMessage = "PIP Config Builder could not find resource " + ENTITLEMENT_CONFIG
								+ " or user does not have sufficient permission to access the resource.";
					}
				}
			}

			if (inStream == null) {
				log.warn(warningMessage);
				return null;
			}

			properties.load(inStream);

		} catch (FileNotFoundException e) {
			if (log.isDebugEnabled()) {
				log.debug(e);
			}
			throw e;
		} catch (IOException e) {
			if (log.isDebugEnabled()) {
				log.debug(e);
			}
			throw e;
		} finally {
			try {
				if (inStream != null) {
					inStream.close();
				}
			} catch (Exception e) {
				log.error("Error while closing input stream ", e);
			}
		}

		return properties;
	}

	/**
	 * @param properties
	 *            which are used to populate pdp properties
	 * @param holder
	 *            holder of properties
	 */
	private void populateEntitlementAttributes(Properties properties, EntitlementConfigHolder holder) {
		log.info("populateEntitlementAttributes");
		Properties pdpProperties = new Properties();

		setProperty(properties, pdpProperties, PDPConstants.ON_DEMAND_POLICY_LOADING);
		setProperty(properties, pdpProperties, PDPConstants.ON_DEMAND_POLICY_MAX_POLICY_ENTRIES);
		setProperty(properties, pdpProperties, PDPConstants.DECISION_CACHING);
		setProperty(properties, pdpProperties, PDPConstants.DECISION_CACHING_INTERVAL);
		setProperty(properties, pdpProperties, PDPConstants.ATTRIBUTE_CACHING);
		setProperty(properties, pdpProperties, PDPConstants.ATTRIBUTE_CACHING_INTERVAL);
		setProperty(properties, pdpProperties, PDPConstants.RESOURCE_CACHING);
		setProperty(properties, pdpProperties, PDPConstants.RESOURCE_CACHING_INTERVAL);
		setProperty(properties, pdpProperties, PDPConstants.PDP_ENABLE);
		setProperty(properties, pdpProperties, PDPConstants.PAP_ENABLE);
		setProperty(properties, pdpProperties, PDPConstants.BALANA_CONFIG_ENABLE);
		setProperty(properties, pdpProperties, PDPConstants.MULTIPLE_DECISION_PROFILE_ENABLE);
		setProperty(properties, pdpProperties, PDPConstants.MAX_POLICY_REFERENCE_ENTRIES);
		setProperty(properties, pdpProperties, PDPConstants.FILESYSTEM_POLICY_PATH);
		setProperty(properties, pdpProperties, PDPConstants.POLICY_ID_REGEXP_PATTERN);
		setProperty(properties, pdpProperties, PDPConstants.PDP_GLOBAL_COMBINING_ALGORITHM);
		setProperty(properties, pdpProperties, PDPConstants.ENTITLEMENT_ITEMS_PER_PAGE);
		setProperty(properties, pdpProperties, PDPConstants.START_UP_POLICY_ADDING);
		setProperty(properties, pdpProperties, PDP_SCHEMA_VALIDATION);
		setProperty(properties, pdpProperties, PDPConstants.ENTITLEMENT_ENGINE_CACHING_INTERVAL);
		setProperty(properties, pdpProperties, PDPConstants.PDP_REGISTRY_LEVEL_POLICY_CACHE_CLEAR);
		setProperty(properties, pdpProperties, PDPConstants.POLICY_CACHING_INTERVAL);

		holder.setEngineProperties(pdpProperties);
	}

	private void setProperty(Properties inProp, Properties outProp, String name) {
		String value;
		if ((value = inProp.getProperty(name)) != null) {
			outProp.setProperty(name, value.trim());
		}
	}

	/**
	 * @param properties
	 * @param holder
	 * @throws Exception
	 */
	private void findExtensions(Properties properties, final EntitlementConfigHolder holder) throws Exception {
		extensionFinder.findExtensions(new ExtensionConfigurer() {
			@Override
			public void configureExtension(Object extension, Properties extensionProperties)
					throws ExtensionConfigurerException {
				log.info(String.format("Registering extension: %s", extension.getClass().getName()));
				try {
					if (extension instanceof PIPAttributeFinder) {
						((PIPAttributeFinder) extension).init(extensionProperties);

						holder.addDesignators((PIPAttributeFinder) extension, extensionProperties);
					} else if (extension instanceof PIPResourceFinder) {
						((PIPResourceFinder) extension).init(extensionProperties);
						holder.addResourceFinders((PIPResourceFinder) extension, extensionProperties);
					} else if (extension instanceof PIPExtension) {
						((PIPExtension) extension).init(extensionProperties);
						holder.addExtensions((PIPExtension) extension, extensionProperties);
					} else if (extension instanceof PolicyFinderModule) {
						((PolicyFinderModule) extension).init(extensionProperties);
						if (extension instanceof PolicyStoreManageModule) {
							holder.addPolicyStore((PolicyStoreManageModule) extension, extensionProperties);
						}
						holder.addPolicyFinderModule((PolicyFinderModule) extension, extensionProperties);
					} else if (extension instanceof PolicyCollection) {
						((PolicyCollection) extension).init(extensionProperties);
						holder.addPolicyCollection((PolicyCollection) extension, extensionProperties);
					} else if (extension instanceof PolicyStoreManageModule) {
						((PolicyStoreManageModule) extension).init(extensionProperties);
						holder.addPolicyStore((PolicyStoreManageModule) extension, extensionProperties);
					} else if (extension instanceof PolicyDataStore) {
						((PolicyDataStore) extension).init(extensionProperties);
						holder.addPolicyDataStore((PolicyDataStore) extension, extensionProperties);
					} else if (extension instanceof EntitlementDataFinderModule) {
						((EntitlementDataFinderModule) extension).init(extensionProperties);
						holder.addPolicyEntitlementDataFinder((EntitlementDataFinderModule) extension,
								extensionProperties);
					} else if (extension instanceof PolicyPublisherModule) {
						((PolicyPublisherModule) extension).init(extensionProperties);
						holder.addPolicyPublisherModule((PolicyPublisherModule) extension, extensionProperties);
					} else if (extension instanceof PolicyVersionManager) {
						((PolicyVersionManager) extension).init(extensionProperties);
						holder.addPolicyVersionModule((PolicyVersionManager) extension, extensionProperties);
					} else if (extension instanceof PostPublisherModule) {
						((PostPublisherModule) extension).init(extensionProperties);
						holder.addPolicyPostPublisherModule((PostPublisherModule) extension, extensionProperties);
					} else if (extension instanceof PublisherVerificationModule) {
						((PublisherVerificationModule) extension).init(extensionProperties);
						holder.addPublisherVerificationModule((PublisherVerificationModule) extension,
								extensionProperties);
					} else if (extension instanceof PAPStatusDataHandler) {
						((PAPStatusDataHandler) extension).init(extensionProperties);
						holder.addNotificationHandler((PAPStatusDataHandler) extension, extensionProperties);
					} else {
						log.error("oops", new Exception(
								String.format("Unrecognized entitlement extension of class %s", extension.getClass())));
						throw new Exception(
								String.format("Unrecognized entitlement extension of class %s", extension.getClass()));
					}
				} catch (Exception e) {
					log.error("oops", e);
					throw new ExtensionConfigurerException(e);
				}
			}
		});
	}
}
