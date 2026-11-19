import {
  createContext,
  useContext,
  useState,
  type PropsWithChildren,
} from 'react'
import { apiRequest } from '../api/client'
import { useQueryClient } from '@tanstack/react-query'

type LoginResponse = {
  accessToken: string
  tokenType: string
}

type RegisterInput = {
  name: string
  email: string
  password: string
}

type AuthContextValue = {
  token: string | null
  email: string
  isAuthenticated: boolean
  login: (email: string, password: string) => Promise<void>
  register: (input: RegisterInput) => Promise<void>
  logout: () => void
}

const TOKEN_KEY = 'sprintforge.accessToken'
const EMAIL_KEY = 'sprintforge.email'

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

export function AuthProvider({ children }: PropsWithChildren) {
  // Keep the login for this browser tab; never store the password.
  const [token, setToken] = useState<string | null>(
    () => sessionStorage.getItem(TOKEN_KEY),
  )
  const [email, setEmail] = useState(
    () => sessionStorage.getItem(EMAIL_KEY) ?? '',
  )

  const queryClient = useQueryClient()

  async function login(userEmail: string, password: string) {
    const result = await apiRequest<LoginResponse>('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email: userEmail, password }),
    })

    sessionStorage.setItem(TOKEN_KEY, result.accessToken)
    sessionStorage.setItem(EMAIL_KEY, userEmail)
    setToken(result.accessToken)
    setEmail(userEmail)
  }

  async function register(input: RegisterInput) {
    // The backend creates the account, then we sign in with those credentials.
    await apiRequest<unknown>('/api/auth/register', {
      method: 'POST',
      body: JSON.stringify(input),
    })

    await login(input.email, input.password)
  }

  function logout() {
    sessionStorage.removeItem(TOKEN_KEY)
    sessionStorage.removeItem(EMAIL_KEY)
    setToken(null)
    setEmail('')
    queryClient.clear()
  }

  return (
    <AuthContext.Provider
      value={{
        token,
        email,
        isAuthenticated: Boolean(token),
        login,
        register,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const context = useContext(AuthContext)

  if (!context) {
    throw new Error('useAuth must be used inside AuthProvider')
  }

  return context
}