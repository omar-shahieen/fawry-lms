export interface ApiErrorBody {
  timestamp?: string
  status?: number
  error?: string
  message?: string
  fieldErrors?: Record<string, string>
}

export class ApiError extends Error {
  readonly status: number
  readonly fieldErrors?: Record<string, string>
  readonly body?: ApiErrorBody

  constructor(status: number, message: string, body?: ApiErrorBody) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.body = body
    this.fieldErrors = body?.fieldErrors
  }
}

export function isApiError(error: unknown): error is ApiError {
  return error instanceof ApiError
}

const STATUS_FALLBACK_MESSAGES: Record<number, string> = {
  400: 'The request was invalid.',
  401: 'You are not signed in.',
  403: 'You do not have permission to do that.',
  404: 'Not found.',
  409: 'That conflicts with the current state.',
  500: 'Something went wrong on the server.',
}

export async function responseToApiError(response: Response): Promise<ApiError> {
  let body: ApiErrorBody | undefined
  try {
    const text = await response.text()
    if (text) body = JSON.parse(text) as ApiErrorBody
  } catch {
    // non-JSON error body — fall through to status-based message
  }

  const message =
    body?.message ||
    (typeof body?.error === 'string' && body.error) ||
    STATUS_FALLBACK_MESSAGES[response.status] ||
    `Request failed with status ${response.status}.`

  return new ApiError(response.status, message, body)
}
