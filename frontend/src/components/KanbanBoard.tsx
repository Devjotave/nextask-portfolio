import { useState, type DragEvent } from 'react'

import type { Task, TaskPriority, TaskStatus } from '../api/tasks'

interface KanbanBoardProps {
  tasks: Task[]
  movingTaskId: number | null
  onMove: (task: Task, status: TaskStatus) => void
  onOpen: (task: Task) => void
}

const columns: Array<{ status: TaskStatus; label: string; tone: string }> = [
  { status: 'TODO', label: 'A fazer', tone: 'slate' },
  { status: 'IN_PROGRESS', label: 'Em andamento', tone: 'violet' },
  { status: 'DONE', label: 'Concluído', tone: 'green' },
]

const priorityLabels: Record<TaskPriority, string> = {
  LOW: 'Baixa',
  MEDIUM: 'Média',
  HIGH: 'Alta',
}

function formatDueDate(value: string): string {
  return new Intl.DateTimeFormat('pt-BR', {
    day: '2-digit',
    month: 'short',
  }).format(new Date(value))
}

export function KanbanBoard({ tasks, movingTaskId, onMove, onOpen }: KanbanBoardProps) {
  const [draggedTaskId, setDraggedTaskId] = useState<number | null>(null)
  const [dragOverStatus, setDragOverStatus] = useState<TaskStatus | null>(null)

  function handleDrop(event: DragEvent<HTMLElement>, status: TaskStatus) {
    event.preventDefault()
    const task = tasks.find((item) => item.id === draggedTaskId)
    setDragOverStatus(null)
    setDraggedTaskId(null)

    if (task && task.status !== status) {
      onMove(task, status)
    }
  }

  return (
    <section className="kanban" aria-label="Quadro Kanban">
      {columns.map((column, columnIndex) => {
        const columnTasks = tasks
          .filter((task) => task.status === column.status)
          .sort((left, right) => left.position - right.position || left.id - right.id)

        return (
          <article
            className={`kanban-column kanban-column--${column.tone}${
              dragOverStatus === column.status ? ' kanban-column--drag-over' : ''
            }`}
            key={column.status}
            aria-labelledby={`kanban-column-${column.status}`}
            onDragOver={(event) => {
              event.preventDefault()
              setDragOverStatus(column.status)
            }}
            onDragLeave={() => setDragOverStatus(null)}
            onDrop={(event) => handleDrop(event, column.status)}
          >
            <header className="kanban-column__header">
              <div>
                <span className="kanban-column__dot" aria-hidden="true" />
                <h2 id={`kanban-column-${column.status}`}>{column.label}</h2>
              </div>
              <span aria-label={`${columnTasks.length} tarefas`}>{columnTasks.length}</span>
            </header>

            <div className="kanban-column__body">
              {columnTasks.map((task) => (
                <article
                  className={`task-card${movingTaskId === task.id ? ' task-card--moving' : ''}`}
                  draggable={movingTaskId !== task.id}
                  key={task.id}
                  onDragStart={() => setDraggedTaskId(task.id)}
                  onDragEnd={() => {
                    setDraggedTaskId(null)
                    setDragOverStatus(null)
                  }}
                >
                  <div className="task-card__meta">
                    <span className={`priority priority--${task.priority.toLowerCase()}`}>
                      {priorityLabels[task.priority]}
                    </span>
                    <span className="task-card__id">NT-{task.id}</span>
                  </div>
                  <h3>
                    <button type="button" onClick={() => onOpen(task)}>
                      {task.title}
                    </button>
                  </h3>
                  {task.description && <p>{task.description}</p>}
                  <footer>
                    <span>{task.dueAt ? `Prazo ${formatDueDate(task.dueAt)}` : 'Sem prazo'}</span>
                    <div className="task-card__moves">
                      {columnIndex > 0 && (
                        <button
                          type="button"
                          aria-label={`Mover ${task.title} para ${columns[columnIndex - 1].label}`}
                          title={`Mover para ${columns[columnIndex - 1].label}`}
                          disabled={movingTaskId === task.id}
                          onClick={() => onMove(task, columns[columnIndex - 1].status)}
                        >
                          ←
                        </button>
                      )}
                      {columnIndex < columns.length - 1 && (
                        <button
                          type="button"
                          aria-label={`Mover ${task.title} para ${columns[columnIndex + 1].label}`}
                          title={`Mover para ${columns[columnIndex + 1].label}`}
                          disabled={movingTaskId === task.id}
                          onClick={() => onMove(task, columns[columnIndex + 1].status)}
                        >
                          →
                        </button>
                      )}
                    </div>
                  </footer>
                </article>
              ))}

              {columnTasks.length === 0 && (
                <div className="kanban-column__empty">
                  <span aria-hidden="true">＋</span>
                  <p>Arraste uma tarefa para cá</p>
                </div>
              )}
            </div>
          </article>
        )
      })}
    </section>
  )
}
