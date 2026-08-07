import React from 'react';
import { useAuth } from '../../contexts/AuthContext';
import { useTheme } from '../../contexts/ThemeContext';
import { GraduationCap, LogOut, User } from 'lucide-react';

const Navbar = ({ roleTitle }) => {
  const { user, logout } = useAuth();
  const { theme } = useTheme() || {};

  return (
    <nav style={{
      display: 'flex',
      justifyContent: 'space-between',
      alignItems: 'center',
      padding: '1rem 2rem',
      background: 'var(--glass-bg)',
      backdropFilter: 'var(--glass-blur)',
      borderBottom: '1px solid var(--glass-border)',
      position: 'sticky',
      top: 0,
      zIndex: 100
    }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
        {theme?.logoUrl ? (
          <img 
            src={theme.logoUrl} 
            alt={theme?.portalName || "Company Logo"} 
            style={{ maxHeight: '36px', maxWidth: '120px', objectFit: 'contain' }} 
          />
        ) : (
          <GraduationCap size={32} color="var(--primary-color)" />
        )}
        <span style={{ fontWeight: 700, fontSize: '1.25rem', letterSpacing: '-0.5px' }}>
          {theme?.portalName || (
            <>
              Enterprise<span style={{ color: 'var(--primary-color)' }}>LMS</span>
            </>
          )}
        </span>
        {roleTitle && (
          <span style={{
            fontSize: '0.75rem',
            background: 'rgba(var(--primary-rgb), 0.1)',
            color: 'var(--primary-color)',
            padding: '2px 8px',
            borderRadius: '12px',
            fontWeight: 600,
            textTransform: 'uppercase'
          }}>
            {roleTitle}
          </span>
        )}
      </div>

      <div style={{ display: 'flex', alignItems: 'center', gap: '1.5rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <User size={18} color="var(--text-secondary)" />
          <span style={{ fontSize: '0.9rem', fontWeight: 500 }}>{user?.username || 'Guest'}</span>
        </div>
        <button 
          onClick={logout} 
          className="btn" 
          style={{
            padding: '0.5rem 1.2rem',
            fontSize: '0.85rem',
            display: 'flex',
            alignItems: 'center',
            gap: '0.5rem',
            backgroundColor: 'rgba(239, 68, 68, 0.08)',
            color: 'var(--danger-color)',
            border: '1px solid rgba(239, 68, 68, 0.2)',
            fontWeight: 600,
            borderRadius: 'var(--radius-md)',
            cursor: 'pointer',
            transition: 'all 0.2s ease-in-out'
          }}
          onMouseEnter={(e) => {
            e.currentTarget.style.backgroundColor = 'var(--danger-color)';
            e.currentTarget.style.color = 'white';
            e.currentTarget.style.borderColor = 'var(--danger-color)';
          }}
          onMouseLeave={(e) => {
            e.currentTarget.style.backgroundColor = 'rgba(239, 68, 68, 0.08)';
            e.currentTarget.style.color = 'var(--danger-color)';
            e.currentTarget.style.borderColor = 'rgba(239, 68, 68, 0.2)';
          }}
        >
          <LogOut size={15} /> Log Out
        </button>
      </div>
    </nav>
  );
};

export default Navbar;
