import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Card } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Badge } from '../components/ui/Badge';
import { client } from '../api/client';
import type { ClaimResponse, ClaimHistoryResponse } from '../types';
import { formatCurrency, formatDate, formatDateTime } from '../utils/formatters';
import { Search, Clock, AlertCircle } from 'lucide-react';

export const TrackClaimPage: React.FC = () => {
  const [searchInput, setSearchInput] = useState('');
  const [claimId, setClaimId] = useState<number | null>(null);

  const { data: claim, isLoading: claimLoading, error: claimError } = useQuery<ClaimResponse>({
    queryKey: ['claim', claimId],
    queryFn: () => client.getClaim(claimId!),
    enabled: !!claimId,
    retry: false,
  });

  const { data: history, isLoading: historyLoading } = useQuery<ClaimHistoryResponse[]>({
    queryKey: ['claim-history', claimId],
    queryFn: () => client.getClaimHistory(claimId!),
    enabled: !!claimId && !!claim,
  });

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    const id = parseInt(searchInput.trim());
    if (isNaN(id) || id <= 0) return;
    setClaimId(id);
  };

  return (
    <div className="max-w-3xl mx-auto py-8 px-4">
      <div className="mb-8">
        <h1 className="text-2xl font-bold text-gray-900">Track Claim Status</h1>
        <p className="text-gray-500 mt-1">Enter your Claim ID to view current status and history</p>
      </div>

      <Card className="mb-6">
        <form onSubmit={handleSearch} className="flex gap-3">
          <input
            type="number"
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
            placeholder="Enter Claim ID (e.g. 42)"
            className="flex-1 px-4 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
            min="1"
          />
          <Button type="submit" variant="primary" isLoading={claimLoading}>
            <span className="flex items-center gap-2">
              <Search size={16} />
              Track
            </span>
          </Button>
        </form>
      </Card>

      {claimError && (
        <div className="flex items-center gap-2 text-red-600 bg-red-50 border border-red-200 rounded-lg p-4 mb-6">
          <AlertCircle size={18} />
          <span>Claim not found. Please check the ID and try again.</span>
        </div>
      )}

      {claim && (
        <>
          <Card className="mb-6">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-xl font-bold">Claim #{claim.claimId}</h2>
              <Badge status={claim.status} />
            </div>
            <div className="grid grid-cols-2 md:grid-cols-3 gap-4 text-sm">
              <div>
                <p className="text-gray-500">Policy</p>
                <p className="font-semibold">{claim.policyNumber}</p>
              </div>
              <div>
                <p className="text-gray-500">Type</p>
                <p className="font-semibold">{claim.claimType}</p>
              </div>
              <div>
                <p className="text-gray-500">Amount</p>
                <p className="font-bold text-lg">{formatCurrency(claim.claimAmount)}</p>
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
                <p className="text-gray-500">Last Updated</p>
                <p className="font-semibold">{formatDateTime(claim.updatedAt)}</p>
              </div>
            </div>
            <div className="mt-4 pt-4 border-t">
              <p className="text-gray-500 text-sm">Description</p>
              <p className="text-gray-700 mt-1">{claim.description}</p>
            </div>
          </Card>

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
                        <div className="flex items-center gap-2 flex-wrap">
                          <Badge status={entry.status} />
                          <span className="text-sm text-gray-500">{formatDateTime(entry.timestamp)}</span>
                          {idx === 0 && <span className="text-xs bg-blue-100 text-blue-700 px-1.5 py-0.5 rounded">Current</span>}
                        </div>
                        {entry.reviewerNotes && (
                          <p className="mt-1 text-sm text-gray-600 bg-gray-50 px-3 py-2 rounded border-l-2 border-gray-300">
                            Note: "{entry.reviewerNotes}"
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
        </>
      )}
    </div>
  );
};
