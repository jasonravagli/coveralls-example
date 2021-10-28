package it.jasonravagli.gym.logic;

public class TransactionException extends RuntimeException {

	private static final long serialVersionUID = -4464621576097610116L;
	
	public TransactionException(String message) {
		super(message);
	}
	
	public TransactionException(String message, Throwable cause) {
		super(message, cause);
	}
	
}
