package tn.esprit.studentmanagement;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import tn.esprit.studentmanagement.entities.Course;
import tn.esprit.studentmanagement.repositories.CourseRepository;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

@SpringBootTest
@ActiveProfiles("test")
class CourseRepositoryTest {

    @Autowired
    private CourseRepository courseRepository;

    @Test
    void testCreateAndFindCourse() {
        Course course = new Course();
        course.setName("Algorithms");
        course.setCode("CS101");
        course.setCredit(4);
        course.setDescription("Basic Algorithms");

        Course saved = courseRepository.save(course);

        Optional<Course> found = courseRepository.findById(saved.getIdCourse());
        assertThat(found).isPresent();
        assertThat(found.get().getCode()).isEqualTo("CS101");
    }
}