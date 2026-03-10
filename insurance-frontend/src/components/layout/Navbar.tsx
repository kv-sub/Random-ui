import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuthStore } from '../../stores/authStore';
import { LogOut, Menu, X, Shield, User, Search } from 'lucide-react';

export const Navbar: React.FC = () => {
  const navigate = useNavigate();
  const { user, isAuthenticated, logout, isAdmin } = useAuthStore();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  if (!isAuthenticated()) return null;

  return (
    <nav className="bg-blue-600 text-white shadow-lg">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between items-center h-16">
          <Link to="/" className="text-xl font-bold flex items-center gap-2">
            <Shield size={22} />
            Insurance Claims
          </Link>

          <div className="hidden md:flex space-x-6">
            <Link to="/" className="hover:opacity-80 transition-opacity">Dashboard</Link>
            {!isAdmin() && (
              <>
                <Link to="/submit-claim" className="hover:opacity-80 transition-opacity">Submit Claim</Link>
                <Link to="/track-claim" className="hover:opacity-80 transition-opacity flex items-center gap-1">
                  <Search size={14} />Track Claim
                </Link>
              </>
            )}
            {isAdmin() && (
              <Link to="/admin/policies" className="hover:opacity-80 transition-opacity">Admin Dashboard</Link>
            )}
          </div>

          <div className="hidden md:flex items-center space-x-4">
            <span className="flex items-center gap-1 text-sm opacity-90">
              {isAdmin() ? <Shield size={16} /> : <User size={16} />}
              {user?.role}
            </span>
            <button
              onClick={handleLogout}
              className="flex items-center space-x-2 hover:opacity-80 transition-opacity bg-blue-700 px-3 py-1.5 rounded-lg"
            >
              <LogOut size={16} />
              <span className="text-sm">Logout</span>
            </button>
          </div>

          <button className="md:hidden" onClick={() => setMobileMenuOpen(!mobileMenuOpen)}>
            {mobileMenuOpen ? <X /> : <Menu />}
          </button>
        </div>

        {mobileMenuOpen && (
          <div className="md:hidden pb-4 space-y-1">
            <Link to="/" className="block px-4 py-2 hover:bg-blue-700 rounded" onClick={() => setMobileMenuOpen(false)}>Dashboard</Link>
            {!isAdmin() && (
              <>
                <Link to="/submit-claim" className="block px-4 py-2 hover:bg-blue-700 rounded" onClick={() => setMobileMenuOpen(false)}>Submit Claim</Link>
                <Link to="/track-claim" className="block px-4 py-2 hover:bg-blue-700 rounded" onClick={() => setMobileMenuOpen(false)}>Track Claim</Link>
              </>
            )}
            {isAdmin() && (
              <Link to="/admin/policies" className="block px-4 py-2 hover:bg-blue-700 rounded" onClick={() => setMobileMenuOpen(false)}>Admin Dashboard</Link>
            )}
            <button
              onClick={() => { handleLogout(); setMobileMenuOpen(false); }}
              className="w-full text-left px-4 py-2 hover:bg-blue-700 rounded flex items-center space-x-2"
            >
              <LogOut size={16} />
              <span>Logout</span>
            </button>
          </div>
        )}
      </div>
    </nav>
  );
};
