import { BrowserRouter } from 'react-router'
import { AuthProvider } from './auth/AuthProvider'
import { ApiErrorBoundary } from './components/ApiErrorBoundary'
import { AppRoutes } from './routes'

function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <ApiErrorBoundary>
          <AppRoutes />
        </ApiErrorBoundary>
      </AuthProvider>
    </BrowserRouter>
  )
}

export default App
