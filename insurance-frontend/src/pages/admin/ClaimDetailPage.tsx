import React, { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import { Card } from '../../components/ui/Card';
import { Button } from '../../components/ui/Button';
import { Badge } from '../../components/ui/Badge';
import { Modal } from '../../components/ui/Modal';
import { client } from '../../api/client';
import type { ClaimResponse, ClaimHistoryResponse } from '../../types';
import { ReviewAction } from '../../types';
import { formatCurrency, formatDate, formatDateTime } from '../../utils/formatters';
import { ArrowLeft, CheckCircle, XCircle, Clock, AlertCircle } from 'lucide-react';

export const ClaimDetailPage: React.FC = () => {
  const { policyId, claimId } = useParams<{ policyId: string; claimId: string }>();
  const navigate = useNavigate();
  const qc = useQueryClient();

  const [reviewModalOpen, setReviewModalOpen] = useState(false);
  const [reviewAction, setReviewAction] = useState<ReviewAction | null>(null);
  const [reviewerNotes, setReviewerNotes] = useState('');

  const { data: claim, isLoading: claimLoading } = useQuery<ClaimResponse>({
    queryKey: ['claim', claimId],
    queryFn: () => client.getClaim(Number(claimId)),
    enabled: !!claimId,
  });

  const { data: history, isLoading: historyLoading } = useQuery<ClaimHistoryResponse[]>({
    queryKey: ['claim-history', claimId],
    queryFn: () => client.getClaimHistory(Number(claimId)),
    enabled: !!claimId,
  });

  const reviewMutation = useMutation({
    mutationFn: () => client.reviewClaim(Number(claimId), {
      action: reviewAction!,
      reviewerNotes: reviewerNotes.trim() || undefined,
    }),
    onSuccess: (updatedClaim) => {
      qc.setQueryData(['claim', claimId], updatedClaim);
      qc.invalidateQueries({ queryKey: ['claim-history', claimId] });
      qc.invalidateQueries({ queryKey: ['claims', 'policy', policyId] });
      toast.success(`Claim ${reviewAction === ReviewAction.APPROVE ? 'approved' : 'rejected'} successfully`);
      setReviewModalOpen(false);
      setReviewerNotes('');
    },
    onError: (err: Error) => {
      toast.error(err.message || 'Failed to review claim');
    },
  });

  const openReviewModal = (action: ReviewAction) => {
    setReviewAction(action);
    setReviewerNotes('');
    setReviewModalOpen(true);
  };

  const canReview = claim && (claim.status === 'SUBMITTED' || claim.status === 'IN_REVIEW');

  if (claimLoading) {
    return <div className="text-center py-12 text-gray-500">Loading claim details...</div>;
  }

  if (!claim) {
    return (
      <div className="max-w-4xl mx-auto py-8 px-4">
        <div className="flex items-center gap-2 text-red-600 bg-red-50 border border-red-200 rounded-lg p-4">
          <AlertCircle size={18} />
          <span>Claim not found.</span>
        </div>
      </div>
    );
  }

  return (
    <>
      <div className="max-w-4xl mx-auto py-8 px-4">
        <button
          onClick={() => navigate(`/admin/claims/${policyId}`)}
          className="flex items-center gap-1 text-gray-500 hover:text-gray-800 transition-colors mb-6"
        >
          <ArrowLeft size={18} />
          <span>Back to Claims List</span>
        </button>

        <div className="flex items-center justify-between mb-6">
          <div>
            <h1 className="text-2xl font-bold text-gray-900">Claim #{claim.claimId}</h1>
            <div className="flex items-center gap-2 mt-1">
              <Badge status={claim.status} />
              <span className="text-sm bg-gray-100 px-2 py-0.5 rounded text-gray-600">{claim.claimType}</span>
            </div>
          </div>
          {canReview && (
            <div className="flex gap-3">
              <Button
                variant="primary"
                onClick={() => openReviewModal(ReviewAction.APPROVE)}
              >
                <span className="flex items-center gap-2">
                  <CheckCircle size={16} />
                  Approve
                </span>
              </Button>
              <Button
                variant="danger"
                onClick={() => openReviewModal(ReviewAction.REJECT)}
              >
                <span className="flex items-center gap-2">
                  <XCircle size={16} />
                  Reject
                </span>
              </Button>
            </div>
          )}
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-6">
          <Card>
            <h2 className="text-lg font-semibold mb-4">Claim Details</h2>
            <dl className="space-y-3">
              <div className="flex justify-between">
                <dt className="text-gray-500 text-sm">Policy Number</dt>
                <dd className="font-medium">{claim.policyNumber}</dd>
              </div>
              <div className="flex justify-between">
                <dt className="text-gray-500 text-sm">Claim Amount</dt>
                <dd className="font-bold text-lg">{formatCurrency(claim.claimAmount)}</dd>
              </div>
              <div className="flex justify-between">
                <dt className="text-gray-500 text-sm">Incident Date</dt>
                <dd className="font-medium">{formatDate(claim.incidentDate)}</dd>
              </div>
              <div className="flex justify-between">
                <dt className="text-gray-500 text-sm">Submitted At</dt>
                <dd className="font-medium">{formatDateTime(claim.createdAt)}</dd>
              </div>
              <div className="flex justify-between">
                <dt className="text-gray-500 text-sm">Last Updated</dt>
                <dd className="font-medium">{formatDateTime(claim.updatedAt)}</dd>
              </div>
            </dl>
          </Card>

          <Card>
            <h2 className="text-lg font-semibold mb-4">Description</h2>
            <p className="text-gray-700 leading-relaxed">{claim.description}</p>
          </Card>
        </div>

        {/* History Timeline */}
        <Card>
          <h2 className="text-lg font-semibold mb-4 flex items-center gap-2">
            <Clock size={20} className="text-blue-600" />
            Status History
          </h2>
          {historyLoading ? (
            <p className="text-gray-500 text-sm">Loading history...</p>
          ) : history && history.length > 0 ? (
            <div className="relative">
              <div className="absolute left-4 top-0 bottom-0 w-0.5 bg-gray-200" />
              <div className="space-y-4">
                {history.map((entry, idx) => (
                  <div key={entry.historyId} className="relative flex items-start gap-4 pl-10">
                    <div className={`absolute left-2.5 w-3 h-3 rounded-full border-2 border-white ring-2 ${
                      entry.status === 'APPROVED' ? 'bg-green-500 ring-green-200' :
                      entry.status === 'REJECTED' ? 'bg-red-500 ring-red-200' :
                      entry.status === 'IN_REVIEW' ? 'bg-blue-500 ring-blue-200' :
                      'bg-yellow-500 ring-yellow-200'
                    }`} />
                    <div className="flex-1 pb-2">
                      <div className="flex items-center gap-2">
                        <Badge status={entry.status} />
                        <span className="text-sm text-gray-500">{formatDateTime(entry.timestamp)}</span>
                        {idx === 0 && <span className="text-xs bg-blue-100 text-blue-700 px-1.5 py-0.5 rounded">Latest</span>}
                      </div>
                      {entry.reviewerNotes && (
                        <p className="mt-1 text-sm text-gray-600 bg-gray-50 px-3 py-2 rounded border-l-2 border-gray-300">
                          "{entry.reviewerNotes}"
                        </p>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          ) : (
            <p className="text-gray-500 text-sm">No history available.</p>
          )}
        </Card>
      </div>

      <Modal
        isOpen={reviewModalOpen}
        title={reviewAction === ReviewAction.APPROVE ? '✅ Approve Claim' : '❌ Reject Claim'}
        onClose={() => setReviewModalOpen(false)}
        actions={
          <>
            <Button variant="secondary" onClick={() => setReviewModalOpen(false)}>
              Cancel
            </Button>
            <Button
              variant={reviewAction === ReviewAction.APPROVE ? 'primary' : 'danger'}
              isLoading={reviewMutation.isPending}
              onClick={() => reviewMutation.mutate()}
            >
              Confirm {reviewAction === ReviewAction.APPROVE ? 'Approval' : 'Rejection'}
            </Button>
          </>
        }
      >
        <div className="space-y-4">
          <p className="text-gray-700">
            You are about to <strong>{reviewAction?.toLowerCase()}</strong> Claim #{claim?.claimId} for{' '}
            <strong>{formatCurrency(claim?.claimAmount ?? 0)}</strong>.
          </p>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1.5">
              Reviewer Notes {reviewAction === ReviewAction.REJECT ? <span className="text-red-500">*</span> : '(optional)'}
            </label>
            <textarea
              value={reviewerNotes}
              onChange={(e) => setReviewerNotes(e.target.value)}
              rows={3}
              placeholder="Add notes for the claimant..."
              className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none resize-none"
            />
          </div>
        </div>
      </Modal>
    </>
  );
};
