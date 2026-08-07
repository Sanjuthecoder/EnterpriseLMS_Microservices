import React from 'react';
import { Lightbulb, Info } from 'lucide-react';

const SuggestionCard = ({ suggestion, type = 'info', onClick }) => {
  const getColors = () => {
    switch (type) {
      case 'warning': return { bg: 'rgba(245, 158, 11, 0.1)', border: 'var(--warning-color)', text: '#b45309', icon: 'var(--warning-color)' };
      case 'danger': return { bg: 'rgba(239, 68, 68, 0.1)', border: 'var(--danger-color)', text: '#b91c1c', icon: 'var(--danger-color)' };
      case 'success': return { bg: 'rgba(16, 185, 129, 0.1)', border: 'var(--success-color)', text: '#047857', icon: 'var(--success-color)' };
      default: return { bg: 'rgba(37, 99, 235, 0.1)', border: 'var(--primary-color)', text: '#1d4ed8', icon: 'var(--primary-color)' };
    }
  };

  const colors = getColors();

  return (
    <div 
      className="glass-panel" 
      style={{
        background: colors.bg,
        borderLeft: `4px solid ${colors.border}`,
        padding: '1.25rem',
        display: 'flex',
        alignItems: 'flex-start',
        gap: '1rem',
        cursor: onClick ? 'pointer' : 'default',
        transition: 'transform 0.2s, box-shadow 0.2s',
        marginBottom: '1rem'
      }}
      onClick={onClick}
    >
      <div style={{ color: colors.icon, marginTop: '2px' }}>
        {type === 'info' ? <Info size={20} /> : <Lightbulb size={20} />}
      </div>
      <div>
        <h4 style={{ margin: '0 0 0.5rem 0', color: colors.text, fontSize: '0.95rem', fontWeight: 600 }}>Action Recommended</h4>
        <p style={{ margin: 0, color: 'var(--text-primary)', fontSize: '0.9rem', lineHeight: 1.5 }}>
          {suggestion}
        </p>
      </div>
    </div>
  );
};

export default SuggestionCard;
