import { authenticatedRequest } from './client'
import type { PageResponse } from './pagination'

export interface Project {
  id: number
  workspaceId: number
  name: string
  description: string | null
  createdById: number
  createdAt: string
  updatedAt: string
}

export interface CreateProjectInput {
  name: string
  description: string
}

export function listProjects(
  token: string,
  workspaceId: number,
): Promise<PageResponse<Project>> {
  return authenticatedRequest<PageResponse<Project>>(
    token,
    `/api/workspaces/${workspaceId}/projects?page=0&size=100`,
  )
}

export function createProject(
  token: string,
  workspaceId: number,
  input: CreateProjectInput,
): Promise<Project> {
  return authenticatedRequest<Project>(
    token,
    `/api/workspaces/${workspaceId}/projects`,
    {
      method: 'POST',
      body: JSON.stringify(input),
    },
  )
}
