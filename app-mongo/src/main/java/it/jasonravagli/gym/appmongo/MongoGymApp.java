package it.jasonravagli.gym.appmongo;

import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;

import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import it.jasonravagli.gym.gui.SwingDialogManageCourse;
import it.jasonravagli.gym.gui.SwingDialogManageMember;
import it.jasonravagli.gym.gui.SwingDialogManageSubs;
import it.jasonravagli.gym.gui.SwingGymView;
import it.jasonravagli.gym.logic.GymController;
import it.jasonravagli.gym.mongodb.MongoCourseRepository;
import it.jasonravagli.gym.mongodb.MongoMemberRepository;
import it.jasonravagli.gym.mongodb.MongoRepositoryProvider;
import it.jasonravagli.gym.mongodb.MongoTransactionManager;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(mixinStandardHelpOptions = true)
public class MongoGymApp implements Callable<Void> {

	private static final Logger LOGGER = LogManager.getLogger(MongoGymApp.class);
	private static final String MONGO_MEMBER_COLLECTION = "members";
	private static final String MONGO_COURSE_COLLECTION = "courses";

	@Option(names = { "--mongo-host" }, description = "MongoDB host address")
	private String mongoHost = "localhost";

	@Option(names = { "--mongo-port" }, description = "MongoDB host port")
	private int mongoPort = 27017;

	@Option(names = { "--db-name" }, description = "Database name")
	private String databaseName = "gym";

	public static void main(String[] args) {
		new CommandLine(new MongoGymApp()).execute(args);
	}

	@Override
	public Void call() throws Exception {
		EventQueue.invokeLater(() -> {
			try {
				MongoClient client = new MongoClient(new ServerAddress(mongoHost, mongoPort));
				ClientSession clientSession = client.startSession();
				MongoDatabase database = client.getDatabase(databaseName);

				// Create collections if they do not exist
				List<String> collectionNames = database.listCollectionNames().into(new ArrayList<>());
				if (!collectionNames.contains(MONGO_MEMBER_COLLECTION))
					database.createCollection(MONGO_MEMBER_COLLECTION);
				if (!collectionNames.contains(MONGO_COURSE_COLLECTION))
					database.createCollection(MONGO_COURSE_COLLECTION);
				MongoCollection<Document> memberCollection = database.getCollection(MONGO_MEMBER_COLLECTION);
				MongoCollection<Document> courseCollection = database.getCollection(MONGO_COURSE_COLLECTION);

				MongoMemberRepository memberRepository = new MongoMemberRepository(memberCollection, courseCollection, clientSession);
				MongoCourseRepository courseRepository = new MongoCourseRepository(courseCollection, clientSession);
				MongoRepositoryProvider repositoryProvider = new MongoRepositoryProvider(memberRepository,
						courseRepository);
				MongoTransactionManager transactionManager = new MongoTransactionManager(clientSession,
						repositoryProvider);

				GymController controller = new GymController();

				SwingDialogManageMember dialogManageMember = new SwingDialogManageMember(controller);
				SwingDialogManageCourse dialogManageCourse = new SwingDialogManageCourse(controller);
				SwingDialogManageSubs dialogManageSubs = new SwingDialogManageSubs(controller);
				SwingGymView gymView = new SwingGymView(controller, dialogManageMember, dialogManageCourse,
						dialogManageSubs);

				controller.setTransactionManager(transactionManager);
				controller.setView(gymView);

				gymView.setVisible(true);
			} catch (Exception e) {
				LOGGER.error("Application terminated due to exception: {}", e.getMessage());
			}
		});
		return null;
	}

}
