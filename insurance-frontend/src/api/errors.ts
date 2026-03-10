export class ApiError extends Error {
  status: number;
  details?: Record<string, string>;

  constructor(status: number, message: string, details?: Record<string, string>) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.details = details;
  }
}

export const getErrorMessage = (error: unknown): string => {
  if (error instanceof ApiError) return error.message;
  if (error instanceof Error) return error.message;
  return 'An unexpected error occurred';
};

export const mapHttpErrorToMessage = (status: number, message?: string): string => {
  switch (status) {
    case 400: return message ?? 'Invalid request. Please check your input.';
    case 401: return 'Unauthorized. Please log in again.';
    case 403: return 'Forbidden. You do not have permission.';
    case 404: return message ?? 'Resource not found.';
    case 409: return message ?? 'Duplicate claim detected within 24 hours.';
    case 500: return 'Server error. Please try again later.';
    default: return message ?? 'An error occurred. Please try again.';
  }
};
