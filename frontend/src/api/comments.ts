import { authenticatedRequest } from './client'
import type { PageResponse } from './pagination'

export interface Comment {
  id: number
  taskId: number
  authorId: number
  authorName: string
  content: string
  createdAt: string
  updatedAt: string
}

export function listComments(
  token: string,
  taskId: number,
): Promise<PageResponse<Comment>> {
  return authenticatedRequest<PageResponse<Comment>>(
    token,
    `/api/tasks/${taskId}/comments?page=0&size=100`,
  )
}

export function createComment(
  token: string,
  taskId: number,
  content: string,
): Promise<Comment> {
  return authenticatedRequest<Comment>(token, `/api/tasks/${taskId}/comments`, {
    method: 'POST',
    body: JSON.stringify({ content }),
  })
}

export function updateComment(
  token: string,
  commentId: number,
  content: string,
): Promise<Comment> {
  return authenticatedRequest<Comment>(token, `/api/comments/${commentId}`, {
    method: 'PUT',
    body: JSON.stringify({ content }),
  })
}

export function deleteComment(token: string, commentId: number): Promise<void> {
  return authenticatedRequest<void>(token, `/api/comments/${commentId}`, {
    method: 'DELETE',
  })
}
