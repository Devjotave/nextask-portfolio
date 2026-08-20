import { useEffect, useState, type FormEvent } from 'react'

import { ApiError, readableError } from '../api/client'
import {
  addWorkspaceMember,
  removeWorkspaceMember,
  updateWorkspaceMember,
  type Workspace,
  type WorkspaceMember,
  type WorkspaceRole,
} from '../api/workspaces'
import { Modal } from './Modal'

interface WorkspaceMembersModalProps {
  isOpen: boolean
  token: string
  workspace: Workspace | null
  members: WorkspaceMember[]
  currentUserId: number
  onClose: () => void
  onMembersChange: (members: WorkspaceMember[]) => void
  onRequestError: (error: unknown) => void
}

export function WorkspaceMembersModal({
  isOpen,
  token,
  workspace,
  members,
  currentUserId,
  onClose,
  onMembersChange,
  onRequestError,
}: WorkspaceMembersModalProps) {
  const [userId, setUserId] = useState('')
  const [role, setRole] = useState<WorkspaceRole>('MEMBER')
  const [busyUserId, setBusyUserId] = useState<number | null>(null)
  const [isAdding, setIsAdding] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (isOpen) {
      setUserId('')
      setRole('MEMBER')
      setError(null)
    }
  }, [isOpen])

  const isOwner = workspace?.role === 'OWNER'

  function reportError(requestError: unknown) {
    if (requestError instanceof ApiError && requestError.status === 401) {
      onRequestError(requestError)
      return
    }
    setError(readableError(requestError))
  }

  async function handleAdd(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!workspace) {
      return
    }

    setError(null)
    setIsAdding(true)
    try {
      const member = await addWorkspaceMember(token, workspace.id, Number(userId), role)
      onMembersChange([...members, member])
      setUserId('')
      setRole('MEMBER')
    } catch (requestError) {
      reportError(requestError)
    } finally {
      setIsAdding(false)
    }
  }

  async function handleRoleChange(member: WorkspaceMember, nextRole: WorkspaceRole) {
    if (!workspace || member.role === nextRole) {
      return
    }

    setError(null)
    setBusyUserId(member.userId)
    try {
      const updated = await updateWorkspaceMember(
        token,
        workspace.id,
        member.userId,
        nextRole,
      )
      onMembersChange(
        members.map((item) => (item.userId === updated.userId ? updated : item)),
      )
    } catch (requestError) {
      reportError(requestError)
    } finally {
      setBusyUserId(null)
    }
  }

  async function handleRemove(member: WorkspaceMember) {
    if (!workspace || !window.confirm(`Remover ${member.name} deste workspace?`)) {
      return
    }

    setError(null)
    setBusyUserId(member.userId)
    try {
      await removeWorkspaceMember(token, workspace.id, member.userId)
      onMembersChange(members.filter((item) => item.userId !== member.userId))
    } catch (requestError) {
      reportError(requestError)
    } finally {
      setBusyUserId(null)
    }
  }

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      eyebrow="Colaboração"
      title="Membros do workspace"
      description={workspace ? `Gerencie quem participa de ${workspace.name}.` : 'Gerencie o time.'}
    >
      <div className="members-modal">
        {error && <div className="form-alert" role="alert">{error}</div>}

        <div className="member-identity">
          <span>Seu ID de usuário</span>
          <strong>{currentUserId}</strong>
          <p>Compartilhe este número com um proprietário para entrar em outro workspace.</p>
        </div>

        {isOwner && (
          <form className="member-add-form" onSubmit={handleAdd}>
            <label className="field">
              <span>ID da pessoa</span>
              <input
                type="number"
                min="1"
                step="1"
                value={userId}
                onChange={(event) => setUserId(event.target.value)}
                placeholder="Ex.: 12"
                required
              />
            </label>
            <label className="field">
              <span>Papel inicial</span>
              <select value={role} onChange={(event) => setRole(event.target.value as WorkspaceRole)}>
                <option value="MEMBER">Membro</option>
                <option value="OWNER">Proprietário</option>
              </select>
            </label>
            <button type="submit" disabled={isAdding}>{isAdding ? 'Adicionando…' : 'Adicionar'}</button>
          </form>
        )}

        <div className="member-list" aria-label="Pessoas no workspace">
          {members.map((member) => (
            <article className="member-row" key={member.userId}>
              <span className="member-row__avatar" aria-hidden="true">
                {member.name.slice(0, 1).toUpperCase()}
              </span>
              <div className="member-row__identity">
                <strong>{member.name}{member.userId === currentUserId ? ' (você)' : ''}</strong>
                <span>{member.email}</span>
              </div>
              {isOwner ? (
                <select
                  aria-label={`Papel de ${member.name}`}
                  value={member.role}
                  disabled={busyUserId === member.userId}
                  onChange={(event) => void handleRoleChange(member, event.target.value as WorkspaceRole)}
                >
                  <option value="MEMBER">Membro</option>
                  <option value="OWNER">Proprietário</option>
                </select>
              ) : (
                <span className="member-role">{member.role === 'OWNER' ? 'Proprietário' : 'Membro'}</span>
              )}
              {isOwner && (
                <button
                  className="member-row__remove"
                  type="button"
                  aria-label={`Remover ${member.name}`}
                  title={
                    member.userId === currentUserId
                      ? 'Sua própria saída não é gerenciada por esta tela'
                      : `Remover ${member.name}`
                  }
                  disabled={
                    busyUserId === member.userId || member.userId === currentUserId
                  }
                  onClick={() => void handleRemove(member)}
                >
                  ×
                </button>
              )}
            </article>
          ))}
        </div>
      </div>
    </Modal>
  )
}
