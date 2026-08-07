import React from 'react';
import { NavLink, Link } from 'react-router-dom';
import { GraduationCap } from 'lucide-react';

const PublicNavbar = () => {
  return (
    <nav style={{
      display: 'flex',
      justifyContent: 'space-between',
      alignItems: 'center',
      padding: '1.25rem 4rem',
      background: 'rgba(255, 255, 255, 0.9)',
      backdropFilter: 'blur(12px)',
      borderBottom: '1px solid var(--border-color)',
      position: 'sticky',
      top: 0,
      zIndex: 1000
    }}>
      <Link to="/" style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', textDecoration: 'none', color: 'var(--text-primary)' }}>
        <GraduationCap size={36} color="var(--primary-color)" />
        <span style={{ fontWeight: 800, fontSize: '1.5rem', letterSpacing: '-0.5px' }}>
          Enterprise<span style={{ color: 'var(--primary-color)' }}>LMS</span>
        </span>
      </Link>

      <div style={{ display: 'flex', alignItems: 'center', gap: '2.5rem' }}>
        <div style={{ display: 'flex', gap: '2rem' }}>
          <NavLink to="/" style={({ isActive }) => ({ color: isActive ? 'var(--primary-color)' : 'var(--text-primary)', fontWeight: 600, fontSize: '0.95rem' })}>Home</NavLink>
          <NavLink to="/features" style={({ isActive }) => ({ color: isActive ? 'var(--primary-color)' : 'var(--text-primary)', fontWeight: 600, fontSize: '0.95rem' })}>Features & ROI</NavLink>
          <NavLink to="/about" style={({ isActive }) => ({ color: isActive ? 'var(--primary-color)' : 'var(--text-primary)', fontWeight: 600, fontSize: '0.95rem' })}>About Us</NavLink>
          <NavLink to="/contact" style={({ isActive }) => ({ color: isActive ? 'var(--primary-color)' : 'var(--text-primary)', fontWeight: 600, fontSize: '0.95rem' })}>Contact</NavLink>
        </div>
        <div style={{ display: 'flex', gap: '1rem' }}>
          <Link to="/auth" className="btn btn-secondary" style={{ padding: '0.6rem 1.5rem' }}>Login</Link>
          <Link to="/auth" className="btn btn-primary" style={{ padding: '0.6rem 1.5rem' }}>Get Started</Link>
        </div>
      </div>
    </nav>
  );
};

export default PublicNavbar;
