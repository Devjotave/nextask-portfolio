import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { vi } from 'vitest'

import { App } from './App'
import { createSession, saveSession } from './auth/session'

const authResponse = {
  accessToken: 'header.payload.signature',
  tokenType: 'Bearer',
  expiresIn: 3_600,
  user: {
    id: 7,
    name: 'Ana Souza',
    email: 'ana@example.com',
    createdAt: '2026-08-19T12:00:00Z',
    updatedAt: '2026-08-19T12:00:00Z',
  },
}

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

describe('NexTask frontend', () => {
  it('redirects unauthenticated visitors to login', async () => {
    window.history.pushState({}, '', '/app')

    render(<App />)

    expect(
      await screen.findByRole('heading', { name: 'Entre na sua conta' }),
    ).toBeInTheDocument()
    expect(window.location.pathname).toBe('/login')
  })

  it('logs in through the API and opens the protected area', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(authResponse))
    vi.stubGlobal('fetch', fetchMock)
    window.history.pushState({}, '', '/login')
    const user = userEvent.setup()

    render(<App />)

    await user.type(screen.getByLabelText('E-mail'), 'ana@example.com')
    await user.type(screen.getByLabelText('Senha'), 'password123')
    await user.click(screen.getByRole('button', { name: 'Entrar no NexTask' }))

    expect(await screen.findByText('Sua jornada no NexTask começou.')).toBeInTheDocument()
    expect(screen.getByText('Ana Souza')).toBeInTheDocument()
    expect(window.location.pathname).toBe('/app')
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/auth/login',
      expect.objectContaining({ method: 'POST' }),
    )
    expect(sessionStorage.getItem('nextask.session')).toContain(
      'header.payload.signature',
    )
  })

  it('shows the problem detail returned by the backend', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        jsonResponse(
          {
            title: 'Authentication failed',
            detail: 'Email or password is incorrect',
            status: 401,
          },
          401,
        ),
      ),
    )
    window.history.pushState({}, '', '/login')
    const user = userEvent.setup()

    render(<App />)

    await user.type(screen.getByLabelText('E-mail'), 'ana@example.com')
    await user.type(screen.getByLabelText('Senha'), 'wrong-password')
    await user.click(screen.getByRole('button', { name: 'Entrar no NexTask' }))

    expect(
      await screen.findByRole('alert', { name: '' }),
    ).toHaveTextContent('Email or password is incorrect')
    expect(window.location.pathname).toBe('/login')
  })

  it('validates password confirmation before registration', async () => {
    const fetchMock = vi.fn()
    vi.stubGlobal('fetch', fetchMock)
    window.history.pushState({}, '', '/register')
    const user = userEvent.setup()

    render(<App />)

    await user.type(screen.getByLabelText('Nome'), 'Ana Souza')
    await user.type(screen.getByLabelText('E-mail'), 'ana@example.com')
    await user.type(screen.getByLabelText('Senha'), 'password123')
    await user.type(screen.getByLabelText('Confirmar senha'), 'different123')
    await user.click(screen.getByRole('button', { name: 'Criar minha conta' }))

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'As senhas precisam ser iguais.',
    )
    expect(fetchMock).not.toHaveBeenCalled()
  })

  it('restores a valid session after a page refresh', async () => {
    saveSession(createSession(authResponse))
    window.history.pushState({}, '', '/app')

    render(<App />)

    expect(await screen.findByText('Sua jornada no NexTask começou.')).toBeInTheDocument()
    expect(screen.getAllByText('ana@example.com')).toHaveLength(2)
  })

  it('clears the session when the user logs out', async () => {
    saveSession(createSession(authResponse))
    window.history.pushState({}, '', '/app')
    const user = userEvent.setup()

    render(<App />)

    await user.click(await screen.findByRole('button', { name: 'Sair da conta' }))

    await waitFor(() => expect(window.location.pathname).toBe('/login'))
    expect(sessionStorage.getItem('nextask.session')).toBeNull()
  })
})
