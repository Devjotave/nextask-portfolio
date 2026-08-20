import { Link } from 'react-router'

import { Brand } from '../components/Brand'

export function NotFoundPage() {
  return (
    <main className="not-found">
      <Brand />
      <span>404</span>
      <h1>Esta página saiu do quadro.</h1>
      <p>O endereço não existe ou foi movido para outro lugar.</p>
      <Link className="primary-button" to="/">
        Voltar ao início <span aria-hidden="true">→</span>
      </Link>
    </main>
  )
}
