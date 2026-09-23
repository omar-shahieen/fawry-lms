package com.fawry.lms.communication;

import com.fawry.lms.course.entities.Course;
import com.fawry.lms.communication.entities.DiscussionPost;
import com.fawry.lms.course.CourseRepository;
import com.fawry.lms.user.UserRepository;
import com.fawry.lms.communication.entities.DiscussionPost;
import com.fawry.lms.user.entities.Role;
import com.fawry.lms.user.entities.User;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CommunicationRepositoryTest {

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private CourseRepository courseRepository;

        @Autowired
        private DiscussionPostRepository discussionPostRepository;

        @Autowired
        private EntityManager entityManager;

        @Test
        void loadsReplyParentAndPaginatesCoursePostsNewestFirst() {
                User author = new User();
                author.setFullName("Test Student");
                author.setEmail("student@example.com");
                author.setPassword("hashed-password");
                author.setRole(Role.STUDENT);
                author = userRepository.saveAndFlush(author);

                User instructor = new User();
                instructor.setFullName("Test Instructor");
                instructor.setEmail("instructor@example.com");
                instructor.setPassword("hashed-password");
                instructor.setRole(Role.INSTRUCTOR);
                instructor = userRepository.saveAndFlush(instructor);

                Course course = new Course();
                course.setTitle("Test Course");
                course.setCode("CS501");
                course.setTerm("Fall 2026");
                course.setInstructor(instructor);
                course = courseRepository.saveAndFlush(course);

                DiscussionPost parent = new DiscussionPost();
                parent.setCourse(course);
                parent.setAuthor(author);
                parent.setTitle("Question");
                parent.setBody("Parent post body");
                Long parentId = discussionPostRepository.saveAndFlush(parent).getId();

                DiscussionPost reply = new DiscussionPost();
                reply.setCourse(course);
                reply.setAuthor(author);
                reply.setBody("Reply body");
                reply.setParentPost(parent);
                discussionPostRepository.saveAndFlush(reply);

                entityManager.clear();

                DiscussionPost reloadedReply = discussionPostRepository.findById(reply.getId()).orElseThrow();
                assertThat(reloadedReply.getParentPost().getId()).isEqualTo(parentId);

                PageRequest pageRequest = PageRequest.of(0, 1);
                Page<DiscussionPost> firstPage = discussionPostRepository
                                .findByCourse_IdOrderByCreatedAtDesc(course.getId(), pageRequest);
                Page<DiscussionPost> secondPage = discussionPostRepository
                                .findByCourse_IdOrderByCreatedAtDesc(course.getId(), pageRequest.next());

                assertThat(firstPage.getTotalElements()).isEqualTo(2);
                assertThat(firstPage.getTotalPages()).isEqualTo(2);
                assertThat(firstPage.getContent()).hasSize(1);
                assertThat(secondPage.getContent()).hasSize(1);
                assertThat(firstPage.getContent().get(0).getCreatedAt())
                                .isAfterOrEqualTo(secondPage.getContent().get(0).getCreatedAt());
        }
}
