import { BrowserRouter } from 'react-router'
import { AuthProvider } from './auth/AuthProvider'
import { AppRoutes } from './routes'

function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <AppRoutes />
      </AuthProvider>
    </BrowserRouter>
  )
}

export default App
