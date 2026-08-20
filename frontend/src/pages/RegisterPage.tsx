import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router'

import { readableError } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { AuthShell } from '../components/AuthShell'

export function RegisterPage() {
  const { register } = useAuth()
  const navigate = useNavigate()
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [passwordConfirmation, setPasswordConfirmation] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError(null)

    if (password !== passwordConfirmation) {
      setError('As senhas precisam ser iguais.')
      return
    }

    setIsSubmitting(true)

    try {
      await register({ name, email, password })
      navigate('/app', { replace: true })
    } catch (requestError) {
      setError(readableError(requestError))
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <AuthShell
      eyebrow="Comece com clareza"
      title="Crie sua conta"
      description="Seu novo espaço de trabalho fica pronto em poucos segundos."
      footer={
        <p>
          Já possui uma conta? <Link to="/login">Entrar</Link>
        </p>
      }
    >
      <form className="auth-form auth-form--register" onSubmit={handleSubmit}>
        {error && (
          <div className="form-alert" role="alert">
            <span aria-hidden="true">!</span>
            {error}
          </div>
        )}

        <label className="field">
          <span>Nome</span>
          <input
            type="text"
            name="name"
            autoComplete="name"
            placeholder="Como podemos chamar você?"
            value={name}
            onChange={(event) => setName(event.target.value)}
            maxLength={120}
            required
          />
        </label>

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

        <div className="field-row">
          <label className="field">
            <span>Senha</span>
            <input
              type="password"
              name="password"
              autoComplete="new-password"
              placeholder="Mínimo de 8 caracteres"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              minLength={8}
              maxLength={128}
              required
            />
          </label>

          <label className="field">
            <span>Confirmar senha</span>
            <input
              type="password"
              name="passwordConfirmation"
              autoComplete="new-password"
              placeholder="Repita a senha"
              value={passwordConfirmation}
              onChange={(event) => setPasswordConfirmation(event.target.value)}
              minLength={8}
              maxLength={128}
              required
            />
          </label>
        </div>

        <button className="primary-button" type="submit" disabled={isSubmitting}>
          <span>{isSubmitting ? 'Criando conta…' : 'Criar minha conta'}</span>
          {!isSubmitting && <span aria-hidden="true">→</span>}
        </button>
      </form>
    </AuthShell>
  )
}
