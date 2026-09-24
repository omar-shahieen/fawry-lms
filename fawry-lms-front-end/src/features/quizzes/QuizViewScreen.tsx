import { useAuth } from '../../auth/context'
import { QuizTakeScreen } from './QuizTakeScreen'
import { StaffQuizView } from './StaffQuizView'

/** Staff and student quiz views are structurally separate components (§7.6.2 rule 5). */
export function QuizViewScreen() {
  const { user } = useAuth()
  if (user?.role === 'STUDENT') return <QuizTakeScreen />
  return <StaffQuizView />
}
