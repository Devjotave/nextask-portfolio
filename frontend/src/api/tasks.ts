import { authenticatedRequest } from './client'
import type { PageResponse } from './pagination'

export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'DONE'
export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH'

export interface Task {
  id: number
  projectId: number
  title: string
  description: string | null
  status: TaskStatus
  priority: TaskPriority
  position: number
  assigneeId: number | null
  createdById: number
  dueAt: string | null
  version: number
  createdAt: string
  updatedAt: string
}

export interface CreateTaskInput {
  title: string
  description: string
  priority: TaskPriority
  position: number
  assigneeId: number | null
  dueAt: string | null
}

export interface UpdateTaskInput extends CreateTaskInput {
  status: TaskStatus
}

export function listTasks(
  token: string,
  projectId: number,
): Promise<PageResponse<Task>> {
  return authenticatedRequest<PageResponse<Task>>(
    token,
    `/api/projects/${projectId}/tasks?page=0&size=100`,
  )
}

export function createTask(
  token: string,
  projectId: number,
  input: CreateTaskInput,
): Promise<Task> {
  return authenticatedRequest<Task>(token, `/api/projects/${projectId}/tasks`, {
    method: 'POST',
    body: JSON.stringify(input),
  })
}

export function updateTask(
  token: string,
  taskId: number,
  input: UpdateTaskInput,
): Promise<Task> {
  return authenticatedRequest<Task>(token, `/api/tasks/${taskId}`, {
    method: 'PUT',
    body: JSON.stringify(input),
  })
}
