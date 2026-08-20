export interface ApiProblem {
  type?: string
  title?: string
  status?: number
  detail?: string
  errors?: Record<string, string>
}

export class ApiError extends Error {
  readonly status: number
  readonly problem?: ApiProblem

  constructor(status: number, message: string, problem?: ApiProblem) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.problem = problem
  }
}

const apiBaseUrl = (import.meta.env.VITE_API_URL ?? '').replace(/\/$/, '')

async function readJson(response: Response): Promise<unknown> {
  const contentType = response.headers.get('content-type') ?? ''

  if (!contentType.includes('json')) {
    return undefined
  }

  return response.json()
}

export async function apiRequest<T>(
  path: string,
  options: RequestInit = {},
): Promise<T> {
  const headers = new Headers(options.headers)

  if (options.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }

  const response = await fetch(`${apiBaseUrl}${path}`, {
    ...options,
    headers,
  })

  if (!response.ok) {
    const problem = (await readJson(response)) as ApiProblem | undefined
    const message =
      problem?.detail ?? problem?.title ?? `A API respondeu com status ${response.status}`

    throw new ApiError(response.status, message, problem)
  }

  if (response.status === 204) {
    return undefined as T
  }

  return (await readJson(response)) as T
}

export function authenticatedRequest<T>(
  token: string,
  path: string,
  options: RequestInit = {},
): Promise<T> {
  const headers = new Headers(options.headers)
  headers.set('Authorization', `Bearer ${token}`)

  return apiRequest<T>(path, {
    ...options,
    headers,
  })
}

export function readableError(error: unknown): string {
  if (error instanceof ApiError) {
    return error.message
  }

  if (error instanceof TypeError) {
    return 'Não foi possível conectar ao NexTask. Confirme se o backend está ligado.'
  }

  return 'Algo inesperado aconteceu. Tente novamente.'
}
