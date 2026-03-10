import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import { Card } from '../../components/ui/Card';
import { Button } from '../../components/ui/Button';
import { client } from '../../api/client';
import { ApiError } from '../../api/errors';
import type { PolicyResponse } from '../../types';
import { formatCurrency, formatDate } from '../../utils/formatters';
import { Search, FileText, ChevronRight, AlertCircle } from 'lucide-react';

export const AdminPoliciesPage: React.FC = () => {
  const navigate = useNavigate();
  const [searchInput, setSearchInput] = useState('');
  const [searchTerm, setSearchTerm] = useState('');

  const { data: policy, isLoading, error } = useQuery<PolicyResponse, ApiError>({
    queryKey: ['policy', searchTerm],
    queryFn: () => client.getPolicy(searchTerm),
    enabled: !!searchTerm,
    retry: false,
  });

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    if (!searchInput.trim()) {
      toast.error('Please enter a policy number');
      return;
    }
    setSearchTerm(searchInput.trim().toUpperCase());
  };

  return (
    <div className="max-w-4xl mx-auto py-8 px-4">
      <div className="mb-8">
        <h1 className="text-2xl font-bold text-gray-900">Admin Dashboard</h1>
        <p className="text-gray-500 mt-1">Search for a policy to view and manage claims</p>
      </div>

      <Card className="mb-6">
        <h2 className="text-lg font-semibold mb-4 flex items-center gap-2">
          <Search size={20} className="text-blue-600" />
          Search Policy
        </h2>
        <form onSubmit={handleSearch} className="flex gap-3">
          <input
            type="text"
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
            placeholder="Enter policy number (e.g. POL-AB123)"
            className="flex-1 px-4 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
          />
          <Button type="submit" variant="primary" isLoading={isLoading}>
            Search
          </Button>
        </form>
      </Card>

      {error && (
        <div className="flex items-center gap-2 text-red-600 bg-red-50 border border-red-200 rounded-lg p-4 mb-6">
          <AlertCircle size={18} />
          <span>{(error as ApiError).message || 'Policy not found'}</span>
        </div>
      )}

      {policy && (
        <Card>
          <div className="flex justify-between items-start mb-6">
            <div>
              <h2 className="text-xl font-bold text-gray-900">{policy.policyNumber}</h2>
              <span className={`inline-block mt-1 px-3 py-0.5 rounded-full text-sm font-medium ${
                policy.status === 'ACTIVE' ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
              }`}>
                {policy.status}
              </span>
            </div>
            <Button
              variant="primary"
              onClick={() => navigate(`/admin/claims/${policy.policyId}`)}
            >
              <span className="flex items-center gap-2">
                <FileText size={16} />
                View Claims
                <ChevronRight size={16} />
              </span>
            </Button>
          </div>

          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
            {[
              { label: 'Policy ID', value: policy.policyId },
              { label: 'Customer ID', value: policy.customerId },
              { label: 'Effective', value: formatDate(policy.effectiveDate) },
              { label: 'Expiry', value: formatDate(policy.expiryDate) },
            ].map(({ label, value }) => (
              <div key={label} className="bg-gray-50 p-3 rounded-lg">
                <p className="text-xs text-gray-500 uppercase tracking-wide">{label}</p>
                <p className="font-semibold">{value}</p>
              </div>
            ))}
          </div>

          <div>
            <h3 className="text-sm font-semibold text-gray-700 mb-3 uppercase tracking-wide">
              Coverage Limits by Type
            </h3>
            <div className="grid grid-cols-2 md:grid-cols-3 gap-3">
              {Object.entries(policy.coverageLimits).map(([type, limit]) => (
                <div key={type} className="bg-blue-50 border border-blue-100 p-3 rounded-lg">
                  <p className="text-xs text-blue-600 font-medium">{type}</p>
                  <p className="font-bold text-gray-900">{formatCurrency(Number(limit))}</p>
                </div>
              ))}
            </div>
            <div className="mt-4 pt-4 border-t">
              <p className="text-sm text-gray-600">
                Total Coverage: <span className="font-bold text-gray-900">{formatCurrency(policy.coverageLimit)}</span>
              </p>
            </div>
          </div>
        </Card>
      )}
    </div>
  );
};
