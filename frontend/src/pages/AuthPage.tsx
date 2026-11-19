import { useState, type SubmitEvent } from 'react'
import { Link, useNavigate } from 'react-router'
import { useAuth } from '../auth/AuthContext'
type AuthPageProps = {
  mode: 'login' | 'register'
}

export default function AuthPage({ mode }: AuthPageProps) {
  const isRegister = mode === 'register'
  const { login, register } = useAuth()
  const navigate = useNavigate()

  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  async function handleSubmit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    setIsSubmitting(true)

    try {
      if (isRegister) {
        await register({ name, email, password })
      } else {
        await login(email, password)
      }

      navigate('/app', { replace: true })
    } catch (requestError) {
      setError(
        requestError instanceof Error
          ? requestError.message
          : 'Something went wrong. Please try again.',
      )
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <main className="auth-page">
      <aside className="brand-panel">
        <Link className="brand" to="/login" aria-label="SprintForge home">
          <span className="brand-mark">SF</span>
          <span>SprintForge</span>
        </Link>

        <div className="brand-message">
          <p className="eyebrow">PLAN. BUILD. SHIP.</p>
          <h1>Make progress visible.</h1>
          <p className="brand-description">
            Bring your team’s work together, from the first idea to the final
            sprint.
          </p>

          <div className="feature-list">
            <p><span>01</span> Organize work by workspace and project</p>
            <p><span>02</span> Keep sprint goals and issues in view</p>
            <p><span>03</span> Give every teammate a clear next step</p>
          </div>
        </div>

        <p className="brand-caption">A calmer way to move work forward.</p>
      </aside>

      <section className="form-panel">
        <Link className="brand mobile-brand" to="/login">
          <span className="brand-mark">SF</span>
          <span>SprintForge</span>
        </Link>

        <div className="auth-card">
          <p className="eyebrow">{isRegister ? 'GET STARTED' : 'WELCOME BACK'}</p>
          <h2>{isRegister ? 'Create your account' : 'Sign in to SprintForge'}</h2>
          <p className="form-intro">
            {isRegister
              ? 'Set up your account and start organizing your team’s work.'
              : 'Enter your details to continue to your workspace.'}
          </p>

          <form onSubmit={handleSubmit}>
            {isRegister && (
              <label className="field">
                <span>Name</span>
                <input
                  autoComplete="name"
                  maxLength={100}
                  onChange={(event) => setName(event.target.value)}
                  placeholder="Your name"
                  required
                  value={name}
                />
              </label>
            )}

            <label className="field">
              <span>Email</span>
              <input
                autoComplete="email"
                maxLength={255}
                onChange={(event) => setEmail(event.target.value)}
                placeholder="you@example.com"
                required
                type="email"
                value={email}
              />
            </label>

            <label className="field">
              <span>Password</span>
              <input
                autoComplete={isRegister ? 'new-password' : 'current-password'}
                maxLength={72}
                minLength={isRegister ? 8 : undefined}
                onChange={(event) => setPassword(event.target.value)}
                placeholder={isRegister ? 'At least 8 characters' : 'Your password'}
                required
                type="password"
                value={password}
              />
            </label>

            {error && (
              <p className="error-message" role="alert">
                {error}
              </p>
            )}

            <button className="primary-button" disabled={isSubmitting} type="submit">
              {isSubmitting
                ? 'Please wait…'
                : isRegister
                  ? 'Create account'
                  : 'Sign in'}
            </button>
          </form>

          <p className="auth-switch">
            {isRegister ? 'Already have an account?' : 'New to SprintForge?'}{' '}
            <Link to={isRegister ? '/login' : '/register'}>
              {isRegister ? 'Sign in' : 'Create an account'}
            </Link>
          </p>
        </div>

        <p className="form-footnote">Your team’s work, in one place.</p>
      </section>
    </main>
  )
}