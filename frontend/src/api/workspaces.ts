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

export interface WorkspaceMember {
  userId: number
  name: string
  email: string
  role: WorkspaceRole
  joinedAt: string
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

export function listWorkspaceMembers(
  token: string,
  workspaceId: number,
): Promise<PageResponse<WorkspaceMember>> {
  return authenticatedRequest<PageResponse<WorkspaceMember>>(
    token,
    `/api/workspaces/${workspaceId}/members?page=0&size=100`,
  )
}

export function addWorkspaceMember(
  token: string,
  workspaceId: number,
  userId: number,
  role: WorkspaceRole,
): Promise<WorkspaceMember> {
  return authenticatedRequest<WorkspaceMember>(
    token,
    `/api/workspaces/${workspaceId}/members`,
    {
      method: 'POST',
      body: JSON.stringify({ userId, role }),
    },
  )
}

export function updateWorkspaceMember(
  token: string,
  workspaceId: number,
  userId: number,
  role: WorkspaceRole,
): Promise<WorkspaceMember> {
  return authenticatedRequest<WorkspaceMember>(
    token,
    `/api/workspaces/${workspaceId}/members/${userId}`,
    {
      method: 'PUT',
      body: JSON.stringify({ role }),
    },
  )
}

export function removeWorkspaceMember(
  token: string,
  workspaceId: number,
  userId: number,
): Promise<void> {
  return authenticatedRequest<void>(
    token,
    `/api/workspaces/${workspaceId}/members/${userId}`,
    { method: 'DELETE' },
  )
}
