// src/api/queries.ts
import type { Deployment, PageResponse } from "@/types/deployment";
import { useQuery, keepPreviousData } from "@tanstack/react-query";

export async function fetchDeployments(limit: number, offset: number): Promise<PageResponse<Deployment>> {
  const res = await fetch(`/api/deployments?limit=${limit}&offset=${offset}`);
  if (!res.ok) {
    // The backend sends its 400 and 404 messages as text/plain.
    const detail = (await res.text()).trim();
    throw new Error(detail || `Failed to load deployments (${res.status})`);
  }
  return res.json();
}

export function useDeploymentsQuery(limit: number, offset: number) {
  return useQuery({
    queryKey: ["deployments", "list", limit, offset],
    queryFn: () => fetchDeployments(limit, offset),
    placeholderData: keepPreviousData,
  });
}

const PAGE_SIZE = 100;

async function fetchAllDeployments(): Promise<Deployment[]> {
  const all: Deployment[] = [];
  let offset: number | null = 0;

  while (offset !== null) {
    const page = await fetchDeployments(PAGE_SIZE, offset);
    all.push(...page.items);
    // Stop on the server's signal. The empty-page check guards against an infinite loop.
    if (page.items.length === 0) break;
    offset = page.nextOffset;
  }

  return all;
}

export function useAllDeployments() {
  return useQuery({
    queryKey: ["deployments", "all"],
    queryFn: fetchAllDeployments,
  });
}