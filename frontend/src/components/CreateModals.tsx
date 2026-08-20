import { useEffect, useState, type FormEvent } from 'react'

import type { CreateProjectInput } from '../api/projects'
import type { CreateTaskInput, TaskPriority } from '../api/tasks'
import { readableError } from '../api/client'
import { Modal } from './Modal'

interface BaseCreateModalProps {
  isOpen: boolean
  onClose: () => void
}

interface CreateWorkspaceModalProps extends BaseCreateModalProps {
  onCreate: (name: string) => Promise<void>
}

export function CreateWorkspaceModal({
  isOpen,
  onClose,
  onCreate,
}: CreateWorkspaceModalProps) {
  const [name, setName] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  useEffect(() => {
    if (isOpen) {
      setName('')
      setError(null)
    }
  }, [isOpen])

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError(null)
    setIsSubmitting(true)

    try {
      await onCreate(name)
      onClose()
    } catch (requestError) {
      setError(readableError(requestError))
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Criar workspace"
      description="Reúna projetos e pessoas em um mesmo espaço."
    >
      <form className="entity-form" onSubmit={handleSubmit}>
        {error && <div className="form-alert" role="alert">{error}</div>}
        <label className="field">
          <span>Nome do workspace</span>
          <input
            autoFocus
            value={name}
            onChange={(event) => setName(event.target.value)}
            placeholder="Ex.: Produto NexTask"
            maxLength={120}
            required
          />
        </label>
        <FormActions onCancel={onClose} isSubmitting={isSubmitting} label="Criar workspace" />
      </form>
    </Modal>
  )
}

interface CreateProjectModalProps extends BaseCreateModalProps {
  onCreate: (input: CreateProjectInput) => Promise<void>
}

export function CreateProjectModal({
  isOpen,
  onClose,
  onCreate,
}: CreateProjectModalProps) {
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  useEffect(() => {
    if (isOpen) {
      setName('')
      setDescription('')
      setError(null)
    }
  }, [isOpen])

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError(null)
    setIsSubmitting(true)

    try {
      await onCreate({ name, description })
      onClose()
    } catch (requestError) {
      setError(readableError(requestError))
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Criar projeto"
      description="Defina o objetivo que será acompanhado neste quadro."
    >
      <form className="entity-form" onSubmit={handleSubmit}>
        {error && <div className="form-alert" role="alert">{error}</div>}
        <label className="field">
          <span>Nome do projeto</span>
          <input
            autoFocus
            value={name}
            onChange={(event) => setName(event.target.value)}
            placeholder="Ex.: Lançamento do MVP"
            maxLength={120}
            required
          />
        </label>
        <label className="field">
          <span>Descrição <small>opcional</small></span>
          <textarea
            value={description}
            onChange={(event) => setDescription(event.target.value)}
            placeholder="O que este projeto precisa entregar?"
            maxLength={1000}
            rows={4}
          />
        </label>
        <FormActions onCancel={onClose} isSubmitting={isSubmitting} label="Criar projeto" />
      </form>
    </Modal>
  )
}

interface CreateTaskModalProps extends BaseCreateModalProps {
  onCreate: (input: Omit<CreateTaskInput, 'position'>) => Promise<void>
}

export function CreateTaskModal({ isOpen, onClose, onCreate }: CreateTaskModalProps) {
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [priority, setPriority] = useState<TaskPriority>('MEDIUM')
  const [dueAt, setDueAt] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  useEffect(() => {
    if (isOpen) {
      setTitle('')
      setDescription('')
      setPriority('MEDIUM')
      setDueAt('')
      setError(null)
    }
  }, [isOpen])

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError(null)
    setIsSubmitting(true)

    try {
      await onCreate({
        title,
        description,
        priority,
        assigneeId: null,
        dueAt: dueAt ? new Date(dueAt).toISOString() : null,
      })
      onClose()
    } catch (requestError) {
      setError(readableError(requestError))
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Criar tarefa"
      description="Adicione uma entrega à coluna A fazer."
    >
      <form className="entity-form" onSubmit={handleSubmit}>
        {error && <div className="form-alert" role="alert">{error}</div>}
        <label className="field">
          <span>Título da tarefa</span>
          <input
            autoFocus
            value={title}
            onChange={(event) => setTitle(event.target.value)}
            placeholder="Ex.: Revisar fluxo de cadastro"
            maxLength={200}
            required
          />
        </label>
        <label className="field">
          <span>Descrição <small>opcional</small></span>
          <textarea
            value={description}
            onChange={(event) => setDescription(event.target.value)}
            placeholder="Contexto, critérios e observações"
            maxLength={4000}
            rows={4}
          />
        </label>
        <div className="field-row">
          <label className="field">
            <span>Prioridade</span>
            <select
              value={priority}
              onChange={(event) => setPriority(event.target.value as TaskPriority)}
            >
              <option value="LOW">Baixa</option>
              <option value="MEDIUM">Média</option>
              <option value="HIGH">Alta</option>
            </select>
          </label>
          <label className="field">
            <span>Prazo <small>opcional</small></span>
            <input
              type="datetime-local"
              value={dueAt}
              onChange={(event) => setDueAt(event.target.value)}
            />
          </label>
        </div>
        <FormActions onCancel={onClose} isSubmitting={isSubmitting} label="Criar tarefa" />
      </form>
    </Modal>
  )
}

interface FormActionsProps {
  onCancel: () => void
  isSubmitting: boolean
  label: string
}

function FormActions({ onCancel, isSubmitting, label }: FormActionsProps) {
  return (
    <div className="entity-form__actions">
      <button className="secondary-button" type="button" onClick={onCancel}>
        Cancelar
      </button>
      <button className="primary-button" type="submit" disabled={isSubmitting}>
        <span>{isSubmitting ? 'Salvando…' : label}</span>
        {!isSubmitting && <span aria-hidden="true">→</span>}
      </button>
    </div>
  )
}
