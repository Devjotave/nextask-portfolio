import { render, screen, waitFor, within } from '@testing-library/react'
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

const workspace = {
  id: 11,
  name: 'Produto NexTask',
  role: 'OWNER',
  createdAt: '2026-08-20T12:00:00Z',
  updatedAt: '2026-08-20T12:00:00Z',
}

const project = {
  id: 21,
  workspaceId: 11,
  name: 'Lançamento do MVP',
  description: 'Frontend colaborativo',
  createdById: 7,
  createdAt: '2026-08-20T12:00:00Z',
  updatedAt: '2026-08-20T12:00:00Z',
}

const todoTask = {
  id: 31,
  projectId: 21,
  title: 'Revisar API',
  description: 'Cobrir o fluxo principal',
  status: 'TODO',
  priority: 'HIGH',
  position: 0,
  assigneeId: null,
  createdById: 7,
  dueAt: null,
  version: 0,
  createdAt: '2026-08-20T12:00:00Z',
  updatedAt: '2026-08-20T12:00:00Z',
}

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

function page<T>(content: T[]) {
  return {
    content,
    page: 0,
    size: 100,
    totalElements: content.length,
    totalPages: content.length === 0 ? 0 : 1,
  }
}

function installApiMock(
  handler: (url: string, method: string, options?: RequestInit) => Response | Promise<Response>,
) {
  const fetchMock = vi.fn(
    async (input: RequestInfo | URL, options?: RequestInit) =>
      handler(String(input), options?.method ?? 'GET', options),
  )
  vi.stubGlobal('fetch', fetchMock)
  return fetchMock
}

function mockEmptyWorkspaces() {
  return installApiMock((url) => {
    if (url.startsWith('/api/workspaces?')) {
      return jsonResponse(page([]))
    }
    throw new Error(`Unexpected request: ${url}`)
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

  it('logs in through the API and opens workspace onboarding', async () => {
    const fetchMock = installApiMock((url, method) => {
      if (url === '/api/auth/login' && method === 'POST') {
        return jsonResponse(authResponse)
      }
      if (url.startsWith('/api/workspaces?')) {
        return jsonResponse(page([]))
      }
      throw new Error(`Unexpected request: ${method} ${url}`)
    })
    window.history.pushState({}, '', '/login')
    const user = userEvent.setup()

    render(<App />)

    await user.type(screen.getByLabelText('E-mail'), 'ana@example.com')
    await user.type(screen.getByLabelText('Senha'), 'password123')
    await user.click(screen.getByRole('button', { name: 'Entrar no NexTask' }))

    expect(
      await screen.findByRole('heading', { name: 'Crie um workspace para começar' }),
    ).toBeInTheDocument()
    expect(screen.getByText('Ana Souza')).toBeInTheDocument()
    expect(window.location.pathname).toBe('/app')
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/auth/login',
      expect.objectContaining({ method: 'POST' }),
    )

    const workspaceRequest = fetchMock.mock.calls.find(([url]) =>
      String(url).startsWith('/api/workspaces?'),
    )
    const headers = workspaceRequest?.[1]?.headers as Headers
    expect(headers.get('Authorization')).toBe('Bearer header.payload.signature')
  })

  it('shows the problem detail returned by the backend', async () => {
    installApiMock((url) => {
      if (url === '/api/auth/login') {
        return jsonResponse(
          {
            title: 'Authentication failed',
            detail: 'Email or password is incorrect',
            status: 401,
          },
          401,
        )
      }
      throw new Error(`Unexpected request: ${url}`)
    })
    window.history.pushState({}, '', '/login')
    const user = userEvent.setup()

    render(<App />)

    await user.type(screen.getByLabelText('E-mail'), 'ana@example.com')
    await user.type(screen.getByLabelText('Senha'), 'wrong-password')
    await user.click(screen.getByRole('button', { name: 'Entrar no NexTask' }))

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Email or password is incorrect',
    )
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
    mockEmptyWorkspaces()
    saveSession(createSession(authResponse))
    window.history.pushState({}, '', '/app')

    render(<App />)

    expect(
      await screen.findByRole('heading', { name: 'Crie um workspace para começar' }),
    ).toBeInTheDocument()
    expect(screen.getAllByText('ana@example.com')).toHaveLength(1)
  })

  it('clears the session when the user logs out', async () => {
    mockEmptyWorkspaces()
    saveSession(createSession(authResponse))
    window.history.pushState({}, '', '/app')
    const user = userEvent.setup()

    render(<App />)

    await user.click(await screen.findByRole('button', { name: 'Sair da conta' }))

    await waitFor(() => expect(window.location.pathname).toBe('/login'))
    expect(sessionStorage.getItem('nextask.session')).toBeNull()
  })

  it('creates a workspace, a project and the first task', async () => {
    const fetchMock = installApiMock((url, method) => {
      if (url.startsWith('/api/workspaces?')) {
        return jsonResponse(page([]))
      }
      if (url === '/api/workspaces' && method === 'POST') {
        const { role: _role, ...createdWorkspace } = workspace
        return jsonResponse(createdWorkspace, 201)
      }
      if (url.startsWith('/api/workspaces/11/projects?')) {
        return jsonResponse(page([]))
      }
      if (url === '/api/workspaces/11/projects' && method === 'POST') {
        return jsonResponse(project, 201)
      }
      if (url.startsWith('/api/projects/21/tasks?')) {
        return jsonResponse(page([]))
      }
      if (url === '/api/projects/21/tasks' && method === 'POST') {
        return jsonResponse(todoTask, 201)
      }
      throw new Error(`Unexpected request: ${method} ${url}`)
    })
    saveSession(createSession(authResponse))
    window.history.pushState({}, '', '/app')
    const user = userEvent.setup()

    render(<App />)

    await screen.findByRole('heading', { name: 'Crie um workspace para começar' })
    await user.click(screen.getAllByRole('button', { name: 'Criar workspace' })[0])
    let dialog = await screen.findByRole('dialog', { name: 'Criar workspace' })
    await user.type(within(dialog).getByLabelText('Nome do workspace'), workspace.name)
    await user.click(within(dialog).getByRole('button', { name: 'Criar workspace' }))

    await screen.findByRole('heading', { name: 'Crie o primeiro projeto' })
    await user.click(screen.getByRole('button', { name: 'Criar projeto' }))
    dialog = await screen.findByRole('dialog', { name: 'Criar projeto' })
    await user.type(within(dialog).getByLabelText('Nome do projeto'), project.name)
    await user.type(within(dialog).getByLabelText(/Descrição/), project.description)
    await user.click(within(dialog).getByRole('button', { name: 'Criar projeto' }))

    await screen.findByRole('article', { name: 'A fazer' })
    await user.click(screen.getByRole('button', { name: '+ Nova tarefa' }))
    dialog = await screen.findByRole('dialog', { name: 'Criar tarefa' })
    await user.type(within(dialog).getByLabelText('Título da tarefa'), todoTask.title)
    await user.type(within(dialog).getByLabelText(/Descrição/), todoTask.description)
    await user.selectOptions(within(dialog).getByLabelText('Prioridade'), 'HIGH')
    await user.click(within(dialog).getByRole('button', { name: 'Criar tarefa' }))

    expect(await screen.findByText('Revisar API')).toBeInTheDocument()
    const taskRequest = fetchMock.mock.calls.find(
      ([url, options]) =>
        String(url) === '/api/projects/21/tasks' && options?.method === 'POST',
    )
    expect(JSON.parse(String(taskRequest?.[1]?.body))).toEqual(
      expect.objectContaining({ title: 'Revisar API', priority: 'HIGH', position: 0 }),
    )
  })

  it('moves a task to the next Kanban column', async () => {
    const fetchMock = installApiMock((url, method) => {
      if (url.startsWith('/api/workspaces?')) {
        return jsonResponse(page([workspace]))
      }
      if (url.startsWith('/api/workspaces/11/projects?')) {
        return jsonResponse(page([project]))
      }
      if (url.startsWith('/api/projects/21/tasks?')) {
        return jsonResponse(page([todoTask]))
      }
      if (url === '/api/tasks/31' && method === 'PUT') {
        return jsonResponse({ ...todoTask, status: 'IN_PROGRESS', version: 1 })
      }
      throw new Error(`Unexpected request: ${method} ${url}`)
    })
    saveSession(createSession(authResponse))
    window.history.pushState({}, '', '/app')
    const user = userEvent.setup()

    render(<App />)

    await user.click(
      await screen.findByRole('button', {
        name: 'Mover Revisar API para Em andamento',
      }),
    )

    const inProgressColumn = screen.getByRole('article', { name: 'Em andamento' })
    expect(within(inProgressColumn).getByText('Revisar API')).toBeInTheDocument()
    const updateRequest = fetchMock.mock.calls.find(
      ([url, options]) => String(url) === '/api/tasks/31' && options?.method === 'PUT',
    )
    expect(JSON.parse(String(updateRequest?.[1]?.body))).toEqual(
      expect.objectContaining({ status: 'IN_PROGRESS', title: 'Revisar API' }),
    )
  })

  it('ends the local session when the API rejects the token', async () => {
    installApiMock((url) => {
      if (url.startsWith('/api/workspaces?')) {
        return jsonResponse(
          {
            title: 'Authentication required',
            detail: 'A valid Bearer token is required to access this resource',
          },
          401,
        )
      }
      throw new Error(`Unexpected request: ${url}`)
    })
    saveSession(createSession(authResponse))
    window.history.pushState({}, '', '/app')

    render(<App />)

    expect(
      await screen.findByRole('heading', { name: 'Entre na sua conta' }),
    ).toBeInTheDocument()
    expect(sessionStorage.getItem('nextask.session')).toBeNull()
  })
})
