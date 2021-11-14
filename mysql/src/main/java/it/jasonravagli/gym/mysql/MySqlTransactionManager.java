package it.jasonravagli.gym.mysql;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.jasonravagli.gym.logic.TransactionCode;
import it.jasonravagli.gym.logic.TransactionException;
import it.jasonravagli.gym.logic.TransactionManager;

public class MySqlTransactionManager implements TransactionManager {
	
	private static final Logger LOGGER = LogManager.getLogger(MySqlTransactionManager.class);
	
	private MySqlRepositoryProvider repositoryProvider;
	private Connection connection;
	
	public MySqlTransactionManager(Connection connection, MySqlRepositoryProvider repositoryProvider) {
		this.connection = connection;
		this.repositoryProvider = repositoryProvider;
	}

	@Override
	public <T> T doInTransaction(TransactionCode<T> code) throws TransactionException {
		Savepoint savepoint = null;
		try {
			connection.setAutoCommit(false);
			savepoint = connection.setSavepoint();
			
			T result = code.apply(repositoryProvider);
			
			connection.commit();
			connection.setAutoCommit(true);
			return result;
		} catch (Exception e) {
			try {
				connection.rollback(savepoint);
				connection.setAutoCommit(true);
			} catch (SQLException rollbackEx) {
				LOGGER.error(rollbackEx.getMessage());
				throw new TransactionException(rollbackEx.getMessage());
			}
			LOGGER.error(e.getMessage());
			throw new TransactionException(e.getMessage());
		}
	}

}
