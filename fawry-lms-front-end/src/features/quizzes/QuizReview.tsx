import { Card, PageHeader } from '../../components/Layout'
import { Badge } from '../../components/Layout'
import { formatDate } from '../../components/formatDate'
import type { Quiz, QuizAnswer } from './api'
import type { MyAttempt, SubmitResult } from './attempt-api'

function readStoredResult(quizId: string): SubmitResult | null {
  try {
    const raw = sessionStorage.getItem(`lms.quizResult.${quizId}`)
    return raw ? (JSON.parse(raw) as SubmitResult) : null
  } catch {
    return null
  }
}

/**
 * Permanent post-submission review. Correctness/score fields render only when
 * the payload actually contains them (never pre-submit — that state cannot reach here).
 * Student detail payload has no option-level correct flags — correctness comes
 * from answers[] (per-question selectedOptionId + isCorrect).
 */
export function QuizReview({ quiz, attempt }: { quiz: Quiz; attempt: MyAttempt | null }) {
  const questions = quiz.questions ?? []
  const stored = readStoredResult(String(quiz.id))

  const answers: QuizAnswer[] =
    quiz.answers && quiz.answers.length > 0 ? quiz.answers : (stored?.answers ?? [])

  const answerByQuestion = new Map<string, QuizAnswer>()
  for (const answer of answers) {
    if (answer.questionId !== undefined && answer.questionId !== null) {
      answerByQuestion.set(String(answer.questionId), answer)
    }
  }

  const score = attempt?.score ?? quiz.score ?? stored?.score
  const total = attempt?.totalQuestions ?? quiz.totalQuestions ?? stored?.totalQuestions ??
    (questions.length > 0 ? questions.length : undefined)

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <PageHeader
        title="Review"
        description="Your permanent record for this quiz — single attempt."
        actions={
          score !== undefined ? (
            <span className="rounded-lg bg-indigo-50 px-3 py-1.5 text-sm font-semibold text-indigo-700">
              Score: {score}
              {total !== undefined ? ` / ${total}` : ''}
            </span>
          ) : undefined
        }
      />

      {attempt?.submittedAt && (
        <div className="rounded-md bg-green-50 px-4 py-3 text-sm font-medium text-green-800">
          Submitted {formatDate(attempt.submittedAt)}
          {attempt.score !== undefined && attempt.score !== null ? ` · score ${attempt.score}` : ''}.
        </div>
      )}

      <div className="space-y-4">
        {questions.map((question, index) => {
          const optionList = question.options ?? []
          const answer =
            question.id !== undefined && question.id !== null
              ? answerByQuestion.get(String(question.id))
              : undefined
          const selectedId = answer?.selectedOptionId

          return (
            <Card key={String(question.id ?? index)}>
              <div className="flex items-start justify-between gap-3">
                <p className="text-sm font-semibold text-gray-900">
                  {index + 1}. {question.text}
                </p>
                {answer?.isCorrect === true && <Badge color="green">correct</Badge>}
                {answer?.isCorrect === false && <Badge color="red">incorrect</Badge>}
              </div>
              <ul className="mt-3 space-y-2">
                {optionList.map((option) => {
                  const isSelected = selectedId !== undefined && option.id === selectedId
                  const isCorrectAnswer = answer !== undefined && isSelected && answer.isCorrect === true
                  const isWrongAnswer = answer !== undefined && isSelected && answer.isCorrect === false
                  return (
                    <li
                      key={String(option.id ?? option.text)}
                      className={`flex items-center gap-2 rounded-md border px-3 py-2 text-sm ${
                        isCorrectAnswer
                          ? 'border-green-300 bg-green-50 text-green-900'
                          : isWrongAnswer
                            ? 'border-red-200 bg-red-50 text-red-800'
                            : 'border-gray-200 bg-white text-gray-700'
                      }`}
                    >
                      <span
                        className={`inline-block size-2 rounded-full ${
                          isCorrectAnswer ? 'bg-green-500' : isWrongAnswer ? 'bg-red-400' : 'bg-gray-300'
                        }`}
                        aria-hidden="true"
                      />
                      {option.text}
                      {isCorrectAnswer && <span className="text-xs font-medium text-green-700">your answer · correct</span>}
                      {isWrongAnswer && <span className="text-xs font-medium text-red-700">your answer</span>}
                    </li>
                  )
                })}
              </ul>
            </Card>
          )
        })}
      </div>
    </div>
  )
}
