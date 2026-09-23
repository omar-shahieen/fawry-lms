The system is a **university Learning Management System (LMS) API**. It supports three main actors—**students, instructors, and administrators**—with authentication and role-based access. The API is a single Spring Boot 4.1.1 monolith (Java 25) backed by PostgreSQL 18.

### 1. Users and Access

A user logs into the system using their email and password and receives access and refresh tokens. Every user has a role: **Student, Instructor, or Admin**. Users can manage their own basic profile, while administrators can create, update, deactivate, and manage users and their roles.  

### 2. Courses

Administrators create courses and assign an instructor to each course. A course contains basic information such as its title, code, description, and academic term. Students can browse active courses and enroll themselves. Instructors and administrators can view the enrolled students, while instructors can manage their own courses.  

### 3. Course Structure and Learning Content

Each course is divided into **ordered sections/modules**. An instructor creates sections and controls their order. Each section can contain Markdown-based learning content with a title and body. Enrolled students can read the course sections and content, while the course instructor or administrator can create, edit, reorder, and delete them.  

### 4. Quizzes

An instructor creates quizzes inside a course. A quiz contains questions, and each question contains multiple options with one or more correct answers represented in the database. Quizzes can remain drafts or be published. Students only see published quizzes and cannot see which options are correct.  

### 5. Taking a Quiz

An enrolled student opens a quiz and submits their selected answers. The server enforces the quiz's time limit, checks the submitted answers, automatically calculates the score, and stores the attempt. The student can later view their own attempt history, while instructors and administrators can view attempts across students.  

### 6. Grades

Quiz attempts become the source of the student's grades. A student can view their quiz results grouped by their enrolled courses. An instructor can view the results of students in their own courses, while administrators can access course-wide grade information. 

### 7. Course Communication

Each course has a discussion area where enrolled students, instructors, and administrators can create posts. Students can ask questions and users can reply to posts, but replies are intentionally limited to **one level**. Users can edit their own posts, while administrators can delete any post for moderation. 

### 8. Announcements

Instructors and administrators can publish announcements for a course. Enrolled users can read them, while the author or administrator can edit or delete them. This provides a simple channel for communicating course-wide information. 

### 9. Dashboards

The system provides role-specific dashboards. A **student dashboard** shows enrolled courses and quiz status/best scores. An **instructor dashboard** summarizes their courses, quiz results, and announcements. An **admin dashboard** provides overall counts such as users, courses, and enrollments. 
