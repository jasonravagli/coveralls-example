package it.jasonravagli.gym.appmysql;

import java.awt.EventQueue;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.concurrent.Callable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.jasonravagli.gym.gui.SwingDialogManageCourse;
import it.jasonravagli.gym.gui.SwingDialogManageMember;
import it.jasonravagli.gym.gui.SwingDialogManageSubs;
import it.jasonravagli.gym.gui.SwingGymView;
import it.jasonravagli.gym.logic.GymController;
import it.jasonravagli.gym.mysql.MySqlCourseRepository;
import it.jasonravagli.gym.mysql.MySqlMemberRepository;
import it.jasonravagli.gym.mysql.MySqlRepositoryProvider;
import it.jasonravagli.gym.mysql.MySqlTransactionManager;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(mixinStandardHelpOptions = true)
public class MySqlGymApp implements Callable<Void> {

	private static final Logger LOGGER = LogManager.getLogger(MySqlGymApp.class);

	@Option(names = { "--mysql-host" }, description = "MySQL host address")
	private String mysqlHost = "localhost";

	@Option(names = { "--mysql-port" }, description = "MySQL host port")
	private int mysqlPort = 3306;

	@Option(names = { "--mysql-user" }, description = "MySQL username")
	private String mysqlUser = "root";
	
	@Option(names = { "--mysql-pwd" }, description = "Boolean flag. When it is true the user will be asked for the MySQL user password")
	private boolean inputPassword = false;

	@Option(names = { "--db-name" }, description = "Database name")
	private String databaseName = "test";
	
	// Default password. The user can overwrite it using the console when mysql-pwd is true
	private String mysqlPassword = "password";

	public static void main(String[] args) {
		new CommandLine(new MySqlGymApp()).execute(args);
	}

	@Override
	public Void call() throws Exception {
		if(inputPassword) {
			LOGGER.info("User password: ");
			mysqlPassword = new String(System.console().readPassword());
		}
		EventQueue.invokeLater(() -> {
			try {
				String connectionUrl = "jdbc:mysql://" + mysqlHost + ":" + mysqlPort + "/" + databaseName; 
				Connection connection = DriverManager.getConnection(connectionUrl, mysqlUser, mysqlPassword);

				MySqlMemberRepository memberRepository = new MySqlMemberRepository(connection);
				MySqlCourseRepository courseRepository = new MySqlCourseRepository(connection);

				MySqlRepositoryProvider repositoryProvider = new MySqlRepositoryProvider(memberRepository,
						courseRepository);
				MySqlTransactionManager transactionManager = new MySqlTransactionManager(connection,
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
