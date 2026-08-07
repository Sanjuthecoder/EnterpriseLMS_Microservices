import React, { createContext, useContext, useState, useEffect } from 'react';
import { useAuth } from './AuthContext';

const TenantContext = createContext(null);

export const TenantProvider = ({ children }) => {
  const { user } = useAuth();
  const [tenantInfo, setTenantInfo] = useState({
    org_id: null,
    company_id: null
  });

  useEffect(() => {
    // Sync tenant info from auth user state
    if (user) {
      setTenantInfo({
        org_id: user.org_id,
        company_id: user.company_id
      });
    } else {
      setTenantInfo({ org_id: null, company_id: null });
    }
  }, [user]);

  return (
    <TenantContext.Provider value={tenantInfo}>
      {children}
    </TenantContext.Provider>
  );
};

export const useTenant = () => useContext(TenantContext);
