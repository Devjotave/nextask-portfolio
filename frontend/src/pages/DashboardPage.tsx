import { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router'

import { ApiError, readableError } from '../api/client'
import {
  createProject,
  listProjects,
  type CreateProjectInput,
  type Project,
} from '../api/projects'
import {
  createTask,
  listTasks,
  updateTask,
  type CreateTaskInput,
  type Task,
  type TaskStatus,
} from '../api/tasks'
import {
  createWorkspace,
  listWorkspaces,
  type Workspace,
} from '../api/workspaces'
import { useAuth } from '../auth/AuthContext'
import { Brand } from '../components/Brand'
import {
  CreateProjectModal,
  CreateTaskModal,
  CreateWorkspaceModal,
} from '../components/CreateModals'
import { KanbanBoard } from '../components/KanbanBoard'

function initials(name: string): string {
  return name
    .trim()
    .split(/\s+/)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase())
    .join('')
}

function nextTodoPosition(tasks: Task[]): number {
  return (
    tasks
      .filter((task) => task.status === 'TODO')
      .reduce((highest, task) => Math.max(highest, task.position), -1) + 1
  )
}

export function DashboardPage() {
  const { user, token, logout } = useAuth()
  const navigate = useNavigate()
  const [workspaces, setWorkspaces] = useState<Workspace[]>([])
  const [projects, setProjects] = useState<Project[]>([])
  const [tasks, setTasks] = useState<Task[]>([])
  const [selectedWorkspaceId, setSelectedWorkspaceId] = useState<number | null>(null)
  const [selectedProjectId, setSelectedProjectId] = useState<number | null>(null)
  const [isLoadingWorkspaces, setIsLoadingWorkspaces] = useState(true)
  const [isLoadingProjects, setIsLoadingProjects] = useState(false)
  const [isLoadingTasks, setIsLoadingTasks] = useState(false)
  const [movingTaskId, setMovingTaskId] = useState<number | null>(null)
  const [pageError, setPageError] = useState<string | null>(null)
  const [isWorkspaceModalOpen, setIsWorkspaceModalOpen] = useState(false)
  const [isProjectModalOpen, setIsProjectModalOpen] = useState(false)
  const [isTaskModalOpen, setIsTaskModalOpen] = useState(false)

  const selectedWorkspace = useMemo(
    () => workspaces.find((workspace) => workspace.id === selectedWorkspaceId) ?? null,
    [selectedWorkspaceId, workspaces],
  )
  const selectedProject = useMemo(
    () => projects.find((project) => project.id === selectedProjectId) ?? null,
    [projects, selectedProjectId],
  )

  const handleRequestError = useCallback(
    (error: unknown) => {
      if (error instanceof ApiError && error.status === 401) {
        logout()
        navigate('/login', { replace: true })
        return
      }

      setPageError(readableError(error))
    },
    [logout, navigate],
  )

  const refreshTasks = useCallback(async () => {
    if (!token || selectedProjectId === null) {
      setTasks([])
      return
    }

    setIsLoadingTasks(true)
    try {
      const response = await listTasks(token, selectedProjectId)
      setTasks(response.content)
    } catch (error) {
      handleRequestError(error)
    } finally {
      setIsLoadingTasks(false)
    }
  }, [handleRequestError, selectedProjectId, token])

  useEffect(() => {
    let active = true

    async function load() {
      if (!token) {
        return
      }

      setIsLoadingWorkspaces(true)
      try {
        const response = await listWorkspaces(token)
        if (!active) {
          return
        }
        setWorkspaces(response.content)
        setSelectedWorkspaceId((current) =>
          current && response.content.some((workspace) => workspace.id === current)
            ? current
            : (response.content[0]?.id ?? null),
        )
      } catch (error) {
        if (active) {
          handleRequestError(error)
        }
      } finally {
        if (active) {
          setIsLoadingWorkspaces(false)
        }
      }
    }

    void load()
    return () => {
      active = false
    }
  }, [handleRequestError, token])

  useEffect(() => {
    let active = true

    async function load() {
      if (!token || selectedWorkspaceId === null) {
        setProjects([])
        setSelectedProjectId(null)
        return
      }

      setIsLoadingProjects(true)
      setProjects([])
      setSelectedProjectId(null)
      setTasks([])

      try {
        const response = await listProjects(token, selectedWorkspaceId)
        if (!active) {
          return
        }
        setProjects(response.content)
        setSelectedProjectId(response.content[0]?.id ?? null)
      } catch (error) {
        if (active) {
          handleRequestError(error)
        }
      } finally {
        if (active) {
          setIsLoadingProjects(false)
        }
      }
    }

    void load()
    return () => {
      active = false
    }
  }, [handleRequestError, selectedWorkspaceId, token])

  useEffect(() => {
    setTasks([])
    void refreshTasks()
  }, [refreshTasks])

  if (!user || !token) {
    return null
  }

  const authenticatedToken = token

  function handleLogout() {
    logout()
    navigate('/login', { replace: true })
  }

  async function handleCreateWorkspace(name: string) {
    try {
      const workspace = await createWorkspace(authenticatedToken, name)
      setWorkspaces((current) => [...current, workspace])
      setSelectedWorkspaceId(workspace.id)
      setPageError(null)
    } catch (error) {
      handleRequestError(error)
      throw error
    }
  }

  async function handleCreateProject(input: CreateProjectInput) {
    if (selectedWorkspaceId === null) {
      return
    }

    try {
      const project = await createProject(authenticatedToken, selectedWorkspaceId, input)
      setProjects((current) => [...current, project])
      setSelectedProjectId(project.id)
      setPageError(null)
    } catch (error) {
      handleRequestError(error)
      throw error
    }
  }

  async function handleCreateTask(input: Omit<CreateTaskInput, 'position'>) {
    if (selectedProjectId === null) {
      return
    }

    try {
      const task = await createTask(authenticatedToken, selectedProjectId, {
        ...input,
        position: nextTodoPosition(tasks),
      })
      setTasks((current) => [...current, task])
      setPageError(null)
    } catch (error) {
      handleRequestError(error)
      throw error
    }
  }

  async function handleMoveTask(task: Task, status: TaskStatus) {
    if (task.status === status || movingTaskId !== null) {
      return
    }

    const position = tasks.filter((item) => item.status === status).length
    setMovingTaskId(task.id)
    setPageError(null)

    try {
      const updatedTask = await updateTask(authenticatedToken, task.id, {
        title: task.title,
        description: task.description ?? '',
        status,
        priority: task.priority,
        position,
        assigneeId: task.assigneeId,
        dueAt: task.dueAt,
      })
      setTasks((current) =>
        current.map((item) => (item.id === updatedTask.id ? updatedTask : item)),
      )
    } catch (error) {
      handleRequestError(error)
      if (error instanceof ApiError && error.status === 409) {
        await refreshTasks()
      }
    } finally {
      setMovingTaskId(null)
    }
  }

  return (
    <div className="app-shell app-shell--kanban">
      <aside className="sidebar workspace-sidebar">
        <Brand compact />

        <nav className="sidebar__nav" aria-label="Navegação principal">
          <a className="nav-item nav-item--active" href="#board" aria-current="page">
            <span aria-hidden="true">▦</span>
            Quadro
          </a>
        </nav>

        <div className="workspace-sidebar__section">
          <div className="workspace-sidebar__heading">
            <span>Workspaces</span>
            <button
              type="button"
              aria-label="Criar workspace"
              onClick={() => setIsWorkspaceModalOpen(true)}
            >
              +
            </button>
          </div>
          <div className="workspace-sidebar__list">
            {workspaces.map((workspace) => (
              <button
                className={workspace.id === selectedWorkspaceId ? 'is-active' : ''}
                type="button"
                key={workspace.id}
                onClick={() => setSelectedWorkspaceId(workspace.id)}
              >
                <span aria-hidden="true">{workspace.name.slice(0, 1).toUpperCase()}</span>
                <span>{workspace.name}</span>
              </button>
            ))}
            {!isLoadingWorkspaces && workspaces.length === 0 && (
              <p>Nenhum espaço criado</p>
            )}
          </div>
        </div>

        <div className="sidebar__phase">
          <span>Fase 9</span>
          <strong>Kanban integrado</strong>
          <div className="phase-progress" aria-label="Progresso da fase concluído">
            <span />
          </div>
        </div>

        <button className="sidebar__logout" type="button" onClick={handleLogout}>
          <span aria-hidden="true">↗</span>
          Sair da conta
        </button>
      </aside>

      <main className="dashboard kanban-page" id="board">
        <header className="dashboard__topbar">
          <div>
            <span className="dashboard__eyebrow">Área de trabalho</span>
            <h1>Quadro Kanban</h1>
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

        {pageError && (
          <div className="page-alert" role="alert">
            <span>{pageError}</span>
            <button type="button" aria-label="Fechar aviso" onClick={() => setPageError(null)}>
              ×
            </button>
          </div>
        )}

        {isLoadingWorkspaces ? (
          <LoadingState label="Carregando seus workspaces…" />
        ) : workspaces.length === 0 ? (
          <EmptyState
            eyebrow="Seu primeiro espaço"
            title="Crie um workspace para começar"
            description="Workspaces organizam projetos, tarefas e pessoas em um único lugar."
            action="Criar workspace"
            onAction={() => setIsWorkspaceModalOpen(true)}
          />
        ) : (
          <>
            <section className="board-toolbar" aria-label="Controles do quadro">
              <div className="board-toolbar__selectors">
                <label>
                  <span>Workspace</span>
                  <select
                    value={selectedWorkspaceId ?? ''}
                    onChange={(event) => setSelectedWorkspaceId(Number(event.target.value))}
                  >
                    {workspaces.map((workspace) => (
                      <option key={workspace.id} value={workspace.id}>
                        {workspace.name}
                      </option>
                    ))}
                  </select>
                </label>

                <span className="toolbar-separator" aria-hidden="true">/</span>

                <label>
                  <span>Projeto</span>
                  <select
                    value={selectedProjectId ?? ''}
                    disabled={projects.length === 0}
                    onChange={(event) => setSelectedProjectId(Number(event.target.value))}
                  >
                    {projects.length === 0 && <option value="">Nenhum projeto</option>}
                    {projects.map((project) => (
                      <option key={project.id} value={project.id}>
                        {project.name}
                      </option>
                    ))}
                  </select>
                </label>
              </div>

              <div className="board-toolbar__actions">
                <button
                  className="secondary-button"
                  type="button"
                  onClick={() => setIsProjectModalOpen(true)}
                >
                  + Projeto
                </button>
                {selectedProject && (
                  <button
                    className="primary-button primary-button--compact"
                    type="button"
                    onClick={() => setIsTaskModalOpen(true)}
                  >
                    <span>+ Nova tarefa</span>
                  </button>
                )}
              </div>
            </section>

            {isLoadingProjects ? (
              <LoadingState label="Carregando projetos…" />
            ) : projects.length === 0 ? (
              <EmptyState
                eyebrow={selectedWorkspace?.role === 'OWNER' ? 'Workspace criado' : 'Workspace vazio'}
                title="Crie o primeiro projeto"
                description="Um projeto reúne as tarefas que avançam juntas pelo quadro."
                action="Criar projeto"
                onAction={() => setIsProjectModalOpen(true)}
              />
            ) : (
              <>
                <section className="project-heading">
                  <div>
                    <span className="section-kicker">
                      {selectedWorkspace?.name} · {selectedWorkspace?.role === 'OWNER' ? 'Proprietário' : 'Membro'}
                    </span>
                    <h2>{selectedProject?.name}</h2>
                    {selectedProject?.description && <p>{selectedProject.description}</p>}
                  </div>
                  <div className="project-heading__count">
                    <strong>{tasks.length}</strong>
                    <span>{tasks.length === 1 ? 'tarefa' : 'tarefas'}</span>
                  </div>
                </section>

                {isLoadingTasks ? (
                  <LoadingState label="Montando o quadro…" />
                ) : (
                  <KanbanBoard
                    tasks={tasks}
                    movingTaskId={movingTaskId}
                    onMove={handleMoveTask}
                  />
                )}
              </>
            )}
          </>
        )}
      </main>

      <CreateWorkspaceModal
        isOpen={isWorkspaceModalOpen}
        onClose={() => setIsWorkspaceModalOpen(false)}
        onCreate={handleCreateWorkspace}
      />
      <CreateProjectModal
        isOpen={isProjectModalOpen}
        onClose={() => setIsProjectModalOpen(false)}
        onCreate={handleCreateProject}
      />
      <CreateTaskModal
        isOpen={isTaskModalOpen}
        onClose={() => setIsTaskModalOpen(false)}
        onCreate={handleCreateTask}
      />
    </div>
  )
}

interface LoadingStateProps {
  label: string
}

function LoadingState({ label }: LoadingStateProps) {
  return (
    <div className="loading-state" role="status">
      <span aria-hidden="true" />
      <p>{label}</p>
    </div>
  )
}

interface EmptyStateProps {
  eyebrow: string
  title: string
  description: string
  action: string
  onAction: () => void
}

function EmptyState({ eyebrow, title, description, action, onAction }: EmptyStateProps) {
  return (
    <section className="empty-state">
      <div className="empty-state__visual" aria-hidden="true">
        <span />
        <span />
        <span />
      </div>
      <span className="section-kicker">{eyebrow}</span>
      <h2>{title}</h2>
      <p>{description}</p>
      <button className="primary-button" type="button" onClick={onAction}>
        <span>{action}</span>
        <span aria-hidden="true">→</span>
      </button>
    </section>
  )
}
