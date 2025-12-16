package tn.esprit.studentmanagement;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import tn.esprit.studentmanagement.entities.*;
import tn.esprit.studentmanagement.repositories.EnrollmentRepository;
import tn.esprit.studentmanagement.repositories.StudentRepository;
import tn.esprit.studentmanagement.repositories.CourseRepository;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class EnrollmentRepositoryTest {

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Test
    void testCreateAndFindEnrollment() {
        // Create student
        Student student = new Student();
        student.setFirstName("Mike");
        student.setLastName("Jordan");
        studentRepository.save(student);

        // Create course
        Course course = new Course();
        course.setName("Math");
        course.setCode("M101");
        course.setCredit(3);
        courseRepository.save(course);

        // Create enrollment
        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setCourse(course);
        enrollment.setEnrollmentDate(LocalDate.now());
        enrollment.setStatus(Status.ACTIVE);
        enrollment.setGrade(95.0);

        Enrollment saved = enrollmentRepository.save(enrollment);

        Optional<Enrollment> found = enrollmentRepository.findById(saved.getIdEnrollment());
        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo(Status.ACTIVE);
    }
}