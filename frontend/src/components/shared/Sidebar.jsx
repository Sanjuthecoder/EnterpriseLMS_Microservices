import React from 'react';
import { NavLink } from 'react-router-dom';

const Sidebar = ({ links }) => {
  return (
    <aside style={{
      width: '240px',
      minWidth: '240px',
      maxWidth: '240px',
      flexShrink: 0,
      background: 'var(--glass-bg)',
      backdropFilter: 'var(--glass-blur)',
      borderRight: '1px solid var(--glass-border)',
      padding: '2rem 1rem',
      display: 'flex',
      flexDirection: 'column',
      gap: '0.5rem',
      height: 'calc(100vh - 73px)',
      position: 'sticky',
      top: '73px'
    }}>
      {links.map((link, idx) => (
        <NavLink
          key={idx}
          to={link.path}
          end={link.path === '/super-admin' || link.path === '/company-admin' || link.path === '/creator' || link.path === '/employee'}
          style={({ isActive }) => ({
            display: 'flex',
            alignItems: 'center',
            gap: '0.75rem',
            padding: '0.75rem 1rem',
            borderRadius: 'var(--radius-md)',
            color: isActive ? 'white' : 'var(--text-secondary)',
            background: isActive ? 'var(--primary-color)' : 'transparent',
            fontWeight: 500,
            transition: 'all 0.2s ease',
            textDecoration: 'none'
          })}
        >
          {link.icon}
          <span>{link.label}</span>
        </NavLink>
      ))}
    </aside>
  );
};

export default Sidebar;
