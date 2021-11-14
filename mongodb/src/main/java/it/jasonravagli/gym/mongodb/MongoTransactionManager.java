package it.jasonravagli.gym.mongodb;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mongodb.client.ClientSession;

import it.jasonravagli.gym.logic.TransactionCode;
import it.jasonravagli.gym.logic.TransactionException;
import it.jasonravagli.gym.logic.TransactionManager;

public class MongoTransactionManager implements TransactionManager {

	private static final Logger LOGGER = LogManager.getLogger(MongoTransactionManager.class);

	private ClientSession clientSession;
	private MongoRepositoryProvider repositoryProvider;

	public MongoTransactionManager(ClientSession clientSession, MongoRepositoryProvider repositoryProvider) {
		this.clientSession = clientSession;
		this.repositoryProvider = repositoryProvider;
	}

	@Override
	public <T> T doInTransaction(TransactionCode<T> code) throws TransactionException {
		try {
			clientSession.startTransaction();
			T result = code.apply(repositoryProvider);
			clientSession.commitTransaction();

			return result;
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
			clientSession.abortTransaction();
			throw new TransactionException(e.getMessage(), e);
		}
	}

}
