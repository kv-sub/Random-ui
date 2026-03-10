import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import toast from 'react-hot-toast';
import type { ClaimSubmissionRequest, PolicyResponse } from '../../types';
import { ClaimType } from '../../types';
import { Button } from '../ui/Button';
import { Modal } from '../ui/Modal';
import { Card } from '../ui/Card';
import { client } from '../../api/client';
import { ApiError } from '../../api/errors';
import { formatCurrency, dateToInputFormat } from '../../utils/formatters';
import { CheckCircle, AlertCircle } from 'lucide-react';

const submissionSchema = z.object({
  policyNumber: z.string()
    .min(1, 'Policy number is required')
    .regex(/^POL-[A-Z0-9]{5}$/, 'Format must be POL-XXXXX (e.g. POL-AB123)'),
  claimType: z.string().min(1, 'Please select a claim type'),
  claimAmount: z.string().min(1, 'Claim amount is required'),
  incidentDate: z.string().min(1, 'Incident date is required'),
  description: z.string()
    .min(10, 'Description must be at least 10 characters')
    .max(1000, 'Description must not exceed 1000 characters'),
});

type SubmissionFormData = z.infer<typeof submissionSchema>;

export const ClaimSubmissionForm: React.FC = () => {
  const navigate = useNavigate();
  const { register, handleSubmit, watch, formState: { errors }, reset } = useForm<SubmissionFormData>({
    resolver: zodResolver(submissionSchema),
  });

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [policy, setPolicy] = useState<PolicyResponse | null>(null);
  const [policyLoading, setPolicyLoading] = useState(false);
  const [policyError, setPolicyError] = useState<string | null>(null);
  const [successModalOpen, setSuccessModalOpen] = useState(false);
  const [successClaimId, setSuccessClaimId] = useState<number | null>(null);
  const [errorModalOpen, setErrorModalOpen] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  const policyNumber = watch('policyNumber');
  const claimType = watch('claimType') as ClaimType;
  const claimAmount = watch('claimAmount');

  useEffect(() => {
    if (!policyNumber || !/^POL-[A-Z0-9]{5}$/.test(policyNumber)) {
      setPolicy(null);
      setPolicyError(null);
      return;
    }

    const timer = setTimeout(async () => {
      setPolicyLoading(true);
      try {
        const policyData = await client.getPolicy(policyNumber);
        if (policyData.status !== 'ACTIVE') {
          setPolicy(null);
          setPolicyError(`Policy is ${policyData.status.toLowerCase()} — only active policies can be used`);
        } else {
          setPolicy(policyData);
          setPolicyError(null);
        }
      } catch (error) {
        setPolicy(null);
        setPolicyError(error instanceof ApiError ? error.message : 'Policy not found');
      } finally {
        setPolicyLoading(false);
      }
    }, 500);

    return () => clearTimeout(timer);
  }, [policyNumber]);

  const handleFormSubmit = async (data: SubmissionFormData) => {
    const amount = parseFloat(data.claimAmount);
    if (isNaN(amount) || amount <= 0) {
      setErrorMessage('Please enter a valid claim amount.');
      setErrorModalOpen(true);
      return;
    }

    setIsSubmitting(true);
    try {
      const payload: ClaimSubmissionRequest = {
        policyNumber: data.policyNumber,
        claimType: data.claimType as ClaimType,
        claimAmount: amount,
        incidentDate: data.incidentDate,
        description: data.description,
      };
      const response = await client.submitClaim(payload);
      setSuccessClaimId(response.claimId);
      setSuccessModalOpen(true);
      toast.success('Claim submitted successfully!');
      reset();
      setPolicy(null);
    } catch (error) {
      if (error instanceof ApiError) {
        setErrorMessage(error.status === 409
          ? 'Duplicate claim: a similar claim was already submitted within the last 24 hours.'
          : error.message || 'Failed to submit claim.');
      } else {
        setErrorMessage('Failed to submit claim. Please try again.');
      }
      setErrorModalOpen(true);
    } finally {
      setIsSubmitting(false);
    }
  };

  const maxDate = dateToInputFormat(new Date());
  const minDate = policy?.effectiveDate;
  const coverageLimit = claimType && policy?.coverageLimits?.[claimType]
    ? policy.coverageLimits[claimType]
    : null;
  const amountNum = parseFloat(claimAmount);
  const isCoverageValid = !claimAmount || !coverageLimit || isNaN(amountNum) || amountNum <= coverageLimit;
  const descriptionLength = watch('description')?.length ?? 0;

  return (
    <>
      <div className="max-w-2xl mx-auto py-8 px-4">
        <Card>
          <h1 className="text-2xl font-bold mb-6 text-gray-900">Submit Insurance Claim</h1>

          <form onSubmit={handleSubmit(handleFormSubmit)} className="space-y-6">
            {/* Policy Number */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1.5">
                Policy Number <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                placeholder="POL-AB123"
                {...register('policyNumber')}
                className="w-full px-4 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
              />
              {errors.policyNumber && (
                <p className="text-red-600 text-sm mt-1 flex items-center gap-1">
                  <AlertCircle size={14} />{errors.policyNumber.message}
                </p>
              )}
              {policyLoading && <p className="text-gray-500 text-sm mt-1">Verifying policy...</p>}
              {policyError && !policyLoading && (
                <p className="text-red-600 text-sm mt-1 flex items-center gap-1">
                  <AlertCircle size={14} />{policyError}
                </p>
              )}
              {policy && !policyLoading && (
                <div className="mt-2 p-3 bg-green-50 border border-green-200 rounded-lg">
                  <p className="text-sm text-green-700 flex items-center gap-1">
                    <CheckCircle size={14} /> Policy verified — active until {policy.expiryDate}
                  </p>
                  <p className="text-xs text-green-600 mt-0.5">
                    Total coverage: {formatCurrency(policy.coverageLimit)}
                  </p>
                </div>
              )}
            </div>

            {/* Claim Type - only show after policy verified */}
            {policy && (
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1.5">
                  Claim Type <span className="text-red-500">*</span>
                </label>
                <select
                  {...register('claimType')}
                  className="w-full px-4 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none bg-white"
                >
                  <option value="">Select claim type</option>
                  {Object.entries(policy.coverageLimits).map(([type, limit]) => (
                    <option key={type} value={type}>
                      {type} — limit: {formatCurrency(Number(limit))}
                    </option>
                  ))}
                </select>
                {errors.claimType && (
                  <p className="text-red-600 text-sm mt-1 flex items-center gap-1">
                    <AlertCircle size={14} />{errors.claimType.message}
                  </p>
                )}
              </div>
            )}

            {/* Incident Date */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1.5">
                Incident Date <span className="text-red-500">*</span>
              </label>
              <input
                type="date"
                {...register('incidentDate')}
                max={maxDate}
                min={minDate}
                className="w-full px-4 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
              />
              {errors.incidentDate && (
                <p className="text-red-600 text-sm mt-1 flex items-center gap-1">
                  <AlertCircle size={14} />{errors.incidentDate.message}
                </p>
              )}
            </div>

            {/* Claim Amount */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1.5">
                Claim Amount ($) <span className="text-red-500">*</span>
              </label>
              <input
                type="number"
                step="0.01"
                min="0.01"
                placeholder="0.00"
                {...register('claimAmount')}
                className="w-full px-4 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
              />
              {errors.claimAmount && (
                <p className="text-red-600 text-sm mt-1 flex items-center gap-1">
                  <AlertCircle size={14} />{errors.claimAmount.message}
                </p>
              )}
              {coverageLimit != null && (
                <p className="text-sm text-gray-500 mt-1">
                  Coverage limit for {claimType}: {formatCurrency(Number(coverageLimit))}
                </p>
              )}
              {!isCoverageValid && (
                <p className="text-red-600 text-sm mt-1 flex items-center gap-1">
                  <AlertCircle size={14} />Amount exceeds coverage limit of {formatCurrency(Number(coverageLimit))}
                </p>
              )}
            </div>

            {/* Description */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1.5">
                Description <span className="text-red-500">*</span>
              </label>
              <textarea
                {...register('description')}
                rows={4}
                placeholder="Describe your claim in detail (10–1000 characters)"
                className="w-full px-4 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none resize-none"
              />
              <div className="flex justify-between mt-1">
                {errors.description ? (
                  <p className="text-red-600 text-sm flex items-center gap-1">
                    <AlertCircle size={14} />{errors.description.message}
                  </p>
                ) : <span />}
                <span className={`text-xs ${descriptionLength > 1000 ? 'text-red-500' : 'text-gray-400'}`}>
                  {descriptionLength}/1000
                </span>
              </div>
            </div>

            <div className="flex gap-4 pt-2">
              <Button
                type="submit"
                variant="primary"
                size="lg"
                isLoading={isSubmitting}
                disabled={!policy || !isCoverageValid}
              >
                Submit Claim
              </Button>
              <Button
                type="button"
                variant="secondary"
                size="lg"
                onClick={() => { reset(); setPolicy(null); setPolicyError(null); }}
              >
                Reset
              </Button>
            </div>
          </form>
        </Card>
      </div>

      <Modal
        isOpen={successModalOpen}
        title="Claim Submitted Successfully"
        onClose={() => { setSuccessModalOpen(false); setSuccessClaimId(null); }}
        actions={
          <>
            <Button variant="secondary" onClick={() => { setSuccessModalOpen(false); setSuccessClaimId(null); }}>
              Submit Another
            </Button>
            <Button variant="primary" onClick={() => { setSuccessModalOpen(false); navigate('/track-claim'); }}>
              Track Claim
            </Button>
          </>
        }
      >
        <div className="space-y-4">
          <div className="flex items-center gap-2 text-green-600">
            <CheckCircle size={20} />
            <span>Your claim has been submitted and is under review.</span>
          </div>
          <div className="bg-blue-50 p-4 rounded-lg border border-blue-200">
            <p className="text-sm text-gray-600">Claim ID</p>
            <p className="text-3xl font-bold text-blue-600">#{successClaimId}</p>
          </div>
          <p className="text-sm text-gray-500">
            Save your Claim ID to track status updates. You can use the Track Claim feature anytime.
          </p>
        </div>
      </Modal>

      <Modal
        isOpen={errorModalOpen}
        title="Submission Error"
        onClose={() => setErrorModalOpen(false)}
        actions={<Button variant="primary" onClick={() => setErrorModalOpen(false)}>Close</Button>}
      >
        <div className="flex items-start gap-3 text-red-600">
          <AlertCircle size={20} className="flex-shrink-0 mt-0.5" />
          <p>{errorMessage}</p>
        </div>
      </Modal>
    </>
  );
};
