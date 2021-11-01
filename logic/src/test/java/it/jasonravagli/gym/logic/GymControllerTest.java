package it.jasonravagli.gym.logic;

import static org.mockito.AdditionalAnswers.answer;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import it.jasonravagli.gym.model.Course;
import it.jasonravagli.gym.model.Member;

public class GymControllerTest {

	private static final String EXCEPTION_MESSAGE = "An exception occurred";

	private AutoCloseable closeable;

	@Mock
	private GymView gymView;

	@Mock
	private MemberRepository memberRepository;

	@Mock
	private CourseRepository courseRepository;

	@Mock
	private TransactionManager transactionManager;

	@Mock
	private RepositoryProvider repositoryProvider;

	private GymController gymController;

	@Before
	public void setup() {
		closeable = MockitoAnnotations.openMocks(this);

		gymController = new GymController();
		gymController.setTransactionManager(transactionManager);
		gymController.setView(gymView);

		when(repositoryProvider.getMemberRepository()).thenReturn(memberRepository);
		when(repositoryProvider.getCourseRepository()).thenReturn(courseRepository);
	}

	@After
	public void releaseMocks() throws Exception {
		closeable.close();
	}

	@Test
	public void testAllMembersWhenEverythingOk() {
		List<Member> members = Collections.singletonList(new Member());
		when(memberRepository.findAll()).thenReturn(members);
		setupTransactionManagerToExecuteCode();

		gymController.allMembers();

		verify(transactionManager).doInTransaction(any());
		verify(memberRepository).findAll();
		verify(gymView).showMembers(members);
	}

	@Test
	public void testAllMembersWhenExceptionIsThrownDuringTransaction() {
		setupTransactionManagerToThrowException(EXCEPTION_MESSAGE);

		gymController.allMembers();

		verify(gymView).showError(EXCEPTION_MESSAGE);
		verifyNoMoreInteractions(gymView);
	}

	@Test
	public void testAllCoursesWhenEverythingOk() {
		List<Course> courses = Collections.singletonList(new Course());
		when(courseRepository.findAll()).thenReturn(courses);
		setupTransactionManagerToExecuteCode();

		gymController.allCourses();

		verify(transactionManager).doInTransaction(any());
		verify(courseRepository).findAll();
		verify(gymView).showCourses(courses);
	}

	@Test
	public void testAllCoursesWhenExceptionIsThrownDuringTransaction() {
		setupTransactionManagerToThrowException(EXCEPTION_MESSAGE);

		gymController.allCourses();

		verify(gymView).showError(EXCEPTION_MESSAGE);
		verifyNoMoreInteractions(gymView);
	}

	@Test
	public void testAddMemberWhenMemberDoesNotExist() {
		UUID idMember = UUID.randomUUID();
		Member member = new Member();
		member.setId(idMember);
		when(memberRepository.findById(idMember)).thenReturn(null);
		setupTransactionManagerToExecuteCode();

		gymController.addMember(member);

		InOrder inOrder = Mockito.inOrder(transactionManager, memberRepository, gymView);
		inOrder.verify(transactionManager).doInTransaction(any());
		inOrder.verify(memberRepository).save(member);
		inOrder.verify(gymView).memberAdded(member);
	}

	@Test
	public void testAddMemberWhenMemberAlreadyExists() {
		UUID existingId = UUID.randomUUID();
		Member existingMember = new Member();
		existingMember.setId(existingId);
		when(memberRepository.findById(existingId)).thenReturn(existingMember);

		Member newMember = new Member();
		newMember.setId(existingId);
		setupTransactionManagerToExecuteCode();

		gymController.addMember(newMember);

		verify(transactionManager).doInTransaction(any());
		verify(memberRepository).findById(existingId);
		verify(gymView).showError("A member with id " + existingId + " already exists");
		verifyNoMoreInteractions(memberRepository, gymView);
	}

	@Test
	public void testAddMemberWhenExceptionIsThrownDuringTransaction() {
		setupTransactionManagerToThrowException(EXCEPTION_MESSAGE);

		gymController.addMember(new Member());

		verify(gymView).showError(EXCEPTION_MESSAGE);
		verifyNoMoreInteractions(gymView);
	}

	@Test
	public void testDeleteMemberWhenMemberExists() {
		UUID idMember = UUID.randomUUID();
		Member member = new Member();
		member.setId(idMember);
		when(memberRepository.findById(idMember)).thenReturn(member);
		setupTransactionManagerToExecuteCode();

		gymController.deleteMember(member);

		InOrder inOrder = Mockito.inOrder(transactionManager, memberRepository, gymView);
		inOrder.verify(transactionManager).doInTransaction(any());
		inOrder.verify(memberRepository).deleteById(idMember);
		inOrder.verify(gymView).memberDeleted(member);
	}

	@Test
	public void testDeleteMemberWhenMemberDoesNotExist() {
		UUID idMember = UUID.randomUUID();
		Member member = new Member();
		member.setId(idMember);
		when(memberRepository.findById(idMember)).thenReturn(null);
		setupTransactionManagerToExecuteCode();

		gymController.deleteMember(member);

		verify(memberRepository).findById(idMember);
		verify(gymView).showError("Member with id " + idMember + " does not exist");
		verifyNoMoreInteractions(memberRepository, gymView);
	}

	@Test
	public void testDeleteMemberWhenExceptionIsThrownDuringTransaction() {
		setupTransactionManagerToThrowException(EXCEPTION_MESSAGE);

		gymController.deleteMember(new Member());

		verify(gymView).showError(EXCEPTION_MESSAGE);
		verifyNoMoreInteractions(gymView);
	}

	@Test
	public void testUpdateMemberWhenMemberExists() {
		UUID idMember = UUID.randomUUID();
		Member existingMember = new Member();
		existingMember.setId(idMember);
		Member updatedMember = new Member();
		updatedMember.setId(idMember);
		when(memberRepository.findById(idMember)).thenReturn(existingMember);
		setupTransactionManagerToExecuteCode();

		gymController.updateMember(updatedMember);

		InOrder inOrder = Mockito.inOrder(transactionManager, memberRepository, gymView);
		inOrder.verify(transactionManager).doInTransaction(any());
		inOrder.verify(memberRepository).update(updatedMember);
		inOrder.verify(gymView).memberUpdated(updatedMember);
	}

	@Test
	public void testUpdateMemberWhenMemberDoesNotExist() {
		UUID idMember = UUID.randomUUID();
		Member updatedMember = new Member();
		updatedMember.setId(idMember);
		when(memberRepository.findById(idMember)).thenReturn(null);
		setupTransactionManagerToExecuteCode();

		gymController.updateMember(updatedMember);

		verify(transactionManager).doInTransaction(any());
		verify(memberRepository).findById(idMember);
		verify(gymView).showError("Member with id " + idMember + " does not exist");
		verifyNoMoreInteractions(memberRepository, gymView);
	}

	@Test
	public void testUpdateMemberWhenExceptionIsThrownDuringTransaction() {
		setupTransactionManagerToThrowException(EXCEPTION_MESSAGE);

		gymController.updateMember(new Member());

		verify(gymView).showError(EXCEPTION_MESSAGE);
		verifyNoMoreInteractions(gymView);
	}

	@Test
	public void testAddCourseWhenCourseDoesNotExist() {
		UUID idCourse = UUID.randomUUID();
		Course course = new Course();
		course.setId(idCourse);
		when(courseRepository.findById(idCourse)).thenReturn(null);
		setupTransactionManagerToExecuteCode();

		gymController.addCourse(course);

		InOrder inOrder = Mockito.inOrder(transactionManager, courseRepository, gymView);
		inOrder.verify(transactionManager).doInTransaction(any());
		inOrder.verify(courseRepository).save(course);
		inOrder.verify(gymView).courseAdded(course);
	}

	@Test
	public void testAddCourseWhenCourseAlreadyExists() {
		UUID existingId = UUID.randomUUID();
		Course existingCourse = new Course();
		existingCourse.setId(existingId);
		when(courseRepository.findById(existingId)).thenReturn(existingCourse);

		Course newCourse = new Course();
		newCourse.setId(existingId);
		setupTransactionManagerToExecuteCode();

		gymController.addCourse(newCourse);

		verify(transactionManager).doInTransaction(any());
		verify(courseRepository).findById(existingId);
		verify(gymView).showError("A course with id " + existingId + " already exists");
		verifyNoMoreInteractions(courseRepository, gymView);
	}

	@Test
	public void testAddCourseWhenExceptionIsThrownDuringTransaction() {
		setupTransactionManagerToThrowException(EXCEPTION_MESSAGE);

		gymController.addCourse(new Course());

		verify(gymView).showError(EXCEPTION_MESSAGE);
		verifyNoMoreInteractions(gymView);
	}

	@Test
	public void testDeleteCourseWhenCourseExists() {
		UUID idCourse = UUID.randomUUID();
		Course course = new Course();
		course.setId(idCourse);
		when(courseRepository.findById(idCourse)).thenReturn(course);
		setupTransactionManagerToExecuteCode();

		gymController.deleteCourse(course);

		InOrder inOrder = Mockito.inOrder(transactionManager, courseRepository, gymView);
		inOrder.verify(transactionManager).doInTransaction(any());
		inOrder.verify(courseRepository).deleteById(idCourse);
		inOrder.verify(gymView).courseDeleted(course);
	}

	@Test
	public void testDeleteCourseWhenCourseDoesNotExists() {
		UUID idCourse = UUID.randomUUID();
		Course course = new Course();
		course.setId(idCourse);
		when(courseRepository.findById(idCourse)).thenReturn(null);
		setupTransactionManagerToExecuteCode();

		gymController.deleteCourse(course);

		verify(transactionManager).doInTransaction(any());
		verify(courseRepository).findById(idCourse);
		verify(gymView).showError("Course with id " + idCourse + " does not exist");
		verifyNoMoreInteractions(courseRepository, gymView);
	}

	@Test
	public void testDeleteCourseWhenExceptionIsThrownDuringTransaction() {
		setupTransactionManagerToThrowException(EXCEPTION_MESSAGE);

		gymController.deleteCourse(new Course());

		verify(gymView).showError(EXCEPTION_MESSAGE);
		verifyNoMoreInteractions(gymView);
	}

	@Test
	public void testUpdateCourseWhenCourseExists() {
		UUID existingId = UUID.randomUUID();
		Course existingCourse = new Course();
		existingCourse.setId(existingId);
		when(courseRepository.findById(existingId)).thenReturn(existingCourse);

		Course updatedCourse = new Course();
		updatedCourse.setId(existingId);
		setupTransactionManagerToExecuteCode();

		gymController.updateCourse(updatedCourse);

		InOrder inOrder = Mockito.inOrder(transactionManager, courseRepository, gymView);
		inOrder.verify(transactionManager).doInTransaction(any());
		inOrder.verify(courseRepository).update(updatedCourse);
		inOrder.verify(gymView).courseUpdated(updatedCourse);
	}

	@Test
	public void testUpdateCourseWhenCourseDoesNotExist() {
		UUID idCourse = UUID.randomUUID();
		Course updatedCourse = new Course();
		updatedCourse.setId(idCourse);
		when(courseRepository.findById(idCourse)).thenReturn(null);
		setupTransactionManagerToExecuteCode();

		gymController.updateCourse(updatedCourse);

		verify(transactionManager).doInTransaction(any());
		verify(courseRepository).findById(idCourse);
		verify(gymView).showError("Course with id " + idCourse + " does not exist");
		verifyNoMoreInteractions(courseRepository, gymView);
	}

	@Test
	public void testUpdateCourseWhenExceptionIsThrownDuringTransaction() {
		setupTransactionManagerToThrowException(EXCEPTION_MESSAGE);

		gymController.updateCourse(new Course());

		verify(gymView).showError(EXCEPTION_MESSAGE);
		verifyNoMoreInteractions(gymView);
	}

	private void setupTransactionManagerToExecuteCode() {
		when(transactionManager.doInTransaction(any()))
				.thenAnswer(answer((TransactionCode<?> code) -> code.apply(repositoryProvider)));
	}

	private void setupTransactionManagerToThrowException(String exceptionMessage) {
		when(transactionManager.doInTransaction(any())).thenThrow(new TransactionException(exceptionMessage));
	}

}
