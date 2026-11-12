// src/api/http.ts
// Reads an error response. The backend sends one of three shapes:
//   text/plain                      -> the message is the body
//   {"error": "..."}                -> domain errors on PATCH and POST
//   {"param": [{"message": ...}]}   -> query-parameter validation
export async function readErrorMessage(res: Response): Promise<string> {
  const text = (await res.text()).trim()
  if (!text) return `Request failed (${res.status})`

  try {
    const body: unknown = JSON.parse(text)
    if (body && typeof body === 'object') {
      if ('error' in body && typeof body.error === 'string') return body.error

      const first = Object.values(body)
        .flat()
        .find((entry): entry is { message: string } =>
          typeof entry === 'object' && entry !== null && 'message' in entry)
      if (first) return first.message
    }
  } catch {
    // Not JSON, so fall through and use the text.
  }
  return text
}
