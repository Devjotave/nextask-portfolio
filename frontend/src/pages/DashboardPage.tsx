import { useNavigate } from 'react-router'

import { useAuth } from '../auth/AuthContext'
import { Brand } from '../components/Brand'

function initials(name: string): string {
  return name
    .trim()
    .split(/\s+/)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase())
    .join('')
}

export function DashboardPage() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  function handleLogout() {
    logout()
    navigate('/login', { replace: true })
  }

  if (!user) {
    return null
  }

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <Brand compact />

        <nav className="sidebar__nav" aria-label="Navegação principal">
          <a className="nav-item nav-item--active" href="#overview" aria-current="page">
            <span aria-hidden="true">⌂</span>
            Visão geral
          </a>
          <span className="nav-item nav-item--disabled">
            <span aria-hidden="true">□</span>
            Workspaces
            <small>em breve</small>
          </span>
          <span className="nav-item nav-item--disabled">
            <span aria-hidden="true">✓</span>
            Minhas tarefas
            <small>em breve</small>
          </span>
        </nav>

        <div className="sidebar__phase">
          <span>Fase 8</span>
          <strong>Frontend conectado</strong>
          <div className="phase-progress" aria-label="Progresso da fase concluído">
            <span />
          </div>
        </div>

        <button className="sidebar__logout" type="button" onClick={handleLogout}>
          <span aria-hidden="true">↗</span>
          Sair da conta
        </button>
      </aside>

      <main className="dashboard" id="overview">
        <header className="dashboard__topbar">
          <div>
            <span className="dashboard__eyebrow">Visão geral</span>
            <h1>Olá, {user.name.split(' ')[0]}.</h1>
          </div>

          <div className="user-chip">
            <div className="user-chip__avatar" aria-hidden="true">
              {initials(user.name)}
            </div>
            <div>
              <strong>{user.name}</strong>
              <span>{user.email}</span>
            </div>
          </div>
        </header>

        <section className="welcome-card">
          <div className="welcome-card__copy">
            <span className="section-kicker">Tudo pronto</span>
            <h2>Sua jornada no NexTask começou.</h2>
            <p>
              A autenticação está conectada à API e sua sessão foi iniciada com sucesso.
              O próximo passo será transformar seus planos em workspaces e quadros.
            </p>
          </div>
          <div className="welcome-card__visual" aria-hidden="true">
            <div className="orbit orbit--one" />
            <div className="orbit orbit--two" />
            <div className="visual-check">✓</div>
          </div>
        </section>

        <section className="status-grid" aria-label="Status da aplicação">
          <article className="status-card">
            <span className="status-card__icon status-card__icon--green" aria-hidden="true">
              ↔
            </span>
            <div>
              <span>Conexão</span>
              <strong>API integrada</strong>
            </div>
            <span className="status-dot">Ativa</span>
          </article>

          <article className="status-card">
            <span className="status-card__icon status-card__icon--blue" aria-hidden="true">
              ◇
            </span>
            <div>
              <span>Autenticação</span>
              <strong>Sessão JWT</strong>
            </div>
            <span className="status-dot">Protegida</span>
          </article>

          <article className="status-card">
            <span className="status-card__icon status-card__icon--violet" aria-hidden="true">
              ◌
            </span>
            <div>
              <span>Perfil</span>
              <strong>{user.email}</strong>
            </div>
            <span className="status-dot">Sincronizado</span>
          </article>
        </section>

        <section className="next-step-card">
          <div>
            <span className="section-kicker">Próximo incremento</span>
            <h2>Workspaces e quadro Kanban</h2>
            <p>
              A base de autenticação e navegação já está pronta para receber os dados de
              colaboração do backend.
            </p>
          </div>
          <span className="next-step-card__number" aria-hidden="true">
            09
          </span>
        </section>
      </main>
    </div>
  )
}
