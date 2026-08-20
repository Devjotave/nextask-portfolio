import { useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router'

import { readableError } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { AuthShell } from '../components/AuthShell'

interface LoginLocationState {
  from?: {
    pathname?: string
  }
}

export function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError(null)
    setIsSubmitting(true)

    try {
      await login({ email, password })
      const state = location.state as LoginLocationState | null
      navigate(state?.from?.pathname ?? '/app', { replace: true })
    } catch (requestError) {
      setError(readableError(requestError))
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <AuthShell
      eyebrow="Bem-vindo de volta"
      title="Entre na sua conta"
      description="Retome de onde parou e dê o próximo passo."
      footer={
        <p>
          Ainda não tem uma conta? <Link to="/register">Criar agora</Link>
        </p>
      }
    >
      <form className="auth-form" onSubmit={handleSubmit}>
        {error && (
          <div className="form-alert" role="alert">
            <span aria-hidden="true">!</span>
            {error}
          </div>
        )}

        <label className="field">
          <span>E-mail</span>
          <input
            type="email"
            name="email"
            autoComplete="email"
            placeholder="voce@exemplo.com"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            maxLength={254}
            required
          />
        </label>

        <label className="field">
          <span>Senha</span>
          <input
            type="password"
            name="password"
            autoComplete="current-password"
            placeholder="Sua senha"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            maxLength={128}
            required
          />
        </label>

        <button className="primary-button" type="submit" disabled={isSubmitting}>
          <span>{isSubmitting ? 'Entrando…' : 'Entrar no NexTask'}</span>
          {!isSubmitting && <span aria-hidden="true">→</span>}
        </button>
      </form>
    </AuthShell>
  )
}
