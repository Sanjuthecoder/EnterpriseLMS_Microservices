import React, { useState } from 'react';
import { X, CheckCircle, Zap } from 'lucide-react';
import api from '../../services/api';

const loadRazorpayScript = () => {
  return new Promise((resolve) => {
    const script = document.createElement('script');
    script.src = 'https://checkout.razorpay.com/v1/checkout.js';
    script.onload = () => resolve(true);
    script.onerror = () => resolve(false);
    document.body.appendChild(script);
  });
};

const PremiumSubscriptionModal = ({ isOpen, onClose, companyId, onUpgradeSuccess }) => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  if (!isOpen) return null;

  const handlePayment = async () => {
    setLoading(true);
    setError(null);
    try {
      const resLoaded = await loadRazorpayScript();
      if (!resLoaded) {
        throw new Error('Razorpay SDK failed to load. Are you offline or using an adblocker?');
      }

      // Create Order in backend
      const orderRes = await api.post('/v1/payments/orders', {
        companyId: companyId,
        amount: 50000, // INR 50,000
        currency: 'INR'
      });

      const { razorpayOrderId, amount, currency } = orderRes.data;

      const options = {
        key: import.meta.env.VITE_RAZORPAY_KEY_ID || 'rzp_test_TCtsanWkmcb9m2',
        amount: amount * 100,
        currency: currency,
        name: 'Enterprise LMS Premium',
        description: 'Upgrade to Premium Tier',
        order_id: razorpayOrderId,
        handler: async (response) => {
          try {
            await api.post('/v1/payments/verify', {
              razorpayOrderId: response.razorpay_order_id,
              razorpayPaymentId: response.razorpay_payment_id,
              razorpaySignature: response.razorpay_signature,
              adminEmail: 'admin@company.com',
              adminName: 'Company Admin'
            });
            onUpgradeSuccess();
            onClose();
          } catch (verifyError) {
            setError('Payment verification failed.');
          }
        },
        prefill: {
          name: 'Company Admin',
          email: 'admin@company.com'
        },
        theme: {
          color: '#2563EB'
        }
      };

      const rzp = new window.Razorpay(options);
      rzp.on('payment.failed', function (response) {
        setError(response.error.description || 'Payment Failed');
      });
      rzp.open();
    } catch (err) {
      setError(err.message || 'Something went wrong while initiating payment.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, background: 'rgba(0,0,0,0.5)', display: 'flex', justifyContent: 'center', alignItems: 'center', zIndex: 1000 }}>
      <div style={{ background: 'var(--bg-secondary)', padding: '2rem', borderRadius: '12px', width: '90%', maxWidth: '500px', position: 'relative' }}>
        <button onClick={onClose} style={{ position: 'absolute', top: '1rem', right: '1rem', background: 'transparent', border: 'none', cursor: 'pointer', color: 'var(--text-secondary)' }}>
          <X size={24} />
        </button>
        
        <h2 style={{ fontSize: '1.5rem', fontWeight: 700, marginBottom: '1rem', color: 'var(--text-primary)', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <Zap color="#F59E0B" /> Upgrade to Premium
        </h2>
        
        <p style={{ color: 'var(--text-secondary)', marginBottom: '1.5rem' }}>Unlock the full potential of your Enterprise LMS with our premium features designed for advanced analytics and interaction.</p>

        <div style={{ marginBottom: '2rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '0.75rem', color: 'var(--text-primary)' }}>
            <CheckCircle color="#10B981" size={20} /> <span>AI-Based Personalized Learning Experience</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '0.75rem', color: 'var(--text-primary)' }}>
            <CheckCircle color="#10B981" size={20} /> <span>Learning Record Store (LRS) Analytics</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', color: 'var(--text-primary)' }}>
            <CheckCircle color="#10B981" size={20} /> <span>WebRTC Live Video Interactions</span>
          </div>
        </div>

        {error && <div style={{ background: 'rgba(239, 68, 68, 0.1)', color: 'var(--danger-color)', padding: '0.75rem', borderRadius: '8px', marginBottom: '1.5rem', fontSize: '0.9rem' }}>{error}</div>}

        <button 
          onClick={handlePayment} 
          disabled={loading}
          style={{ width: '100%', padding: '0.75rem', background: 'var(--primary-color)', color: 'white', border: 'none', borderRadius: '8px', fontWeight: 600, fontSize: '1rem', cursor: loading ? 'not-allowed' : 'pointer', opacity: loading ? 0.7 : 1 }}
        >
          {loading ? 'Processing...' : 'Pay ₹50,000 & Upgrade Now'}
        </button>
      </div>
    </div>
  );
};

export default PremiumSubscriptionModal;
