import axios from 'axios';
import type { AxiosInstance } from 'axios';
import type {
  ClaimSubmissionRequest,
  ClaimResponse,
  ClaimReviewRequest,
  ClaimHistoryResponse,
  PolicyResponse,
} from '../types';
import { ApiError, mapHttpErrorToMessage } from './errors';

const BASE_URL = '/api/v1';

class ApiClient {
  private axiosInstance: AxiosInstance;

  constructor() {
    this.axiosInstance = axios.create({
      baseURL: BASE_URL,
      headers: { 'Content-Type': 'application/json' },
    });

    this.axiosInstance.interceptors.response.use(
      (response) => response,
      (error: unknown) => {
        if (axios.isAxiosError(error) && error.response) {
          const status = error.response.status;
          const data = error.response.data as { message?: string; details?: Record<string, string> };
          const message = data?.message ?? mapHttpErrorToMessage(status);
          const details = data?.details ?? {};
          throw new ApiError(status, message, details);
        }
        throw error;
      }
    );
  }

  async submitClaim(payload: ClaimSubmissionRequest): Promise<ClaimResponse> {
    const response = await this.axiosInstance.post<ClaimResponse>('/claims', payload);
    return response.data;
  }

  async getClaim(claimId: number): Promise<ClaimResponse> {
    const response = await this.axiosInstance.get<ClaimResponse>(`/claims/${claimId}`);
    return response.data;
  }

  async reviewClaim(claimId: number, payload: ClaimReviewRequest): Promise<ClaimResponse> {
    const response = await this.axiosInstance.patch<ClaimResponse>(`/claims/${claimId}/review`, payload);
    return response.data;
  }

  async getClaimHistory(claimId: number): Promise<ClaimHistoryResponse[]> {
    const response = await this.axiosInstance.get<ClaimHistoryResponse[]>(`/claims/${claimId}/history`);
    return response.data;
  }

  async getClaimsByPolicy(policyId: number): Promise<ClaimResponse[]> {
    const response = await this.axiosInstance.get<ClaimResponse[]>(`/claims/policy/${policyId}`);
    return response.data;
  }

  async getPolicy(policyNumber: string): Promise<PolicyResponse> {
    const response = await this.axiosInstance.get<PolicyResponse>(`/policies/${policyNumber}`);
    return response.data;
  }
}

export const client = new ApiClient();
