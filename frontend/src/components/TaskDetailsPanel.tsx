import { useEffect, useState, type FormEvent } from 'react'

import {
  createComment,
  deleteComment,
  listComments,
  updateComment,
  type Comment,
} from '../api/comments'
import { ApiError, readableError } from '../api/client'
import type {
  Task,
  TaskPriority,
  TaskStatus,
  UpdateTaskInput,
} from '../api/tasks'
import type { WorkspaceMember } from '../api/workspaces'

interface TaskDetailsPanelProps {
  task: Task | null
  token: string
  members: WorkspaceMember[]
  currentUserId: number
  isWorkspaceOwner: boolean
  onClose: () => void
  onSave: (taskId: number, input: UpdateTaskInput) => Promise<void>
  onRequestError: (error: unknown) => void
}

const statusLabels: Record<TaskStatus, string> = {
  TODO: 'A fazer',
  IN_PROGRESS: 'Em andamento',
  DONE: 'Concluído',
}

function toLocalDateTime(value: string | null): string {
  if (!value) {
    return ''
  }

  const date = new Date(value)
  const offset = date.getTimezoneOffset() * 60_000
  return new Date(date.getTime() - offset).toISOString().slice(0, 16)
}

function formatDateTime(value: string): string {
  return new Intl.DateTimeFormat('pt-BR', {
    dateStyle: 'short',
    timeStyle: 'short',
  }).format(new Date(value))
}

export function TaskDetailsPanel({
  task,
  token,
  members,
  currentUserId,
  isWorkspaceOwner,
  onClose,
  onSave,
  onRequestError,
}: TaskDetailsPanelProps) {
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [status, setStatus] = useState<TaskStatus>('TODO')
  const [priority, setPriority] = useState<TaskPriority>('MEDIUM')
  const [assigneeId, setAssigneeId] = useState('')
  const [dueAt, setDueAt] = useState('')
  const [comments, setComments] = useState<Comment[]>([])
  const [newComment, setNewComment] = useState('')
  const [editingCommentId, setEditingCommentId] = useState<number | null>(null)
  const [editingContent, setEditingContent] = useState('')
  const [isLoadingComments, setIsLoadingComments] = useState(false)
  const [isSavingTask, setIsSavingTask] = useState(false)
  const [isSavingComment, setIsSavingComment] = useState(false)
  const [busyCommentId, setBusyCommentId] = useState<number | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)

  useEffect(() => {
    if (!task) {
      return
    }

    setTitle(task.title)
    setDescription(task.description ?? '')
    setStatus(task.status)
    setPriority(task.priority)
    setAssigneeId(task.assigneeId?.toString() ?? '')
    setDueAt(toLocalDateTime(task.dueAt))
  }, [task])

  useEffect(() => {
    if (task) {
      setError(null)
      setSuccess(null)
      setEditingCommentId(null)
      setEditingContent('')
      setNewComment('')
    }
  }, [task?.id])

  useEffect(() => {
    let active = true

    async function load() {
      if (!task) {
        setComments([])
        return
      }

      setIsLoadingComments(true)
      try {
        const response = await listComments(token, task.id)
        if (active) {
          setComments(response.content)
        }
      } catch (requestError) {
        if (active) {
          reportError(requestError)
        }
      } finally {
        if (active) {
          setIsLoadingComments(false)
        }
      }
    }

    void load()
    return () => {
      active = false
    }
  }, [task?.id, token])

  useEffect(() => {
    if (!task) {
      return undefined
    }

    function handleEscape(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        onClose()
      }
    }

    document.addEventListener('keydown', handleEscape)
    return () => document.removeEventListener('keydown', handleEscape)
  }, [onClose, task])

  function reportError(requestError: unknown) {
    if (requestError instanceof ApiError && requestError.status === 401) {
      onRequestError(requestError)
      return
    }
    setError(readableError(requestError))
  }

  if (!task) {
    return null
  }

  const activeTask = task

  async function handleSaveTask(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError(null)
    setSuccess(null)
    setIsSavingTask(true)

    try {
      await onSave(activeTask.id, {
        title,
        description,
        status,
        priority,
        position: activeTask.position,
        assigneeId: assigneeId ? Number(assigneeId) : null,
        dueAt: dueAt ? new Date(dueAt).toISOString() : null,
      })
      setSuccess('Detalhes atualizados.')
    } catch (requestError) {
      reportError(requestError)
    } finally {
      setIsSavingTask(false)
    }
  }

  async function handleCreateComment(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError(null)
    setIsSavingComment(true)

    try {
      const comment = await createComment(token, activeTask.id, newComment)
      setComments((current) => [...current, comment])
      setNewComment('')
    } catch (requestError) {
      reportError(requestError)
    } finally {
      setIsSavingComment(false)
    }
  }

  async function handleUpdateComment(commentId: number) {
    setError(null)
    setBusyCommentId(commentId)

    try {
      const updated = await updateComment(token, commentId, editingContent)
      setComments((current) =>
        current.map((comment) => (comment.id === updated.id ? updated : comment)),
      )
      setEditingCommentId(null)
      setEditingContent('')
    } catch (requestError) {
      reportError(requestError)
    } finally {
      setBusyCommentId(null)
    }
  }

  async function handleDeleteComment(commentId: number) {
    if (!window.confirm('Excluir este comentário?')) {
      return
    }

    setError(null)
    setBusyCommentId(commentId)
    try {
      await deleteComment(token, commentId)
      setComments((current) => current.filter((comment) => comment.id !== commentId))
    } catch (requestError) {
      reportError(requestError)
    } finally {
      setBusyCommentId(null)
    }
  }

  return (
    <div className="details-backdrop" onMouseDown={(event) => {
      if (event.target === event.currentTarget) {
        onClose()
      }
    }}>
      <aside
        className="task-details"
        role="dialog"
        aria-modal="true"
        aria-labelledby="task-details-title"
      >
        <header className="task-details__header">
          <div>
            <span>NT-{task.id}</span>
            <h2 id="task-details-title">Detalhes da tarefa</h2>
          </div>
          <button type="button" aria-label="Fechar detalhes" onClick={onClose}>×</button>
        </header>

        <div className="task-details__scroll">
          {error && <div className="form-alert" role="alert">{error}</div>}
          {success && <div className="success-alert" role="status">{success}</div>}

          <form className="task-editor" onSubmit={handleSaveTask}>
            <label className="field">
              <span>Título</span>
              <input
                value={title}
                onChange={(event) => setTitle(event.target.value)}
                maxLength={200}
                required
              />
            </label>

            <label className="field">
              <span>Descrição</span>
              <textarea
                value={description}
                onChange={(event) => setDescription(event.target.value)}
                maxLength={4000}
                rows={5}
              />
            </label>

            <div className="field-row">
              <label className="field">
                <span>Status</span>
                <select value={status} onChange={(event) => setStatus(event.target.value as TaskStatus)}>
                  {Object.entries(statusLabels).map(([value, label]) => (
                    <option key={value} value={value}>{label}</option>
                  ))}
                </select>
              </label>
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
            </div>

            <div className="field-row">
              <label className="field">
                <span>Responsável</span>
                <select value={assigneeId} onChange={(event) => setAssigneeId(event.target.value)}>
                  <option value="">Sem responsável</option>
                  {members.map((member) => (
                    <option key={member.userId} value={member.userId}>{member.name}</option>
                  ))}
                </select>
              </label>
              <label className="field">
                <span>Prazo</span>
                <input
                  type="datetime-local"
                  value={dueAt}
                  onChange={(event) => setDueAt(event.target.value)}
                />
              </label>
            </div>

            <button className="primary-button task-editor__save" type="submit" disabled={isSavingTask}>
              {isSavingTask ? 'Salvando…' : 'Salvar detalhes'}
            </button>
          </form>

          <section className="comments-section" aria-labelledby="comments-title">
            <header>
              <div>
                <span className="section-kicker">Colaboração</span>
                <h3 id="comments-title">Comentários</h3>
              </div>
              <span>{comments.length}</span>
            </header>

            <form className="comment-composer" onSubmit={handleCreateComment}>
              <label className="sr-only" htmlFor="new-comment">Novo comentário</label>
              <textarea
                id="new-comment"
                value={newComment}
                onChange={(event) => setNewComment(event.target.value)}
                placeholder="Compartilhe uma atualização…"
                maxLength={2000}
                rows={3}
                required
              />
              <button type="submit" disabled={isSavingComment}>
                {isSavingComment ? 'Enviando…' : 'Comentar'}
              </button>
            </form>

            {isLoadingComments ? (
              <p className="comments-empty">Carregando comentários…</p>
            ) : comments.length === 0 ? (
              <p className="comments-empty">Nenhum comentário ainda. Inicie a conversa.</p>
            ) : (
              <div className="comment-list">
                {comments.map((comment) => {
                  const canManage = comment.authorId === currentUserId || isWorkspaceOwner
                  const isEditing = editingCommentId === comment.id

                  return (
                    <article className="comment-card" key={comment.id}>
                      <div className="comment-card__avatar" aria-hidden="true">
                        {comment.authorName.slice(0, 1).toUpperCase()}
                      </div>
                      <div className="comment-card__body">
                        <header>
                          <strong>{comment.authorName}</strong>
                          <time dateTime={comment.createdAt}>{formatDateTime(comment.createdAt)}</time>
                        </header>
                        {isEditing ? (
                          <div className="comment-edit">
                            <label className="sr-only" htmlFor={`comment-${comment.id}`}>Editar comentário</label>
                            <textarea
                              id={`comment-${comment.id}`}
                              value={editingContent}
                              onChange={(event) => setEditingContent(event.target.value)}
                              maxLength={2000}
                              rows={3}
                            />
                            <div>
                              <button type="button" onClick={() => setEditingCommentId(null)}>Cancelar</button>
                              <button
                                type="button"
                                disabled={!editingContent.trim() || busyCommentId === comment.id}
                                onClick={() => void handleUpdateComment(comment.id)}
                              >
                                Salvar
                              </button>
                            </div>
                          </div>
                        ) : (
                          <p>{comment.content}</p>
                        )}
                        {canManage && !isEditing && (
                          <div className="comment-card__actions">
                            <button type="button" onClick={() => {
                              setEditingCommentId(comment.id)
                              setEditingContent(comment.content)
                            }}>Editar</button>
                            <button
                              type="button"
                              disabled={busyCommentId === comment.id}
                              onClick={() => void handleDeleteComment(comment.id)}
                            >Excluir</button>
                          </div>
                        )}
                      </div>
                    </article>
                  )
                })}
              </div>
            )}
          </section>
        </div>
      </aside>
    </div>
  )
}
