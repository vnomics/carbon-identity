package org.wso2.carbon.identity.entitlement.internal;

public class ExtensionFinderException extends Exception {

	private static final long serialVersionUID = 1L;

	public ExtensionFinderException(String message) {
		super(message);
	}

	public ExtensionFinderException(Throwable cause) {
		super(cause);
	}

	public ExtensionFinderException(String message, Throwable cause) {
		super(message, cause);
	}
}
