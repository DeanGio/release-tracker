// src/features/deployments/schema.ts
import { z } from 'zod'

export const createDeploymentSchema = z.object({
  serviceName: z
    .string()
    .trim()
    .min(1, 'Service name is required')
    .max(100, 'Service name must be 100 characters or fewer'),
  version: z
    .string()
    .trim()
    .min(1, 'Version is required')
    .max(50, 'Version must be 50 characters or fewer'),
  environment: z.enum(['DEV', 'STAGING', 'PROD'], {
    error: 'Select an environment',
  }),
})

export type CreateDeploymentInput = z.infer<typeof createDeploymentSchema>
