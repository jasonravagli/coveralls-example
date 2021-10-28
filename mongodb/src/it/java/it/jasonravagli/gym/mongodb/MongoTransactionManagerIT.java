package it.jasonravagli.gym.mongodb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.bson.Document;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.mongodb.MongoClient;
import com.mongodb.MongoException;
import com.mongodb.ServerAddress;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import it.jasonravagli.gym.logic.RepositoryProvider;
import it.jasonravagli.gym.logic.TransactionCode;
import it.jasonravagli.gym.logic.TransactionException;

public class MongoTransactionManagerIT {

	private static final String MONGO_HOST = "localhost";
	private static final String MONGO_DATABASE = "test";
	private static final String MONGO_COLLECTION = "test_collection";

	// Get the docker container mapped port
	private static int mongoPort = Integer.parseInt(System.getProperty("mongo.port", "27017"));

	private AutoCloseable autoCloseable;

	private MongoClient client;
	private ClientSession clientSession;
	private MongoCollection<Document> testCollection;

	@Mock
	private MongoRepositoryProvider provider;

	private MongoTransactionManager transactionManager;

	@Before
	public void setup() {
		autoCloseable = MockitoAnnotations.openMocks(this);

		client = new MongoClient(new ServerAddress(MONGO_HOST, mongoPort));
		clientSession = client.startSession();
		MongoDatabase database = client.getDatabase(MONGO_DATABASE);
		// Clean the database
		database.drop();
		testCollection = database.getCollection(MONGO_COLLECTION);

		transactionManager = new MongoTransactionManager(clientSession, provider);
	}

	@After
	public void tearDown() throws Exception {
		client.close();
		autoCloseable.close();
	}

	@Test
	public void testDoInTransactionWhenEverythingOkShouldExecuteCode() {
		Document doc = new Document("name", "test");
		Integer returnValue = 10;
		TransactionCode<Integer> code = repositoryProvider -> {
			testCollection.insertOne(doc);
			return returnValue;
		};

		Integer result = transactionManager.doInTransaction(code);

		assertThat(result).isEqualTo(returnValue);
		assertThat(readAllDocumentsFromDatabase()).containsExactly(doc);
	}

	@Test
	public void testDoInTransactionShouldProvideRepositoryProvider() {
		TransactionCode<RepositoryProvider> code = repositoryProvider -> {
			return repositoryProvider;
		};

		RepositoryProvider usedProvider = transactionManager.doInTransaction(code);

		assertThat(usedProvider).isEqualTo(provider);
	}

	@Test
	public void testDoInTransactionWhenCodeThrowMongoExceptionShouldCatchAndThrowCustomException() {
		String exceptionMessage = "Exception thrown";
		MongoException exception = new MongoException(exceptionMessage);
		TransactionCode<Void> code = repositoryProvider -> {
			throw exception;
		};

		assertThatThrownBy(() -> transactionManager.doInTransaction(code)).isInstanceOf(TransactionException.class)
				.hasMessage(exceptionMessage).hasCause(exception);
	}

	@Test
	public void testDoInTransactionShouldEnsureAtomicity() {
		Document doc = new Document("name", "test");
		TransactionCode<Void> code = repositoryProvider -> {
			testCollection.insertOne(clientSession, doc);
			throw new MongoException("Exception thrown");
		};

		try {
			transactionManager.doInTransaction(code);
		} catch (TransactionException e) {

		}

		assertThat(readAllDocumentsFromDatabase()).isEmpty();
	}

	private List<Document> readAllDocumentsFromDatabase() {
		return StreamSupport.stream(testCollection.find().spliterator(), false).collect(Collectors.toList());
	}
}
