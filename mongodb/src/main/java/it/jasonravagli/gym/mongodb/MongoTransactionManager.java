package it.jasonravagli.gym.mongodb;

import com.mongodb.MongoException;
import com.mongodb.client.ClientSession;

import it.jasonravagli.gym.logic.TransactionCode;
import it.jasonravagli.gym.logic.TransactionException;
import it.jasonravagli.gym.logic.TransactionManager;

public class MongoTransactionManager implements TransactionManager {

	private ClientSession clientSession;
	private MongoRepositoryProvider repositoryProvider;

	public MongoTransactionManager(ClientSession clientSession, MongoRepositoryProvider repositoryProvider) {
		this.clientSession = clientSession;
		this.repositoryProvider = repositoryProvider;
	}

	@Override
	public <T> T doInTransaction(TransactionCode<T> code) throws TransactionException {
		try {
			return clientSession.withTransaction(() -> code.apply(repositoryProvider));
		} catch (MongoException e) {
			throw new TransactionException(e.getMessage(), e);
		}
	}

}
