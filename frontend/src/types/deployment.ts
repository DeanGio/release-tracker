// src/types/deployment.ts
export type Environment = 'DEV' | 'STAGING' | 'PROD'
export type DeploymentStatus = 'IN_PROGRESS' | 'SUCCEEDED' | 'FAILED' // confirm against the Java enum
export type TransitionTarget = Exclude<DeploymentStatus, "IN_PROGRESS">;

export interface Deployment {
  id: string
  serviceName: string
  version: string
  environment: Environment
  status: DeploymentStatus
  startTime: string // ISO-8601 from Instant
  endTime: string | null
}

export interface PageResponse<T> {
  items: T[]
  limit: number
  offset: number
  total: number
  nextOffset: number | null
}
