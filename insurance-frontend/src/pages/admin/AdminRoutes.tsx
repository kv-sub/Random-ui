import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { AdminPoliciesPage } from './AdminPoliciesPage';
import { ClaimListPage } from './ClaimListPage';
import { ClaimDetailPage } from './ClaimDetailPage';

export const AdminRoutes: React.FC = () => (
  <Routes>
    <Route path="/policies" element={<AdminPoliciesPage />} />
    <Route path="/claims/:policyId" element={<ClaimListPage />} />
    <Route path="/claims/:policyId/:claimId" element={<ClaimDetailPage />} />
    <Route path="*" element={<Navigate to="/admin/policies" replace />} />
  </Routes>
);
