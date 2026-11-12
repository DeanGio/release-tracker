// src/api/mutations.ts
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { transitionDeployment, type TransitionTarget } from "./deployments";

export function useTransitionDeployment() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, transitionTo }: { id: string; transitionTo: TransitionTarget }) =>
      transitionDeployment(id, transitionTo),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["deployments"] }),
  });
}
