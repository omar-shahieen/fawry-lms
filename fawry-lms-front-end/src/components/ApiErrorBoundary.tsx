import { Component } from 'react'
import type { ErrorInfo, ReactNode } from 'react'
import { Button } from './Button'

interface Props {
  children: ReactNode
}

interface State {
  hasError: boolean
}

/**
 * Catches unexpected render errors and shows a friendly 500 screen —
 * never a raw stack trace or a blank page (frontend-build.md §8.3a/8.3c).
 */
export class ApiErrorBoundary extends Component<Props, State> {
  state: State = { hasError: false }

  static getDerivedStateFromError(): State {
    return { hasError: true }
  }

  componentDidCatch(_error: Error, _errorInfo: ErrorInfo): void {
    // Intentionally not logging internals to the console.
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="flex min-h-svh flex-col items-center justify-center gap-3 bg-gray-50 p-6 text-center">
          <p className="text-sm font-semibold text-red-600">500</p>
          <h1 className="text-2xl font-semibold text-gray-900">Something went wrong</h1>
          <p className="max-w-md text-sm text-gray-500">
            An unexpected error occurred. Your data is safe — try reloading the page.
          </p>
          <Button onClick={() => window.location.reload()}>Reload</Button>
        </div>
      )
    }
    return this.props.children
  }
}
