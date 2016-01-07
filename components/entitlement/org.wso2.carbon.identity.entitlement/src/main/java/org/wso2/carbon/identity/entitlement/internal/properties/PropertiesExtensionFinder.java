package org.wso2.carbon.identity.entitlement.internal.properties;

import java.util.Properties;

import org.wso2.carbon.identity.entitlement.internal.ExtensionConfigurer;
import org.wso2.carbon.identity.entitlement.internal.ExtensionConfigurerException;
import org.wso2.carbon.identity.entitlement.internal.ExtensionFinder;
import org.wso2.carbon.identity.entitlement.internal.ExtensionFinderException;

/**
 * Creates and a collection of objects from the given properties and a given
 * property prefix. Objects are defined in properties using the following
 * format:<br>
 * <br>
 * <em>propertyPrefix</em>.<em>n</em>.className=<em>className</em><br>
 * <br>
 * Where the <em>propertyPrefix</em> is the configured propertyPrefix,
 * <em>n</em> is an integer starting with 1 that indicates position in the
 * collection (must be defined consecutively), and <em>className</em> is the
 * fully qualified class name to instantiate and configure. The class must have
 * a public no-args constructor.<br>
 * <br>
 * Instantiated objects are configured using an implementation of
 * {@link ExtensionConfigurer}. The properties passed to this object are
 * discovered in the properties using the following format:<br>
 * <br>
 * <em>propertyPrefix</em>.<em>n</em>.config.<em>childPropertyName</em>=
 * <em>childPropertyValue</em><br>
 * <br>
 * Where the <em>propertyPrefix</em> is described above, <em>n</em> is the same
 * index as one of class names defined to instantiate above,
 * <em>childPropertyName</em> is the key of a property that will be passed to
 * the {@link ExtensionConfigurer} implementation and
 * <em>childPropertyValue</em> is the value.<br>
 * <br>
 * <br>
 * Example:<br>
 * <br>
 * 
 * <pre>
 * {@code
 * # Configures two attribute finders to be instantiated
 * PropertyExtensionFinder.1=com.foo.bar.FooAttributeFinder
 * PropertyExtensionFinder.2=com.foo.bar.BarAttributeFinder
 * 
 * # Configure the first attribute finder
 * PropertyExtensionFinder.1.config.myFooProperty=myFooValue
 * PropertyExtensionFinder.1.config.myFooProperty2=myOtherFooValue
 * 
 * # Configure the second attribute finder
 * PropertyExtensionFinder.2.config.myBarProperty=bar
 * }
 * </pre>
 */
public class PropertiesExtensionFinder implements ExtensionFinder {

	private Properties properties;
	private String propertyPrefix = "PropertyExtensionFinder";

	@Override
	public void init(Properties properties) {
		this.properties = properties;
	}

	@Override
	public void findExtensions(ExtensionConfigurer configurer) throws ExtensionFinderException {
		int i = 1;

		while (properties.getProperty(String.format("%s.%d", propertyPrefix, i)) != null) {
			String className = properties.getProperty(String.format("%s.%d", propertyPrefix, i));
			try {
				Class<?> clazz = Thread.currentThread().getContextClassLoader().loadClass(className);
				Object child = clazz.newInstance();

				Properties childProps = new Properties();
				String childPropPrefix = String.format("%s.%d.config.", propertyPrefix, i);
				for (Object key : properties.keySet()) {
					if (key instanceof String) {
						String keyStr = (String) key;
						if (keyStr.startsWith(childPropPrefix)) {
							String childPropertyKey = keyStr.substring(childPropPrefix.length());
							childProps.put(childPropertyKey, properties.get(key));
						}
					}
				}

				if (configurer != null) {
					configurer.configureExtension(child, childProps);
				}
			} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
					| ExtensionConfigurerException e) {
				throw new ExtensionFinderException(e);
			}
			i++;
		}
	}

	public void setPropertyPrefix(String propertyPrefix) {
		this.propertyPrefix = propertyPrefix;
	}
}
