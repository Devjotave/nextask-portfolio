import { useEffect, type MouseEvent, type PropsWithChildren } from 'react'

interface ModalProps extends PropsWithChildren {
  isOpen: boolean
  title: string
  description: string
  eyebrow?: string
  onClose: () => void
}

export function Modal({
  isOpen,
  title,
  description,
  eyebrow = 'Novo no quadro',
  onClose,
  children,
}: ModalProps) {
  useEffect(() => {
    if (!isOpen) {
      return undefined
    }

    function handleEscape(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        onClose()
      }
    }

    document.addEventListener('keydown', handleEscape)
    return () => document.removeEventListener('keydown', handleEscape)
  }, [isOpen, onClose])

  if (!isOpen) {
    return null
  }

  function handleBackdropClick(event: MouseEvent<HTMLDivElement>) {
    if (event.target === event.currentTarget) {
      onClose()
    }
  }

  return (
    <div className="modal-backdrop" onMouseDown={handleBackdropClick}>
      <section
        className="modal-card"
        role="dialog"
        aria-modal="true"
        aria-labelledby="modal-title"
        aria-describedby="modal-description"
      >
        <button
          className="modal-card__close"
          type="button"
          aria-label="Fechar"
          onClick={onClose}
        >
          ×
        </button>
        <header>
          <span className="section-kicker">{eyebrow}</span>
          <h2 id="modal-title">{title}</h2>
          <p id="modal-description">{description}</p>
        </header>
        {children}
      </section>
    </div>
  )
}
