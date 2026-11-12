// src/api/deployments.ts
import type { CreateDeploymentInput } from '@/features/deployments/schema'
import type { Deployment, PageResponse, TransitionTarget } from '@/types/deployment'
import { readErrorMessage } from './http'

export type { TransitionTarget } from '@/types/deployment'

export async function listDeployments(limit = 100, offset = 0): Promise<PageResponse<Deployment>> {
  const params = new URLSearchParams({ limit: String(limit), offset: String(offset) })
  const res = await fetch(`/api/deployments?${params}`)
  if (!res.ok) throw new Error(await readErrorMessage(res))
  return res.json()
}

export async function createDeployment(input: CreateDeploymentInput): Promise<Deployment> {
  const res = await fetch('/api/deployments', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  })
  if (!res.ok) throw new Error(await readErrorMessage(res))
  return res.json()
}

export async function transitionDeployment(
  id: string,
  transitionTo: TransitionTarget,
): Promise<Deployment> {
  const res = await fetch(`/api/deployments/${encodeURIComponent(id)}/status`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ transitionTo, endTime: new Date().toISOString() }),
  })
  if (!res.ok) throw new Error(await readErrorMessage(res))
  return res.json()
}