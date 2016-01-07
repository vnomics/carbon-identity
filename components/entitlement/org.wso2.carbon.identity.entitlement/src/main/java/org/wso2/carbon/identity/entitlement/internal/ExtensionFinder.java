package org.wso2.carbon.identity.entitlement.internal;

import java.util.Properties;

/**
 * Describes an object that finds entitlement extensions
 * 
 * @author Sam Nelson
 *
 */
public interface ExtensionFinder {
	/**
	 * Initializes the extension finder with the given properties
	 * 
	 * @param properties
	 *            The properties to configure the extension finder with
	 */
	void init(Properties properties);

	/**
	 * Finds entitlement extensions and configures each separately using the
	 * given configurer
	 * 
	 * @param configurer
	 *            The object that will configure extensions that are found
	 * @throws ExtensionFinderException
	 */
	void findExtensions(ExtensionConfigurer configurer) throws ExtensionFinderException;
}
