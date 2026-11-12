// src/features/deployments/CreateDeploymentForm.tsx
import { Controller, useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { createDeployment } from '@/api/deployments'
import { createDeploymentSchema, type CreateDeploymentInput } from './schema'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import {
  Field, FieldError, FieldGroup, FieldLabel,
} from '@/components/ui/field'
import {
  Select, SelectContent, SelectItem, SelectTrigger, SelectValue,
} from '@/components/ui/select'

const EMPTY: CreateDeploymentInput = { serviceName: '', version: '', environment: 'DEV' }

export function CreateDeploymentForm() {
  const queryClient = useQueryClient()

  const form = useForm<CreateDeploymentInput>({
    resolver: zodResolver(createDeploymentSchema),
    defaultValues: EMPTY,
  })

  const mutation = useMutation({
    mutationFn: createDeployment,
    onSuccess: () => {
      form.reset(EMPTY)
      queryClient.invalidateQueries({ queryKey: ['deployments'] })
    },
    onError: (err) => {
      form.setError('root', {
        message: err instanceof Error ? err.message : 'Could not create deployment',
      })
    },
  })

  return (
    <form
      onSubmit={form.handleSubmit((values) => {
        form.clearErrors('root')
        mutation.mutate(values)
      })}
      className="max-w-md"
    >
      <FieldGroup>
        <Controller
          name="serviceName"
          control={form.control}
          render={({ field, fieldState }) => (
            <Field data-invalid={fieldState.invalid}>
              <FieldLabel htmlFor="serviceName">Service name</FieldLabel>
              <Input
                {...field}
                id="serviceName"
                aria-invalid={fieldState.invalid}
              />
              {fieldState.invalid && <FieldError errors={[fieldState.error]} />}
            </Field>
          )}
        />

        <Controller
          name="version"
          control={form.control}
          render={({ field, fieldState }) => (
            <Field data-invalid={fieldState.invalid}>
              <FieldLabel htmlFor="version">Version</FieldLabel>
              <Input
                {...field}
                id="version"
                aria-invalid={fieldState.invalid}
              />
              {fieldState.invalid && <FieldError errors={[fieldState.error]} />}
            </Field>
          )}
        />

        <Controller
          name="environment"
          control={form.control}
          render={({ field, fieldState }) => (
            <Field data-invalid={fieldState.invalid}>
              <FieldLabel htmlFor="environment">Environment</FieldLabel>
              <Select value={field.value} onValueChange={field.onChange}>
                <SelectTrigger id="environment" aria-invalid={fieldState.invalid}>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="DEV">DEV</SelectItem>
                  <SelectItem value="STAGING">STAGING</SelectItem>
                  <SelectItem value="PROD">PROD</SelectItem>
                </SelectContent>
              </Select>
              {fieldState.invalid && <FieldError errors={[fieldState.error]} />}
            </Field>
          )}
        />

        {form.formState.errors.root && (
          <FieldError errors={[form.formState.errors.root]} />
        )}

        <Button type="submit" disabled={mutation.isPending}>
          {mutation.isPending ? 'Creating…' : 'Create deployment'}
        </Button>
      </FieldGroup>
    </form>
  )
}
