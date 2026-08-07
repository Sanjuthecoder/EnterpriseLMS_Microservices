import React, { createContext, useContext, useState, useEffect } from 'react';
import { useAuth } from './AuthContext';
import api from '../services/api';

const ThemeContext = createContext(null);

export const ThemeProvider = ({ children }) => {
  const { user } = useAuth();
  const [theme, setTheme] = useState({
    primaryColor: '#2563eb', // Default Blue
    secondaryColor: '#10b981', // Default Green
    fontFamily: 'Inter, sans-serif',
    logoUrl: null,
    portalName: ''
  });

  useEffect(() => {
    // If logged in and part of a company, fetch theme
    if (user?.companyId) {
      api.get(`/auth/companies/${user.companyId}/theme`)
        .then(res => {
          if (res.data) {
            const companyTheme = {
              logoUrl: res.data.logoUrl || null,
              portalName: res.data.themeConfig?.portalName || res.data.companyName || '',
              primaryColor: res.data.themeConfig?.primaryColor || '#2563eb',
              secondaryColor: res.data.themeConfig?.secondaryColor || '#10b981',
              fontFamily: res.data.themeConfig?.fontFamily || 'Inter, sans-serif'
            };
            setTheme(companyTheme);
            applyTheme(companyTheme);
          }
        })
        .catch(err => console.error("Failed to fetch theme", err));
    }
  }, [user]);

  const applyTheme = (themeConfig) => {
    const root = document.documentElement;
    if (themeConfig.primaryColor) {
      root.style.setProperty('--primary-color', themeConfig.primaryColor);
    }
    if (themeConfig.secondaryColor) {
      root.style.setProperty('--secondary-color', themeConfig.secondaryColor);
    }
    if (themeConfig.fontFamily) {
      root.style.setProperty('--font-family', themeConfig.fontFamily);
      document.body.style.fontFamily = themeConfig.fontFamily;
    }
    // Convert hex to rgb for opacity-based rgba variables
    if (themeConfig.primaryColor) {
      const hex = themeConfig.primaryColor.replace('#', '');
      const r = parseInt(hex.substring(0, 2), 16);
      const g = parseInt(hex.substring(2, 4), 16);
      const b = parseInt(hex.substring(4, 6), 16);
      root.style.setProperty('--primary-rgb', `${r}, ${g}, ${b}`);
    }
  };

  const updateTheme = async (newThemeConfig) => {
    try {
      const res = await api.put(`/company-admin/theme`, newThemeConfig);
      if (res.data) {
        const companyTheme = {
          logoUrl: res.data.logoUrl || null,
          portalName: res.data.themeConfig?.portalName || res.data.name || '',
          primaryColor: res.data.themeConfig?.primaryColor || '#2563eb',
          secondaryColor: res.data.themeConfig?.secondaryColor || '#10b981',
          fontFamily: res.data.themeConfig?.fontFamily || 'Inter, sans-serif'
        };
        setTheme(companyTheme);
        applyTheme(companyTheme);
      }
      return { success: true };
    } catch (error) {
      return { success: false, message: 'Failed to update theme' };
    }
  };

  return (
    <ThemeContext.Provider value={{ theme, updateTheme }}>
      {children}
    </ThemeContext.Provider>
  );
};

export const useTheme = () => useContext(ThemeContext);
