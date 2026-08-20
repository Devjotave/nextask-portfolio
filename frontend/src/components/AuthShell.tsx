import type { ReactNode } from 'react'

import { Brand } from './Brand'

interface AuthShellProps {
  eyebrow: string
  title: string
  description: string
  children: ReactNode
  footer: ReactNode
}

export function AuthShell({
  eyebrow,
  title,
  description,
  children,
  footer,
}: AuthShellProps) {
  return (
    <main className="auth-shell">
      <section className="auth-story" aria-label="Apresentação do NexTask">
        <Brand />

        <div className="auth-story__content">
          <span className="phase-badge">
            <span aria-hidden="true" />
            Sua rotina, em movimento
          </span>
          <h1>
            Menos ruído.
            <br />
            Mais <em>progresso.</em>
          </h1>
          <p>
            Um espaço direto para transformar planos em entregas e manter todo mundo
            na mesma direção.
          </p>
        </div>

        <div className="auth-story__proof" aria-label="Características da plataforma">
          <div>
            <strong>01</strong>
            <span>Espaços organizados</span>
          </div>
          <div>
            <strong>02</strong>
            <span>Prioridades visíveis</span>
          </div>
          <div>
            <strong>03</strong>
            <span>Equipe alinhada</span>
          </div>
        </div>
      </section>

      <section className="auth-panel">
        <div className="auth-panel__mobile-brand">
          <Brand compact />
        </div>

        <div className="auth-card">
          <header className="auth-card__header">
            <span>{eyebrow}</span>
            <h2>{title}</h2>
            <p>{description}</p>
          </header>

          {children}

          <footer className="auth-card__footer">{footer}</footer>
        </div>

        <p className="auth-panel__security">
          <span aria-hidden="true">◇</span> Sessão protegida com JWT
        </p>
      </section>
    </main>
  )
}
