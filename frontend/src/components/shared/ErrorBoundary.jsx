import React from 'react';
import { AlertTriangle } from 'lucide-react';

class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, error };
  }

  componentDidCatch(error, errorInfo) {
    console.error("ErrorBoundary caught an error", error, errorInfo);
  }

  render() {
    if (this.state.hasError) {
      return (
        <div style={{
          display: 'flex', 
          justifyContent: 'center', 
          alignItems: 'center', 
          minHeight: '100vh',
          backgroundColor: 'var(--bg-primary, #f9fafb)',
          padding: '2rem'
        }}>
          <div style={{
            backgroundColor: 'var(--card-bg, #ffffff)',
            padding: '3rem',
            borderRadius: '12px',
            boxShadow: '0 10px 25px rgba(0, 0, 0, 0.05)',
            textAlign: 'center',
            maxWidth: '500px'
          }}>
            <AlertTriangle size={64} style={{ color: 'var(--danger-color, #ef4444)', margin: '0 auto 1.5rem auto' }} />
            <h2 style={{ fontSize: '1.5rem', fontWeight: '600', marginBottom: '1rem', color: 'var(--text-primary, #111827)' }}>
              Something went wrong
            </h2>
            <p style={{ color: 'var(--text-secondary, #6b7280)', marginBottom: '2rem', lineHeight: '1.6' }}>
              We've encountered an unexpected error. Our engineering team has been notified.
            </p>
            <button 
              onClick={() => window.location.reload()}
              style={{
                backgroundColor: 'var(--primary-color, #2563eb)',
                color: 'white',
                border: 'none',
                padding: '0.75rem 1.5rem',
                borderRadius: '6px',
                fontWeight: '500',
                cursor: 'pointer',
                transition: 'background-color 0.2s'
              }}
            >
              Refresh Page
            </button>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}

export default ErrorBoundary;
