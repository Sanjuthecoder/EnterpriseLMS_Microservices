import React, { createContext, useContext, useState, useEffect } from 'react';
import api from '../services/api';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // Check for existing session
    const token = localStorage.getItem('token');
    const storedUser = localStorage.getItem('user');
    
    if (token && storedUser) {
      try {
        setUser(JSON.parse(storedUser));
      } catch (e) {
        console.error("Failed to parse stored user", e);
        localStorage.removeItem('token');
        localStorage.removeItem('user');
      }
    }
    setLoading(false);
  }, []);

  const login = async (credentials) => {
    try {
      const response = await api.post('/auth/login', credentials);
      const { token, userId, username, email, role, orgId, companyId, subscriptionTier } = response.data;
      
      const userData = { userId, username, email, role, orgId, companyId, subscriptionTier: subscriptionTier || 'FREE' };
      
      localStorage.setItem('token', token);
      localStorage.setItem('user', JSON.stringify(userData));
      if (orgId) {
        localStorage.setItem('tenant_id', orgId);
      }
      
      setUser(userData);
      return { success: true, role };
    } catch (error) {
      const errorMsg = error.response?.data && typeof error.response.data === 'string'
        ? error.response.data
        : (error.response?.data?.message || error.message || 'Login failed');
      return { 
        success: false, 
        message: errorMsg 
      };
    }
  };

  const logout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    localStorage.removeItem('tenant_id');
    setUser(null);
    window.location.href = '/';
  };

  const updateUser = (newUserData) => {
    setUser(prev => {
      const updated = { ...prev, ...newUserData };
      localStorage.setItem('user', JSON.stringify(updated));
      return updated;
    });
  };

  return (
    <AuthContext.Provider value={{ user, loading, login, logout, updateUser }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => useContext(AuthContext);
