import React from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { Card } from '../../components/ui/Card';
import { Button } from '../../components/ui/Button';
import { Badge } from '../../components/ui/Badge';
import { client } from '../../api/client';
import type { ClaimResponse } from '../../types';
import { formatCurrency, formatDate, formatDateTime } from '../../utils/formatters';
import { ArrowLeft, Eye, AlertCircle, FileText } from 'lucide-react';

export const ClaimListPage: React.FC = () => {
  const { policyId } = useParams<{ policyId: string }>();
  const navigate = useNavigate();

  const { data: claims, isLoading, error } = useQuery<ClaimResponse[]>({
    queryKey: ['claims', 'policy', policyId],
    queryFn: () => client.getClaimsByPolicy(Number(policyId)),
    enabled: !!policyId,
  });

  return (
    <div className="max-w-5xl mx-auto py-8 px-4">
      <div className="mb-6 flex items-center gap-3">
        <button
          onClick={() => navigate('/admin/policies')}
          className="flex items-center gap-1 text-gray-500 hover:text-gray-800 transition-colors"
        >
          <ArrowLeft size={18} />
          <span>Back to Policy Search</span>
        </button>
      </div>

      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-2">
          <FileText size={24} className="text-blue-600" />
          Claims for Policy #{policyId}
        </h1>
        <p className="text-gray-500 mt-1">
          {isLoading ? 'Loading...' : `${claims?.length ?? 0} claim(s) found`}
        </p>
      </div>

      {isLoading && (
        <div className="text-center py-12 text-gray-500">Loading claims...</div>
      )}

      {error && (
        <div className="flex items-center gap-2 text-red-600 bg-red-50 border border-red-200 rounded-lg p-4">
          <AlertCircle size={18} />
          <span>Failed to load claims. Please try again.</span>
        </div>
      )}

      {claims && claims.length === 0 && (
        <Card>
          <div className="text-center py-8 text-gray-500">
            <FileText className="mx-auto mb-3 opacity-30" size={48} />
            <p className="text-lg font-medium">No claims found</p>
            <p className="text-sm">No claims have been submitted for this policy yet.</p>
          </div>
        </Card>
      )}

      {claims && claims.length > 0 && (
        <div className="space-y-4">
          {claims.map((claim) => (
            <Card key={claim.claimId} className="hover:shadow-md transition-shadow">
              <div className="flex items-start justify-between">
                <div className="flex-1">
                  <div className="flex items-center gap-3 mb-2">
                    <span className="text-lg font-bold text-gray-900">Claim #{claim.claimId}</span>
                    <Badge status={claim.status} />
                    <span className="text-sm bg-gray-100 px-2 py-0.5 rounded text-gray-600">{claim.claimType}</span>
                  </div>
                  <div className="grid grid-cols-2 md:grid-cols-4 gap-3 text-sm">
                    <div>
                      <p className="text-gray-500">Amount</p>
                      <p className="font-semibold">{formatCurrency(claim.claimAmount)}</p>
                    </div>
                    <div>
                      <p className="text-gray-500">Incident Date</p>
                      <p className="font-semibold">{formatDate(claim.incidentDate)}</p>
                    </div>
                    <div>
                      <p className="text-gray-500">Submitted</p>
                      <p className="font-semibold">{formatDateTime(claim.createdAt)}</p>
                    </div>
                    <div>
                      <p className="text-gray-500">Policy</p>
                      <p className="font-semibold">{claim.policyNumber}</p>
                    </div>
                  </div>
                  <p className="mt-2 text-sm text-gray-600 truncate">{claim.description}</p>
                </div>
                <Link
                  to={`/admin/claims/${policyId}/${claim.claimId}`}
                  className="ml-4 flex-shrink-0"
                >
                  <Button variant="secondary" size="sm">
                    <span className="flex items-center gap-1">
                      <Eye size={14} />
                      Review
                    </span>
                  </Button>
                </Link>
              </div>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
};
