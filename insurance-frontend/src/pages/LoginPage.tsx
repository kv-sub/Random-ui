import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '../stores/authStore';
import { Shield, User } from 'lucide-react';

export const LoginPage: React.FC = () => {
  const navigate = useNavigate();
  const { login } = useAuthStore();

  const handleLogin = (role: 'CUSTOMER' | 'ADMIN') => {
    login(role);
    navigate('/');
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50">
      <div className="max-w-md w-full bg-white p-8 rounded-lg shadow-lg">
        <div className="text-center mb-8">
          <Shield className="mx-auto text-blue-600 mb-3" size={48} />
          <h1 className="text-2xl font-bold text-gray-900">Insurance Claims Portal</h1>
          <p className="text-gray-500 mt-1 text-sm">Select your role to continue</p>
        </div>

        <div className="space-y-4">
          <button
            onClick={() => handleLogin('CUSTOMER')}
            className="w-full flex items-center gap-4 p-4 border-2 border-gray-200 rounded-lg hover:border-blue-500 hover:bg-blue-50 transition-all group"
          >
            <div className="bg-blue-100 p-3 rounded-full group-hover:bg-blue-200 transition-colors">
              <User className="text-blue-600" size={24} />
            </div>
            <div className="text-left">
              <div className="font-semibold text-gray-900">Customer</div>
              <div className="text-sm text-gray-500">Submit and track insurance claims</div>
            </div>
          </button>

          <button
            onClick={() => handleLogin('ADMIN')}
            className="w-full flex items-center gap-4 p-4 border-2 border-gray-200 rounded-lg hover:border-purple-500 hover:bg-purple-50 transition-all group"
          >
            <div className="bg-purple-100 p-3 rounded-full group-hover:bg-purple-200 transition-colors">
              <Shield className="text-purple-600" size={24} />
            </div>
            <div className="text-left">
              <div className="font-semibold text-gray-900">Admin / Reviewer</div>
              <div className="text-sm text-gray-500">Review and process claim submissions</div>
            </div>
          </button>
        </div>

        <p className="text-center text-xs text-gray-400 mt-6">
          Demo portal — no password required
        </p>
      </div>
    </div>
  );
};
