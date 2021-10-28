package it.jasonravagli.gym.logic;

public interface TransactionManager {

	<T> T doInTransaction(TransactionCode<T> code) throws TransactionException;

}
