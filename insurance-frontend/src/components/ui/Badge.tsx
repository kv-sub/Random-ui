import React from 'react';
import { ClaimStatus } from '../../types';

const statusColors: Record<ClaimStatus, string> = {
  [ClaimStatus.SUBMITTED]: 'bg-yellow-100 text-yellow-800',
  [ClaimStatus.IN_REVIEW]: 'bg-blue-100 text-blue-800',
  [ClaimStatus.APPROVED]: 'bg-green-100 text-green-800',
  [ClaimStatus.REJECTED]: 'bg-red-100 text-red-800',
};

interface BadgeProps {
  status: ClaimStatus;
}

export const Badge: React.FC<BadgeProps> = ({ status }) => (
  <span className={`px-3 py-1 rounded-full text-sm font-medium ${statusColors[status]}`}>
    {status.replace('_', ' ')}
  </span>
);
