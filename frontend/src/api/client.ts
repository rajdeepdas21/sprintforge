// One place for JSON requests, bearer tokens, and readable API errors.
type ApiOptions = RequestInit & {
  token?: string
}

type ApiErrorBody = {
  message?: string
  details?: Array<{ message?: string }>
}

export class ApiError extends Error {
  public readonly status: number

  constructor(message: string, status: number) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}
export async function apiRequest<T>(
  path: string,
  options: ApiOptions = {},
): Promise<T> {
  const { token, ...requestOptions } = options
  const headers = new Headers(requestOptions.headers)

  headers.set('Accept', 'application/json')

  if (requestOptions.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }

  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }

  const response = await fetch(path, {
    ...requestOptions,
    headers,
  })

  // A 204 response has no JSON body.
  if (response.status === 204) {
    return undefined as T
  }

  const data: unknown = await response.json().catch(() => null)

  if (!response.ok) {
    const body = data as ApiErrorBody | null
    const validationMessages = body?.details
      ?.map((detail) => detail.message)
      .filter((message): message is string => Boolean(message))
      .join(' ')

    throw new ApiError(
      validationMessages || body?.message || `Request failed (${response.status})`,
      response.status,
    )
  }

  return data as T
}