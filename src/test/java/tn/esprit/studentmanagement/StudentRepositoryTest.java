package tn.esprit.studentmanagement;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import tn.esprit.studentmanagement.entities.Student;
import tn.esprit.studentmanagement.repositories.StudentRepository;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class StudentRepositoryTest {

    @Autowired
    private StudentRepository studentRepository;

    @Test
    void testCreateAndFindStudent() {
        Student student = new Student();
        student.setFirstName("John");
        student.setLastName("Doe");
        student.setEmail("john.doe@example.com");
        student.setPhone("123456789");
        student.setDateOfBirth(LocalDate.of(2000, 1, 1));
        student.setAddress("123 Main St");

        Student saved = studentRepository.save(student);

        Optional<Student> found = studentRepository.findById(saved.getIdStudent());
        assertThat(found).isPresent();
        assertThat(found.get().getFirstName()).isEqualTo("John");
    }

    @Test
    void testDeleteStudent() {
        Student student = new Student();
        student.setFirstName("Alice");
        student.setLastName("Smith");

        Student saved = studentRepository.save(student);
        Long id = saved.getIdStudent();

        studentRepository.deleteById(id);

        Optional<Student> found = studentRepository.findById(id);
        assertThat(found).isNotPresent();
    }
}