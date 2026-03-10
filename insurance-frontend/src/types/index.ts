// Use const objects instead of enums (erasableSyntaxOnly)
export const ClaimStatus = {
  SUBMITTED: 'SUBMITTED',
  IN_REVIEW: 'IN_REVIEW',
  APPROVED: 'APPROVED',
  REJECTED: 'REJECTED',
} as const;
export type ClaimStatus = (typeof ClaimStatus)[keyof typeof ClaimStatus];

export const ClaimType = {
  MEDICAL: 'MEDICAL',
  DENTAL: 'DENTAL',
  VISION: 'VISION',
  LIFE: 'LIFE',
  AUTO: 'AUTO',
  HOME: 'HOME',
  DISABILITY: 'DISABILITY',
} as const;
export type ClaimType = (typeof ClaimType)[keyof typeof ClaimType];

export const PolicyStatus = {
  ACTIVE: 'ACTIVE',
  INACTIVE: 'INACTIVE',
  EXPIRED: 'EXPIRED',
  CANCELLED: 'CANCELLED',
  PENDING: 'PENDING',
} as const;
export type PolicyStatus = (typeof PolicyStatus)[keyof typeof PolicyStatus];

export const ReviewAction = {
  APPROVE: 'APPROVE',
  REJECT: 'REJECT',
} as const;
export type ReviewAction = (typeof ReviewAction)[keyof typeof ReviewAction];

export interface ClaimSubmissionRequest {
  policyNumber: string;
  claimType: ClaimType;
  claimAmount: number;
  incidentDate: string;
  description: string;
}

export interface ClaimResponse {
  claimId: number;
  policyId: number;
  policyNumber: string;
  claimType: ClaimType;
  claimAmount: number;
  incidentDate: string;
  description: string;
  status: ClaimStatus;
  createdAt: string;
  updatedAt: string;
}

export interface ClaimReviewRequest {
  action: ReviewAction;
  reviewerNotes?: string;
}

export interface ClaimHistoryResponse {
  historyId: number;
  claimId: number;
  status: ClaimStatus;
  timestamp: string;
  reviewerNotes?: string;
}

export interface PolicyResponse {
  policyId: number;
  policyNumber: string;
  customerId: number;
  status: PolicyStatus;
  effectiveDate: string;
  expiryDate: string;
  coverageLimit: number;
  coverageLimits: Record<ClaimType, number>;
}

export interface AuthUser {
  role: 'CUSTOMER' | 'ADMIN';
}
