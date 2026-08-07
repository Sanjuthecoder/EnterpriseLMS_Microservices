import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import AnalyticsOverview from '../AnalyticsOverview';
import api from '../../../services/api';
import aiApi from '../../../services/aiApi';
import * as AuthContext from '../../../contexts/AuthContext';
import * as ToastProvider from '../../../components/shared/ToastProvider';

jest.mock('../../../services/api', () => ({
  __esModule: true,
  default: {
    get: jest.fn()
  }
}));

jest.mock('../../../services/aiApi', () => ({
  __esModule: true,
  default: {
    getCompanyInsights: jest.fn()
  },
  isPremiumGateError: jest.fn(),
  getAiErrorMessage: jest.fn()
}));

const mockNavigate = jest.fn();
jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockNavigate,
  useLocation: () => ({ state: null })
}));

const mockToast = {
  premium: jest.fn(),
  error: jest.fn(),
  success: jest.fn()
};

describe('AnalyticsOverview Component - AI Integration', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    jest.spyOn(ToastProvider, 'useToast').mockReturnValue(mockToast);

    // Mock base API calls
    api.get.mockImplementation((url) => {
      if (url.includes('/employees')) return Promise.resolve({ data: [] });
      if (url.includes('/courses')) return Promise.resolve({ data: [{ courseId: '101', title: 'Test Course' }] });
      if (url.includes('/enrollments')) return Promise.resolve({ data: [] });
      if (url.includes('/analytics/roi')) return Promise.resolve({ data: {} });
      return Promise.resolve({ data: {} });
    });
  });

  it('shows premium lock when FREE user selects a course for AI analysis', async () => {
    jest.spyOn(AuthContext, 'useAuth').mockReturnValue({
      user: { userId: 1, role: 'COMPANY_ADMIN', subscriptionTier: 'FREE', companyId: 99 }
    });

    render(<BrowserRouter><AnalyticsOverview /></BrowserRouter>);

    await waitFor(() => {
      expect(screen.getByText(/Test Course/i)).toBeInTheDocument();
    });

    // Actually there are two selects. The AI one has default option '-- Analyze Course with AI --'
    const aiSelect = screen.getByDisplayValue('-- Analyze Course with AI --');
    fireEvent.change(aiSelect, { target: { value: '101' } });

    await waitFor(() => {
      expect(screen.getByText(/Premium Feature/i)).toBeInTheDocument();
      expect(aiApi.getCompanyInsights).not.toHaveBeenCalled();
    });
  });

  it('fetches and displays AI insights when PREMIUM user selects a course', async () => {
    jest.spyOn(AuthContext, 'useAuth').mockReturnValue({
      user: { userId: 1, role: 'COMPANY_ADMIN', subscriptionTier: 'PREMIUM', companyId: 99 }
    });

    aiApi.getCompanyInsights.mockResolvedValueOnce({
      data: {
        averageCompletionPercentage: 85,
        instructionalDesignSummary: 'Great course.',
        generatedAt: new Date().toISOString()
      }
    });

    render(<BrowserRouter><AnalyticsOverview /></BrowserRouter>);

    await waitFor(() => {
      expect(screen.getByText(/Test Course/i)).toBeInTheDocument();
    });

    const aiSelect = screen.getByDisplayValue('-- Analyze Course with AI --');
    fireEvent.change(aiSelect, { target: { value: '101' } });

    await waitFor(() => {
      expect(aiApi.getCompanyInsights).toHaveBeenCalledWith('101', 99);
      expect(screen.getByText(/Instructional Design Summary/i)).toBeInTheDocument();
      expect(screen.getByText(/Great course/i)).toBeInTheDocument();
    });
  });
});
