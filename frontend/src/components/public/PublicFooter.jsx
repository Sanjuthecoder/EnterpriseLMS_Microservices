import React from 'react';
import { GraduationCap, Globe, Mail, MessageCircle } from 'lucide-react';
import { Link } from 'react-router-dom';

const PublicFooter = () => {
  return (
    <footer style={{ background: '#0f172a', color: '#f8fafc', padding: '4rem 4rem 2rem 4rem', marginTop: 'auto' }}>
      <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr 1fr 1fr', gap: '4rem', maxWidth: '1200px', margin: '0 auto', borderBottom: '1px solid #334155', paddingBottom: '3rem' }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '1.5rem' }}>
            <GraduationCap size={32} color="#3b82f6" />
            <span style={{ fontWeight: 800, fontSize: '1.5rem', letterSpacing: '-0.5px' }}>
              Enterprise<span style={{ color: '#3b82f6' }}>LMS</span>
            </span>
          </div>
          <p style={{ color: '#94a3b8', lineHeight: 1.6, fontSize: '0.95rem', marginBottom: '1.5rem', maxWidth: '300px' }}>
            Transforming corporate training through adaptive intelligence, deep telemetry, and actionable ROI metrics.
          </p>
          <div style={{ display: 'flex', gap: '1rem', color: '#94a3b8' }}>
            <Globe size={20} style={{ cursor: 'pointer' }} />
            <Mail size={20} style={{ cursor: 'pointer' }} />
            <MessageCircle size={20} style={{ cursor: 'pointer' }} />
          </div>
        </div>

        <div>
          <h4 style={{ fontSize: '1.1rem', fontWeight: 600, marginBottom: '1.5rem', color: '#f1f5f9' }}>Platform</h4>
          <ul style={{ listStyle: 'none', padding: 0, margin: 0, display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
            <li><Link to="/features" style={{ color: '#94a3b8', fontSize: '0.9rem' }}>Adaptive Pathways</Link></li>
            <li><Link to="/features" style={{ color: '#94a3b8', fontSize: '0.9rem' }}>Telemetry & Analytics</Link></li>
            <li><Link to="/features" style={{ color: '#94a3b8', fontSize: '0.9rem' }}>Content Creator Studio</Link></li>
            <li><Link to="/features" style={{ color: '#94a3b8', fontSize: '0.9rem' }}>ROI Tracking</Link></li>
          </ul>
        </div>

        <div>
          <h4 style={{ fontSize: '1.1rem', fontWeight: 600, marginBottom: '1.5rem', color: '#f1f5f9' }}>Company</h4>
          <ul style={{ listStyle: 'none', padding: 0, margin: 0, display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
            <li><Link to="/about" style={{ color: '#94a3b8', fontSize: '0.9rem' }}>About Us</Link></li>
            <li><Link to="/contact" style={{ color: '#94a3b8', fontSize: '0.9rem' }}>Careers</Link></li>
            <li><Link to="/contact" style={{ color: '#94a3b8', fontSize: '0.9rem' }}>Contact Sales</Link></li>
          </ul>
        </div>

        <div>
          <h4 style={{ fontSize: '1.1rem', fontWeight: 600, marginBottom: '1.5rem', color: '#f1f5f9' }}>Legal</h4>
          <ul style={{ listStyle: 'none', padding: 0, margin: 0, display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
            <li><Link to="#" style={{ color: '#94a3b8', fontSize: '0.9rem' }}>Privacy Policy</Link></li>
            <li><Link to="#" style={{ color: '#94a3b8', fontSize: '0.9rem' }}>Terms of Service</Link></li>
            <li><Link to="#" style={{ color: '#94a3b8', fontSize: '0.9rem' }}>Security</Link></li>
          </ul>
        </div>
      </div>
      
      <div style={{ textAlign: 'center', paddingTop: '2rem', color: '#64748b', fontSize: '0.85rem' }}>
        &copy; {new Date().getFullYear()} EnterpriseLMS. All rights reserved. Built for modern organizations.
      </div>
    </footer>
  );
};

export default PublicFooter;
