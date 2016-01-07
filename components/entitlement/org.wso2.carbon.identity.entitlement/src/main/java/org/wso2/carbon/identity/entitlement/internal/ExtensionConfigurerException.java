package org.wso2.carbon.identity.entitlement.internal;

public class ExtensionConfigurerException extends Exception {

	private static final long serialVersionUID = 1L;

	public ExtensionConfigurerException(String message) {
		super(message);
	}

	public ExtensionConfigurerException(Throwable cause) {
		super(cause);
	}

	public ExtensionConfigurerException(String message, Throwable cause) {
		super(message, cause);
	}
}
