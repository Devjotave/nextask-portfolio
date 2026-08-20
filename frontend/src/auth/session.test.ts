import { clearSession, createSession, readSession, saveSession } from './session'

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

describe('auth session', () => {
  it('creates and restores a valid session', () => {
    const session = createSession(authResponse)
    saveSession(session)

    expect(readSession()).toEqual(session)
  })

  it('removes an expired session', () => {
    saveSession({
      ...createSession(authResponse),
      expiresAt: Date.now() - 1,
    })

    expect(readSession()).toBeNull()
    expect(sessionStorage.getItem('nextask.session')).toBeNull()
  })

  it('ignores corrupted browser data', () => {
    sessionStorage.setItem('nextask.session', '{not-json')

    expect(readSession()).toBeNull()

    clearSession()
    expect(sessionStorage.getItem('nextask.session')).toBeNull()
  })
})
