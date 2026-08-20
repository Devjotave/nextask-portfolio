import { authenticatedRequest } from './client'
import type { PageResponse } from './pagination'

export type WorkspaceRole = 'OWNER' | 'MEMBER'

export interface Workspace {
  id: number
  name: string
  role: WorkspaceRole
  createdAt: string
  updatedAt: string
}

interface WorkspaceResponse {
  id: number
  name: string
  createdAt: string
  updatedAt: string
}

export function listWorkspaces(token: string): Promise<PageResponse<Workspace>> {
  return authenticatedRequest<PageResponse<Workspace>>(
    token,
    '/api/workspaces?page=0&size=100',
  )
}

export async function createWorkspace(token: string, name: string): Promise<Workspace> {
  const workspace = await authenticatedRequest<WorkspaceResponse>(
    token,
    '/api/workspaces',
    {
      method: 'POST',
      body: JSON.stringify({ name }),
    },
  )

  return { ...workspace, role: 'OWNER' }
}
