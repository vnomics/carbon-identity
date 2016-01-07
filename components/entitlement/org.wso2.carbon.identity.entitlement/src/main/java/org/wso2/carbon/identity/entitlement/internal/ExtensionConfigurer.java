package org.wso2.carbon.identity.entitlement.internal;

import java.util.Properties;

public interface ExtensionConfigurer {
	void configureExtension(Object extension, Properties extensionProperties) throws ExtensionConfigurerException;
}
