import React from 'react';
import { Link } from 'react-router-dom';
import { useAuthStore } from '../stores/authStore';
import { FileText, Search, CheckCircle, Clock, Shield, PlusCircle } from 'lucide-react';

export const Home: React.FC = () => {
  const { user } = useAuthStore();
  const isAdmin = user?.role === 'ADMIN';

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-gray-900">
            Welcome, {isAdmin ? 'Admin' : 'Customer'}!
          </h1>
          <p className="text-gray-500 mt-1">Insurance Claims Management Portal</p>
        </div>

        {isAdmin ? (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div className="bg-white p-6 rounded-lg shadow">
              <div className="flex items-center gap-3 mb-4">
                <Shield className="text-purple-600" size={28} />
                <h2 className="text-xl font-semibold">Admin Capabilities</h2>
              </div>
              <ul className="space-y-3 text-gray-700">
                <li className="flex items-center gap-2"><CheckCircle size={16} className="text-green-500" /> Search policies by policy number</li>
                <li className="flex items-center gap-2"><CheckCircle size={16} className="text-green-500" /> View all claims for a policy</li>
                <li className="flex items-center gap-2"><CheckCircle size={16} className="text-green-500" /> Approve or reject claims with reviewer notes</li>
                <li className="flex items-center gap-2"><CheckCircle size={16} className="text-green-500" /> View full claim history timeline</li>
              </ul>
            </div>
            <div className="bg-white p-6 rounded-lg shadow">
              <h3 className="text-lg font-semibold mb-4">Quick Actions</h3>
              <Link
                to="/admin/policies"
                className="inline-flex items-center gap-2 bg-purple-600 text-white px-5 py-2.5 rounded-lg hover:bg-purple-700 transition-colors"
              >
                <Shield size={18} />
                Go to Admin Dashboard
              </Link>
            </div>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div className="bg-white p-6 rounded-lg shadow">
              <div className="flex items-center gap-3 mb-4">
                <FileText className="text-blue-600" size={28} />
                <h2 className="text-xl font-semibold">Customer Features</h2>
              </div>
              <ul className="space-y-3 text-gray-700">
                <li className="flex items-center gap-2"><CheckCircle size={16} className="text-green-500" /> Submit new insurance claims</li>
                <li className="flex items-center gap-2"><CheckCircle size={16} className="text-green-500" /> Real-time policy verification</li>
                <li className="flex items-center gap-2"><CheckCircle size={16} className="text-green-500" /> Coverage limit validation per claim type</li>
                <li className="flex items-center gap-2"><CheckCircle size={16} className="text-green-500" /> Track claim status by claim ID</li>
              </ul>
            </div>
            <div className="bg-white p-6 rounded-lg shadow">
              <h3 className="text-lg font-semibold mb-4">Quick Actions</h3>
              <div className="space-y-3">
                <Link
                  to="/submit-claim"
                  className="inline-flex items-center gap-2 bg-blue-600 text-white px-5 py-2.5 rounded-lg hover:bg-blue-700 transition-colors"
                >
                  <PlusCircle size={18} />
                  Submit a New Claim
                </Link>
                <br />
                <Link
                  to="/track-claim"
                  className="inline-flex items-center gap-2 bg-gray-100 text-gray-800 px-5 py-2.5 rounded-lg hover:bg-gray-200 transition-colors mt-2"
                >
                  <Search size={18} />
                  Track Claim Status
                </Link>
              </div>
            </div>
          </div>
        )}

        <div className="mt-8 bg-blue-50 border border-blue-200 rounded-lg p-4">
          <div className="flex items-center gap-2 text-blue-800">
            <Clock size={18} />
            <span className="font-medium">About this Portal</span>
          </div>
          <p className="text-blue-700 text-sm mt-1">
            This portal connects to the Insurance Claims System backend API at <code className="bg-blue-100 px-1 rounded">/api/v1</code>.
            Claims are processed in real-time and status updates are reflected immediately.
          </p>
        </div>
      </div>
    </div>
  );
};
