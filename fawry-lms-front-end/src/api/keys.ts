export const queryKeys = {
  me: ['me'] as const,

  users: (filters?: Record<string, unknown>) => ['users', filters ?? {}] as const,
  user: (id: string | number) => ['user', id] as const,

  courses: (filters?: Record<string, unknown>) => ['courses', filters ?? {}] as const,
  course: (id: string) => ['course', id] as const,
  courseStudents: (id: string) => ['course', id, 'students'] as const,

  sections: (courseId: string) => ['course', courseId, 'sections'] as const,
  contentList: (sectionId: string) => ['section', sectionId, 'content'] as const,
  content: (id: string) => ['content', id] as const,

  courseQuizzes: (courseId: string) => ['course', courseId, 'quizzes'] as const,
  quiz: (id: string) => ['quiz', id] as const,
  quizAttemptMine: (id: string) => ['quiz', id, 'attempt-mine'] as const,
  quizAttempts: (id: string, filters?: Record<string, unknown>) =>
    ['quiz', id, 'attempts', filters ?? {}] as const,

  myGrades: ['grades', 'me'] as const,
  courseGrades: (courseId: string, filters?: Record<string, unknown>) =>
    ['course', courseId, 'grades', filters ?? {}] as const,

  discussion: (courseId: string, filters?: Record<string, unknown>) =>
    ['course', courseId, 'discussion', filters ?? {}] as const,
  announcements: (courseId: string, filters?: Record<string, unknown>) =>
    ['course', courseId, 'announcements', filters ?? {}] as const,

  studentDashboard: ['dashboard', 'student'] as const,
  instructorDashboard: ['dashboard', 'instructor'] as const,
  adminDashboard: ['dashboard', 'admin'] as const,
}
